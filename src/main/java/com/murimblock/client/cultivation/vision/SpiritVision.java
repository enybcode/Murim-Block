package com.murimblock.client.cultivation.vision;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.client.cultivation.vision.VisionRules.Temper;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * What a meditator sees with closed eyes ({@code docs/cultivation/meditation-vision.md}): the world goes dark, then a
 * wave from the body reveals, by realm, blocks as blue holographic outlines and living things as stars (size from the
 * body, colour from hostility, twinkle from strength). Fast things leave only a smeared afterimage. A Mortal sees only
 * the Qi motes of {@link com.murimblock.client.cultivation.MeditationHud}.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SpiritVision {
    /** Blocks a tick an entity must move to leave an afterimage, and how long the afterimage lasts. */
    private static final double FAST = 0.45;
    private static final long TRAIL_MILLIS = 600;
    /** A star's radius is at most this share of its distance to the camera. */
    private static final float MAX_STAR_SPREAD = 0.05f;
    private static final float REVEAL_DELAY = 0.5f;
    private static final float REVEAL_SECONDS = 1.8f;
    private static final int OUTLINE = 0x3C96FF;
    private static final int OUTLINE_CORE = 0x78C8FF;
    private static final int WAVE = 0xC8F0FF;
    private static final int HALO = 0xFFD36A;

    private record Sample(double x, double y, double z, long at) {
    }

    private static final Map<Integer, ArrayDeque<Sample>> TRAILS = new HashMap<>();
    private static Map<Integer, Vec3> lastPositions = new HashMap<>();

    private SpiritVision() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ClientCultivation.get().meditating()) {
            VisionMesh.clear();
            TRAILS.clear();
            lastPositions.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        int realm = ClientCultivation.get().realm();
        VisionMesh.tick(mc.level, BlockPos.containing(mc.player.getEyePosition()),
                ClosedEyes.closed() ? VisionRules.blockReach(realm) : 0);
        sampleTrails(mc, VisionRules.starReach(realm));
    }

    /** Records where fast entities and flying projectiles were, for their afterimages. */
    private static void sampleTrails(Minecraft mc, int reach) {
        long now = Util.getMillis();
        Map<Integer, Vec3> positions = new HashMap<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (reach == 0 || !shown(entity, mc) || entity.distanceTo(mc.player) > reach) {
                continue;
            }
            Vec3 at = entity.position().add(0, entity.getBbHeight() / 2, 0);
            positions.put(entity.getId(), at);
            Vec3 before = lastPositions.get(entity.getId());
            if (before != null && before.distanceTo(at) > FAST) {
                TRAILS.computeIfAbsent(entity.getId(), id -> new ArrayDeque<>())
                        .add(new Sample(at.x, at.y, at.z, now));
            }
        }
        lastPositions = positions;
        for (Iterator<ArrayDeque<Sample>> it = TRAILS.values().iterator(); it.hasNext(); ) {
            ArrayDeque<Sample> trail = it.next();
            while (!trail.isEmpty() && now - trail.peekFirst().at() > TRAIL_MILLIS) {
                trail.pollFirst();
            }
            if (trail.isEmpty()) {
                it.remove();
            }
        }
    }

    private static boolean shown(Entity entity, Minecraft mc) {
        if (entity == mc.player || entity == mc.getCameraEntity() || entity.isSpectator()) {
            return false;
        }
        return entity instanceof LivingEntity living && living.isAlive() && !(entity instanceof ArmorStand)
                || entity instanceof Projectile;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float eyes = ClosedEyes.amount(partialTick);
        if (eyes <= 0 || mc.level == null || mc.player == null) {
            return;
        }
        Camera camera = event.getCamera();
        PoseStack.Pose pose = event.getPoseStack().last();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        veil(buffers.getBuffer(VisionRenderTypes.DARK), pose, camera, eyes * eyes * (3 - 2 * eyes));
        buffers.endBatch(VisionRenderTypes.DARK);

        float alpha = smooth(0.65f, 1, eyes);
        if (alpha <= 0) {
            return;
        }
        int realm = ClientCultivation.get().realm();
        float time = (Util.getMillis() % 3_600_000L) / 1000f;
        float reveal = smooth(0, 1, (ClosedEyes.revealSeconds(partialTick) - REVEAL_DELAY) / REVEAL_SECONDS);
        outlines(buffers, pose, camera.getPosition(), realm, alpha, reveal, time);
        stars(mc, buffers, pose, camera, realm, alpha, reveal, time, partialTick);
    }

    /** A dark quad just in front of the camera, as wide as any view. */
    private static void veil(VertexConsumer v, PoseStack.Pose pose, Camera camera, float shut) {
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        int a = Math.round(255 * Mth.clamp(shut, 0, 1));
        float[][] corners = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
        for (float[] c : corners) {
            v.addVertex(pose, look.x() * 0.3f + (left.x() * c[0] + up.x() * c[1]) * 4,
                    look.y() * 0.3f + (left.y() * c[0] + up.y() * c[1]) * 4,
                    look.z() * 0.3f + (left.z() * c[0] + up.z() * c[1]) * 4).setColor(2, 3, 6, a);
        }
    }

    private static void outlines(MultiBufferSource.BufferSource buffers, PoseStack.Pose pose, Vec3 cam, int realm,
                                 float alpha, float reveal, float time) {
        VisionMesh.Mesh mesh = VisionMesh.shown();
        int full = VisionRules.blockReach(realm);
        if (full == 0 || mesh.edgeCount() == 0) {
            return;
        }
        float reach = reveal * Math.min(full, mesh.reach());
        float ox = (float) (mesh.origin().getX() - cam.x);
        float oy = (float) (mesh.origin().getY() - cam.y);
        float oz = (float) (mesh.origin().getZ() - cam.z);
        // The outlines breathe on the 4 s loop of the seated body.
        float pulse = 0.78f + 0.22f * Mth.sin(time * Mth.PI / 2);
        boolean flickers = VisionRules.flickers(realm);
        float shimmer = flickers ? 0.75f + 0.25f * Mth.sin(time * 13) : 1;
        float sharp = VisionRules.sharpness(realm);

        float fill = VisionRules.faceFill(realm);
        if (fill > 0) {
            VertexConsumer v = buffers.getBuffer(VisionRenderTypes.GLOW);
            int a = Math.round(255 * fill * alpha * pulse);
            for (int face : mesh.faces()) {
                int x = VisionMesh.faceX(face), y = VisionMesh.faceY(face), z = VisionMesh.faceZ(face);
                Direction d = VisionMesh.faceDirection(face);
                if (Mth.length(x + d.getStepX() * 0.5f, y + d.getStepY() * 0.5f, z + d.getStepZ() * 0.5f) > reach) {
                    continue;
                }
                for (int[] c : VisionMesh.corners(x, y, z, d)) {
                    v.addVertex(pose, c[0] + ox, c[1] + oy, c[2] + oz).setColor(70, 160, 255, a);
                }
            }
            buffers.endBatch(VisionRenderTypes.GLOW);
        }
        int flick = (int) (time * 9);
        lines(buffers, VisionRenderTypes.HALO_LINES, pose, mesh, ox, oy, oz, full, reach, reveal, flickers, flick,
                OUTLINE, alpha * pulse * shimmer * (0.22f - 0.1f * sharp));
        lines(buffers, VisionRenderTypes.CORE_LINES, pose, mesh, ox, oy, oz, full, reach, reveal, flickers, flick,
                OUTLINE_CORE, alpha * pulse * shimmer * (0.55f + 0.35f * sharp));
    }

    private static void lines(MultiBufferSource.BufferSource buffers, RenderType type, PoseStack.Pose pose,
                              VisionMesh.Mesh mesh, float ox, float oy, float oz, int full, float reach, float reveal,
                              boolean flickers, int flick, int rgb, float strength) {
        VertexConsumer v = buffers.getBuffer(type);
        float[] e = mesh.edges();
        float[] distances = mesh.distances();
        for (int i = 0; i < distances.length; i++) {
            float d = distances[i];
            if (d > reach) {
                break;
            }
            if (flickers && hash(i, flick) < 0.16f) {
                continue;
            }
            // Brighter near the body, fading out toward the reach; the edge of the reveal wave shines white.
            boolean front = reveal < 1 && reach - d < 1.2f;
            float k = front ? 0.9f : (float) Math.pow(Math.max(0, 1 - d / full), 0.7) * strength;
            int color = front ? WAVE : rgb;
            int a = Math.round(255 * Mth.clamp(k, 0, 1));
            if (a == 0) {
                continue;
            }
            int o = i * 6;
            float nx = e[o + 3] - e[o], ny = e[o + 4] - e[o + 1], nz = e[o + 5] - e[o + 2];
            v.addVertex(pose, e[o] + ox, e[o + 1] + oy, e[o + 2] + oz)
                    .setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, a).setNormal(pose, nx, ny, nz);
            v.addVertex(pose, e[o + 3] + ox, e[o + 4] + oy, e[o + 5] + oz)
                    .setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, a).setNormal(pose, nx, ny, nz);
        }
        buffers.endBatch(type);
    }

    private static void stars(Minecraft mc, MultiBufferSource.BufferSource buffers, PoseStack.Pose pose, Camera camera,
                              int realm, float alpha, float reveal, float time, float partialTick) {
        int full = VisionRules.starReach(realm);
        if (full == 0) {
            return;
        }
        Vec3 cam = camera.getPosition();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        boolean halos = VisionRules.halos(realm);
        Vec3 eye = mc.player.getEyePosition(partialTick);
        VertexConsumer v = buffers.getBuffer(VisionRenderTypes.GLOW);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!shown(entity, mc)) {
                continue;
            }
            Vec3 at = entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2, 0);
            double dist = at.distanceTo(eye);
            if (dist > reveal * full) {
                continue;
            }
            float fade = 1 - smooth(full * 0.85f, full, (float) dist);
            int rgb = temper(entity).rgb();
            // A star close to the camera (a mob beside you in F5) is kept small enough not to blind the view.
            float radius = Math.min(Math.max(VisionRules.starRadius(entity.getBbWidth(), entity.getBbHeight()),
                    (float) dist * 0.008f), (float) at.distanceTo(cam) * MAX_STAR_SPREAD);
            ArrayDeque<Sample> trail = TRAILS.get(entity.getId());
            long now = Util.getMillis();
            if (trail != null) {
                int n = 0;
                for (Sample s : trail) {
                    float age = (now - s.at()) / (float) TRAIL_MILLIS;
                    if (n++ % 2 == 0 && age < 1) {
                        star(v, pose, left, up, (float) (s.x() - cam.x), (float) (s.y() - cam.y),
                                (float) (s.z() - cam.z), radius * 0.8f, rgb, 0.3f * (1 - age) * alpha * fade, 0, false);
                    }
                }
            }
            if (entity instanceof Projectile) {
                continue;
            }
            float strength = entity instanceof LivingEntity living ? VisionRules.strength(living.getMaxHealth(),
                    living.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                            ? (float) living.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0) : 0;
            float twinkle = 0.62f + 0.38f * Mth.sin(time * Mth.TWO_PI * VisionRules.twinkleHz(strength)
                    + entity.getId() * 1.7f);
            // Moving fast, the body itself is only a blur.
            float bright = twinkle * alpha * fade * (trail != null ? 0.4f : 1);
            star(v, pose, left, up, (float) (at.x - cam.x), (float) (at.y - cam.y), (float) (at.z - cam.z), radius,
                    rgb, bright, time * 0.2f + entity.getId(), halos && entity instanceof Player);
        }
        buffers.endBatch(VisionRenderTypes.GLOW);
    }

    private static Temper temper(Entity entity) {
        if (entity instanceof Player) {
            return Temper.CULTIVATOR;
        }
        if (entity instanceof Mob mob && mob.isAggressive() || entity instanceof Projectile) {
            return entity instanceof Projectile ? Temper.HOSTILE : Temper.TARGETING;
        }
        if (entity instanceof Enemy) {
            return Temper.HOSTILE;
        }
        return entity instanceof NeutralMob ? Temper.NEUTRAL : Temper.PASSIVE;
    }

    /** A star facing the camera: a soft glow, a pale core, four spikes, and a gold halo for a cultivator. */
    private static void star(VertexConsumer v, PoseStack.Pose pose, Vector3f left, Vector3f up, float x, float y,
                             float z, float r, int rgb, float bright, float spin, boolean halo) {
        int a = Math.round(255 * Mth.clamp(bright, 0, 1));
        if (a == 0) {
            return;
        }
        int red = rgb >> 16 & 0xFF, green = rgb >> 8 & 0xFF, blue = rgb & 0xFF;
        disc(v, pose, left, up, x, y, z, r * 2.6f, red, green, blue, Math.round(a * 0.85f));
        disc(v, pose, left, up, x, y, z, r * 0.5f, (red + 2 * 255) / 3, (green + 2 * 255) / 3, (blue + 2 * 255) / 3, a);
        float length = r * (2.2f + 1.4f * bright);
        for (int i = 0; i < 4; i++) {
            float angle = spin + i * Mth.HALF_PI;
            float cos = Mth.cos(angle), sin = Mth.sin(angle);
            float wide = r * 0.12f;
            vertex(v, pose, left, up, x, y, z, -sin * wide, cos * wide, red, green, blue, Math.round(a * 0.7f));
            vertex(v, pose, left, up, x, y, z, sin * wide, -cos * wide, red, green, blue, Math.round(a * 0.7f));
            vertex(v, pose, left, up, x, y, z, cos * length, sin * length, red, green, blue, 0);
            vertex(v, pose, left, up, x, y, z, cos * length, sin * length, red, green, blue, 0);
        }
        if (halo) {
            int ha = Math.round(a * 0.55f);
            for (int i = 0; i < 16; i++) {
                float a0 = i * Mth.TWO_PI / 16, a1 = (i + 1) * Mth.TWO_PI / 16;
                float in = r * 1.9f, out = r * 2.2f;
                vertex(v, pose, left, up, x, y, z, Mth.cos(a0) * in, Mth.sin(a0) * in, 0xFF, 0xD3, 0x6A, ha);
                vertex(v, pose, left, up, x, y, z, Mth.cos(a0) * out, Mth.sin(a0) * out, 0xFF, 0xD3, 0x6A, ha);
                vertex(v, pose, left, up, x, y, z, Mth.cos(a1) * out, Mth.sin(a1) * out, 0xFF, 0xD3, 0x6A, ha);
                vertex(v, pose, left, up, x, y, z, Mth.cos(a1) * in, Mth.sin(a1) * in, 0xFF, 0xD3, 0x6A, ha);
            }
        }
    }

    /** A disc bright at the centre, clear at the rim, as quads folded into triangles. */
    private static void disc(VertexConsumer v, PoseStack.Pose pose, Vector3f left, Vector3f up, float x, float y,
                             float z, float r, int red, int green, int blue, int a) {
        for (int i = 0; i < 12; i++) {
            float a0 = i * Mth.TWO_PI / 12, a1 = (i + 1) * Mth.TWO_PI / 12;
            vertex(v, pose, left, up, x, y, z, 0, 0, red, green, blue, a);
            vertex(v, pose, left, up, x, y, z, 0, 0, red, green, blue, a);
            vertex(v, pose, left, up, x, y, z, Mth.cos(a0) * r, Mth.sin(a0) * r, red, green, blue, 0);
            vertex(v, pose, left, up, x, y, z, Mth.cos(a1) * r, Mth.sin(a1) * r, red, green, blue, 0);
        }
    }

    private static void vertex(VertexConsumer v, PoseStack.Pose pose, Vector3f left, Vector3f up, float x, float y,
                               float z, float across, float upward, int red, int green, int blue, int a) {
        v.addVertex(pose, x + left.x() * across + up.x() * upward, y + left.y() * across + up.y() * upward,
                z + left.z() * across + up.z() * upward).setColor(red, green, blue, a);
    }

    private static float smooth(float from, float to, float value) {
        float k = Mth.clamp((value - from) / (to - from), 0, 1);
        return k * k * (3 - 2 * k);
    }

    private static float hash(int i, int k) {
        int h = i * 374761393 + k * 668265263;
        h = (h ^ h >>> 13) * 1274126177;
        return ((h ^ h >>> 16) >>> 8) / (float) (1 << 24);
    }
}
