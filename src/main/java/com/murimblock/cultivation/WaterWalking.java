package com.murimblock.cultivation;

import com.murimblock.qi.QiService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Running on water, from Qi Liquidation (docs/cultivation/refonte-cultivation.md, 18/10/2026): sprinting, a cultivator
 * runs over still water (a source under open air), held up at its very surface (WaterWalkMixin: the water a solid
 * {@link #SURFACE} for them, a hair above the water, so that they are not in it); stopping, slowing to a walk or sneaking,
 * they sink (to dive on purpose). Each second run on water costs {@link #QI_PER_SECOND} Qi; without, they sink. Worked
 * out on both sides: the server, and the player's own client (their realm and Qi are sent to them).
 */
public final class WaterWalking {
    /** The realm it comes with. */
    public static final CultivationRealm REALM = CultivationRealm.QI_LIQUIDATION;
    /** What a second run on water costs. */
    public static final double QI_PER_SECOND = 3;
    /** The water's surface under a runner: just above the water (a source is 8/9 of a block deep). */
    public static final VoxelShape SURFACE = Block.box(0, 0, 0, 16, 14.4, 16);

    private WaterWalking() {
    }

    /** Whether this player may run on water now: the realm reached, sprinting, not sneaking, some Qi left. */
    public static boolean able(Player player) {
        return player.isSprinting() && !player.isShiftKeyDown() && !player.isSpectator() && !player.getAbilities().flying
                && CultivationService.getRealm(player).ordinal() >= REALM.ordinal() && QiService.getQi(player) >= 1;
    }

    /** Whether this water holds this player up: a source of water under open air (its surface), and they may run on it. */
    public static boolean holdsAt(Player player, BlockGetter level, BlockPos pos, BlockState state) {
        var fluid = state.getFluidState();
        return fluid.is(FluidTags.WATER) && fluid.isSource() && level.getFluidState(pos.above()).isEmpty() && able(player);
    }

    /** Whether this player is running on water now: on its surface, the water under the feet. */
    public static boolean onWater(Player player) {
        BlockPos at = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        return player.onGround() && player.level().getFluidState(at).is(FluidTags.WATER)
                && player.getY() - at.getY() > 0.85 && player.getY() - at.getY() < 0.95;
    }

    /** Each second on water, its Qi. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 == 0 && onWater(player) && able(player)) QiService.removeQi(player, QI_PER_SECOND);
    }
}
