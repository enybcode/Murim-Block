package com.murimblock.integration.epicfight;

import com.mojang.serialization.MapCodec;
import com.murimblock.Murimblock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EpicFightContentRegistries {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Murimblock.MOD_ID);

    static { LOOT.register("remove_epicfight_items", () -> RemoveEpicFightLoot.CODEC); }
    private EpicFightContentRegistries() { }
    public static void register(IEventBus bus) { LOOT.register(bus); }
}
