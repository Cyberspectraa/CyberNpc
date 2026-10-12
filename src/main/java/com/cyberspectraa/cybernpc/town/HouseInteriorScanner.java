package com.cyberspectraa.cybernpc.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tight on-demand walkable-interior flood scan: never loads chunks or alters
 * blocks, and rejects scans that spill outside the 14-block envelope.
 * The clicked front door is held closed virtually, even when open in-world.
 */
public final class HouseInteriorScanner {
    private static final int HORIZONTAL = 14;
    private static final int VERTICAL = 8;
    private static final int MAX_CELLS = 1200;

    public record Result(boolean success, String message, BlockPos inside, Set<Long> beds) {
        static Result fail(String message) {
            return new Result(false, message, BlockPos.ZERO, Set.of());
        }
    }

    public static Result scan(ServerLevel level, BlockPos doorPos) {
        if (!loaded(level, doorPos)) return Result.fail("House door is not in a loaded chunk.");
        BlockPos door = lowerDoor(level, doorPos);
        BlockState state = level.getBlockState(door);
        if (!(state.getBlock() instanceof DoorBlock)) {
            return Result.fail("Click a two-block door to register a house.");
        }
        Direction direction = state.getValue(DoorBlock.FACING);
        Scan front = flood(level, door.relative(direction), door);
        Scan back = flood(level, door.relative(direction.getOpposite()), door);
        Scan chosen = null;
        if (front != null && !front.escaped && !front.beds.isEmpty()) chosen = front;
        if (back != null && !back.escaped && !back.beds.isEmpty()
                && (chosen == null || back.walkable.size() < chosen.walkable.size())) {
            chosen = back;
        }
        if (chosen == null) {
            if ((front != null && front.escaped) || (back != null && back.escaped)) {
                return Result.fail("House scan reached open ground or the safe size limit. Close wall gaps and try again.");
            }
            return Result.fail("No reachable beds behind this door. Check the interior and any doors between rooms.");
        }
        return new Result(true, "Home scanned", chosen.start, Set.copyOf(chosen.beds));
    }

    private static Scan flood(ServerLevel level, BlockPos start, BlockPos door) {
        if (!standable(level, start, door)) return null;
        Scan scan = new Scan(start.immutable());
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        scan.walkable.add(start.asLong());
        queue.add(start.immutable());

        while (!queue.isEmpty()) {
            if (scan.walkable.size() > MAX_CELLS) {
                scan.escaped = true;
                break;
            }
            BlockPos pos = queue.removeFirst();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = pos.relative(direction);
                collectBed(level, side, scan.beds);
                collectBed(level, side.below(), scan.beds);
                collectBed(level, side.above(), scan.beds);
                enqueue(level, start, door, side, scan, queue);
                BlockState lower = loaded(level, side) ? level.getBlockState(side)
                        : net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState();
                if (lower.getBlock() instanceof StairBlock || lower.getBlock() instanceof SlabBlock) {
                    enqueue(level, start, door, side.above(), scan, queue);
                }
                if (loaded(level, side) && level.getBlockState(side)
                        .getCollisionShape(level, side).isEmpty()) {
                    enqueue(level, start, door, side.below(), scan, queue);
                }
            }
        }
        return scan;
    }

    private static void enqueue(ServerLevel level, BlockPos start, BlockPos door,
                                BlockPos next, Scan scan, ArrayDeque<BlockPos> queue) {
        if (Math.abs(next.getX() - door.getX()) > HORIZONTAL
                || Math.abs(next.getZ() - door.getZ()) > HORIZONTAL
                || Math.abs(next.getY() - door.getY()) > VERTICAL) {
            scan.escaped = true;
            return;
        }
        if (!loaded(level, next)) {
            scan.escaped = true;
            return;
        }
        if (!standable(level, next, door)) return;
        if (scan.walkable.add(next.asLong())) {
            queue.add(next.immutable());
        }
    }

    static boolean standable(ServerLevel level, BlockPos pos, BlockPos door) {
        if (pos.equals(door) || pos.equals(door.above())
                || !loaded(level, pos) || !loaded(level, pos.above())
                || !loaded(level, pos.below())) return false;
        return passable(level, pos, door)
                && passable(level, pos.above(), door)
                && !level.getBlockState(pos.below())
                        .getCollisionShape(level, pos.below()).isEmpty();
    }

    private static boolean passable(ServerLevel level, BlockPos pos, BlockPos door) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock) {
            // Interior doors may be closed; allow the scan to pass through them
            // while never crossing the selected exterior entrance.
            return !pos.equals(door) && !pos.equals(door.above());
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static void collectBed(ServerLevel level, BlockPos candidate, Set<Long> beds) {
        if (!loaded(level, candidate)) return;
        BlockState state = level.getBlockState(candidate);
        if (state.getBlock() instanceof BedBlock
                && state.hasProperty(BedBlock.PART)
                && state.getValue(BedBlock.PART) == BedPart.HEAD) {
            beds.add(candidate.asLong());
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
        boolean escaped;

        Scan(BlockPos start) {
            this.start = start;
        }
    }

    private HouseInteriorScanner() {}
}
