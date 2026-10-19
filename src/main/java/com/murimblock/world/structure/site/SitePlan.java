package com.murimblock.world.structure.site;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;

/** The Murim sites, by the name their structure JSON gives as {@code plan} (docs/structures). */
public enum SitePlan implements StringRepresentable {
    STILT_VILLAGE("stilt_village", StiltVillage::new),
    BANDIT_DEN("bandit_den", BanditDen::new),
    BEACON_POST("beacon_post", BeaconPost::new),
    BAMBOO_TEMPLE("bamboo_temple", BambooTemple::new),
    LOTUS_GARDEN("lotus_garden", LotusGarden::new),
    PEACH_HAMLET("peach_hamlet", PeachHamlet::new),
    ALLIANCE_OUTPOST("alliance_outpost", AllianceOutpost::new),
    ALLIANCE_ARENA("alliance_arena", AllianceArena::new),
    BANDIT_CAMP("bandit_camp", BanditCamp::new),
    RAVEN_TOWER("raven_tower", RavenTower::new);

    /** The largest reach of any site, in blocks. */
    public static final int MAX_REACH = 60;

    public static final Codec<SitePlan> CODEC = StringRepresentable.fromEnum(SitePlan::values);

    private final String name;
    private final Supplier<Site> factory;
    private Site site;

    SitePlan(String name, Supplier<Site> factory) {
        this.name = name;
        this.factory = factory;
    }

    public Site site() {
        if (site == null) site = factory.get();
        return site;
    }

    @Override public String getSerializedName() {
        return name;
    }

    public static SitePlan byName(String name) {
        for (SitePlan p : values()) if (p.name.equals(name)) return p;
        throw new IllegalArgumentException("Unknown Murim site plan " + name);
    }
}
