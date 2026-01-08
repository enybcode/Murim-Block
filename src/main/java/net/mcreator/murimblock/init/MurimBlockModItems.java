/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.Item;

import net.mcreator.murimblock.item.ShieldDemonicItem;
import net.mcreator.murimblock.item.PillsItem;
import net.mcreator.murimblock.item.OpenEyeItem;
import net.mcreator.murimblock.item.MurimSwordItem;
import net.mcreator.murimblock.item.EpeeEntrainementItem;
import net.mcreator.murimblock.MurimBlockMod;

import java.util.function.Function;

public class MurimBlockModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(MurimBlockMod.MODID);
	public static final DeferredItem<Item> PILLS;
	public static final DeferredItem<Item> MURIM_SWORD;
	public static final DeferredItem<Item> SHIELD_DEMONIC;
	public static final DeferredItem<Item> OPEN_EYE;
	public static final DeferredItem<Item> EPEE_ENTRAINEMENT;
	static {
		PILLS = register("pills", PillsItem::new);
		MURIM_SWORD = register("murim_sword", MurimSwordItem::new);
		SHIELD_DEMONIC = register("shield_demonic", ShieldDemonicItem::new);
		OPEN_EYE = register("open_eye", OpenEyeItem::new);
		EPEE_ENTRAINEMENT = register("epee_entrainement", EpeeEntrainementItem::new);
	}

	// Start of user code block custom items
	// End of user code block custom items
	private static <I extends Item> DeferredItem<I> register(String name, Function<Item.Properties, ? extends I> supplier) {
		return REGISTRY.registerItem(name, supplier, new Item.Properties());
	}
}