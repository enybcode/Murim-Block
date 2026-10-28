package com.murimblock.shenfa.client;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.Pose;
import java.lang.reflect.Method;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * First person on Shenfa: what the player sees of itself is the body others see.
 * <ul>
 * <li>The body (chest, arms, legs, feet) is drawn in the world from the same animated pose, the head and whatever
 * sits on it left out, so looking down shows the body and its steps, right at any field of view.</li>
 * <li>Vanilla's first-person hands stay for ordinary play (mining, placing, eating, every item pose); while a Shenfa
 * layer plays (a guard, a strike) our arms and held items take their place, from the same pose.</li>
 * <li>The camera follows the head (the eye above the neck) as the body moves it, smoothed, at most
 * {@link #MOST_SHIFT} blocks from vanilla's eye.</li>
 * </ul>
 * {@code /skeletal firstperson on|off} switches it (on by default when skeletal players are on).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class FirstPerson {
    /** The eye above the neck (the {@code Head} joint) in model pixels: vanilla's eye height at rest. */
    static final float EYE_ABOVE_NECK = 1.62f / (SkeletalPlayerRenderer.PLAYER_SCALE / 16) - 24;
    private static final float MOST_SHIFT = 0.35f;
    private static final float BACK_LOOKING_DOWN = 0.2f;
    /** The share of the way the camera moves toward the head each frame at 60 frames a second. */
    private static final float FOLLOW = 0.35f;
    private static final Method SET_POSITION = setPosition();

    /**
     * On unless the "First-person Model" mod (tr7zw, id {@code firstperson}) is there: it draws a body of its own,
     * and two bodies would show. Epic Fight turns that mod off instead; we step aside until that is tried with it.
     */
    private static boolean enabled = !net.neoforged.fml.ModList.get().isLoaded("firstperson");
    /** Whether the body being drawn is the local player's, in first person. */
    private static boolean drawing;
    /** Where the head puts the eye, from vanilla's eye (blocks), as last drawn; and the camera's smoothed shift. */
    private static final Vector3f eyeShift = new Vector3f(), cameraShift = new Vector3f();
    private static long lastShift;
    /** For checks: the nearest any hand came to the camera (blocks) since {@link #resetNearest}. */
    private static float nearestHand = Float.MAX_VALUE;

    private FirstPerson() {
    }

    private static Method setPosition() {
        try {
            Method method = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Camera.setPosition not found", e);
        }
    }

    public static void enable(boolean on) {
        enabled = on;
    }

    /** Whether the local player is seen from its own eyes, drawn by Shenfa. */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        return enabled && player != null && SkeletalPlayers.enabled() && SkeletalPlayers.animated()
                && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == player
                && !player.isSpectator() && !player.isSleeping()
                // Lying flat (swimming, crawling, gliding) the eye sits inside the chest: vanilla's hands instead.
                && !player.isVisuallySwimming() && !player.isFallFlying();
    }

    /** Whether the body being drawn right now is the local player's seen in first person. */
    static boolean drawing() {
        return drawing;
    }

    /**
     * Whether our arms replace vanilla's hands: in combat mode (the stance between strikes too, so the arms do not
     * swap back and forth), or while a Shenfa layer (a guard, a strike) plays.
     */
    public static boolean ownArms() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && (PlayerAnimators.inCombat() || PlayerAnimators.layerPlaying(mc.player));
    }

    /** The body, drawn after the other entities with the same buffers. */
    @SubscribeEvent
    public static void drawBody(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Vec3 camera = event.getCamera().getPosition();
        // Looking down, the body steps back under the eye (by up to {@link #BACK_LOOKING_DOWN} blocks): straight under
        // it, the chest's top would fill the view and hide the legs.
        float pitch = player.getViewXRot(partialTick);
        float back = BACK_LOOKING_DOWN * Math.max(0, Mth.sin(pitch * Mth.DEG_TO_RAD));
        Vec3 ahead = Vec3.directionFromRotation(0, player.getViewYRot(partialTick));
        double x = Mth.lerp(partialTick, player.xOld, player.getX()) - ahead.x * back;
        double y = Mth.lerp(partialTick, player.yOld, player.getY());
        double z = Mth.lerp(partialTick, player.zOld, player.getZ()) - ahead.z * back;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        drawing = true;
        try {
            mc.getEntityRenderDispatcher().render(player, x - camera.x, y - camera.y, z - camera.z,
                    Mth.lerp(partialTick, player.yRotO, player.getYRot()), partialTick, event.getPoseStack(), buffers,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(player, partialTick));
        } finally {
            drawing = false;
        }
        buffers.endBatch();
    }

    /** How far the arms rise in one's own view in combat (degrees): the stance's fists sit below the eye. */
    private static final float RAISE = 28;

    /**
     * Lifts the arms in the local player's own view while in combat, so the guard and the fists sit in sight (Epic
     * Fight adjusts its first-person poses per clip); others see the stance as it is.
     */
    static void raiseArms(Pose pose) {
        for (String side : new String[] {"L", "R"}) {
            int arm = Biped.ARMATURE.get("Arm_" + side).index();
            var joint = pose.get(arm);
            Quaternionf raised = new Quaternionf().rotateX((float) Math.toRadians(-RAISE))
                    .mul(joint.rotation());
            pose.set(arm, JointTransform.of(joint.translation(), raised, joint.scale()));
        }
    }

    /** Vanilla's hands, unless our arms are drawn. */
    @SubscribeEvent
    public static void hands(RenderHandEvent event) {
        if (active() && ownArms()) {
            event.setCanceled(true);
        }
    }

    /** The camera to the head's eye: the field of view is worked out after the camera is placed, before the world. */
    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) {
            return;
        }
        Camera camera = event.getCamera();
        long now = System.nanoTime();
        float frames = lastShift == 0 ? 1 : Math.min(10, (now - lastShift) / 1.6667e7f);
        lastShift = now;
        Vector3f target = active() ? eyeShift : new Vector3f();
        float follow = 1 - (float) Math.pow(1 - FOLLOW, frames);
        cameraShift.lerp(target, follow);
        if (cameraShift.length() > MOST_SHIFT) {
            cameraShift.normalize(MOST_SHIFT);
        }
        if (cameraShift.lengthSquared() < 1e-8f || !active()) {
            return;
        }
        try {
            SET_POSITION.invoke(camera, camera.getPosition().add(cameraShift.x, cameraShift.y, cameraShift.z));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Camera.setPosition", e);
        }
    }

    /** Called by the renderer while drawing the local player in first person: where the head puts the eye. */
    static void eye(Vector3f shift) {
        eyeShift.set(shift);
    }

    /** Called by the renderer: how near a hand (blocks, from the camera) came this frame. */
    static void hand(float distance) {
        nearestHand = Math.min(nearestHand, distance);
    }

    /** For checks: the nearest a hand came to the camera since the last call, then starts again. */
    public static float resetNearest() {
        float nearest = nearestHand;
        nearestHand = Float.MAX_VALUE;
        return nearest;
    }
}
