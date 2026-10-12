package com.murimblock.world.feature;

import com.murimblock.world.block.MurimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Jiangnan pond: a shallow (1 to 2 blocks) irregular sheet of still water on flat ground, mud and clay bottom, lotus
 * leaves and flowers floating on it. Only on near-flat land, so it reads as a polder pond, not a hole.
 */
public class ShallowPondFeature extends Feature<NoneFeatureConfiguration> {
    public ShallowPondFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        int rx = 4 + random.nextInt(6), rz = 3 + random.nextInt(5);
        // Flat enough: the rim varies by one block at most.
        for (int dx = -rx; dx <= rx; dx += rx) for (int dz = -rz; dz <= rz; dz += rz) {
            int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX() + dx, origin.getZ() + dz);
            if (Math.abs(h - surface) > 1) return false;
        }
        int water = surface - 1;
        double phase = random.nextDouble() * Math.PI * 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                if (!inside(dx, dz, rx, rz, phase)) continue;
                double d = Math.pow((double) dx / rx, 2) + Math.pow((double) dz / rz, 2);
                int depth = d < 0.35 ? 2 : 1;
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                // Keep the water inside: every side must be solid ground or more pond.
                boolean contained = true;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockState s = level.getBlockState(pos.set(x + side.getStepX(), water, z + side.getStepZ()));
                    if (!s.isSolid() && !inside(dx + side.getStepX(), dz + side.getStepZ(), rx, rz, phase)) contained = false;
                }
                if (!contained) continue;
                for (int y = 0; y < depth; y++) level.setBlock(pos.set(x, water - y, z), Blocks.WATER.defaultBlockState(), 2);
                level.setBlock(pos.set(x, water - depth, z), random.nextFloat() < 0.7f ? Blocks.MUD.defaultBlockState() : Blocks.CLAY.defaultBlockState(), 2);
                for (int y = water + 1; y <= water + 3; y++) {
                    BlockState above = level.getBlockState(pos.set(x, y, z));
                    if (!above.isAir() && !above.is(BlockTags.REPLACEABLE) && !above.is(BlockTags.FLOWERS)) break;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                if (random.nextFloat() < 0.16f) {
                    BlockState pad = random.nextFloat() < 0.3f ? MurimBlocks.LOTUS_BLOSSOM.get().defaultBlockState() : MurimBlocks.LOTUS_PAD.get().defaultBlockState();
                    level.setBlock(pos.set(x, water + 1, z), pad, 2);
                }
            }
        }
        return true;
    }

    private static boolean inside(int dx, int dz, int rx, int rz, double phase) {
        double a = Math.atan2(dz, dx);
        double edge = 1 + 0.18 * Math.sin(a * 3 + phase) + 0.1 * Math.sin(a * 5 - phase);
        return Math.pow((double) dx / rx, 2) + Math.pow((double) dz / rz, 2) <= edge * edge;
    }
}
