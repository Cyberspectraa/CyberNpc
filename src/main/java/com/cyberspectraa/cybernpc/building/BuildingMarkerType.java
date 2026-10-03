package com.cyberspectraa.cybernpc.building;

import javax.annotation.Nullable;

public enum BuildingMarkerType {
    HOME("Home", BuildingType.HOME, RoomKind.MAIN, null),
    SHOP("Shop", BuildingType.SHOP, RoomKind.MAIN, null),
    CHURCH("Church", BuildingType.CHURCH, RoomKind.MAIN, null),
    BANK("Bank", BuildingType.BANK, RoomKind.MAIN, null),
    POST_OFFICE("Post Office", BuildingType.POST_OFFICE, RoomKind.MAIN, null),
    INN("Inn", BuildingType.INN, RoomKind.MAIN, null),

    PUBLIC_AREA("Public Area", null, RoomKind.PUBLIC_AREA, null),
    STAFF_ONLY("Staff Only", null, RoomKind.STAFF_ONLY, null),
    BEDROOM("Bedroom", null, RoomKind.BEDROOM, null),

    ALTAR("Altar", null, null, BuildingPointType.ALTAR),
    COUNTER("Counter", null, null, BuildingPointType.COUNTER);

    private final String displayName;
    @Nullable
    private final BuildingType buildingType;
    @Nullable
    private final RoomKind roomKind;
    @Nullable
    private final BuildingPointType pointType;

    BuildingMarkerType(
            String displayName,
            @Nullable BuildingType buildingType,
            @Nullable RoomKind roomKind,
            @Nullable BuildingPointType pointType
    ) {
        this.displayName = displayName;
        this.buildingType = buildingType;
        this.roomKind = roomKind;
        this.pointType = pointType;
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
