package net.mcreator.murimblock.item;

import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;

import java.util.Optional;
import java.util.List;

public class ShieldDemonicItem extends ShieldItem {
	public ShieldDemonicItem(Item.Properties properties) {
		super(properties.repairable(TagKey.create(Registries.ITEM, ResourceLocation.parse("murim_block:shield_demonic_repair_items"))).component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK).equippableUnswappable(EquipmentSlot.OFFHAND)
				.component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(0.25f, 1, List.of(new BlocksAttacks.DamageReduction(90.0f, Optional.empty(), 0, 1)), new BlocksAttacks.ItemDamageFunction(3, 1, 1),
						Optional.of(DamageTypeTags.BYPASSES_SHIELD), Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK)))
				.durability(999));
	}
}