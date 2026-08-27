package com.murimblock.qi;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QiDataTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void newPlayerStartsFullAtOneHundred() {
        QiData data = QiData.initial();

        assertEquals(100.0, data.qi(), EPSILON);
        assertEquals(100.0, data.qiMax(), EPSILON);
    }

    @Test
    void qiAlwaysStaysInsideItsCapacity() {
        assertEquals(100.0, new QiData(500.0, 100.0).qi(), EPSILON);
        assertEquals(0.0, new QiData(-50.0, 100.0).qi(), EPSILON);
        assertEquals(0.0, new QiData(50.0, -100.0).qiMax(), EPSILON);
    }

    @Test
    void addAndRemoveQiClampAtBothLimits() {
        QiData data = new QiData(950.0, 1_000.0).addQi(200.0);
        assertEquals(1_000.0, data.qi(), EPSILON);

        data = new QiData(100.0, 1_000.0).removeQi(500.0);
        assertEquals(0.0, data.qi(), EPSILON);
    }

    @Test
    void regenerationIsOnePercentPerMinute() {
        QiData oneHundred = new QiData(50.0, 100.0)
                .regenerateForTicks(QiConstants.TICKS_PER_MINUTE);
        QiData oneThousand = new QiData(100.0, 1_000.0)
                .regenerateForTicks(QiConstants.TICKS_PER_MINUTE);

        assertEquals(51.0, oneHundred.qi(), EPSILON);
        assertEquals(110.0, oneThousand.qi(), EPSILON);
    }

    @Test
    void fractionalRegenerationIsPreserved() {
        QiData data = new QiData(0.0, 50.0)
                .regenerateForTicks(QiConstants.TICKS_PER_MINUTE);

        assertEquals(0.5, data.qi(), EPSILON);
    }

    @Test
    void scheduledRegenerationIntervalsAddUpToOneMinute() {
        QiData data = new QiData(50.0, 100.0);
        int intervals = QiConstants.TICKS_PER_MINUTE / QiConstants.REGENERATION_INTERVAL_TICKS;

        for (int i = 0; i < intervals; i++) {
            data = data.regenerateForTicks(QiConstants.REGENERATION_INTERVAL_TICKS);
        }

        assertEquals(51.0, data.qi(), EPSILON);
    }

    @Test
    void regenerationStopsExactlyAtMaximum() {
        QiData data = new QiData(999.0, 1_000.0)
                .regenerateForTicks(QiConstants.TICKS_PER_MINUTE);

        assertEquals(1_000.0, data.qi(), EPSILON);
    }

    @Test
    void increasingCapacityDoesNotRefillQi() {
        QiData data = QiData.initial().addQiMax(900.0);

        assertEquals(100.0, data.qi(), EPSILON);
        assertEquals(1_000.0, data.qiMax(), EPSILON);
    }

    @Test
    void reducingCapacityClampsCurrentQi() {
        QiData data = new QiData(800.0, 1_000.0).withQiMax(500.0);

        assertEquals(500.0, data.qi(), EPSILON);
        assertEquals(500.0, data.qiMax(), EPSILON);
    }

    @Test
    void refillOnlyFillsToCurrentCapacity() {
        QiData data = new QiData(250.0, 1_000.0).refill();

        assertEquals(1_000.0, data.qi(), EPSILON);
        assertEquals(1_000.0, data.qiMax(), EPSILON);
    }

    @Test
    void codecRoundTripPreservesPersistentState() {
        QiData expected = new QiData(380.5, 1_200.0);
        var encoded = QiData.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
        QiData restored = QiData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(expected, restored);
    }

    @Test
    void negativeOperationAmountsAreRejected() {
        QiData data = QiData.initial();

        assertThrows(IllegalArgumentException.class, () -> data.addQi(-1.0));
        assertThrows(IllegalArgumentException.class, () -> data.removeQiMax(-1.0));
    }
}
