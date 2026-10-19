package com.murimblock.network;

import com.murimblock.Murimblock;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The Crescent Moon Halberd Art's messages to the clients (martial/CrescentMoonHalberd, client/CrescentMoonClient). */
public final class CrescentMoonPayloads {
    private CrescentMoonPayloads() {
    }

    /**
     * To the players near: a light of moonlight there (a Photon light, {@code range} blocks wide, {@code ticks} long): a
     * flash at an impact (FLASH), the steady light of the full moon (MOONLIGHT), or night falling round the place (NIGHT:
     * the sky darkened for whoever is within {@code range}).
     */
    public record Fx(int kind, double x, double y, double z, float range, int ticks) implements CustomPacketPayload {
        public static final int FLASH = 0, MOONLIGHT = 1, NIGHT = 2;
        public static final Type<Fx> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "crescent_moon_fx"));
        public static final StreamCodec<ByteBuf, Fx> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Fx::kind,
                ByteBufCodecs.DOUBLE, Fx::x,
                ByteBufCodecs.DOUBLE, Fx::y,
                ByteBufCodecs.DOUBLE, Fx::z,
                ByteBufCodecs.FLOAT, Fx::range,
                ByteBufCodecs.VAR_INT, Fx::ticks,
                Fx::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
