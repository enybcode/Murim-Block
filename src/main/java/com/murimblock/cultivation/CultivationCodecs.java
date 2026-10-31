package com.murimblock.cultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** How cultivation is saved and sent. Field names in {@link #SAVED} are saved IDs: keep them ({@code stage_qi} is read
 * back as max Qi past the stage floor, see {@link Cultivation}). */
public final class CultivationCodecs {
    public static final Codec<Cultivation> SAVED = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("realm").forGetter(Cultivation::realm),
            Codec.INT.fieldOf("stage").forGetter(Cultivation::stage),
            Codec.DOUBLE.fieldOf("stage_qi").forGetter(Cultivation::stageQi),
            Codec.DOUBLE.fieldOf("qi").forGetter(Cultivation::qi),
            Codec.DOUBLE.fieldOf("max_qi").forGetter(Cultivation::maxQiExact),
            Codec.INT.fieldOf("meridians").forGetter(Cultivation::meridians),
            Codec.STRING.listOf().optionalFieldOf("defeated", java.util.List.of()).forGetter(Cultivation::defeated))
            .apply(i, Cultivation::new));

    public static final StreamCodec<ByteBuf, Cultivation.Snapshot> SNAPSHOT = StreamCodec.of((buf, s) -> {
        ByteBufCodecs.VAR_INT.encode(buf, s.realm());
        ByteBufCodecs.VAR_INT.encode(buf, s.stage());
        buf.writeDouble(s.stageQi());
        buf.writeDouble(s.qi());
        ByteBufCodecs.VAR_INT.encode(buf, s.maxQi());
        ByteBufCodecs.VAR_INT.encode(buf, s.meridians());
        buf.writeBoolean(s.meditating());
        buf.writeFloat(s.calm());
    }, buf -> new Cultivation.Snapshot(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            buf.readDouble(), buf.readDouble(), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
            buf.readBoolean(), buf.readFloat()));

    private CultivationCodecs() {
    }
}
