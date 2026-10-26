package com.murimblock.anim;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PoseTest {
    private static final float EPS = 1e-4f;
    private static final Armature BIPED = Biped.ARMATURE;
    private static final float QUARTER = (float) Math.PI / 2;

    private static int index(String joint) {
        return BIPED.get(joint).index();
    }

    private static Vector3f modelPosition(Pose pose, String joint) {
        return pose.toModelMatrices(BIPED)[index(joint)].getTranslation(new Vector3f());
    }

    /** {@code pose} with {@code joint} turned by {@code rotation} on top of its rest transform. */
    private static Pose turned(Pose pose, String joint, Quaternionf rotation) {
        Pose result = pose.copy();
        result.set(index(joint), pose.get(index(joint)).add(JointTransform.rotation(rotation)));
        return result;
    }

    private static void assertNear(Vector3f expected, Vector3f actual) {
        assertTrue(expected.distance(actual) < EPS, "expected " + expected + ", got " + actual);
    }

    @Test
    void raisingTheArmForwardMovesTheHandForward() {
        // Turning the upper arm -90° about X swings it from hanging down to pointing forward (+Z).
        Pose pose = turned(BIPED.bindPose(), "upper_arm_l", new Quaternionf().rotateX(-QUARTER));
        // Rest offsets from the shoulder pivot (5, 22, 0): elbow (1, -4, 0), hand (1, -9, 0).
        assertNear(new Vector3f(6, 22, 4), modelPosition(pose, "forearm_l"));
        assertNear(new Vector3f(6, 22, 9), modelPosition(pose, "hand_l"));
    }

    @Test
    void bendingTheElbowMovesOnlyWhatHangsBelow() {
        Pose pose = turned(BIPED.bindPose(), "forearm_l", new Quaternionf().rotateX(-QUARTER));
        assertNear(new Vector3f(6, 18, 0), modelPosition(pose, "forearm_l"));
        assertNear(new Vector3f(6, 18, 5), modelPosition(pose, "hand_l"));
        assertNear(new Vector3f(-6, 13, 0), modelPosition(pose, "hand_r"));
    }

    @Test
    void interpolationSlerpsHalfway() {
        Pose rest = BIPED.bindPose();
        Pose raised = turned(rest, "upper_arm_l", new Quaternionf().rotateX(-QUARTER));
        Pose half = Pose.interpolate(rest, raised, 0.5f);
        Quaternionf expected = new Quaternionf().rotateX(-QUARTER / 2);
        assertTrue(new Quaternionf(half.get(index("upper_arm_l")).rotation()).equals(expected, EPS));
        // Endpoints are the poses themselves.
        assertNear(modelPosition(raised, "hand_l"), modelPosition(Pose.interpolate(rest, raised, 1), "hand_l"));
        assertNear(modelPosition(rest, "hand_l"), modelPosition(Pose.interpolate(rest, raised, 0), "hand_l"));
    }

    @Test
    void mirroringSwapsSides() {
        // Left arm raised forward and out to the side (+X), so both rotation axes are checked.
        Quaternionf lift = new Quaternionf().rotateX(-QUARTER).rotateZ(0.4f);
        Pose left = turned(BIPED.bindPose(), "upper_arm_l", lift);
        Pose right = left.mirrored(BIPED);
        Vector3f leftHand = modelPosition(left, "hand_l");
        assertNear(new Vector3f(-leftHand.x, leftHand.y, leftHand.z), modelPosition(right, "hand_r"));
        assertNear(modelPosition(BIPED.bindPose(), "hand_l"), modelPosition(right, "hand_l"));
        // Twice gives the original back.
        assertNear(leftHand, modelPosition(right.mirrored(BIPED), "hand_l"));
    }

    @Test
    void overwriteKeepsJointsOutsideTheMask() {
        Pose rest = BIPED.bindPose();
        Pose raised = turned(turned(rest, "upper_arm_l", new Quaternionf().rotateX(-QUARTER)),
                "thigh_l", new Quaternionf().rotateX(-QUARTER));
        Pose mixed = rest.overwrite(raised, JointMask.withChildren(BIPED, "chest"));
        assertSame(raised.get(index("upper_arm_l")), mixed.get(index("upper_arm_l")));
        assertSame(rest.get(index("thigh_l")), mixed.get(index("thigh_l")));
    }

    @Test
    void additiveLayerStacksOnTop() {
        Pose rest = BIPED.bindPose();
        Pose delta = new Pose(BIPED.size());
        delta.set(index("chest"), JointTransform.rotation(new Quaternionf().rotateY(0.3f)));
        Pose base = turned(rest, "chest", new Quaternionf().rotateY(0.2f));
        Pose stacked = base.add(delta, JointMask.all(BIPED));
        Quaternionf expected = new Quaternionf().rotateY(0.5f);
        assertTrue(new Quaternionf(stacked.get(index("chest")).rotation()).equals(expected, EPS));
        assertNear(modelPosition(base, "thigh_r"), modelPosition(stacked, "thigh_r"));
    }

    @Test
    void skinMatricesFollowTheJoint() {
        // A vertex at the left hand's rest position goes where the hand goes.
        Pose pose = turned(BIPED.bindPose(), "upper_arm_l", new Quaternionf().rotateX(-QUARTER));
        Vector3f vertex = modelPosition(BIPED.bindPose(), "hand_l");
        Vector3f moved = pose.toSkinMatrices(BIPED)[index("hand_l")].transformPosition(vertex, new Vector3f());
        assertNear(modelPosition(pose, "hand_l"), moved);
    }
}
