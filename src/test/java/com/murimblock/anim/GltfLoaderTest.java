package com.murimblock.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Loads the biped files exported from Blender (tools/blender/export.sh) as the mod ships them. */
class GltfLoaderTest {
    private static final Path DIR = Path.of("src/main/resources/assets/murimblock/skeletal");
    private static final Set<String> PARTS = Set.of("head", "body", "arm_l", "arm_r", "leg_l", "leg_r", "hat", "jacket",
            "sleeve_l", "sleeve_r", "pants_l", "pants_r", "armor_head", "armor_hat", "armor_body", "armor_arm_r",
            "armor_arm_l", "armor_leg_r", "armor_leg_l", "armor_inner_body", "armor_inner_leg_r", "armor_inner_leg_l");

    private static SkinnedModel load(String file) {
        return GltfLoader.load(read(DIR.resolve(file)), name -> bytes(DIR.resolve(name)), Biped.ARMATURE);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] bytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void bothBipedsMatchTheSkeleton() {
        for (String file : new String[] {"biped.gltf", "biped_slim.gltf"}) {
            SkinnedModel model = load(file);
            assertTrue(Biped.check(model.armature()).isEmpty(), file);
            assertEquals(PARTS, model.parts().stream().map(SkinnedMesh::name).collect(Collectors.toSet()), file);
            for (int i = 0; i < model.armature().size(); i++) {
                Joint ours = Biped.ARMATURE.joint(i);
                Joint theirs = model.armature().joint(i);
                assertTrue(new Vector3f(ours.bind().translation()).distance(theirs.bind().translation()) < 1e-4f,
                        file + " " + ours.name());
                // Bones point straight up in Blender, so rests have no rotation.
                assertTrue(new Quaternionf(theirs.bind().rotation()).equals(new Quaternionf(), 1e-4f), ours.name());
            }
        }
    }

    @Test
    void partsSitWhereVanillaPutsThem() {
        SkinnedModel wide = load("biped.gltf");
        assertBounds(wide.part("head"), -4, 24, -4, 4, 32, 4);
        assertBounds(wide.part("arm_l"), 4, 12, -2, 8, 24, 2);
        assertBounds(wide.part("hat"), -4.5f, 23.5f, -4.5f, 4.5f, 32.5f, 4.5f);
        assertBounds(load("biped_slim.gltf").part("arm_r"), -7, 11.5f, -2, -4, 23.5f, 2);
        assertTrue(wide.part("hat").outer());
        assertTrue(!wide.part("head").outer());
    }

    @Test
    void everyVertexIsWeightedAndTextured() {
        for (SkinnedMesh part : load("biped.gltf").parts()) {
            for (int v = 0; v < part.vertexCount(); v++) {
                float sum = 0;
                for (int i = 0; i < SkinnedMesh.INFLUENCES; i++) {
                    sum += part.weights()[v * SkinnedMesh.INFLUENCES + i];
                }
                assertEquals(1, sum, 1e-4f, part.name());
                assertTrue(part.uvs()[v * 2] >= 0 && part.uvs()[v * 2] <= 1, part.name());
                assertTrue(part.uvs()[v * 2 + 1] >= 0 && part.uvs()[v * 2 + 1] <= 1, part.name());
            }
            for (int index : part.indices()) {
                assertTrue(index < part.vertexCount(), part.name());
            }
        }
    }

    @Test
    void limbsAreRigidWithAHingeOnTheHelper() {
        SkinnedModel model = load("biped.gltf");
        Armature armature = model.armature();
        SkinnedMesh arm = model.part("arm_l");
        int upper = armature.get("Arm_L").index();
        int lower = armature.get("Hand_L").index();
        int elbow = armature.get("Elbow_L").index();
        boolean sawElbow = false;
        for (int v = 0; v < arm.vertexCount(); v++) {
            int joint = arm.joints()[v * SkinnedMesh.INFLUENCES];
            assertEquals(1, arm.weights()[v * SkinnedMesh.INFLUENCES], 1e-4f, "one joint per vertex");
            float y = arm.positions()[v * 3 + 1];
            float z = arm.positions()[v * 3 + 2];
            if (joint == elbow) {
                sawElbow = true;
                // The hinge's outer edge: at the elbow, on the back.
                assertEquals(18, y, 1e-4f);
                assertEquals(-2, z, 1e-4f);
            } else if (y > 18) {
                assertEquals(upper, joint);
            } else if (y < 18) {
                assertEquals(lower, joint);
            }
        }
        assertTrue(sawElbow);
    }

    @Test
    void leftArmorMirrorsTheRight() {
        // Vanilla maps the left armor limbs with the right ones' texture, flipped: each left vertex at (x, y, z) has the
        // texture of the right vertex at (-x, y, z).
        SkinnedModel model = load("biped.gltf");
        for (String[] pair : new String[][] {{"armor_arm_l", "armor_arm_r"}, {"armor_inner_leg_l", "armor_inner_leg_r"}}) {
            SkinnedMesh left = model.part(pair[0]);
            SkinnedMesh right = model.part(pair[1]);
            for (int v = 0; v < left.vertexCount(); v++) {
                boolean found = false;
                for (int w = 0; w < right.vertexCount() && !found; w++) {
                    found = Math.abs(left.positions()[v * 3] + right.positions()[w * 3]) < 0.25f
                            && Math.abs(left.positions()[v * 3 + 1] - right.positions()[w * 3 + 1]) < 1e-4f
                            && Math.abs(left.positions()[v * 3 + 2] - right.positions()[w * 3 + 2]) < 1e-4f
                            && Math.abs(left.uvs()[v * 2] - right.uvs()[w * 2]) < 1e-4f
                            && Math.abs(left.uvs()[v * 2 + 1] - right.uvs()[w * 2 + 1]) < 1e-4f;
                }
                assertTrue(found, pair[0] + " vertex " + v);
            }
        }
        assertTrue(model.part("armor_head").armor());
        assertTrue(!model.part("armor_head").outer());
    }

    @Test
    void wrongSkeletonsAreRefused() {
        Armature other = Armature.builder().root("Root", JointTransform.IDENTITY).build();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GltfLoader.load(read(DIR.resolve("biped.gltf")), name -> bytes(DIR.resolve(name)), other));
        assertTrue(error.getMessage().startsWith("skeleton does not match"), error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> GltfLoader.load("{\"buffers\": [], \"skins\": []}",
                name -> new byte[0], null));
    }

    @Test
    void loadsWithoutAReference() {
        SkinnedModel model = GltfLoader.load(read(DIR.resolve("biped.gltf")), name -> bytes(DIR.resolve(name)), null);
        assertNotNull(model.armature().find("Elbow_R"));
        assertEquals(Biped.ARMATURE.size(), model.armature().size());
    }

    private static void assertBounds(SkinnedMesh part, float x0, float y0, float z0, float x1, float y1, float z1) {
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        float[] p = part.positions();
        for (int i = 0; i < p.length; i++) {
            min[i % 3] = Math.min(min[i % 3], p[i]);
            max[i % 3] = Math.max(max[i % 3], p[i]);
        }
        float[] expected = {x0, y0, z0, x1, y1, z1};
        float[] actual = {min[0], min[1], min[2], max[0], max[1], max[2]};
        for (int i = 0; i < 6; i++) {
            assertEquals(expected[i], actual[i], 1e-4f, part.name() + " bound " + i);
        }
    }
}
