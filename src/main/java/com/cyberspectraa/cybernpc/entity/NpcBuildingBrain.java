package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.building.BuildingPointType;
import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.cyberspectraa.cybernpc.town.TownHomeSavedData;
import com.cyberspectraa.cybernpc.town.TownWorkSavedData;
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
    private static final double WORK_SNAP_SQR = 0.75D * 0.75D;
    private static final int REPATH_INTERVAL = 20;

    private final CyberNpcEntity npc;

    @Nullable
    private BlockPos homeTarget;
    @Nullable
    private BlockPos activeBed;
    private int repathCooldown;
    @Nullable
    private TownWorkSavedData.JobAssignment cachedTownJob;
    private long lastJobLookupTick = -1L;
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
        BuildingSavedData.WorkAssignment work =
                buildings.findAssignedWork(level, npc);
        // New opt-in town homes take precedence. Existing marker homes remain
        // untouched and continue to function for pre-migration residents.
        TownHomeSavedData.HomeAssignment townHome =
                TownHomeSavedData.get(level).assignedHome(level, npc.getUUID());
        // Job data is persistent and dimension aware, but only query it every
        // 40 ticks. NPC movement must not scan saved jobs every game tick.
        if (lastJobLookupTick < 0 || npc.tickCount - lastJobLookupTick >= 40) {
            cachedTownJob = TownWorkSavedData.get(level).assignedJob(level, npc.getUUID());
            lastJobLookupTick = npc.tickCount;
        }

        if (npc.isSleeping()) {
            if (level.isNight() && (home != null || townHome != null)) {
                npc.getNavigation().stop();
                npc.setSprinting(false);
                busy = true;
                return true;
            }

            wakeIfNeeded(level);
        }

        if (!level.isNight()) {
            homeTarget = null;

            // Existing Work marker assignments take absolute precedence.
            if (work != null) {
                busy = true;
                return tickWork(work);
            }

            if (cachedTownJob != null && shouldWorkNow(level)) {
                busy = true;
                return tickTownWork(cachedTownJob);
            }

            busy = false;
            return false;
        }

        if (townHome != null) {
            busy = true;
            npc.setSprinting(false);
            BlockPos assigned = townHome.bed();
            if (assigned != null
                    && level.getChunkSource().getChunkNow(
                            assigned.getX() >> 4, assigned.getZ() >> 4) != null) {
                BlockPos bed = normalizeBedHead(level, assigned);
                if (bed != null && !isBedOccupied(level, bed)) {
                    if (npc.distanceToSqr(Vec3.atBottomCenterOf(bed)) > BED_USE_DISTANCE_SQR) {
                        navigateTo(bed, 0.72D);
                    } else {
                        npc.getNavigation().stop();
                        setBedOccupied(level, bed, true);
                        activeBed = bed;
                        npc.startSleeping(bed);
                        homeTarget = null;
                    }
                    return true;
                }
            }
            // A removed or temporarily occupied bed does not erase housing
            // identity or cause an NPC to take somebody else's claimed bed.
            if (level.getChunkSource().getChunkNow(
                    townHome.inside().getX() >> 4, townHome.inside().getZ() >> 4) != null) {
                if (npc.blockPosition().distSqr(townHome.inside()) > HOME_ARRIVE_SQR) {
                    navigateTo(townHome.inside(), 0.70D);
                } else {
                    npc.getNavigation().stop();
                }
            }
            return true;
        }

        if (home == null) {
            homeTarget = null;
            busy = false;
            return false;
        }

        busy = true;
        npc.setSprinting(false);

        BlockPos bedHead =
                nearestAvailableBed(level, home);

        if (bedHead != null) {
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
        cachedTownJob = null;
        lastJobLookupTick = -1L;
        busy = false;
    }

    /**
     * A modest day routine for NEW station-based jobs only. Legacy marker
     * workers are untouched. Staggered shifts, lunch and short breathers
     * allow normal idle/social/stroll AI to run when off duty.
     */
    private boolean shouldWorkNow(ServerLevel level) {
        if (npc.getHunger() <= 5.0F
                || npc.getHealth() < npc.getMaxHealth() * 0.45F) return false;
        long time = Math.floorMod(level.getDayTime(), 24000L);
        long offset = Math.floorMod(npc.getUUID().hashCode(), 900) - 450;
        boolean shift = (time >= 2500L + offset && time <= 6300L + offset)
                || (time >= 7900L + offset && time <= 11100L + offset);
        // A 20-second breather during each longer shift, not permanent idle.
        return shift && (time + offset) % 2800L < 2400L;
    }

    private boolean tickTownWork(TownWorkSavedData.JobAssignment job) {
        BlockPos stand = job.stand();
        npc.setSprinting(false);
        if (npc.distanceToSqr(Vec3.atBottomCenterOf(stand)) > 2.0D * 2.0D) {
            navigateTo(stand, 0.69D);
        } else {
            npc.getNavigation().stop();
            if (npc.tickCount % 40 == 0) {
                npc.getLookControl().setLookAt(
                        job.station().getX() + 0.5D,
                        job.station().getY() + 0.7D,
                        job.station().getZ() + 0.5D);
            }
        }
        return true;
    }

    private boolean tickWork(
            BuildingSavedData.WorkAssignment assignment
    ) {
        BuildingSavedData.ActivityPoint point =
                assignment.point();
        Vec3 center = Vec3.atBottomCenterOf(point.pos());

        npc.setSprinting(false);

        if (npc.distanceToSqr(center) > WORK_SNAP_SQR) {
            navigateTo(point.pos(), 0.72D);
            return true;
        }

        npc.getNavigation().stop();
        npc.setDeltaMovement(Vec3.ZERO);
        npc.moveTo(
                center.x,
                center.y,
                center.z,
                point.yaw(),
                npc.getXRot()
        );
        npc.setYRot(point.yaw());
        npc.setYHeadRot(point.yaw());
        npc.setYBodyRot(point.yaw());
        return true;
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

    @Nullable
    private BlockPos nearestAvailableBed(
            ServerLevel level,
            BuildingSavedData.BuildingRecord home
    ) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BuildingSavedData.ActivityPoint point
                : home.points()) {
            if (point.type() != BuildingPointType.BED) {
                continue;
            }

            BlockPos head = normalizeBedHead(
                    level,
                    point.pos()
            );
            if (head == null || isBedOccupied(level, head)) {
                continue;
            }

            double distance =
                    npc.blockPosition().distSqr(head);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = head;
            }
        }

        return best;
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
