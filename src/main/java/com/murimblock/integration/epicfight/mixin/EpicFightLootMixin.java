package com.murimblock.integration.epicfight.mixin;

import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.types.registry.RegisterMobSkillBookLootTableEvent;
import yesman.epicfight.data.loot.EpicFightLootTables;

@Mixin(value = EpicFightLootTables.class, remap = false)
public abstract class EpicFightLootMixin {
    @Inject(method = "onLootTableRegistry", at = @At("HEAD"), cancellable = true)
    private static void murimblock$skipBookPools(LootTableLoadEvent event, CallbackInfo callback) { callback.cancel(); }

    @Inject(method = "createSkillLootTable", at = @At("HEAD"), cancellable = true)
    private static void murimblock$skipBookDrops(RegisterMobSkillBookLootTableEvent event, CallbackInfo callback) { callback.cancel(); }
}
