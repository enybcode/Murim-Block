package com.murimblock.cultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What every client may know of a player's Qi (synced to all who see them, never saved): whether they meditate, how
 * full their reserve is (the aura's size), their main style (the aura's colour, and the bare-hand guard's pose) and how
 * rich in Qi the place they stand is ({@link CultivationRules.Place}, for the Cultivation tab). Nothing here gives away
 * a realm: that is only read with Qi Sense, decided by the server for each reader.
 */
public record AuraState(boolean meditating, float fill, Optional<ResourceLocation> mainArt, int place) {
    public static final AuraState NONE = new AuraState(false, 0, Optional.empty(), 0);

    public static final Codec<AuraState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("meditating").forGetter(AuraState::meditating),
            Codec.FLOAT.fieldOf("fill").forGetter(AuraState::fill),
            ResourceLocation.CODEC.optionalFieldOf("main_art").forGetter(AuraState::mainArt),
            Codec.INT.optionalFieldOf("place", 0).forGetter(AuraState::place)
    ).apply(i, AuraState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuraState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AuraState::meditating,
            ByteBufCodecs.FLOAT, AuraState::fill,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), AuraState::mainArt,
            ByteBufCodecs.VAR_INT, AuraState::place,
            AuraState::new);

    public AuraState withMeditating(boolean value) {
        return new AuraState(value, fill, mainArt, place);
    }

    public AuraState withFill(float value) {
        return new AuraState(meditating, value, mainArt, place);
    }

    public CultivationRules.Place placeKind() {
        CultivationRules.Place[] all = CultivationRules.Place.values();
        return all[Math.max(0, Math.min(all.length - 1, place))];
    }
}
