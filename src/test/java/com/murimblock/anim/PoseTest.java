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
        Pose pose = turned(BIPED.bindPose(), "Arm_L", new Quaternionf().rotateX(-QUARTER));
        // Rest offsets from the shoulder (6, 22, 0): elbow (0, -4, 0), grip (0, -9, 0).
        assertNear(new Vector3f(6, 22, 4), modelPosition(pose, "Hand_L"));
        assertNear(new Vector3f(6, 22, 9), modelPosition(pose, "Tool_L"));
    }

    @Test
    void bendingTheElbowMovesOnlyWhatHangsBelow() {
        Pose pose = turned(BIPED.bindPose(), "Hand_L", new Quaternionf().rotateX(-QUARTER));
        assertNear(new Vector3f(6, 18, 0), modelPosition(pose, "Hand_L"));
        assertNear(new Vector3f(6, 18, 5), modelPosition(pose, "Tool_L"));
        assertNear(new Vector3f(-6, 13, 0), modelPosition(pose, "Tool_R"));
    }

    @Test
    void helpersTurnHalfAsFar() {
        Pose pose = turned(turned(BIPED.bindPose(), "Hand_L", new Quaternionf().rotateX(-QUARTER)),
                "Leg_R", new Quaternionf().rotateX(QUARTER));
        Biped.settleHelpers(pose);
        assertTrue(new Quaternionf(pose.get(index("Elbow_L")).rotation()).equals(new Quaternionf().rotateX(-QUARTER / 2), EPS));
        assertTrue(new Quaternionf(pose.get(index("Knee_R")).rotation()).equals(new Quaternionf().rotateX(QUARTER / 2), EPS));
        assertTrue(new Quaternionf(pose.get(index("Elbow_R")).rotation()).equals(new Quaternionf(), EPS));
        assertNear(modelPosition(pose, "Hand_L"), modelPosition(pose, "Elbow_L"));
    }

    @Test
    void interpolationSlerpsHalfway() {
        Pose rest = BIPED.bindPose();
        Pose raised = turned(rest, "Arm_L", new Quaternionf().rotateX(-QUARTER));
        Pose half = Pose.interpolate(rest, raised, 0.5f);
        Quaternionf expected = new Quaternionf().rotateX(-QUARTER / 2);
        assertTrue(new Quaternionf(half.get(index("Arm_L")).rotation()).equals(expected, EPS));
        // Endpoints are the poses themselves.
        assertNear(modelPosition(raised, "Tool_L"), modelPosition(Pose.interpolate(rest, raised, 1), "Tool_L"));
        assertNear(modelPosition(rest, "Tool_L"), modelPosition(Pose.interpolate(rest, raised, 0), "Tool_L"));
    }

    @Test
    void mirroringSwapsSides() {
        // Left arm raised forward and out to the side (+X), so both rotation axes are checked.
        Quaternionf lift = new Quaternionf().rotateX(-QUARTER).rotateZ(0.4f);
        Pose left = turned(BIPED.bindPose(), "Arm_L", lift);
        Pose right = left.mirrored(BIPED);
        Vector3f leftGrip = modelPosition(left, "Tool_L");
        assertNear(new Vector3f(-leftGrip.x, leftGrip.y, leftGrip.z), modelPosition(right, "Tool_R"));
        assertNear(modelPosition(BIPED.bindPose(), "Tool_L"), modelPosition(right, "Tool_L"));
        // Twice gives the original back.
        assertNear(leftGrip, modelPosition(right.mirrored(BIPED), "Tool_L"));
    }

    @Test
    void overwriteKeepsJointsOutsideTheMask() {
        Pose rest = BIPED.bindPose();
        Pose raised = turned(turned(rest, "Arm_L", new Quaternionf().rotateX(-QUARTER)),
                "Thigh_L", new Quaternionf().rotateX(-QUARTER));
        Pose mixed = rest.overwrite(raised, JointMask.withChildren(BIPED, "Chest"));
        assertSame(raised.get(index("Arm_L")), mixed.get(index("Arm_L")));
        assertSame(rest.get(index("Thigh_L")), mixed.get(index("Thigh_L")));
    }

    @Test
    void additiveLayerStacksOnTop() {
        Pose delta = new Pose(BIPED.size());
        delta.set(index("Chest"), JointTransform.rotation(new Quaternionf().rotateY(0.3f)));
        Pose base = turned(BIPED.bindPose(), "Chest", new Quaternionf().rotateY(0.2f));
        Pose stacked = base.add(delta, JointMask.all(BIPED));
        Quaternionf expected = new Quaternionf().rotateY(0.5f);
        assertTrue(new Quaternionf(stacked.get(index("Chest")).rotation()).equals(expected, EPS));
        assertNear(modelPosition(base, "Thigh_R"), modelPosition(stacked, "Thigh_R"));
    }

    @Test
    void skinMatricesFollowTheJoint() {
        // A vertex at the left grip's rest position goes where the grip goes.
        Pose pose = turned(BIPED.bindPose(), "Arm_L", new Quaternionf().rotateX(-QUARTER));
        Vector3f vertex = modelPosition(BIPED.bindPose(), "Tool_L");
        Vector3f moved = pose.toSkinMatrices(BIPED)[index("Tool_L")].transformPosition(vertex, new Vector3f());
        assertNear(modelPosition(pose, "Tool_L"), moved);
    }
}
