package com.cyberspectraa.cybernpc.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A room is a bounded 2D floor footprint at ONE Y level. Door blocks are
 * boundaries even when open; scanning never follows stairs to another floor.
 * Any unbounded/unloaded/outdoor scan FAILS instead of claiming half a home.
 */
public final class FloorRoomScanner {
    private static final int LIMIT = 1600;
    private static final int RADIUS = 32;

    public record Result(boolean success, String message, BlockPos inside,
                         Set<Long> cells, Set<Long> doors, Set<Long> beds) {
        static Result fail(String message) {
            return new Result(false, message, BlockPos.ZERO, Set.of(), Set.of(), Set.of());
        }
    }

    private FloorRoomScanner() {}

    public static Result scan(ServerLevel level, BlockPos start) {
        if (!loaded(level, start) || !loaded(level, start.above())
                || !loaded(level, start.below())) {
            return Result.fail("Stand near the room and load its chunks before scanning.");
        }
        if (!standable(level, start)) {
            return Result.fail("Click a solid floor tile inside the room, not a wall, bed or doorway.");
        }
        if (level.canSeeSky(start.above())) {
            return Result.fail("This floor is open to the sky. Add a roof before assigning it as a room.");
        }

        Set<Long> cells = new LinkedHashSet<>();
        Set<Long> doors = new LinkedHashSet<>();
        Set<Long> beds = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        cells.add(start.asLong());
        queue.add(start.immutable());

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            collectBed(level, current, beds);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);
                if (boundaryDoor(level, next)) {
                    doors.add(normalizeDoor(level, next).asLong());
                    continue;
                }
                collectBed(level, next, beds);
                if (!loaded(level, next) || !loaded(level, next.above())
                        || !loaded(level, next.below())) {
                    return Result.fail("A room edge reaches an unloaded chunk. Move closer and rescan.");
                }
                if (!standable(level, next)) continue;
                if (level.canSeeSky(next.above())) {
                    return Result.fail("The room has an opening to the outdoors. Close it with walls or doors.");
                }
                if (Math.abs(next.getX() - start.getX()) > RADIUS
                        || Math.abs(next.getZ() - start.getZ()) > RADIUS) {
                    return Result.fail("The room is too open (over 32 blocks from the clicked floor). Add walls or doors.");
                }
                if (cells.contains(next.asLong())) continue;
                if (cells.size() >= LIMIT) {
                    return Result.fail("Room exceeds 1600 floor spaces. Divide it with walls and doors.");
                }
                cells.add(next.asLong());
                queue.addLast(next.immutable());
            }
        }

        if (doors.isEmpty()) {
            return Result.fail("No door was found around this room. Add at least one door to define its entrance.");
        }
        return new Result(true, "Room scanned", start.immutable(),
                Set.copyOf(cells), Set.copyOf(doors), Set.copyOf(beds));
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        if (boundaryDoor(level, pos)) return false;
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && !floor.getCollisionShape(level, pos.below()).isEmpty();
    }

    private static boolean boundaryDoor(ServerLevel level, BlockPos pos) {
        if (!loaded(level, pos) || !loaded(level, pos.above())) return false;
        return isDoor(level.getBlockState(pos))
                || isDoor(level.getBlockState(pos.above()));
    }

    private static boolean isDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof FenceGateBlock;
    }

    private static BlockPos normalizeDoor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below().immutable();
        }
        return pos.immutable();
    }

    private static void collectBed(ServerLevel level, BlockPos candidate,
                                   Set<Long> beds) {
        if (!loaded(level, candidate)) return;
        BlockState state = level.getBlockState(candidate);
        if (!(state.getBlock() instanceof BedBlock)
                || !state.hasProperty(BedBlock.PART)
                || !state.hasProperty(BedBlock.FACING)) return;
        BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD
                ? candidate : candidate.relative(state.getValue(BedBlock.FACING));
        if (loaded(level, head)) {
            BlockState target = level.getBlockState(head);
            if (target.getBlock() instanceof BedBlock
                    && target.getValue(BedBlock.PART) == BedPart.HEAD) {
                beds.add(head.asLong());
            }
        }
    }

    private static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }
}
