package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.building.BuildingPointType;
import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

final class NpcBuildingBrain {
    private static final double BED_USE_DISTANCE_SQR = 4.0D;
    private static final double HOME_ARRIVE_SQR = 1.5D;
    private static final int REPATH_INTERVAL = 20;
    private static final int HOME_CLAIM_INTERVAL = 200;
    private static final double HOME_CLAIM_RADIUS_SQR = 64.0D * 64.0D;

    private final CyberNpcEntity npc;

    @Nullable
    private BlockPos homeTarget;
    @Nullable
    private BlockPos activeBed;
    private int repathCooldown;
    private int homeClaimCooldown;
    private boolean busy;

    NpcBuildingBrain(CyberNpcEntity npc) {
        this.npc = npc;
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || npc.getNpcType() != NpcType.MAIN
                || NpcServiceRole.fromRole(npc.getRole())
                != NpcServiceRole.NONE) {
            wakeIfNeeded(levelOrNull());
            busy = false;
            return false;
        }

        BuildingSavedData buildings = BuildingSavedData.get(level);
        BuildingSavedData.BuildingRecord home =
                buildings.findResidentBuilding(level, npc);

        if (home == null) {
            if (homeClaimCooldown > 0) {
                homeClaimCooldown--;
            } else {
                homeClaimCooldown = HOME_CLAIM_INTERVAL;
                home = buildings.findOrClaimNearestHome(
                        level,
                        npc,
                        HOME_CLAIM_RADIUS_SQR
                );
            }
        }

        if (npc.isSleeping()) {
            if (level.isNight() && home != null) {
                npc.getNavigation().stop();
                npc.setSprinting(false);
                busy = true;
                return true;
            }

            wakeIfNeeded(level);
        }

        if (!level.isNight() || home == null) {
            homeTarget = null;
            busy = false;
            return false;
        }

        busy = true;
        npc.setSprinting(false);

        BuildingSavedData.ActivityPoint bedPoint =
                buildings.nearestPoint(
                        home,
                        BuildingPointType.BED,
                        npc.blockPosition()
                );

        BlockPos bedHead = bedPoint == null
                ? null
                : normalizeBedHead(level, bedPoint.pos());

        if (bedHead != null && !isBedOccupied(level, bedHead)) {
            Vec3 bedCenter = Vec3.atBottomCenterOf(bedHead);

            if (npc.distanceToSqr(bedCenter)
                    > BED_USE_DISTANCE_SQR) {
                navigateTo(bedHead, 0.72D);
                return true;
            }

            npc.getNavigation().stop();
            setBedOccupied(level, bedHead, true);
            activeBed = bedHead;
            npc.startSleeping(bedHead);
            homeTarget = null;
            return true;
        }

        if (homeTarget == null
                || !home.contains(homeTarget)
                || npc.blockPosition().distSqr(homeTarget)
                <= HOME_ARRIVE_SQR) {
            homeTarget = buildings.randomInteriorTarget(
                    level,
                    home,
                    npc.getRandom()
            );
        }

        if (homeTarget == null) {
            npc.getNavigation().stop();
            return true;
        }

        if (npc.blockPosition().distSqr(homeTarget)
                <= HOME_ARRIVE_SQR) {
            npc.getNavigation().stop();
            return true;
        }

        navigateTo(homeTarget, 0.70D);
        return true;
    }

    boolean isBusy() {
        return busy || npc.isSleeping();
    }

    void release() {
        wakeIfNeeded(levelOrNull());
        homeTarget = null;
        activeBed = null;
        repathCooldown = 0;
        homeClaimCooldown = 0;
        busy = false;
    }

    private void navigateTo(BlockPos pos, double speed) {
        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }

        repathCooldown = REPATH_INTERVAL;
        npc.getNavigation().moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                speed
        );
    }

    private void wakeIfNeeded(@Nullable ServerLevel level) {
        if (npc.isSleeping()) {
            npc.stopSleeping();
        }

        if (level != null && activeBed != null) {
            setBedOccupied(level, activeBed, false);
        }

        activeBed = null;
    }

    @Nullable
    private ServerLevel levelOrNull() {
        return npc.level() instanceof ServerLevel level
                ? level
                : null;
    }

    private static boolean isBedOccupied(
            ServerLevel level,
            BlockPos head
    ) {
        BlockState state = level.getBlockState(head);
        return state.getBlock() instanceof BedBlock
                && state.hasProperty(BedBlock.OCCUPIED)
                && state.getValue(BedBlock.OCCUPIED);
    }

    private static void setBedOccupied(
            ServerLevel level,
            BlockPos head,
            boolean occupied
    ) {
        BlockPos normalized = normalizeBedHead(level, head);
        if (normalized == null) {
            return;
        }

        BlockState headState = level.getBlockState(normalized);
        if (!(headState.getBlock() instanceof BedBlock)
                || !headState.hasProperty(BedBlock.OCCUPIED)
                || !headState.hasProperty(BedBlock.FACING)) {
            return;
        }

        if (headState.getValue(BedBlock.OCCUPIED) != occupied) {
            level.setBlock(
                    normalized,
                    headState.setValue(
                            BedBlock.OCCUPIED,
                            occupied
                    ),
                    Block.UPDATE_CLIENTS
            );
        }

        Direction facing = headState.getValue(BedBlock.FACING);
        BlockPos foot =
                normalized.relative(facing.getOpposite());
        BlockState footState = level.getBlockState(foot);

        if (footState.getBlock() instanceof BedBlock
                && footState.hasProperty(BedBlock.OCCUPIED)
                && footState.getValue(BedBlock.OCCUPIED)
                != occupied) {
            level.setBlock(
                    foot,
                    footState.setValue(
                            BedBlock.OCCUPIED,
                            occupied
                    ),
                    Block.UPDATE_CLIENTS
            );
        }
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

        Direction facing = state.getValue(BedBlock.FACING);
        BlockPos head = pos.relative(facing);
        BlockState headState = level.getBlockState(head);

        if (headState.getBlock() instanceof BedBlock
                && headState.hasProperty(BedBlock.PART)
                && headState.getValue(BedBlock.PART)
                == BedPart.HEAD) {
            return head.immutable();
        }

        return null;
    }
}
