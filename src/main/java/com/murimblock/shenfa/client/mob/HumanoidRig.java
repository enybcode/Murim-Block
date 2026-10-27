package com.murimblock.shenfa.client.mob;

import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.BipedPoses;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.Pose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * A humanoid mob (zombie, skeleton, piglin, enderman...) on the biped skeleton, so it plays the same clips as the
 * player. Vanilla's six rigid parts are read into the biped (head, chest, upper arms and thighs, the rest straight),
 * and written back from it: each part pivots where its joint stands and turns with it; an arm or a leg points from
 * its pivot to the hand or foot, so a bent elbow still sends the rigid arm where the fist goes.
 */
public final class HumanoidRig implements MobRig {
    /** A humanoid model's parts: vanilla's fields. */
    public static final HumanoidRig HUMANOID = new HumanoidRig(model -> {
        HumanoidModel<?> humanoid = (HumanoidModel<?>) model;
        return new Parts(humanoid.head, humanoid.hat, humanoid.body, humanoid.leftArm, humanoid.rightArm,
                humanoid.leftLeg, humanoid.rightLeg, null);
    });
    /** An illager's parts (vindicator, pillager, evoker, illusioner): its crossed arms give way to both arms. */
    public static final HumanoidRig ILLAGER = new HumanoidRig(model -> {
        ModelPart root = ((IllagerModel<?>) model).root();
        return new Parts(root.getChild("head"), null, root.getChild("body"), root.getChild("left_arm"),
                root.getChild("right_arm"), root.getChild("left_leg"), root.getChild("right_leg"),
                root.getChild("arms"));
    });
    private static final Armature BIPED = Biped.ARMATURE;
    private static final int HEAD = index(Biped.HEAD), CHEST = index(Biped.CHEST);
    private static final int ARM_L = index("Arm_L"), ARM_R = index("Arm_R"), TOOL_L = index("Tool_L"),
            TOOL_R = index("Tool_R"), THIGH_L = index("Thigh_L"), THIGH_R = index("Thigh_R"), LEG_L = index("Leg_L"),
            LEG_R = index("Leg_R");
    /** The shin's length to the sole (pixels). */
    private static final float SHIN = 6;

    /** The six parts a humanoid is drawn with, the hat that copies the head, and arms crossed at rest. */
    private record Parts(ModelPart head, ModelPart hat, ModelPart body, ModelPart leftArm, ModelPart rightArm,
                         ModelPart leftLeg, ModelPart rightLeg, ModelPart crossedArms) {
    }

    private final Function<EntityModel<?>, Parts> parts;

    private HumanoidRig(Function<EntityModel<?>, Parts> parts) {
        this.parts = parts;
    }

    private static int index(String name) {
        return BIPED.get(name).index();
    }

    @Override
    public Armature armature() {
        return BIPED;
    }

    @Override
    public boolean biped() {
        return true;
    }

    @Override
    public Pose capture(EntityModel<?> model) {
        Parts humanoid = parts.apply(model);
        Pose pose = BIPED.bindPose();
        Quaternionf chest = vanilla(humanoid.body);
        Quaternionf toChest = new Quaternionf(chest).conjugate();
        turn(pose, CHEST, chest);
        turn(pose, HEAD, new Quaternionf(toChest).mul(vanilla(humanoid.head)));
        turn(pose, ARM_L, new Quaternionf(toChest).mul(vanilla(humanoid.leftArm)));
        turn(pose, ARM_R, new Quaternionf(toChest).mul(vanilla(humanoid.rightArm)));
        turn(pose, THIGH_L, vanilla(humanoid.leftLeg));
        turn(pose, THIGH_R, vanilla(humanoid.rightLeg));
        Biped.settleHelpers(pose);
        return pose;
    }

