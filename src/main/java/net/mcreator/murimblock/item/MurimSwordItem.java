package net.mcreator.murimblock.item;

import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class MurimSwordItem extends Item {
	private static final ToolMaterial TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 250, 4f, 0, 2, TagKey.create(Registries.ITEM, ResourceLocation.parse("murim_block:murim_sword_repair_items")));

	public MurimSwordItem(Item.Properties properties) {
		super(properties.sword(TOOL_MATERIAL, 3f, 2.5f));
	}
}