package com.murimblock.world.feature;

import com.murimblock.world.MurimMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Fills the Murim map's lakes that lie above sea level ({@link MurimMap#LAKES} with a level), whose bowls the relief
 * already dug: water up to the lake's level, and a white salt crust on the shore around it (the Qinghai salt lake).
 * Runs once per chunk and touches only that chunk.
 */
public class HighLakeFeature extends Feature<NoneFeatureConfiguration> {
    /** How far past its radius a lake's banks may still lie under its level. */
    private static final int BANKS = 80;
    /** The salt crust's width above the water. */
    private static final int CRUST = 3;

    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState SALT = Blocks.CALCITE.defaultBlockState();

    public HighLakeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                MurimMap.Lake lake = MurimMap.lakeAt(x, z, BANKS);
                if (lake == null || lake.level() <= 0) {
                    continue;
                }
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (ground < lake.level()) {
                    for (int y = ground; y < lake.level(); y++) {
                        level.setBlock(pos.set(x, y, z), WATER, 2);
                    }
                    any = true;
                } else if (ground < lake.level() + CRUST) {
                    pos.set(x, ground - 1, z);
                    if (level.getBlockState(pos).isSolid()) {
                        level.setBlock(pos, SALT, 2);
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
