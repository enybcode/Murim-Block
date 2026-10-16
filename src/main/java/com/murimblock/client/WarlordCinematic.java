package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.mob.BanditWarlordEntity;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The Bandit Warlord's entrance as the players in the camp see it (Enzo, 16/10/2026: "a cinematic when he spawns"),
 * started by the server (WarlordEntrancePayload) when the camp's last man dies, on the server's timing (WarlordEntrance):
 * <ol>
 *   <li>the war drum before the longhall, close, the view jolting at each of its three beats;</li>
 *   <li>up from the drum to the sky over the plaza: a red point, growing;</li>
 *   <li>the meteor seen from the plaza's edge, a red point growing; then riding down with him, close, from just above
 *   his shoulder, the camp rushing up below (like the meteor in One Punch Man);</li>
 *   <li>the impact: a white flash, the view shaken hard, wide from the rim on the blast;</li>
 *   <li>low before him as he rises from the dust and roars: his name written large.</li>
 * </ol>
 * Purely visual (the server drops him, carves the crater, and keeps the players from harm meanwhile); the player cannot
 * move while it plays.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class WarlordCinematic {
    /** When the warlord is let fall (WarlordEntrance.FALL_AT), and how long after the impact the film lasts (ticks). */
    private static final int FALL_AT = 50, AFTER = 64, LONGEST = 300;
    /** How long the meteor is a red point seen from the ground before the camera rides down with him (ticks). */
    private static final int RED_POINT = 18;
    private static final int[] BEATS = {10, 22, 34};

    @Nullable private static Marker camera;
    private static BlockPos plaza = BlockPos.ZERO, drum = BlockPos.ZERO;
    private static int ticks, impactAt = -1;
    private static float shake, flash, fov = 70;
    @Nullable private static BanditWarlordEntity warlord;
    @Nullable private static Vec3 smoothEye, smoothLook;

    private WarlordCinematic() { }

    public static boolean active() {
        return camera != null;
    }

    public static void start(BlockPos plazaPos, BlockPos drumPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || FinishCinematic.active() || GrabCinematic.active()) return;
        plaza = plazaPos;
        drum = drumPos;
        ticks = 0;
        impactAt = -1;
        shake = flash = 0;
        fov = 70;
        warlord = null;
        smoothEye = smoothLook = null;
        camera = new Marker(EntityType.MARKER, mc.level);
        mc.setCameraEntity(camera);
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (camera == null) return;
        camera = null;
        warlord = null;
        shake = flash = 0;
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (camera == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || camera.level() != mc.level) { stop(); return; }
        ticks++;
        for (int beat : BEATS) if (ticks == beat) { shake = 6; fov = 62; }
        if (warlord == null || warlord.isRemoved()) warlord = find(mc);
        if (impactAt < 0 && warlord != null && ticks > FALL_AT && !warlord.isMeteor()) {
            impactAt = ticks;
            shake = 14;
            flash = 1;
        }
        shake *= 0.84F;
        flash *= 0.88F;
        if (impactAt >= 0 && ticks > impactAt + AFTER || ticks > LONGEST) stop();
    }

    @Nullable private static BanditWarlordEntity find(Minecraft mc) {
        BanditWarlordEntity best = null;
        double nearest = Double.MAX_VALUE;
        Vec3 c = Vec3.atBottomCenterOf(plaza);
        for (BanditWarlordEntity w : mc.level.getEntitiesOfClass(BanditWarlordEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(64, 140, 64))) {
            double d = w.distanceToSqr(c);
            if (d < nearest) { nearest = d; best = w; }
        }
        return best;
    }

    /** How far to ease in {@code dt} ticks, {@code perFrame} being the share at sixty frames a second. */
    private static float eased(float perFrame, float dt) {
        return 1 - (float) Math.pow(1 - perFrame, dt * 3);
    }

    private static float ease(float a, float b, float t) {
        float k = Mth.clamp((t - a) / (b - a), 0, 1);
        return k * k * (3 - 2 * k);
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (camera == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { stop(); return; }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false), t = ticks + partial;
        Vec3 c = Vec3.atBottomCenterOf(plaza), d = Vec3.atCenterOf(drum), up = new Vec3(0, 1, 0);
        Vec3 toPlaza = c.subtract(d).multiply(1, 0, 1);
        Vec3 f = toPlaza.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : toPlaza.normalize();
        Vec3 side = new Vec3(-f.z, 0, f.x);
        Vec3 sky = c.add(0, 70, 0);
        Vec3 him = warlord == null ? sky : warlord.getPosition(partial).add(0, warlord.isMeteor() ? 1.5 : 3.0, 0);
        Vec3 eye, look;
        float targetFov;
        if (impactAt < 0 && t < 40) {
            // 1. The drum, close, from the plaza's side.
            eye = d.add(f.scale(4.5)).add(side.scale(1.5)).add(up.scale(0.6 + 0.3 * ease(0, 40, t)));
            look = d;
            targetFov = 58;
        } else if (impactAt < 0 && t < FALL_AT + 6) {
            // 2. Up from the drum to the sky over the plaza.
            float k = ease(40, FALL_AT + 6, t);
            eye = d.add(f.scale(4.5)).add(side.scale(1.5)).add(up.scale(0.9 + 3 * k));
            look = d.lerp(sky, k);
            targetFov = 58 + 12 * k;
        } else if (impactAt < 0 && t < FALL_AT + RED_POINT) {
            // 3. From the plaza's edge, looking up: a red point in the sky, growing.
            eye = c.add(f.scale(-22)).add(side.scale(8)).add(up.scale(16));
            look = him;
            targetFov = 34;
        } else if (impactAt < 0) {
            // 4. Riding down with him (Enzo: "a real close-up, seen from above, like the meteor in One Punch Man"): just above
            // and behind his shoulder, looking down past him at the camp rushing up, wrapped in his fire.
            float k = ease(FALL_AT + RED_POINT, FALL_AT + RED_POINT + 10, t);
            Vec3 above = him.add(up.scale(4.5)).add(side.scale(2.2)).subtract(f.scale(2.0));
            Vec3 wide = c.add(f.scale(-22)).add(side.scale(8)).add(up.scale(16));
            eye = wide.lerp(above, k);
            look = him.lerp(c, 0.25 * k).subtract(up.scale(0.5 * k));
            targetFov = 34 + 46 * k;
            shake = Math.max(shake, 1.5F * k);
        } else if (t < impactAt + 26) {
            // 5. The impact, wide from the rim.
            float k = ease(impactAt, impactAt + 26, t);
            eye = c.add(f.scale(-24)).add(side.scale(10)).add(up.scale(20 - 4 * k));
            look = c.add(0, 1, 0);
            targetFov = 80;
        } else {
            // 6. Low before him as he rises and roars.
            // His body's facing (his head turns first, toward whoever is near).
            Vec3 face = warlord == null ? f.scale(-1) : Vec3.directionFromRotation(0, Mth.lerp(partial, warlord.yBodyRotO, warlord.yBodyRot)).multiply(1, 0, 1).normalize();
            Vec3 base = warlord == null ? c : warlord.getPosition(partial);
            float k = ease(impactAt + 26, impactAt + AFTER, t);
            eye = base.add(face.scale(7.0 - 1.5 * k)).add(up.scale(1.6)).add(new Vec3(-face.z, 0, face.x).scale(2));
            look = base.add(0, 2.4 + 0.6 * k, 0);
            targetFov = 62 - 8 * k;
        }
        // Eased toward this frame's shot by the time gone, not by frames: the same at any frame rate.
        float dt = Math.max(0.02F, event.getPartialTick().getGameTimeDeltaTicks());
        fov += (targetFov - fov) * eased(0.15F, dt);
        smoothEye = smoothEye == null ? eye : smoothEye.lerp(eye, eased(impactAt < 0 && t > FALL_AT + RED_POINT ? 0.7F : 0.25F, dt));
        smoothLook = smoothLook == null ? look : smoothLook.lerp(look, eased(impactAt < 0 && t > FALL_AT ? 0.6F : 0.25F, dt));
        Vec3 dir = smoothLook.subtract(smoothEye);
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));
        camera.setPos(smoothEye);
        camera.xo = camera.xOld = smoothEye.x;
        camera.yo = camera.yOld = smoothEye.y;
        camera.zo = camera.zOld = smoothEye.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (camera == null) return;
        double n = System.nanoTime() / 1.6e7;
        event.setRoll(event.getRoll() + shake * Mth.sin((float) n));
        event.setPitch(event.getPitch() + shake * 0.6F * Mth.cos((float) (n * 1.3)));
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (camera != null) event.setFOV(fov);
    }

    /** No walking off while it plays. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (camera == null) return;
        var input = event.getInput();
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
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

    /** Cinema bars, the impact's white flash, and his name. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (camera == null) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight(), bar = Math.round(h * 0.09F);
        if (flash > 0.02F) g.fill(0, 0, w, h, (Math.round(flash * 230) << 24) | 0xFFF4E0);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        if (impactAt >= 0 && ticks > impactAt + 30) {
            float a = Mth.clamp((ticks - impactAt - 30) / 10F, 0, 1) * Mth.clamp((impactAt + AFTER - ticks) / 6F, 0, 1);
            int alpha = Math.max(4, Math.round(a * 255)) << 24;
            Component name = Component.translatable("entity.murimblock.bandit_warlord");
            Component title = Component.translatable("cinematic.murimblock.warlord.title");
            var pose = g.pose();
            pose.pushPose();
            pose.translate(w / 2F, h * 0.62F, 0);
            pose.scale(3, 3, 3);
            g.drawCenteredString(mc.font, name, 0, 0, alpha | 0xE8C25A);
            pose.popPose();
            g.drawCenteredString(mc.font, title, w / 2, Math.round(h * 0.62F) + 30, alpha | 0xD8D0C0);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        camera = null;
        warlord = null;
        shake = flash = 0;
    }
}
