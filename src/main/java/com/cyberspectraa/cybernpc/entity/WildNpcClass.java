package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.compat.BetterHorsesCompat;
import com.cyberspectraa.cybernpc.compat.IronSpellsCompat;
import net.minecraft.util.RandomSource;

import java.util.Locale;

public enum WildNpcClass {
    CLASSLESS("classless", "Classless", 65, false),
    ARCHER("archer", "Archer", 8, false),
    KNIGHT("knight", "Knight", 6, false),
    ROGUE("rogue", "Rogue", 5, false),
    BERSERKER("berserker", "Berserker", 5, false),
    BEAST_TAMER("beast_tamer", "Beast Tamer", 3, false),
    HORSE_TAMER("horse_tamer", "Horse Tamer", 3, false),
    MAGE("mage", "Mage", 3, true),
    CLERIC("cleric", "Cleric", 2, true),
    SPELLBLADE("spellblade", "Spellblade", 3, true);

    private final String serializedName;
    private final String displayName;
    private final int spawnWeight;
    private final boolean requiresIronSpells;

    WildNpcClass(
            String serializedName,
            String displayName,
            int spawnWeight,
            boolean requiresIronSpells
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.spawnWeight = spawnWeight;
        this.requiresIronSpells = requiresIronSpells;
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

    public boolean requiresIronSpells() {
        return requiresIronSpells;
    }

    public boolean usesSpellBook() {
        return this == MAGE || this == CLERIC || this == SPELLBLADE;
    }

    public boolean hasMagicSchool() {
        return this == MAGE || this == CLERIC || this == SPELLBLADE;
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
        int total = 0;
        for (WildNpcClass npcClass : values()) {
            if (!npcClass.isAvailable()) {
                continue;
            }
            total += npcClass.spawnWeight;
        }

        int roll = random.nextInt(total);
        for (WildNpcClass npcClass : values()) {
            if (!npcClass.isAvailable()) {
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
        if (this == HORSE_TAMER && !BetterHorsesCompat.isLoaded()) {
            return false;
        }

        return !requiresIronSpells || IronSpellsCompat.isLoaded();
    }
}
