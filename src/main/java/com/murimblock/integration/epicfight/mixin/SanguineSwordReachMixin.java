package com.murimblock.integration.epicfight.mixin;

import com.murimblock.martial.SanguineArt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Epic Fight's sword cuts take their reach from the held item; the blood sword is held in an empty hand, so without
 * this they would reach as far as a fist. It cuts with a long sword's reach (the blade is about 2 blocks).
 */
@Mixin(value = LivingEntityPatch.class, remap = false)
public abstract class SanguineSwordReachMixin {
    @Inject(method = "getColliderMatching", at = @At("HEAD"), cancellable = true)
    private void murimblock$bloodSwordReach(InteractionHand hand, CallbackInfoReturnable<Collider> result) {
        if (hand == InteractionHand.MAIN_HAND && ((LivingEntityPatch<?>) (Object) this).getOriginal() instanceof Player p
                && SanguineArt.holdsSword(p)) result.setReturnValue(ColliderPreset.LONGSWORD);
    }
}
