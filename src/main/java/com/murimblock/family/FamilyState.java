package com.murimblock.family;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per player, saved: the house they were born into (empty before the first draw), whether the first Destiny window's
 * free reroll is still there, and the pity count. Field names in {@link #CODEC} are saved IDs: keep them.
 */
public final class FamilyState {
    public static final Codec<FamilyState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("family", "").forGetter(s -> s.family),
            Codec.BOOL.optionalFieldOf("free_reroll", true).forGetter(s -> s.freeReroll),
            Codec.INT.optionalFieldOf("since_epic", 0).forGetter(s -> s.sinceEpic))
            .apply(i, FamilyState::new));

    private String family = "";
    private boolean freeReroll = true;
    private int sinceEpic;

    public FamilyState() {
    }

    private FamilyState(String family, boolean freeReroll, int sinceEpic) {
        this.family = family;
        this.freeReroll = freeReroll;
        this.sinceEpic = sinceEpic;
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
    }
}
