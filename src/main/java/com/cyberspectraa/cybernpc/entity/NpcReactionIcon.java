package com.cyberspectraa.cybernpc.entity;

import java.util.Locale;

public enum NpcReactionIcon {
    NONE("", 0),
    HAPPY(":)", 1),
    THINKING("...", 1),
    CONFUSED("?", 2),
    FRIENDLY("<3", 2),
    GREETING("o/", 2),
    FOOD("♥+", 2),
    SLEEP("☾", 2),
    HOME("⌂", 2),
    MOUNT("♞", 2),
    BEAST("✦", 3),
    GROUP_INVITE("+?", 3),
    GROUP_ACCEPT("+!", 3),
    DANGER("⚠", 4),
    COMBAT("⚔", 4),
    SURPRISED("!", 3),
    SAD(":(", 3),
    ANNOYED("-_-", 4),
    SCARED("!!", 5),
    ANGRY(">:[", 6);

    private final String glyph;
    private final int priority;

    NpcReactionIcon(String glyph, int priority) {
        this.glyph = glyph;
        this.priority = priority;
    }

    public String glyph() {
        return glyph;
    }

    public int priority() {
        return priority;
    }

    public static NpcReactionIcon fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }
}
