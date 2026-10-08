package com.murimblock.integration.epicfight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Keep the engine's saved IDs/API intact, but remove stamina as a gameplay constraint on both sides. */
@Mixin(value = PlayerPatch.class, remap = false)
public abstract class EpicFightStaminaMixin {
    @Shadow public abstract float getMaxStamina();

    @Inject(method = "getStamina()F", at = @At("HEAD"), cancellable = true)
    private void murimblock$readFullStamina(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(getMaxStamina());
    }

    @Inject(method = "hasStamina(F)Z", at = @At("HEAD"), cancellable = true)
    private void murimblock$ignoreStaminaLimit(float amount, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(Float.isFinite(amount) && amount >= 0);
    }

    @Inject(method = "setStamina(F)V", at = @At("HEAD"), cancellable = true)
    private void murimblock$ignoreStaminaChanges(float value, CallbackInfo callback) {
        callback.cancel();
    }
}
