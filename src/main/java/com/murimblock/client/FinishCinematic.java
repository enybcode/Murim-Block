package com.murimblock.client;

import com.murimblock.Murimblock;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;

/**
 * The finish seen as a short film by both people in it, the executor and the one executed (docs/combat/STYLES.md):
 * the camera leaves the player for four cut shots around the two of them, with cinema bars and no HUD, then comes
 * back. A wide side shot while the weapon rises, a low angle from the victim up at the blade, a high angle on the
 * impact (with a shake), a slow turn around the executor. The victim's death screen waits for the end of the film.
 * Purely visual: the server decides the finish and the death.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class FinishCinematic {
    /** Length of the film, in seconds (the finish animation is 1.7 s; the blow lands at 0.9 s). */
    public static final float DURATION = 2.0F;
    /** Shot boundaries, in seconds. */
    static final float SHOT_LOW = 0.5F, SHOT_IMPACT = 0.86F, SHOT_TURN = 1.3F;
    /** Height of each cinema bar, as a share of the screen. */
    private static final float BAR = 0.11F;

    private static int executorId = -1, victimId = -1;
    private static long startTick;
    @Nullable private static Marker camera;
    @Nullable private static Screen deferredDeath;
    /** Last known positions, in case one of the two disappears (the dead victim) during the film. */
    private static Vec3 lastExecutor = Vec3.ZERO, lastVictim = Vec3.ZERO;
    private static float shake;

    private FinishCinematic() {
    }

    public static boolean active() {
        return camera != null;
    }

    /** Starts the film (from the server's message): this client is the executor or the victim. */
    public static void start(int executor, int victim) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity e = mc.level.getEntity(executor), v = mc.level.getEntity(victim);
        if (e == null || v == null) return;
        executorId = executor;
        victimId = victim;
        lastExecutor = e.position();
        lastVictim = v.position();
        startTick = mc.level.getGameTime();
        camera = new Marker(EntityType.MARKER, mc.level);
        place(0);
        mc.setCameraEntity(camera);
    }

    /** Ends the film (its end, or the finish broken off): back to the player, then the deferred death screen. */
    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (camera == null) return;
        camera = null;
        shake = 0;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (deferredDeath != null) {
            Screen death = deferredDeath;
            deferredDeath = null;
            if (mc.player != null && !mc.player.isAlive()) mc.setScreen(death);
        }
    }

    // ---------------------------------------------------------------- the shots

    /** Seconds into the film at this frame. */
    private static float time(float partial) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? DURATION : (mc.level.getGameTime() - startTick + partial) / 20.0F;
    }

    private static void place(float t) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level.getEntity(executorId), v = mc.level.getEntity(victimId);
        if (e != null) lastExecutor = e.position();
        if (v != null && v.isAlive()) lastVictim = v.position();
        Vec3 ex = lastExecutor, vi = lastVictim;
        Vec3 f = new Vec3(vi.x - ex.x, 0, vi.z - ex.z);
        f = f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
        // The executor's and the victim's films are filmed from opposite sides.
        double sign = mc.player != null && mc.player.getId() == victimId ? 1 : -1;
        Vec3 s = new Vec3(-f.z, 0, f.x).scale(sign);
        Vec3 mid = ex.add(vi).scale(0.5);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 eye, look;
        if (t < SHOT_LOW) {
            // 1. Wide, from the side, drifting forward while the weapon rises.
            eye = mid.add(s.scale(3.4)).add(up.scale(1.3)).add(f.scale(-0.4 + 0.6 * t));
            look = mid.add(up.scale(1.0));
        } else if (t < SHOT_IMPACT) {
            // 2. Low, beside the victim's head, up at the raised blade; a slow push in.
            float k = (t - SHOT_LOW) / (SHOT_IMPACT - SHOT_LOW);
            eye = vi.add(f.scale(-0.1 - 0.25 * k)).add(s.scale(0.9)).add(up.scale(0.2));
            look = ex.add(up.scale(1.7));
        } else if (t < SHOT_TURN) {
            // 3. High, over the victim, on the impact; the camera sinks a little.
            float k = (t - SHOT_IMPACT) / (SHOT_TURN - SHOT_IMPACT);
            eye = vi.add(f.scale(-0.2)).add(s.scale(1.6)).add(up.scale(2.7 - 0.5 * k));
            look = vi.add(up.scale(0.2));
        } else {
            // 4. A slow turn around the executor standing over the body.
            double a = Math.atan2(s.z, s.x) + (t - SHOT_TURN) * 0.9;
            eye = ex.add(Math.cos(a) * 3.0, 1.5, Math.sin(a) * 3.0);
            look = ex.add(up.scale(1.0));
        }
        Vec3 d = look.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        // Same old and new values: the camera does not smooth across a cut.
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
        shake = t >= SHOT_IMPACT && t < SHOT_IMPACT + 0.2F ? 4.0F * (1 - (t - SHOT_IMPACT) / 0.2F) : 0;
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (camera == null) return;
        float t = time(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (t >= DURATION) stop();
        else place(t);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (camera != null && (mc.level == null || camera.level() != mc.level || time(0) >= DURATION)) stop();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (camera != null && shake > 0) event.setRoll(event.getRoll() + shake * Mth.sin((float) (System.nanoTime() / 2.0e7)));
    }

    // ---------------------------------------------------------------- a film, not a game screen

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (camera != null) event.setCanRender(TriState.FALSE);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (camera != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (camera != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (camera == null) return;
        GuiGraphics g = event.getGuiGraphics();
        int bar = Math.round(g.guiHeight() * BAR);
        g.fill(0, 0, g.guiWidth(), bar, 0xFF000000);
        g.fill(0, g.guiHeight() - bar, g.guiWidth(), g.guiHeight(), 0xFF000000);
    }

    /** The victim sees the end of the film before the death screen. */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (camera != null && event.getNewScreen() instanceof DeathScreen) {
            deferredDeath = event.getNewScreen();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        camera = null;
        deferredDeath = null;
        shake = 0;
    }
}