    @Override
    public void apply(EntityModel<?> model, Pose pose, Pose captured) {
        Parts humanoid = parts.apply(model);
        Matrix4f[] joints = pose.toModelMatrices(BIPED);
        Matrix4f[] rest = BIPED.bindPose().toModelMatrices(BIPED);
        Matrix4f[] before = captured.toModelMatrices(BIPED);
        // Only parts whose joints the layers moved are written: the others keep vanilla's exact pose.
        if (changed(joints, before, HEAD)) {
            set(humanoid.head, at(joints[HEAD]), rotation(joints[HEAD]));
            if (humanoid.hat != null) {
                humanoid.hat.copyFrom(humanoid.head);
            }
        }
        if (changed(joints, before, CHEST)) {
            // The body hangs from the neck, where the head pivots.
            set(humanoid.body, at(joints[HEAD]), rotation(joints[CHEST]));
        }
        limb(humanoid.leftArm, joints, rest, before, ARM_L, tip(joints, TOOL_L), tip(rest, TOOL_L));
        limb(humanoid.rightArm, joints, rest, before, ARM_R, tip(joints, TOOL_R), tip(rest, TOOL_R));
        limb(humanoid.leftLeg, joints, rest, before, THIGH_L, sole(joints, LEG_L), sole(rest, LEG_L));
        limb(humanoid.rightLeg, joints, rest, before, THIGH_R, sole(joints, LEG_R), sole(rest, LEG_R));
        if (humanoid.crossedArms != null && (changed(joints, before, ARM_L) || changed(joints, before, ARM_R))) {
            // A strike needs both arms free: the crossed arms give way, as when an illager attacks.
            humanoid.crossedArms.visible = false;
            humanoid.leftArm.visible = true;
            humanoid.rightArm.visible = true;
        }
        if (model instanceof PlayerModel<?> player) {
            player.leftSleeve.copyFrom(player.leftArm);
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftPants.copyFrom(player.leftLeg);
            player.rightPants.copyFrom(player.rightLeg);
            player.jacket.copyFrom(player.body);
        }
    }

    /** A rigid limb from {@code joint}, turned so the line from its pivot to its end points where the end is now. */
    private static void limb(ModelPart part, Matrix4f[] joints, Matrix4f[] rest, Matrix4f[] before, int joint,
                             Vector3f end, Vector3f restEnd) {
        if (!changed(joints, before, joint) && end.distanceSquared(endOf(before, joint, joints)) < 1e-6f) {
            return;
        }
        Vector3f pivot = joints[joint].getTranslation(new Vector3f());
        Quaternionf turn = joints[joint].getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f restLine = restEnd.sub(rest[joint].getTranslation(new Vector3f()), new Vector3f()).normalize();
        Vector3f line = turn.transform(new Vector3f(restLine));
        Vector3f wanted = new Vector3f(end).sub(pivot);
        if (wanted.lengthSquared() > 1e-6f) {
            turn = new Quaternionf().rotationTo(line, wanted.normalize()).mul(turn);
        }
        set(part, at(joints[joint]), toVanilla(turn));
    }

    /** The limb's end in {@code before}, matched by joint: the hand tip for an arm, the sole for a leg. */
    private static Vector3f endOf(Matrix4f[] before, int joint, Matrix4f[] joints) {
        return joint == ARM_L ? tip(before, TOOL_L) : joint == ARM_R ? tip(before, TOOL_R)
                : joint == THIGH_L ? sole(before, LEG_L) : sole(before, LEG_R);
    }

    private static boolean changed(Matrix4f[] a, Matrix4f[] b, int joint) {
        return !a[joint].equals(b[joint], 1e-5f);
    }

    private static Vector3f tip(Matrix4f[] joints, int tool) {
        return joints[tool].getTranslation(new Vector3f());
    }

    private static Vector3f sole(Matrix4f[] joints, int shin) {
        return joints[shin].transformPosition(new Vector3f(0, -SHIN, 0));
    }

    /** Our model space to vanilla's: the same point, vanilla's Y down from 24 pixels up and Z the other way. */
    private static Vector3f at(Matrix4f joint) {
        Vector3f at = joint.getTranslation(new Vector3f());
        return new Vector3f(at.x, 24 - at.y, -at.z);
    }

    private static Quaternionf rotation(Matrix4f joint) {
        return toVanilla(joint.getUnnormalizedRotation(new Quaternionf()).normalize());
    }

    /** Vanilla's turn of a part (in our space) from its X, Y and Z turns. */
    private static Quaternionf vanilla(ModelPart part) {
        return BipedPoses.fromVanilla(part.xRot, part.yRot, part.zRot);
    }

    /** The other way: a turn in our space as vanilla's (a half turn about X between the two). */
    private static Quaternionf toVanilla(Quaternionfc ours) {
        Vector3f euler = new Quaternionf(ours).getEulerAnglesZYX(new Vector3f());
        return new Quaternionf().rotationZYX(-euler.z, -euler.y, euler.x);
    }

    private static void set(ModelPart part, Vector3f at, Quaternionf vanillaTurn) {
        part.x = at.x;
        part.y = at.y;
        part.z = at.z;
        Vector3f euler = vanillaTurn.getEulerAnglesZYX(new Vector3f());
        part.xRot = euler.x;
        part.yRot = euler.y;
        part.zRot = euler.z;
    }

    private static void turn(Pose pose, int joint, Quaternionf rotation) {
        JointTransform rest = pose.get(joint);
        pose.set(joint, JointTransform.of(rest.translation(), rotation, rest.scale()));
    }
}
