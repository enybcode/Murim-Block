package net.mcreator.murimblock.item;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.mcreator.murimblock.procedures.TheArtoftheSwordVolumeIRightclickedOnBlockProcedure;

public class TheArtoftheSwordVolumeIItem extends Item {
	private static final ToolMaterial TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 1, 4f, 0, 2, TagKey.create(Registries.ITEM, ResourceLocation.parse("murim_block:the_artofthe_sword_volume_i_repair_items")));

	public TheArtoftheSwordVolumeIItem(Item.Properties properties) {
		super(properties.pickaxe(TOOL_MATERIAL, 3f, -3f));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		super.useOn(context);
		TheArtoftheSwordVolumeIRightclickedOnBlockProcedure.execute(context.getPlayer());
		return InteractionResult.SUCCESS;
	}
}