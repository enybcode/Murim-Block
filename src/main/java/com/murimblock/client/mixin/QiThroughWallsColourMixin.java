package com.murimblock.client.mixin;

import com.murimblock.client.cultivation.QiThroughWalls;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Qi vision through walls: the glowing outline of one seen through walls is its aura's colour (QiThroughWalls). Client only. */
@Mixin(Entity.class)
public abstract class QiThroughWallsColourMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void murimblock$qiColour(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide) return;
        int colour = QiThroughWalls.colour(self);
        if (colour >= 0) cir.setReturnValue(colour);
    }
}
