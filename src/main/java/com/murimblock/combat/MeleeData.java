package com.murimblock.combat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Unsaved, server-owned action state, also synchronized to tracking players. */
public record MeleeData(Action action, long startedAt, long endsAt) {
    public enum Action {
        IDLE, GUARD, SWING, GUARD_IMPACT, CLASH, GUARD_BREAK;

        static Action decode(int value) {
            if (value < 0 || value >= values().length) throw new IllegalArgumentException("Invalid melee action");
            return values()[value];
        }
    }

    public MeleeData {
        java.util.Objects.requireNonNull(action);
        if (startedAt < 0 || endsAt < startedAt) throw new IllegalArgumentException("Invalid action times");
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, MeleeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(Action::decode, Action::ordinal), MeleeData::action,
            ByteBufCodecs.VAR_LONG, MeleeData::startedAt,
            ByteBufCodecs.VAR_LONG, MeleeData::endsAt,
            MeleeData::new);

    public static MeleeData initial() {
        return new MeleeData(Action.IDLE, 0, 0);
    }

    public static MeleeData begin(Action action, long tick, int duration) {
        if (duration < 0) throw new IllegalArgumentException("Negative action duration");
        return new MeleeData(action, tick, action == Action.GUARD ? Long.MAX_VALUE : Math.addExact(tick, duration));
    }

    public boolean isGuarding() {
        return action == Action.GUARD || action == Action.GUARD_IMPACT;
    }

    public boolean locksAttack(long tick) {
        return action != Action.GUARD && isActive(tick);
    }

    public boolean isActive(long tick) {
        return action != Action.IDLE && tick >= startedAt && tick < endsAt;
    }
}
