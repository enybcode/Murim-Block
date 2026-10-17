package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.LightProvider;
import com.lowdragmc.photon.client.light.LightSink;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.AuraParticle;
import com.murimblock.martial.ThunderSaberParticles;
import com.murimblock.network.ThunderSaberPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Purple Heaven Thunder Saber's lightning on the client (docs/combat/styles/purple-heaven-thunder-saber.md, "Les
 * effets"): sharp forms, the show made by the light and the contrast (Enzo, 17/10/2026: the higher arts rich in effects,
 * "tu te sers toujours de Photon"). Violet, white and gold:
 * <ul>
 *   <li><b>the bolts</b>: a jagged path from the sky, a white core and a violet glow, branches, re-drawn every other tick
 *   so they crackle, then gone; a flash and a scorch where they land;</li>
 *   <li><b>the shockwaves</b>: a ring of cracks and a thin ring running out along the ground, a flash at the heart;</li>
 *   <li><b>the arcs</b>: Rolling Thunder's lightning leaping from body to body; the electrified crackling all over;</li>
 *   <li><b>Rolling Thunder</b>: a wide disc of light round the turning saber; <b>Thunder Descends</b>: the leap's trail;</li>
 *   <li><b>Heaven's Drum</b>: a cage of vertical arcs round the player, a flare and a deeper pulse at each blow stopped,
 *   the discharge's ring when it is beaten;</li>
 *   <li><b>Severing Flash</b>: arcs crawling over the player while it charges, then a crescent sweeping ahead;</li>
 *   <li><b>Nine Heavens' Wrath</b>: the thunder seal turning on the ground, lighting up bolt after bolt, a dark storm
 *   cloud turning over it, lit from inside at each bolt, and the column of lightning;</li>
 *   <li><b>Thunder Sovereign's Descent</b>: the player wrapped in crackling lightning, a light following them.</li>
 * </ul>
 * Every flash, bolt, arc and charge is a Photon light ({@link #submitLights}). Drawn additively (RenderType.eyes: bright on
 * black, black adds nothing), always lit; the cloud alone translucent.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ThunderSaberFx {
    static final ResourceLocation BEAM = tex("beam"), GLOW = tex("glow"), RING = tex("ring"), SHOCK = tex("shock"), SEAL = tex("seal"),
            CRESCENT = tex("crescent"), CLOUD = tex("cloud");
    /** The lightning's violet, its white heart, the gold of the sparks. */
    static final float[] VIOLET = {0.7F, 0.5F, 1.0F}, WHITE = {0.95F, 0.92F, 1.0F}, GOLD = {1.0F, 0.82F, 0.35F}, STEEL = {0.86F, 0.9F, 1.0F};

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/fx/thunder/" + name + ".png");
    }

    private ThunderSaberFx() {
    }

    // ---------------------------------------------------------------- what is shown

    private record Bolt(Vec3 from, Vec3 to, float start, float life, float width, long seed) { }
    private record Shock(Vec3 at, float start, float reach, boolean thunder) { }
    private record Arcs(List<Vec3> chain, float start, long seed) { }
    private record Leap(int playerId, Vec3 from, Vec3 to, float start, float ticks, boolean thunder) { }
    private record Spin(int playerId, float start, float ticks, boolean thunder) { }
    private record Wave(Vec3 from, Vec3 to, float start, float ticks, boolean thunder) { }
    private record Wrath(Vec3 centre, float radius, float start, float length, int bolts) { }
    private record Column(Vec3 at, float radius, float start) { }
    private record Answer(int targetId, float start) { }

    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final List<Shock> SHOCKS = new ArrayList<>();
    private static final List<Arcs> ARCS = new ArrayList<>();
    private static final List<Leap> LEAPS = new ArrayList<>();
    private static final List<Spin> SPINS = new ArrayList<>();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final List<Wrath> WRATHS = new ArrayList<>();
    private static final List<Column> COLUMNS = new ArrayList<>();
    private static final List<Answer> ANSWERS = new ArrayList<>();
    /** Each player's drums (beats), Heaven's Drum held (since when), Severing Flash charging (since when), the Descent (until). */
    private static final Map<Integer, Integer> DRUMS = new HashMap<>();
    private static final Map<Integer, Float> DRUMMING = new HashMap<>(), CHARGING = new HashMap<>(), SOVEREIGN = new HashMap<>();
    /** The blows Heaven's Drum stopped, and when the last was, for its pulse. */
    private static final Map<Integer, float[]> DRUM_PULSE = new HashMap<>();
    /** The electrified bodies: until when. */
    private static final Map<Integer, Float> ELECTRIFIED = new HashMap<>();

    private static final int BOLT_LIFE = 8, SHOCK_TICKS = 14, ARC_TICKS = 6, COLUMN_TICKS = 16, ANSWER_TICKS = 16;
    private static SpriteSet sparks;

    static boolean idle() {
        return BOLTS.isEmpty() && SHOCKS.isEmpty() && ARCS.isEmpty() && LEAPS.isEmpty() && SPINS.isEmpty() && WAVES.isEmpty()
                && WRATHS.isEmpty() && COLUMNS.isEmpty() && ANSWERS.isEmpty() && DRUMMING.isEmpty() && CHARGING.isEmpty()
                && SOVEREIGN.isEmpty() && ELECTRIFIED.isEmpty();
    }

    public static void clear() {
        BOLTS.clear(); SHOCKS.clear(); ARCS.clear(); LEAPS.clear(); SPINS.clear(); WAVES.clear(); WRATHS.clear(); COLUMNS.clear();
        ANSWERS.clear(); DRUMS.clear(); DRUMMING.clear(); CHARGING.clear(); SOVEREIGN.clear(); DRUM_PULSE.clear(); ELECTRIFIED.clear();
    }

    /** For the HUD and tours: a player's beats as this client knows them. */
    public static int beats(int entityId) {
        return DRUMS.getOrDefault(entityId, 0);
    }

    /** For tours: the electrified bodies this client shows. */
    public static int electrifiedShown() {
        return ELECTRIFIED.size();
    }

    // ---------------------------------------------------------------- from the server

    public static void drums(int entityId, int beats) {
        if (beats <= 0) DRUMS.remove(entityId); else DRUMS.put(entityId, beats);
    }

    public static void electrified(int entityId, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (ticks <= 0) ELECTRIFIED.remove(entityId);
        else ELECTRIFIED.put(entityId, level.getGameTime() + (float) ticks);
    }

    private static Vec3 v(Vector3f f) {
        return new Vec3(f.x(), f.y(), f.z());
    }

    public static void fx(ThunderSaberPayloads.Fx fx) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float now = level.getGameTime();
        List<Vec3> p = fx.points().stream().map(ThunderSaberFx::v).toList();
        RandomSource random = level.random;
        boolean thunder = !fx.ids().isEmpty() && fx.ids().get(0) == 1;
        switch (fx.kind()) {
            case ThunderSaberPayloads.Fx.BOLT -> {
                if (p.size() < 2) return;
                BOLTS.add(new Bolt(p.get(0), p.get(1), now, BOLT_LIFE, Math.max(0.4F, fx.value() / 10.0F), random.nextLong()));
                burstSparks(level, p.get(1).add(0, 0.2, 0), 18, 0.25);
            }
            case ThunderSaberPayloads.Fx.SHOCKWAVE -> {
                if (p.isEmpty()) return;
                SHOCKS.add(new Shock(p.get(0), now, fx.value() / 10.0F, thunder));
                for (int i = 0; i < 26; i++) {
                    double a = random.nextDouble() * Math.PI * 2, sp = 0.2 + random.nextDouble() * 0.25;
                    spark(level, p.get(0).x, p.get(0).y + 0.15, p.get(0).z, Math.cos(a) * sp, 0.05 + random.nextDouble() * 0.15, Math.sin(a) * sp,
                            0.09F, 10 + random.nextInt(10), thunder ? VIOLET : GOLD);
                }
            }
            case ThunderSaberPayloads.Fx.ARCS -> {
                if (p.size() >= 2) ARCS.add(new Arcs(p, now, random.nextLong()));
                for (Vec3 q : p.subList(1, p.size())) burstSparks(level, q, 8, 0.15);
            }
            case ThunderSaberPayloads.Fx.LEAP -> {
                if (p.size() >= 2) LEAPS.add(new Leap(fx.playerId(), p.get(0), p.get(1), now + 4, Math.max(1, fx.value()), thunder));
            }
            case ThunderSaberPayloads.Fx.SPIN -> SPINS.add(new Spin(fx.playerId(), now, Math.max(1, fx.value()), thunder));
            case ThunderSaberPayloads.Fx.DRUM -> {
                DRUMMING.put(fx.playerId(), now);
                DRUM_PULSE.put(fx.playerId(), new float[] {0, -100});
            }
            case ThunderSaberPayloads.Fx.DRUM_BLOCK -> {
                DRUM_PULSE.put(fx.playerId(), new float[] {fx.value(), now});
                Entity e = level.getEntity(fx.playerId());
                if (e != null && !p.isEmpty()) {
                    Vec3 chest = e.position().add(0, 1.2, 0);
                    Vec3 meet = chest.add(p.get(0).subtract(e.position()).multiply(1, 0, 1).normalize().scale(1.45));
                    burstSparks(level, meet, 14, 0.2);
                    BOLTS.add(new Bolt(meet.add(0, 1.0, 0), meet.add(0, -1.1, 0), now, 4, 0.5F, random.nextLong()));
                }
            }
            case ThunderSaberPayloads.Fx.DRUM_END -> {
                DRUMMING.remove(fx.playerId());
                DRUM_PULSE.remove(fx.playerId());
                if (!p.isEmpty() && fx.value() > 0) SHOCKS.add(new Shock(p.get(0), now, fx.value() / 10.0F, thunder));
            }
            case ThunderSaberPayloads.Fx.CHARGE -> {
                if (fx.value() > 0) CHARGING.put(fx.playerId(), now); else CHARGING.remove(fx.playerId());
            }
            case ThunderSaberPayloads.Fx.WAVE -> {
                if (p.size() >= 2) WAVES.add(new Wave(p.get(0), p.get(1), now, Math.max(1, fx.value()), thunder));
            }
            case ThunderSaberPayloads.Fx.WRATH -> {
                if (p.isEmpty()) return;
                int length = fx.ids().isEmpty() ? 80 : fx.ids().get(0), bolts = fx.ids().size() > 1 ? fx.ids().get(1) : 6;
                WRATHS.add(new Wrath(p.get(0), fx.value() / 10.0F, now, length, bolts));
            }
            case ThunderSaberPayloads.Fx.COLUMN -> {
                if (p.isEmpty()) return;
                COLUMNS.add(new Column(p.get(0), Math.max(0.6F, fx.value() / 10.0F), now));
                burstSparks(level, p.get(0).add(0, 0.3, 0), 40, 0.4);
            }
            case ThunderSaberPayloads.Fx.SOVEREIGN -> SOVEREIGN.put(fx.playerId(), now + Math.max(1, fx.value()));
            case ThunderSaberPayloads.Fx.ANSWERS -> ANSWERS.add(new Answer(fx.target(), now));
            default -> { }
        }
    }

    // ---------------------------------------------------------------- particles

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ThunderSaberParticles.THUNDER_SPARK.get(), set -> {
            sparks = set;
            return new AuraParticle.Provider(set);
        });
    }

    static void spark(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float size, int life, float[] c) {
        if (sparks == null) return;
        AuraParticle p = new AuraParticle(level, x, y, z, vx, vy, vz, sparks).big(size, life);
        p.setColor(c[0], c[1], c[2]);
        Minecraft.getInstance().particleEngine.add(p);
    }

    private static void burstSparks(ClientLevel level, Vec3 at, int n, double speed) {
        RandomSource r = level.random;
        for (int i = 0; i < n; i++)
            spark(level, at.x, at.y, at.z, (r.nextDouble() - 0.5) * speed * 2, r.nextDouble() * speed, (r.nextDouble() - 0.5) * speed * 2,
                    0.07F, 8 + r.nextInt(10), i % 4 == 0 ? GOLD : i % 3 == 0 ? WHITE : VIOLET);
    }

    // ---------------------------------------------------------------- ticks

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) { clear(); return; }
        if (mc.isPaused()) return;
        float now = level.getGameTime();
        BOLTS.removeIf(b -> now - b.start > b.life);
        SHOCKS.removeIf(s -> now - s.start > SHOCK_TICKS);
        ARCS.removeIf(a -> now - a.start > ARC_TICKS);
        LEAPS.removeIf(l -> now - l.start > l.ticks + 8);
        SPINS.removeIf(s -> now - s.start > s.ticks + 6);
        WAVES.removeIf(w -> now - w.start > w.ticks + 10);
        WRATHS.removeIf(w -> now - w.start > w.length + 20);
        COLUMNS.removeIf(c -> now - c.start > COLUMN_TICKS);
        ANSWERS.removeIf(a -> now - a.start > ANSWER_TICKS);
        SOVEREIGN.values().removeIf(until -> now > until);
        ELECTRIFIED.values().removeIf(until -> now > until);
        DRUMMING.values().removeIf(since -> now - since > 80);
        CHARGING.values().removeIf(since -> now - since > 80);
        RandomSource r = level.random;
        // The electrified crackle: a short arc somewhere on the body now and then, sparks.
        for (Map.Entry<Integer, Float> en : ELECTRIFIED.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null || r.nextInt(3) != 0) continue;
            Vec3 c = e.position().add((r.nextDouble() - 0.5) * e.getBbWidth(), r.nextDouble() * e.getBbHeight(), (r.nextDouble() - 0.5) * e.getBbWidth());
            BOLTS.add(new Bolt(c, c.add((r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.9, (r.nextDouble() - 0.5) * 0.9), now, 2, 0.25F, r.nextLong()));
            spark(level, c.x, c.y, c.z, 0, 0.02, 0, 0.05F, 6, VIOLET);
        }
        // The Descent: arcs all over the player, sparks at their feet.
        for (Integer id : SOVEREIGN.keySet()) {
            Entity e = level.getEntity(id);
            if (e == null) continue;
            for (int k = 0; k < 2; k++) {
                Vec3 c = e.position().add((r.nextDouble() - 0.5) * 1.2, r.nextDouble() * 2.2, (r.nextDouble() - 0.5) * 1.2);
                BOLTS.add(new Bolt(c, c.add((r.nextDouble() - 0.5) * 1.4, (r.nextDouble() - 0.3) * 1.4, (r.nextDouble() - 0.5) * 1.4), now, 2, 0.35F, r.nextLong()));
            }
            spark(level, e.getX(), e.getY() + 0.1, e.getZ(), (r.nextDouble() - 0.5) * 0.3, 0.1, (r.nextDouble() - 0.5) * 0.3, 0.08F, 10, r.nextBoolean() ? VIOLET : GOLD);
        }
        // Severing Flash charging: sparks gather round the player, faster as it charges.
        for (Map.Entry<Integer, Float> en : CHARGING.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null) continue;
            float k = Mth.clamp((now - en.getValue()) / 30F, 0, 1);
            if (r.nextFloat() > 0.3F + 0.7F * k) continue;
            double a = r.nextDouble() * Math.PI * 2;
            Vec3 c = e.position().add(Math.cos(a) * 1.4, 0.4 + r.nextDouble() * 1.6, Math.sin(a) * 1.4);
            Vec3 to = e.position().add(0, 1.0, 0).subtract(c).scale(0.12);
            spark(level, c.x, c.y, c.z, to.x, to.y, to.z, 0.06F, 8, k > 0.95F ? WHITE : VIOLET);
        }
        // The storm over Nine Heavens' Wrath: a few drops of light fall from it.
        for (Wrath w : WRATHS) {
            float t = now - w.start;
            if (t < 6 || t > w.length || r.nextInt(2) != 0) continue;
            double a = r.nextDouble() * Math.PI * 2, rr = Math.sqrt(r.nextDouble()) * w.radius;
            spark(level, w.centre.x + Math.cos(a) * rr, w.centre.y + 11, w.centre.z + Math.sin(a) * rr, 0, -0.5, 0, 0.05F, 20, VIOLET);
        }
    }

    // ---------------------------------------------------------------- light

    private static final LightProvider LIGHTS = ThunderSaberFx::submitLights;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PhotonLights.addProvider(LIGHTS));
    }

    private static void light(LightSink sink, Vec3 at, float[] c, float intensity, float range, float volumetric) {
        if (intensity <= 0.01F) return;
        DynamicLight l = sink.next();
        if (l == null) return;
        l.reset().point().at(at.x, at.y, at.z).color(c[0], c[1], c[2]).intensity(intensity).range(range).sourceRadius(0.2F).volumetric(volumetric);
    }

    /**
     * Photon asks for this frame's lights: each bolt a hard flash at its foot and a glow along it, the arcs, the shockwaves'
     * hearts, the drum's pulse, the charge growing, the storm lit at each bolt, the column, the drums' glow at the saber.
     */
    private static void submitLights(LightSink sink, float partial) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || idle() && DRUMS.isEmpty()) return;
        float now = level.getGameTime() + partial;
        for (Bolt b : BOLTS) {
            float k = 1 - Mth.clamp((now - b.start) / b.life, 0, 1);
            if (b.width < 0.45F) { light(sink, b.to, VIOLET, 0.6F * k, 3, 0); continue; }
            light(sink, b.to.add(0, 0.6, 0), WHITE, 6.0F * k * k * b.width, 10 + 6 * b.width, 0.5F * k);
            light(sink, b.from.lerp(b.to, 0.5), VIOLET, 3.0F * k * b.width, 14, 0.4F * k);
        }
        for (Shock s : SHOCKS) {
            float k = 1 - Mth.clamp((now - s.start) / SHOCK_TICKS, 0, 1);
            light(sink, s.at.add(0, 0.8, 0), s.thunder ? VIOLET : GOLD, 3.5F * k * k, 4 + s.reach * 2, 0.2F * k);
        }
        for (Arcs a : ARCS) {
            float k = 1 - Mth.clamp((now - a.start) / ARC_TICKS, 0, 1);
            for (int i = 1; i < a.chain.size(); i++) light(sink, a.chain.get(i), VIOLET, 2.4F * k, 6, 0.2F * k);
        }
        for (Column c : COLUMNS) {
            float k = 1 - Mth.clamp((now - c.start) / COLUMN_TICKS, 0, 1);
            light(sink, c.at.add(0, 2, 0), WHITE, 9.0F * k * k, 22, 0.8F * k);
            light(sink, c.at.add(0, 8, 0), VIOLET, 5.0F * k, 18, 0.5F * k);
        }
        for (Wave w : WAVES) {
            float t = now - w.start, k = Mth.clamp(t / w.ticks, 0, 1);
            if (t > w.ticks + 2) continue;
            light(sink, w.from.lerp(w.to, k), w.thunder ? VIOLET : STEEL, 3.0F, 7, 0.3F);
        }
        for (Wrath w : WRATHS) {
            float t = now - w.start;
            if (t > w.length) continue;
            light(sink, w.centre.add(0, 0.5, 0), VIOLET, 1.2F * Mth.clamp(t / 10, 0, 1), 6 + w.radius, 0.2F);
        }
        for (Map.Entry<Integer, Float> en : DRUMMING.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null) continue;
            float[] pulse = DRUM_PULSE.getOrDefault(en.getKey(), new float[] {0, -100});
            float hit = Math.max(0, 1 - (now - pulse[1]) / 6);
            light(sink, e.position().add(0, 1.2, 0), VIOLET, 1.4F + 0.15F * pulse[0] + 3.0F * hit, 6 + 0.4F * pulse[0], 0.25F);
        }
        for (Map.Entry<Integer, Float> en : CHARGING.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null) continue;
            float k = Mth.clamp((now - en.getValue()) / 30F, 0, 1);
            light(sink, e.position().add(0, 1.0, 0), k > 0.95F ? WHITE : VIOLET, 0.5F + 3.0F * k, 3 + 5 * k, 0.3F * k);
        }
        for (Map.Entry<Integer, Float> en : SOVEREIGN.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e != null) light(sink, e.getPosition(partial).add(0, 1.2, 0), VIOLET, 3.0F + 0.6F * Mth.sin(now * 0.9F), 9, 0.4F);
        }
        for (Answer a : ANSWERS) {
            Entity e = level.getEntity(a.targetId);
            float k = 1 - Mth.clamp((now - a.start) / ANSWER_TICKS, 0, 1);
            if (e != null) light(sink, e.position().add(0, e.getBbHeight() + 5, 0), WHITE, 4.0F * k, 12, 0.6F * k);
        }
        // The drums' glow at the saber of each player who has some: a pulse of violet, stronger with each drum.
        for (Map.Entry<Integer, Integer> en : DRUMS.entrySet()) {
            Entity e = level.getEntity(en.getKey());
            if (e == null || en.getValue() < 3 || e.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > 48 * 48) continue;
            float drums = en.getValue() / 3;
            light(sink, e.getPosition(partial).add(0, 1.1, 0), VIOLET, (0.25F + 0.15F * Mth.sin(now * 0.3F)) * drums, 2.5F + drums, 0);
        }
    }

    // ---------------------------------------------------------------- drawing

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || idle()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = level.getGameTime() + partial;
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        // The storm cloud first, translucent, so the light drawn after shows through it.
        List<Consumer<VertexConsumer>> clouds = new ArrayList<>();
        for (Wrath w : WRATHS) cloud(clouds, m, w, now, camera, eye);
        if (!clouds.isEmpty()) {
            RenderType type = RenderType.entityTranslucent(CLOUD);
            VertexConsumer out = buffers.getBuffer(type);
            for (var d : clouds) d.accept(out);
            buffers.endBatch(type);
        }
        Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws = new LinkedHashMap<>();
        for (ResourceLocation t : new ResourceLocation[] {SEAL, SHOCK, RING, CRESCENT, BEAM, GLOW}) draws.put(t, new ArrayList<>());
        for (Wrath w : WRATHS) drawSeal(draws, m, w, now, eye);
        for (Shock s : SHOCKS) drawShock(draws, m, s, now, camera, eye);
        for (Bolt b : BOLTS) drawBolt(draws, m, b, now, camera, eye);
        for (Arcs a : ARCS) drawArcs(draws, m, a, now, eye);
        for (Leap l : LEAPS) drawLeap(draws, m, l, now, eye);
        for (Spin s : SPINS) drawSpin(draws, m, s, now, eye, level, partial);
        for (Wave w : WAVES) drawWave(draws, m, w, now, camera, eye);
        for (Column c : COLUMNS) drawColumn(draws, m, c, now, camera, eye);
        for (Answer a : ANSWERS) drawAnswer(draws, m, a, now, camera, eye, level);
        for (Map.Entry<Integer, Float> en : DRUMMING.entrySet()) drawDrum(draws, m, en.getKey(), en.getValue(), now, camera, eye, level, partial);
        for (Map.Entry<ResourceLocation, List<Consumer<VertexConsumer>>> d : draws.entrySet()) {
            if (d.getValue().isEmpty()) continue;
            RenderType type = RenderType.eyes(d.getKey());
            VertexConsumer out = buffers.getBuffer(type);
            for (var draw : d.getValue()) draw.accept(out);
            buffers.endBatch(type);
        }
    }

    /** A jagged path from a to b, the same for a given seed: `segments` pieces, `jag` of their length thrown aside. */
    static List<Vec3> jagged(Vec3 a, Vec3 b, int segments, double jag, long seed) {
        RandomSource r = RandomSource.create(seed);
        Vec3 along = b.subtract(a);
        double len = along.length();
        Vec3 dir = along.normalize();
        Vec3 side = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = side.cross(dir).normalize(), w = dir.cross(u).normalize();
        List<Vec3> pts = new ArrayList<>();
        pts.add(a);
        for (int i = 1; i < segments; i++) {
            double k = (double) i / segments, amp = Math.sin(k * Math.PI) * jag * len / segments * 2.2;
            pts.add(a.add(along.scale(k)).add(u.scale((r.nextDouble() - 0.5) * 2 * amp)).add(w.scale((r.nextDouble() - 0.5) * 2 * amp)));
        }
        pts.add(b);
        return pts;
    }

    /** A bolt: a white core, a violet glow, two branches; it crackles (a new path every other tick) and dies out. */
    private static void drawBolt(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Bolt b, float now, Camera camera, Vec3 eye) {
        float t = now - b.start, k = 1 - Mth.clamp(t / b.life, 0, 1);
        if (k <= 0) return;
        long seed = b.seed + (long) (t / 2);
        double len = b.from.distanceTo(b.to);
        int segments = Math.max(3, Math.min(18, (int) (len * 1.6)));
        List<Vec3> pts = jagged(b.from, b.to, segments, 0.9, seed);
        float flick = t < 1 ? 1 : (seed % 3 == 0 ? 0.65F : 1);
        for (int i = 1; i < pts.size(); i++) {
            Vec3 a = pts.get(i - 1).subtract(eye), c = pts.get(i).subtract(eye);
            draws.get(BEAM).add(out -> beam(out, m, a, c, 0.13F * b.width, WHITE, k * flick));
            draws.get(BEAM).add(out -> beam(out, m, a, c, 0.6F * b.width, VIOLET, 0.6F * k * flick));
            draws.get(BEAM).add(out -> beam(out, m, a, c, 1.4F * b.width, VIOLET, 0.18F * k * flick));
        }
        // Branches, out from the upper half.
        RandomSource r = RandomSource.create(seed * 31);
        for (int n = 0; n < (b.width >= 0.8F ? 3 : b.width >= 0.45F ? 1 : 0); n++) {
            Vec3 s = pts.get(1 + r.nextInt(Math.max(1, pts.size() / 2)));
            Vec3 e = s.add((r.nextDouble() - 0.5) * 3 * b.width, -r.nextDouble() * 2.5 * b.width, (r.nextDouble() - 0.5) * 3 * b.width);
            List<Vec3> br = jagged(s, e, 4, 1.3, seed + n);
            for (int i = 1; i < br.size(); i++) {
                Vec3 a = br.get(i - 1).subtract(eye), c = br.get(i).subtract(eye);
                draws.get(BEAM).add(out -> beam(out, m, a, c, 0.05F * b.width, WHITE, 0.8F * k));
                draws.get(BEAM).add(out -> beam(out, m, a, c, 0.25F * b.width, VIOLET, 0.4F * k));
            }
        }
        if (b.width >= 0.45F && t < 4) {
            Vec3 foot = b.to.add(0, 0.3, 0).subtract(eye);
            float s = (1.6F + b.width * 2) * (1 - t / 4);
            draws.get(GLOW).add(out -> sprite(out, m, foot, camera, s, WHITE, 1 - t / 4));
            // A scorch of cracks where it struck.
            Vec3 ground = b.to.add(0, 0.04, 0).subtract(eye);
            draws.get(SHOCK).add(out -> flat(out, m, ground, 0.7F * b.width + 0.3F, 0, VIOLET, 0.8F * k));
        }
    }

    /** A shockwave: a ring of cracks on the ground and a thin ring running out, a flash at its heart. */
    private static void drawShock(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Shock s, float now, Camera camera, Vec3 eye) {
        float t = now - s.start, k = Mth.clamp(t / SHOCK_TICKS, 0, 1), fade = 1 - k;
        float[] c = s.thunder ? VIOLET : GOLD;
        Vec3 ground = s.at.add(0, 0.05, 0).subtract(eye);
        float run = 1 - (1 - k) * (1 - k);
        draws.get(SHOCK).add(out -> flat(out, m, ground, s.reach * (0.5F + 0.5F * run), 0, c, fade));
        draws.get(RING).add(out -> flat(out, m, ground.add(0, 0.02, 0), 0.3F + s.reach * 1.1F * run, 0, WHITE, fade));
        draws.get(RING).add(out -> flat(out, m, ground.add(0, 0.6, 0), 0.3F + s.reach * 0.8F * run, 0, c, 0.6F * fade));
        if (t < 5) {
            Vec3 heart = s.at.add(0, 0.6, 0).subtract(eye);
            draws.get(GLOW).add(out -> sprite(out, m, heart, camera, 2.2F * (1 - t / 5), c, 1 - t / 5));
        }
    }

    /** Arcs leaping from point to point, crackling. */
    private static void drawArcs(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Arcs a, float now, Vec3 eye) {
        float t = now - a.start, k = 1 - Mth.clamp(t / ARC_TICKS, 0, 1);
        long seed = a.seed + (long) t;
        for (int i = 1; i < a.chain.size(); i++) {
            List<Vec3> pts = jagged(a.chain.get(i - 1), a.chain.get(i), 7, 1.0, seed + i);
            for (int j = 1; j < pts.size(); j++) {
                Vec3 p0 = pts.get(j - 1).subtract(eye), p1 = pts.get(j).subtract(eye);
                draws.get(BEAM).add(out -> beam(out, m, p0, p1, 0.07F, WHITE, k));
                draws.get(BEAM).add(out -> beam(out, m, p0, p1, 0.32F, VIOLET, 0.5F * k));
            }
        }
    }

    /** Thunder Descends' leap: its arc drawn behind the body, fading. */
    private static void drawLeap(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Leap l, float now, Vec3 eye) {
        float t = now - l.start;
        if (t < 0) return;
        float head = Mth.clamp(t / l.ticks, 0, 1), fade = 1 - Mth.clamp((t - l.ticks) / 8, 0, 1);
        float[] c = l.thunder ? VIOLET : STEEL;
        int n = 12;
        for (int i = 1; i <= n; i++) {
            float s0 = (i - 1) / (float) n * head, s1 = i / (float) n * head;
            Vec3 a = arc(l, s0).subtract(eye), b = arc(l, s1).subtract(eye);
            float bright = fade * (0.25F + 0.75F * (i / (float) n));
            draws.get(BEAM).add(out -> beam(out, m, a, b, 0.5F, c, 0.55F * bright));
        }
    }

    private static Vec3 arc(Leap l, float s) {
        double e = s * s * (3 - 2 * s);
        return l.from.lerp(l.to, e).add(0, 1.1 + Math.sin(s * Math.PI) * 2.6, 0);
    }

    /** Rolling Thunder: a wide disc of light round the turning saber, brightest at its leading edge. */
    private static void drawSpin(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Spin s, float now, Vec3 eye, ClientLevel level, float partial) {
        Entity e = level.getEntity(s.playerId);
        if (e == null) return;
        float t = now - s.start;
        float fade = t < s.ticks ? Math.min(1, t / 2) : 1 - Mth.clamp((t - s.ticks) / 6, 0, 1);
        float[] c = s.thunder ? VIOLET : STEEL;
        Vec3 centre = e.getPosition(partial).add(0, 1.15, 0);
        // The saber's sweep: a fan of thin lines from near the body out to its reach, turning with the spin.
        double turn = t / (double) 9 * Math.PI * 2;
        for (int i = 0; i < 18; i++) {
            double a = turn - i * 0.17;
            float bright = fade * (1 - i / 18F);
            Vec3 in = centre.add(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9).subtract(eye), outP = centre.add(Math.cos(a) * 2.6, -0.1, Math.sin(a) * 2.6).subtract(eye);
            draws.get(BEAM).add(out -> beam(out, m, in, outP, 0.45F, c, 0.5F * bright));
        }
        Vec3 ring = centre.subtract(eye);
        draws.get(RING).add(out -> flat(out, m, ring, 2.7F, (float) turn, c, 0.55F * fade));
    }

    /** Severing Flash's wave: a crescent lying flat, sweeping ahead, a trail of light behind it. */
    private static void drawWave(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Wave w, float now, Camera camera, Vec3 eye) {
        float t = now - w.start, k = Mth.clamp(t / w.ticks, 0, 1), fade = 1 - Mth.clamp((t - w.ticks) / 10, 0, 1);
        float[] c = w.thunder ? VIOLET : STEEL;
        Vec3 dir = w.to.subtract(w.from).multiply(1, 0, 1).normalize();
        Vec3 at = w.from.lerp(w.to, k).subtract(eye);
        if (t <= w.ticks + 1) {
            float size = 2.6F + (w.thunder ? 0.8F : 0);
            draws.get(CRESCENT).add(out -> lying(out, m, at, dir, size, c, 1));
            draws.get(CRESCENT).add(out -> lying(out, m, at.add(0, 0.05, 0), dir, size * 0.8F, WHITE, 0.8F));
            draws.get(GLOW).add(out -> sprite(out, m, at, camera, 1.6F, c, 0.7F));
        }
        Vec3 a = w.from.subtract(eye), b = w.from.lerp(w.to, k).subtract(eye);
        draws.get(BEAM).add(out -> beam(out, m, a, b, 0.12F, WHITE, 0.7F * fade));
        draws.get(BEAM).add(out -> beam(out, m, a, b, 0.9F, c, 0.3F * fade));
    }

    /** The thunder seal under Nine Heavens' Wrath, turning slowly, brighter as the bolts come. */
    private static void drawSeal(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Wrath w, float now, Vec3 eye) {
        float t = now - w.start;
        float in = Mth.clamp(t / 8, 0, 1), out = 1 - Mth.clamp((t - w.length) / 16, 0, 1);
        float lit = Mth.clamp((t - 22) / 30F, 0, 1);
        Vec3 ground = w.centre.add(0, 0.06, 0).subtract(eye);
        draws.get(SEAL).add(o -> flat(o, m, ground, w.radius + 0.4F, t * 0.02F, VIOLET, (0.55F + 0.45F * lit) * in * out));
        draws.get(SEAL).add(o -> flat(o, m, ground.add(0, 0.02, 0), w.radius + 0.4F, t * 0.02F, GOLD, 0.35F * lit * in * out));
        draws.get(RING).add(o -> flat(o, m, ground.add(0, 0.03, 0), w.radius + 1.2F, -t * 0.03F, VIOLET, 0.4F * in * out));
    }

    /** The storm cloud: dark puffs in a ring over the circle, turning, lit violet from inside at each bolt. */
    private static void cloud(List<Consumer<VertexConsumer>> draws, Matrix4f m, Wrath w, float now, Camera camera, Vec3 eye) {
        float t = now - w.start;
        float in = Mth.clamp(t / 14, 0, 1), out = 1 - Mth.clamp((t - w.length) / 20, 0, 1), a = in * out;
        if (a <= 0) return;
        RandomSource r = RandomSource.create((long) (w.centre.x * 31 + w.centre.z * 17));
        // The last bolt's flash lights the cloud from inside.
        float flash = 0;
        for (Bolt b : BOLTS) if (b.from.y > w.centre.y + 8 && now - b.start < 4) flash = Math.max(flash, 1 - (now - b.start) / 4);
        int puffs = 28;
        for (int i = 0; i < puffs; i++) {
            double ang = r.nextDouble() * Math.PI * 2 + t * 0.01 * (i % 2 == 0 ? 1 : -1), rad = Math.sqrt(r.nextDouble()) * (w.radius + 2.6);
            Vec3 p = w.centre.add(Math.cos(ang) * rad, 11.5 + r.nextDouble() * 1.4, Math.sin(ang) * rad).subtract(eye);
            float size = 2.6F + (float) r.nextDouble() * 2.4F, spin = (float) r.nextDouble() * 6;
            float lift = flash * (1 - (float) (rad / (w.radius + 2.6)) * 0.5F);
            int rr = (int) (255 * Math.min(1, 0.55F + lift * 0.9F)), gg = (int) (255 * Math.min(1, 0.5F + lift * 0.6F)), bb = (int) (255 * Math.min(1, 0.65F + lift));
            float alpha = a * 0.9F;
            draws.add(o -> translucentSprite(o, m, p, camera, size, spin, rr, gg, bb, alpha));
        }
    }

    /** The column of lightning: a thick core, a ring of bolts round it, rising rings. */
    private static void drawColumn(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Column c, float now, Camera camera, Vec3 eye) {
        float t = now - c.start, k = 1 - Mth.clamp(t / COLUMN_TICKS, 0, 1);
        Vec3 foot = c.at.subtract(eye), top = c.at.add(0, 18, 0).subtract(eye);
        draws.get(BEAM).add(out -> beam(out, m, foot, top, 0.5F * c.radius, WHITE, k));
        draws.get(BEAM).add(out -> beam(out, m, foot, top, 1.6F * c.radius, VIOLET, 0.6F * k));
        long seed = (long) (c.at.x * 13 + c.at.z * 7) + (long) (t / 2);
        for (int i = 0; i < 6; i++) {
            double a = i / 6.0 * Math.PI * 2 + t * 0.2;
            Vec3 base = c.at.add(Math.cos(a) * c.radius * 0.8, 0, Math.sin(a) * c.radius * 0.8);
            List<Vec3> pts = jagged(base.add(0, 14, 0), base, 9, 0.8, seed + i);
            for (int j = 1; j < pts.size(); j++) {
                Vec3 p0 = pts.get(j - 1).subtract(eye), p1 = pts.get(j).subtract(eye);
                draws.get(BEAM).add(out -> beam(out, m, p0, p1, 0.12F, WHITE, 0.8F * k));
            }
        }
        for (int i = 0; i < 3; i++) {
            float h = (t * 0.9F + i * 3) % 9;
            Vec3 ring = c.at.add(0, h, 0).subtract(eye);
            draws.get(RING).add(out -> flat(out, m, ring, c.radius * (1.3F + h * 0.05F), 0, VIOLET, k * (1 - h / 9)));
        }
        if (t < 6) draws.get(GLOW).add(out -> sprite(out, m, c.at.add(0, 1, 0).subtract(eye), camera, 5F * (1 - t / 6), WHITE, 1 - t / 6));
    }

    /** Heaven Answers: a seam of light opening in the sky over the target. */
    private static void drawAnswer(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, Answer a, float now, Camera camera, Vec3 eye, ClientLevel level) {
        Entity e = level.getEntity(a.targetId);
        if (e == null) return;
        float t = now - a.start, k = 1 - Mth.clamp(t / ANSWER_TICKS, 0, 1);
        Vec3 c = e.position().add(0, e.getBbHeight() + 5, 0);
        Vec3 l = c.add(-1.6, 0, 0).subtract(eye), r = c.add(1.6, 0, 0).subtract(eye);
        draws.get(BEAM).add(out -> beam(out, m, l, r, 0.15F + 0.3F * (1 - k), WHITE, k));
        draws.get(GLOW).add(out -> sprite(out, m, c.subtract(eye), camera, 3.0F, VIOLET, 0.7F * k));
    }

    /** Heaven's Drum: a cage of crackling vertical arcs round the player, rings at its foot and top, pulsing at each blow. */
    private static void drawDrum(Map<ResourceLocation, List<Consumer<VertexConsumer>>> draws, Matrix4f m, int playerId, float since, float now, Camera camera,
                                 Vec3 eye, ClientLevel level, float partial) {
        Entity e = level.getEntity(playerId);
        if (e == null) return;
        float t = now - since, in = Mth.clamp(t / 6, 0, 1);
        float[] pulse = DRUM_PULSE.getOrDefault(playerId, new float[] {0, -100});
        float hit = Math.max(0, 1 - (now - pulse[1]) / 6), bright = in * (0.6F + 0.4F * hit);
        Vec3 c = e.getPosition(partial);
        long seed = playerId * 7919L + (long) (t / 2);
        double radius = 1.45;
        for (int i = 0; i < 7; i++) {
            double a = i / 7.0 * Math.PI * 2 + t * 0.08;
            Vec3 bottom = c.add(Math.cos(a) * radius, 0.05, Math.sin(a) * radius), top = c.add(Math.cos(a + 0.4) * radius, 2.4, Math.sin(a + 0.4) * radius);
            List<Vec3> pts = jagged(bottom, top, 6, 0.7, seed + i);
            for (int j = 1; j < pts.size(); j++) {
                Vec3 p0 = pts.get(j - 1).subtract(eye), p1 = pts.get(j).subtract(eye);
                draws.get(BEAM).add(out -> beam(out, m, p0, p1, 0.06F, WHITE, 0.8F * bright));
                draws.get(BEAM).add(out -> beam(out, m, p0, p1, 0.26F, VIOLET, 0.45F * bright));
            }
        }
        Vec3 foot = c.add(0, 0.06, 0).subtract(eye), head = c.add(0, 2.4, 0).subtract(eye);
        draws.get(RING).add(out -> flat(out, m, foot, (float) radius + 0.2F, 0, VIOLET, bright));
        draws.get(RING).add(out -> flat(out, m, head, (float) radius + 0.1F, 0, VIOLET, 0.6F * bright));
        if (hit > 0) draws.get(RING).add(out -> flat(out, m, c.add(0, 1.2, 0).subtract(eye), (float) radius + 0.6F * (1 - hit), 0, WHITE, hit));
    }

    // ---------------------------------------------------------------- quads

    /** A square sprite facing the camera, `size` blocks across, colour c at brightness b. */
    static void sprite(VertexConsumer out, Matrix4f m, Vec3 at, Camera camera, float size, float[] c, float b) {
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();
        float h = size / 2;
        Vector3f a = new Vector3f(left).mul(h), d = new Vector3f(up).mul(h);
        vertex(out, m, at.x + a.x() + d.x(), at.y + a.y() + d.y(), at.z + a.z() + d.z(), 0, 0, c, b);
        vertex(out, m, at.x + a.x() - d.x(), at.y + a.y() - d.y(), at.z + a.z() - d.z(), 0, 1, c, b);
        vertex(out, m, at.x - a.x() - d.x(), at.y - a.y() - d.y(), at.z - a.z() - d.z(), 1, 1, c, b);
        vertex(out, m, at.x - a.x() + d.x(), at.y - a.y() + d.y(), at.z - a.z() + d.z(), 1, 0, c, b);
    }

    /** A flat square on the ground (horizontal), `radius` out from its middle, turned `spin` radians. */
    static void flat(VertexConsumer out, Matrix4f m, Vec3 at, float radius, float spin, float[] c, float b) {
        float cs = Mth.cos(spin) * radius, sn = Mth.sin(spin) * radius;
        vertex(out, m, at.x - cs + sn, at.y, at.z - sn - cs, 0, 0, c, b);
        vertex(out, m, at.x - cs - sn, at.y, at.z - sn + cs, 0, 1, c, b);
        vertex(out, m, at.x + cs - sn, at.y, at.z + sn + cs, 1, 1, c, b);
        vertex(out, m, at.x + cs + sn, at.y, at.z + sn - cs, 1, 0, c, b);
    }

    /** A crescent lying flat, its curve leading along `dir`, `size` blocks across. */
    static void lying(VertexConsumer out, Matrix4f m, Vec3 at, Vec3 dir, float size, float[] c, float b) {
        Vec3 fwd = dir.scale(size / 2), side = new Vec3(-dir.z, 0, dir.x).scale(size / 2);
        // The texture's top (v 0) is the crescent's outer curve: it leads.
        vertex(out, m, at.x + fwd.x - side.x, at.y, at.z + fwd.z - side.z, 0, 0, c, b);
        vertex(out, m, at.x - fwd.x - side.x, at.y, at.z - fwd.z - side.z, 0, 1, c, b);
        vertex(out, m, at.x - fwd.x + side.x, at.y, at.z - fwd.z + side.z, 1, 1, c, b);
        vertex(out, m, at.x + fwd.x + side.x, at.y, at.z + fwd.z + side.z, 1, 0, c, b);
    }

    /** A line of light from a to b (camera-relative), `width` blocks wide, turned to face the eye. */
    static void beam(VertexConsumer out, Matrix4f m, Vec3 a, Vec3 b, float width, float[] c, float bright) {
        Vec3 along = b.subtract(a), mid = a.add(b).scale(0.5);
        Vec3 side = along.cross(mid).normalize().scale(width / 2);
        if (!Double.isFinite(side.x)) return;
        vertex(out, m, a.x + side.x, a.y + side.y, a.z + side.z, 0, 0, c, bright);
        vertex(out, m, a.x - side.x, a.y - side.y, a.z - side.z, 0, 1, c, bright);
        vertex(out, m, b.x - side.x, b.y - side.y, b.z - side.z, 1, 1, c, bright);
        vertex(out, m, b.x + side.x, b.y + side.y, b.z + side.z, 1, 0, c, bright);
    }

    static void vertex(VertexConsumer out, Matrix4f m, double x, double y, double z, float u, float v, float[] c, float b) {
        float k = Mth.clamp(b, 0, 1);
        out.addVertex(m, (float) x, (float) y, (float) z).setColor((int) (255 * c[0] * k), (int) (255 * c[1] * k), (int) (255 * c[2] * k), 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    /** A translucent sprite (the cloud): its own colour and alpha. */
    static void translucentSprite(VertexConsumer out, Matrix4f m, Vec3 at, Camera camera, float size, float spin, int r, int g, int b, float alpha) {
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();
        float h = size / 2, cs = Mth.cos(spin) * h, sn = Mth.sin(spin) * h;
        Vector3f a = new Vector3f(left).mul(cs).add(new Vector3f(up).mul(sn)), d = new Vector3f(left).mul(-sn).add(new Vector3f(up).mul(cs));
        int al = (int) (255 * Mth.clamp(alpha, 0, 1));
        float[][] corners = {{1, 1, 0, 0}, {1, -1, 0, 1}, {-1, -1, 1, 1}, {-1, 1, 1, 0}};
        for (float[] q : corners)
            out.addVertex(m, (float) (at.x + a.x() * q[0] + d.x() * q[1]), (float) (at.y + a.y() * q[0] + d.y() * q[1]), (float) (at.z + a.z() * q[0] + d.z() * q[1]))
                    .setColor(r, g, b, al).setUv(q[2], q[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }
}
