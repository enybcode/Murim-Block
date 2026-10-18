package com.murimblock.martial;

import com.murimblock.network.OverlordFlashPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Overlord Spear's effects at an impact: a flash of blood red and gold for whoever is near (a Photon light), and the
 * ground broken by Epic Fight's own fracture.
 */
public final class OverlordFx {
    private OverlordFx() {
    }

    /** A flash `range` blocks wide at `at`, a little longer the wider it is. */
    public static void flash(ServerLevel level, Vec3 at, float range) {
        OverlordFlashPayload payload = new OverlordFlashPayload(at.x, at.y, at.z, range, 6 + Math.round(range * 0.6F));
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(at) < 160 * 160) PacketDistributor.sendToPlayer(p, payload);
    }

    /**
     * The ground broken round `at` (a point on the ground), out to `radius`: Epic Fight's own fracture, as its slams do
     * (LevelUtil.circleSlamFracture: the blocks tilted and raised round the impact, its debris and sound, all put back by
     * itself), for every player near.
     */
    public static void shock(LivingEntity by, Vec3 at, double radius) {
        yesman.epicfight.api.utils.LevelUtil.circleSlamFracture(by, by.level(), at.subtract(0, 1, 0), radius, false, false);
    }
}
