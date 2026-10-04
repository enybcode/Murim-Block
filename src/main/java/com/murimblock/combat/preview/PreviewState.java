package com.murimblock.combat.preview;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Unsaved snapshot. Entity attachment addressing supplies actor identity and lifetime. */
public record PreviewState(long generation, long startedAt, long sampledAt, int durationTicks, boolean playing) {
    public static final StreamCodec<RegistryFriendlyByteBuf, PreviewState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, PreviewState::generation,
            ByteBufCodecs.VAR_LONG, PreviewState::startedAt,
            ByteBufCodecs.VAR_LONG, PreviewState::sampledAt,
            ByteBufCodecs.VAR_INT, PreviewState::durationTicks,
            ByteBufCodecs.BOOL, PreviewState::playing,
            PreviewState::new);

    public PreviewState {
        if (generation < 0 || startedAt < 0 || sampledAt < startedAt
                || durationTicks < 0 || durationTicks > PreviewDefinition.DURATION_TICKS
                || (playing && (generation == 0 || durationTicks == 0))) {
            throw new IllegalArgumentException("Invalid preview snapshot");
        }
    }

    public static PreviewState initial() {
        return new PreviewState(0, 0, 0, 0, false);
    }

    public PreviewState begin(long now) {
        return new PreviewState(Math.incrementExact(generation), now, now, PreviewDefinition.DURATION_TICKS, true);
    }

    public PreviewState sample(long now) {
        return new PreviewState(generation, startedAt, Math.max(sampledAt, now), durationTicks, playing);
    }

    public PreviewState stop(long now) {
        PreviewState sample = sample(now);
        return new PreviewState(generation, startedAt, sample.sampledAt, durationTicks, false);
    }

    public long sampledAge() {
        return Math.min(durationTicks, sampledAt - startedAt);
    }

    public boolean isActive(long now) {
        return playing && now >= startedAt && now - startedAt < durationTicks;
    }
}
