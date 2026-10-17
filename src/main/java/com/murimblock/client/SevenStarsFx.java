package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.AuraParticle;
import com.murimblock.martial.SevenStarsParticles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.lowdragmc.photon.client.light.LightProvider;
import com.lowdragmc.photon.client.light.LightSink;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Seven Stars Sword's light on the client (docs/combat/styles/seven-stars-sword.md, "Les effets"): simple, thin and
 * clean shapes, the show made by the light (Enzo, 17/10/2026: "les effets plus clairs").
 * <ul>
 *   <li><b>the stars on a target</b>: seven places on a ring over its head, turning slowly; each star it carries lights
 *   one (a four-pointed star of white light, a soft halo), the others stay as faint points, so the count reads at a
 *   glance. Star dust falls from the lit ones;</li>
 *   <li><b>a full constellation</b>: the seven leave the ring for the Big Dipper, facing the viewer over the head,
 *   joined by fine lines of light, the whole pulsing;</li>
 *   <li><b>the burst</b>: the seven plunge into the target; a thin ring of light runs out along the ground, a white
 *   flash at its heart, a spray of star dust.</li>
 *   <li><b>Shooting Star</b>: where the player left, their figure stays a moment in pale light and falls apart into star
 *   dust; the streak is a fine, sharp line of light with a soft glow round it, a comet's head running along it in the
 *   first ticks, then fading from its start to its end; a cross of light flares on each body it cut, and a star where it
 *   ends.</li>
 * </ul>
 * The art is light (Enzo, 17/10/2026: "utilise Photon pour les effets lumineux, il va falloir jouer autour des lumières vu
 * que c'est les étoiles"): every lit star is a small Photon point light that lights the target and the ground under it, a
 * full constellation a stronger light pulsing in the Dipper with a touch of volumetric glow, a burst a bright light at the
 * heart that dies with the ring, the plunging stars each lit ({@link #submitLights}). Drawn additively (RenderType.eyes:
 * bright on black, black adds nothing), always lit.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SevenStarsFx {
    static final ResourceLocation STAR = tex("star"), DOT = tex("dot"), BEAM = tex("beam"), RING = tex("ring"),
            SEAL = tex("seal_dipper"), SIGIL = tex("seal_star"), SWORD = tex("light_sword");
    /** The ring of places over the head: its radius and height above the head (blocks), its turn (radians a tick). */
    private static final float RING_RADIUS = 0.55F, RING_HEIGHT = 0.45F, RING_SPIN = 0.045F;
    /** A lit star's size, a place not lit, the Dipper's scale (blocks a unit), how long the Dipper takes to form. */
    private static final float STAR_SIZE = 0.42F, PLACE_SIZE = 0.09F, DIPPER_SCALE = 0.2F, FORM_TICKS = 6;
    /** The Big Dipper: its seven stars from the handle's end to the bowl, its lines (scripts/concepts/seven-stars-mock.cjs). */
    private static final float[][] DIPPER = {{-2.2F, 0.1F}, {-1.2F, 0.35F}, {-0.4F, 0.4F}, {0.4F, 0.4F}, {1.9F, 1.0F}, {2.0F, -0.1F}, {0.8F, -0.6F}};
    private static final int[][] LINES = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 3}};
    /** How long a burst shows (ticks), the plunge's share of it, and how far its ring runs (blocks). */
    private static final int BURST_TICKS = 16, PLUNGE_TICKS = 4;
    private static final float BURST_REACH = 2.6F;

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/fx/seven_stars/" + name + ".png");
    }

    /** What a target shows: its stars, and since when its constellation is full (or -1). */
    private static final class Shown {
        UUID owner;
        int count;
        float fullSince = -1;
    }

    private record Burst(int entityId, Vec3 at, float height, float start) { }

    /** A Shooting Star: who, from where to where (their feet), when, the bodies it cut. */
    private record Streak(int playerId, Vec3 from, Vec3 to, float start, List<Integer> hits) { }

    private static final List<Streak> STREAKS = new ArrayList<>();

    /** A figure left in pale light where a player stood: who, where, from when, for how long. */
    record Ghost(int playerId, Vec3 at, float start, float life) { }

    static final List<Ghost> GHOSTS = new ArrayList<>();
    /** How long a streak shows (ticks), how long its head runs, how long the figure left behind lasts. */
    private static final int STREAK_TICKS = 14, HEAD_TICKS = 3, GHOST_TICKS = 10;
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/afterimage.png");
    /** True while the figure left behind is drawn: its name tag is not. */
    private static boolean drawingGhost;

    private static final Map<Integer, Shown> SHOWN = new HashMap<>();
    private static final List<Burst> BURSTS = new ArrayList<>();
    private static SpriteSet dust;

    private SevenStarsFx() {
    }

    /** The stars' Photon lights, asked for every frame. */
    private static final LightProvider LIGHTS = SevenStarsFx::submitLights;
    /** Light colours: the stars' pale blue-white, a burst's white. */
    static final float[] STAR_LIGHT = {0.7F, 0.84F, 1.0F}, BURST_LIGHT = {0.9F, 0.95F, 1.0F};
    /** At most this many targets light their stars one by one (beyond, one light each). */
    private static final int LIT_TARGETS = 12;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PhotonLights.addProvider(LIGHTS));
    }

    /**
     * Photon asks for this frame's lights: a small light at each lit star (the nearest targets), a stronger one pulsing in
     * a full constellation, a burst's bright light dying with its ring, a light on each star plunging into the body.
     */
    private static void submitLights(LightSink sink, float partial) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float now = level.getGameTime() + partial;
        Camera camera = mc.gameRenderer.getMainCamera();
        SevenStarsTechniqueFx.lights(sink, level, now, camera);
        if (SHOWN.isEmpty() && BURSTS.isEmpty() && STREAKS.isEmpty()) return;
        Vec3 eye = camera.getPosition();
        List<Map.Entry<Integer, Shown>> near = new ArrayList<>();
        for (Map.Entry<Integer, Shown> en : SHOWN.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e != null && e.distanceToSqr(eye) < 64 * 64) near.add(en);
        }
        near.sort((a, b) -> Double.compare(level.getEntity(a.getKey()).distanceToSqr(eye), level.getEntity(b.getKey()).distanceToSqr(eye)));
        for (int n = 0; n < near.size(); n++) {
            Entity e = level.getEntity(near.get(n).getKey());
            Shown s = near.get(n).getValue();
            boolean full = s.fullSince >= 0;
            if (n < LIT_TARGETS) for (int i = 0; i < s.count; i++) {
                float twinkle = 0.85F + 0.15F * Mth.sin(now * 0.3F + i * 1.7F);
                light(sink, placeOf(e, s, i, now, camera), STAR_LIGHT, 0.45F * twinkle, 4.5F, 0);
            }
            if (full) {
                Vec3 top = e.getPosition(partial).add(0, e.getBbHeight() + RING_HEIGHT + 0.25, 0);
                light(sink, top, STAR_LIGHT, 1.4F + 0.4F * Mth.sin(now * 0.4F), 8, 0.3F);
            }
        }
        for (Streak st : STREAKS) {
            float t = now - st.start, fade = 1 - Mth.clamp(t / STREAK_TICKS, 0, 1);
            Vec3 a = st.from.add(0, 0.9, 0), b = st.to.add(0, 0.9, 0);
            if (t < HEAD_TICKS) light(sink, a.lerp(b, t / HEAD_TICKS), STAR_LIGHT, 3.0F, 7, 0.4F);
            light(sink, a.lerp(b, 0.5), STAR_LIGHT, 1.6F * fade * fade, 2 + (float) a.distanceTo(b) / 2, 0.2F * fade);
            light(sink, b, BURST_LIGHT, 2.0F * fade, 6, 0);
            if (t < 8) for (int id : st.hits) {
                Entity e = level.getEntity(id);
                if (e != null) light(sink, e.getBoundingBox().getCenter(), BURST_LIGHT, 1.8F * (1 - t / 8), 4, 0);
            }
        }
        for (Burst b : BURSTS) {
            float t = now - b.start, k = Mth.clamp(t / BURST_TICKS, 0, 1);
            Vec3 heart = b.at.add(0, b.height * 0.6, 0);
            light(sink, heart, BURST_LIGHT, 4.5F * (1 - k) * (1 - k), 12, 0.6F * (1 - k));
            if (t < PLUNGE_TICKS) {
                Vec3 top = b.at.add(0, b.height + RING_HEIGHT + 0.25, 0);
                for (int i = 0; i < 7; i++) light(sink, dipperPlace(top, i, camera).lerp(heart, (t / PLUNGE_TICKS) * (t / PLUNGE_TICKS)), STAR_LIGHT, 0.5F, 3, 0);
            }
        }
    }

    static void light(LightSink sink, Vec3 at, float[] c, float intensity, float range, float volumetric) {
        if (intensity <= 0.01F) return;
        DynamicLight l = sink.next();
        if (l == null) return;
        l.reset().point().at(at.x, at.y, at.z).color(c[0], c[1], c[2]).intensity(intensity).range(range).sourceRadius(0.15F).volumetric(volumetric);
    }

    // ---------------------------------------------------------------- from the server

    /** The stars a target carries now (0: none). */
    public static void marks(int entityId, UUID owner, int count) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (count <= 0) { SHOWN.remove(entityId); return; }
        Shown s = SHOWN.computeIfAbsent(entityId, k -> new Shown());
        s.owner = owner;
        if (count >= 7 && s.count < 7) {
            s.fullSince = level.getGameTime();
            Entity e = level.getEntity(entityId);
        } else if (count < 7) {
            s.fullSince = -1;
        }
        s.count = count;
    }

    /** A constellation burst: the plunge, the ring, the flash, the dust. */
    public static void burst(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        SHOWN.remove(entityId);
        Entity e = level.getEntity(entityId);
        if (e == null) return;
        BURSTS.add(new Burst(entityId, e.position(), e.getBbHeight(), level.getGameTime()));
        Vec3 heart = e.position().add(0, e.getBbHeight() * 0.6, 0);
        RandomSource random = level.random;
        for (int i = 0; i < 40; i++) {
            double a = random.nextDouble() * Math.PI * 2, up = random.nextDouble() * 0.25;
            double speed = 0.15 + random.nextDouble() * 0.25;
            spark(level, heart.x, heart.y, heart.z, Math.cos(a) * speed, up, Math.sin(a) * speed, 0.1F, 18 + random.nextInt(14));
        }
    }

    /** A Shooting Star went: the streak, the figure left behind falling into dust, the crosses on the bodies cut. */
    public static void streak(int playerId, Vector3f from, Vector3f to, List<Integer> hits) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Vec3 a = new Vec3(from.x(), from.y(), from.z()), b = new Vec3(to.x(), to.y(), to.z());
        STREAKS.add(new Streak(playerId, a, b, level.getGameTime(), List.copyOf(hits)));
        GHOSTS.add(new Ghost(playerId, a, level.getGameTime(), GHOST_TICKS));
        RandomSource random = level.random;
        // The figure left behind will fall into dust: the dust rises from where it stood.
        for (int i = 0; i < 26; i++)
            spark(level, a.x + (random.nextDouble() - 0.5) * 0.6, a.y + random.nextDouble() * 1.8, a.z + (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.02, 0.01 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.02, 0.06F, 24 + random.nextInt(16));
        // Sparkles left along the line.
        for (int i = 0; i < 24; i++) {
            Vec3 at = a.lerp(b, random.nextDouble()).add(0, 0.6 + random.nextDouble() * 0.6, 0);
            spark(level, at.x, at.y, at.z, 0, -0.005, 0, 0.05F, 14 + random.nextInt(12));
        }
        for (int id : hits) {
            Entity e = level.getEntity(id);
            if (e == null) continue;
            Vec3 c = e.getBoundingBox().getCenter();
            for (int i = 0; i < 10; i++)
                spark(level, c.x, c.y, c.z, (random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.3) * 0.2, (random.nextDouble() - 0.5) * 0.25, 0.08F, 12 + random.nextInt(8));
        }
    }

    /** No name tag on the figure left behind. */
    @SubscribeEvent
    public static void onNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent event) {
        if (drawingGhost) event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
    }

    // ---------------------------------------------------------------- particles and light

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SevenStarsParticles.STAR_DUST.get(), sprites -> {
            dust = sprites;
            return new AuraParticle.Provider(sprites);
        });
    }

    static void spark(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life) {
        if (dust == null) return;
        AuraParticle p = new AuraParticle(level, x, y, z, vx, vy, vz, dust).big(size, life);
        p.setColor(0.82F, 0.9F, 1.0F);
        Minecraft.getInstance().particleEngine.add(p);
    }

    /** Star dust falls from the lit stars now and then. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) { clear(); return; }
        if (mc.isPaused()) return;
        long now = level.getGameTime();
        BURSTS.removeIf(b -> now - b.start > BURST_TICKS);
        STREAKS.removeIf(st -> now - st.start > Math.max(STREAK_TICKS, GHOST_TICKS));
        GHOSTS.removeIf(g -> now - g.start > g.life);
        SevenStarsTechniqueFx.tick(level, now);
        for (Iterator<Map.Entry<Integer, Shown>> it = SHOWN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Shown> en = it.next();
            Entity e = level.getEntity(en.getKey());
            if (e == null || !e.isAlive()) { it.remove(); continue; }
            Shown s = en.getValue();
            if (level.random.nextInt(7) >= s.count) continue;
            Vec3 at = placeOf(e, s, level.random.nextInt(s.count), now, mc.gameRenderer.getMainCamera());
            spark(level, at.x, at.y, at.z, (level.random.nextDouble() - 0.5) * 0.01, -0.02, (level.random.nextDouble() - 0.5) * 0.01, 0.045F, 20 + level.random.nextInt(10));
        }
    }

    // ---------------------------------------------------------------- drawing

    /** Where a target's star `i` stands now: on the ring, or in the Dipper once full (blending while it forms). */
    private static Vec3 placeOf(Entity e, Shown s, int i, float now, Camera camera) {
        Vec3 top = e.getPosition(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)).add(0, e.getBbHeight() + RING_HEIGHT, 0);
        double a = now * RING_SPIN + i * Math.PI * 2 / 7;
        Vec3 ring = top.add(Math.cos(a) * RING_RADIUS, 0, Math.sin(a) * RING_RADIUS);
        if (s.fullSince < 0) return ring;
        float k = Mth.clamp((now - s.fullSince) / FORM_TICKS, 0, 1);
        k = k * k * (3 - 2 * k);
        return ring.lerp(dipperPlace(top.add(0, 0.25, 0), i, camera), k);
    }

    /** Star `i` of the Big Dipper over a point, facing the camera. */
    static Vec3 dipperPlace(Vec3 centre, int i, Camera camera) {
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();
        float u = DIPPER[i][0] * DIPPER_SCALE, v = DIPPER[i][1] * DIPPER_SCALE;
        return centre.add(-left.x() * u + up.x() * v, -left.y() * u + up.y() * v, -left.z() * u + up.z() * v);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || (SHOWN.isEmpty() && BURSTS.isEmpty() && STREAKS.isEmpty() && SevenStarsTechniqueFx.idle())) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = level.getGameTime() + partial;
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        Matrix4f m = pose.last().pose();
        // Gathered by texture, then drawn one texture at a time (a buffer source holds one open batch).
        Map<ResourceLocation, List<java.util.function.Consumer<VertexConsumer>>> draws = new java.util.LinkedHashMap<>();
        for (ResourceLocation t : new ResourceLocation[] {SEAL, SIGIL, RING, BEAM, SWORD, DOT, STAR}) draws.put(t, new ArrayList<>());
        for (Map.Entry<Integer, Shown> en : SHOWN.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null || e.isInvisible() || (e == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson())) continue;
            Shown s = en.getValue();
            boolean full = s.fullSince >= 0;
            float pulse = full ? 0.8F + 0.2F * Mth.sin(now * 0.4F) : 1;
            // The lines, once the Dipper has formed.
            if (full) {
                float k = Mth.clamp((now - s.fullSince - FORM_TICKS * 0.5F) / FORM_TICKS, 0, 1);
                for (int[] l : LINES) {
                    Vec3 a = placeOf(e, s, l[0], now, camera).subtract(eye), b = placeOf(e, s, l[1], now, camera).subtract(eye);
                    draws.get(BEAM).add(out -> beam(out, m, a, b, eye, 0.035F, 0.7F * k * pulse));
                }
            }
            for (int i = 0; i < 7; i++) {
                Vec3 at = placeOf(e, s, i, now, camera).subtract(eye);
                if (i < s.count) {
                    // Each star twinkles a little on its own beat.
                    float twinkle = 0.85F + 0.15F * Mth.sin(now * 0.3F + i * 1.7F), size = STAR_SIZE * (full ? 1.15F : 1) * twinkle, spin = now * 0.02F + i;
                    draws.get(STAR).add(out -> sprite(out, m, at, camera, size, pulse, spin));
                } else {
                    draws.get(DOT).add(out -> sprite(out, m, at, camera, PLACE_SIZE, 0.35F, 0));
                }
            }
        }
        for (Burst b : BURSTS) drawBurst(draws, m, b, now, camera, eye);
        for (Streak st : STREAKS) drawStreak(draws, m, st, now, camera, eye, level);
        SevenStarsTechniqueFx.draw(draws, m, now, camera, eye, level);
        drawGhosts(pose, buffers, now, eye, level, partial);
        for (Map.Entry<ResourceLocation, List<java.util.function.Consumer<VertexConsumer>>> d : draws.entrySet()) {
            if (d.getValue().isEmpty()) continue;
            RenderType type = RenderType.eyes(d.getKey());
            VertexConsumer out = buffers.getBuffer(type);
            for (var draw : d.getValue()) draw.accept(out);
            buffers.endBatch(type);
        }
    }

    /** The burst: the seven plunge into the body, then the ring runs out along the ground and the flash fades. */
    private static void drawBurst(Map<ResourceLocation, List<java.util.function.Consumer<VertexConsumer>>> draws, Matrix4f m, Burst b, float now, Camera camera, Vec3 eye) {
        float t = now - b.start;
        Vec3 heart = b.at.add(0, b.height * 0.6, 0);
        if (t < PLUNGE_TICKS) {
            float k = t / PLUNGE_TICKS;
            Vec3 top = b.at.add(0, b.height + RING_HEIGHT + 0.25, 0);
            for (int i = 0; i < 7; i++) {
                Vec3 at = dipperPlace(top, i, camera).lerp(heart, k * k).subtract(eye);
                int n = i;
                draws.get(STAR).add(out -> sprite(out, m, at, camera, STAR_SIZE * (1 - 0.4F * k), 1, n));
            }
        }
        float k = Mth.clamp(t / BURST_TICKS, 0, 1), fade = 1 - k;
        // The ring along the ground.
        Vec3 ground = b.at.add(0, 0.06, 0).subtract(eye);
        draws.get(RING).add(out -> flat(out, m, ground, 0.4F + BURST_REACH * (1 - (1 - k) * (1 - k)), fade));
        // The flash at the heart.
        if (t < 8) draws.get(DOT).add(out -> sprite(out, m, heart.subtract(eye), camera, 1.6F * (1 - t / 8), 1 - t / 8, 0));
    }

    /**
     * A streak: a fine core and a soft glow, fading from its start to its end; the comet's head running along it first;
     * a star where it ends; a cross of light on each body cut.
     */
    private static void drawStreak(Map<ResourceLocation, List<java.util.function.Consumer<VertexConsumer>>> draws, Matrix4f m, Streak st, float now,
                                   Camera camera, Vec3 eye, ClientLevel level) {
        float t = now - st.start;
        if (t > STREAK_TICKS) return;
        float fade = 1 - t / STREAK_TICKS;
        Vec3 a = st.from.add(0, 0.9, 0), b = st.to.add(0, 0.9, 0);
        // The line shows behind the head, then wipes away from where it began.
        float head = Mth.clamp(t / HEAD_TICKS, 0, 1), tail = Mth.clamp((t - HEAD_TICKS) / (STREAK_TICKS - HEAD_TICKS), 0, 1);
        Vec3 from = a.lerp(b, tail * tail).subtract(eye), to = a.lerp(b, head).subtract(eye);
        draws.get(BEAM).add(out -> beam(out, m, from, to, eye, 0.12F, fade));
        draws.get(BEAM).add(out -> beam(out, m, from, to, eye, 0.9F, 0.45F * fade));
        // Two fine speed lines along it, above and below.
        for (float dy : new float[] {0.32F, -0.42F}) {
            Vec3 f2 = a.lerp(b, Math.min(1, tail + 0.15F)).add(0, dy, 0).subtract(eye), t2 = a.lerp(b, head * 0.9F).add(0, dy, 0).subtract(eye);
            draws.get(BEAM).add(out -> beam(out, m, f2, t2, eye, 0.04F, 0.6F * fade));
        }
        if (t < HEAD_TICKS + 1) {
            Vec3 at = a.lerp(b, head).subtract(eye);
            draws.get(STAR).add(out -> sprite(out, m, at, camera, 1.1F, 1, now * 0.3F));
            draws.get(DOT).add(out -> sprite(out, m, at, camera, 1.4F, 0.6F, 0));
        }
        if (t >= HEAD_TICKS - 1 && t < HEAD_TICKS + 8) {
            float k = (t - HEAD_TICKS + 1) / 9;
            Vec3 end = b.subtract(eye);
            draws.get(STAR).add(out -> sprite(out, m, end, camera, 1.3F * (1 - k * 0.5F), 1 - k, k));
        }
        if (t < 7) for (int id : st.hits) {
            Entity e = level.getEntity(id);
            if (e == null) continue;
            Vec3 c = e.getBoundingBox().getCenter().subtract(eye);
            float k = t / 7;
            draws.get(STAR).add(out -> sprite(out, m, c, camera, 1.5F + 0.5F * k, 1 - k, (float) Math.PI / 4));
        }
    }

    /** The figure left where a streak began: the player's own model as it is now, in pale light, fading. */
    private static void drawGhosts(PoseStack pose, MultiBufferSource.BufferSource buffers, float now, Vec3 eye, ClientLevel level, float partial) {
        Minecraft mc = Minecraft.getInstance();
        RenderType glow = RenderType.eyes(WHITE);
        drawingGhost = true;
        try {
            for (Ghost st : GHOSTS) {
                float t = now - st.start;
                if (t < 0 || t > st.life) continue;
                Entity e = level.getEntity(st.playerId);
                if (!(e instanceof net.minecraft.world.entity.player.Player player) || (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson())) continue;
                float b = 0.6F * (1 - t / st.life);
                @SuppressWarnings("unchecked")
                var renderer = (net.minecraft.client.renderer.entity.EntityRenderer<net.minecraft.world.entity.player.Player>) mc.getEntityRenderDispatcher().getRenderer(player);
                MultiBufferSource tinted = type -> new CloudStepEffects.Tinted(buffers.getBuffer(glow), (int) (170 * b), (int) (200 * b), (int) (255 * b));
                pose.pushPose();
                pose.translate(st.at.x - eye.x, st.at.y - eye.y, st.at.z - eye.z);
                renderer.render(player, Mth.lerp(partial, player.yRotO, player.getYRot()), partial, pose, tinted, LightTexture.FULL_BRIGHT);
                pose.popPose();
            }
        } finally {
            drawingGhost = false;
        }
        buffers.endBatch(glow);
    }

    /** A square sprite facing the camera, `size` blocks across, turned `spin` radians, brightness `b`. */
    static void sprite(VertexConsumer out, Matrix4f m, Vec3 at, Camera camera, float size, float b, float spin) {
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();
        float h = size / 2, c = Mth.cos(spin) * h, s = Mth.sin(spin) * h;
        // Two corner vectors turned by `spin` in the camera's plane.
        Vector3f a = new Vector3f(left).mul(c).add(new Vector3f(up).mul(s));
        Vector3f d = new Vector3f(left).mul(-s).add(new Vector3f(up).mul(c));
        int lum = (int) (255 * Mth.clamp(b, 0, 1));
        vertex(out, m, at.x + a.x() + d.x(), at.y + a.y() + d.y(), at.z + a.z() + d.z(), 0, 0, lum);
        vertex(out, m, at.x + a.x() - d.x(), at.y + a.y() - d.y(), at.z + a.z() - d.z(), 0, 1, lum);
        vertex(out, m, at.x - a.x() - d.x(), at.y - a.y() - d.y(), at.z - a.z() - d.z(), 1, 1, lum);
        vertex(out, m, at.x - a.x() + d.x(), at.y - a.y() + d.y(), at.z - a.z() + d.z(), 1, 0, lum);
    }

    /** A flat square on the ground, `radius` blocks out from its middle. */
    static void flat(VertexConsumer out, Matrix4f m, Vec3 at, float radius, float b) {
        int lum = (int) (255 * Mth.clamp(b, 0, 1));
        vertex(out, m, at.x - radius, at.y, at.z - radius, 0, 0, lum);
        vertex(out, m, at.x - radius, at.y, at.z + radius, 0, 1, lum);
        vertex(out, m, at.x + radius, at.y, at.z + radius, 1, 1, lum);
        vertex(out, m, at.x + radius, at.y, at.z - radius, 1, 0, lum);
    }

    /** A line of light from a to b (camera-relative), `width` blocks wide, turned to face the eye. */
    static void beam(VertexConsumer out, Matrix4f m, Vec3 a, Vec3 b, Vec3 eye, float width, float bright) {
        Vec3 along = b.subtract(a);
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = along.cross(mid).normalize().scale(width / 2);
        if (!Double.isFinite(side.x)) return;
        int lum = (int) (255 * Mth.clamp(bright, 0, 1));
        vertex(out, m, a.x + side.x, a.y + side.y, a.z + side.z, 0, 0, lum);
        vertex(out, m, a.x - side.x, a.y - side.y, a.z - side.z, 0, 1, lum);
        vertex(out, m, b.x - side.x, b.y - side.y, b.z - side.z, 1, 1, lum);
        vertex(out, m, b.x + side.x, b.y + side.y, b.z + side.z, 1, 0, lum);
    }

    static void vertex(VertexConsumer out, Matrix4f m, double x, double y, double z, float u, float v, int lum) {
        out.addVertex(m, (float) x, (float) y, (float) z).setColor(lum, lum, lum, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    /** For tests and tours: forget everything shown. */
    public static void clear() {
        SHOWN.clear();
        BURSTS.clear();
        STREAKS.clear();
        GHOSTS.clear();
        SevenStarsTechniqueFx.clear();
    }

    /** One of the techniques' effects (SevenStarsPayloads.Fx). */
    public static void fx(com.murimblock.network.SevenStarsPayloads.Fx payload) {
        SevenStarsTechniqueFx.receive(payload);
    }

    /** The stars shown over a body, set from elsewhere (the finish lights them one by one). */
    static void show(int entityId, int count) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (count <= 0) { SHOWN.remove(entityId); return; }
        Shown s = SHOWN.computeIfAbsent(entityId, k -> new Shown());
        if (count >= 7 && s.count < 7) s.fullSince = level.getGameTime();
        s.count = count;
    }

    /** For tours: the stars this client shows on an entity (0 if none). */
    public static int shown(int entityId) {
        Shown s = SHOWN.get(entityId);
        return s == null ? 0 : s.count;
    }
}
