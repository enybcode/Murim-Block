package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Loads the test clip set exported from Blender (tools/blender/export_clips.py, set "test", in murim_clips.py). */
class ClipLoaderTest {
    private static final Path DIR = Path.of("src/main/resources/assets/murimblock/skeletal/animations");

    private static String read(String file) {
        try {
            return Files.readString(DIR.resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] bytes(String file) {
        try {
            return Files.readAllBytes(DIR.resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, Animation> load(String sidecar) {
        return ClipLoader.load("test", read("test.gltf"), ClipLoaderTest::bytes, Biped.ARMATURE, sidecar);
    }

    private static Pose at(Animation animation, float time) {
        return animation.clip().sample(time, Biped.ARMATURE.bindPose());
    }

    private static Quaternionf degrees(float x, float y, float z) {
        return new Quaternionf().rotateX((float) Math.toRadians(x)).rotateY((float) Math.toRadians(y))
                .rotateZ((float) Math.toRadians(z));
    }

    private static void assertRotation(Quaternionf expected, Pose pose, String joint) {
        Quaternionf actual = new Quaternionf(pose.get(Biped.ARMATURE.get(joint).index()).rotation());
        // q and -q are the same turn.
        assertTrue(Math.abs(Math.abs(actual.dot(expected)) - 1) < 1e-4f, joint + ": " + actual + " vs " + expected);
    }

    @Test
    void clipsHaveTheirLengthAndSettings() {
        Map<String, Animation> clips = load(read("test.json"));
        assertEquals(List.of("bounce", "wave"), List.copyOf(clips.keySet()));
        Animation bounce = clips.get("bounce");
        assertEquals("test/bounce", bounce.id());
        assertEquals(1, bounce.clip().duration(), 1e-4f);
        assertTrue(bounce.loop());
        assertEquals(Biped.ARMATURE.size(), bounce.mask().count());
        Animation wave = clips.get("wave");
        assertEquals(2, wave.clip().duration(), 1e-4f);
        assertEquals(0.2f, wave.transition(), 1e-6f);
        assertTrue(wave.mask().contains(Biped.ARMATURE.get("Hand_R").index()));
        assertFalse(wave.mask().contains(Biped.ARMATURE.get("Leg_L").index()));
    }

    @Test
    void keyframesMatchWhatBlenderWasGiven() {
        Animation bounce = load(null).get("bounce");
        // At a quarter second (frame 5) the root has sunk 1.5 px, thighs forward and shins back by twice as much.
        Pose low = at(bounce, 0.25f);
        Vector3f root = new Vector3f(low.get(Biped.ARMATURE.get(Biped.ROOT).index()).translation());
        assertTrue(root.distance(0, 12 - 1.5f, 0) < 1e-4f, root.toString());
        float angle = (float) Math.toDegrees(Math.acos((12 - 1.5) / 12));
        assertRotation(degrees(-angle, 0, 0), low, "Thigh_L");
        assertRotation(degrees(2 * angle, 0, 0), low, "Leg_R");
        // Blender's constraint is baked: the knee turns half as far as the shin.
        assertRotation(degrees(angle, 0, 0), low, "Knee_R");
        assertRotation(degrees(-100, 0, 0), low, "Hand_L");

        // Between frames 0 and 5 Blender interpolates with a Bézier ease; the export samples each frame, so a time
        // between two frames is between their values.
        float drop = 12 - at(bounce, 0.125f).get(Biped.ARMATURE.get(Biped.ROOT).index()).translation().y();
        assertTrue(drop > 0.5f && drop < 1.5f, "drop " + drop);

        Pose wave = at(load(null).get("wave"), 0.5f);
        assertRotation(degrees(-160, 0, 0), wave, "Arm_R");
        assertRotation(degrees(-70, 0, 0), wave, "Hand_R");
        assertRotation(degrees(0, -10, 0), wave, "Chest");
    }

    @Test
    void badSidecarsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> load("{\"clips\": {\"jab\": {}}}"));
        assertThrows(IllegalArgumentException.class, () -> load("{\"clips\": {\"wave\": {\"lop\": true}}}"));
        assertThrows(IllegalArgumentException.class, () -> load("{\"clips\": {\"wave\": {\"mask\": [\"Tail\"]}}}"));
    }
}
