package com.murimblock.integration.epicfight;

import yesman.epicfight.world.capabilities.item.WeaponCategory;

/**
 * The weapon types Murimblock adds to Epic Fight's, for the sect weapons it has no type for (as addons such as Dawn Day
 * add theirs): each has its own moveset (MurimWeapons) and its own moves (SectWeaponAnimations). Registered with Epic
 * Fight's enum manager when the mod is built ({@link #register}).
 */
public enum MurimWeaponCategories implements WeaponCategory {
    /** The iron fan (铁扇): one hand, quick flicks of the open fan. */
    IRON_FAN,
    /** The chain whip (九节鞭): one hand, lashes of a long chain. */
    CHAIN_WHIP,
    /** The flying needles: one hand, thrown in fans. */
    FLYING_NEEDLES;

    private final int id;

    MurimWeaponCategories() {
        this.id = WeaponCategory.ENUM_MANAGER.assign(this);
    }

    @Override
    public int universalOrdinal() {
        return id;
    }

    /** Tells Epic Fight about these types; called from the mod's constructor, before its registries are filled. */
    public static void register(String modId) {
        WeaponCategory.ENUM_MANAGER.registerEnumCls(modId, MurimWeaponCategories.class);
    }
}
