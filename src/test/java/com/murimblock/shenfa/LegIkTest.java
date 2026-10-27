package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class LegIkTest {
    private static final Vector3f SOLE = new Vector3f(0, -6, 0);
    private static final int THIGH = Biped.ARMATURE.get("Thigh_L").index();
    private static final int SHIN = Biped.ARMATURE.get("Leg_L").index();

    private static Vector3f reach(Pose pose, Vector3f target, float weight) {
        LegIk.reach(Biped.ARMATURE, pose, THIGH, SHIN, SOLE, target, weight);
        return LegIk.sole(Biped.ARMATURE, pose, SHIN, SOLE);
    }

    @Test
    void theSoleReachesPointsAroundTheHip() {
        // The hip is at (2, 12, 0); the leg is 12 px long.
        for (Vector3f target : new Vector3f[] {new Vector3f(2, 1, 4), new Vector3f(2, 2, -5), new Vector3f(3, 1, 2),
                new Vector3f(2, 4, 0)}) {
            Vector3f sole = reach(Biped.ARMATURE.bindPose(), target, 1);
            assertTrue(sole.distance(target) < 0.01f, target + " reached " + sole);
        }
    }

    @Test
    void theKneeBendsForward() {
        Pose pose = Biped.ARMATURE.bindPose();
        reach(pose, new Vector3f(2, 4, 0), 1);
        // A positive turn about X bends the shin back, the knee forward.
        Quaternionf shin = new Quaternionf(pose.get(SHIN).rotation());
        assertTrue(shin.x > 0, shin.toString());
        Vector3f knee = Biped.ARMATURE.bindPose().toModelMatrices(Biped.ARMATURE)[SHIN].getTranslation(new Vector3f());
        Vector3f bent = pose.toModelMatrices(Biped.ARMATURE)[SHIN].getTranslation(new Vector3f());
        assertTrue(bent.z > knee.z + 1, "knee " + bent);
    }

    @Test
    void outOfReachStretchesTowardTheTarget() {
        Vector3f sole = reach(Biped.ARMATURE.bindPose(), new Vector3f(2, -10, 8), 1);
        Vector3f hip = new Vector3f(2, 12, 0);
        assertEquals(12, sole.distance(hip), 0.01f);
        Vector3f toTarget = new Vector3f(0, -22, 8).normalize();
        assertTrue(new Vector3f(sole).sub(hip).normalize().dot(toTarget) > 0.999f);
    }

    @Test
    void halfWeightGoesHalfWay() {
        Pose pose = Biped.ARMATURE.bindPose();
        Vector3f sole = reach(pose, new Vector3f(2, 0.5f, 6), 0.5f);
        Vector3f rest = new Vector3f(2, 0, 0);
        assertTrue(sole.distance(rest) > 1 && sole.distance(new Vector3f(2, 0.5f, 6)) > 1, sole.toString());
        Pose untouched = Biped.ARMATURE.bindPose();
        assertTrue(reach(untouched, new Vector3f(2, 0.5f, 6), 0).distance(rest) < 1e-4f);
    }

    @Test
    void turnedLegsLeaveTheChestFacingAhead() {
        Pose pose = BipedPoses.test("bend");
        int chest = Biped.ARMATURE.get(Biped.CHEST).index();
        var before = pose.toModelMatrices(Biped.ARMATURE);
        BipedPoses.turnLegs(pose, (float) Math.toRadians(60));
        var after = pose.toModelMatrices(Biped.ARMATURE);
        assertTrue(after[chest].equals(before[chest], 1e-4f), "the chest stays");
        // The left knee (ahead and to the left of the body at rest) swings toward the body's left with the hips.
        Vector3f thigh = after[THIGH].getTranslation(new Vector3f()), was = before[THIGH].getTranslation(new Vector3f());
        assertTrue(thigh.z < was.z, thigh + " vs " + was);
    }
}
