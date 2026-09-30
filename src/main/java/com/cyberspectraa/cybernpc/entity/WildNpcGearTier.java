package com.cyberspectraa.cybernpc.entity;

import net.minecraft.util.RandomSource;

import java.util.Locale;

public enum WildNpcGearTier {
    STANDARD("standard", "Standard", 70, 0.0D, 0.0D, 0.0D),
    FINE("fine", "Fine", 20, 2.0D, 0.005D, 0.5D),
    RARE("rare", "Rare", 8, 4.0D, 0.010D, 1.0D),
    ELITE("elite", "Elite", 2, 8.0D, 0.015D, 2.0D);

    private final String serializedName;
    private final String displayName;
    private final int weight;
    private final double healthBonus;
    private final double speedBonus;
    private final double attackBonus;

    WildNpcGearTier(
            String serializedName,
            String displayName,
            int weight,
            double healthBonus,
            double speedBonus,
            double attackBonus
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.weight = weight;
        this.healthBonus = healthBonus;
        this.speedBonus = speedBonus;
        this.attackBonus = attackBonus;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public double healthBonus() {
        return healthBonus;
    }

    public double speedBonus() {
        return speedBonus;
    }

    public double attackBonus() {
        return attackBonus;
    }

    public static WildNpcGearTier randomTier(RandomSource random) {
        int total = 0;
        for (WildNpcGearTier tier : values()) {
            total += tier.weight;
        }

        int roll = random.nextInt(total);
        for (WildNpcGearTier tier : values()) {
            roll -= tier.weight;
            if (roll < 0) {
                return tier;
            }
        }

        return STANDARD;
    }

    public static WildNpcGearTier fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (WildNpcGearTier tier : values()) {
            if (tier.serializedName.equals(normalized)) {
                return tier;
            }
        }

        return STANDARD;
    }
}
