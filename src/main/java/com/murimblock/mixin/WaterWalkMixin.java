package com.murimblock.mixin;

import com.murimblock.cultivation.WaterWalking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Running on water (WaterWalking): still water holds a runner up at its very surface, a little above the water (so that
 * they are not in it: in it, Minecraft and NeoForge would stop their sprint, and they would sink).
 */
@Mixin(LiquidBlock.class)
public abstract class WaterWalkMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void murimblock$runOnWater(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entity && entity.getEntity() instanceof Player player
                && context.isAbove(WaterWalking.SURFACE, pos, true) && WaterWalking.holdsAt(player, level, pos, state))
            cir.setReturnValue(WaterWalking.SURFACE);
    }
}
