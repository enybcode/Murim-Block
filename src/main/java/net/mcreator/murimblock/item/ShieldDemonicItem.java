package net.mcreator.murimblock.item;

public class ShieldDemonicItem extends ShieldItem {
	public ShieldDemonicItem(Item.Properties properties) {
		super(properties.repairable(TagKey.create(Registries.ITEM, ResourceLocation.parse("murim_block:shield_demonic_repair_items"))).component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK).equippableUnswappable(EquipmentSlot.OFFHAND)
				.component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(0.25f, 1, List.of(new BlocksAttacks.DamageReduction(90.0f, Optional.empty(), 0, 1)), new BlocksAttacks.ItemDamageFunction(3, 1, 1),
						Optional.of(DamageTypeTags.BYPASSES_SHIELD), Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK)))
				.durability(100));
	}
}