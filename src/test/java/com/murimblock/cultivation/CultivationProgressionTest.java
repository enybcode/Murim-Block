package com.murimblock.cultivation;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CultivationProgressionTest {
    @Test
    void initialValueIsQiSensingEarly() {
        assertEquals(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.EARLY), CultivationData.initial());
    }

    @Test
    void qiSensingEarlyAdvancesToMiddle() {
        assertEquals(
                Optional.of(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.MIDDLE)),
                CultivationProgression.getNext(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.EARLY))
        );
    }

    @Test
    void qiSensingMiddleAdvancesToLate() {
        assertEquals(
                Optional.of(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.LATE)),
                CultivationProgression.getNext(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.MIDDLE))
        );
    }

    @Test
    void qiSensingLateAdvancesToPeak() {
        assertEquals(
                Optional.of(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.PEAK)),
                CultivationProgression.getNext(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.LATE))
        );
    }

    @Test
    void qiSensingPeakAdvancesToQiGuidingEarly() {
        assertEquals(
                Optional.of(new CultivationData(CultivationRealm.QI_GUIDING, CultivationStage.EARLY)),
                CultivationProgression.getNext(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.PEAK))
        );
    }

    @Test
    void coreFormationPeakAdvancesToNascentSoulEarly() {
        assertEquals(
                Optional.of(new CultivationData(CultivationRealm.NASCENT_SOUL, CultivationStage.EARLY)),
                CultivationProgression.getNext(new CultivationData(CultivationRealm.CORE_FORMATION, CultivationStage.PEAK))
        );
    }

    @Test
    void qiMaxBelowRequirementCannotAttemptBreakthrough() {
        CultivationData data = new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.EARLY);

        assertFalse(CultivationProgression.canAttemptBreakthrough(data, 99.0));
    }

    @Test
    void qiMaxEqualToRequirementCanAttemptBreakthrough() {
        CultivationData data = new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.EARLY);

        assertTrue(CultivationProgression.canAttemptBreakthrough(data, 100.0));
    }

    @Test
    void qiMaxAboveRequirementCanAttemptBreakthrough() {
        CultivationData data = new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.EARLY);

        assertTrue(CultivationProgression.canAttemptBreakthrough(data, 101.0));
    }

    @Test
    void enoughQiMaxDoesNotAutomaticallyAdvanceCultivation() {
        CultivationData data = new CultivationData(CultivationRealm.QI_GUIDING, CultivationStage.LATE);

        assertTrue(CultivationProgression.canAttemptBreakthrough(data, 500.0));
        assertEquals(new CultivationData(CultivationRealm.QI_GUIDING, CultivationStage.LATE), data);
    }

    @Test
    void minorBreakthroughIsDetectedInsideSameRealm() {
        CultivationData data = new CultivationData(CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);

        assertEquals(Optional.of(BreakthroughType.MINOR), CultivationProgression.getBreakthroughType(data));
    }

    @Test
    void majorBreakthroughIsDetectedBetweenRealms() {
        CultivationData data = new CultivationData(CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);

        assertEquals(Optional.of(BreakthroughType.MAJOR), CultivationProgression.getBreakthroughType(data));
    }

    @Test
    void finalLevelHasNoNextStage() {
        CultivationData data = new CultivationData(CultivationRealm.VOID_ENLIGHTENMENT, CultivationStage.PEAK);

        assertTrue(CultivationProgression.getNext(data).isEmpty());
        assertTrue(CultivationProgression.getRequiredQiMaxForNext(data).isEmpty());
        assertTrue(CultivationProgression.getBreakthroughType(data).isEmpty());
        assertFalse(CultivationProgression.canAttemptBreakthrough(data, Double.MAX_VALUE));
    }

    @Test
    void codecRoundTripPreservesPersistentState() {
        CultivationData expected = new CultivationData(CultivationRealm.CORE_FORMATION, CultivationStage.MIDDLE);
        var encoded = CultivationData.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
        CultivationData restored = CultivationData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(expected, restored);
    }

    @Test
    void resetReturnsQiSensingEarly() {
        assertEquals(CultivationData.initial(), CultivationProgression.reset());
    }
}
