package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** The shipped fighting clips read with their attack data, the mobs' keyframe sets (the fight's rules are checked in game, where Minecraft is). */
class CombatDataTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/murimblock/skeletal");

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, Animation> biped(String set) {
        Path folder = ASSETS.resolve("animations");
        return ClipLoader.load(set, read(folder.resolve(set + ".gltf")), uri -> {
            try {
                return Files.readAllBytes(folder.resolve(uri));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Biped.ARMATURE, read(folder.resolve(set + ".json")));
    }

    @Test
    void everyStrikeHasItsPhases() {
        for (String set : List.of("fists")) {
            for (Animation clip : biped(set).values()) {
                if (clip.id().endsWith("stance") || clip.id().endsWith("stance_walk") || clip.id().endsWith("guard")) {
                    assertNull(clip.attack(), clip.id());
                    continue;
                }
                assertNotNull(clip.attack(), clip.id() + " has attack data");
                AttackData.Phase phase = clip.attack().phases().get(0);
                assertTrue(phase.activeFrom() < phase.activeTo() && phase.recovery() <= clip.clip().duration() + 1e-4f,
                        clip.id());
                assertFalse(phase.colliders().isEmpty(), clip.id() + " strikes with a collider on a joint");
            }
        }
    }

    @Test
    void dodgesProtectAndMoveTheBody() {
        Map<String, Animation> combat = biped("combat");
        for (String dodge : List.of("dodge_back", "dodge_left", "dodge_right", "dodge_forward")) {
            Animation clip = combat.get(dodge);
            assertTrue(clip.attack().invulnerableAt(0.1f), dodge);
            assertFalse(clip.attack().invulnerableAt(clip.clip().duration()), dodge);
            assertNotNull(clip.rootMotion(), dodge);
        }
        // Back is -Z, left is +X: the root ends a block (16 px) that way.
        float[] back = combat.get("dodge_back").clip().value(0, Clip.Path.TRANSLATION, 1);
        float[] left = combat.get("dodge_left").clip().value(0, Clip.Path.TRANSLATION, 1);
        assertEquals(-16, back[2], 0.5f);
        assertEquals(16, left[0], 0.5f);
    }
}
