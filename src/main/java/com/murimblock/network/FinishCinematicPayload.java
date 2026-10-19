package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to the two players of a finish: start (or, with {@code start} false, break off) the cinematic camera. Sent
 * only to the executor and the victim; everyone else watches the finish as usual. {@code finish} says which finish is
 * played, so the film's shots follow its timing ({@link #GENERIC}, {@link #AZURE_CLOUD}, {@link #LONG_FIST}, {@link #SCARLET_BLOOM}).
 */
public record FinishCinematicPayload(boolean start, int executorId, int victimId, int finish) implements CustomPacketPayload {
    /** The generic finish (1.7 s, the blow at 0.9 s). */
    public static final int GENERIC = 0;
    /** Last Rain Over the Clouds, the Azure Cloud Sword Art's finish (2.2 s, the blow at 1.5 s). */
    public static final int AZURE_CLOUD = 1;
    /** Final Palm, the Long Fist's finish (2 s, the palm at 1.4 s). */
    public static final int LONG_FIST = 2;
    /** Scarlet Bloom, the Sanguine Art's finish (7.8 s: the body thrown up at 0.7 s, the cage closing from 3.5 s, its burst at 5.0 s, the body landing, executed, at 6.1 s). */
    public static final int SCARLET_BLOOM = 3;
    /** Mountain Burial, the Mountain-Shattering Fist's finish (2.4 s: three blows, the last at 1.4 s). */
    public static final int MOUNTAIN_BURIAL = 4;
    /** Extinguished Stars, the Seven Stars Sword's: seven thrusts, seven stars lit and put out at 1.9 s. */
    public static final int SEVEN_STARS = 5;
    /** The Last Thunderclap, the Purple Heaven Thunder Saber's: a bolt on the raised saber, the blow at 1.3 s. */
    public static final int THUNDER_SABER = 8;
    /** Last Bell, the Golden Bell's: the palm on the chest, three rings of the bell, the last at 1.6 s. */
    public static final int LAST_BELL = 6;
    /** Noon Sentence, the Majin Fist's: a sun raised over the body at 0.6 s, brought down on it at 1.6 s. */
    public static final int NOON_SENTENCE = 7;

    /** Overlord's Verdict, the Overlord Spear's: the fallen lifted on the point at 0.9 s, driven into the ground at 1.6 s. */
    public static final int OVERLORD_VERDICT = 9;
    /** Last Message, the Raven Messenger Art's: a door opened under the fallen at 0.45 s, the fan shut on them at 1.72 s. */
    public static final int LAST_MESSAGE = 10;

    public static final CustomPacketPayload.Type<FinishCinematicPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "finish_cinematic"));
    public static final StreamCodec<ByteBuf, FinishCinematicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FinishCinematicPayload::start,
            ByteBufCodecs.VAR_INT, FinishCinematicPayload::executorId,
            ByteBufCodecs.VAR_INT, FinishCinematicPayload::victimId,
            ByteBufCodecs.VAR_INT, FinishCinematicPayload::finish,
            FinishCinematicPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
