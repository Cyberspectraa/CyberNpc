package com.cyberspectraa.cybernpc.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * One-click house scan, including large connected multi-floor houses.
 *
 * A hard distance and reachable-cell budget prevent unlimited scanning;
 * roof/shelter detection prevents a second entrance, open garden, or double
 * door from flooding the surrounding town. We never request chunk loading.
 */
public final class HouseInteriorScanner {
    // 0.44.48: 128 horizontal / 48 vertical, up from 48 / 16.
    private static final int HORIZONTAL = 128;
    private static final int VERTICAL = 48;
    // Even within the expanded envelope, cap actual traversed interior space.
    private static final int MAX_CELLS = 25000;
    // Supports tall atriums and halls without treating exposed streets as rooms.
    private static final int ROOF_SEARCH_HEIGHT = 72;

    public record Result(boolean success, String message, BlockPos inside,
                         Set<Long> beds, boolean partial) {
        static Result fail(String message) {
            return new Result(false, message, BlockPos.ZERO, Set.of(), false);
        }
    }

    public static Result scan(ServerLevel level, BlockPos doorPos) {
        if (!loaded(level, doorPos)) {
            return Result.fail("House door is not in a loaded chunk.");
        }
        BlockPos door = lowerDoor(level, doorPos);
        BlockState state = level.getBlockState(door);
        if (!(state.getBlock() instanceof DoorBlock)) {
            return Result.fail("Click the front door of the house.");
        }

        Direction facing = state.getValue(DoorBlock.FACING);
        Scan front = flood(level, door.relative(facing), door);
        Scan back = flood(level, door.relative(facing.getOpposite()), door);
        Scan chosen = null;

        if (front != null && front.complete() && !front.beds.isEmpty()) {
            chosen = front;
        }
        if (back != null && back.complete() && !back.beds.isEmpty()
                && (chosen == null
                || back.beds.size() > chosen.beds.size()
                || (back.beds.size() == chosen.beds.size()
                && back.walkable.size() < chosen.walkable.size()))) {
            chosen = back;
        }

        if (chosen != null) {
            return new Result(true, "Home scanned (" + chosen.walkable.size()
                    + " accessible indoor spaces)", chosen.start, Set.copyOf(chosen.beds),
                    chosen.stoppedUnloaded);
        }

        // Explain the actual blocker rather than blaming house size every time.
        if ((front != null && front.stoppedUnloaded)
                || (back != null && back.stoppedUnloaded)) {
            return Result.fail("No reachable beds found in loaded indoor rooms. "
                    + "The scan also encountered unloaded chunks; increase server view "
                    + "distance or keep the far rooms loaded, then rescan.");
        }
        if ((front != null && front.stoppedBoundary)
                || (back != null && back.stoppedBoundary)) {
            return Result.fail("House exceeds the 128-block horizontal / 48-block "
                    + "vertical scan range from its front door.");
        }
        if ((front != null && front.stoppedBudget)
                || (back != null && back.stoppedBudget)) {
            return Result.fail("House scan reached the 25000-room-space safety budget. "
                    + "Check whether an open passage connects the house to other covered buildings.");
        }
        if (front == null && back == null) {
            return Result.fail("Neither side of this door has a walkable, roofed "
                    + "interior. Check the floor, entrance, roof, and door position.");
        }
        return Result.fail("No beds were found in connected indoor rooms. "
                + "The scanner follows doors and stairs, and ignores open streets.");
    }

