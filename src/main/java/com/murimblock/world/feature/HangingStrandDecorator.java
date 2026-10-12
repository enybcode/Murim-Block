package com.murimblock.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.murimblock.world.MurimWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Strands hanging from the underside of a crown: weeping willow fronds, banyan aerial roots. Each leaf with air below
 * has `probability` to start a strand of `length` blocks, stopping at the first block that is not air.
 */
public class HangingStrandDecorator extends TreeDecorator {
    public static final MapCodec<HangingStrandDecorator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("block").forGetter(d -> d.block),
            Codec.floatRange(0, 1).fieldOf("probability").forGetter(d -> d.probability),
            IntProvider.codec(1, 32).fieldOf("length").forGetter(d -> d.length)
    ).apply(i, HangingStrandDecorator::new));

    private final BlockStateProvider block;
    private final float probability;
    private final IntProvider length;

    public HangingStrandDecorator(BlockStateProvider block, float probability, IntProvider length) {
        this.block = block;
        this.probability = probability;
        this.length = length;
    }

    @Override protected TreeDecoratorType<?> type() {
        return MurimWorldgen.HANGING_STRANDS.get();
    }

    @Override public void place(Context context) {
        RandomSource random = context.random();
        for (BlockPos leaf : context.leaves()) {
            if (random.nextFloat() >= probability || !context.isAir(leaf.below())) continue;
            int n = length.sample(random);
            BlockPos.MutableBlockPos pos = leaf.below().mutable();
            for (int i = 0; i < n && context.isAir(pos); i++) {
                context.setBlock(pos.immutable(), block.getState(random, pos));
                pos.move(0, -1, 0);
            }
        }
    }
}
