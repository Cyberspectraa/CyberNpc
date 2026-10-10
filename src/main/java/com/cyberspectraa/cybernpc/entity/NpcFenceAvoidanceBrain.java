package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Lightweight recovery for the common vanilla behaviour where pathfinding
 * returns a partial path ending against a fence because the actual target is
 * on the inaccessible other side.
 *
 * This does not replace Minecraft pathfinding and does not continuously scan.
 * It only wakes up when an NPC with an active path is physically close to a
 * fence/wall/closed gate and stops making progress.
 */
final class NpcFenceAvoidanceBrain {
    private static final int STALL_TICKS_BEFORE_RECOVERY = 10;
    private static final int RECOVERY_TICKS = 30;
    private static final int BLOCKED_PATH_COOLDOWN_TICKS = 80;
    private static final int RECOVERY_REPATH_INTERVAL = 5;
    private static final int[] ESCAPE_HEIGHT_OFFSETS = {0, 1, -1, 2, -2};

    private static final double MOVEMENT_EPSILON_SQR = 0.0009D;
    private static final double FENCE_DETECT_INFLATE = 0.42D;
    private static final double RECOVERY_REACHED_SQR = 1.5D * 1.5D;

    private final CyberNpcEntity npc;

    private int stallTicks;
    private int recoveryTicks;
    private int blockedPathCooldown;
    private int recoveryRepathCooldown;

    @Nullable
    private BlockPos recoveryTarget;

    private boolean hasMovementSample;
    private double lastX;
    private double lastZ;

    NpcFenceAvoidanceBrain(CyberNpcEntity npc) {
        this.npc = npc;
    }

