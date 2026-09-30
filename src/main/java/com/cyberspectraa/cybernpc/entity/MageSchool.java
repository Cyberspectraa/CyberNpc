package com.cyberspectraa.cybernpc.entity;

import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Locale;

public enum MageSchool {
    FIRE(
            "fire",
            "Fire",
            "pyromancer",
            "blaze_spell_book",
            List.of("firebolt", "fireball", "flaming_strike", "magma_bomb")
    ),
    ICE(
            "ice",
            "Ice",
            "cryomancer",
            null,
            List.of("icicle", "frostwave", "snowball", "ray_of_frost")
    ),
    LIGHTNING(
            "lightning",
            "Lightning",
            "electromancer",
            null,
            List.of("lightning_bolt", "chain_lightning", "electrocute", "shockwave")
    ),
    NATURE(
            "nature",
            "Nature",
            "plagued",
            "druidic_spell_book",
            List.of("acid_orb", "poison_arrow", "root", "poison_splash")
    ),
    HOLY(
            "holy",
            "Holy",
            "priest",
            "villager_spell_book",
            List.of("guiding_bolt", "divine_smite", "heal", "wisp")
    ),
    ENDER(
            "ender",
            "Ender",
            "shadowwalker",
            "dragonskin_spell_book",
            List.of("magic_missile", "magic_arrow", "dragon_breath", "evasion")
    ),
    BLOOD(
            "blood",
            "Blood",
            "cultist",
            "necronomicon_spell_book",
            List.of("blood_needles", "blood_slash", "wither_skull", "ray_of_siphoning")
    ),
    EVOCATION(
            "evocation",
            "Evocation",
            "archevoker",
            "evoker_spell_book",
            List.of("fang_strike", "spectral_hammer", "slow", "firecracker")
    );

    private final String serializedName;
    private final String displayName;
    private final String armorPrefix;
    private final String preferredSpellBookId;
    private final List<String> spellIds;

    MageSchool(
            String serializedName,
            String displayName,
            String armorPrefix,
            String preferredSpellBookId,
            List<String> spellIds
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.armorPrefix = armorPrefix;
        this.preferredSpellBookId = preferredSpellBookId;
        this.spellIds = spellIds;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public String armorPrefix() {
        return armorPrefix;
    }

    public String preferredSpellBookId() {
        return preferredSpellBookId;
    }

    public List<String> spellIds() {
        return spellIds;
    }

    public static MageSchool randomSchool(RandomSource random) {
        MageSchool[] values = values();
        return values[random.nextInt(values.length)];
    }

    public static MageSchool fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return FIRE;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MageSchool school : values()) {
            if (school.serializedName.equals(normalized)) {
                return school;
            }
        }

        return FIRE;
    }
}
