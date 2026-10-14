package com.murimblock.world;

import com.murimblock.Murimblock;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Saplings of the Murim trees grow the same configured features the biomes generate
 * (data/murimblock/worldgen/configured_feature). The banyan also grows from four saplings into its giant form.
 */
public final class MurimTrees {
    public static final TreeGrower OSMANTHUS = grower("osmanthus", null, "osmanthus");
    public static final TreeGrower BANYAN = grower("banyan", "banyan_giant", "banyan");
    public static final TreeGrower HUANGSHAN_PINE = grower("huangshan_pine", null, "huangshan_pine");
    public static final TreeGrower JUJUBE = grower("jujube", null, "jujube");
    public static final TreeGrower DOVE_TREE = grower("dove_tree", null, "dove_tree");
    public static final TreeGrower WILLOW = grower("willow", null, "willow");
    public static final TreeGrower PEACH = grower("peach", null, "peach");
    public static final TreeGrower DESERT_POPLAR = grower("desert_poplar", null, "desert_poplar");
    public static final TreeGrower TIANSHAN_SPRUCE = grower("tianshan_spruce", null, "tianshan_spruce");

    private MurimTrees() { }

    public static ResourceKey<ConfiguredFeature<?, ?>> feature(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }

    private static TreeGrower grower(String name, String mega, String regular) {
        return new TreeGrower(Murimblock.MOD_ID + "_" + name, Optional.ofNullable(mega).map(MurimTrees::feature),
                Optional.of(feature(regular)), Optional.empty());
    }
}
