package com.murimblock;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Inspect the actual distributable, not just the source directory. */
class DistributionBoundaryTest {
    @Test
    void shippedJarContainsRuntimeResourcesButNoDevelopmentHarnessOrLegacyGui() throws IOException {
        try (ZipFile jar = distribution()) {
            assertNotNull(jar.getEntry("com/murimblock/Murimblock.class"));
            assertNotNull(jar.getEntry("data/murimblock/epicfight_mobpatch/training_opponent.json"));
            assertNotNull(jar.getEntry("assets/murimblock/textures/gui/sprites/hud/qi_bar_progress.png"));
            for (var entry : jar.stream().toList()) {
                String name = entry.getName();
                assertFalse(name.contains("FoundationGameTests") || name.contains("ClientVisualSmoke")
                        || name.contains("ClientCaptureChecks") || name.contains("GameTest"), name);
                assertFalse(name.startsWith("yesman/") || name.startsWith("docs/") || name.startsWith("scripts/"), name);
                assertFalse(name.endsWith("_v4.png") || name.endsWith(".bbmodel"), name);
                assertFalse(name.equals("assets/minecraft/font/default.json"), name);
            }
        }
    }

    @Test
    void everyPackagedJsonResourceParses() throws IOException {
        try (ZipFile jar = distribution()) {
            for (var entry : jar.stream().filter(value -> value.getName().endsWith(".json")).toList()) {
                try (var input = jar.getInputStream(entry)) {
                    assertNotNull(JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)), entry.getName());
                }
            }
        }
    }

    private static ZipFile distribution() throws IOException {
        String path = System.getProperty("murimblock.test.distribution");
        assertNotNull(path, "Run with Gradle so the actual jar is built before this test");
        return new ZipFile(path);
    }
}
