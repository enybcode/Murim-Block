package com.murimblock.martial;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SanguineDataTest {
    @Test void scrollAndQuestsTeachFiveTechniquesInOrder() {
        var art = MartialArts.art(MartialArts.SANGUINE_ART);
        var data = MartialData.EMPTY.learn(art);
        assertEquals(Optional.of(MartialArts.SANGUINE_ART), data.mainArt());
        assertEquals(Optional.of(MartialArts.BLOOD_CRYSTAL.id()), data.slot(0));
        assertEquals(Optional.of(MartialArts.CRIMSON_STANCE), data.passive());
        assertFalse(data.hasTechnique(MartialArts.SCARLET_TEAR.id()));
        assertFalse(MartialProgress.advance(data, art, MartialArts.SCARLET_RELEASE_HITS, 15).moved());
        var advanced = MartialProgress.advance(data, art, MartialArts.CRIMSON_CONTACT_HITS, 19);
        assertNull(advanced.unlocked());
        advanced = MartialProgress.advance(advanced.data(), art, MartialArts.CRIMSON_CONTACT_HITS, 1);
        assertEquals(MartialArts.SCARLET_TEAR, advanced.unlocked());
        assertEquals(Optional.of(MartialArts.SCARLET_TEAR.id()), advanced.data().slot(1));
        advanced = MartialProgress.advance(advanced.data(), art, MartialArts.SCARLET_RELEASE_HITS, 15);
        assertEquals(MartialArts.BLOOD_CRYSTAL_FORM, advanced.unlocked());
        assertEquals(Optional.of(MartialArts.BLOOD_CRYSTAL_FORM.id()), advanced.data().slot(2));
        assertEquals(MartialArts.CRYSTAL_BARRAGE.id(), MartialProgress.currentQuest(advanced.data(), art).unlocks());
        advanced = MartialProgress.advance(advanced.data(), art, MartialArts.CRYSTAL_MASTERY_HITS, 10);
        assertEquals(MartialArts.CRYSTAL_BARRAGE, advanced.unlocked());
        assertEquals(Optional.of(MartialArts.CRYSTAL_BARRAGE.id()), advanced.data().slot(3));
        assertEquals(MartialArts.BLOOD_ICEBERG.id(), MartialProgress.currentQuest(advanced.data(), art).unlocks());
        advanced = MartialProgress.advance(advanced.data(), art, MartialArts.CRIMSON_PEAK_FORMS, 3);
        assertEquals(MartialArts.BLOOD_ICEBERG, advanced.unlocked());
        assertEquals(Optional.of(MartialArts.BLOOD_ICEBERG.id()), advanced.data().slot(4));
        assertNull(MartialProgress.currentQuest(advanced.data(), art));
    }
    @Test void changingStyleKeepsExistingArtsAndBarAssignments() {
        var azure = MartialArts.art(MartialArts.AZURE_CLOUD_SWORD);
        var sanguine = MartialArts.art(MartialArts.SANGUINE_ART);
        var data = MartialData.EMPTY.learn(azure).learn(sanguine);
        assertEquals(Optional.of(MartialArts.AZURE_CLOUD_SWORD), data.mainArt());
        assertEquals(Optional.of(MartialArts.CLOUD_STEP_SLASH.id()), data.slot(0));
        data = data.withMainArt(MartialArts.SANGUINE_ART);
        assertEquals(Optional.of(MartialArts.CRIMSON_STANCE), data.passive());
        assertEquals(azure.passive().defaults(), data.withMainArt(azure.id()).strikes(azure.passive()));
    }
}
