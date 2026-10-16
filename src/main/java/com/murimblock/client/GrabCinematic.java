package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.CrabKingAnimations;
import com.murimblock.mob.CrabKingEntity;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The Crab King's grab as the player caught in it lives it (Enzo, 16/10: a first-person film, the camera moving to
 * raise the tension). It starts by itself when this player plays the victim's animation (crab_king_victim, sent by the
 * server with the hold), and keeps to its clock: no message of its own. The camera stays at the player's head, just
 * behind it, so the body, the jaws round it and the king are seen:
 * <ol>
 *   <li>lifted (0 to 0.5 s): looking down at the feet, the ground falling away and the jaws, then up to his face;</li>
 *   <li>drawn close (to 0.85 s): a slow push in on his face, the view narrowing, heartbeats; then down at the jaws
 *   closing on the sides, just before they squeeze;</li>
 *   <li>each squeeze (CrabKingEntity.SQUEEZES): a hard shake, the view punched in, red at the edges; between them the
 *   camera breathes, from his face to the jaws;</li>
 *   <li>wound back (from 1.85 s): wide, tilting further and further;</li>
 *   <li>thrown (crab_king_thrown): behind the body as it flies, spinning, the king left behind; the landing shakes it,
 *   and the player gets the view back.</li>
 * </ol>
 * Purely visual: the server holds, squeezes and throws; the player can do nothing meanwhile (CrabKingEntity.isHeld).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GrabCinematic {
    private enum Phase { HELD, FLYING, LANDED }

    /** After the landing, how long the shaken view stays before the player gets it back (seconds). */
    private static final float SETTLE = 0.7F;
    @Nullable private static Marker camera;
    private static Phase phase = Phase.HELD;
    /** Ticks flying, then ticks since the landing. */
    private static int phaseTicks;
    private static float shake, roll, fov = 70, red;
    private static int squeezesHeard;
    private static boolean heartbeat1, heartbeat2;
    private static Vec3 lastKing = Vec3.ZERO;
    /** The camera's place and aim, eased from frame to frame: the held body is moved by the server twenty times a second. */
    @Nullable private static Vec3 smoothEye, smoothLook;
    @Nullable private static CrabKingEntity king;

    private GrabCinematic() { }

    public static boolean active() {
        return camera != null;
    }

    /** Which of the victim's animations this player plays now (null: neither), and how far in (seconds). */
    @Nullable private static Object[] playing(Player player, float partial) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
        if (patch == null) return null;
        var main = patch.getAnimator().getPlayerFor(null);
        if (main == null) return null;
        var animation = main.getRealAnimation();
        if (animation != CrabKingAnimations.VICTIM && animation != CrabKingAnimations.THROWN) return null;
        float t = main.getPrevElapsedTime() + (main.getElapsedTime() - main.getPrevElapsedTime()) * partial;
        return new Object[] {animation, t};
    }

    @Nullable private static CrabKingEntity nearestKing(Player player) {
        CrabKingEntity best = null;
        double nearest = 20 * 20;
        for (CrabKingEntity k : player.level().getEntitiesOfClass(CrabKingEntity.class, player.getBoundingBox().inflate(20))) {
            double d = k.distanceToSqr(player);
            if (d < nearest) { nearest = d; best = k; }
        }
        return best;
    }

    private static void start(Minecraft mc) {
        if (FinishCinematic.active()) return;
        camera = new Marker(EntityType.MARKER, mc.level);
        phase = Phase.HELD;
        phaseTicks = 0;
        squeezesHeard = 0;
        heartbeat1 = heartbeat2 = false;
        shake = roll = red = 0;
        fov = 70;
        smoothEye = smoothLook = null;
        king = nearestKing(mc.player);
        mc.setCameraEntity(camera);
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (camera == null) return;
        camera = null;
        king = null;
        shake = roll = red = 0;
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { stop(); return; }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Object[] now = playing(mc.player, partial);
        if (camera == null) {
            if (now != null && now[0] == CrabKingAnimations.VICTIM && mc.player.isAlive()) start(mc);
            if (camera == null) return;
        }
        if (camera.level() != mc.level || !mc.player.isAlive() && phase != Phase.LANDED) { stop(); return; }
        if (now != null && now[0] == CrabKingAnimations.THROWN && phase == Phase.HELD) { phase = Phase.FLYING; phaseTicks = 0; }
        if (now == null && phase != Phase.LANDED) {
            // The victim's animations are over: thrown and landed (knocked down), or let go.
            phase = Phase.LANDED;
            phaseTicks = 0;
            shake = 8;
        }
        if (king != null && (king.isRemoved() || king.level() != mc.level)) king = null;
        if (king != null) lastKing = king.getEyePosition(partial);
        place(mc, now == null ? 0 : (float) now[1], partial);
    }

    /** Ticks: the phases' clocks, the shake and red dying down. */
    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (camera == null) return;
        phaseTicks++;
        shake *= 0.82F;
        red *= 0.86F;
        if (phase == Phase.LANDED && phaseTicks > SETTLE * 20) stop();
    }

    private static Vec3 lookAt(Vec3 from, Vec3 to, float[] angles) {
        Vec3 d = to.subtract(from);
        angles[0] = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        angles[1] = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        return d;
    }

    private static float ease(float a, float b, float t) {
        float k = Mth.clamp((t - a) / (b - a), 0, 1);
        return k * k * (3 - 2 * k);
    }

    private static void place(Minecraft mc, float t, float partial) {
        Player me = mc.player;
        Vec3 head = me.getEyePosition(partial), up = new Vec3(0, 1, 0);
        Vec3 toKing = lastKing.subtract(head);
        Vec3 f = new Vec3(toKing.x, 0, toKing.z);
        f = f.lengthSqr() < 1e-4 ? me.getLookAngle().multiply(1, 0, 1).normalize() : f.normalize();
        // The side away from his great arm (it comes from his right shoulder): the camera stays out of it.
        Vec3 side = new Vec3(-f.z, 0, f.x);
        if (king != null) {
            Vec3 kf = Vec3.directionFromRotation(0, king.yBodyRot), kingRight = new Vec3(-kf.z, 0, kf.x);
            side = kingRight.scale(-1);
        }
        Vec3 eye, look;
        float targetFov = 70, targetRoll = 0;
        if (phase == Phase.HELD) {
            float[] sq = CrabKingEntity.SQUEEZES;
            if (t < 0.5F) {
                // 1. Lifted: down at the feet, the ground falling away and the jaws; then up to his face.
                float k = ease(0.12F, 0.5F, t);
                eye = head.add(side.scale(0.85)).add(up.scale(0.45)).subtract(f.scale(0.45));
                Vec3 down = head.subtract(up.scale(2.2)).add(f.scale(0.5));
                look = down.lerp(lastKing, k);
                targetFov = 75 - 12 * k;
                shake = Math.max(shake, 1.2F * (1 - k));
            } else if (t < sq[0]) {
                // 2. Drawn close to his face, a slow push in, the view narrowing; then down at the jaws about to close.
                float k = ease(0.5F, 0.72F, t), j = ease(0.7F, sq[0], t);
                // From beside the head, out of the way of his face: the two faces in the frame, then down at the jaws.
                eye = head.add(side.scale(0.9 - 0.15 * k)).add(up.scale(0.45)).subtract(f.scale(0.45 - 0.1 * j));
                Vec3 jaws = head.subtract(up.scale(0.75));
                look = head.lerp(lastKing, 0.6).lerp(jaws, j);
                targetFov = 62 - 18 * k + 14 * j;
                if (!heartbeat1 && t > 0.55F) { heartbeat1 = true; beat(mc, head); }
                if (!heartbeat2 && t > 0.72F) { heartbeat2 = true; beat(mc, head); }
            } else if (t < 1.85F) {
                // 3. The squeezes: a jolt each, red at the edges; between them, from the jaws to his face and back.
                while (squeezesHeard < sq.length && t >= sq[squeezesHeard]) {
                    squeezesHeard++;
                    shake = 9;
                    red = 1;
                    fov = 40;
                    roll = (squeezesHeard % 2 == 0 ? 1 : -1) * 7;
                    mc.level.playLocalSound(head.x, head.y, head.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4F, 1.4F, false);
                }
                float breath = Mth.sin((t - sq[0]) * 5.5F) * 0.5F + 0.5F;
                eye = head.add(side.scale(0.9 - 0.15 * breath)).add(up.scale(0.45)).subtract(f.scale(0.4));
                Vec3 jaws = head.subtract(up.scale(0.8));
                look = jaws.lerp(head.lerp(lastKing, 0.6), breath * 0.8F);
                targetFov = 56 - 6 * breath;
            } else {
                // 4. Wound back over his shoulder: wide, the view tilting further and further.
                float k = ease(1.85F, CrabKingAnimations.GRAB_RELEASE, t);
                eye = head.add(up.scale(1.6 + 0.6 * k)).add(side.scale(1.0)).subtract(f.scale(0.4 * k));
                look = lastKing.lerp(head, 0.5).add(up.scale(-1.2 * k));
                targetFov = 70 + 22 * k;
                targetRoll = -28 * k;
            }
        } else if (phase == Phase.FLYING) {
            // 5. Thrown: behind the body as it flies, spinning, looking back at the king left behind.
            float s = phaseTicks + partial;
            Vec3 v = me.getDeltaMovement();
            Vec3 back = v.lengthSqr() < 1e-4 ? f.scale(-1) : v.normalize().scale(-1);
            eye = head.add(back.scale(-0.6)).add(up.scale(0.5));
            look = lastKing.lerp(head.add(v.normalize().scale(4)), ease(0, 12, s));
            targetFov = 95;
            targetRoll = (s * 26) % 360;
            shake = Math.max(shake, 2);
        } else {
            // On the ground, knocked down: low, the impact shaking it, then back.
            eye = head.add(up.scale(0.6)).subtract(f.scale(1.2));
            look = head.subtract(up.scale(0.3)).add(f.scale(1.0));
            targetFov = 72;
        }
        fov += (targetFov - fov) * 0.18F;
        roll += (targetRoll - roll) * (phase == Phase.FLYING ? 1 : 0.2F);
        // Eased toward this frame's shot (quicker in flight).
        float keep = phase == Phase.FLYING ? 0.55F : 0.75F;
        smoothEye = smoothEye == null ? eye : smoothEye.lerp(eye, 1 - keep);
        smoothLook = smoothLook == null ? look : smoothLook.lerp(look, 1 - keep);
        eye = smoothEye;
        look = smoothLook;
        float[] a = new float[2];
        lookAt(eye, look, a);
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot(a[0]);
        camera.setXRot(a[1]);
        camera.yRotO = a[0];
        camera.xRotO = a[1];
    }

    private static void beat(Minecraft mc, Vec3 at) {
        mc.level.playLocalSound(at.x, at.y, at.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.9F, false);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (camera == null) return;
        double n = System.nanoTime() / 1.6e7;
        event.setRoll(event.getRoll() + roll + shake * Mth.sin((float) n));
        event.setPitch(event.getPitch() + shake * 0.6F * Mth.cos((float) (n * 1.3)));
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (camera != null) event.setFOV(fov);
    }

    // ---------------------------------------------------------------- a film, not a game screen

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (camera != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (camera != null) event.setCanceled(true);
    }

    /** Thin cinema bars, and the red of a squeeze closing in from the edges. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (camera == null) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight(), bar = Math.round(h * 0.07F);
        if (red > 0.02F) {
            int edge = Math.round(Math.min(w, h) * 0.26F), alpha = Math.round(red * 120);
            int c = (alpha << 24) | 0x8A0000, none = 0x008A0000;
            g.fillGradient(0, 0, w, edge, c, none);
            g.fillGradient(0, h - edge, w, h, none, c);
            // The sides: thin bands growing fainter inward.
            for (int i = 0; i < 12; i++) {
                int a = Math.round(alpha * 0.35F * (1 - i / 12F)), cc = (a << 24) | 0x8A0000, x = i * edge / 12, x2 = (i + 1) * edge / 12;
                g.fill(x, 0, x2, h, cc);
                g.fill(w - x2, 0, w - x, h, cc);
            }
        }
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        camera = null;
        king = null;
        shake = roll = red = 0;
    }
}
