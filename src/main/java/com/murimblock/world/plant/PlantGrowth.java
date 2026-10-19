package com.murimblock.world.plant;

import com.murimblock.world.plant.PlantCatalog.Climate;
import com.murimblock.world.plant.PlantCatalog.Rarity;
import com.murimblock.world.plant.PlantCatalog.Species;

/**
 * How a planted herb grows (docs/cultivation/plants.md), without the world: four stages (seedling, young, mature, spirit
 * maturity), each condition good, poor or missing, the chance of the next stage on each random tick, and the purity the
 * harvest will have. Durations (the default random tick speed: a block is ticked about every 68 seconds): a common herb
 * matures in some 25 minutes in good conditions, a rare one in about an hour and a half; poor conditions slow it about
 * three times; a missing one stops it. Spirit maturity also needs Murim land rich in Qi and few plants crowding it.
 */
public final class PlantGrowth {
    public static final int SEEDLING = 0, YOUNG = 1, MATURE = 2, SPIRIT = 3;
    /** Purity of a seedling, its gain each stage in good or poor conditions, its least and most; a spirit herb's least. */
    public static final int START_PURITY = 50, GOOD_GAIN = 12, POOR_GAIN = 3, MIN_PURITY = 20, MAX_PURITY = 100, SPIRIT_PURITY = 90;
    /** More planted herbs than this within four blocks: the Qi is shared, no spirit maturity. */
    public static final int CROWD = 8;

    public enum Condition { GOOD, POOR, MISSING }

    private PlantGrowth() {
    }

    /** Light at the plant (sky or block, 0 to 15). */
    public static Condition light(int light) {
        return light >= 9 ? Condition.GOOD : light >= 6 ? Condition.POOR : Condition.MISSING;
    }

    /** The biome's temperature against the warmth the species wants. */
    public static Condition warmth(Climate wants, float temperature) {
        return switch (wants) {
            case ANY -> Condition.GOOD;
            case COLD -> temperature < 0.3F ? Condition.GOOD : temperature < 0.8F ? Condition.POOR : Condition.MISSING;
            case HOT -> temperature > 1.2F ? Condition.GOOD : temperature > 0.5F ? Condition.POOR : Condition.MISSING;
            case MILD -> temperature >= 0.3F && temperature <= 1.2F ? Condition.GOOD : Condition.POOR;
        };
    }

    /** Water near: needed by the wet earth's and the water's plants, welcome to the others. */
    public static Condition water(boolean needsWater, boolean waterNear) {
        return waterNear ? Condition.GOOD : needsWater ? Condition.MISSING : Condition.GOOD;
    }

    /** The worst of them. */
    public static Condition overall(Condition... all) {
        Condition worst = Condition.GOOD;
        for (Condition c : all) if (c.ordinal() > worst.ordinal()) worst = c;
        return worst;
    }

    /** The chance, on a random tick, of the next stage (seedling to young, young to mature). */
    public static double growChance(Rarity rarity, Condition condition) {
        double base = switch (rarity) { case COMMON -> 0.12; case UNCOMMON -> 0.08; case RARE -> 0.045; case VERY_RARE -> 0.025; };
        return condition == Condition.GOOD ? base : condition == Condition.POOR ? base * 0.35 : 0;
    }

    /**
     * The chance, on a random tick of a mature plant, of spirit maturity: only on Murim land (qiRich), in good conditions,
     * not crowded; twice as likely on a place of power.
     */
    public static double spiritChance(Rarity rarity, Condition condition, boolean qiRich, boolean powerPlace, int neighbours) {
        if (!qiRich || condition != Condition.GOOD || neighbours > CROWD) return 0;
        return growChance(rarity, condition) * 0.25 * (powerPlace ? 2 : 1);
    }

    /** The purity after a stage grown in these conditions. */
    public static int purityAfter(int purity, Condition condition, int toStage) {
        int next = purity + (condition == Condition.GOOD ? GOOD_GAIN : condition == Condition.POOR ? POOR_GAIN : 0);
        if (toStage == SPIRIT) next = Math.max(next, SPIRIT_PURITY);
        return Math.clamp(next, MIN_PURITY, MAX_PURITY);
    }

    /** How many herbs (or flowers) a harvest gives: more of a common plant, doubled at spirit maturity. */
    public static int harvestCount(Species species, int age) {
        int base = switch (species.rarity()) { case COMMON -> 2; case UNCOMMON -> 2; case RARE -> 1; case VERY_RARE -> 1; };
        return age >= SPIRIT ? base * 2 : base;
    }

    /** The chance that a harvest also gives back what it grows from (certain at spirit maturity). */
    public static double seedChance(Species species, int age) {
        if (age >= SPIRIT) return 1;
        return switch (species.rarity()) { case COMMON -> 0.6; case UNCOMMON -> 0.5; case RARE -> 0.4; case VERY_RARE -> 0.35; };
    }
}
