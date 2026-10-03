package com.cyberspectraa.cybernpc.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One-time interior scanner used by Building and Room markers.
 *
 * Important design rule: a scan is never rejected for being "too big".
 * The scanner saves everything it safely found and marks the result as
 * truncated. A Room marker can then extend the same Building record.
 */
public final class RoomScanner {
    private static final int MAX_CELLS = 120_000;
    private static final int MAX_HORIZONTAL_DISTANCE = 96;
    private static final int MAX_VERTICAL_DISTANCE = 48;

    public static ScanResult scan(
            ServerLevel level,
            BlockPos preferredStart
    ) {
        BlockPos start = findStart(level, preferredStart);
        if (start == null) {
            return ScanResult.failed(
                    "Place the marker in open space inside the building."
            );
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Set<BlockPos> entrances = new LinkedHashSet<>();
        Set<BlockPos> beds = new LinkedHashSet<>();

        queue.add(start);
        visited.add(start.asLong());

        boolean truncated = false;

        scan:
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);

                if (Math.abs(next.getX() - start.getX())
                        > MAX_HORIZONTAL_DISTANCE
                        || Math.abs(next.getZ() - start.getZ())
                        > MAX_HORIZONTAL_DISTANCE
                        || Math.abs(next.getY() - start.getY())
                        > MAX_VERTICAL_DISTANCE) {
                    truncated = true;
                    continue;
                }

                BlockState state = level.getBlockState(next);

                if (isEntrance(state)) {
                    entrances.add(normalizeEntrance(state, next));
                }

                if (state.getBlock() instanceof BedBlock) {
                    BlockPos head = normalizeBedHead(level, next);
                    if (head != null) {
                        beds.add(head);
                    }
                }

                if (!isInteriorSpace(level, next, state, start)) {
                    continue;
                }

                long packed = next.asLong();
                if (!visited.add(packed)) {
                    continue;
                }

                if (visited.size() >= MAX_CELLS) {
                    truncated = true;
                    break scan;
                }

                queue.addLast(next.immutable());
            }
        }

        List<BlockPos> cells = visited.stream()
                .map(value -> BlockPos.of(value.longValue()))
                .sorted(Comparator
                        .comparingInt((BlockPos pos) -> pos.getY())
                        .thenComparingInt(BlockPos::getZ)
                        .thenComparingInt(BlockPos::getX))
                .toList();

        return new ScanResult(
                true,
                "",
                start,
                compress(cells),
                entrances,
                beds,
                cells.size(),
                truncated
        );
    }

    @Nullable
    private static BlockPos findStart(
            ServerLevel level,
            BlockPos preferred
    ) {
        BlockPos[] candidates = new BlockPos[]{
                preferred,
                preferred.above(),
                preferred.below(),
                preferred.north(),
                preferred.south(),
                preferred.east(),
                preferred.west()
        };

        BlockPos fallback = null;

        for (BlockPos candidate : candidates) {
            BlockState state = level.getBlockState(candidate);
            if (!isPassable(level, candidate, state)) {
                continue;
            }

            if (!level.canSeeSky(candidate.above())) {
                return candidate.immutable();
            }

            if (fallback == null) {
                fallback = candidate.immutable();
            }
        }

        return fallback;
    }

    private static boolean isInteriorSpace(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            BlockPos start
    ) {
        if (!isPassable(level, pos, state)) {
            return false;
        }

        // This is the key leak guard. Exterior air is not allowed to become
        // part of a Building even if the front door is wide open.
        return pos.equals(start)
                || !level.canSeeSky(pos.above());
    }

    private static boolean isPassable(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }

        // Doors/gates are semantic boundaries but are still connections
        // between rooms. Treating them as traversable lets one Building
        // marker discover most normal multi-room buildings automatically.
        if (isEntrance(state)) {
            return true;
        }

        // Beds are furniture, not walls. They can be crossed by the scan even
        // though NPC pathfinding will still use their real collision.
        if (state.getBlock() instanceof BedBlock) {
            return true;
        }

        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isEntrance(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapDoorBlock
                || state.getBlock() instanceof FenceGateBlock;
    }

    private static BlockPos normalizeEntrance(
            BlockState state,
            BlockPos pos
    ) {
        if (state.getBlock() instanceof DoorBlock
                && state.hasProperty(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF
        )
                && state.getValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF
        ) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
            return pos.below().immutable();
        }

        return pos.immutable();
    }

    @Nullable
    private static BlockPos normalizeBedHead(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)
                || !state.hasProperty(BedBlock.PART)
                || !state.hasProperty(BedBlock.FACING)) {
            return null;
        }

        if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return pos.immutable();
        }

        BlockPos head = pos.relative(state.getValue(BedBlock.FACING));
        BlockState headState = level.getBlockState(head);

        return headState.getBlock() instanceof BedBlock
                && headState.hasProperty(BedBlock.PART)
                && headState.getValue(BedBlock.PART) == BedPart.HEAD
                ? head.immutable()
                : null;
    }

    private static List<BuildingSavedData.Zone> compress(
            List<BlockPos> cells
    ) {
        Map<Integer, Set<Long>> byY = new HashMap<>();

        for (BlockPos pos : cells) {
            byY.computeIfAbsent(
                    pos.getY(),
                    ignored -> new HashSet<>()
            ).add(packXZ(pos.getX(), pos.getZ()));
        }

        List<BuildingSavedData.Zone> flat = new ArrayList<>();

        for (Map.Entry<Integer, Set<Long>> entry : byY.entrySet()) {
            int y = entry.getKey();
            Set<Long> remaining = new HashSet<>(entry.getValue());

            while (!remaining.isEmpty()) {
                long first = remaining.iterator().next();
                int x0 = unpackX(first);
                int z0 = unpackZ(first);

                int x1 = x0;
                while (remaining.contains(packXZ(x1 + 1, z0))) {
                    x1++;
                }

                int z1 = z0;
                boolean canGrow = true;

                while (canGrow) {
                    int nextZ = z1 + 1;
                    for (int x = x0; x <= x1; x++) {
                        if (!remaining.contains(packXZ(x, nextZ))) {
                            canGrow = false;
                            break;
                        }
                    }

                    if (canGrow) {
                        z1 = nextZ;
                    }
                }

                for (int z = z0; z <= z1; z++) {
                    for (int x = x0; x <= x1; x++) {
                        remaining.remove(packXZ(x, z));
                    }
                }

                flat.add(new BuildingSavedData.Zone(
                        new BlockPos(x0, y, z0),
                        new BlockPos(x1, y, z1)
                ));
            }
        }

        flat.sort(Comparator
                .comparingInt((BuildingSavedData.Zone zone) ->
                        zone.min().getX())
                .thenComparingInt(zone -> zone.min().getZ())
                .thenComparingInt(zone -> zone.max().getX())
                .thenComparingInt(zone -> zone.max().getZ())
                .thenComparingInt(zone -> zone.min().getY()));

        List<BuildingSavedData.Zone> merged = new ArrayList<>();

        for (BuildingSavedData.Zone zone : flat) {
            boolean joined = false;

            for (int i = 0; i < merged.size(); i++) {
                BuildingSavedData.Zone existing = merged.get(i);

                if (existing.min().getX() == zone.min().getX()
                        && existing.max().getX() == zone.max().getX()
                        && existing.min().getZ() == zone.min().getZ()
                        && existing.max().getZ() == zone.max().getZ()
                        && existing.max().getY() + 1
                        == zone.min().getY()) {
                    merged.set(
                            i,
                            new BuildingSavedData.Zone(
                                    existing.min(),
                                    new BlockPos(
                                            existing.max().getX(),
                                            zone.max().getY(),
                                            existing.max().getZ()
                                    )
                            )
                    );
                    joined = true;
                    break;
                }
            }

            if (!joined) {
                merged.add(zone);
            }
        }

        return merged;
    }

    private static long packXZ(int x, int z) {
        return ((long) x & 0xffffffffL)
                | (((long) z & 0xffffffffL) << 32);
    }

    private static int unpackX(long packed) {
        return (int) packed;
    }

    private static int unpackZ(long packed) {
        return (int) (packed >> 32);
    }

    public record ScanResult(
            boolean success,
            String error,
            BlockPos start,
            List<BuildingSavedData.Zone> zones,
            Set<BlockPos> entrances,
            Set<BlockPos> beds,
            int cellCount,
            boolean truncated
    ) {
        private static ScanResult failed(String error) {
            return new ScanResult(
                    false,
                    error,
                    BlockPos.ZERO,
                    List.of(),
                    Set.of(),
                    Set.of(),
                    0,
                    false
            );
        }
    }

    private RoomScanner() {
    }
}
