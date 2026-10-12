package com.murimblock.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small plant of the Murim biomes (fern, grass tuft, shrub, herb) that grows on the soil it lives on in nature. */
public class MurimPlantBlock extends BushBlock {
    public static final MapCodec<MurimPlantBlock> CODEC = simpleCodec(p -> new MurimPlantBlock(Soil.GRASS, 13, p));
    private final Soil soil;
    private final VoxelShape shape;

    public MurimPlantBlock(Soil soil, int height, Properties properties) {
        super(properties);
        this.soil = soil;
        this.shape = Block.box(2, 0, 2, 14, height, 14);
    }

    @Override protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return soil.accepts(state);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape.move(state.getOffset(level, pos).x, 0, state.getOffset(level, pos).z);
    }

    /** What a plant can root in. */
    public enum Soil {
        /** Grass, dirt, podzol, moss, mud (the vanilla #dirt tag) and farmland. */
        GRASS,
        /** Desert plants: sand, red sand, gravel and the grass/dirt of oases. */
        SAND,
        /** Waterside plants: mud, clay and the #dirt tag. */
        WET;

        public boolean accepts(BlockState state) {
            boolean dirt = state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
            return switch (this) {
                case GRASS -> dirt;
                case SAND -> dirt || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(MurimBlocks.GOBI_GRAVEL.get())
                        || state.is(MurimBlocks.LOESS.get());
                case WET -> dirt || state.is(Blocks.CLAY) || state.is(Blocks.MUD) || state.is(BlockTags.SAND);
            };
        }
    }
}
