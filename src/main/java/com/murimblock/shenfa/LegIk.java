package com.murimblock.shenfa;

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
    private static final float MOST_BEND = (float) Math.toRadians(160);

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
        // A knee never folds all the way (and a leg folded flat has no stable direction).
        float bend = (float) Math.min(MOST_BEND, Math.PI - Math.acos(Math.max(-1, Math.min(1, cos))));
        Quaternionf shinTurn = new Quaternionf().rotateX(bend);

        // The thigh: turned in model space so the sole, with the new knee, points at the target.
        Quaternionf thighModel = model[thigh].getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f soleNow = new Vector3f(sole);
        shinTurn.transform(soleNow).add(knee);
        thighModel.transform(soleNow);
        Vector3f wanted = new Vector3f(target).sub(hip);
        // First about the knee's hinge axis (a signed angle: a leg swung far back or up never flips to the other
        // side), then the small sideways rest.
        Vector3f axis = thighModel.transform(new Vector3f(1, 0, 0));
        Vector3f from = new Vector3f(soleNow).sub(new Vector3f(axis).mul(soleNow.dot(axis)));
        Vector3f to = new Vector3f(wanted).sub(new Vector3f(axis).mul(wanted.dot(axis)));
        float angle = (float) Math.atan2(new Vector3f(from).cross(to).dot(axis), from.dot(to));
        Quaternionf hinge = new Quaternionf().fromAxisAngleRad(axis, angle);
        Vector3f hinged = hinge.transform(new Vector3f(soleNow)).normalize();
        Quaternionf swing = new Quaternionf().rotationTo(hinged, wanted.normalize()).mul(hinge);
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
