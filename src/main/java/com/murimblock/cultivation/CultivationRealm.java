package com.murimblock.cultivation;

import java.util.Locale;
import java.util.Optional;

public enum CultivationRealm {
    QI_SENSING("qi_sensing", "Qi Sensing", "Perception du Qi", "Humain eveille"),
    QI_GUIDING("qi_guiding", "Qi Guiding", "Guidage du Qi", "Apprenti martial"),
    QI_CONDENSATION("qi_condensation", "Qi Condensation", "Condensation du Qi", "Disciple confirme"),
    QI_LIQUIDATION("qi_liquidation", "Qi Liquidation", "Liquefaction du Qi", "Expert martial"),
    QI_CRYSTALLIZATION("qi_crystallization", "Qi Crystallization", "Cristallisation du Qi", "Maitre martial"),
    FOUNDATION_ESTABLISHMENT("foundation_establishment", "Foundation Establishment", "Etablissement des Fondations", "Grand maitre"),
    CORE_FORMATION("core_formation", "Core Formation", "Formation du Noyau", "Maitre legendaire"),
    NASCENT_SOUL("nascent_soul", "Nascent Soul", "Ame Naissante", "Transcendant"),
    VOID_ENLIGHTENMENT("void_enlightenment", "Void Enlightenment", "Eveil du Vide", "Etre hors du monde");

    private final String serializedName;
    private final String displayName;
    private final String frenchName;
    private final String status;

    CultivationRealm(String serializedName, String displayName, String frenchName, String status) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.frenchName = frenchName;
        this.status = status;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public String frenchName() {
        return frenchName;
    }

    public String status() {
        return status;
    }

    public Optional<CultivationRealm> next() {
        int nextOrdinal = ordinal() + 1;
        return nextOrdinal >= values().length ? Optional.empty() : Optional.of(values()[nextOrdinal]);
    }

    public static Optional<CultivationRealm> bySerializedName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (CultivationRealm realm : values()) {
            if (realm.serializedName.equals(normalized)) {
                return Optional.of(realm);
            }
        }
        return Optional.empty();
    }
}
