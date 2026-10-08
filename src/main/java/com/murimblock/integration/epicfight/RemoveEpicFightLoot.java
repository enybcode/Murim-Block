package com.murimblock.integration.epicfight;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class RemoveEpicFightLoot extends LootModifier {
    public static final MapCodec<RemoveEpicFightLoot> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, RemoveEpicFightLoot::new));

    public RemoveEpicFightLoot(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        loot.removeIf(EpicFightContentPolicy::blocked);
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
