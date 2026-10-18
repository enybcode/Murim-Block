package com.murimblock.martial;

import com.murimblock.mob.MurimEntities;
import com.murimblock.network.OverlordFlashPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Overlord Spear's effects at an impact: a flash of blood red and gold for whoever is near (a Photon light), a shock
 * wave spreading over the ground and cracks of embers left in it (OverlordQiEntity).
 */
public final class OverlordFx {
    private OverlordFx() {
    }

    /** A flash `range` blocks wide at `at`, a little longer the wider it is. */
    public static void flash(ServerLevel level, Vec3 at, float range) {
        OverlordFlashPayload payload = new OverlordFlashPayload(at.x, at.y, at.z, range, 6 + Math.round(range * 0.6F));
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(at) < 160 * 160) PacketDistributor.sendToPlayer(p, payload);
    }

    /** A shock wave from `at` (on the ground) out to `radius`, and the cracks it leaves. */
    public static void shock(ServerLevel level, Vec3 at, double radius) {
        shape(level, at.add(0, 0.08, 0), OverlordQiEntity.RING, 0.3F, (float) radius, 5 + (int) Math.round(radius), 9 + (int) Math.round(radius));
        shape(level, at.add(0, 0.03, 0), OverlordQiEntity.CRACKS, (float) radius * 0.3F, (float) radius * 0.75F, 4, 50);
    }

    /** A small burst of the same: a ring and a few cracks (the ram's wave, a thrust's). */
    public static void spark(ServerLevel level, Vec3 at, double radius) {
        shape(level, at.add(0, 0.08, 0), OverlordQiEntity.RING, 0.1F, (float) radius, 4, 8);
    }

    private static void shape(ServerLevel level, Vec3 at, byte kind, float from, float to, int grow, int life) {
        OverlordQiEntity qi = MurimEntities.OVERLORD_QI.get().create(level);
        if (qi == null) return;
        qi.setPos(at.x, at.y, at.z);
        qi.shape(kind, 1.0F, from, to, grow, new org.joml.Quaternionf(), life);
        level.addFreshEntity(qi);
    }
}
