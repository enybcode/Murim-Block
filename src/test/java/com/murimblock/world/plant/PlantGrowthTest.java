package com.murimblock.world.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murimblock.world.plant.PlantCatalog.Climate;
import com.murimblock.world.plant.PlantCatalog.Rarity;
import com.murimblock.world.plant.PlantGrowth.Condition;
import org.junit.jupiter.api.Test;

class PlantGrowthTest {
    @Test
    void lightWarmthAndWater() {
        assertEquals(Condition.GOOD, PlantGrowth.light(12));
        assertEquals(Condition.POOR, PlantGrowth.light(7));
        assertEquals(Condition.MISSING, PlantGrowth.light(3));
        assertEquals(Condition.GOOD, PlantGrowth.warmth(Climate.COLD, 0.0F));
        assertEquals(Condition.MISSING, PlantGrowth.warmth(Climate.COLD, 1.5F));
        assertEquals(Condition.GOOD, PlantGrowth.warmth(Climate.HOT, 2.0F));
        assertEquals(Condition.POOR, PlantGrowth.warmth(Climate.MILD, 2.0F));
        assertEquals(Condition.MISSING, PlantGrowth.water(true, false));
        assertEquals(Condition.GOOD, PlantGrowth.water(false, false));
        assertEquals(Condition.MISSING, PlantGrowth.overall(Condition.GOOD, Condition.MISSING, Condition.POOR));
    }

    @Test
    void rarerGrowsSlowerAndPoorConditionsSlowItMore() {
        assertTrue(PlantGrowth.growChance(Rarity.COMMON, Condition.GOOD) > PlantGrowth.growChance(Rarity.RARE, Condition.GOOD));
        assertTrue(PlantGrowth.growChance(Rarity.COMMON, Condition.POOR) < PlantGrowth.growChance(Rarity.COMMON, Condition.GOOD));
        assertEquals(0, PlantGrowth.growChance(Rarity.COMMON, Condition.MISSING));
    }

    @Test
    void spiritMaturityWantsQiGoodConditionsAndRoom() {
        assertEquals(0, PlantGrowth.spiritChance(Rarity.COMMON, Condition.GOOD, false, false, 0));
        assertEquals(0, PlantGrowth.spiritChance(Rarity.COMMON, Condition.POOR, true, false, 0));
        assertEquals(0, PlantGrowth.spiritChance(Rarity.COMMON, Condition.GOOD, true, false, PlantGrowth.CROWD + 1));
        double murim = PlantGrowth.spiritChance(Rarity.COMMON, Condition.GOOD, true, false, 0);
        assertTrue(murim > 0 && PlantGrowth.spiritChance(Rarity.COMMON, Condition.GOOD, true, true, 0) == murim * 2);
    }

    @Test
    void purityRisesWithGoodStagesAndSpiritIsPure() {
        assertEquals(PlantGrowth.START_PURITY + PlantGrowth.GOOD_GAIN, PlantGrowth.purityAfter(PlantGrowth.START_PURITY, Condition.GOOD, 1));
        assertEquals(PlantGrowth.SPIRIT_PURITY, PlantGrowth.purityAfter(30, Condition.GOOD, PlantGrowth.SPIRIT));
        assertEquals(PlantGrowth.MAX_PURITY, PlantGrowth.purityAfter(99, Condition.GOOD, 2));
    }

    @Test
    void theCatalogKeepsItsOrderAndEverySpeciesItsIndex() {
        for (int i = 0; i < PlantCatalog.ALL.size(); i++) assertEquals(i, PlantCatalog.ALL.get(i).index());
        assertEquals("chrysanthemum", PlantCatalog.ALL.get(0).id());
        assertTrue(PlantCatalog.ALL.size() <= 64, "the crop's species property holds 64");
    }
}
