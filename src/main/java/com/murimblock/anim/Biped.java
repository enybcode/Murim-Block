package com.murimblock.anim;

import java.util.List;

/**
 * The humanoid skeleton shared by players and humanoid mobs. At rest it matches the vanilla player: legs from
 * y 0 to 12, body to 24, head to 32, arms hanging from the vanilla shoulder pivot (x ±5, y 22). Bind positions in
 * comments are in model space; the code gives each joint's offset from its parent.
 */
public final class Biped {
    public static final String ROOT = "root";
    public static final String HIPS = "hips";
    public static final String SPINE = "spine";
    public static final String CHEST = "chest";
    public static final String NECK = "neck";
    public static final String HEAD = "head";

    public static final Armature ARMATURE = build();

    private Biped() {
    }

    private static Armature build() {
        Armature.Builder builder = Armature.builder()
                // On the ground between the feet; carries root motion only.
                .root(ROOT, JointTransform.IDENTITY)
                .joint(HIPS, ROOT, JointTransform.translation(0, 12, 0))   // (0, 12, 0)
                .joint(SPINE, HIPS, JointTransform.translation(0, 3, 0))   // (0, 15, 0)
                .joint(CHEST, SPINE, JointTransform.translation(0, 4, 0))  // (0, 19, 0)
                .joint(NECK, CHEST, JointTransform.translation(0, 4, 0))   // (0, 23, 0)
                .joint(HEAD, NECK, JointTransform.translation(0, 1, 0));   // (0, 24, 0)
        for (String side : new String[] {"l", "r"}) {
            float x = side.equals("l") ? 1 : -1;
            builder.joint("shoulder_" + side, CHEST, JointTransform.translation(2 * x, 3, 0))      // (±2, 22, 0)
                    .joint("upper_arm_" + side, "shoulder_" + side, JointTransform.translation(3 * x, 0, 0)) // (±5, 22)
                    .joint("forearm_" + side, "upper_arm_" + side, JointTransform.translation(x, -4, 0))   // (±6, 18)
                    .joint("hand_" + side, "forearm_" + side, JointTransform.translation(0, -5, 0))        // (±6, 13)
                    // The grip, where a held item sits; its orientation is tuned with the renderer (step 3).
                    .joint("tool_" + side, "hand_" + side, JointTransform.translation(0, -1, 0))           // (±6, 12)
                    .joint("thigh_" + side, HIPS, JointTransform.translation(2 * x, 0, 0))                // (±2, 12)
                    .joint("shin_" + side, "thigh_" + side, JointTransform.translation(0, -6, 0))          // (±2, 6)
                    .joint("foot_" + side, "shin_" + side, JointTransform.translation(0, -4, 0));          // (±2, 2)
        }
        return builder.build();
    }

    /** What keeps {@code loaded} (a skeleton read from a model file) from playing biped poses; empty when it fits. */
    public static List<String> check(Armature loaded) {
        return ARMATURE.shapeDifferences(loaded);
    }
}
