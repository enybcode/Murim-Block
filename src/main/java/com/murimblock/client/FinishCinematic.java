package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.network.FinishCinematicPayload;
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
    /**
     * A film's timing, in seconds: its length and where each shot starts (the low angle, the impact, the turn). The
     * generic finish is 1.7 s with the blow at 0.9 s; Last Rain Over the Clouds is 2.2 s, the blade raised to the sky
     * until 1.1 s, the blow at 1.5 s.
     */
    record Timing(float duration, float low, float impact, float turn) { }

    static final Timing GENERIC = new Timing(2.0F, 0.5F, 0.86F, 1.3F);
    static final Timing AZURE_CLOUD = new Timing(2.6F, 0.95F, 1.46F, 1.95F);
    /** Final Palm: the breath until 1.0 s, down on one knee, the palm at 1.4 s. */
    static final Timing LONG_FIST = new Timing(2.4F, 0.9F, 1.36F, 1.85F);
    /** Mountain Burial: the fist raised until 0.3 s, three blows, the last at 1.4 s. */
    static final Timing MOUNTAIN_BURIAL = new Timing(2.6F, 0.3F, 1.36F, 1.9F);
    /** Extinguished Stars: seven thrusts from 0.35 s, the stars put out at 1.9 s. */
    static final Timing SEVEN_STARS = new Timing(2.6F, 0.35F, 1.9F, 2.1F);
    /** Noon Sentence: the palm raised until 0.6 s, the sun brought down at 1.6 s. */
    static final Timing NOON_SENTENCE = new Timing(2.6F, 0.6F, 1.6F, 2.0F);
    /**
     * Scarlet Bloom: its own moving camera ({@link #placeScarlet}); 7.8 s, the eruption at 0.7 s, the cage closing from
     * 3.5 s, the burst at 5.0 s (slow motion until 5.6 s), the landing at 6.1 s.
     */
    static final Timing SCARLET_BLOOM = new Timing(7.8F, 0.7F, 6.1F, 6.1F);
    /** While Scarlet Bloom's cage closes, the film is in black and white (Enzo's choice), seconds. */
    static final float MONO_FROM = 3.5F, MONO_TO = 5.0F;
    private static final net.minecraft.resources.ResourceLocation MONOCHROME =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "shaders/post/scarlet_monochrome.json");
    /** Whether the black and white is on now. */
    private static boolean mono;
    /** Length of the generic film, in seconds. */
    public static final float DURATION = GENERIC.duration();
    /** Height of each cinema bar, as a share of the screen. */
    private static final float BAR = 0.11F;

    private static Timing timing = GENERIC;
    private static int executorId = -1, victimId = -1;
    private static long startTick;
    @Nullable private static Marker camera;
    @Nullable private static Screen deferredDeath;
    /** Last known positions, in case one of the two disappears (the dead victim) during the film. */
    private static Vec3 lastExecutor = Vec3.ZERO, lastVictim = Vec3.ZERO;
    private static float shake, roll;
    /** Where the victim lay when Scarlet Bloom began. */
    private static Vec3 scarletOrigin = Vec3.ZERO;
    private static float lastPartial;

    private FinishCinematic() {
    }

    public static boolean active() {
        return camera != null;
    }

    /** Starts the film (from the server's message): this client is the executor or the victim. */
    public static void start(int executor, int victim) {
        start(executor, victim, FinishCinematicPayload.GENERIC);
    }

    /** Starts the film of a given finish ({@link FinishCinematicPayload#GENERIC}, {@link FinishCinematicPayload#AZURE_CLOUD}). */
    public static void start(int executor, int victim, int finish) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Entity e = mc.level.getEntity(executor), v = mc.level.getEntity(victim);
        if (e == null || v == null) return;
        timing = finish == FinishCinematicPayload.AZURE_CLOUD ? AZURE_CLOUD : finish == FinishCinematicPayload.LONG_FIST ? LONG_FIST
                : finish == FinishCinematicPayload.MOUNTAIN_BURIAL ? MOUNTAIN_BURIAL
                : finish == FinishCinematicPayload.SEVEN_STARS ? SEVEN_STARS
                : finish == FinishCinematicPayload.NOON_SENTENCE ? NOON_SENTENCE
                : finish == FinishCinematicPayload.SCARLET_BLOOM ? SCARLET_BLOOM : GENERIC;
        scarletOrigin = v.position();
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
        roll = 0;
        monochrome(false);
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
        return mc.level == null ? timing.duration() : (mc.level.getGameTime() - startTick + partial) / 20.0F;
    }

    private static void place(float t) {
        Minecraft mc = Minecraft.getInstance();
        if (timing == SCARLET_BLOOM) { placeScarlet(t, lastPartial); return; }
        roll = 0;
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
        boolean sky = timing == AZURE_CLOUD;
        if (t < timing.low()) {
            // 1. Wide, from the side, drifting forward while the weapon rises (for the Azure Cloud finish, a little
            // higher, to see the cloud gather over the body).
            eye = mid.add(s.scale(3.4)).add(up.scale(sky ? 1.8 : 1.3)).add(f.scale(-0.4 + 0.6 * t / timing.low() * 0.5));
            look = mid.add(up.scale(sky ? 1.8 : 1.0));
        } else if (t < timing.impact()) {
            // 2. Low, beside the victim's head, up at the raised blade (at the cloud and the leap); a slow push in.
            float k = (t - timing.low()) / (timing.impact() - timing.low());
            eye = vi.add(f.scale(-0.1 - 0.25 * k)).add(s.scale(0.9)).add(up.scale(0.2));
            look = ex.add(up.scale(sky ? 2.4 : 1.7));
        } else if (t < timing.turn()) {
            // 3. High, over the victim, on the impact; the camera sinks a little.
            float k = (t - timing.impact()) / (timing.turn() - timing.impact());
            eye = vi.add(f.scale(-0.2)).add(s.scale(1.6)).add(up.scale(2.7 - 0.5 * k));
            look = vi.add(up.scale(0.2));
        } else {
            // 4. A slow turn around the executor standing over the body.
            double a = Math.atan2(s.z, s.x) + (t - timing.turn()) * 0.9;
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
        shake = t >= timing.impact() && t < timing.impact() + 0.2F ? 4.0F * (1 - (t - timing.impact()) / 0.2F) : 0;
    }

    /** Turns the black and white of Scarlet Bloom on or off (Minecraft's own post effect, shaders/post/scarlet_monochrome). */
    private static void monochrome(boolean on) {
        if (on == mono) return;
        mono = on;
        Minecraft mc = Minecraft.getInstance();
        if (on) mc.gameRenderer.loadEffect(MONOCHROME);
        else mc.gameRenderer.shutdownEffect();
    }

    /**
     * Scarlet Bloom's camera, after Enzo's reference film: never still, it moves through the finish instead of cutting
     * between fixed shots. Over the executor's shoulder, pushing in while he winds up and drives his hand in; at the
     * eruption, low by the crystals, rising and tilting up to follow the body into the sky (shaken); around the body
     * hanging in its cage, turning faster and faster, rolling; from above as it falls; then close on the executor,
     * turning from behind his shoulder round to his face.
     */
    private static void placeScarlet(float t, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level.getEntity(executorId), v = mc.level.getEntity(victimId);
        if (e != null) lastExecutor = e.getPosition(partial);
        if (v != null && v.isAlive()) lastVictim = v.getPosition(partial);
        Vec3 ex = lastExecutor, vi = lastVictim, o = scarletOrigin;
        Vec3 f = new Vec3(o.x - ex.x, 0, o.z - ex.z);
        f = f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
        double sign = mc.player != null && mc.player.getId() == victimId ? 1 : -1;
        Vec3 s = new Vec3(-f.z, 0, f.x).scale(sign), up = new Vec3(0, 1, 0);
        Vec3 eye, look;
        roll = 0;
        if (t < 0.7F) {
            float k = t / 0.7F;
            eye = ex.subtract(f.scale(2.4 - 0.9 * k)).add(s.scale(0.9)).add(up.scale(1.25 - 0.35 * k));
            look = o.add(up.scale(0.3));
            roll = -4 * k;
        } else if (t < 2.0F) {
            float k = (t - 0.7F) / 1.3F, ease = 1 - (1 - k) * (1 - k);
            eye = o.subtract(f.scale(1.5)).add(s.scale(4.0)).add(up.scale(0.3 + 4.2 * ease));
            look = vi.add(up.scale(0.6));
            roll = 6 * (1 - k);
        } else if (t < 3.5F) {
            // Round the body in its cage, faster and faster, rolling.
            float k = (t - 2.0F) / 1.5F, u = t - 2.0F;
            double a = Math.atan2(s.z, s.x) + u * (1.4 + 1.1 * u);
            double r = 6.0 - 1.4 * k;
            eye = vi.add(Math.cos(a) * r, 0.5 + 0.7 * Math.sin(t * 1.3), Math.sin(a) * r);
            look = vi.add(up.scale(0.6));
            roll = 9 * Mth.sin(t * 2.2F);
        } else if (t < 5.0F) {
            // The cage closes (black and white): a slow push in on the body, the turn almost stopped.
            float k = (t - 3.5F) / 1.5F, ease = k * k * (3 - 2 * k);
            double a0 = Math.atan2(s.z, s.x) + 1.5 * (1.4 + 1.1 * 1.5);
            double a = a0 + 0.35 * k;
            // Outside the cage (3 blocks round the body, closing to 1.35): the beams pass before the lens, never through it.
            double r = 5.4 - 1.6 * ease;
            eye = vi.add(Math.cos(a) * r, 0.9 - 0.3 * ease, Math.sin(a) * r);
            look = vi.add(up.scale(0.7));
            roll = 9 * Mth.sin(3.5F * 2.2F) * (1 - ease);
        } else if (t < 5.6F) {
            // The burst, in slow motion: pulled back and below, looking up at the bloom.
            float k = (t - 5.0F) / 0.6F, ease = 1 - (1 - k) * (1 - k);
            double a = Math.atan2(s.z, s.x) + 3.6 + 0.3 * k;
            double r = 2.4 + 5.2 * ease;
            eye = vi.add(Math.cos(a) * r, -1.6 - 1.2 * ease, Math.sin(a) * r);
            look = vi.add(up.scale(0.8));
        } else if (t < 6.4F) {
            // The pieces fall: from above the crater, sinking with them.
            // From the side, a little high, looking down with the pieces as they fall into the crater.
            float k = (t - 5.6F) / 0.8F, ease = k * (2 - k);
            eye = o.add(s.scale(6.0)).subtract(f.scale(2.0)).add(up.scale(6.5 - 3.5 * ease));
            look = o.add(up.scale(8.0 * (1 - ease) + 0.4));
        } else {
            float k = Math.min(1, (t - 6.4F) / 1.4F), ease = k * k * (3 - 2 * k);
            double a = Math.atan2(-f.z, -f.x) + sign * ease * 2.3;
            eye = ex.add(Math.cos(a) * 1.7, 1.75 - 0.2 * ease, Math.sin(a) * 1.7);
            look = ex.add(up.scale(1.55)).add(f.scale(0.4 * (1 - ease)));
        }
        Vec3 d = look.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
        shake = t >= 0.7F && t < 1.1F ? 6.0F * (1 - (t - 0.7F) / 0.4F) : t >= 5.0F && t < 5.15F ? 3.0F
                : t >= 6.1F && t < 6.4F ? 6.0F * (1 - (t - 6.1F) / 0.3F) : 0;
        monochrome(t >= MONO_FROM && t < MONO_TO);
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (camera == null) return;
        lastPartial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float t = time(lastPartial);
        if (t >= timing.duration()) stop();
        else place(t);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (camera != null && (mc.level == null || camera.level() != mc.level || time(0) >= timing.duration())) stop();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (camera != null) event.setRoll(event.getRoll() + roll + (shake > 0 ? shake * Mth.sin((float) (System.nanoTime() / 2.0e7)) : 0));
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
        if (mono) monochrome(false);
        camera = null;
        deferredDeath = null;
        shake = 0;
    }
}
