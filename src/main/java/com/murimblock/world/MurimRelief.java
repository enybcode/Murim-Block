package com.murimblock.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Vanilla's terrain offset or jaggedness, pulled toward the Murim map's {@link MurimMap#relief relief}
 * ({@code murimblock:murim_relief}). {@code part} says which: {@code offset} (the ground height; in
 * {@code overworld/depth}) or {@code jaggedness} (the peaks; in {@code overworld/sloped_cheese}). Each column mixes
 * vanilla's value and ours by the relief's weight, so flat zones lose vanilla's hills and wild ones keep them.
 */
public record MurimRelief(DensityFunction input, String part) implements DensityFunction {
    public static final MapCodec<MurimRelief> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(MurimRelief::input),
            Codec.STRING.fieldOf("part").forGetter(MurimRelief::part)).apply(i, MurimRelief::new));
    public static final KeyDispatchDataCodec<MurimRelief> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        MurimMap.Relief relief = MurimMap.relief(context.blockX(), context.blockZ());
        double vanilla = input.compute(context);
        if (relief.weight() <= 0) {
            return vanilla;
        }
        // Vanilla's ground lies where the offset is (Y - 128) / 128, but its noise squashes high ground: measured in
        // game, the ground above Y 90 came out about 20 % short of the offset's height, hence the boost.
        double height = relief.height();
        if (height > 90) {
            height += .2 * (height - 90);
        }
        double ours = part.equals("offset") ? (height - 128) / 128 : relief.jaggedness();
        return Mth.lerp(relief.weight(), vanilla, ours);
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MurimRelief(input.mapAll(visitor), part));
    }

    @Override
    public double minValue() {
        return Math.min(input.minValue(), part.equals("offset") ? -1 : 0);
    }

    @Override
    public double maxValue() {
        return Math.max(input.maxValue(), part.equals("offset") ? 1.5 : 1.2);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
