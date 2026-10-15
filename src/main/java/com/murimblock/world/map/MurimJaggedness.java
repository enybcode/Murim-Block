package com.murimblock.world.map;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The jaggedness of the north's ranges ({@link MurimMap#jaggedness}), added to vanilla's in
 * minecraft:overworld/sloped_cheese: on the high ranges, the terrain breaks into spires and knife-edge ridges as
 * vanilla's jagged peaks do.
 */
public final class MurimJaggedness implements DensityFunction.SimpleFunction {
    public static final MurimJaggedness INSTANCE = new MurimJaggedness();
    public static final MapCodec<MurimJaggedness> DATA_CODEC = MapCodec.unit(INSTANCE);
    public static final KeyDispatchDataCodec<MurimJaggedness> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    private MurimJaggedness() {
    }

    @Override
    public double compute(FunctionContext context) {
        return MurimMap.jaggedness(context.blockX(), context.blockZ());
    }

    @Override
    public double minValue() {
        return 0;
    }

    @Override
    public double maxValue() {
        return MurimMap.JAGGED;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
