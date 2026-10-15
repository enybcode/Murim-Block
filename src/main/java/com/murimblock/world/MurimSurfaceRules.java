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
                // Tablelands: mostly bare yellow earth (the real plateau), grass in broad patches.
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.3, Double.MAX_VALUE), block(MurimBlocks.LOESS.get())),
                        block(Blocks.GRASS_BLOCK)))),
                SurfaceRules.ifTrue(underFloor, block(MurimBlocks.LOESS.get()))));

        RuleSource desert = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.HEXI_FRONTIER_DESERT), SurfaceRules.sequence(
                // Gobi: sand drifts over most of it, broad pavements of dark varnished gravel between; sandstone below.
                SurfaceRules.ifTrue(floor, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.2, Double.MAX_VALUE), block(Blocks.SAND)),
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
                // Steep banks: bare rooty earth and coarse soil, mossy in the damp; leaf litter (podzol) under the bamboo.
                SurfaceRules.ifTrue(steep, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(fewPatches, block(Blocks.MOSS_BLOCK)),
                        SurfaceRules.ifTrue(patches, block(Blocks.ROOTED_DIRT)),
                        block(Blocks.COARSE_DIRT))),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(dry, SurfaceRules.ifTrue(patches, block(Blocks.PODZOL))))));

        RuleSource jiangnan = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.JIANGNAN_WATERLANDS), SurfaceRules.sequence(
                // Mud and clay under the water and along the low banks.
                SurfaceRules.ifTrue(SurfaceRules.not(dry), SurfaceRules.ifTrue(floor, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(patches, block(Blocks.CLAY)), block(Blocks.MUD)))),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(64), 0)),
                        block(Blocks.MUD)))));

        RuleSource peach = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.PEACH_BLOSSOM_VALLEY), SurfaceRules.sequence(
                SurfaceRules.ifTrue(steep, block(MurimBlocks.PILLAR_SANDSTONE.get()))));

        // The north (14/10/2026): the cold lays its snow layers and ice on top by itself (freeze_top_layer).
        RuleSource glaciers = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.KUNLUN_GLACIERS), SurfaceRules.sequence(
                // Bare rock on the cliffs, blue ice where the surface noise is high; on the ledges, tongues of glacier ice in
                // the snow.
                SurfaceRules.ifTrue(steep, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(fewPatches, block(Blocks.PACKED_ICE)),
                        block(Blocks.STONE))),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(fewPatches, block(Blocks.PACKED_ICE)),
                        block(Blocks.SNOW_BLOCK)))),
                SurfaceRules.ifTrue(underFloor, block(Blocks.STONE))));

        RuleSource snowfields = SurfaceRules.ifTrue(SurfaceRules.isBiome(MurimBiomes.TIANSHAN_SNOWFIELDS), SurfaceRules.sequence(
                // Stony slopes; broad drifts of packed snow over the frozen meadows.
                SurfaceRules.ifTrue(steep, block(Blocks.STONE)),
                SurfaceRules.ifTrue(floor, SurfaceRules.ifTrue(dry, SurfaceRules.ifTrue(patches, block(Blocks.SNOW_BLOCK))))));

        return SurfaceRules.sequence(karst, loess, desert, peaks, bamboo, jiangnan, peach, glaciers, snowfields);
    }
}
