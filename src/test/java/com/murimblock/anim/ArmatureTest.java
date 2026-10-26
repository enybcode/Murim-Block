package com.murimblock.anim;

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
        assertEquals(22, BIPED.size());
    }

    @Test
    void restPoseMatchesTheVanillaPlayer() {
        assertNear(new Vector3f(0, 24, 0), restPosition("head"));
        assertNear(new Vector3f(5, 22, 0), restPosition("upper_arm_l"));
        assertNear(new Vector3f(-5, 22, 0), restPosition("upper_arm_r"));
        assertNear(new Vector3f(6, 18, 0), restPosition("forearm_l"));
        assertNear(new Vector3f(2, 12, 0), restPosition("thigh_l"));
        assertNear(new Vector3f(-2, 2, 0), restPosition("foot_r"));
    }

    @Test
    void leftAndRightAreTwins() {
        assertEquals(BIPED.get("hand_r").index(), BIPED.mirrorOf(BIPED.get("hand_l").index()));
        assertEquals(BIPED.get("shin_l").index(), BIPED.mirrorOf(BIPED.get("shin_r").index()));
        assertEquals(BIPED.get("chest").index(), BIPED.mirrorOf(BIPED.get("chest").index()));
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
        Armature.Builder other = Armature.builder().root("root", JointTransform.IDENTITY)
                .joint("hips", "root", JointTransform.translation(0, 12, 0))
                .joint("head", "hips", JointTransform.translation(0, 12, 0));
        List<String> problems = Biped.check(other.build());
        assertTrue(problems.stream().anyMatch(p -> p.startsWith("head should hang from neck")), problems.toString());
        assertTrue(problems.contains("missing joint chest"), problems.toString());
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
        JointMask upper = JointMask.withChildren(BIPED, "chest");
        assertTrue(upper.contains(BIPED.get("hand_l").index()));
        assertTrue(upper.contains(BIPED.get("head").index()));
        assertTrue(!upper.contains(BIPED.get("thigh_l").index()));
        assertEquals(13, upper.count());
        assertEquals(12, upper.without(JointMask.of(BIPED, "head")).count());
        assertEquals(BIPED.size(), JointMask.all(BIPED).count());
    }
}
