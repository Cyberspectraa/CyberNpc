package com.cyberspectraa.cybernpc.entity;

import net.minecraft.util.RandomSource;

public final class CyberNpcNameGenerator {
    private static final String[] MALE_NAMES = {
            "Adam", "Arthur", "Benjamin", "Callum", "Cameron", "Charlie",
            "Daniel", "Edward", "Elliot", "Ethan", "Felix", "Finn",
            "Freddie", "George", "Harry", "Henry", "Isaac", "Jack",
            "Jacob", "James", "Joseph", "Leo", "Lewis", "Liam",
            "Logan", "Lucas", "Mason", "Matthew", "Max", "Nathan",
            "Noah", "Oliver", "Oscar", "Reece", "Ryan", "Sebastian",
            "Theo", "Thomas", "Toby", "William"
    };

    private static final String[] FEMALE_NAMES = {
            "Alice", "Amelia", "Ava", "Bella", "Chloe", "Daisy",
            "Eleanor", "Emily", "Emma", "Erin", "Evie", "Freya",
            "Grace", "Hannah", "Holly", "Isabelle", "Jessica", "Katie",
            "Layla", "Lily", "Lucy", "Maisie", "Maya", "Mia",
            "Molly", "Olivia", "Phoebe", "Poppy", "Rosie", "Ruby",
            "Sarah", "Scarlett", "Sienna", "Sophia", "Sophie", "Summer",
            "Willow", "Zoe"
    };

    public static String randomWildName(
            RandomSource random,
            NpcAppearance.Gender gender
    ) {
        String[] names = gender == NpcAppearance.Gender.FEMALE
                ? FEMALE_NAMES
                : MALE_NAMES;

        return names[random.nextInt(names.length)];
    }

    /**
     * Compatibility fallback for older callers. New NPCs should always pass
     * their persistent appearance gender so the visible name matches it.
     */
    public static String randomWildName(RandomSource random) {
        return randomWildName(
                random,
                NpcAppearance.Gender.random(random)
        );
    }

    private CyberNpcNameGenerator() {
    }
}
