package com.cyberspectraa.cybernpc.building;

import java.util.Locale;

public enum BuildingType {
    HOME("home", "Home", false),
    SHOP("shop", "Shop", true),
    CHURCH("church", "Church", true),
    BANK("bank", "Bank", true),
    POST_OFFICE("post_office", "Post Office", true),
    INN("inn", "Inn", true),
    WORKSHOP("workshop", "Workshop", false),
    GUARD_HOUSE("guard_house", "Guard House", false),
    PUBLIC("public", "Public Building", true),
    PRIVATE("private", "Private Building", false);

    private final String serializedName;
    private final String displayName;
    private final boolean publicAccess;

    BuildingType(
            String serializedName,
            String displayName,
            boolean publicAccess
    ) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.publicAccess = publicAccess;
    }

    public String serializedName() {
        return serializedName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public BuildingType next() {
        BuildingType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static BuildingType fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            return HOME;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (BuildingType type : values()) {
            if (type.serializedName.equals(normalized)
                    || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }

        return HOME;
    }
}
