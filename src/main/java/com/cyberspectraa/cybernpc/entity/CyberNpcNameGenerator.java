package com.cyberspectraa.cybernpc.entity;

import net.minecraft.util.RandomSource;

public final class CyberNpcNameGenerator {
    private static final String[] WILD_NAMES = {
            "Alex", "Arthur", "Bailey", "Ben", "Casey", "Charlie", "Daniel", "Elliot",
            "Ellis", "Emery", "Evan", "Finley", "George", "Harper", "Harry", "Hayden",
            "Jamie", "Jesse", "Jordan", "Kai", "Leo", "Logan", "Morgan", "Noah",
            "Oliver", "Parker", "Quinn", "Reese", "Riley", "Robin", "Rowan", "Sam",
            "Taylor", "Theo", "Toby", "William"
    };

    public static String randomWildName(RandomSource random) {
        return WILD_NAMES[random.nextInt(WILD_NAMES.length)];
    }

    private CyberNpcNameGenerator() {
    }
}
