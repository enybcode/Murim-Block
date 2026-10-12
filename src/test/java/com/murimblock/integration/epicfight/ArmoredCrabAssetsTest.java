package com.murimblock.integration.epicfight;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmoredCrabAssetsTest {
    private static final String ROOT = "assets/murimblock/";

    @Test
    void everyAnimationTrackReferencesTheShippedRigWithOrderedFiniteFrames() throws IOException {
        JsonObject model = json("animmodels/entity/armored_crab.json");
        var joints = new HashSet<String>();
        for (var joint : model.getAsJsonObject("armature").getAsJsonArray("joints")) {
            assertTrue(joints.add(joint.getAsString()), "Duplicate rig joint");
        }
        assertEquals(28, joints.size());
        assertTrue(joints.containsAll(java.util.List.of("Root", "body", "claw_r_palm", "claw_l_palm")));
        for (var entry : Map.of("idle", 3.0, "walk", 0.75, "attack_right", 1.25,
                "attack_left", 22.0 / 24, "hurt", 0.5, "death", 1.625).entrySet()) {
            var tracks = json("animmodels/animations/armored_crab/" + entry.getKey() + ".json")
                    .getAsJsonArray("animation");
            assertFalse(tracks.isEmpty(), entry.getKey());
            var seen = new HashSet<String>();
            double duration = 0;
            for (var value : tracks) {
                var track = value.getAsJsonObject();
                String name = track.get("name").getAsString();
                assertTrue(joints.contains(name), "Unknown animation joint: " + name);
                assertTrue(seen.add(name), "Duplicate track: " + name);
                var times = track.getAsJsonArray("time");
                var transforms = track.getAsJsonArray("transform");
                assertFalse(times.isEmpty());
                assertEquals(times.size(), transforms.size());
                double previous = -1;
                for (int i = 0; i < times.size(); i++) {
                    double time = times.get(i).getAsDouble();
                    assertTrue(Double.isFinite(time) && time >= 0 && time > previous);
                    previous = time;
                    var matrix = transforms.get(i).getAsJsonArray();
                    assertEquals(16, matrix.size());
                    for (var component : matrix) assertTrue(Double.isFinite(component.getAsDouble()));
                }
                duration = Math.max(duration, previous);
            }
            assertEquals(entry.getValue(), duration, 0.0001, entry.getKey());
        }
    }

    @Test
    void textureAndEnglishSpawnEggResourcesArePresent() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(ROOT + "textures/entity/armored_crab.png")) {
            assertNotNull(input);
            var image = ImageIO.read(input);
            assertNotNull(image);
            assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
        }
        assertEquals("minecraft:item/template_spawn_egg", json("models/item/armored_crab_spawn_egg.json")
                .get("parent").getAsString());
        var language = json("lang/en_us.json");
        assertEquals("Armored Crab", language.get("entity.murimblock.armored_crab").getAsString());
        assertTrue(language.has("item.murimblock.armored_crab_spawn_egg"));
    }

    private JsonObject json(String name) throws IOException {
        var input = getClass().getClassLoader().getResourceAsStream(ROOT + name);
        assertNotNull(input, name);
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
