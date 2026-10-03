package com.cyberspectraa.cybernpc.building;

public enum NpcMarkerKind {
    BUILDING("Building"),
    ROOM("Room"),
    BED("Bed"),
    WORK("Work");

    private final String displayName;

    NpcMarkerKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
