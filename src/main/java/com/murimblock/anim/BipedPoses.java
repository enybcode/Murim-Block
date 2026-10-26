package com.murimblock.anim;

import java.util.Map;
import org.joml.Quaternionf;

/** Hand-made biped poses until clips exist (step 4): the rest pose, test poses, and the head following the look. */
public final class BipedPoses {
    /** Test poses: degrees about X then Y per joint, on top of the rest. The same as tools/blender/render.py. */
    public static final Map<String, Map<String, float[]>> TEST = Map.of(
            "rest", Map.of(),
            "bend", Map.of(
                    "Arm_L", new float[] {-60, 0}, "Arm_R", new float[] {-30, 0},
                    "Hand_L", new float[] {-150, 0}, "Hand_R", new float[] {-110, 0},
                    "Thigh_L", new float[] {-70, 0}, "Leg_L", new float[] {120, 0},
                    "Thigh_R", new float[] {20, 0}, "Leg_R", new float[] {60, 0},
                    "Chest", new float[] {0, 25}, "Head", new float[] {0, -25}),
            "joints", Map.of("Hand_L", new float[] {-135, 0}, "Leg_L", new float[] {135, 0}),
            // Arms raised forward and elbows bent: what a guard will look like, to check held items.
            "guard", Map.of(
                    "Arm_L", new float[] {-70, -20}, "Hand_L", new float[] {-100, 0},
                    "Arm_R", new float[] {-50, 20}, "Hand_R", new float[] {-110, 0}));

    private BipedPoses() {
    }

    /** A test pose by name, helpers settled; the rest pose for an unknown name. */
    public static Pose test(String name) {
        Pose pose = Biped.ARMATURE.bindPose();
        TEST.getOrDefault(name, Map.of()).forEach((joint, degrees) -> {
            int index = Biped.ARMATURE.get(joint).index();
            Quaternionf rotation = new Quaternionf().rotateX((float) Math.toRadians(degrees[0]))
                    .rotateY((float) Math.toRadians(degrees[1]));
            pose.set(index, pose.get(index).add(JointTransform.rotation(rotation)));
        });
        Biped.settleHelpers(pose);
        return pose;
    }

    /**
     * Vanilla's standing player: arms swaying a little with {@code age} (ticks), and an arm holding an item raised by
     * 18°. Until the idle clip exists (step 5) this is how a player stands, so we look like vanilla at rest.
     */
    public static Pose stand(float age, boolean rightHolds, boolean leftHolds) {
        Pose pose = Biped.ARMATURE.bindPose();
        float sway = (float) Math.cos(age * 0.09f) * 0.05f + 0.05f;
        float swing = (float) Math.sin(age * 0.067f) * 0.05f;
        float raise = (float) (Math.PI / 10);
        vanillaArm(pose, "Arm_R", (rightHolds ? -raise : 0) + swing, sway);
        vanillaArm(pose, "Arm_L", (leftHolds ? -raise : 0) - swing, -sway);
        return pose;
    }

    /**
     * An arm turned like a vanilla model part (radians about X, then Z). Vanilla's model is upside down and faces
     * -Z, a half turn about X from ours: its X turns are ours, its Z turns are reversed.
     */
    private static void vanillaArm(Pose pose, String joint, float x, float z) {
        int index = Biped.ARMATURE.get(joint).index();
        pose.set(index, pose.get(index).add(JointTransform.rotation(new Quaternionf().rotateZ(-z).rotateX(x))));
    }

    /**
     * Turns the head by the look relative to the body: {@code yaw} in Minecraft degrees (positive toward the body's
     * right), {@code pitch} positive looking down.
     */
    public static void look(Pose pose, float yaw, float pitch) {
        int head = Biped.ARMATURE.get(Biped.HEAD).index();
        // The body faces +Z with its left on +X, so turning right is a negative turn about Y.
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch));
        pose.set(head, pose.get(head).add(JointTransform.rotation(rotation)));
    }
}
