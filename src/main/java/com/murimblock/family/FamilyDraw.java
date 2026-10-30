package com.murimblock.family;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * The draw of a house: a rarity by its share (60 / 25 / 11 / 4 %), then one of its houses evenly. Pity: after
 * {@link #PITY} draws in a row without an epic or legendary house, the next draw is epic or better.
 */
public final class FamilyDraw {
    public static final int PITY = 10;

    private FamilyDraw() {
    }

    /** Draws a house; {@code sinceEpic} is how many draws in a row gave no epic or legendary house. */
    public static Family draw(RandomGenerator random, int sinceEpic) {
        List<Family.Rarity> pool = new ArrayList<>(List.of(Family.Rarity.values()));
        if (sinceEpic >= PITY) {
            pool.removeIf(r -> r.ordinal() < Family.Rarity.EPIC.ordinal());
        }
        int total = pool.stream().mapToInt(r -> r.percent).sum();
        int roll = random.nextInt(total);
        Family.Rarity rarity = pool.get(pool.size() - 1);
        for (Family.Rarity r : pool) {
            if (roll < r.percent) {
                rarity = r;
                break;
            }
            roll -= r.percent;
        }
        List<Family> houses = new ArrayList<>();
        for (Family f : Family.values()) {
            if (f.rarity == rarity) {
                houses.add(f);
            }
        }
        return houses.get(random.nextInt(houses.size()));
    }

    /** The pity count after drawing {@code house}. */
    public static int afterDraw(int sinceEpic, Family house) {
        return house.rarity.ordinal() >= Family.Rarity.EPIC.ordinal() ? 0 : sinceEpic + 1;
    }
}
