package com.murimblock.integration.epicfight.mixin;

import com.murimblock.martial.SanguineArt;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

/** Empty hands use CapabilityItem's base method rather than WeaponCapability's override. */
@Mixin(value = CapabilityItem.class, remap = false)
public abstract class SanguineFistMixin {
    @Inject(method = "getAutoAttackMotion", at = @At("HEAD"), cancellable = true)
    private void murimblock$sanguineFists(PlayerPatch<?> player,
            CallbackInfoReturnable<List<AnimationAccessor<? extends AttackAnimation>>> result) {
        var chain = SanguineArt.autoAttackMotions(player, (CapabilityItem) (Object) this);
        if (chain != null) result.setReturnValue(chain);
    }
}
