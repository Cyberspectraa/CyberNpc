package com.cyberspectraa.cybernpc.building;

import java.util.Locale;

public enum BuildingPointType {
    ALTAR("altar", "Altar"),
    BED("bed", "Bed"),
    WORK("work", "Work"),
    COUNTER("counter", "Counter"),
    SOCIAL("social", "Social"),
    SEATING("seating", "Seating");

    private final String serializedName;
    private final String displayName;

    BuildingPointType(String serializedName, String displayName) {
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public static BuildingPointType fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return SOCIAL;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (BuildingPointType type : values()) {
            if (type.serializedName.equals(normalized)
                    || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }

        return SOCIAL;
    }
}
