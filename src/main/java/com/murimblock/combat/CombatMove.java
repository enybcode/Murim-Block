package com.murimblock.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * One strike, read from {@code data/<namespace>/combat_moves/<name>.json}. Times are in ticks, distances in blocks,
 * the angle is the full width of the cone in front of the eyes.
 */
public record CombatMove(ResourceLocation animation, Phases phases, double reach, double angle, float damage,
                         double knockback, double step, double moveSpeed, int hitStop) {
    public static final Codec<CombatMove> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("animation").forGetter(CombatMove::animation),
            Phases.CODEC.fieldOf("phases").forGetter(CombatMove::phases),
            Codec.doubleRange(0, 8).fieldOf("reach").forGetter(CombatMove::reach),
            Codec.doubleRange(1, 360).fieldOf("angle").forGetter(CombatMove::angle),
            Codec.floatRange(0, 1000).fieldOf("damage").forGetter(CombatMove::damage),
            Codec.doubleRange(0, 5).optionalFieldOf("knockback", 0.4).forGetter(CombatMove::knockback),
            Codec.doubleRange(0, 2).optionalFieldOf("step", 0.0).forGetter(CombatMove::step),
            Codec.doubleRange(0, 1).optionalFieldOf("move_speed", 1.0).forGetter(CombatMove::moveSpeed),
            Codec.intRange(0, 20).optionalFieldOf("hit_stop", 0).forGetter(CombatMove::hitStop)
    ).apply(i, CombatMove::new));
    public static final StreamCodec<ByteBuf, CombatMove> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Ticks of the whole move. */
    public int length() {
        return phases.windup() + phases.active() + phases.recovery();
    }

    /** Windup before the blow lands, ticks when it can hit, recovery before the next action. */
    public record Phases(int windup, int active, int recovery) {
        public static final Codec<Phases> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 100).fieldOf("windup").forGetter(Phases::windup),
                Codec.intRange(1, 100).fieldOf("active").forGetter(Phases::active),
                Codec.intRange(0, 100).fieldOf("recovery").forGetter(Phases::recovery)
        ).apply(i, Phases::new));
    }
}
