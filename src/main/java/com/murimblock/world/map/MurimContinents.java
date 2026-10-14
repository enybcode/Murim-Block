package com.murimblock.world.map;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Vanilla's continentalness shaped by the Murim map ({@link MurimMap#continents}): replaces
 * minecraft:overworld/continents (data/minecraft/worldgen/density_function), so the relief and vanilla's own biome
 * choice both see the Murim lands as land and the ring round them as a deep ocean.
 */
public record MurimContinents(DensityFunction argument) implements DensityFunction {
    public static final MapCodec<MurimContinents> DATA_CODEC = DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument")
            .xmap(MurimContinents::new, MurimContinents::argument);
    public static final KeyDispatchDataCodec<MurimContinents> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return MurimMap.continents(context.blockX(), context.blockZ(), argument.compute(context));
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) {
        argument.fillArray(values, provider);
        for (int i = 0; i < values.length; i++) {
            FunctionContext context = provider.forIndex(i);
            values[i] = MurimMap.continents(context.blockX(), context.blockZ(), values[i]);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MurimContinents(argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return Math.min(argument.minValue(), MurimMap.DEEP_SEA);
    }

    @Override
    public double maxValue() {
        return Math.max(argument.maxValue(), MurimMap.LAND);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
