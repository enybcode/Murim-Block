package com.murimblock.cultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A Qi particle and its power, 0 (barely any Qi) to 1 (a great reserve): bigger and denser as it grows. */
public record QiParticleOptions(ParticleType<QiParticleOptions> type, float power) implements ParticleOptions {
    @Override
    public ParticleType<QiParticleOptions> getType() {
        return type;
    }

    /** A particle type carrying {@link QiParticleOptions}. */
    public static ParticleType<QiParticleOptions> newType() {
        return new ParticleType<>(true) {
            private final MapCodec<QiParticleOptions> codec =
                    Codec.FLOAT.fieldOf("power").xmap(power -> new QiParticleOptions(this, power), QiParticleOptions::power);
            private final StreamCodec<RegistryFriendlyByteBuf, QiParticleOptions> streamCodec =
                    ByteBufCodecs.FLOAT.map(power -> new QiParticleOptions(this, power), QiParticleOptions::power).cast();

            @Override
            public MapCodec<QiParticleOptions> codec() {
                return codec;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, QiParticleOptions> streamCodec() {
                return streamCodec;
            }
        };
    }
}
