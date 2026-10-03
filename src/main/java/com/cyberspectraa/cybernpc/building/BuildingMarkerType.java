package com.cyberspectraa.cybernpc.building;

import javax.annotation.Nullable;

/**
 * Marker types are deliberately simple. They do not scan the structure.
 * Instead each marker owns a small, fixed influence area so setup can never
 * fail because a build is large, irregular, open-plan or highly decorated.
 */
public enum BuildingMarkerType {
    HOME("Home", BuildingType.HOME, RoomKind.MAIN, null, 10, 6),
    SHOP("Shop", BuildingType.SHOP, RoomKind.MAIN, null, 16, 8),
    CHURCH("Church", BuildingType.CHURCH, RoomKind.MAIN, null, 32, 12),
    BANK("Bank", BuildingType.BANK, RoomKind.MAIN, null, 16, 8),
    POST_OFFICE("Post Office", BuildingType.POST_OFFICE, RoomKind.MAIN, null, 16, 8),
    INN("Inn", BuildingType.INN, RoomKind.MAIN, null, 20, 10),

    PUBLIC_AREA("Public Area", null, RoomKind.PUBLIC_AREA, null, 12, 8),
    STAFF_ONLY("Staff Only", null, RoomKind.STAFF_ONLY, null, 8, 6),
    BEDROOM("Bedroom", null, RoomKind.BEDROOM, null, 6, 5),

    ALTAR("Altar", null, null, BuildingPointType.ALTAR, 0, 0),
    COUNTER("Counter", null, null, BuildingPointType.COUNTER, 0, 0);

    private final String displayName;
    @Nullable
    private final BuildingType buildingType;
    @Nullable
    private final RoomKind roomKind;
    @Nullable
    private final BuildingPointType pointType;
    private final int horizontalRadius;
    private final int verticalRadius;

    BuildingMarkerType(
            String displayName,
            @Nullable BuildingType buildingType,
            @Nullable RoomKind roomKind,
            @Nullable BuildingPointType pointType,
            int horizontalRadius,
            int verticalRadius
    ) {
        this.displayName = displayName;
        this.buildingType = buildingType;
        this.roomKind = roomKind;
        this.pointType = pointType;
        this.horizontalRadius = horizontalRadius;
        this.verticalRadius = verticalRadius;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isPrimary() {
        return buildingType != null && roomKind == RoomKind.MAIN;
    }

    public boolean isRoomMarker() {
        return roomKind != null;
    }

    public boolean isPointMarker() {
        return pointType != null;
    }

    @Nullable
    public BuildingType buildingType() {
        return buildingType;
    }

    @Nullable
    public RoomKind roomKind() {
        return roomKind;
    }

    @Nullable
    public BuildingPointType pointType() {
        return pointType;
    }

    public int horizontalRadius() {
        return horizontalRadius;
    }

    public int verticalRadius() {
        return verticalRadius;
    }

    public enum RoomKind {
        MAIN(false),
        PUBLIC_AREA(false),
        STAFF_ONLY(true),
        BEDROOM(true);

        private final boolean restricted;

        RoomKind(boolean restricted) {
            this.restricted = restricted;
        }

        public boolean restricted() {
            return restricted;
        }
    }
}
