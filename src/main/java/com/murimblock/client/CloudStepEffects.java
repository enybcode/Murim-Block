package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * What the art's fast moves look like besides their animation (docs/combat/styles/azure-cloud-sword.md): Cloud Step
 * Slash's dash and Heaven-Splitting Crescent's rise. Pale azure afterimages of the player left along the way, fading;
 * thin speed lines trailing behind (below, going up); a puff of dust where it leaves; and, for the one who moves, a
 * short push of the field of view (in first person, speed lines on the screen). Each client draws it for every player
 * it sees move (it reads their Epic Fight animation): nothing goes through the network.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CloudStepEffects {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/afterimage.png");
    /**
     * A fast move of the art: its animation, the stretch of it where the body flies (seconds), and whether it goes up
     * (else forward). Cloud Step Slash's dash, from the launch to the stop; Heaven-Splitting Crescent's rise, from the
     * leap (AzureCloudSword: 4 ticks in) to near the top.
     */
    private record Fast(java.util.function.Supplier<yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<?>> animation,
                        float from, float to, boolean up) {
        float length() {
            return to - from;
        }
    }

    private static final List<Fast> FAST = List.of(
            new Fast(() -> MurimAnimations.CLOUD_STEP_SLASH, 0.02F, 0.22F, false),
            new Fast(() -> MurimAnimations.HEAVEN_SPLITTING_CRESCENT, 0.2F, 0.82F, true));

    /** A player in a fast move, `t` seconds into its flying stretch (negative before it). */
    private record InMove(Fast move, float t) {
        boolean flying() {
            return t >= 0 && t <= move.length();
        }
    }
    /** How long an afterimage takes to fade (ticks), and how bright it starts. */
    private static final float FADE_TICKS = 7.0F, BRIGHT = 0.55F;
    private static final DustParticleOptions AZURE = new DustParticleOptions(new Vector3f(0.62F, 0.85F, 1.0F), 0.9F);

    /** An afterimage: where the player was, and when (client game time). */
    private record Ghost(Vec3 at, long tick) { }

    private static final Map<UUID, Deque<Ghost>> GHOSTS = new HashMap<>();
    /** The players seen dashing last tick (to puff the dust once, as they leave). */
    private static final Map<UUID, Boolean> DASHING = new HashMap<>();
    /** True while afterimages are drawn: their name tags are not. */
    private static boolean drawingGhosts;

    private CloudStepEffects() {
    }

    /** Whether this client draws afterimages of this player now (for the multiplayer check). */
    public static boolean hasAfterimages(UUID player) {
        Deque<Ghost> ghosts = GHOSTS.get(player);
        return ghosts != null && !ghosts.isEmpty();
    }

    /** The fast move this player is in, if any. */
    @javax.annotation.Nullable
    static InMove inMove(Player player) {
        PlayerPatch<?> patch = EpicFightBridge.patch(player);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        if (main == null || main.isEmpty() || main.getRealAnimation() == null) return null;
        for (Fast f : FAST) if (f.animation.get() != null && main.getRealAnimation() == f.animation.get()) return new InMove(f, main.getElapsedTime() - f.from);
        return null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) return;
        long now = level.getGameTime();
        for (AbstractClientPlayer player : level.players()) {
            InMove move = inMove(player);
            boolean dashing = move != null && move.flying();
            boolean was = DASHING.getOrDefault(player.getUUID(), false);
            if (dashing) {
                GHOSTS.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).addLast(new Ghost(player.position(), now));
                wind(level, player, !was, move.move.up);
            }
            DASHING.put(player.getUUID(), dashing);
        }
        for (Iterator<Map.Entry<UUID, Deque<Ghost>>> it = GHOSTS.entrySet().iterator(); it.hasNext(); ) {
            Deque<Ghost> ghosts = it.next().getValue();
            while (!ghosts.isEmpty() && now - ghosts.peekFirst().tick > FADE_TICKS) ghosts.removeFirst();
            if (ghosts.isEmpty()) it.remove();
        }
        DASHING.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        STREAKS.removeIf(s -> now - s.tick > STREAK_TICKS);
    }

    /** Wind pulled behind the body (below it, going up), and at the start a puff of dust at the feet. */
    private static void wind(ClientLevel level, Player player, boolean leaving, boolean up) {
        Vec3 forward = up ? Vec3.ZERO : Vec3.directionFromRotation(0, player.getYRot());
        Vec3 at = player.position();
        var random = level.getRandom();
        if (leaving) {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                level.addParticle(ParticleTypes.POOF, at.x + Math.cos(a) * 0.4, at.y + 0.1, at.z + Math.sin(a) * 0.4,
                        Math.cos(a) * 0.08 - forward.x * 0.1, 0.02, Math.sin(a) * 0.08 - forward.z * 0.1);
            }
        }
        // Speed lines: thin streaks of light around the body, trailing back along the way (not for one's own eyes: they
        // would cross the camera; the screen has its own, onRenderGui).
        boolean ownEyes = player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        for (int i = 0; i < (ownEyes ? 0 : 5); i++) {
            if (up) {
                // All around the body, trailing down.
                double a = random.nextDouble() * Math.PI * 2, r = 0.45 + random.nextDouble() * 0.5;
                Vec3 from = at.add(Math.cos(a) * r, random.nextDouble() * 2.0, Math.sin(a) * r);
                STREAKS.add(new Streak(from, from.subtract(0, 1.2 + random.nextDouble() * 1.4, 0), level.getGameTime()));
                continue;
            }
            double side = (random.nextDouble() - 0.5) * 1.6, height = 0.15 + random.nextDouble() * 1.7;
            Vec3 from = at.add(-forward.z * side, height, forward.x * side).add(forward.scale(random.nextDouble() * 0.8));
            STREAKS.add(new Streak(from, from.subtract(forward.scale(1.4 + random.nextDouble() * 1.6)), level.getGameTime()));
        }
        for (int i = 0; i < (ownEyes ? 0 : 2); i++) {
            Vec3 p = at.add((random.nextDouble() - 0.5) * 0.8, 0.4 + random.nextDouble() * 1.2, (random.nextDouble() - 0.5) * 0.8);
            level.addParticle(AZURE, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    /** A speed line, from its front end to its tail, and when it was drawn (client game time). */
    private record Streak(Vec3 from, Vec3 to, long tick) { }

    private static final List<Streak> STREAKS = new ArrayList<>();
    /** How long a speed line lasts (ticks), and its width (blocks). */
    private static final float STREAK_TICKS = 4.0F, STREAK_WIDTH = 0.035F;

    /** The speed lines, each a thin strip turned to the camera, fading. */
    private static void drawStreaks(PoseStack pose, VertexConsumer out, Vec3 camera, float now) {
        PoseStack.Pose p = pose.last();
        for (Streak s : STREAKS) {
            float fade = 1 - (now - s.tick) / STREAK_TICKS;
            if (fade <= 0) continue;
            Vec3 a = s.from.subtract(camera), b = s.to.subtract(camera);
            Vec3 side = b.subtract(a).cross(a.add(b).scale(0.5)).normalize().scale(STREAK_WIDTH);
            int c = (int) (255 * 0.8F * fade), cb = (int) (255 * fade);
            // Bright at the front, nothing at the tail.
            streakVertex(p, out, a.add(side), c, c, cb);
            streakVertex(p, out, a.subtract(side), c, c, cb);
            streakVertex(p, out, b.subtract(side), 0, 0, 0);
            streakVertex(p, out, b.add(side), 0, 0, 0);
            // And its back (seen from either side).
            streakVertex(p, out, b.add(side), 0, 0, 0);
            streakVertex(p, out, b.subtract(side), 0, 0, 0);
            streakVertex(p, out, a.subtract(side), c, c, cb);
            streakVertex(p, out, a.add(side), c, c, cb);
        }
    }

    private static void streakVertex(PoseStack.Pose p, VertexConsumer out, Vec3 v, int r, int g, int b) {
        out.addVertex(p, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, 255).setUv(0.5F, 0.5F)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
    }

    /** The afterimages: the player's own model, as it is now, at each place it passed, in fading azure light. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || (GHOSTS.isEmpty() && STREAKS.isEmpty())) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType glow = RenderType.eyes(WHITE);
        drawingGhosts = true;
        try {
            for (Map.Entry<UUID, Deque<Ghost>> e : GHOSTS.entrySet()) {
                Player player = level.getPlayerByUUID(e.getKey());
                if (player == null || (player == mc.player && mc.options.getCameraType().isFirstPerson())) continue;
                @SuppressWarnings("unchecked")
                EntityRenderer<Player> renderer = (EntityRenderer<Player>) mc.getEntityRenderDispatcher().getRenderer(player);
                List<Ghost> ghosts = new ArrayList<>(e.getValue());
                Vec3 here = player.getPosition(partial);
                for (Ghost g : ghosts) {
                    float fade = 1 - (now - g.tick) / FADE_TICKS;
                    // Not where the player stands now (it would only brighten them).
                    if (fade <= 0 || g.at.distanceToSqr(here) < 0.36) continue;
                    float b = BRIGHT * fade;
                    MultiBufferSource tinted = type -> new Tinted(buffers.getBuffer(glow), (int) (90 * b), (int) (170 * b), (int) (255 * b));
                    pose.pushPose();
                    pose.translate(g.at.x - camera.x, g.at.y - camera.y, g.at.z - camera.z);
                    renderer.render(player, Mth.lerp(partial, player.yRotO, player.getYRot()), partial, pose, tinted, LightTexture.FULL_BRIGHT);
                    pose.popPose();
                }
            }
        } finally {
            drawingGhosts = false;
        }
        drawStreaks(pose, buffers.getBuffer(glow), camera, now);
        buffers.endBatch(glow);
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (drawingGhosts) event.setCanRender(TriState.FALSE);
    }

    /** Seen from the eyes: how many speed lines rush in from the edges of the screen. */
    private static final int SCREEN_LINES = 30;

    /**
     * For the one who dashes in first person (the afterimages and the lines around the body are behind the camera):
     * speed lines rushing in from the edges of the screen, as in a drawn fight, strong as the body leaves, gone as it
     * stops. They move every tick.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson() || mc.options.hideGui) return;
        InMove move = inMove(mc.player);
        if (move == null) return;
        float t = move.t, length = move.move.length();
        if (t < 0 || t > length + 0.12F) return;
        // Up in a few hundredths of a second, down over the rest of the flight and the stop.
        float strength = t < 0.04F ? t / 0.04F : Mth.clamp(1 - (t - 0.04F) / (length + 0.08F), 0, 1);
        if (strength <= 0) return;
        GuiGraphics g = event.getGuiGraphics();
        float cx = g.guiWidth() / 2.0F, cy = g.guiHeight() / 2.0F, reach = (float) Math.hypot(cx, cy);
        var random = net.minecraft.util.RandomSource.create(mc.level.getGameTime() * 31L);
        for (int i = 0; i < SCREEN_LINES; i++) {
            float angle = (i + random.nextFloat()) * 360.0F / SCREEN_LINES;
            float inner = reach * (0.55F + random.nextFloat() * 0.25F + 0.15F * (1 - strength));
            float width = 0.5F + random.nextFloat() * 1.5F;
            int alpha = (int) (strength * (90 + random.nextInt(80)));
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
            // Bright toward the middle, a touch of azure at the edge.
            g.fillGradient((int) inner, (int) -width, (int) reach + 4, (int) Math.ceil(width), (alpha << 24) | 0xFFFFFF, ((alpha / 2) << 24) | 0xBFE0FF);
            g.pose().popPose();
        }
    }

    /** In first person, a body the dash goes through is not drawn while the eye is inside it (only its inside would show). */
    // First, before Epic Fight draws the body its own way.
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onRenderLiving(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() == mc.player || !mc.options.getCameraType().isFirstPerson() || inMove(mc.player) == null) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        if (event.getEntity().getBoundingBox().inflate(0.3).contains(eye)) event.setCanceled(true);
    }

    /** For the one who dashes: the view pushed out a little as they leave, back as they stop. */
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (event.getPlayer() != Minecraft.getInstance().player) return;
        InMove move = inMove(event.getPlayer());
        if (move == null) return;
        float span = move.move.length() + 0.18F;
        if (move.t < 0 || move.t > span) return;
        float push = Mth.sin(Mth.PI * Mth.clamp(move.t / span, 0, 1));
        event.setNewFovModifier(event.getNewFovModifier() * (1 + 0.12F * push));
    }

    /** Everything drawn in one flat colour (the white texture's middle), lit full. */
    private record Tinted(VertexConsumer out, int r, int g, int b) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            out.addVertex(x, y, z);
            return this;
        }

        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            out.setColor(r, g, b, 255);
            return this;
        }

        @Override public VertexConsumer setUv(float u, float v) {
            out.setUv(0.5F, 0.5F);
            return this;
        }

        @Override public VertexConsumer setUv1(int u, int v) {
            out.setUv1(u, v);
            return this;
        }

        @Override public VertexConsumer setUv2(int u, int v) {
            out.setUv2(240, 240);
            return this;
        }

        @Override public VertexConsumer setNormal(float x, float y, float z) {
            out.setNormal(x, y, z);
            return this;
        }
    }
}
