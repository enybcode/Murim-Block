package com.murimblock.integration.epicfight.mixin;

import com.murimblock.martial.AzureCloudSword;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

/**
 * A martial art's passive changes the M1 chain (docs/combat/STYLES.md, "Ce qu'apporte un style"): a sword in the hand
 * of a student of the Azure Cloud Sword Art strikes with Flowing Cloud Stance's strikes, as placed in their four M1
 * slots, instead of the sword's own chain. Epic Fight still plays them (its combo counter, dash and air attacks).
 */
@Mixin(value = WeaponCapability.class, remap = false)
public abstract class EpicFightStanceMixin {
    @Inject(method = "getAutoAttackMotion", at = @At("HEAD"), cancellable = true)
    private void murimblock$stanceStrikes(PlayerPatch<?> playerPatch,
                                          CallbackInfoReturnable<List<AnimationAccessor<? extends AttackAnimation>>> callback) {
        List<AnimationAccessor<? extends AttackAnimation>> stance = AzureCloudSword.autoAttackMotions(playerPatch, (CapabilityItem) (Object) this);
        if (stance != null) callback.setReturnValue(stance);
    }
}
