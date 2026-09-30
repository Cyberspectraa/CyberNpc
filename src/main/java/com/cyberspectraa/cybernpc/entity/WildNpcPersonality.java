package com.cyberspectraa.cybernpc.entity;

import net.minecraft.util.RandomSource;

import java.util.Locale;

public enum WildNpcPersonality {
    BALANCED(
            "balanced", "Balanced", 24,
            0, 0.0D, 8.0F, 0.60F, 0.95D,
            0, 0, 0.0D, 1.0D
    ),
    BRAVE(
            "brave", "Brave", 12,
            8, 6.0D, 6.0F, 0.45F, 0.88D,
            -1, -1, -0.25D, 0.90D
    ),
    CAUTIOUS(
            "cautious", "Cautious", 10,
            -10, -8.0D, 10.0F, 0.75F, 1.18D,
            2, 2, 0.80D, 1.20D
    ),
    AGGRESSIVE(
            "aggressive", "Aggressive", 10,
            15, 9.0D, 5.0F, 0.40F, 0.82D,
            -2, -1, -0.45D, 0.75D
    ),
    TACTICAL(
            "tactical", "Tactical", 10,
            0, 2.0D, 8.0F, 0.70F, 1.00D,
            0, 1, 0.20D, 1.45D
    ),
    RECKLESS(
            "reckless", "Reckless", 6,
            20, 15.0D, 4.5F, 0.30F, 0.75D,
            -3, -2, -0.70D, 0.65D
    ),
    PROTECTIVE(
            "protective", "Protective", 7,
            -2, 1.0D, 8.0F, 0.82F, 1.00D,
            0, 1, 0.15D, 1.10D
    ),
    LOYAL(
            "loyal", "Loyal", 6,
            0, 4.0D, 7.0F, 0.85F, 0.92D,
            0, 0, 0.05D, 1.05D
    ),
    OPPORTUNISTIC(
            "opportunistic", "Opportunistic", 5,
            5, 5.0D, 7.5F, 0.58F, 0.90D,
            -1, 0, 0.10D, 1.25D
    ),
    SKITTISH(
            "skittish", "Skittish", 4,
            -15, -14.0D, 12.0F, 0.90F, 1.28D,
            2, 3, 1.20D, 1.35D
    ),
    STUBBORN(
            "stubborn", "Stubborn", 3,
            10, 10.0D, 5.5F, 0.50F, 0.84D,
            1, 2, -0.20D, 0.85D
    ),
    PATIENT(
            "patient", "Patient", 3,
            -5, 0.0D, 9.0F, 0.68F, 1.04D,
            1, 0, 0.55D, 1.30D
    );

    private final String serializedName;
    private final String displayName;
    private final int weight;
    private final int aggressionModifier;
    private final double confidenceModifier;
    private final float emergencyHealth;
    private final float helpHealthFraction;
    private final double threatRequiredRatio;
    private final int attackCooldownModifier;
    private final int recoveryModifier;
    private final double spacingOffset;
    private final double strafeMultiplier;

    WildNpcPersonality(
            String serializedName,
            String displayName,
            int weight,
            int aggressionModifier,
            double confidenceModifier,
            float emergencyHealth,
            float helpHealthFraction,
            double threatRequiredRatio,
            int attackCooldownModifier,
            int recoveryModifier,
            double spacingOffset,
            double strafeMultiplier
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.weight = weight;
        this.aggressionModifier = aggressionModifier;
        this.confidenceModifier = confidenceModifier;
        this.emergencyHealth = emergencyHealth;
        this.helpHealthFraction = helpHealthFraction;
        this.threatRequiredRatio = threatRequiredRatio;
        this.attackCooldownModifier = attackCooldownModifier;
        this.recoveryModifier = recoveryModifier;
        this.spacingOffset = spacingOffset;
        this.strafeMultiplier = strafeMultiplier;
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

    public int aggressionModifier() {
        return aggressionModifier;
    }

    public double confidenceModifier() {
        return confidenceModifier;
    }

    public float emergencyHealth() {
        return emergencyHealth;
    }

    public float helpHealthFraction() {
        return helpHealthFraction;
    }

    public double threatRequiredRatio() {
        return threatRequiredRatio;
    }

    public int attackCooldownModifier() {
        return attackCooldownModifier;
    }

    public int recoveryModifier() {
        return recoveryModifier;
    }

    public double spacingOffset() {
        return spacingOffset;
    }

    public double strafeMultiplier() {
        return strafeMultiplier;
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
