package com.murimblock.registry;

import com.mojang.serialization.MapCodec;
import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiomeSource;
import com.murimblock.world.MurimContinents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Murim map's world generation types (docs/world/map.md). */
public final class MurimWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Murimblock.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Murimblock.MOD_ID);

    static {
        BIOME_SOURCES.register("murim_map", () -> MurimBiomeSource.CODEC);
        DENSITY_FUNCTIONS.register("murim_continents", () -> MurimContinents.MAP_CODEC);
    }

    private MurimWorldgen() {
    }
}
