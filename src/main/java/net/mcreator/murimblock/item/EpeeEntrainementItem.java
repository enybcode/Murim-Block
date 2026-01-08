package net.mcreator.murimblock.item;

public class EpeeEntrainementItem extends Item {
	private static final ToolMaterial TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 70, 4f, 0, 2, TagKey.create(Registries.ITEM, ResourceLocation.parse("murim_block:epee_entrainement_repair_items")));

	public EpeeEntrainementItem(Item.Properties properties) {
		super(properties.sword(TOOL_MATERIAL, 3.5f, -3f));
	}
}