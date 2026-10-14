package com.murimblock.world;

import com.murimblock.Murimblock;
import com.murimblock.world.block.MurimBlocks;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

/**
 * World generation of the Murim biomes: Java features (rock peaks, yardangs, bamboo groves, cliff pines, ponds, the
 * biome's own ground), the hanging-strand tree decorator, and the TerraBlender region and surface rules. The biomes,
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
    public static final DeferredHolder<Feature<?>, WaterlandsFeature> WATERLANDS = FEATURES.register("waterlands", WaterlandsFeature::new);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Murimblock.MOD_ID);
    /** The Cloud Cliff Sanctuary (built by SanctuaryPlacement; registered so /locate structure knows it). */
    public static final DeferredHolder<StructureType<?>, StructureType<SanctuaryStructure>> SANCTUARY_STRUCTURE =
            STRUCTURE_TYPES.register("cloud_cliff_sanctuary", () -> () -> SanctuaryStructure.CODEC);
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<HangingStrandDecorator>> HANGING_STRANDS =
            DECORATORS.register("hanging_strands", () -> new TreeDecoratorType<>(HangingStrandDecorator.CODEC));

    private MurimWorldgen() { }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
        DECORATORS.register(bus);
        STRUCTURE_TYPES.register(bus);
        bus.addListener(MurimWorldgen::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new MurimRegion(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "overworld")));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, Murimblock.MOD_ID, MurimSurfaceRules.rules());
            MurimBlocks.registerFlammability();
        });
    }
}
