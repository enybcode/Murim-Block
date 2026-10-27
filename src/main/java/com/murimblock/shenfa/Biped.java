package com.murimblock.shenfa;

import java.util.List;
import org.joml.Quaternionf;

/**
 * The humanoid skeleton shared by players and humanoid mobs. Its layout follows Epic Fight's biped (same 20 joints,
 * same names and parents; positions rounded to whole pixels), rebuilt by us: their files are not ours to copy.
 * <p>
 * Limbs are rigid blocks. {@code Elbow_*} and {@code Knee_*} are helper joints: the mesh's hinge at the joint hangs
 * from them, and they always turn half as far as the forearm or shin ({@link #settleHelpers}), so the outer corner of
 * a bent limb is filled instead of opening. In Epic Fight's names {@code Arm} is the upper arm, {@code Hand} the
 * forearm with the hand, {@code Leg} the shin. {@code Root} is the pelvis and carries root motion.
 * <p>
 * Model positions at rest are in the comments; the code gives each joint's offset from its parent.
 */
public final class Biped {
    public static final String ROOT = "Root";
    public static final String TORSO = "Torso";
    public static final String CHEST = "Chest";
    public static final String HEAD = "Head";

    public static final Armature ARMATURE = build();

    private static final int[][] HELPERS = helpers();

    private Biped() {
    }

    private static Armature build() {
        Armature.Builder builder = Armature.builder()
                .root(ROOT, JointTransform.translation(0, 12, 0))                  // (0, 12, 0)
                .joint(TORSO, ROOT, JointTransform.translation(0, 1, 0))           // (0, 13, 0)
                .joint(CHEST, TORSO, JointTransform.translation(0, 5, 0))          // (0, 18, 0)
                .joint(HEAD, CHEST, JointTransform.translation(0, 6, 0));          // (0, 24, 0)
        for (String side : new String[] {"L", "R"}) {
            float x = side.equals("L") ? 1 : -1;
            builder.joint("Thigh_" + side, ROOT, JointTransform.translation(2 * x, 0, 0))           // (±2, 12)
                    .joint("Leg_" + side, "Thigh_" + side, JointTransform.translation(0, -6, 0))      // (±2, 6)
                    .joint("Knee_" + side, "Thigh_" + side, JointTransform.translation(0, -6, 0))     // (±2, 6)
                    // The collarbone starts at the top of the chest, in the middle.
                    .joint("Shoulder_" + side, CHEST, JointTransform.translation(0, 6, 0))            // (0, 24)
                    // Vanilla's arm pivot (Epic Fight puts it at the arm's middle, x ±6), so a raised arm matches vanilla.
                    .joint("Arm_" + side, "Shoulder_" + side, JointTransform.translation(5 * x, -2, 0)) // (±5, 22)
                    .joint("Hand_" + side, "Arm_" + side, JointTransform.translation(x, -4, 0))        // (±6, 18)
                    // The grip, where a held item sits; its orientation is tuned with the renderer (step 3).
                    .joint("Tool_" + side, "Hand_" + side, JointTransform.translation(0, -5, 0))       // (±6, 13)
                    .joint("Elbow_" + side, "Arm_" + side, JointTransform.translation(x, -4, 0));      // (±6, 18)
        }
        return builder.build();
    }

    /** Pairs of (helper joint, joint it follows at half the angle). */
    private static int[][] helpers() {
        String[][] names = {{"Elbow_L", "Hand_L"}, {"Elbow_R", "Hand_R"}, {"Knee_L", "Leg_L"}, {"Knee_R", "Leg_R"}};
        int[][] result = new int[names.length][];
        for (int i = 0; i < names.length; i++) {
            result[i] = new int[] {ARMATURE.get(names[i][0]).index(), ARMATURE.get(names[i][1]).index()};
        }
        return result;
    }

    /**
     * Turns each elbow and knee helper half as far as its forearm or shin (relative to their rest), keeping its rest
     * offset. Call it on every biped pose before drawing, after layers are mixed.
     */
    public static void settleHelpers(Pose pose) {
        for (int[] pair : HELPERS) {
            JointTransform follows = pose.get(pair[1]);
            JointTransform rest = ARMATURE.joint(pair[0]).bind();
            Quaternionf half = new Quaternionf().slerp(follows.rotation(), 0.5f);
            pose.set(pair[0], JointTransform.of(rest.translation(), half, rest.scale()));
        }
    }

    /** What keeps {@code loaded} (a skeleton read from a model file) from playing biped poses; empty when it fits. */
    public static List<String> check(Armature loaded) {
        return ARMATURE.shapeDifferences(loaded);
    }
}
