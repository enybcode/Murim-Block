package com.murimblock.cultivation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** What a cultivator's journal remembers: toxicity fading, the day's condensation, missions once a day, old saves. */
class CultivationJournalTest {
    @Test
    void toxicityFadesAPointAMinute() {
        CultivationJournal j = CultivationJournal.EMPTY.withToxicity(30, 1000);
        assertEquals(30, j.toxicityAt(1000), 1e-9);
        assertEquals(20, j.toxicityAt(1000 + 10 * 1200), 1e-9);
        assertEquals(0, j.toxicityAt(1000 + 60 * 1200), 1e-9);
    }

    @Test
    void theDaysCondensationStartsAgainEachDay() {
        CultivationJournal j = CultivationJournal.EMPTY.condensed(100, 600).condensed(200, 600);
        assertEquals(1200, j.condensedOn(300));
        assertEquals(0, j.condensedOn(24_000 + 100), "a new day starts from nothing");
        assertEquals(20, j.condensed(24_100, 20).condensedOn(24_200));
    }

    @Test
    void aMissionIsDoneOnceAnInGameDay() {
        CultivationJournal j = CultivationJournal.EMPTY.withMission("storm", 5000);
        assertTrue(j.missionDone("storm", 6000));
        assertFalse(j.missionDone("sparring", 6000));
        assertFalse(j.missionDone("storm", 24_000 + 10));
    }

    @Test
    void anOldSaveLoadsEmptyAndANewOneComesBackWhole() {
        CultivationJournal loaded = CultivationJournal.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        assertEquals(CultivationJournal.EMPTY, loaded);
        CultivationJournal j = CultivationJournal.EMPTY.withToxicity(42, 7).withFailures(2).withBreakingPill(9000).condensed(30_000, 400).withMission("storm", 30_000);
        var json = CultivationJournal.CODEC.encodeStart(JsonOps.INSTANCE, j).getOrThrow();
        assertEquals(j, CultivationJournal.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json.toString())).getOrThrow());
    }

    @Test
    void theAuraStateGivesNoRealmAway() {
        var fist = Optional.of(ResourceLocation.fromNamespaceAndPath("murimblock", "long_fist"));
        AuraState a = new AuraState(true, 0.5F, fist, 2, fist, 640F);
        assertEquals(CultivationRules.Place.HIGH, a.placeKind());
        var json = AuraState.CODEC.encodeStart(JsonOps.INSTANCE, a).getOrThrow().toString();
        assertFalse(json.contains("realm"), "the aura everyone sees must not carry a realm: " + json);
        assertEquals(a, AuraState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow());
    }
}
