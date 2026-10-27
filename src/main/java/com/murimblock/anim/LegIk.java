package com.murimblock.anim;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Two-bone inverse kinematics for a leg: turns the thigh and bends the knee so the sole reaches a point, the knee
 * bending forward as a knee does. Used to keep a planted foot where it stands while the body moves over it.
 */
public final class LegIk {
    private static final float EPSILON = 1e-3f;

    private LegIk() {
    }

    /**
     * Moves {@code thigh} and {@code shin} of {@code pose} (local transforms, changed in place) so the sole reaches
     * {@code target}, by {@code weight} (0 keeps the pose, 1 reaches the target). A target out of reach is reached as
     * far as the leg goes.
     *
     * @param sole   the sole in the shin's space (pixels), for the biped (0, -6, 0)
     * @param target in model space (pixels)
     */
    public static void reach(Armature armature, Pose pose, int thigh, int shin, Vector3fc sole, Vector3fc target,
                             float weight) {
        if (weight <= 0) {
            return;
        }
        Matrix4f[] model = pose.toModelMatrices(armature);
        Vector3f hip = model[thigh].getTranslation(new Vector3f());
        Vector3fc knee = pose.get(shin).translation();
        float upper = knee.length(), lower = sole.length();
        float distance = Math.max(Math.abs(upper - lower) + EPSILON,
                Math.min(upper + lower - EPSILON, new Vector3f(target).sub(hip).length()));

        // The knee: the angle the law of cosines gives for this hip-to-sole distance, bent about the shin's X.
        float cos = (upper * upper + lower * lower - distance * distance) / (2 * upper * lower);
        float bend = (float) (Math.PI - Math.acos(Math.max(-1, Math.min(1, cos))));
        Quaternionf shinTurn = new Quaternionf().rotateX(bend);

        // The thigh: turned in model space so the sole, with the new knee, points at the target.
        Quaternionf thighModel = model[thigh].getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f soleNow = new Vector3f(sole);
        shinTurn.transform(soleNow).add(knee);
        thighModel.transform(soleNow);
        Vector3f wanted = new Vector3f(target).sub(hip);
        Quaternionf swing = new Quaternionf().rotationTo(soleNow.normalize(), wanted.normalize());
        Joint thighJoint = armature.joint(thigh);
        Quaternionf parent = thighJoint.isRoot() ? new Quaternionf()
                : model[thighJoint.parent()].getUnnormalizedRotation(new Quaternionf()).normalize();
        Quaternionf thighLocal = new Quaternionf(parent).conjugate().mul(swing).mul(thighModel);

        JointTransform t = pose.get(thigh), s = pose.get(shin);
        pose.set(thigh, JointTransform.of(t.translation(), new Quaternionf(t.rotation()).slerp(thighLocal, weight),
                t.scale()));
        pose.set(shin, JointTransform.of(s.translation(), new Quaternionf(s.rotation()).slerp(shinTurn, weight),
                s.scale()));
    }

    /** Where the sole is in model space (pixels). */
    public static Vector3f sole(Armature armature, Pose pose, int shin, Vector3fc sole) {
        return pose.toModelMatrices(armature)[shin].transformPosition(new Vector3f(sole));
    }
}
