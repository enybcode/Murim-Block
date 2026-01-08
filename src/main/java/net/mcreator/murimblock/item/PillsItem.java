package net.mcreator.murimblock.item;

public class PillsItem extends Item {
	public PillsItem(Item.Properties properties) {
		super(properties.rarity(Rarity.UNCOMMON).stacksTo(12).fireResistant().food((new FoodProperties.Builder()).nutrition(3).saturationModifier(0.3f).alwaysEdible().build(), Consumables.defaultFood().consumeSeconds(2.5F).build())
				.attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
						.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build())
				.enchantable(1));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		PillsPlayerFinishesUsingItemProcedure.execute(world, entity);
		return retval;
	}
}