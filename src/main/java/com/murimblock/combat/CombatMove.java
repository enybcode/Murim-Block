package com.murimblock.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * One strike, read from {@code data/<namespace>/combat_moves/<name>.json}. Times are in ticks, distances in blocks,
 * the angle is the full width of the cone in front of the eyes.
 */
public record CombatMove(ResourceLocation animation, Phases phases, double reach, double angle, float damage,
                         double knockback, double step, double moveSpeed, int hitStop, FirstPerson firstPerson) {
    public static final Codec<CombatMove> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("animation").forGetter(CombatMove::animation),
            Phases.CODEC.fieldOf("phases").forGetter(CombatMove::phases),
            Codec.doubleRange(0, 8).fieldOf("reach").forGetter(CombatMove::reach),
            Codec.doubleRange(1, 360).fieldOf("angle").forGetter(CombatMove::angle),
            Codec.floatRange(0, 1000).fieldOf("damage").forGetter(CombatMove::damage),
            Codec.doubleRange(0, 5).optionalFieldOf("knockback", 0.4).forGetter(CombatMove::knockback),
            Codec.doubleRange(0, 2).optionalFieldOf("step", 0.0).forGetter(CombatMove::step),
            Codec.doubleRange(0, 1).optionalFieldOf("move_speed", 1.0).forGetter(CombatMove::moveSpeed),
            Codec.intRange(0, 20).optionalFieldOf("hit_stop", 0).forGetter(CombatMove::hitStop),
            FirstPerson.CODEC.optionalFieldOf("first_person", FirstPerson.NONE).forGetter(CombatMove::firstPerson)
    ).apply(i, CombatMove::new));
    public static final StreamCodec<ByteBuf, CombatMove> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Ticks of the whole move. */
    public int length() {
        return phases.windup() + phases.active() + phases.recovery();
    }

    /**
     * What our own eyes see of the strike: where each fist is, in camera space (x right, y up, -z ahead, blocks),
     * at given ticks of the move. The arm reaches from its shoulder to the fist; between keys it eases.
     */
    public record FirstPerson(List<Key> left, List<Key> right) {
        public static final FirstPerson NONE = new FirstPerson(List.of(), List.of());
        public static final Codec<FirstPerson> CODEC = RecordCodecBuilder.create(i -> i.group(
                Key.CODEC.listOf().optionalFieldOf("left", List.of()).forGetter(FirstPerson::left),
                Key.CODEC.listOf().optionalFieldOf("right", List.of()).forGetter(FirstPerson::right)
        ).apply(i, FirstPerson::new));

        /** The fist at {@code tick} (fractional), or null when this arm has no keys. */
        public static Vec3 fist(List<Key> keys, float tick) {
            if (keys.isEmpty()) {
                return null;
            }
            if (tick <= keys.getFirst().tick()) {
                return keys.getFirst().fist();
            }
            for (int i = 1; i < keys.size(); i++) {
                Key to = keys.get(i);
                if (tick <= to.tick()) {
                    Key from = keys.get(i - 1);
                    float t = (tick - from.tick()) / Math.max(1.0E-3f, to.tick() - from.tick());
                    float eased = t * t * (3 - 2 * t);
                    return from.fist().lerp(to.fist(), Mth.clamp(eased, 0, 1));
                }
            }
            return keys.getLast().fist();
        }
    }

    public record Key(float tick, Vec3 fist) {
        public static final Codec<Key> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("tick").forGetter(Key::tick),
                Vec3.CODEC.fieldOf("fist").forGetter(Key::fist)
        ).apply(i, Key::new));
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
