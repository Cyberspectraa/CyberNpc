package com.cyberspectraa.cybernpc.entity;

import java.util.Locale;

public enum NpcType {
    MAIN,
    QUEST,
    WILD;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static NpcType fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return MAIN;
        }

        for (NpcType type : values()) {
            if (type.serializedName().equalsIgnoreCase(value)) {
                return type;
            }
        }

        return MAIN;
    }
}
