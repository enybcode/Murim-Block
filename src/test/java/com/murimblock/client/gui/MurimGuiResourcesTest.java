package com.murimblock.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.murimblock.cultivation.CultivationRealm;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MurimGuiResourcesTest {
    @Test
    void englishIsTheOnlyPackagedModLanguageAndRealmNamesMatchTheApi() throws IOException {
        JsonObject language = json("lang/en_us.json");
        assertEquals("Profile", language.get("gui.murimblock.tab.profile").getAsString());
        assertEquals("Info", language.get("gui.murimblock.tab.infos").getAsString());
        assertEquals("Stage: ???", language.get("gui.murimblock.status_placeholder").getAsString());
        assertEquals("???", language.get("gui.murimblock.stage_unknown").getAsString());
        for (CultivationRealm realm : CultivationRealm.values()) {
            assertEquals(realm.displayName(), language.get("gui.murimblock.realm." + realm.serializedName()).getAsString());
        }
        assertNull(getClass().getClassLoader().getResource("assets/murimblock/lang/fr_fr.json"));
    }

    @Test
    void allPlayerFacingRealmStatusesAreEnglish() {
        assertEquals(List.of("Awakened Human", "Martial Apprentice", "Established Disciple", "Martial Expert",
                        "Martial Master", "Grandmaster", "Legendary Master", "Transcendent", "Beyond the Mortal World"),
                Arrays.stream(CultivationRealm.values()).map(CultivationRealm::status).toList());
    }

    @Test
    void integratedPagesHaveEnglishLabelsForAllNewFieldsAndProgressStates() throws IOException {
        JsonObject language = json("lang/en_us.json");
        for (String key : List.of("qi_reserve", "techniques.title", "techniques.library", "techniques.details",
                "techniques.nothing_selected", "cultivation.current", "cultivation.stage", "cultivation.next_stage",
                "cultivation.no_requirement", "cultivation.breakthrough", "cultivation.state.ready",
                "cultivation.state.not_ready", "cultivation.state.complete", "infos.controls", "infos.charge_qi",
                "infos.combat_mode", "infos.player_status", "infos.qi_max")) {
            assertTrue(language.has("gui.murimblock." + key), key);
            assertTrue(!language.get("gui.murimblock." + key).getAsString().isBlank(), key);
        }
    }

    @Test
    void bitmapFontDefinitionUsesTheMinecraftCodecAndKeepsUnicodeFallback() throws IOException {
        JsonObject definition = json("font/manuscript.json");
        JsonObject bitmap = definition.getAsJsonArray("providers").get(1).getAsJsonObject();
        var decoded = BitmapProvider.Definition.CODEC.codec().parse(JsonOps.INSTANCE, bitmap);
        assertTrue(decoded.result().isPresent(), decoded.toString());
        assertEquals(8, decoded.result().orElseThrow().height());
        assertEquals(7, decoded.result().orElseThrow().ascent());
        assertEquals(4, definition.getAsJsonArray("providers").get(0).getAsJsonObject()
                .getAsJsonObject("advances").get(" ").getAsInt());
        assertEquals("minecraft:default", definition.getAsJsonArray("providers").get(2).getAsJsonObject()
                .get("id").getAsString());
    }

    @Test
    void fontAtlasContainsEveryPrintableGlyphWithDistinctLowercaseAndNoAntialiasing() throws IOException {
        BufferedImage atlas;
        try (InputStream input = resource("textures/font/manuscript.png")) {
            atlas = ImageIO.read(input);
        }
        assertNotNull(atlas);
        assertEquals(128, atlas.getWidth());
        assertEquals(48, atlas.getHeight());
        for (int codepoint = 33; codepoint <= 126; codepoint++) {
            assertTrue(glyphHasPixels(atlas, codepoint), "Missing glyph " + (char) codepoint);
        }
        boolean different = false;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                different |= pixel(atlas, 'A', x, y) != pixel(atlas, 'a', x, y);
            }
        }
        assertTrue(different, "Lowercase must not be rendered as uppercase");
        for (int y = 0; y < atlas.getHeight(); y++) {
            for (int x = 0; x < atlas.getWidth(); x++) {
                int alpha = atlas.getRGB(x, y) >>> 24;
                assertTrue(alpha == 0 || alpha == 255, "Font must have crisp pixel edges");
            }
        }
    }

    private boolean glyphHasPixels(BufferedImage image, int codepoint) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if ((pixel(image, codepoint, x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int pixel(BufferedImage image, int codepoint, int x, int y) {
        int index = codepoint - 32;
        return image.getRGB(index % 16 * 8 + x, index / 16 * 8 + y);
    }

    private JsonObject json(String path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private InputStream resource(String path) {
        InputStream input = getClass().getClassLoader().getResourceAsStream("assets/murimblock/" + path);
        assertNotNull(input, path);
        return input;
    }
}
