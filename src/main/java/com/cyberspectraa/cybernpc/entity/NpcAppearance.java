package com.cyberspectraa.cybernpc.entity;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Locale;

public final class NpcAppearance {
    private static final String[] SKIN_TONES = {
            "pale", "medium", "mediumdark", "tan", "tanmedium", "tandark", "dark"
    };

    private static final String[] EYE_STYLES = {
            "low", "middle", "high"
    };

    private static final String[] HAIR_STYLES = {
            "bowlcut",
            "center_parted_short",
            "dreadlocks",
            "shaggy",
            "steve",
            "wavy_long"
    };

    private NpcAppearance() {
    }

    public enum Gender {
        MALE("male", "Male", false),
        FEMALE("female", "Female", true);

        private final String serializedName;
        private final String displayName;
        private final boolean slim;

        Gender(String serializedName, String displayName, boolean slim) {
            this.serializedName = serializedName;
            this.displayName = displayName;
            this.slim = slim;
        }

        public String serializedName() {
            return serializedName;
        }

        public String displayName() {
            return displayName;
        }

        public boolean slim() {
            return slim;
        }

        public static Gender random(RandomSource random) {
            return random.nextBoolean() ? FEMALE : MALE;
        }

        public static Gender fromSerializedName(String value) {
            if (value == null || value.isBlank()) {
                return MALE;
            }

            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Gender gender : values()) {
                if (gender.serializedName.equals(normalized)) {
                    return gender;
                }
            }

            return MALE;
        }
    }

    public static int skinToneCount() {
        return SKIN_TONES.length;
    }

    public static int eyeStyleCount() {
        return EYE_STYLES.length;
    }

    public static int hairStyleCount() {
        return HAIR_STYLES.length;
    }

    public static int sanitizeSkinTone(int index) {
        return Mth.clamp(index, 0, SKIN_TONES.length - 1);
    }

    public static int sanitizeEyeStyle(int index) {
        return Mth.clamp(index, 0, EYE_STYLES.length - 1);
    }

    public static int sanitizeHairStyle(int index) {
        return Mth.clamp(index, 0, HAIR_STYLES.length - 1);
    }

    public static String skinToneKey(int index) {
        return SKIN_TONES[sanitizeSkinTone(index)];
    }

    public static String eyeStyleKey(int index) {
        return EYE_STYLES[sanitizeEyeStyle(index)];
    }

    public static String hairStyleKey(int index) {
        return HAIR_STYLES[sanitizeHairStyle(index)];
    }

    public static String skinToneDisplayName(int index) {
        return pretty(skinToneKey(index));
    }

    public static String eyeStyleDisplayName(int index) {
        return pretty(eyeStyleKey(index));
    }

    public static String hairStyleDisplayName(int index) {
        return pretty(hairStyleKey(index));
    }

    public static String hairColorDisplayName() {
        return "Brown";
    }

    private static String pretty(String key) {
        String[] words = key.split("_");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }

        return builder.toString();
    }
}
