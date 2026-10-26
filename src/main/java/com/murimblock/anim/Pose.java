package com.murimblock.anim;

import java.util.Arrays;
import org.joml.Matrix4f;

/** One transform per joint of an armature, relative to each joint's parent. */
public final class Pose {
    private final JointTransform[] transforms;

    public Pose(int size) {
        transforms = new JointTransform[size];
        Arrays.fill(transforms, JointTransform.IDENTITY);
    }

    private Pose(JointTransform[] transforms) {
        this.transforms = transforms;
    }

    public int size() {
        return transforms.length;
    }

    public JointTransform get(int index) {
        return transforms[index];
    }

    public void set(int index, JointTransform transform) {
        transforms[index] = transform;
    }

    public Pose copy() {
        return new Pose(transforms.clone());
    }

    /** From {@code a} at {@code t = 0} to {@code b} at {@code t = 1}, joint by joint. */
    public static Pose interpolate(Pose a, Pose b, float t) {
        Pose result = new Pose(a.size());
        for (int i = 0; i < a.size(); i++) {
            result.transforms[i] = JointTransform.interpolate(a.transforms[i], b.transforms[i], t);
        }
        return result;
    }

    /** A copy where the joints of {@code mask} take {@code top}'s transforms. */
    public Pose overwrite(Pose top, JointMask mask) {
        Pose result = copy();
        for (int i = 0; i < size(); i++) {
            if (mask.contains(i)) {
                result.transforms[i] = top.transforms[i];
            }
        }
        return result;
    }

    /** A copy where the joints of {@code mask} get {@code delta}'s transforms added on top (additive layer). */
    public Pose add(Pose delta, JointMask mask) {
        Pose result = copy();
        for (int i = 0; i < size(); i++) {
            if (mask.contains(i)) {
                result.transforms[i] = transforms[i].add(delta.transforms[i]);
            }
        }
        return result;
    }

    /** The same motion with left and right swapped: a left jab becomes a right one. */
    public Pose mirrored(Armature armature) {
        Pose result = new Pose(size());
        for (int i = 0; i < size(); i++) {
            result.transforms[armature.mirrorOf(i)] = transforms[i].mirrored();
        }
        return result;
    }

    /** Each joint's transform in model space, parents applied. */
    public Matrix4f[] toModelMatrices(Armature armature) {
        Matrix4f[] model = new Matrix4f[size()];
        for (Joint joint : armature.joints()) {
            Matrix4f local = transforms[joint.index()].toMatrix(new Matrix4f());
            model[joint.index()] = joint.isRoot() ? local : new Matrix4f(model[joint.parent()]).mul(local);
        }
        return model;
    }

    /** For skinning: each joint's matrix that moves a rest-pose vertex to where this pose puts it. */
    public Matrix4f[] toSkinMatrices(Armature armature) {
        Matrix4f[] model = toModelMatrices(armature);
        for (int i = 0; i < model.length; i++) {
            model[i].mul(armature.inverseBind(i));
        }
        return model;
    }
}
