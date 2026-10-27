package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ArmatureTest {
    private static final float EPS = 1e-4f;
    private static final Armature BIPED = Biped.ARMATURE;

    private static Vector3f restPosition(String joint) {
        return BIPED.bindPose().toModelMatrices(BIPED)[BIPED.get(joint).index()].getTranslation(new Vector3f());
    }

    private static void assertNear(Vector3f expected, Vector3f actual) {
        assertTrue(expected.distance(actual) < EPS, "expected " + expected + ", got " + actual);
    }

    @Test
    void parentsComeBeforeChildren() {
        for (Joint joint : BIPED.joints()) {
            assertTrue(joint.parent() < joint.index(), joint.name());
        }
        assertEquals(20, BIPED.size());
    }

    @Test
    void restPoseMatchesTheVanillaPlayer() {
        assertNear(new Vector3f(0, 24, 0), restPosition("Head"));
        assertNear(new Vector3f(5, 22, 0), restPosition("Arm_L"));
        assertNear(new Vector3f(-5, 22, 0), restPosition("Arm_R"));
        assertNear(new Vector3f(6, 18, 0), restPosition("Hand_L"));
        assertNear(new Vector3f(2, 12, 0), restPosition("Thigh_L"));
        assertNear(new Vector3f(-2, 6, 0), restPosition("Leg_R"));
    }

    @Test
    void leftAndRightAreTwins() {
        assertEquals(BIPED.get("Hand_R").index(), BIPED.mirrorOf(BIPED.get("Hand_L").index()));
        assertEquals(BIPED.get("Knee_L").index(), BIPED.mirrorOf(BIPED.get("Knee_R").index()));
        assertEquals(BIPED.get("Chest").index(), BIPED.mirrorOf(BIPED.get("Chest").index()));
    }

    @Test
    void restSkinMatricesLeaveVerticesInPlace() {
        for (Matrix4f skin : BIPED.bindPose().toSkinMatrices(BIPED)) {
            assertTrue(skin.equals(new Matrix4f(), EPS), skin.toString());
        }
    }

    @Test
    void shapeCheckSpotsAMovedJoint() {
        assertTrue(Biped.check(BIPED).isEmpty());
        Armature.Builder other = Armature.builder().root("Root", JointTransform.IDENTITY)
                .joint("Torso", "Root", JointTransform.translation(0, 1, 0))
                .joint("Head", "Torso", JointTransform.translation(0, 11, 0));
        List<String> problems = Biped.check(other.build());
        assertTrue(problems.stream().anyMatch(p -> p.startsWith("Head should hang from Chest")), problems.toString());
        assertTrue(problems.contains("missing joint Chest"), problems.toString());
    }

    @Test
    void builderRefusesBadOrders() {
        assertThrows(IllegalArgumentException.class,
                () -> Armature.builder().root("root", JointTransform.IDENTITY).joint("arm", "chest", JointTransform.IDENTITY));
        assertThrows(IllegalArgumentException.class,
                () -> Armature.builder().root("root", JointTransform.IDENTITY).root("other", JointTransform.IDENTITY));
        assertThrows(IllegalArgumentException.class,
                () -> Armature.builder().root("root", JointTransform.IDENTITY).joint("root", "root", JointTransform.IDENTITY));
    }

    @Test
    void masksTakeChildren() {
        JointMask upper = JointMask.withChildren(BIPED, "Chest");
        assertTrue(upper.contains(BIPED.get("Hand_L").index()));
        assertTrue(upper.contains(BIPED.get("Head").index()));
        assertTrue(!upper.contains(BIPED.get("Thigh_L").index()));
        assertEquals(12, upper.count());
        assertEquals(11, upper.without(JointMask.of(BIPED, "Head")).count());
        assertEquals(BIPED.size(), JointMask.all(BIPED).count());
    }
}
