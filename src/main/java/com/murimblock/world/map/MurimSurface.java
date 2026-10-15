package com.murimblock.world.map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import terrablender.api.SurfaceRuleManager;
import terrablender.worldgen.IExtendedNoiseGeneratorSettings;

/**
 * The Murim surface rules (MurimSurfaceRules, added to TerraBlender's default overworld rules) on a world with the
 * Murim map: TerraBlender only switches its rules on for the worlds whose biomes it places itself (a multi-noise
 * source); this does it for the Murim one, before any chunk is generated.
 */
public final class MurimSurface {
    private MurimSurface() {
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        for (LevelStem stem : event.getServer().registryAccess().registryOrThrow(Registries.LEVEL_STEM)) {
            if (stem.generator() instanceof NoiseBasedChunkGenerator generator && generator.getBiomeSource() instanceof MurimBiomeSource) {
                ((IExtendedNoiseGeneratorSettings) (Object) generator.generatorSettings().value()).setRuleCategory(SurfaceRuleManager.RuleCategory.OVERWORLD);
            }
        }
    }
}
