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
        shock(by, at, radius, true);
    }

    /** As above; without its dust and sound when it is one of many (a rolling line, a ring): the broken ground shows better. */
    public static void shock(LivingEntity by, Vec3 at, double radius, boolean loud) {
        // Its own shock-wave damage off: the technique deals the blow.
        yesman.epicfight.api.utils.LevelUtil.circleSlamFracture(by, by.level(), at.subtract(0, 1, 0), radius, !loud, !loud, false);
    }

    /**
     * A fracture rolling along the ground from `start` along `dir` (flat), `length` blocks: a break every `step`
     * blocks, one each `ticks` ticks, growing from `r0` to `r1` across.
     */
    public static void line(ServerPlayer by, Vec3 start, Vec3 dir, double length, double step, double r0, double r1, int ticks) {
        int n = Math.max(1, (int) Math.floor(length / step));
        for (int i = 0; i <= n; i++) {
            Vec3 at = start.add(dir.scale(i * step));
            double r = r0 + (r1 - r0) * i / n;
            boolean loud = i == 0;
            if (i == 0 || ticks <= 0) shock(by, at, r, loud);
            else MartialCombat.later(by, i * ticks, () -> shock(by, at, r, false));
        }
    }

    /**
     * Breaks round `centre`, `radius` out, `n` of them `r` across, going round from `from` degrees (Minecraft's yaw),
     * one each `ticks` ticks after `delay` (all at once if 0).
     */
    public static void ring(ServerPlayer by, Vec3 centre, double radius, int n, double r, float from, int delay, int ticks) {
        for (int i = 0; i < n; i++) {
            Vec3 at = centre.add(Vec3.directionFromRotation(0, from + i * 360F / n).scale(radius));
            int when = delay + i * ticks;
            if (when <= 0) shock(by, at, r, false);
            else MartialCombat.later(by, when, () -> shock(by, at, r, false));
        }
    }
}
