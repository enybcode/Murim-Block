package com.murimblock.qi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable, normalized Qi state for one player.
 */
public record QiData(double qi, double qiMax) {
    public static final Codec<QiData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("qi").forGetter(QiData::qi),
            Codec.DOUBLE.fieldOf("qi_max").forGetter(QiData::qiMax)
    ).apply(instance, QiData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QiData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeDouble(data.qi);
                buffer.writeDouble(data.qiMax);
            },
            buffer -> new QiData(buffer.readDouble(), buffer.readDouble())
    );

    public QiData {
        qiMax = normalizeCapacity(qiMax);
        qi = Math.clamp(normalizeQi(qi), 0.0, qiMax);
    }

    public static QiData initial() {
        return new QiData(QiConstants.INITIAL_QI, QiConstants.INITIAL_QI_MAX);
    }

    public QiData withQi(double value) {
        return new QiData(value, qiMax);
    }

    public QiData addQi(double amount) {
        requireNonNegativeFinite(amount, "amount");
        return withQi(saturatedAdd(qi, amount));
    }

    public QiData removeQi(double amount) {
        requireNonNegativeFinite(amount, "amount");
        return withQi(Math.max(0.0, qi - amount));
    }

    public QiData withQiMax(double value) {
        return new QiData(qi, value);
    }

    public QiData addQiMax(double amount) {
        requireNonNegativeFinite(amount, "amount");
        return withQiMax(saturatedAdd(qiMax, amount));
    }

    public QiData removeQiMax(double amount) {
        requireNonNegativeFinite(amount, "amount");
        return withQiMax(Math.max(0.0, qiMax - amount));
    }

    public QiData refill() {
        return withQi(qiMax);
    }

    public QiData regenerateForTicks(long elapsedTicks) {
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks must not be negative");
        }
        if (elapsedTicks == 0 || qi >= qiMax) {
            return this;
        }

        double minutes = (double) elapsedTicks / QiConstants.TICKS_PER_MINUTE;
        double regenerated = qiMax * QiConstants.REGENERATION_PER_MINUTE * minutes;
        return addQi(regenerated);
    }

    private static double normalizeQi(double value) {
        if (Double.isNaN(value) || value <= 0.0) {
            return 0.0;
        }
        return Double.isInfinite(value) ? Double.MAX_VALUE : value;
    }

    private static double normalizeCapacity(double value) {
        if (Double.isNaN(value) || value <= 0.0) {
            return 0.0;
        }
        return Double.isInfinite(value) ? Double.MAX_VALUE : value;
    }

    private static double saturatedAdd(double left, double right) {
        return left > Double.MAX_VALUE - right ? Double.MAX_VALUE : left + right;
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be a non-negative finite number");
        }
    }
}
