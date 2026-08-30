package com.murimblock.cultivation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CultivationProgression {
    private static final Map<CultivationRealm, Map<CultivationStage, Double>> REQUIRED_QI_MAX =
            buildRequiredQiMaxTable();

    private CultivationProgression() {
    }

    public static Optional<CultivationData> getNext(CultivationData data) {
        return data.stage().next()
                .map(nextStage -> new CultivationData(data.realm(), nextStage))
                .or(() -> data.realm().next().map(nextRealm -> new CultivationData(nextRealm, CultivationStage.EARLY)));
    }

    public static Optional<Double> getRequiredQiMaxForNext(CultivationData data) {
        if (getNext(data).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(REQUIRED_QI_MAX.get(data.realm()).get(data.stage()));
    }

    public static boolean canAttemptBreakthrough(CultivationData data, double qiMax) {
        return getRequiredQiMaxForNext(data)
                .map(required -> qiMax >= required)
                .orElse(false);
    }

    public static Optional<BreakthroughType> getBreakthroughType(CultivationData data) {
        return getNext(data).map(next -> data.realm() == next.realm() ? BreakthroughType.MINOR : BreakthroughType.MAJOR);
    }

    public static CultivationData reset() {
        return CultivationData.initial();
    }

    private static Map<CultivationRealm, Map<CultivationStage, Double>> buildRequiredQiMaxTable() {
        // Provisional balancing table. Values are centralized so future gameplay tuning stays contained here.
        double[][] values = {
                {100.0, 150.0, 200.0, 250.0},
                {300.0, 400.0, 500.0, 650.0},
                {750.0, 875.0, 1_000.0, 1_250.0},
                {1_500.0, 1_750.0, 2_000.0, 2_500.0},
                {3_000.0, 3_500.0, 4_000.0, 5_000.0},
                {6_000.0, 7_000.0, 8_000.0, 10_000.0},
                {12_000.0, 14_000.0, 16_000.0, 20_000.0},
                {25_000.0, 30_000.0, 35_000.0, 45_000.0},
                {60_000.0, 75_000.0, 90_000.0, 0.0}
        };

        Map<CultivationRealm, Map<CultivationStage, Double>> table = new EnumMap<>(CultivationRealm.class);
        CultivationRealm[] realms = CultivationRealm.values();
        CultivationStage[] stages = CultivationStage.values();
        for (int realmIndex = 0; realmIndex < realms.length; realmIndex++) {
            Map<CultivationStage, Double> stageRequirements = new EnumMap<>(CultivationStage.class);
            for (int stageIndex = 0; stageIndex < stages.length; stageIndex++) {
                stageRequirements.put(stages[stageIndex], values[realmIndex][stageIndex]);
            }
            table.put(realms[realmIndex], Map.copyOf(stageRequirements));
        }
        return Map.copyOf(table);
    }
}
