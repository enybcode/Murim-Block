package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SkinningTest {
    private static final Path DIR = Path.of("src/main/resources/assets/murimblock/skeletal");
    private static final SkinnedModel MODEL = load();
    private static final float EPS = 1e-4f;

    private static SkinnedModel load() {
        try {
            String json = Files.readString(DIR.resolve("biped.gltf"));
            return GltfLoader.load(json, name -> {
                try {
                    return Files.readAllBytes(DIR.resolve(name));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Biped.ARMATURE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static float[][] skin(SkinnedMesh mesh, Pose pose) {
        float[] positions = new float[mesh.vertexCount() * 3];
        float[] normals = new float[mesh.vertexCount() * 3];
        Skinning.skin(mesh, pose.toSkinMatrices(MODEL.armature()), positions, normals);
        return new float[][] {positions, normals};
    }

    @Test
    void restPoseLeavesTheBodyAsModelled() {
        for (SkinnedMesh part : MODEL.parts()) {
            float[][] skinned = skin(part, BipedPoses.test("rest"));
            for (int i = 0; i < part.positions().length; i++) {
                assertEquals(part.positions()[i], skinned[0][i], EPS, part.name());
                assertEquals(part.normals()[i], skinned[1][i], EPS, part.name());
            }
        }
    }

    @Test
    void bentElbowFoldsTheForearmAndFillsTheHinge() {
        Pose pose = Biped.ARMATURE.bindPose();
        int hand = Biped.ARMATURE.get("Hand_L").index();
        pose.set(hand, pose.get(hand).add(JointTransform.rotation(new Quaternionf().rotateX((float) Math.toRadians(-90)))));
        Biped.settleHelpers(pose);
        SkinnedMesh arm = MODEL.part("arm_l");
        float[] moved = skin(arm, pose)[0];
        int elbow = Biped.ARMATURE.get("Elbow_L").index();
        for (int v = 0; v < arm.vertexCount(); v++) {
            Vector3f rest = new Vector3f(arm.positions()[v * 3], arm.positions()[v * 3 + 1], arm.positions()[v * 3 + 2]);
            Vector3f now = new Vector3f(moved[v * 3], moved[v * 3 + 1], moved[v * 3 + 2]);
            int joint = arm.joints()[v * SkinnedMesh.INFLUENCES];
            if (rest.y < 18 - EPS) {
                // The forearm swings forward: the fist (y 12) ends up 6 px in front of the elbow.
                assertEquals(18, now.y, 4.01f);
                if (rest.y < 12 + EPS) {
                    assertEquals(6, now.z, EPS);
                }
            } else if (joint == elbow) {
                // The hinge's outer corner, on the back, turns half way: 45° down from behind the elbow.
                float half = (float) Math.toRadians(45);
                assertEquals(18 - 2 * Math.sin(half), now.y, EPS);
                assertEquals(-2 * Math.cos(half), now.z, EPS);
            } else if (rest.y > 18 + EPS) {
                assertTrue(rest.distance(now) < EPS, "the upper arm stays put");
            }
        }
    }

    @Test
    void lookTurnsOnlyTheHead() {
        Pose pose = BipedPoses.test("rest");
        BipedPoses.look(pose, 90, 0);
        // Looking 90° right: the face (+Z) turns to the body's right (-X).
        Vector3f front = pose.toModelMatrices(Biped.ARMATURE)[Biped.ARMATURE.get(Biped.HEAD).index()]
                .transformDirection(new Vector3f(0, 0, 1));
        assertEquals(-1, front.x, EPS);
        assertEquals(0, front.z, EPS);
    }
}
