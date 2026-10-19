package com.murimblock.martial;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The fist arts strengthen the bare hands' plain blows (M1), each by its overall power (Enzo, 17/10/2026): while the main
 * style is a fist art and the main hand is empty, every M1 deals a flat extra, then a share more. The techniques keep
 * their own power (TechniquePower); weapons in hand get nothing from this.
 * <ul>
 *   <li>Golden Bell, the defensive starter: +1, x1.10;</li>
 *   <li>Long Fist, the first art of the fist: +2, x1.15;</li>
 *   <li>Mountain-Shattering Fist, from Qi Condensation: +4, x1.30;</li>
 *   <li>Majin Fist, from Qi Liquidation: +5, x1.40.</li>
 * </ul>
 */
public final class FistPower {
    /** A fist art's boost to the bare hands' M1: a flat extra (half hearts), then a multiplier. */
    public record Boost(float extra, float factor) { }

    public static final Map<ResourceLocation, Boost> BOOSTS = Map.of(
            MartialArts.GOLDEN_BELL, new Boost(1.0F, 1.10F),
            MartialArts.LONG_FIST, new Boost(2.0F, 1.15F),
            MartialArts.MOUNTAIN_SHATTERING_FIST, new Boost(4.0F, 1.30F),
            MartialArts.MAJIN_FIST, new Boost(5.0F, 1.40F));

    private FistPower() {
    }

    /** The boost this player's M1 takes now: bare-handed, a fist art as main style; none otherwise. */
    public static Optional<Boost> boost(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) return Optional.empty();
        return MartialService.data(player).mainArt().map(BOOSTS::get);
    }
}
