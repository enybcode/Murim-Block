package com.murimblock.item;

import com.murimblock.Murimblock;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** Murim's armour materials. */
public final class MurimArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Murimblock.MOD_ID);

    /**
     * The Bandit Chief's iron lamellae, laced with red cord: between iron and diamond (17 armour in all, toughness 1 a
     * piece), mended with iron. Its flat look (textures/models/armor/chief_lamellar_layer_1/2) is Minecraft's own model;
     * with Epic Fight each piece is its own 3D mesh (animmodels/armor/, scripts/animation/chief-armor.cjs).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CHIEF_LAMELLAR = MATERIALS.register("chief_lamellar",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                defense.put(ArmorItem.Type.HELMET, 2);
                defense.put(ArmorItem.Type.CHESTPLATE, 7);
                defense.put(ArmorItem.Type.LEGGINGS, 6);
                defense.put(ArmorItem.Type.BOOTS, 2);
                defense.put(ArmorItem.Type.BODY, 7);
            }), 12, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "chief_lamellar"))), 1.0F, 0.0F));

    /** Durability: between iron (15) and diamond (33). */
    public static final int CHIEF_LAMELLAR_DURABILITY = 25;

    /**
     * The Peak Guard's iron lamellae laced with gold, the Captain of the Peak Guard's: as strong as diamond (20 armour in
     * all, toughness 2 a piece), mended with iron. Flat look textures/models/armor/peak_guard_layer_1/2; with Epic Fight
     * each piece is its own 3D mesh (animmodels/armor/, scripts/animation/peak-guard-armor.cjs).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PEAK_GUARD = MATERIALS.register("peak_guard",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                defense.put(ArmorItem.Type.HELMET, 3);
                defense.put(ArmorItem.Type.CHESTPLATE, 8);
                defense.put(ArmorItem.Type.LEGGINGS, 6);
                defense.put(ArmorItem.Type.BOOTS, 3);
                defense.put(ArmorItem.Type.BODY, 8);
            }), 14, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "peak_guard"))), 2.0F, 0.0F));

    /** Durability: diamond's (33) less a little. */
    public static final int PEAK_GUARD_DURABILITY = 30;

    /**
     * The Blood Sect's lacquer, its soldiers': blood-red lacquered plates rimmed in black iron, an iron face guard with
     * fangs. Between the Bandit Chief's lamellae and the Peak Guard's (18 armour in all, toughness 1 a piece), mended with
     * iron. Flat look textures/models/armor/blood_lacquer_layer_1/2; with Epic Fight each piece is its own 3D mesh
     * (animmodels/armor/, scripts/animation/blood-soldier-game.cjs).
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLOOD_LACQUER = MATERIALS.register("blood_lacquer",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                defense.put(ArmorItem.Type.HELMET, 2);
                defense.put(ArmorItem.Type.CHESTPLATE, 8);
                defense.put(ArmorItem.Type.LEGGINGS, 5);
                defense.put(ArmorItem.Type.BOOTS, 3);
                defense.put(ArmorItem.Type.BODY, 8);
            }), 13, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "blood_lacquer"))), 1.0F, 0.0F));

    /** Durability: between iron (15) and diamond (33). */
    public static final int BLOOD_LACQUER_DURABILITY = 27;

    private MurimArmorMaterials() { }
}
