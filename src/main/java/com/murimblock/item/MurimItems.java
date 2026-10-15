package com.murimblock.item;

import com.murimblock.Murimblock;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialManualItem;
import com.murimblock.mob.MurimEntities;
import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murimblock items and its own creative tab. The training opponent stays summon-only on purpose. */
public final class MurimItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Murimblock.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Murimblock.MOD_ID);

    /** Teal shell with sand-coloured spots, matching the crab texture. */
    public static final DeferredItem<DeferredSpawnEggItem> ARMORED_CRAB_SPAWN_EGG = ITEMS.register("armored_crab_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.ARMORED_CRAB, 0x1D5C63, 0xC9B27C, new Item.Properties()));

    /** Shell teal with the sand of its barnacles; spawning it also brings its two crabs. */
    public static final DeferredItem<DeferredSpawnEggItem> CRAB_MAN_SPAWN_EGG = ITEMS.register("crab_man_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.CRAB_MAN, 0x087C8B, 0xE7D1A1, new Item.Properties()));

    /**
     * The mountain bandits' sabre: a broad single-edged blade widening toward a clipped point, a brass guard, a grip
     * wrapped in red cloth. A sword for Epic Fight (data/murimblock/capabilities/weapons/bandit_dao.json), between stone
     * and iron: 5 damage in a player's hand, the 4 of a bandit's blows (its own attack is 0).
     */
    public static final DeferredItem<SwordItem> BANDIT_DAO = ITEMS.register("bandit_dao",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.4F))));

    /**
     * The Bandit Chief's great axe: a crescent blade on a haft taller than a man, two-handed (a greatsword for Epic
     * Fight: data/murimblock/capabilities/weapons/chief_great_axe.json). Heavy and slow: 9 damage in a player's hand,
     * the 8 of the chief's blows (his own attack is 0). He drops it 3 times in a hundred.
     */
    public static final DeferredItem<AxeItem> CHIEF_GREAT_AXE = ITEMS.register("chief_great_axe",
            () -> new AxeItem(Tiers.IRON, new Item.Properties().rarity(Rarity.UNCOMMON).attributes(AxeItem.createAttributes(Tiers.IRON, 6.0F, -3.3F))));

    /**
     * The Orthodox Alliance soldiers' spear, the Azure Peak Spear: a willow-leaf head on an azure socket where the three
     * peaks of the Alliance flag are cast, blue horsehair, a navy shaft banded in gold, 2.6 blocks long. A spear for Epic
     * Fight (data/murimblock/capabilities/weapons/alliance_spear.json: its stance, walk, run and thrusts): 6 damage,
     * a little slower than a sword. Model from scripts/animation/alliance-spear-model.cjs.
     */
    public static final DeferredItem<SwordItem> ALLIANCE_SPEAR = ITEMS.register("alliance_spear",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.8F))));

    /**
     * The Bandit Chief's armour, his look on a man's build: an iron brow band with a tiger plaque, a lamellar vest with
     * his tiger skin and spiked pauldron, crimson trousers under lamellar tassets, fur-cuffed boots
     * (MurimArmorMaterials.CHIEF_LAMELLAR). He drops one piece now and then (loot_table/entities/bandit_chief.json).
     */
    public static final DeferredItem<ArmorItem> CHIEF_BROW_BAND = chiefArmour("chief_brow_band", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> CHIEF_LAMELLAR_VEST = chiefArmour("chief_lamellar_vest", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> CHIEF_TASSETS = chiefArmour("chief_tassets", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> CHIEF_FUR_BOOTS = chiefArmour("chief_fur_boots", ArmorItem.Type.BOOTS);

    private static DeferredItem<ArmorItem> chiefArmour(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new ArmorItem(MurimArmorMaterials.CHIEF_LAMELLAR, type, new Item.Properties()
                .rarity(Rarity.UNCOMMON).durability(type.getDurability(MurimArmorMaterials.CHIEF_LAMELLAR_DURABILITY))));
    }

    /** Tiger orange and the dark of his vest. */
    public static final DeferredItem<DeferredSpawnEggItem> BANDIT_CHIEF_SPAWN_EGG = ITEMS.register("bandit_chief_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.BANDIT_CHIEF, 0xD07A28, 0x2A1A14, new Item.Properties()));

    /** Ash-grey robe and the red of the bandits' headbands. */
    public static final DeferredItem<DeferredSpawnEggItem> MOUNTAIN_BANDIT_SPAWN_EGG = ITEMS.register("mountain_bandit_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.MOUNTAIN_BANDIT, 0x6F6A62, 0xA7302A, new Item.Properties()));

    /**
     * The Azure Cloud (蒼雲劍), Sensei Yun's jian: a straight double-edged blade with a cloud inlaid in blue, a silver
     * cloud guard and pommel, an azure grip and a long tassel. A sword for Epic Fight
     * (data/murimblock/capabilities/weapons/azure_cloud_sword.json), a master's blade: 7 damage and quick in a player's
     * hand, the 6 of the sensei's blows (his own attack is 0). It does not drop from him yet.
     */
    public static final DeferredItem<SwordItem> AZURE_CLOUD_SWORD = ITEMS.register("azure_cloud_sword",
            () -> new SwordItem(Tiers.DIAMOND, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.2F))));

    /**
     * The Snow Plum Jian (雪梅剑), the Orthodox Alliance disciples' sword: a long slender jian, the three peaks of the
     * Alliance flag inlaid at its root and three plum blossoms above them, a small gold guard with upturned wings, an
     * azure grip and a long blue tassel that swings (client/TasselSwordRenderer). A sword for Epic Fight
     * (data/murimblock/capabilities/weapons/snow_plum_jian.json): an iron sword's 6 damage and speed. Model from
     * scripts/animation/snow-plum-jian-model.cjs.
     */
    public static final DeferredItem<SwordItem> SNOW_PLUM_JIAN = ITEMS.register("snow_plum_jian",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));

    /**
     * The Orthodox Alliance's Rattan Shield (藤牌): rattan coiled into a deep dome, a medallion with the Alliance flag on
     * top, a blue cord bound round the edge. A shield (Epic Fight gives every shield its guard and parry), lighter than
     * the vanilla one. Model from scripts/animation/rattan-shield-model.cjs, held through item_skins/rattan_shield.json.
     */
    public static final DeferredItem<ShieldItem> RATTAN_SHIELD = ITEMS.register("rattan_shield",
            () -> new ShieldItem(new Item.Properties().durability(280)));

    /** The azure of his robe and the white of his beard. */
    public static final DeferredItem<DeferredSpawnEggItem> AZURE_SENSEI_SPAWN_EGG = ITEMS.register("azure_sensei_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.AZURE_SENSEI, 0x2F6FC0, 0xEFEBE4, new Item.Properties()));

    /** Jade medallion of the Cloud Cliff sect: its needle points to the sanctuary. */
    public static final DeferredItem<SectTokenItem> SECT_TOKEN = ITEMS.register("sect_token",
            () -> new SectTokenItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** Manual of the Cloud Cliff sect's art: read, it teaches the Azure Cloud Sword Art and is used up. */
    public static final DeferredItem<MartialManualItem> AZURE_CLOUD_SWORD_MANUAL = ITEMS.register("azure_cloud_sword_manual",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), MartialArts.AZURE_CLOUD_SWORD));

    /** Scroll of the first art of the fist: read, it teaches the Long Fist and is used up. Where it is found is still to decide. */
    public static final DeferredItem<MartialManualItem> LONG_FIST_SCROLL = ITEMS.register("long_fist_scroll",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), MartialArts.LONG_FIST));

    public static final DeferredItem<MartialManualItem> SANGUINE_ART_SCROLL = ITEMS.register("sanguine_art_scroll",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), MartialArts.SANGUINE_ART));

    /** Elixirs of cultivation (docs/cultivation/qi-cultivation.md), made from the flora of the Murim lands. */
    public static final DeferredItem<com.murimblock.cultivation.PillItem> QI_GATHERING_PILL = ITEMS.register("qi_gathering_pill",
            () -> new com.murimblock.cultivation.PillItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),
                    com.murimblock.cultivation.PillItem.Kind.QI_GATHERING));
    public static final DeferredItem<com.murimblock.cultivation.PillItem> REALM_BREAKING_PILL = ITEMS.register("realm_breaking_pill",
            () -> new com.murimblock.cultivation.PillItem(new Item.Properties().stacksTo(8).rarity(Rarity.RARE),
                    com.murimblock.cultivation.PillItem.Kind.REALM_BREAKING));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MURIMBLOCK_TAB = TABS.register("murimblock",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.murimblock"))
                    .icon(() -> new ItemStack(ARMORED_CRAB_SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ARMORED_CRAB_SPAWN_EGG.get());
                        output.accept(CRAB_MAN_SPAWN_EGG.get());
                        output.accept(MOUNTAIN_BANDIT_SPAWN_EGG.get());
                        output.accept(BANDIT_DAO.get());
                        output.accept(BANDIT_CHIEF_SPAWN_EGG.get());
                        output.accept(CHIEF_GREAT_AXE.get());
                        output.accept(ALLIANCE_SPEAR.get());
                        output.accept(SNOW_PLUM_JIAN.get());
                        output.accept(RATTAN_SHIELD.get());
                        output.accept(AZURE_SENSEI_SPAWN_EGG.get());
                        output.accept(AZURE_CLOUD_SWORD.get());
                        output.accept(CHIEF_BROW_BAND.get());
                        output.accept(CHIEF_LAMELLAR_VEST.get());
                        output.accept(CHIEF_TASSETS.get());
                        output.accept(CHIEF_FUR_BOOTS.get());
                        output.accept(SECT_TOKEN.get());
                        output.accept(AZURE_CLOUD_SWORD_MANUAL.get());
                        output.accept(LONG_FIST_SCROLL.get());
                        output.accept(SANGUINE_ART_SCROLL.get());
                        output.accept(QI_GATHERING_PILL.get());
                        output.accept(REALM_BREAKING_PILL.get());
                    })
                    .build());

    /** Rock, trees and plants of the seven Murim biomes (docs/biomes). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NATURE_TAB = TABS.register("murimblock_nature",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.murimblock_nature"))
                    .icon(() -> new ItemStack(MurimBlocks.PEACH.sapling().get()))
                    .withTabsBefore(MURIMBLOCK_TAB.getKey())
                    .displayItems((parameters, output) -> MurimBlocks.creativeOrder()
                            .forEach(block -> output.accept(block.get())))
                    .build());

    private MurimItems() { }

    public static void register(IEventBus bus) {
        MurimArmorMaterials.MATERIALS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
