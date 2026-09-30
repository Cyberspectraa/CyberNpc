package com.cyberspectraa.cybernpc.entity;

import net.minecraft.util.RandomSource;

import java.util.Locale;

public enum WildNpcPersonality {
    BALANCED("balanced", "Balanced", 45),
    BRAVE("brave", "Brave", 20),
    CAUTIOUS("cautious", "Cautious", 15),
    AGGRESSIVE("aggressive", "Aggressive", 12),
    TACTICAL("tactical", "Tactical", 8);

    private final String serializedName;
    private final String displayName;
    private final int weight;

    WildNpcPersonality(String serializedName, String displayName, int weight) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.weight = weight;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public int weight() {
        return weight;
    }

    public static WildNpcPersonality fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return BALANCED;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (WildNpcPersonality personality : values()) {
            if (personality.serializedName.equals(normalized)) {
                return personality;
            }
        }

        return BALANCED;
    }

    public static WildNpcPersonality randomPersonality(RandomSource random) {
        int total = 0;
        for (WildNpcPersonality personality : values()) {
            total += personality.weight;
        }

        int roll = random.nextInt(total);
        for (WildNpcPersonality personality : values()) {
            roll -= personality.weight;
            if (roll < 0) {
                return personality;
            }
        }

        return BALANCED;
    }
}
