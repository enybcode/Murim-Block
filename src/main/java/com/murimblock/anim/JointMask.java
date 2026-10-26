package com.murimblock.anim;

import java.util.BitSet;

/** The joints a layer of animation drives; the others keep what the layers below gave them. Immutable. */
public final class JointMask {
    private final BitSet joints;

    private JointMask(BitSet joints) {
        this.joints = joints;
    }

    public static JointMask all(Armature armature) {
        BitSet set = new BitSet(armature.size());
        set.set(0, armature.size());
        return new JointMask(set);
    }

    /** These joints only. */
    public static JointMask of(Armature armature, String... names) {
        BitSet set = new BitSet(armature.size());
        for (String name : names) {
            set.set(armature.get(name).index());
        }
        return new JointMask(set);
    }

    /** These joints and every joint below them (for example "chest" takes the arms and the head). */
    public static JointMask withChildren(Armature armature, String... names) {
        BitSet set = new BitSet(armature.size());
        for (String name : names) {
            set.set(armature.get(name).index());
        }
        // Parents come first, so one pass catches every descendant.
        for (Joint joint : armature.joints()) {
            if (!joint.isRoot() && set.get(joint.parent())) {
                set.set(joint.index());
            }
        }
        return new JointMask(set);
    }

    public boolean contains(int index) {
        return joints.get(index);
    }

    /** The joints of this mask that are not in {@code other}. */
    public JointMask without(JointMask other) {
        BitSet set = (BitSet) joints.clone();
        set.andNot(other.joints);
        return new JointMask(set);
    }

    public int count() {
        return joints.cardinality();
    }
}
