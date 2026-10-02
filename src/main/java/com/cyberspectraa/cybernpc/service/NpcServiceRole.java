package com.cyberspectraa.cybernpc.service;

import java.util.Locale;

public enum NpcServiceRole {
    NONE(""),
    BANKER("Banker"),
    COURIER("Courier"),
    GUARD("Guard");

    private final String displayName;

    NpcServiceRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static NpcServiceRole fromRole(String role) {
        if (role == null || role.isBlank()) {
            return NONE;
        }

        String normalized = role.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "banker", "bank" -> BANKER;
            case "courier", "mailman", "postman", "mail" -> COURIER;
            case "guard", "town_guard", "town guard" -> GUARD;
            default -> NONE;
        };
    }
}
