package com.murimblock.martial;

import com.murimblock.world.structure.SanctuaryPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The Cloud Cliff Sanctuary's bell, which counts for "Above the Sea of Clouds". Its library's lectern gives nothing any
 * more: the Azure Cloud Sword Manual is only won by defeating Sensei Yun (decision of 14/10/2026).
 */
public final class SectLibrary {
    private SectLibrary() {
    }

    /** How far from the sanctuary's centre its bell tower stands, at most (blocks, horizontally). */
    static final double BELL_REACH = 48;

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().dimension() != Level.OVERWORLD) return;
        // Ringing the sanctuary's bell (it rings as usual): the first step of "Above the Sea of Clouds".
        if (player.level().getBlockState(event.getPos()).is(Blocks.BELL) && nearSanctuary(player.serverLevel(), event.getPos())) {
            MartialProgress.count(player, MartialArts.SEA_OF_CLOUDS_BELL, 1);
        }
    }

    /** Whether a block belongs to this world's sanctuary (its bell tower). */
    static boolean nearSanctuary(ServerLevel overworld, BlockPos pos) {
        BlockPos centre = SanctuaryPlacement.get(overworld).centre();
        if (centre == null) return false;
        double dx = pos.getX() - centre.getX(), dz = pos.getZ() - centre.getZ();
        return dx * dx + dz * dz <= BELL_REACH * BELL_REACH;
    }
}
