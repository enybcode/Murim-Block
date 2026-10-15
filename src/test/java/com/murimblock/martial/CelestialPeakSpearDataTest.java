package com.murimblock.martial;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CelestialPeakSpearDataTest {
    @Test void theManualAndQuestsTeachThreeTechniquesInOrderWithNoSecret() {
        var art = MartialArts.art(MartialArts.CELESTIAL_PEAK_SPEAR);
        assertEquals(3, art.techniques().size(), "three techniques, no secret");
        assertTrue(art.techniques().stream().noneMatch(MartialArts.Technique::secret));
        var data = MartialData.EMPTY.learn(art);
        assertEquals(Optional.of(MartialArts.PIERCING_DRAGON.id()), data.slot(0));
        assertEquals(Optional.of(MartialArts.LONG_REACH), data.passive());
        var advanced = MartialProgress.advance(data, art, MartialArts.FIRST_LINE_DRAGON_HITS, 19);
        assertNull(advanced.unlocked());
        advanced = MartialProgress.advance(advanced.data(), art, MartialArts.FIRST_LINE_DRAGON_HITS, 1);
        assertEquals(MartialArts.PLUM_BLOSSOM_THRUSTS, advanced.unlocked());
        advanced = MartialProgress.advance(advanced.data(), art, MartialArts.HOLD_THE_DISTANCE_HITS, 30);
        assertEquals(MartialArts.THOUSAND_SOLDIER_SWEEP, advanced.unlocked());
        assertNull(MartialProgress.currentQuest(advanced.data(), art));
        // Early-game costs, as the other starting arts.
        assertTrue(MartialArts.PIERCING_DRAGON.qiCost() <= 25 && MartialArts.THOUSAND_SOLDIER_SWEEP.qiCost() <= 25);
    }
}
