package com.murimblock.combat.knockdown;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Whether a player is knocked down (K.O.) and how long until they get up. Saved, so leaving the game does not escape a
 * knock-down, and synced to every player who sees them (their crawl and carried animations depend on it).
 */
public record KnockdownData(boolean downed, int ticksLeft) {
    public static final KnockdownData STANDING = new KnockdownData(false, 0);

    public static final Codec<KnockdownData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("downed").forGetter(KnockdownData::downed),
            Codec.INT.fieldOf("ticks_left").forGetter(KnockdownData::ticksLeft)
    ).apply(instance, KnockdownData::new));

    public static final StreamCodec<ByteBuf, KnockdownData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KnockdownData::downed,
            ByteBufCodecs.VAR_INT, KnockdownData::ticksLeft,
            KnockdownData::new
    );

    public static KnockdownData downedFor(int ticks) {
        return new KnockdownData(true, ticks);
    }
}
