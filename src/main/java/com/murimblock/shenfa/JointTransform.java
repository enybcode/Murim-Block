package com.murimblock.shenfa;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** One joint's transform relative to its parent: translation (pixels), rotation, scale. Immutable. */
public final class JointTransform {
    public static final JointTransform IDENTITY =
            new JointTransform(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1));

    private final Vector3f translation;
    private final Quaternionf rotation;
    private final Vector3f scale;

    private JointTransform(Vector3f translation, Quaternionf rotation, Vector3f scale) {
        this.translation = translation;
        this.rotation = rotation;
        this.scale = scale;
    }

    public static JointTransform of(Vector3fc translation, Quaternionfc rotation, Vector3fc scale) {
        return new JointTransform(new Vector3f(translation), new Quaternionf(rotation).normalize(),
                new Vector3f(scale));
    }

    public static JointTransform translation(float x, float y, float z) {
        return new JointTransform(new Vector3f(x, y, z), new Quaternionf(), new Vector3f(1, 1, 1));
    }

    public static JointTransform rotation(Quaternionfc rotation) {
        return of(new Vector3f(), rotation, new Vector3f(1, 1, 1));
    }

    public Vector3fc translation() {
        return translation;
    }

    public Quaternionfc rotation() {
        return rotation;
    }

    public Vector3fc scale() {
        return scale;
    }

    /** From {@code a} at {@code t = 0} to {@code b} at {@code t = 1}: lerp for translation and scale, slerp for rotation. */
    public static JointTransform interpolate(JointTransform a, JointTransform b, float t) {
        return new JointTransform(a.translation.lerp(b.translation, t, new Vector3f()),
                a.rotation.slerp(b.rotation, t, new Quaternionf()).normalize(),
                a.scale.lerp(b.scale, t, new Vector3f()));
    }

    /** {@code delta} laid on top of this one: translations add, rotations compose, scales multiply. */
    public JointTransform add(JointTransform delta) {
        return new JointTransform(translation.add(delta.translation, new Vector3f()),
                rotation.mul(delta.rotation, new Quaternionf()).normalize(), scale.mul(delta.scale, new Vector3f()));
    }

    /** Reflected across the body's middle plane (x = 0), so a left joint's motion becomes a right joint's. */
    public JointTransform mirrored() {
        return new JointTransform(new Vector3f(-translation.x, translation.y, translation.z),
                new Quaternionf(rotation.x, -rotation.y, -rotation.z, rotation.w), new Vector3f(scale));
    }

    /** Translation, then rotation, then scale, as a matrix written into {@code dest}. */
    public Matrix4f toMatrix(Matrix4f dest) {
        return dest.translationRotateScale(translation, rotation, scale);
    }

    @Override
    public String toString() {
        return "JointTransform[t=" + translation + ", r=" + rotation + ", s=" + scale + "]";
    }
}
