package com.murimblock.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FamilyDrawTest {
    @Test
    void raritiesFollowTheirShares() {
        Random random = new Random(29102026L);
        Map<Family.Rarity, Integer> counts = new EnumMap<>(Family.Rarity.class);
        Set<Family> seen = new HashSet<>();
        int draws = 200_000;
        for (int i = 0; i < draws; i++) {
            Family f = FamilyDraw.draw(random, 0);
            counts.merge(f.rarity, 1, Integer::sum);
            seen.add(f);
        }
        for (Family.Rarity r : Family.Rarity.values()) {
            assertEquals(r.percent / 100.0, counts.get(r) / (double) draws, 0.01, r.name());
        }
        assertEquals(Family.values().length, seen.size(), "every house can be drawn");
    }

    @Test
    void pityGivesEpicOrBetter() {
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            assertTrue(FamilyDraw.draw(random, FamilyDraw.PITY).rarity.ordinal() >= Family.Rarity.EPIC.ordinal());
        }
        assertEquals(3, FamilyDraw.afterDraw(2, Family.WANG));
        assertEquals(0, FamilyDraw.afterDraw(7, Family.MOYONG));
    }

    @Test
    void houseCounts() {
        Map<Family.Rarity, Integer> counts = new EnumMap<>(Family.Rarity.class);
        Set<String> ids = new HashSet<>();
        for (Family f : Family.values()) {
            counts.merge(f.rarity, 1, Integer::sum);
            assertTrue(ids.add(f.id()), "ids are unique");
            assertEquals(f, Family.byId(f.id()));
        }
        assertEquals(3, counts.get(Family.Rarity.LEGENDARY));
        assertEquals(4, counts.get(Family.Rarity.EPIC));
        assertEquals(5, counts.get(Family.Rarity.RARE));
        assertEquals(8, counts.get(Family.Rarity.COMMON));
    }
}
