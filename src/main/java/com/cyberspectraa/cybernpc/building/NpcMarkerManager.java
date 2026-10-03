package com.cyberspectraa.cybernpc.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public final class NpcMarkerManager {
    private static final double LINK_RADIUS_SQR = 96.0D * 96.0D;
    private static final int BED_SEARCH_HORIZONTAL = 8;
    private static final int BED_SEARCH_VERTICAL = 4;

    public static RegistrationResult register(
            ServerLevel level,
            BlockPos markerPos,
            NpcMarkerKind kind,
            Direction facing
    ) {
        UUID markerId = NpcMarkerIds.id(
                level.dimension(),
                markerPos,
                kind
        );

        BuildingSavedData data = BuildingSavedData.get(level);

        // Re-registration is deterministic and safe.
        data.removeMarker(markerId);

        return switch (kind) {
            case BUILDING -> registerBuilding(
                    level,
                    markerPos,
                    markerId,
                    data
            );
            case ROOM -> registerRoom(
                    level,
                    markerPos,
                    markerId,
                    data
            );
            case BED -> registerBed(
                    level,
                    markerPos,
                    markerId,
                    data
            );
            case WORK -> registerWork(
                    level,
                    markerPos,
                    markerId,
                    facing,
                    data
            );
        };
    }

    public static void unregister(
            ServerLevel level,
            BlockPos markerPos,
            NpcMarkerKind kind
    ) {
        BuildingSavedData.get(level).removeMarker(
                NpcMarkerIds.id(
                        level.dimension(),
                        markerPos,
                        kind
                )
        );
    }

    public static RegistrationResult status(
            ServerLevel level,
            BlockPos markerPos,
            NpcMarkerKind kind
    ) {
        UUID markerId = NpcMarkerIds.id(
                level.dimension(),
                markerPos,
                kind
        );
        BuildingSavedData data = BuildingSavedData.get(level);
        BuildingSavedData.BuildingRecord building =
                data.findBuildingForMarker(markerId);

        if (building == null) {
            return RegistrationResult.failure(
                    kind == NpcMarkerKind.BUILDING
                            ? "Building marker is not registered."
                            : kind.displayName()
                            + " marker is not linked. Place a Building marker first, then replace this marker."
            );
        }

        if (kind == NpcMarkerKind.BUILDING) {
            return RegistrationResult.success(
                    "Building linked: " + building.name() + "."
            );
        }

        if (kind == NpcMarkerKind.BED) {
            BuildingSavedData.MarkerPoint point =
                    data.findMarkerPoint(markerId);
            return point == null
                    ? RegistrationResult.failure(
                    "No bed is linked to this Bed marker."
            )
                    : RegistrationResult.success(
                    "Bed marker linked to "
                            + building.name() + "."
            );
        }

        if (kind == NpcMarkerKind.WORK) {
            return RegistrationResult.success(
                    "Work marker linked to "
                            + building.name() + "."
            );
        }

        return RegistrationResult.success(
                "Room marker linked to "
                        + building.name() + "."
        );
    }

    private static RegistrationResult registerBuilding(
            ServerLevel level,
            BlockPos markerPos,
            UUID markerId,
            BuildingSavedData data
    ) {
        RoomScanner.ScanResult scan =
                RoomScanner.scan(level, markerPos);

        if (!scan.success()) {
            return RegistrationResult.failure(scan.error());
        }

        // Replace only an old legacy record that owns the exact marker
        // position. Nearby real four-marker buildings are never deleted.
        BuildingSavedData.BuildingRecord legacy =
                data.getBuildingAt(level.dimension(), markerPos);
        if (legacy != null
                && legacy.primaryMarkerId() == null
                && legacy.core().equals(markerPos)) {
            data.removeBuilding(legacy.id());
        }

        UUID buildingId = data.createMarkerBuilding(
                level.dimension(),
                markerId,
                markerPos,
                BuildingType.PUBLIC,
                scan,
                level
        );

        BuildingSavedData.BuildingRecord building =
                data.getRecord(buildingId);

        String name = building == null
                ? "Building"
                : building.name();

        if (scan.truncated()) {
            return RegistrationResult.success(
                    name + " registered. The scan reached its safe limit; add Room markers in any missed sections."
            );
        }

        return RegistrationResult.success(
                name + " registered (" + scan.cellCount()
                        + " interior blocks)."
        );
    }

    private static RegistrationResult registerRoom(
            ServerLevel level,
            BlockPos markerPos,
            UUID markerId,
            BuildingSavedData data
    ) {
        RoomScanner.ScanResult scan =
                RoomScanner.scan(level, markerPos);

        if (!scan.success()) {
            return RegistrationResult.failure(scan.error());
        }

        BuildingSavedData.BuildingRecord building =
                data.findBuildingForRoom(
                        level.dimension(),
                        scan.entrances(),
                        markerPos,
                        LINK_RADIUS_SQR
                );

        if (building == null) {
            return RegistrationResult.failure(
                    "No Building marker is close enough. Place the Building marker first."
            );
        }

        boolean added = data.addMarkerRoom(
                building.id(),
                markerId,
                BuildingMarkerType.RoomKind.PUBLIC_AREA,
                scan,
                level
        );

        if (!added) {
            return RegistrationResult.failure(
                    "Room could not be linked to the Building."
            );
        }

        return RegistrationResult.success(
                "Room linked to " + building.name()
                        + (scan.truncated()
                        ? ". Scan reached its safe limit; another Room marker can extend it."
                        : ".")
        );
    }

    private static RegistrationResult registerBed(
            ServerLevel level,
            BlockPos markerPos,
            UUID markerId,
            BuildingSavedData data
    ) {
        BuildingSavedData.BuildingRecord building =
                findLinkedBuilding(level, markerPos, data);

        if (building == null) {
            return RegistrationResult.failure(
                    "No Building marker is close enough. Place the Building marker first."
            );
        }

        BlockPos bedHead = findNearestBed(level, markerPos);
        if (bedHead == null) {
            return RegistrationResult.failure(
                    "No bed was found within 8 blocks of this marker."
            );
        }

        BlockState state = level.getBlockState(bedHead);
        float yaw = state.hasProperty(BedBlock.FACING)
                ? state.getValue(BedBlock.FACING).toYRot()
                : 0.0F;

        data.addMarkerPoint(
                building.id(),
                markerId,
                BuildingPointType.BED,
                bedHead,
                yaw
        );

        return RegistrationResult.success(
                "Nearest bed linked to " + building.name() + "."
        );
    }

    private static RegistrationResult registerWork(
            ServerLevel level,
            BlockPos markerPos,
            UUID markerId,
            Direction facing,
            BuildingSavedData data
    ) {
        BuildingSavedData.BuildingRecord building =
                findLinkedBuilding(level, markerPos, data);

        if (building == null) {
            return RegistrationResult.failure(
                    "No Building marker is close enough. Place the Building marker first."
            );
        }

        data.addMarkerPoint(
                building.id(),
                markerId,
                BuildingPointType.WORK,
                markerPos,
                facing.toYRot()
        );

        return RegistrationResult.success(
                "Work spot linked to " + building.name()
                        + ". Select an NPC with the Town Register and use it on this marker."
        );
    }

    @Nullable
    private static BuildingSavedData.BuildingRecord findLinkedBuilding(
            ServerLevel level,
            BlockPos markerPos,
            BuildingSavedData data
    ) {
        BuildingSavedData.BuildingRecord inside =
                data.getBuildingAt(
                        level.dimension(),
                        markerPos
                );
        if (inside != null) {
            return inside;
        }

        return data.findBuildingForRoom(
                level.dimension(),
                Set.of(),
                markerPos,
                LINK_RADIUS_SQR
        );
    }

    @Nullable
    private static BlockPos findNearestBed(
            ServerLevel level,
            BlockPos origin
    ) {
        return BlockPos.betweenClosedStream(
                        origin.offset(
                                -BED_SEARCH_HORIZONTAL,
                                -BED_SEARCH_VERTICAL,
                                -BED_SEARCH_HORIZONTAL
                        ),
                        origin.offset(
                                BED_SEARCH_HORIZONTAL,
                                BED_SEARCH_VERTICAL,
                                BED_SEARCH_HORIZONTAL
                        )
                )
                .map(BlockPos::immutable)
                .filter(pos -> {
                    BlockState state = level.getBlockState(pos);
                    return state.getBlock() instanceof BedBlock
                            && state.hasProperty(BedBlock.PART)
                            && state.getValue(BedBlock.PART)
                            == BedPart.HEAD;
                })
                .min(Comparator.comparingDouble(origin::distSqr))
                .orElse(null);
    }

    public record RegistrationResult(
            boolean success,
            String message
    ) {
        private static RegistrationResult success(String message) {
            return new RegistrationResult(true, message);
        }

        private static RegistrationResult failure(String message) {
            return new RegistrationResult(false, message);
        }
    }

    private NpcMarkerManager() {
    }
}
