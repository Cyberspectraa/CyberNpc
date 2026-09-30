package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.compat.IronSpellsCompat;
import net.minecraft.util.RandomSource;

import java.util.Locale;

public enum WildNpcClass {
    CLASSLESS("classless", "Classless", 78),
    ARCHER("archer", "Archer", 8),
    KNIGHT("knight", "Knight", 6),
    ROGUE("rogue", "Rogue", 5),
    MAGE("mage", "Mage", 3);

    private final String serializedName;
    private final String displayName;
    private final int spawnWeight;

    WildNpcClass(String serializedName, String displayName, int spawnWeight) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.spawnWeight = spawnWeight;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public int spawnWeight() {
        return spawnWeight;
    }

    public static WildNpcClass fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return CLASSLESS;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (WildNpcClass npcClass : values()) {
            if (npcClass.serializedName.equals(normalized)) {
                return npcClass;
            }
        }

        return CLASSLESS;
    }

    public static WildNpcClass randomSpawnClass(RandomSource random) {
        boolean allowMage = IronSpellsCompat.isLoaded();

        int total = 0;
        for (WildNpcClass npcClass : values()) {
            if (npcClass == MAGE && !allowMage) {
                continue;
            }
            total += npcClass.spawnWeight;
        }

        int roll = random.nextInt(total);
        for (WildNpcClass npcClass : values()) {
            if (npcClass == MAGE && !allowMage) {
                continue;
            }

            roll -= npcClass.spawnWeight;
            if (roll < 0) {
                return npcClass;
            }
        }

        return CLASSLESS;
    }

    public boolean isAvailable() {
        return this != MAGE || IronSpellsCompat.isLoaded();
    }
}
