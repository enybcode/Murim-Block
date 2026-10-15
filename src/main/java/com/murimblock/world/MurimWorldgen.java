package com.murimblock.world;

import com.murimblock.Murimblock;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.feature.AwayFromSitesFilter;
import com.murimblock.world.feature.CliffPineFeature;
import com.murimblock.world.feature.GroundBodyFeature;
import com.murimblock.world.feature.HangingStrandDecorator;
import com.murimblock.world.feature.MosoBambooFeature;
import com.murimblock.world.feature.RockPillarFeature;
import com.murimblock.world.feature.ShallowPondFeature;
import com.murimblock.world.feature.TieredPineFeature;
import com.murimblock.world.feature.WaterlandsFeature;
import com.murimblock.world.feature.YardangFeature;
import com.murimblock.world.structure.SanctuaryStructure;
import com.murimblock.world.structure.site.SitePiece;
import com.murimblock.world.structure.site.SiteStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import terrablender.api.SurfaceRuleManager;

/**
 * World generation of the Murim biomes: Java features (rock peaks, yardangs, bamboo groves, cliff pines, ponds, the
 * biome's own ground), the hanging-strand tree decorator, the Murim map (biome source, continentalness) and the
 * surface rules (through TerraBlender). The biomes,
 * configured and placed features are data (data/murimblock/worldgen, written by scripts/worldgen/generate-nature.cjs).
 */
public final class MurimWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Murimblock.MOD_ID);
    public static final DeferredRegister<TreeDecoratorType<?>> DECORATORS = DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, Murimblock.MOD_ID);

    public static final DeferredHolder<Feature<?>, RockPillarFeature> ROCK_PILLAR = FEATURES.register("rock_pillar", RockPillarFeature::new);
    public static final DeferredHolder<Feature<?>, YardangFeature> YARDANG = FEATURES.register("yardang", YardangFeature::new);
    public static final DeferredHolder<Feature<?>, MosoBambooFeature> MOSO_BAMBOO_GROVE = FEATURES.register("moso_bamboo_grove", MosoBambooFeature::new);
    public static final DeferredHolder<Feature<?>, CliffPineFeature> CLIFF_PINE = FEATURES.register("cliff_pine", CliffPineFeature::new);
    public static final DeferredHolder<Feature<?>, ShallowPondFeature> SHALLOW_POND = FEATURES.register("shallow_pond", ShallowPondFeature::new);
    public static final DeferredHolder<Feature<?>, GroundBodyFeature> GROUND_BODY = FEATURES.register("ground_body", GroundBodyFeature::new);
    public static final DeferredHolder<Feature<?>, TieredPineFeature> TIERED_PINE = FEATURES.register("tiered_pine", TieredPineFeature::new);
    public static final DeferredHolder<Feature<?>, com.murimblock.world.feature.CamphorTreeFeature> CAMPHOR_TREE =
            FEATURES.register("camphor_tree", com.murimblock.world.feature.CamphorTreeFeature::new);
    public static final DeferredHolder<Feature<?>, WaterlandsFeature> WATERLANDS = FEATURES.register("waterlands", WaterlandsFeature::new);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Murimblock.MOD_ID);
    /** The Cloud Cliff Sanctuary (built by SanctuaryPlacement; registered so /locate structure knows it). */
    public static final DeferredHolder<StructureType<?>, StructureType<SanctuaryStructure>> SANCTUARY_STRUCTURE =
            STRUCTURE_TYPES.register("cloud_cliff_sanctuary", () -> () -> SanctuaryStructure.CODEC);
    /** The Murim sites (docs/structures): real structures placed by structure sets, found by /locate. */
    public static final DeferredHolder<StructureType<?>, StructureType<SiteStructure>> SITE_STRUCTURE =
            STRUCTURE_TYPES.register("site", () -> () -> SiteStructure.CODEC);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Murimblock.MOD_ID);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SITE_PIECE = STRUCTURE_PIECES.register("site", () -> SitePiece::new);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Murimblock.MOD_ID);
    /** Keeps trees, bamboo and rock peaks off the Murim sites. */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<AwayFromSitesFilter>> AWAY_FROM_SITES =
            PLACEMENT_MODIFIERS.register("away_from_sites", () -> () -> AwayFromSitesFilter.CODEC);
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<HangingStrandDecorator>> HANGING_STRANDS =
            DECORATORS.register("hanging_strands", () -> new TreeDecoratorType<>(HangingStrandDecorator.CODEC));

    /** The Murim map (world/map): the overworld's biome source and its shaped continentalness. */
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Murimblock.MOD_ID);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.biome.BiomeSource>, com.mojang.serialization.MapCodec<com.murimblock.world.map.MurimBiomeSource>> MURIM_WORLD =
            BIOME_SOURCES.register("murim_world", () -> com.murimblock.world.map.MurimBiomeSource.CODEC);
    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Murimblock.MOD_ID);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>, com.mojang.serialization.MapCodec<com.murimblock.world.map.MurimContinents>> MURIM_CONTINENTS =
            DENSITY_FUNCTION_TYPES.register("murim_continents", () -> com.murimblock.world.map.MurimContinents.DATA_CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>, com.mojang.serialization.MapCodec<com.murimblock.world.map.MurimUplift>> MURIM_UPLIFT =
            DENSITY_FUNCTION_TYPES.register("murim_uplift", () -> com.murimblock.world.map.MurimUplift.DATA_CODEC);
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction>, com.mojang.serialization.MapCodec<com.murimblock.world.map.MurimJaggedness>> MURIM_JAGGEDNESS =
            DENSITY_FUNCTION_TYPES.register("murim_jaggedness", () -> com.murimblock.world.map.MurimJaggedness.DATA_CODEC);

    private MurimWorldgen() { }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
        DECORATORS.register(bus);
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
        PLACEMENT_MODIFIERS.register(bus);
        BIOME_SOURCES.register(bus);
        DENSITY_FUNCTION_TYPES.register(bus);
        bus.addListener(MurimWorldgen::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // No TerraBlender region any more: the Murim biomes grow only on the Murim map (MurimBiomeSource). TerraBlender
            // still applies their surface rules (it picks rules by the biome's namespace), switched on by MurimSurface.
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Murimblock.MOD_ID, MurimSurfaceRules.rules());
            MurimBlocks.registerFlammability();
        });
    }
}
