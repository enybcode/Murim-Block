package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Feet on uneven ground, and standing feet while the body turns. */
class FootPlanterTest {
    private static final float TO_BLOCKS = 0.9375f / 16;
    private static final float FRAME = 1 / 60f;
    private static final int LEG_L = Biped.ARMATURE.get("Leg_L").index();
    private static final int LEG_R = Biped.ARMATURE.get("Leg_R").index();
    private static final Vector3f AHEAD = new Vector3f(0, 0, 1);

    private static Pose standing() {
        Pose pose = Biped.ARMATURE.bindPose();
        Biped.settleHelpers(pose);
        return pose;
    }

    private static Vector3f sole(Pose pose, int shin) {
        return LegIk.sole(Biped.ARMATURE, pose, shin, FootPlanter.SOLE);
    }

    @Test
    void eachFootStandsOnTheGroundUnderIt() {
        FootPlanter planter = new FootPlanter();
        // The body stands at y 10 on a step: the left foot (x > 0) on it, the right one a fifth of a block lower.
        FootPlanter.Ground ground = (x, z, near) -> x > 0 ? 10 : 9.8f;
        Vector3f at = new Vector3f(0, 10, 0);
        Pose pose = null;
        for (int frame = 0; frame < 30; frame++) {
            pose = standing();
            planter.apply(pose, at, 0, TO_BLOCKS, FRAME, FootPlanter.Mode.STAND, AHEAD, ground);
        }
        assertEquals(0, sole(pose, LEG_L).y, 0.05f);
        assertEquals(-0.2f / TO_BLOCKS, sole(pose, LEG_R).y, 0.05f);
        // The hips sank (as far as a standing body does) to let the right foot reach down.
        float hips = pose.get(Biped.ARMATURE.get(Biped.ROOT).index()).translation().y();
        assertTrue(hips < 12 - 3, "hips at " + hips);
    }

    @Test
    void aSwingingFootNeverGoesThroughTheGround() {
        Animation walk = new Animation("walk", stride(), true, 0.1f, JointMask.all(Biped.ARMATURE));
        FootPlanter planter = new FootPlanter();
        // Ground a third of a block above the body's feet everywhere ahead (a step up).
        FootPlanter.Ground ground = (x, z, near) -> z > 0.2f ? near + 0.3f : near;
        for (int frame = 0; frame < 120; frame++) {
            float t = frame * FRAME;
            Pose pose = walk.clip().sample(t % walk.clip().duration(), Biped.ARMATURE.bindPose());
            Biped.settleHelpers(pose);
            Vector3f at = new Vector3f(0, 5, 0);
            planter.apply(pose, at, 0, TO_BLOCKS, FRAME, FootPlanter.Mode.STEP, AHEAD, ground);
            for (int shin : new int[] {LEG_L, LEG_R}) {
                Vector3f sole = sole(pose, shin);
                float floor = sole.z * TO_BLOCKS > 0.2f ? 0.3f / TO_BLOCKS : 0;
                assertTrue(sole.y > floor - 0.2f, "frame " + frame + ": sole " + sole + " under " + floor);
            }
        }
    }

    /** A plain stride: the left leg swings forward and back through the ground line. */
    private static Clip stride() {
        int thigh = Biped.ARMATURE.get("Thigh_L").index();
        org.joml.Quaternionf ahead = new org.joml.Quaternionf().rotateX((float) Math.toRadians(-40));
        org.joml.Quaternionf behind = new org.joml.Quaternionf().rotateX((float) Math.toRadians(30));
        return new Clip("stride", java.util.List.of(new Clip.Channel(thigh, Clip.Path.ROTATION,
                Clip.Interpolation.LINEAR, new float[] {0, 0.4f, 0.8f}, new float[] {
                    behind.x, behind.y, behind.z, behind.w, ahead.x, ahead.y, ahead.z, ahead.w,
                    behind.x, behind.y, behind.z, behind.w})));
    }

    @Test
    void standingFeetStayPutWhileTheBodyTurnsAndStepOneAtATime() {
        FootPlanter planter = new FootPlanter();
        Vector3f at = new Vector3f();
        Vector3f[] last = new Vector3f[2];
        int steps = 0;
        boolean[] stepping = new boolean[2];
        for (int frame = 0; frame < 180; frame++) {
            // A quarter turn over three seconds.
            float yaw = (float) Math.toRadians(Math.min(90, frame * 0.5f));
            Pose pose = standing();
            planter.apply(pose, at, yaw, TO_BLOCKS, FRAME, FootPlanter.Mode.STAND, AHEAD, null);
            int[] shins = {LEG_L, LEG_R};
            boolean[] moved = new boolean[2];
            for (int side = 0; side < 2; side++) {
                Vector3f world = sole(pose, shins[side]).mul(TO_BLOCKS).rotateY(yaw);
                if (last[side] != null) {
                    moved[side] = world.distance(last[side]) > 0.002f;
                }
                last[side] = world;
                if (moved[side] && !stepping[side]) {
                    steps++;
                }
                stepping[side] = moved[side];
            }
            assertTrue(!(moved[0] && moved[1]), "both feet moved at frame " + frame);
        }
        // A quarter turn takes a few steps: the feet did not turn with the body.
        assertTrue(steps >= 3, "steps " + steps);
    }
}
