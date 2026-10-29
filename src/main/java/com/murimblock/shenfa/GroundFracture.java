package com.murimblock.shenfa;

import com.murimblock.combat.CombatPayloads;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The ground fracturing under a blow (after V1's spiritual pressure, which used Epic Fight's ground slam): from the
 * server, every client near draws the ground round {@code center} broken into a ring of tilted, lifted blocks that
 * rises outward like a wave and settles back after a few seconds (client.GroundFractures). Nothing changes in the world:
 * damage and knock-back are the caller's. A clip can also fracture the ground by itself (its {@code fracture} event).
 */
public final class GroundFracture {
    private GroundFracture() {
    }

    /** Fractures the ground round {@code center} (the feet, on the ground) out to {@code radius} blocks. */
    public static void at(ServerLevel level, Vec3 center, float radius) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(BlockPos.containing(center)),
                new CombatPayloads.Fracture(center.x, center.y, center.z, radius, level.random.nextInt()));
    }
}
