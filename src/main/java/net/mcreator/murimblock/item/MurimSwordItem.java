package net.mcreator.murimblock.item;

public class MurimSwordItem extends Item {
	private static final ToolMaterial TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 250, 4f, 0, 2, TagKey.create(Registries.ITEM, ResourceLocation.parse("murim_block:murim_sword_repair_items")));

	public MurimSwordItem(Item.Properties properties) {
		super(properties.sword(TOOL_MATERIAL, 3f, 2.5f));
	}
}