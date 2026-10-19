package com.murimblock.cultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's cultivation methods: the one practised, those known (ids, Ordinary Breath always), and until when (game time)
 * their Qi is out of tune after a change of method.
 */
public record MethodData(String current, List<String> known, long discordUntil) {
    public static final MethodData INITIAL = new MethodData(CultivationMethod.ORDINARY_BREATH.id(), List.of(CultivationMethod.ORDINARY_BREATH.id()), 0);

    public static final Codec<MethodData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("current").forGetter(MethodData::current),
            Codec.STRING.listOf().fieldOf("known").forGetter(MethodData::known),
            Codec.LONG.optionalFieldOf("discord_until", 0L).forGetter(MethodData::discordUntil)
    ).apply(i, MethodData::new));

    public static final StreamCodec<ByteBuf, MethodData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MethodData::current,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(32)), MethodData::known,
            ByteBufCodecs.VAR_LONG, MethodData::discordUntil,
            MethodData::new);

    public CultivationMethod method() {
        return CultivationMethod.byId(current).orElse(CultivationMethod.ORDINARY_BREATH);
    }

    public boolean knows(CultivationMethod method) {
        return method == CultivationMethod.ORDINARY_BREATH || known.contains(method.id());
    }

    public MethodData learn(CultivationMethod method) {
        if (knows(method)) return this;
        List<String> more = new ArrayList<>(known);
        more.add(method.id());
        return new MethodData(current, List.copyOf(more), discordUntil);
    }

    public MethodData practise(CultivationMethod method, long discordUntil) {
        return new MethodData(method.id(), known, discordUntil);
    }

    public boolean discordAt(long now) {
        return now < discordUntil;
    }
}
