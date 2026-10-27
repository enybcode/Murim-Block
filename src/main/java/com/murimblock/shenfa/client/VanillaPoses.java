package com.murimblock.shenfa.client;

import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.BipedPoses;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.Pose;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Vanilla's arm work on top of our animation: the arm poses for using items (bow, crossbow, shield, trident,
 * spyglass, goat horn, brush, and any pose other mods add through NeoForge), the attack swing, and an arm raised a
 * little when it holds an item. Vanilla's own player model works them out every frame; its arms and body are read
 * back and laid on our joints:
 * <ul>
 * <li>an item pose takes the arm (both for two-handed poses) as vanilla places it relative to the body's root, the
 * forearm straight, fading in and out over {@link #FADE} seconds instead of vanilla's snap, each arm keeping the way
 * round it started on;</li>
 * <li>the swing is added as the change vanilla's swing makes to each arm and to the body's turn, so it plays over
 * any clip (walking, running, a guard);</li>
 * <li>holding an item raises the arm by vanilla's 18°, on the animator only (the {@code stand} pose has it).</li>
 * </ul>
 */
final class VanillaPoses {
    /** Seconds an item pose takes to come and go (vanilla snaps; a trident's arm goes up half a turn). */
    private static final float FADE = 0.15f;
    private static final float RAISE = (float) Math.toRadians(-18);
    private static final Method SET_MODEL_PROPERTIES = method();

    private static final Map<AbstractClientPlayer, Arms> STATES = new WeakHashMap<>();

    private VanillaPoses() {
    }

    private static Method method() {
        try {
            Method method = PlayerRenderer.class.getDeclaredMethod("setModelProperties", AbstractClientPlayer.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("PlayerRenderer.setModelProperties not found", e);
        }
    }

    /** How far each arm is held by an item pose or raised, and the item pose last seen (kept to fade it out). */
    private static final class Arms {
        final float[] hold = new float[2], raise = new float[2];
        final Quaternionf[] held = {new Quaternionf(), new Quaternionf()};
        float lastTime = Float.NaN;
        /** Keeps each arm turning the same way through a fade: a raised trident is half a turn from a hanging arm. */
        final Animator.SteadyBlend steady = new Animator.SteadyBlend();
    }

    /**
     * Lays vanilla's arm work on {@code pose} (local transforms, changed in place).
     *
     * @param netHeadYaw and {@code headPitch}: vanilla's head angles in degrees, as it passes them to the model
     * @param animated   whether {@code pose} comes from the animator (it then also gets the raise for held items)
     */
    static void apply(PlayerRenderer renderer, AbstractClientPlayer player, Pose pose, float partialTick,
                      float netHeadYaw, float headPitch, boolean animated) {
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        try {
            SET_MODEL_PROPERTIES.invoke(renderer, player);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("setModelProperties", e);
        }
        Entity vehicle = player.getVehicle();
        boolean sitting = player.isPassenger() && vehicle != null && vehicle.shouldRiderSit();
        model.riding = sitting;
        model.young = player.isBaby();
        float limbSpeed = 0, limbPosition = 0;
        if (!sitting && player.isAlive()) {
            limbSpeed = Math.min(player.walkAnimation.speed(partialTick), 1);
            limbPosition = player.walkAnimation.position(partialTick);
        }
        float bob = player.tickCount + partialTick;
        model.attackTime = 0;
        model.prepareMobModel(player, limbPosition, limbSpeed, partialTick);
        model.setupAnim(player, limbPosition, limbSpeed, bob, netHeadYaw, headPitch);
        Quaternionf[] still = {vanilla(model.rightArm), vanilla(model.leftArm)};
        float stillBody = model.body.yRot;
        float attack = player.getAttackAnim(partialTick);
        Quaternionf[] swung = still;
        float swungBody = stillBody;
        if (attack > 0) {
            model.attackTime = attack;
            model.setupAnim(player, limbPosition, limbSpeed, bob, netHeadYaw, headPitch);
            swung = new Quaternionf[] {vanilla(model.rightArm), vanilla(model.leftArm)};
            swungBody = model.body.yRot;
        }

        Arms arms = STATES.computeIfAbsent(player, key -> new Arms());
        float now = player.level().getGameTime() + partialTick;
        float seconds = Float.isNaN(arms.lastTime) ? 1 : Math.max(0, Math.min(0.25f, (now - arms.lastTime) / 20));
        arms.lastTime = now;
        HumanoidModel.ArmPose[] poses = {model.rightArmPose, model.leftArmPose};
        for (int side = 0; side < 2; side++) {
            HumanoidModel.ArmPose own = poses[side], other = poses[1 - side];
            boolean hold = special(own) || other.isTwoHanded();
            boolean raise = animated && !hold && own == HumanoidModel.ArmPose.ITEM;
            if (hold) {
                arms.held[side] = still[side];
            }
            arms.hold[side] = approach(arms.hold[side], hold ? 1 : 0, seconds / FADE);
            arms.raise[side] = approach(arms.raise[side], raise ? 1 : 0, seconds / FADE);
        }

        String[] armJoints = {"Arm_R", "Arm_L"}, shoulders = {"Shoulder_R", "Shoulder_L"}, hands = {"Hand_R", "Hand_L"};
        Matrix4f[] before = pose.toModelMatrices(Biped.ARMATURE);
        Quaternionf[] parentBefore = new Quaternionf[2];
        for (int side = 0; side < 2; side++) {
            int arm = index(armJoints[side]);
            parentBefore[side] = before[index(shoulders[side])].getUnnormalizedRotation(new Quaternionf()).normalize();
            Quaternionf local = new Quaternionf(pose.get(arm).rotation());
            float raise = ease(arms.raise[side]);
            if (raise > 0) {
                local.rotateX(RAISE * raise);
            }
            // Blended every frame, even with no item pose (weight 0), so the steady blend always knows the last turn.
            float hold = ease(arms.hold[side]);
            Quaternionf target = new Quaternionf(parentBefore[side]).conjugate().mul(arms.held[side]);
            local = arms.steady.rotation(side, 4, local, target, hold);
            int hand = index(hands[side]);
            JointTransform forearm = pose.get(hand);
            pose.set(hand, JointTransform.of(forearm.translation(),
                    arms.steady.rotation(2 + side, 4, forearm.rotation(), new Quaternionf(), hold), forearm.scale()));
            setRotation(pose, arm, local);
        }

        if (attack > 0) {
            // Vanilla's swing turns the body, and moves each arm on top: the change from the still arm, in the body's
            // root space, is laid on our arm as it was before the chest turned.
            int chest = index(Biped.CHEST);
            setRotation(pose, chest, new Quaternionf(pose.get(chest).rotation()).rotateY(-(swungBody - stillBody)));
            Matrix4f[] after = pose.toModelMatrices(Biped.ARMATURE);
            for (int side = 0; side < 2; side++) {
                int arm = index(armJoints[side]);
                Quaternionf change = new Quaternionf(swung[side]).mul(new Quaternionf(still[side]).conjugate());
                Quaternionf inModel = new Quaternionf(parentBefore[side]).mul(pose.get(arm).rotation());
                Quaternionf parentAfter = after[index(shoulders[side])].getUnnormalizedRotation(new Quaternionf())
                        .normalize();
                setRotation(pose, arm, parentAfter.conjugate().mul(change.mul(inModel)));
            }
        }
        Biped.settleHelpers(pose);
    }

    /** Whether vanilla poses the arm for an item in use or held a special way (not just empty or holding). */
    private static boolean special(HumanoidModel.ArmPose pose) {
        return pose != HumanoidModel.ArmPose.EMPTY && pose != HumanoidModel.ArmPose.ITEM;
    }

    /** A vanilla part's turn relative to the model's root, in our space. */
    private static Quaternionf vanilla(ModelPart part) {
        return BipedPoses.fromVanilla(part.xRot, part.yRot, part.zRot);
    }

    private static int index(String joint) {
        return Biped.ARMATURE.get(joint).index();
    }

    private static void setRotation(Pose pose, int joint, Quaternionf rotation) {
        JointTransform current = pose.get(joint);
        pose.set(joint, JointTransform.of(current.translation(), rotation, current.scale()));
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    private static float ease(float t) {
        return t * t * (3 - 2 * t);
    }
}
