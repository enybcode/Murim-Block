package com.murimblock.family;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * Per player, saved: the house they were born into (empty before the first draw), whether the first Destiny window's
 * free reroll is still there, the pity count, the attributes its bonuses sit on and the house whose items were given.
 * Field names in {@link #CODEC} are saved IDs: keep them.
 */
public final class FamilyState {
    public static final Codec<FamilyState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("family", "").forGetter(s -> s.family),
            Codec.BOOL.optionalFieldOf("free_reroll", true).forGetter(s -> s.freeReroll),
            Codec.INT.optionalFieldOf("since_epic", 0).forGetter(s -> s.sinceEpic),
            Codec.STRING.listOf().optionalFieldOf("stats", List.of()).forGetter(s -> s.stats),
            Codec.STRING.optionalFieldOf("gifts_for", "").forGetter(s -> s.giftsFor))
            .apply(i, FamilyState::new));

    private String family = "";
    private boolean freeReroll = true;
    private int sinceEpic;
    /** The attributes the house's bonuses sit on, to take them back. */
    private List<String> stats = List.of();
    /** The house whose items (and art) were given, so they are given once. */
    private String giftsFor = "";

    public FamilyState() {
    }

    private FamilyState(String family, boolean freeReroll, int sinceEpic, List<String> stats, String giftsFor) {
        this.family = family;
        this.freeReroll = freeReroll;
        this.sinceEpic = sinceEpic;
        this.stats = stats;
        this.giftsFor = giftsFor;
    }

    public List<String> statsApplied() {
        return stats;
    }

    public void statsApplied(List<String> attributes) {
        stats = List.copyOf(attributes);
    }

    public String giftsFor() {
        return giftsFor;
    }

    public void giftsGiven(String house) {
        giftsFor = house;
    }

    /** The house, or null before the first draw. */
    public Family family() {
        return Family.byId(family);
    }

    public boolean freeReroll() {
        return freeReroll;
    }

    public int sinceEpic() {
        return sinceEpic;
    }

    /** Takes {@code house} as drawn: it becomes the player's, and the pity count moves on. */
    public void drawn(Family house) {
        family = house.id();
        sinceEpic = FamilyDraw.afterDraw(sinceEpic, house);
    }

    /** Sets the house without a draw (a command). */
    public void set(Family house) {
        family = house == null ? "" : house.id();
    }

    public void spendFreeReroll() {
        freeReroll = false;
    }

    /** Back to before the first draw (a command): the next login draws again with a free reroll. */
    public void reset() {
        family = "";
        freeReroll = true;
        sinceEpic = 0;
        giftsFor = "";
    }
}
