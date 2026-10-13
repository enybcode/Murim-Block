package com.murimblock.world;

import com.murimblock.world.block.MurimBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

/**
 * Surface of the Murim biomes (the top blocks; GroundBodyFeature does the rock underneath). Anything not matched here
 * falls back to the vanilla overworld rules (grass, dirt, snow, beaches...).
 */
public final class MurimSurfaceRules {
    private MurimSurfaceRules() { }

    private static RuleSource block(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static RuleSource rules() {
        ConditionSource steep = SurfaceRules.steep();
        ConditionSource floor = SurfaceRules.ON_FLOOR;
        ConditionSource underFloor = SurfaceRules.UNDER_FLOOR;
        ConditionSource dry = SurfaceRules.waterBlockCheck(-1, 0);
        ConditionSource patches = SurfaceRules.noiseCondition(Noises.SURFACE, 0.12, Double.MAX_VALUE);
        ConditionSource fewPatches = SurfaceRules.noiseCondition(Noises.SURFACE, 0.35, Double.MAX_VALUE);

        RuleSource karst = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.JADE_KARST_FOREST), SurfaceRules.sequence(
                // Bare limestone on steep slopes, mossy where the surface noise is high.
                SurfaceRules.ifTrue(steep, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(fewPatches, block(MurimBlocks.MOSSY_KARST_LIMESTONE.get())),
                        block(MurimBlocks.KARST_LIMESTONE.get()))),
                // Riverside and wet hollows: gravel and clay bars along the Li.
                SurfaceRules.ifTrue(SurfaceRules.not(dry), SurfaceRules.ifTrue(floor, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(patches, block(Blocks.CLAY)), block(Blocks.GRAVEL))))));

        RuleSource loess = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.LOESS_PLATEAU), SurfaceRules.sequence(
                // Gully walls and steep faces: bare loess; a red fossil-soil band at some heights.
                SurfaceRules.ifTrue(steep, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(96), 0),
                                SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(99), 0)), block(MurimBlocks.RED_LOESS.get()))),
                        block(MurimBlocks.LOESS.get()))),
                // Tablelands: thin grass on loess, wide bare loess where the steppe is worn.
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.05, Double.MAX_VALUE), block(MurimBlocks.LOESS.get())),
                        block(Blocks.GRASS_BLOCK)))),
                SurfaceRules.ifTrue(underFloor, block(MurimBlocks.LOESS.get()))));

        RuleSource desert = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.HEXI_FRONTIER_DESERT), SurfaceRules.sequence(
                // Gobi: dark varnished gravel plains with drifts of sand; sandstone below.
                SurfaceRules.ifTrue(floor, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(patches, block(Blocks.SAND)),
                        block(MurimBlocks.GOBI_GRAVEL.get()))),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 4, CaveSurface.FLOOR),
                        block(Blocks.SANDSTONE)),
                SurfaceRules.ifTrue(underFloor, block(Blocks.SAND))));

        RuleSource peaks = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.CLOUD_SEA_PEAKS), SurfaceRules.sequence(
                // Sheer pale granite; above 160 the ledges hold snow, below that moss and grass.
                SurfaceRules.ifTrue(steep, block(MurimBlocks.PALE_GRANITE.get())),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(170), 0), block(Blocks.SNOW_BLOCK))),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(fewPatches, block(Blocks.MOSS_BLOCK)))));

        RuleSource bamboo = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.SHU_BAMBOO_SEA), SurfaceRules.sequence(
                // Red beds of the Sichuan basin on cliffs; leaf litter (podzol) under the bamboo.
                SurfaceRules.ifTrue(steep, block(Blocks.RED_TERRACOTTA)),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(dry, SurfaceRules.ifTrue(patches, block(Blocks.PODZOL))))));

        RuleSource jiangnan = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.JIANGNAN_WATERLANDS), SurfaceRules.sequence(
                // Mud and clay under the water and along the low banks.
                SurfaceRules.ifTrue(SurfaceRules.not(dry), SurfaceRules.ifTrue(floor, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(patches, block(Blocks.CLAY)), block(Blocks.MUD)))),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(64), 0)),
                        block(Blocks.MUD)))));

        RuleSource peach = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.PEACH_BLOSSOM_VALLEY), SurfaceRules.sequence(
                SurfaceRules.ifTrue(steep, block(MurimBlocks.PILLAR_SANDSTONE.get()))));

        return SurfaceRules.sequence(karst, loess, desert, peaks, bamboo, jiangnan, peach);
    }
}
