package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Thousand Drizzle's cuts drawn in the air (docs/combat/styles/azure-cloud-sword.md): as each cut of the flurry passes
 * through the middle, a thin, sharp line of light opens along the arc the blade's point draws in its plane (the same
 * table as scripts/animation/azure-drizzle.cjs), traced from where the blade comes from to where it goes in a blink, then
 * fading; the last cut leaves a longer one. Each client draws it for every player it sees cut: nothing goes through the
 * network. A beat after the last cut, the server has the six cuts open again on the body at once ({@link #delayedCuts}).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class DrizzleSlashEffects {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/afterimage.png");

    /**
     * A cut of the animation: when it passes the middle (s), its line's angle from level (+ rising to the right), its way
     * (+1 left to right), and how far each side of straight ahead its arc goes (degrees).
     */
    private record Cut(float t, float roll, int way, float sweep) { }

    /** As in azure-drizzle.cjs: six cuts along their own lines, then the wide last one. */
    private static final Cut[] CUTS = {
            new Cut(0.2F, -35, 1, 52), new Cut(0.32F, 0, -1, 52), new Cut(0.44F, 40, 1, 52),
            new Cut(0.56F, -80, 1, 52), new Cut(0.68F, -50, -1, 52), new Cut(0.8F, -10, 1, 52),
            new Cut(1.05F, 5, -1, 80)};
    /** How long a line of light lasts (ticks), how long it takes to open (ticks), the arc's reach and its centre's height. */
    private static final float LIFE = 6.0F, OPEN = 1.2F, REACH = 1.7F, HEIGHT = 1.2F;
    private static final int ARC_POINTS = 16;

    /** A line opened in the world: the arc it follows (from where the blade came), when, how wide. */
    private record Slash(List<Vec3> arc, long tick, float width) { }

    private static final List<Slash> SLASHES = new ArrayList<>();
    /** Each player's time in their flurry at the last tick (-1 when not in it). */
    private static final Map<UUID, Float> LAST = new HashMap<>();

    private DrizzleSlashEffects() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        long now = level.getGameTime();
        for (AbstractClientPlayer player : level.players()) {
            float t = flurryTime(player);
            float last = LAST.getOrDefault(player.getUUID(), -1.0F);
            if (t >= 0) {
                for (Cut cut : CUTS) if (last < cut.t && t >= cut.t) open(player, cut, now);
            }
            LAST.put(player.getUUID(), t);
        }
        LAST.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        SLASHES.removeIf(s -> now - s.tick > LIFE);
        BODY_CUTS.removeIf(c -> now - c.tick > LIFE);
    }

    /** How far into Thousand Drizzle's flurry this player is (seconds), or -1. */
    private static float flurryTime(AbstractClientPlayer player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        if (main == null || main.isEmpty() || main.getRealAnimation() == null || MurimAnimations.THOUSAND_DRIZZLE == null) return -1;
        var a = main.getRealAnimation();
        return a == MurimAnimations.THOUSAND_DRIZZLE || a == MurimAnimations.THOUSAND_DRIZZLE_STORM ? main.getElapsedTime() : -1;
    }

    private static void open(AbstractClientPlayer player, Cut cut, long now) {
        Vec3 forward = Vec3.directionFromRotation(0, player.yBodyRot);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double r = Math.toRadians(cut.roll);
        Vec3 along = right.scale(Math.cos(r) * cut.way).add(0, Math.sin(r) * cut.way, 0);
        // The arc the blade's point draws in its plane: from the side it came from, through straight ahead, to the other.
        Vec3 pivot = player.position().add(0, HEIGHT, 0);
        boolean last = cut.sweep > 60;
        List<Vec3> arc = new ArrayList<>();
        for (int i = 0; i <= ARC_POINTS; i++) {
            double a = Math.toRadians(cut.sweep * (2.0 * i / ARC_POINTS - 1));
            arc.add(pivot.add(forward.scale(Math.cos(a) * (last ? REACH + 0.4 : REACH))).add(along.scale(Math.sin(a) * (last ? REACH + 0.4 : REACH))));
        }
        SLASHES.add(new Slash(arc, now, last ? 0.065F : 0.04F));
    }

    /**
     * From the server: the flurry's delayed cuts open on a body, all six at once (a few hundredths of a second apart),
     * each a straight line across it at its cut's angle, a little off the middle so they cross.
     */
    public static void delayedCuts(int attackerId, int targetId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getEntity(targetId) == null) return;
        DELAYED_CUTS_SEEN.merge(targetId, 1, Integer::sum);
        long now = mc.level.getGameTime();
        var random = net.minecraft.util.RandomSource.create(targetId * 31L + now);
        for (int c = 0; c < 6; c++) {
            // A little off the middle (across the line), so they cross.
            BODY_CUTS.add(new BodyCut(targetId, CUTS[c].roll, (random.nextFloat() - 0.5F) * 0.5F, now + random.nextInt(3)));
        }
    }

    /**
     * A delayed cut on a body: which body, its line's angle (as for the flurry), how far off the middle, when it opens.
     * Drawn facing whoever looks (so it reads from every side) and following the body as it flies.
     */
    private record BodyCut(int entity, float roll, float offset, long tick) { }

    private static final List<BodyCut> BODY_CUTS = new ArrayList<>();
    /** How many times this client was told of delayed cuts on each body (entity id), for the multiplayer check. */
    private static final Map<Integer, Integer> DELAYED_CUTS_SEEN = new HashMap<>();

    /** How many times this client saw the flurry's delayed cuts open on this body. */
    public static int delayedCutsSeenOn(int entityId) {
        return DELAYED_CUTS_SEEN.getOrDefault(entityId, 0);
    }

    /** The line of a delayed cut now: across the body as the camera sees it, at its angle, through its middle or near. */
    @javax.annotation.Nullable
    private static List<Vec3> bodyLine(ClientLevel level, BodyCut cut, Vec3 camera, float partial) {
        var target = level.getEntity(cut.entity);
        if (target == null) return null;
        Vec3 centre = target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0);
        Vec3 view = centre.subtract(camera);
        if (view.lengthSqr() < 1e-4) return null;
        view = view.normalize();
        Vec3 right = view.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 up = right.cross(view).normalize();
        double r = Math.toRadians(cut.roll);
        Vec3 along = right.scale(Math.cos(r)).add(up.scale(Math.sin(r)));
        Vec3 across = right.scale(-Math.sin(r)).add(up.scale(Math.cos(r)));
        double size = Math.max(0.9, Math.max(target.getBbWidth(), target.getBbHeight()) * 0.7);
        Vec3 mid = centre.add(across.scale(cut.offset * size)).subtract(view.scale(target.getBbWidth() * 0.6));
        List<Vec3> line = new ArrayList<>();
        for (int i = 0; i <= ARC_POINTS; i++) line.add(mid.add(along.scale(size * (2.0 * i / ARC_POINTS - 1))));
        return line;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || (SLASHES.isEmpty() && BODY_CUTS.isEmpty())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float now = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType glow = CutRenderType.CUT;
        VertexConsumer out = buffers.getBuffer(glow);
        for (Slash s : SLASHES) {
            float age = now - s.tick;
            if (age < 0 || age > LIFE) continue;
            // Traced from where the blade came, then held a moment, then fading.
            float open = Mth.clamp(age / OPEN, 0, 1);
            float fade = age < 2 ? 1 : Mth.clamp(1 - (age - 2) / (LIFE - 2), 0, 1);
            // A soft azure glow around a thin white edge.
            strip(pose, out, s.arc, open, camera, s.width * 2.2F, 0.25F * fade, 0.55F * fade, 1.0F * fade);
            strip(pose, out, s.arc, open, camera, s.width, fade, fade, fade);
        }
        for (BodyCut c : BODY_CUTS) {
            float age = now - c.tick;
            if (age < 0 || age > LIFE) continue;
            List<Vec3> line = bodyLine(mc.level, c, camera, event.getPartialTick().getGameTimeDeltaPartialTick(false));
            if (line == null) continue;
            float open = Mth.clamp(age / OPEN, 0, 1);
            float fade = age < 2 ? 1 : Mth.clamp(1 - (age - 2) / (LIFE - 2), 0, 1);
            strip(pose, out, line, open, camera, 0.11F, 0.25F * fade, 0.55F * fade, 1.0F * fade);
            strip(pose, out, line, open, camera, 0.05F, fade, fade, fade);
        }
        buffers.endBatch(glow);
    }

    /**
     * Additive light drawn over everything (no depth test): a cut shows across the body it opens on, as if traced on it,
     * rather than hidden inside it.
     */
    private abstract static class CutRenderType extends RenderType {
        static final RenderType CUT = create("murimblock_cut", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY,
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                        .setShaderState(RENDERTYPE_EYES_SHADER)
                        .setTextureState(new TextureStateShard(WHITE, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));

        private CutRenderType(String name, com.mojang.blaze3d.vertex.VertexFormat format, com.mojang.blaze3d.vertex.VertexFormat.Mode mode, int size,
                              boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
            super(name, format, mode, size, affectsCrumbling, sortOnUpload, setup, clear);
        }
    }

    /** A thin line of light along an arc, up to the share `open` of it, turned to the camera (an additive light). */
    private static void strip(PoseStack pose, VertexConsumer out, List<Vec3> arc, float open, Vec3 camera, float width, float r, float g, float bl) {
        PoseStack.Pose p = pose.last();
        int n = arc.size() - 1;
        // Only the part traced so far (the arc opens from where the blade came).
        float shown = open * n;
        for (int i = 0; i < n && i < shown; i++) {
            float s0 = (float) i / n, s1 = Math.min(i + 1, shown) / n;
            Vec3 c0 = arc.get(i).subtract(camera);
            Vec3 c1 = c0.add(arc.get(i + 1).subtract(camera).subtract(c0).scale((s1 - s0) * n));
            Vec3 along = c1.subtract(c0);
            if (along.lengthSqr() < 1e-8) continue;
            // Turned to the camera about its own line; sharp at both ends, widest in the middle.
            Vec3 side = along.cross(c0.add(c1).scale(0.5)).normalize();
            float w0 = width * Mth.sin(Mth.PI * s0), w1 = width * Mth.sin(Mth.PI * s1);
            quad(p, out, c0.add(side.scale(w0)), c0.subtract(side.scale(w0)), c1.subtract(side.scale(w1)), c1.add(side.scale(w1)), r, g, bl);
        }
    }

    /** Both faces (seen from either side). */
    private static void quad(PoseStack.Pose p, VertexConsumer out, Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3, float r, float g, float b) {
        int cr = (int) (r * 255), cg = (int) (g * 255), cb = (int) (b * 255);
        for (Vec3 v : new Vec3[]{v0, v1, v2, v3, v3, v2, v1, v0}) {
            out.addVertex(p, (float) v.x, (float) v.y, (float) v.z).setColor(cr, cg, cb, 255).setUv(0.5F, 0.5F)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
        }
    }
}
