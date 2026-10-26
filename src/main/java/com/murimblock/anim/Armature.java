package com.murimblock.anim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * A skeleton: joints listed parent first, so one pass from the start computes every joint's model transform.
 * Model space is in pixels (1/16 block), Y up, the body facing +Z, its left side on +X, the root on the ground
 * between the feet.
 */
public final class Armature {
    private final List<Joint> joints;
    private final Map<String, Joint> byName;
    /** For each joint, its mirror (a "_l" joint's "_r" twin), or itself for a joint in the middle. */
    private final int[] mirrors;
    /** For each joint, the inverse of its model transform at rest: moves a rest-pose vertex into joint space. */
    private final Matrix4f[] inverseBinds;

    private Armature(List<Joint> joints) {
        this.joints = Collections.unmodifiableList(joints);
        Map<String, Joint> names = new HashMap<>();
        for (Joint joint : joints) {
            names.put(joint.name(), joint);
        }
        byName = Collections.unmodifiableMap(names);
        mirrors = new int[joints.size()];
        for (Joint joint : joints) {
            Joint twin = names.get(mirrorName(joint.name()));
            mirrors[joint.index()] = twin == null ? joint.index() : twin.index();
        }
        Matrix4f[] model = bindPose().toModelMatrices(this);
        inverseBinds = new Matrix4f[model.length];
        for (int i = 0; i < model.length; i++) {
            inverseBinds[i] = model[i].invert(new Matrix4f());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String mirrorName(String name) {
        if (name.endsWith("_l")) {
            return name.substring(0, name.length() - 2) + "_r";
        }
        if (name.endsWith("_r")) {
            return name.substring(0, name.length() - 2) + "_l";
        }
        return name;
    }

    public int size() {
        return joints.size();
    }

    public List<Joint> joints() {
        return joints;
    }

    public Joint joint(int index) {
        return joints.get(index);
    }

    /** The joint with this name, or null. */
    public Joint find(String name) {
        return byName.get(name);
    }

    public Joint get(String name) {
        Joint joint = byName.get(name);
        if (joint == null) {
            throw new IllegalArgumentException("No joint named " + name);
        }
        return joint;
    }

    public int mirrorOf(int index) {
        return mirrors[index];
    }

    public Matrix4fc inverseBind(int index) {
        return inverseBinds[index];
    }

    /** The rest pose: every joint at its bind transform. */
    public Pose bindPose() {
        Pose pose = new Pose(size());
        for (Joint joint : joints) {
            pose.set(joint.index(), joint.bind());
        }
        return pose;
    }

    /**
     * What differs between this armature and {@code other} in joint names, order or parents (bind transforms may
     * differ). Empty when a pose made for one fits the other.
     */
    public List<String> shapeDifferences(Armature other) {
        List<String> problems = new ArrayList<>();
        if (size() != other.size()) {
            problems.add("expected " + size() + " joints, found " + other.size());
        }
        for (Joint joint : joints) {
            Joint theirs = other.find(joint.name());
            if (theirs == null) {
                problems.add("missing joint " + joint.name());
                continue;
            }
            String parent = joint.isRoot() ? "none" : joint(joint.parent()).name();
            String theirParent = theirs.isRoot() ? "none" : other.joint(theirs.parent()).name();
            if (!parent.equals(theirParent)) {
                problems.add(joint.name() + " should hang from " + parent + ", not " + theirParent);
            }
            if (joint.index() != theirs.index()) {
                problems.add(joint.name() + " should be joint " + joint.index() + ", not " + theirs.index());
            }
        }
        return problems;
    }

    /** Joints added in order; a parent must be added before its children. */
    public static final class Builder {
        private final List<Joint> joints = new ArrayList<>();
        private final Map<String, Integer> indices = new HashMap<>();

        private Builder() {
        }

        public Builder root(String name, JointTransform bind) {
            return add(name, -1, bind);
        }

        public Builder joint(String name, String parent, JointTransform bind) {
            Integer parentIndex = indices.get(parent);
            if (parentIndex == null) {
                throw new IllegalArgumentException("Joint " + name + ": parent " + parent + " is not added yet");
            }
            return add(name, parentIndex, bind);
        }

        private Builder add(String name, int parent, JointTransform bind) {
            if (indices.containsKey(name)) {
                throw new IllegalArgumentException("Two joints named " + name);
            }
            if (parent < 0 && !joints.isEmpty()) {
                throw new IllegalArgumentException("Joint " + name + ": an armature has a single root");
            }
            indices.put(name, joints.size());
            joints.add(new Joint(name, joints.size(), parent, bind));
            return this;
        }

        public Armature build() {
            if (joints.isEmpty()) {
                throw new IllegalStateException("An armature needs at least one joint");
            }
            return new Armature(new ArrayList<>(joints));
        }
    }
}
