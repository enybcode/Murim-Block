package com.murimblock.martial;

import com.murimblock.network.CrescentMoonPayloads;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Crescent Moon Halberd's lights for whoever is near (Photon lights on their clients: client/CrescentMoonClient). */
public final class CrescentMoonFx {
    private CrescentMoonFx() {
    }

    /** A flash of pale moonlight `range` blocks wide at `at`, a little longer the wider it is. */
    public static void flash(ServerLevel level, Vec3 at, float range) {
        send(level, at, new CrescentMoonPayloads.Fx(CrescentMoonPayloads.Fx.FLASH, at.x, at.y, at.z, range, 5 + Math.round(range * 0.6F)));
    }

    /** The full moon's steady light from `at` for `ticks`. */
    public static void moonlight(ServerLevel level, Vec3 at, float range, int ticks) {
        send(level, at, new CrescentMoonPayloads.Fx(CrescentMoonPayloads.Fx.MOONLIGHT, at.x, at.y, at.z, range, ticks));
    }

    /** Night falling round `at` for `ticks`, for whoever is within `range`. */
    public static void night(ServerLevel level, Vec3 at, float range, int ticks) {
        send(level, at, new CrescentMoonPayloads.Fx(CrescentMoonPayloads.Fx.NIGHT, at.x, at.y, at.z, range, ticks));
    }

    private static void send(ServerLevel level, Vec3 at, CrescentMoonPayloads.Fx payload) {
        for (ServerPlayer p : level.players()) if (p.connection != null && p.distanceToSqr(at) < 160 * 160) PacketDistributor.sendToPlayer(p, payload);
    }
}
