package com.murimblock.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Vanilla's continentalness, bent to the Murim map ({@code murimblock:murim_continents}, wrapped around vanilla's in
 * {@code minecraft:overworld/continents}): land everywhere on the continent, a coast at its edge, deep sea around.
 * Inside, vanilla's value still varies above the inland threshold, so the relief keeps its variety.
 */
public record MurimContinents(DensityFunction input) implements DensityFunction {
    public static final MapCodec<MurimContinents> MAP_CODEC = DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input")
            .xmap(MurimContinents::new, MurimContinents::input);
    public static final KeyDispatchDataCodec<MurimContinents> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** Vanilla's coast lies between -0.19 and -0.11; inland starts above. */
    private static final double COAST = -0.19;
    private static final double INLAND = 0.05;

    @Override
    public double compute(FunctionContext context) {
        double land = MurimMap.landDistance(context.blockX(), context.blockZ());
        if (land >= 300) {
            return Math.max(input.compute(context), INLAND);
        }
        if (land >= 0) {
            return Mth.lerp(land / 300, COAST, Math.max(input.compute(context), INLAND));
        }
        if (land > -500) {
            return Mth.lerp(-land / 500, COAST, -0.6);
        }
        return Mth.lerp(Math.min(1, (-land - 500) / 1000), -0.6, -1.0);
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MurimContinents(input.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return Math.min(input.minValue(), -1.0);
    }

    @Override
    public double maxValue() {
        return Math.max(input.maxValue(), INLAND);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