    /**
     * @return true while fence recovery owns movement for this tick.
     */
    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || !npc.isAlive()
                || npc.isPassenger()
                || npc.isSleeping()) {
            resetTransientState();
            samplePosition();
            return false;
        }

        if (recoveryTarget != null && recoveryTicks > 0) {
            return tickRecovery(level);
        }

        PathNavigation navigation = npc.getNavigation();
        Path path = navigation.getPath();

        if (blockedPathCooldown > 0) {
            blockedPathCooldown--;

            if (isFenceBlockedPartialPath(level, path)) {
                navigation.stop();
                npc.setSprinting(false);
                samplePosition();
                return true;
            }
        }

        boolean navigating = path != null
                && !path.isDone()
                && !navigation.isDone();

        double movedSqr = horizontalMovementSinceLastSample();
        samplePosition();

        // Most NPCs are stationary or already following a reachable path.
        // Do not scan the blocks around every one of them every tick.
        if (!navigating) {
            stallTicks = Math.max(0, stallTicks - 2);
            return false;
        }
        if (path.canReach()
                && !npc.horizontalCollision
                && movedSqr >= MOVEMENT_EPSILON_SQR
                && stallTicks == 0) {
            return false;
        }

        BlockPos nearbyFence = findNearestBlockingFence(level);
        if (nearbyFence == null) {
            stallTicks = Math.max(0, stallTicks - 2);
            return false;
        }

        boolean incompleteAtFence =
                isFenceBlockedPartialPath(level, path);

        if (npc.horizontalCollision
                || movedSqr < MOVEMENT_EPSILON_SQR
                || incompleteAtFence) {
            stallTicks++;
        } else {
            stallTicks = Math.max(0, stallTicks - 1);
        }

        if (stallTicks < STALL_TICKS_BEFORE_RECOVERY) {
            return false;
        }

        stallTicks = 0;
        BlockPos escape = findReachableEscape(level, nearbyFence);

        navigation.stop();
        npc.setSprinting(false);
        blockedPathCooldown = BLOCKED_PATH_COOLDOWN_TICKS;

        if (escape == null) {
            // Even without a good local escape, stopping the partial path
            // prevents the entity from endlessly pushing into the fence.
            return true;
        }

        recoveryTarget = escape;
        recoveryTicks = RECOVERY_TICKS;
        recoveryRepathCooldown = 0;
        return tickRecovery(level);
    }

    boolean isDestinationFenceSafe(BlockPos pos) {
        if (!(npc.level() instanceof ServerLevel level) || pos == null) {
            return true;
        }

        return !isBlockingFence(level.getBlockState(pos));
    }

    private boolean tickRecovery(ServerLevel level) {
        if (recoveryTarget == null || recoveryTicks <= 0) {
            finishRecovery();
            return false;
        }

        recoveryTicks--;
        npc.setSprinting(false);

        Vec3 targetCenter = Vec3.atBottomCenterOf(recoveryTarget);
        if (npc.distanceToSqr(targetCenter) <= RECOVERY_REACHED_SQR) {
            npc.getNavigation().stop();
            finishRecovery();
            return false;
        }

        if (recoveryRepathCooldown > 0) {
            recoveryRepathCooldown--;
            return true;
        }

        recoveryRepathCooldown = RECOVERY_REPATH_INTERVAL;

        Path path = npc.getNavigation().createPath(
                recoveryTarget,
                0
        );

        if (path == null || !path.canReach()) {
            npc.getNavigation().stop();
            finishRecovery();
            return false;
        }

        npc.getNavigation().moveTo(path, 0.78D);
        return true;
    }

    @Nullable
    private BlockPos findReachableEscape(
            ServerLevel level,
            BlockPos fencePos
    ) {
        BlockPos origin = npc.blockPosition();
        BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;

        // Small local rings are enough to unhook from a fence while keeping
        // this extremely cheap compared with ordinary pathfinding.
        for (int radius = 2; radius <= 5; radius++) {
            for (int step = 0; step < 16; step++) {
                double angle = step * (Math.PI * 2.0D / 16.0D);
                int x = (int) Math.floor(
                        npc.getX() + Math.cos(angle) * radius
                );
                int z = (int) Math.floor(
                        npc.getZ() + Math.sin(angle) * radius
                );

                BlockPos candidate = resolveStandablePosition(
                        level,
                        x,
                        origin.getY(),
                        z
                );

                if (candidate == null
                        || isNearBlockingFence(level, candidate, 1)) {
                    continue;
                }

                Path path = npc.getNavigation().createPath(
                        candidate,
                        0
                );

                if (path == null || !path.canReach()) {
                    continue;
                }

                double fenceDistance =
                        candidate.distSqr(fencePos);
                double travelDistance =
                        candidate.distSqr(origin);

                // Prefer getting clear of the obstacle, but avoid picking an
                // unnecessarily distant recovery point.
                double score =
                        fenceDistance * 2.0D - travelDistance * 0.20D;

                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }

        return best;
    }

    @Nullable
    private BlockPos resolveStandablePosition(
            ServerLevel level,
            int x,
            int originY,
            int z
    ) {
        for (int yOffset : ESCAPE_HEIGHT_OFFSETS) {
            BlockPos feet = new BlockPos(
                    x,
                    originY + yOffset,
                    z
            );

            if (isStandable(level, feet)) {
                return feet.immutable();
            }
        }

        return null;
    }

    private boolean isStandable(
            ServerLevel level,
            BlockPos feet
    ) {
        BlockPos head = feet.above();
        BlockPos support = feet.below();

        if (!level.hasChunkAt(feet)
                || !level.getFluidState(feet).isEmpty()
                || !level.getFluidState(head).isEmpty()
                || !level.getBlockState(feet)
                .getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(head)
                .getCollisionShape(level, head).isEmpty()
                || level.getBlockState(support)
                .getCollisionShape(level, support).isEmpty()) {
            return false;
        }

        return !isBlockingFence(level.getBlockState(feet))
                && !isBlockingFence(level.getBlockState(support));
    }

    private boolean isFenceBlockedPartialPath(
            ServerLevel level,
            @Nullable Path path
    ) {
        if (path == null
                || path.isDone()
                || path.canReach()
                || path.getEndNode() == null) {
            return false;
        }

        BlockPos end = new BlockPos(
                path.getEndNode().x,
                path.getEndNode().y,
                path.getEndNode().z
        );

        return isNearBlockingFence(level, end, 1);
    }

    @Nullable
    private BlockPos findNearestBlockingFence(ServerLevel level) {
        AABB box = npc.getBoundingBox().inflate(
                FENCE_DETECT_INFLATE,
                0.25D,
                FENCE_DETECT_INFLATE
        );

        BlockPos min = BlockPos.containing(
                box.minX,
                box.minY,
                box.minZ
        );
        BlockPos max = BlockPos.containing(
                box.maxX,
                box.maxY,
                box.maxZ
        );

        BlockPos origin = npc.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            if (!isBlockingFence(level.getBlockState(cursor))) {
                continue;
            }

            double distance = cursor.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = cursor.immutable();
            }
        }

        return best;
    }

    private boolean isNearBlockingFence(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius
    ) {
        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                for (int y = -1; y <= 1; y++) {
                    if (isBlockingFence(
                            level.getBlockState(
                                    center.offset(x, y, z)
                            )
                    )) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isBlockingFence(BlockState state) {
        if (state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof TrapDoorBlock) {
            return true;
        }

        return state.getBlock() instanceof FenceGateBlock
                && state.hasProperty(FenceGateBlock.OPEN)
                && !state.getValue(FenceGateBlock.OPEN);
    }

    private double horizontalMovementSinceLastSample() {
        if (!hasMovementSample) {
            return Double.MAX_VALUE;
        }

        double dx = npc.getX() - lastX;
        double dz = npc.getZ() - lastZ;
        return dx * dx + dz * dz;
    }

    private void samplePosition() {
        lastX = npc.getX();
        lastZ = npc.getZ();
        hasMovementSample = true;
    }

    private void finishRecovery() {
        recoveryTarget = null;
        recoveryTicks = 0;
        recoveryRepathCooldown = 0;
        stallTicks = 0;
    }

    private void resetTransientState() {
        stallTicks = 0;
        recoveryTarget = null;
        recoveryTicks = 0;
        recoveryRepathCooldown = 0;
    }
}
