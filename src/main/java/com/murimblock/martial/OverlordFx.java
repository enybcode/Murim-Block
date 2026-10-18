package com.murimblock.martial;

import com.murimblock.network.OverlordFlashPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Overlord Spear's lights: a flash of blood red and gold at an impact, for whoever is near (a Photon light). */
public final class OverlordFx {
    private OverlordFx() {
    }

    /** A flash `range` blocks wide at `at`, a little longer the wider it is. */
    public static void flash(ServerLevel level, Vec3 at, float range) {
        OverlordFlashPayload payload = new OverlordFlashPayload(at.x, at.y, at.z, range, 6 + Math.round(range * 0.6F));
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(at) < 160 * 160) PacketDistributor.sendToPlayer(p, payload);
    }
}
