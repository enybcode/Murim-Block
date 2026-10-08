package com.murimblock.integration.epicfight.mixin;

import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.mob.StrayPatch;

@Mixin(value = StrayPatch.class, remap = false)
public abstract class EpicFightStrayEquipmentMixin {
    // Preserve the superclass combat setup and existing vanilla equipment, skip only the custom clothing.
    @Inject(method = "onJoinWorld(Lnet/minecraft/world/entity/monster/AbstractSkeleton;Lnet/minecraft/world/level/Level;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/AbstractSkeleton;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", ordinal = 0),
            cancellable = true)
    private void murimblock$keepVanillaEquipment(AbstractSkeleton entity, Level level, boolean worldgen, CallbackInfo callback) {
        callback.cancel();
    }
}
