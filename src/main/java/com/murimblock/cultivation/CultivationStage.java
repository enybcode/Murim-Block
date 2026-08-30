package com.murimblock.cultivation;

import java.util.Locale;
import java.util.Optional;

public enum CultivationStage {
    EARLY("early", "Early"),
    MIDDLE("middle", "Middle"),
    LATE("late", "Late"),
    PEAK("peak", "Peak");

    private final String serializedName;
    private final String displayName;

    CultivationStage(String serializedName, String displayName) {
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public Optional<CultivationStage> next() {
        int nextOrdinal = ordinal() + 1;
        return nextOrdinal >= values().length ? Optional.empty() : Optional.of(values()[nextOrdinal]);
    }

    public static Optional<CultivationStage> bySerializedName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (CultivationStage stage : values()) {
            if (stage.serializedName.equals(normalized)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
