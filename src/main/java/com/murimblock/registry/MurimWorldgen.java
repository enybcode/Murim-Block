package com.murimblock.registry;

import com.mojang.serialization.MapCodec;
import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiomeSource;
import com.murimblock.world.MurimContinents;
import com.murimblock.world.MurimRelief;
import com.murimblock.world.feature.PillarForestFeature;
import com.murimblock.world.feature.RockPillarFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Murim map's world generation types (docs/world/map.md). */
public final class MurimWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Murimblock.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Murimblock.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Murimblock.MOD_ID);

    static {
        BIOME_SOURCES.register("murim_map", () -> MurimBiomeSource.CODEC);
        DENSITY_FUNCTIONS.register("murim_continents", () -> MurimContinents.MAP_CODEC);
        DENSITY_FUNCTIONS.register("murim_relief", () -> MurimRelief.MAP_CODEC);
        FEATURES.register("pillar_forest", PillarForestFeature::new);
        FEATURES.register("rock_pillar", RockPillarFeature::new);
    }

    private MurimWorldgen() {
    }
}
