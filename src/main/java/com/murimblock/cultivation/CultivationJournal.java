package com.murimblock.cultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murimblock.qi.QiConstants;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a cultivator's path remembers besides the realm (saved with the player, sent to them for the Cultivation tab):
 * pill toxicity (and when it was last set, it fades by itself), the condensation done this in-game day, the failed great
 * breakthroughs at the current realm, until when a Realm-Breaking Pill helps, and the day each inner-world mission was
 * last done. Older saves load without it.
 */
public record CultivationJournal(double toxicity, long toxicityAt, long day, int condensedTicks, int failures,
                                 long breakingPillUntil, Map<String, Long> missions) {
    public static final CultivationJournal EMPTY = new CultivationJournal(0, 0, -1, 0, 0, 0, Map.of());

    public static final Codec<CultivationJournal> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("toxicity", 0.0).forGetter(CultivationJournal::toxicity),
            Codec.LONG.optionalFieldOf("toxicity_at", 0L).forGetter(CultivationJournal::toxicityAt),
            Codec.LONG.optionalFieldOf("day", -1L).forGetter(CultivationJournal::day),
            Codec.INT.optionalFieldOf("condensed_ticks", 0).forGetter(CultivationJournal::condensedTicks),
            Codec.INT.optionalFieldOf("failures", 0).forGetter(CultivationJournal::failures),
            Codec.LONG.optionalFieldOf("breaking_pill_until", 0L).forGetter(CultivationJournal::breakingPillUntil),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("missions", Map.of()).forGetter(CultivationJournal::missions)
    ).apply(i, CultivationJournal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CultivationJournal> STREAM_CODEC = StreamCodec.of(
            (buf, j) -> {
                buf.writeDouble(j.toxicity);
                buf.writeVarLong(j.toxicityAt);
                buf.writeVarLong(j.day + 1);
                buf.writeVarInt(j.condensedTicks);
                buf.writeVarInt(j.failures);
                buf.writeVarLong(j.breakingPillUntil);
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG).encode(buf, new HashMap<>(j.missions));
            },
            buf -> new CultivationJournal(buf.readDouble(), buf.readVarLong(), buf.readVarLong() - 1, buf.readVarInt(), buf.readVarInt(),
                    buf.readVarLong(), ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG).decode(buf)));

    public CultivationJournal {
        missions = Map.copyOf(missions);
    }

    // ---------------------------------------------------------------- toxicity

    /** The toxicity now (it fades by {@link CultivationRules#TOXICITY_DECAY_PER_MINUTE} a minute since it was set). */
    public double toxicityAt(long now) {
        double minutes = Math.max(0, now - toxicityAt) / (double) QiConstants.TICKS_PER_MINUTE;
        return Math.max(0, toxicity - minutes * CultivationRules.TOXICITY_DECAY_PER_MINUTE);
    }

    public CultivationJournal withToxicity(double value, long now) {
        return new CultivationJournal(Math.max(0, value), now, day, condensedTicks, failures, breakingPillUntil, missions);
    }

    // ---------------------------------------------------------------- condensation of the day

    public static long dayOf(long gameTime) {
        return Math.floorDiv(gameTime, CultivationRules.DAY_TICKS);
    }

    /** Ticks of condensation already done this in-game day. */
    public int condensedOn(long gameTime) {
        return day == dayOf(gameTime) ? condensedTicks : 0;
    }

    public CultivationJournal condensed(long gameTime, int ticks) {
        return new CultivationJournal(toxicity, toxicityAt, dayOf(gameTime), condensedOn(gameTime) + ticks, failures, breakingPillUntil, missions);
    }

    // ---------------------------------------------------------------- breakthroughs

    public CultivationJournal withFailures(int value) {
        return new CultivationJournal(toxicity, toxicityAt, day, condensedTicks, Math.max(0, value), breakingPillUntil, missions);
    }

    public CultivationJournal withBreakingPill(long until) {
        return new CultivationJournal(toxicity, toxicityAt, day, condensedTicks, failures, until, missions);
    }

    public boolean breakingPillActive(long now) {
        return breakingPillUntil > now;
    }

    // ---------------------------------------------------------------- missions

    /** Whether this mission was already done this in-game day. */
    public boolean missionDone(String id, long gameTime) {
        Long last = missions.get(id);
        return last != null && last == dayOf(gameTime);
    }

    public CultivationJournal withMission(String id, long gameTime) {
        Map<String, Long> m = new HashMap<>(missions);
        m.put(id, dayOf(gameTime));
        return new CultivationJournal(toxicity, toxicityAt, day, condensedTicks, failures, breakingPillUntil, m);
    }
}
