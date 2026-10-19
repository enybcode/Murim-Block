package com.murimblock.client.mixin;

import com.murimblock.client.cultivation.QiThroughWalls;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Qi vision through walls: the hidden living near enough glow, outlined over everything (QiThroughWalls). */
@Mixin(Minecraft.class)
public abstract class QiThroughWallsGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void murimblock$qiThroughWalls(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (QiThroughWalls.seen(entity)) cir.setReturnValue(true);
    }
}
