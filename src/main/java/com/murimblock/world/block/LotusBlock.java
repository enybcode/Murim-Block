package com.murimblock.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.WaterlilyBlock;

/** Lotus (Nelumbo nucifera) leaf or leaf with its pink flower, floating on still water like a lily pad. */
public class LotusBlock extends WaterlilyBlock {
    public static final MapCodec<LotusBlock> CODEC = simpleCodec(LotusBlock::new);

    public LotusBlock(Properties properties) {
        super(properties);
    }

    @Override public MapCodec<WaterlilyBlock> codec() {
        return (MapCodec<WaterlilyBlock>) (MapCodec<?>) CODEC;
    }
}
