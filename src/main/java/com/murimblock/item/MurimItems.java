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

    /** Orange and the near-black of its stripes. */
    public static final DeferredItem<DeferredSpawnEggItem> TIGER_SPAWN_EGG = ITEMS.register("tiger_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.TIGER, 0xE39A35, 0x22273A, new Item.Properties()));
    /**
     * What the tiger leaves, one time in five each (loot_table/entities/tiger.json): its striped pelt and its claws.
     * Icons from scripts/animation/tiger-item-icons.cjs.
     */
    public static final DeferredItem<Item> TIGER_PELT = ITEMS.register("tiger_pelt", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> TIGER_CLAW = ITEMS.register("tiger_claw", () -> new Item(new Item.Properties()));

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
    public static final DeferredItem<DeferredSpawnEggItem> CRAB_KING_SPAWN_EGG = ITEMS.register("crab_king_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.CRAB_KING, 0x6E1A20, 0x55361F, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BANDIT_CHIEF_SPAWN_EGG = ITEMS.register("bandit_chief_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.BANDIT_CHIEF, 0xD07A28, 0x2A1A14, new Item.Properties()));
    /** The warlord's crimson lacquer and black iron. */
    public static final DeferredItem<DeferredSpawnEggItem> BANDIT_WARLORD_SPAWN_EGG = ITEMS.register("bandit_warlord_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.BANDIT_WARLORD, 0x9C1F1A, 0x2E3036, new Item.Properties()));

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
     * Two Murim knives, daggers for Epic Fight (data/murimblock/capabilities/weapons: its own dagger moveset, quick
     * stabs and slashes, two of them dual-wielded), not thrown; sold by the Alliance merchant, rarely found (3 %) in a
     * site's barrel (chests/murim_cache): the Bishou (匕首), a straight double-edged dagger, black cord and a
     * jade pommel, 4 damage; the Flying Knife (飞刀), a thin leaf blade, a steel ring and a red cloth, lighter and quicker,
     * 3 damage. Models from scripts/animation/murim-knives-model.cjs (Enzo's choice, 15/10/2026).
     */
    public static final DeferredItem<SwordItem> BISHOU = ITEMS.register("bishou",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 1, -2.0F))));
    public static final DeferredItem<SwordItem> FLYING_KNIFE = ITEMS.register("flying_knife",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 0, -1.6F))));

    /**
     * The Orthodox Alliance's Rattan Shield (藤牌): rattan coiled into a deep dome, a medallion with the Alliance flag on
     * top, a blue cord bound round the edge. A shield (Epic Fight gives every shield its guard and parry), lighter than
     * the vanilla one. Model from scripts/animation/rattan-shield-model.cjs, held through item_skins/rattan_shield.json.
     */
    /**
     * The sect weapons, each with its own Epic Fight weapon type and moveset (integration/epicfight/MurimWeapons,
     * SectWeaponAnimations): the iron fan (its ribs opened by client/FanRenderer), the chain whip (its links hung on a
     * simulated chain, client/ChainWhipRenderer) and the flying needles, thrown in fans (mob/FlyingNeedleEntity).
     */
    public static final DeferredItem<SwordItem> IRON_FAN = ITEMS.register("iron_fan",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 1, -2.0F))));
    public static final DeferredItem<SwordItem> CHAIN_WHIP = ITEMS.register("chain_whip",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.6F))));
    /** Each needle thrown hurts for a share of the thrower's attack (FlyingNeedleEntity.DAMAGE_SHARE). */
    public static final DeferredItem<SwordItem> FLYING_NEEDLES = ITEMS.register("flying_needles",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 0, -2.0F))));
    public static final DeferredItem<ShieldItem> RATTAN_SHIELD = ITEMS.register("rattan_shield",
            () -> new ShieldItem(new Item.Properties().durability(280)));

    /**
     * The Murim's money, as in ancient China (docs/items/currency.md), four kinds, each worth nine of the one below and
     * changed at a crafting table (nine into one, one back into nine):
     * <ul>
     *   <li>the copper cash (铜钱), the round bronze coin with its square hole, small change: bandits and crab men carry a
     *   few;</li>
     *   <li>the string of cash (一串钱), nine coins on a red cord;</li>
     *   <li>the silver tael (银两), a small boat-shaped ingot, the money of trade: the Alliance Merchant's prices, the
     *   bosses' and the soldiers' loot;</li>
     *   <li>the gold yuanbao (金元宝), the great gold ingot, for fortunes (the sect's token at the merchant).</li>
     * </ul>
     * Icons from scripts/animation/currency-icons.cjs and silver-tael-icon.cjs.
     */
    public static final DeferredItem<Item> COPPER_CASH = ITEMS.register("copper_cash", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CASH_STRING = ITEMS.register("cash_string", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_TAEL = ITEMS.register("silver_tael", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_YUANBAO = ITEMS.register("gold_yuanbao", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> ALLIANCE_MERCHANT_SPAWN_EGG = ITEMS.register("alliance_merchant_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.ALLIANCE_MERCHANT, 0x2B5FA8, 0xE9D9A6, new Item.Properties()));
    /** The Alliance's soldiers' eggs, in azure, each with its own second colour. */
    public static final DeferredItem<DeferredSpawnEggItem> ALLIANCE_SPEAR_GUARD_SPAWN_EGG = soldierEgg(MurimEntities.ALLIANCE_SPEAR_GUARD, 0xB4BAC0);
    public static final DeferredItem<DeferredSpawnEggItem> ALLIANCE_SWORD_DISCIPLE_SPAWN_EGG = soldierEgg(MurimEntities.ALLIANCE_SWORD_DISCIPLE, 0xEBE6D8);
    public static final DeferredItem<DeferredSpawnEggItem> ALLIANCE_ARCHER_SPAWN_EGG = soldierEgg(MurimEntities.ALLIANCE_ARCHER, 0xC8A96A);
    public static final DeferredItem<DeferredSpawnEggItem> ALLIANCE_WARRIOR_MONK_SPAWN_EGG = soldierEgg(MurimEntities.ALLIANCE_WARRIOR_MONK, 0xB8862E);
    public static final DeferredItem<DeferredSpawnEggItem> ALLIANCE_SERGEANT_SPAWN_EGG = soldierEgg(MurimEntities.ALLIANCE_SERGEANT, 0xE9D9A6);

    private static DeferredItem<DeferredSpawnEggItem> soldierEgg(
            net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<com.murimblock.mob.AllianceSoldierEntity>> type, int spots) {
        return ITEMS.register(type.getId().getPath() + "_spawn_egg", () -> new DeferredSpawnEggItem(type, 0x2B5FA8, spots, new Item.Properties()));
    }

    /**
     * The Captain of the Peak Guard's spear, the Crimson Dragon Spear: a gold-edged leaf blade, a gold dragon's head for
     * its socket, a cross bar set with two cyan gems, a great red mane, a red lacquered shaft banded in gold. A spear for
     * Epic Fight (data/murimblock/capabilities/weapons/crimson_dragon_spear.json), stronger than the soldiers': 7 damage.
     * He leaves it now and then (loot_table/entities/peak_captain.json). Model from scripts/animation/crimson-dragon-spear-model.cjs.
     */
    /**
     * The Traveller's Staff (行者棍), the Traveller's Staff Art's weapon (Enzo's design sheet, 16/10/2026): a plain staff of
     * pale ash, two blocks long, iron ferrules, a red cord at its middle. A spear for Epic Fight
     * (data/murimblock/capabilities/weapons/traveller_staff.json), held at its middle; the art's animations slide it
     * through the hands (scripts/animation/staff-art.cjs). Model from scripts/animation/traveller-staff-model.cjs.
     */
    public static final DeferredItem<SwordItem> TRAVELLER_STAFF = ITEMS.register("traveller_staff",
            () -> new SwordItem(Tiers.WOOD, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.WOOD, 3, -2.6F))));
    public static final DeferredItem<SwordItem> CRIMSON_DRAGON_SPEAR = ITEMS.register("crimson_dragon_spear",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.8F))));

    /**
     * The Peak Guard's armour, the captain's, on a man's build: a plumed iron helm with cheek guards and a neck curtain,
     * a cuirass of gold-laced lamellae with a mirror plate and lion pauldrons, lamellar tassets with knee guards, greaves
     * (MurimArmorMaterials.PEAK_GUARD). He wears it all and drops one piece every time (loot_table/entities/peak_captain.json).
     */
    public static final DeferredItem<ArmorItem> PEAK_GUARD_HELM = peakGuardArmour("peak_guard_helm", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> PEAK_GUARD_LAMELLAR = peakGuardArmour("peak_guard_lamellar", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> PEAK_GUARD_TASSETS = peakGuardArmour("peak_guard_tassets", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> PEAK_GUARD_GREAVES = peakGuardArmour("peak_guard_greaves", ArmorItem.Type.BOOTS);

    private static DeferredItem<ArmorItem> peakGuardArmour(String id, ArmorItem.Type type) {
        return ITEMS.register(id, () -> new ArmorItem(MurimArmorMaterials.PEAK_GUARD, type, new Item.Properties()
                .rarity(Rarity.RARE).durability(type.getDurability(MurimArmorMaterials.PEAK_GUARD_DURABILITY))));
    }

    /** The azure of his coat and the gold of his armour. */
    public static final DeferredItem<DeferredSpawnEggItem> PEAK_CAPTAIN_SPAWN_EGG = ITEMS.register("peak_captain_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.PEAK_CAPTAIN, 0x2B5FA8, 0xE9C46A, new Item.Properties()));

    /** The azure of his robe and the white of his beard. */
    public static final DeferredItem<DeferredSpawnEggItem> AZURE_SENSEI_SPAWN_EGG = ITEMS.register("azure_sensei_spawn_egg",
            () -> new DeferredSpawnEggItem(MurimEntities.AZURE_SENSEI, 0x2F6FC0, 0xEFEBE4, new Item.Properties()));

    /** Jade medallion of the Cloud Cliff sect: its needle points to the sanctuary. */
    public static final DeferredItem<SectTokenItem> SECT_TOKEN = ITEMS.register("sect_token",
            () -> new SectTokenItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /**
     * Locator pendants, sold dear by the Alliance Merchant: like the sect token, a needle that points to the nearest
     * structure of one kind. Only the structures that matter have one (Enzo, 15/10): those with a boss, or with people
     * who give quests (none yet). Each looks like where it leads
     * (scripts/worldgen/PendantTextures.java).
     */
    public static final DeferredItem<LocatorPendantItem> BAMBOO_TEMPLE_PENDANT = pendant("misty_bamboo_temple");
    public static final DeferredItem<LocatorPendantItem> BANDIT_DEN_PENDANT = pendant("yellow_river_bandit_den");
    public static final DeferredItem<LocatorPendantItem> ALLIANCE_ARENA_PENDANT = pendant("alliance_arena");

    private static DeferredItem<LocatorPendantItem> pendant(String structure) {
        return ITEMS.register(structure + "_pendant",
                () -> new LocatorPendantItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), structure));
    }

    /** Every locator pendant, in the merchant's order. */
    public static java.util.List<DeferredItem<LocatorPendantItem>> pendants() {
        return java.util.List.of(BAMBOO_TEMPLE_PENDANT, BANDIT_DEN_PENDANT, ALLIANCE_ARENA_PENDANT);
    }

    /** Manual of the Cloud Cliff sect's art: read, it teaches the Azure Cloud Sword Art and is used up. */
    public static final DeferredItem<MartialManualItem> AZURE_CLOUD_SWORD_MANUAL = ITEMS.register("azure_cloud_sword_manual",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), MartialArts.AZURE_CLOUD_SWORD));

    /** Scroll of the first art of the fist: read, it teaches the Long Fist and is used up. Where it is found is still to decide. */
    /** The Bandit Warlord's scroll: read (from Qi Condensation), it teaches the Mountain-Shattering Fist and is used up. */
    public static final DeferredItem<MartialManualItem> MOUNTAIN_SHATTERING_FIST_SCROLL = ITEMS.register("mountain_shattering_fist_scroll",
            () -> new MartialManualItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), MartialArts.MOUNTAIN_SHATTERING_FIST));
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
                        output.accept(TIGER_SPAWN_EGG.get());
                        output.accept(TIGER_PELT.get());
                        output.accept(TIGER_CLAW.get());
                        output.accept(MOUNTAIN_BANDIT_SPAWN_EGG.get());
                        output.accept(BANDIT_DAO.get());
                        output.accept(BANDIT_CHIEF_SPAWN_EGG.get());
                        output.accept(BANDIT_WARLORD_SPAWN_EGG.get());
                        output.accept(CRAB_KING_SPAWN_EGG.get());
                        output.accept(CHIEF_GREAT_AXE.get());
                        output.accept(ALLIANCE_SPEAR.get());
                        output.accept(SNOW_PLUM_JIAN.get());
                        output.accept(BISHOU.get());
                        output.accept(FLYING_KNIFE.get());
                        output.accept(RATTAN_SHIELD.get());
                        output.accept(IRON_FAN.get());
                        output.accept(CHAIN_WHIP.get());
                        output.accept(FLYING_NEEDLES.get());
                        output.accept(AZURE_SENSEI_SPAWN_EGG.get());
                        output.accept(AZURE_CLOUD_SWORD.get());
                        output.accept(ALLIANCE_MERCHANT_SPAWN_EGG.get());
                        output.accept(ALLIANCE_SPEAR_GUARD_SPAWN_EGG.get());
                        output.accept(ALLIANCE_SWORD_DISCIPLE_SPAWN_EGG.get());
                        output.accept(ALLIANCE_ARCHER_SPAWN_EGG.get());
                        output.accept(ALLIANCE_WARRIOR_MONK_SPAWN_EGG.get());
                        output.accept(ALLIANCE_SERGEANT_SPAWN_EGG.get());
                        output.accept(COPPER_CASH.get());
                        output.accept(CASH_STRING.get());
                        output.accept(SILVER_TAEL.get());
                        output.accept(GOLD_YUANBAO.get());
                        output.accept(CHIEF_BROW_BAND.get());
                        output.accept(CHIEF_LAMELLAR_VEST.get());
                        output.accept(CHIEF_TASSETS.get());
                        output.accept(CHIEF_FUR_BOOTS.get());
                        output.accept(PEAK_CAPTAIN_SPAWN_EGG.get());
                        output.accept(CRIMSON_DRAGON_SPEAR.get());
                        output.accept(PEAK_GUARD_HELM.get());
                        output.accept(PEAK_GUARD_LAMELLAR.get());
                        output.accept(PEAK_GUARD_TASSETS.get());
                        output.accept(PEAK_GUARD_GREAVES.get());
                        MurimBlocks.buildingOrder().forEach(block -> output.accept(block.get()));
                        output.accept(SECT_TOKEN.get());
                        pendants().forEach(pendant -> output.accept(pendant.get()));
                        output.accept(AZURE_CLOUD_SWORD_MANUAL.get());
                        output.accept(LONG_FIST_SCROLL.get());
                        output.accept(MOUNTAIN_SHATTERING_FIST_SCROLL.get());
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
    /** Every weapon of every mod installed, Murimblock's first (WeaponsTab). */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS_TAB = TABS.register("murimblock_weapons",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.murimblock_weapons"))
                    .icon(() -> new ItemStack(ALLIANCE_SPEAR.get()))
                    .withTabsBefore(NATURE_TAB.getKey())
                    .displayItems(WeaponsTab::fill)
                    .build());

    private MurimItems() { }

    public static void register(IEventBus bus) {
        MurimArmorMaterials.MATERIALS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
