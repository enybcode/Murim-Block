/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.murimblock.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item;

import net.mcreator.murimblock.item.*;
import net.mcreator.murimblock.MurimBlockMod;

import java.util.function.Function;

public class MurimBlockModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(MurimBlockMod.MODID);
	public static final DeferredItem<Item> PILLS;
	public static final DeferredItem<Item> MURIM_SWORD;
	public static final DeferredItem<Item> SHIELD_DEMONIC;
	public static final DeferredItem<Item> OPEN_EYE;
	public static final DeferredItem<Item> EPEE_ENTRAINEMENT;
	public static final DeferredItem<Item> MEDITATION;
	public static final DeferredItem<Item> CRAB_SPAWN_EGG;
	public static final DeferredItem<Item> ARTOFSWORD;
	static {
		PILLS = register("pills", PillsItem::new);
		MURIM_SWORD = register("murim_sword", MurimSwordItem::new);
		SHIELD_DEMONIC = register("shield_demonic", ShieldDemonicItem::new);
		OPEN_EYE = register("open_eye", OpenEyeItem::new);
		EPEE_ENTRAINEMENT = register("epee_entrainement", EpeeEntrainementItem::new);
		MEDITATION = register("meditation", MeditationItem::new);
		CRAB_SPAWN_EGG = register("crab_spawn_egg", properties -> new SpawnEggItem(MurimBlockModEntities.CRAB.get(), properties));
		ARTOFSWORD = register("artofsword", ArtofswordItem::new);
	}

	// Start of user code block custom items
	// End of user code block custom items
	private static <I extends Item> DeferredItem<I> register(String name, Function<Item.Properties, ? extends I> supplier) {
		return REGISTRY.registerItem(name, supplier, new Item.Properties());
	}
}