    private static Scan flood(ServerLevel level, BlockPos start, BlockPos door) {
        if (!standable(level, start, door)) return null;
        Scan scan = new Scan(start.immutable());
        if (!sheltered(level, start, scan)) return null;

        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        scan.walkable.add(start.asLong());
        pending.add(start.immutable());

        while (!pending.isEmpty() && scan.complete()) {
            BlockPos pos = pending.removeFirst();

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = pos.relative(direction);
                collectBed(level, side, scan);
                collectBed(level, side.below(), scan);
                collectBed(level, side.above(), scan);
                enqueue(level, door, side, scan, pending);

                if (!scan.complete()) break;
                BlockState sideState = loaded(level, side) ? level.getBlockState(side) : null;
                if (sideState != null
                        && (sideState.getBlock() instanceof StairBlock
                        || sideState.getBlock() instanceof SlabBlock)) {
                    enqueue(level, door, side.above(), scan, pending);
                }
                if (!scan.complete()) break;
                if (sideState != null
                        && sideState.getCollisionShape(level, side).isEmpty()) {
                    enqueue(level, door, side.below(), scan, pending);
                }
                if (!scan.complete()) break;
            }
        }
        return scan;
    }

    private static void enqueue(ServerLevel level, BlockPos door, BlockPos next,
                                Scan scan, ArrayDeque<BlockPos> pending) {
        if (scan.walkable.contains(next.asLong())) return;
        if (!loaded(level, next) || !loaded(level, next.above())
                || !loaded(level, next.below())) {
            // Stop at this chunk without forcing it to load. Successfully
            // scanned beds can still register as a PARTIAL home scan; a
            // refresh must preserve the older, currently unloaded bed slots.
            scan.stoppedUnloaded = true;
            return;
        }
        if (!standable(level, next, door) || !sheltered(level, next, scan)) return;

        if (Math.abs(next.getX() - door.getX()) > HORIZONTAL
                || Math.abs(next.getZ() - door.getZ()) > HORIZONTAL
                || Math.abs(next.getY() - door.getY()) > VERTICAL) {
            scan.stoppedBoundary = true;
            return;
        }
        if (scan.walkable.size() >= MAX_CELLS) {
            scan.stoppedBudget = true;
            return;
        }
        scan.walkable.add(next.asLong());
        pending.add(next.immutable());
    }

    /**
     * Scanning does not require walls to be hermetically sealed: a roofed
     * walkable interior is enough. In particular, a *second* exterior door
     * must never let the search wander endlessly into the outside village.
     */
    private static boolean sheltered(ServerLevel level, BlockPos standPos, Scan scan) {
        long key = standPos.asLong();
        Boolean cached = scan.roofCache.get(key);
        if (cached != null) return cached;

        boolean sheltered = false;
        if (!level.canSeeSky(standPos)) {
            for (int height = 2; height <= ROOF_SEARCH_HEIGHT; height++) {
                BlockPos above = standPos.above(height);
                if (above.getY() >= level.getMaxBuildHeight()) break;
                BlockState block = level.getBlockState(above);
                // A tree canopy should not count as a house roof.
                if (!(block.getBlock() instanceof LeavesBlock)
                        && !block.getCollisionShape(level, above).isEmpty()) {
                    sheltered = true;
                    break;
                }
            }
        }
        scan.roofCache.put(key, sheltered);
        return sheltered;
    }

    static boolean standable(ServerLevel level, BlockPos pos, BlockPos door) {
        if (pos.equals(door) || pos.equals(door.above())
                || !loaded(level, pos) || !loaded(level, pos.above())
                || !loaded(level, pos.below())) {
            return false;
        }
        return passable(level, pos, door)
                && passable(level, pos.above(), door)
                && !level.getBlockState(pos.below())
                .getCollisionShape(level, pos.below()).isEmpty();
    }

    private static boolean passable(ServerLevel level, BlockPos pos, BlockPos door) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) {
            // Interior doors (including closed ones) connect rooms, but the
            // clicked entrance is deliberately not an interior connection.
            return !pos.equals(door) && !pos.equals(door.above());
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static void collectBed(ServerLevel level, BlockPos candidate, Scan scan) {
        if (!loaded(level, candidate)) return;
        BlockState state = level.getBlockState(candidate);
        if (state.getBlock() instanceof BedBlock
                && state.hasProperty(BedBlock.PART)
                && state.getValue(BedBlock.PART) == BedPart.HEAD
                && sheltered(level, candidate.above(), scan)) {
            scan.beds.add(candidate.asLong());
        }
    }

    public static BlockPos lowerDoor(ServerLevel level, BlockPos clicked) {
        if (!loaded(level, clicked)) return clicked;
        BlockState state = level.getBlockState(clicked);
        if (state.getBlock() instanceof DoorBlock
                && state.hasProperty(DoorBlock.HALF)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return clicked.below();
        }
        return clicked;
    }

    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static final class Scan {
        final BlockPos start;
        final Set<Long> walkable = new LinkedHashSet<>();
        final Set<Long> beds = new LinkedHashSet<>();
        final Map<Long, Boolean> roofCache = new HashMap<>();
        boolean stoppedUnloaded;
        boolean stoppedBoundary;
        boolean stoppedBudget;

        Scan(BlockPos start) {
            this.start = start;
        }

        boolean complete() {
            // The loaded portion of a large home is useful and safe; only
            // hard range/work-budget failures make a scan unusable.
            return !stoppedBoundary && !stoppedBudget;
        }
    }

    private HouseInteriorScanner() {}
}
