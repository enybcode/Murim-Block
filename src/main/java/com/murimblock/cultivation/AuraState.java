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
 * rich in Qi the place they stand is ({@link CultivationRules.Place}, for the Cultivation tab); for the meditation's
 * aura (Enzo, 17/10/2026), the art that shows in it, the one they have mastered best ({@code auraArt}: its colour and its
 * little effects), and how much Qi they hold ({@code qi}: the bigger the reserve, the bigger and higher the flames);
 * how far their aura weighs on those around ({@code pressure}, AuraPressure: 0 when it does not, sneaking or below
 * Foundation), what everyone sees of it with the naked eye (18/10/2026). Nothing here gives away a realm itself: that is
 * only read with Qi Sense, decided by the server for each reader.
 */
public record AuraState(boolean meditating, float fill, Optional<ResourceLocation> mainArt, int place, Optional<ResourceLocation> auraArt, float qi,
                        float pressure) {
    public static final AuraState NONE = new AuraState(false, 0, Optional.empty(), 0, Optional.empty(), 0, 0);

    public static final Codec<AuraState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("meditating").forGetter(AuraState::meditating),
            Codec.FLOAT.fieldOf("fill").forGetter(AuraState::fill),
            ResourceLocation.CODEC.optionalFieldOf("main_art").forGetter(AuraState::mainArt),
            Codec.INT.optionalFieldOf("place", 0).forGetter(AuraState::place),
            ResourceLocation.CODEC.optionalFieldOf("aura_art").forGetter(AuraState::auraArt),
            Codec.FLOAT.optionalFieldOf("qi", 0F).forGetter(AuraState::qi),
            Codec.FLOAT.optionalFieldOf("pressure", 0F).forGetter(AuraState::pressure)
    ).apply(i, AuraState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AuraState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AuraState decode(RegistryFriendlyByteBuf buf) {
            return new AuraState(buf.readBoolean(), buf.readFloat(), ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, AuraState a) {
            buf.writeBoolean(a.meditating);
            buf.writeFloat(a.fill);
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, a.mainArt);
            ByteBufCodecs.VAR_INT.encode(buf, a.place);
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, a.auraArt);
            buf.writeFloat(a.qi);
            buf.writeFloat(a.pressure);
        }
    };

    public AuraState withMeditating(boolean value) {
        return new AuraState(value, fill, mainArt, place, auraArt, qi, pressure);
    }

    public AuraState withFill(float value) {
        return new AuraState(meditating, value, mainArt, place, auraArt, qi, pressure);
    }

    public CultivationRules.Place placeKind() {
        CultivationRules.Place[] all = CultivationRules.Place.values();
        return all[Math.max(0, Math.min(all.length - 1, place))];
    }
}
