package com.murimblock.world.map;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Vanilla's terrain offset raised by the mountains of the north ({@link MurimMap#uplift}): used in
 * minecraft:overworld/depth (data/minecraft/worldgen/density_function), so the relief climbs into immense ice ranges
 * there and nowhere else.
 */
public record MurimUplift(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<MurimUplift> DATA_CODEC = DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument")
            .xmap(MurimUplift::new, MurimUplift::argument);
    public static final KeyDispatchDataCodec<MurimUplift> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return argument.compute(context) + MurimMap.uplift(context.blockX(), context.blockZ());
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) {
        argument.fillArray(values, provider);
        for (int i = 0; i < values.length; i++) {
            FunctionContext context = provider.forIndex(i);
            values[i] += MurimMap.uplift(context.blockX(), context.blockZ());
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MurimUplift(argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return argument.minValue();
    }

    @Override
    public double maxValue() {
        return argument.maxValue() + MurimMap.UPLIFT;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
