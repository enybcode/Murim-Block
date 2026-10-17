package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.LightProvider;
import com.lowdragmc.photon.client.light.LightSink;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.martial.GoldenBell;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * The Golden Bell on the client (docs/combat/styles/golden-bell.md):
 * <ul>
 *   <li><b>the body's matter</b>: the player's own skin, its skin-coloured pixels only (hair and clothes stay theirs),
 *   recoloured gold, iron or stone, keeping its light and shade; the recoloured skin replaces theirs
 *   (client/mixin/PlayerSkinMixin), so Epic Fight's model wears it too. Made once a skin and a matter, from the skin
 *   texture itself (read back from the screen's memory);</li>
 *   <li><b>the bell</b>: a golden bell of light coming down over the player as the form begins, and pulsing out when it
 *   rings (the form ends, Bell Toll, Golden Mountain); sparks of iron or gold when a blow is sent back;</li>
 *   <li><b>the light</b>: a soft golden Photon light on the player while in the form, a flash at each ring.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GoldenBellSkins {
    private static final ResourceLocation BELL = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/fx/golden_bell/bell.png");
    /** What each player's body is made of now, and whether they hold the form. */
    private static final Map<Integer, Integer> MATTER = new HashMap<>();
    private static final Map<Integer, Boolean> FORM = new HashMap<>();
    /** The recoloured skins made: by the skin's texture and the matter. */
    private static final Map<String, ResourceLocation> MADE = new HashMap<>();
    /** Bells shown: whose, when, and whether coming down (the form begins) or ringing out. */
    private record Bell(int entityId, float start, boolean descend) { }
    private static final List<Bell> BELLS = new ArrayList<>();
    private static final LightProvider LIGHTS = GoldenBellSkins::submitLights;
    private static final float[] GOLD = {1.0F, 0.78F, 0.32F};

    private GoldenBellSkins() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PhotonLights.addProvider(LIGHTS));
    }

    public static void receive(int entityId, int skin, boolean form) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(entityId);
        float now = level.getGameTime();
        if (skin == -1) {
            BELLS.add(new Bell(entityId, now, false));
            return;
        }
        if (skin == -2) {
            if (e != null) sparks(level, e, FORM.getOrDefault(entityId, false));
            return;
        }
        boolean was = FORM.getOrDefault(entityId, false);
        if (form && !was) BELLS.add(new Bell(entityId, now, true));
        if (skin == GoldenBell.SKIN_NONE) MATTER.remove(entityId); else MATTER.put(entityId, skin);
        if (form) FORM.put(entityId, true); else FORM.remove(entityId);
    }

    private static void sparks(ClientLevel level, Entity e, boolean gold) {
        RandomSource r = level.random;
        Vec3 c = e.position().add(e.getLookAngle().multiply(1, 0, 1).normalize().scale(0.6)).add(0, 1.3, 0);
        for (int i = 0; i < 14; i++)
            level.addParticle(gold ? net.minecraft.core.particles.ParticleTypes.WAX_OFF : net.minecraft.core.particles.ParticleTypes.CRIT,
                    c.x, c.y, c.z, (r.nextDouble() - 0.5) * 0.6, r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 0.6);
    }

    // ---------------------------------------------------------------- the skin

    /** When a skin could not be recoloured yet (not loaded): not tried again before a second has passed. */
    private static final Map<String, Long> WAITING = new HashMap<>();
    private static int made;

    /**
     * The skin this player shows: theirs, or theirs recoloured to their body's matter. Only for the player in the world
     * with that id: a preview of a player (another mod's wardrobe, emotes) keeps its own skin.
     */
    public static PlayerSkin skinFor(AbstractClientPlayer player, PlayerSkin own) {
        Integer matter = MATTER.get(player.getId());
        if (matter == null || matter == GoldenBell.SKIN_NONE || own == null) return own;
        if (player.level() == null || player.level().getEntity(player.getId()) != player) return own;
        ResourceLocation made = made(own.texture(), matter);
        return made == null ? own : new PlayerSkin(made, own.textureUrl(), own.capeTexture(), own.elytraTexture(), own.model(), own.secure());
    }

    private static ResourceLocation made(ResourceLocation skin, int matter) {
        String key = skin + "#" + matter;
        ResourceLocation done = MADE.get(key);
        if (done != null) return done;
        if (!RenderSystem.isOnRenderThread()) return null;
        long now = net.minecraft.Util.getMillis();
        Long waited = WAITING.get(key);
        if (waited != null && now - waited < 1000) return null;
        WAITING.put(key, now);
        Minecraft mc = Minecraft.getInstance();
        // Never register a texture under the skin's name: if it is not there yet (a skin still downloading, another
        // mod's skin made later), wait for it.
        AbstractTexture missing = net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getTexture();
        AbstractTexture tex = mc.getTextureManager().getTexture(skin, missing);
        if (tex == null || tex == missing) return null;
        NativeImage img = null;
        try {
            RenderSystem.bindTexture(tex.getId());
            int w = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH);
            int h = org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_HEIGHT);
            if (w <= 0 || h <= 0 || w > 1024 || h > 1024) return null;
            img = new NativeImage(w, h, false);
            img.downloadTexture(0, false);
            recolour(img, matter);
            ResourceLocation out = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "golden_bell_skin/" + (made++));
            mc.getTextureManager().register(out, new DynamicTexture(img));
            img = null;
            MADE.put(key, out);
            WAITING.remove(key);
            return out;
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().warn("Golden Bell: could not recolour the skin {}", skin, e);
            return null;
        } finally {
            if (img != null) img.close();
        }
    }

    /** Leaving a world: the recoloured skins are let go, and what was known of the players there. */
    @SubscribeEvent
    public static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation texture : MADE.values()) mc.getTextureManager().release(texture);
        MADE.clear();
        WAITING.clear();
        MATTER.clear();
        FORM.clear();
        BELLS.clear();
    }

    /** Whether a colour (0..255) is skin: warm, a little saturated, not too dark (hair and leather mostly are). */
    static boolean isSkin(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (max < 70 || r < g || g < b * 0.75) return false;
        float sat = (max - min) / (float) max;
        if (sat < 0.12F || sat > 0.7F) return false;
        float hue = (float) (Mth.atan2(Math.sqrt(3) * (g - b), 2.0 * r - g - b) * Mth.RAD_TO_DEG);
        return hue >= 4 && hue <= 46;
    }

    /** The skin's skin-coloured pixels in gold, iron or stone, their light kept. */
    static void recolour(NativeImage img, int matter) {
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
            int abgr = img.getPixelRGBA(x, y);
            int a = (abgr >>> 24) & 0xFF, b = (abgr >>> 16) & 0xFF, g = (abgr >>> 8) & 0xFF, r = abgr & 0xFF;
            if (a == 0 || !isSkin(r, g, b)) continue;
            float l = (0.3F * r + 0.59F * g + 0.11F * b) / 255F;
            int[] c = switch (matter) {
                case GoldenBell.SKIN_GOLD -> new int[] {(int) (255 * Math.min(1, l * 1.25F + 0.1F)), (int) (200 * Math.min(1, l * 1.2F)), (int) (70 * l + 20 * l * l)};
                case GoldenBell.SKIN_IRON -> new int[] {(int) (150 * l + 40), (int) (156 * l + 40), (int) (168 * l + 44)};
                default -> {
                    int vein = ((x * 7 + y * 13) % 11 == 0) ? -26 : 0;
                    yield new int[] {(int) (128 * l + 46 + vein), (int) (122 * l + 44 + vein), (int) (112 * l + 40 + vein)};
                }
            };
            // A bright edge here and there: the polish of gold, the edge of iron.
            if (matter != GoldenBell.SKIN_NONE && matter != 3 && (x + y * 3) % 9 == 0) for (int k = 0; k < 3; k++) c[k] = Math.min(255, c[k] + 34);
            img.setPixelRGBA(x, y, (a << 24) | (Mth.clamp(c[2], 0, 255) << 16) | (Mth.clamp(c[1], 0, 255) << 8) | Mth.clamp(c[0], 0, 255));
        }
    }

    // ---------------------------------------------------------------- the bell and the light

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) { MATTER.clear(); FORM.clear(); BELLS.clear(); return; }
        float now = level.getGameTime();
        BELLS.removeIf(b -> now - b.start > (b.descend ? 26 : 14));
        MATTER.keySet().removeIf(id -> level.getEntity(id) == null);
        FORM.keySet().removeIf(id -> level.getEntity(id) == null);
    }

    private static void submitLights(LightSink sink, float partial) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float now = level.getGameTime() + partial;
        for (int id : FORM.keySet()) {
            Entity e = level.getEntity(id);
            if (e != null) light(sink, e.getPosition(partial).add(0, 1.1, 0), 0.9F + 0.15F * Mth.sin(now * 0.15F), 5);
        }
        for (Bell b : BELLS) {
            Entity e = level.getEntity(b.entityId);
            if (e == null) continue;
            float k = (now - b.start) / (b.descend ? 26 : 14);
            light(sink, e.getPosition(partial).add(0, 1.2, 0), (b.descend ? 1.6F : 3.0F) * (1 - k), 8);
        }
    }

    private static void light(LightSink sink, Vec3 at, float intensity, float range) {
        if (intensity <= 0.01F) return;
        DynamicLight l = sink.next();
        if (l != null) l.reset().point().at(at.x, at.y, at.z).color(GOLD[0], GOLD[1], GOLD[2]).intensity(intensity).range(range).sourceRadius(0.3F);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BELLS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = level.getGameTime() + partial;
        Vec3 eye = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.eyes(BELL);
        VertexConsumer out = buffers.getBuffer(type);
        Matrix4f m = pose.last().pose();
        for (Bell b : BELLS) {
            Entity e = level.getEntity(b.entityId);
            if (e == null) continue;
            float t = now - b.start;
            Vec3 feet = e.getPosition(partial);
            float drop, scale, bright;
            if (b.descend) {
                // Down from above in half a second, a moment held, then melting into the body.
                float k = Mth.clamp(t / 10, 0, 1);
                drop = 3.2F * (1 - k * k * (3 - 2 * k));
                scale = 1;
                bright = t < 16 ? 0.85F : 0.85F * (1 - (t - 16) / 10);
            } else {
                // The ring: the bell swells out and fades.
                float k = t / 14;
                drop = 0;
                scale = 1 + 0.9F * k;
                bright = 0.8F * (1 - k);
            }
            bell(out, m, feet.subtract(eye).add(0, drop, 0), scale, Mth.clamp(bright, 0, 1), now * 0.02F);
        }
        buffers.endBatch(type);
    }

    /** The bell: a lathe of its profile (a dome, the waist, the flared lip), 16 sides, its texture's bands round it. */
    private static void bell(VertexConsumer out, Matrix4f m, Vec3 at, float scale, float bright, float spin) {
        // Profile from the top down: (radius, height) in blocks.
        float[][] p = {{0.0F, 2.75F}, {0.32F, 2.7F}, {0.55F, 2.5F}, {0.66F, 2.1F}, {0.7F, 1.5F}, {0.78F, 0.9F}, {0.95F, 0.35F}, {1.12F, 0.05F}};
        int sides = 16, lum = (int) (255 * bright);
        for (int i = 0; i < sides; i++) {
            float a0 = spin + i * Mth.TWO_PI / sides, a1 = spin + (i + 1) * Mth.TWO_PI / sides;
            for (int k = 0; k + 1 < p.length; k++) {
                float v0 = (float) k / (p.length - 1), v1 = (float) (k + 1) / (p.length - 1), u0 = (float) i / sides, u1 = (float) (i + 1) / sides;
                vtx(out, m, at, p[k][0] * scale, p[k][1] * scale, a0, u0, v0, lum);
                vtx(out, m, at, p[k + 1][0] * scale, p[k + 1][1] * scale, a0, u0, v1, lum);
                vtx(out, m, at, p[k + 1][0] * scale, p[k + 1][1] * scale, a1, u1, v1, lum);
                vtx(out, m, at, p[k][0] * scale, p[k][1] * scale, a1, u1, v0, lum);
            }
        }
    }

    private static void vtx(VertexConsumer out, Matrix4f m, Vec3 at, float r, float y, float a, float u, float v, int lum) {
        out.addVertex(m, (float) (at.x + Mth.cos(a) * r), (float) (at.y + y), (float) (at.z + Mth.sin(a) * r)).setColor(lum, lum, lum, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    /** For tours: what this client shows a player's body made of (0: their own). */
    public static int matter(int entityId) {
        return MATTER.getOrDefault(entityId, 0);
    }
}
