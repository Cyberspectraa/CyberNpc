package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

final class WildNpcSleepBrain {
    private static final int BED_SEARCH_RADIUS = 24;
    private static final int BED_SEARCH_VERTICAL = 6;
    private static final int BED_SEARCH_COOLDOWN = 100;
    private static final double BED_USE_DISTANCE_SQR = 5.0D;

    private final CyberNpcEntity npc;

    @Nullable
    private BlockPos claimedBed;

    private int searchCooldown;
    private boolean busy;

    WildNpcSleepBrain(CyberNpcEntity npc) {
        this.npc = npc;
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)) {
            busy = false;
            return false;
        }

        if (level.isNight()
                && npc.shouldExplorerReturnBeforeSleeping()) {
            busy = false;
            return false;
        }

        if (npc.isSleeping()) {
            if (npc.isCombatActive()
                    || !level.isNight()
                    || npc.getHunger() <= CyberNpcEntity.HUNT_HUNGER_THRESHOLD
                    || !isValidClaimedBed(level)) {
                wakeUp();
                busy = false;
                return false;
            }

            npc.getNavigation().stop();
            npc.setSprinting(false);
            npc.setShiftKeyDown(false);
            busy = true;
            return true;
        }

        if (!level.isNight()
                || npc.isCombatActive()
                || npc.getHunger() <= CyberNpcEntity.HUNT_HUNGER_THRESHOLD) {
            busy = false;
            return false;
        }

        if (!isValidClaimedBed(level) || isClaimedByAnotherNpc(level, claimedBed)) {
            claimedBed = null;

            if (searchCooldown > 0) {
                searchCooldown--;
                busy = false;
                return false;
            }

            searchCooldown = BED_SEARCH_COOLDOWN;
            claimedBed = findClaimableBed(level);

            if (claimedBed == null) {
                busy = false;
                return false;
            }

            npc.showReaction(NpcReactionIcon.SLEEP, 50);
        }

        if (isBedOccupied(level, claimedBed)) {
            busy = false;
            return false;
        }

        busy = true;

        npc.prepareForSleep();

        Vec3 bedCenter = Vec3.atBottomCenterOf(claimedBed);
        if (npc.distanceToSqr(bedCenter) > BED_USE_DISTANCE_SQR) {
            npc.getNavigation().moveTo(bedCenter.x, bedCenter.y, bedCenter.z, 0.95D);
            return true;
        }

        npc.getNavigation().stop();
        setBedOccupied(level, claimedBed, true);
        npc.startSleeping(claimedBed);
        return true;
    }

    boolean isBusy() {
        return busy || npc.isSleeping();
    }

    void wakeUp() {
        busy = false;
        boolean wasSleeping = npc.isSleeping();

        if (!(npc.level() instanceof ServerLevel level)) {
            if (wasSleeping) {
                npc.stopSleeping();
            }
            return;
        }

        if (wasSleeping) {
            npc.stopSleeping();

            if (claimedBed != null) {
                setBedOccupied(level, claimedBed, false);
            }
        }

        npc.getNavigation().stop();
    }

    void interrupt() {
        wakeUp();
    }

    @Nullable
    BlockPos getClaimedBed() {
        return claimedBed;
    }

    void addSaveData(CompoundTag tag) {
        if (claimedBed != null) {
            tag.putLong("CyberNpcClaimedBed", claimedBed.asLong());
        }
    }

    void readSaveData(CompoundTag tag) {
        claimedBed = tag.contains("CyberNpcClaimedBed")
                ? BlockPos.of(tag.getLong("CyberNpcClaimedBed"))
                : null;

        searchCooldown = 0;
        busy = false;

        // Sleeping itself is intentionally not restored directly. After loading,
        // the NPC walks back to the claimed bed if it is still night.
        if (npc.isSleeping()) {
            wakeUp();
        }
    }

    private boolean isValidClaimedBed(ServerLevel level) {
        if (claimedBed == null) {
            return false;
        }

        BlockPos normalized = normalizeBedHead(level, claimedBed);
        if (normalized == null) {
            claimedBed = null;
            return false;
        }

        claimedBed = normalized;
        return true;
    }

    @Nullable
    private BlockPos findClaimableBed(ServerLevel level) {
        BlockPos origin = npc.blockPosition();

        return BlockPos.betweenClosedStream(
                        origin.offset(-BED_SEARCH_RADIUS, -BED_SEARCH_VERTICAL, -BED_SEARCH_RADIUS),
                        origin.offset(BED_SEARCH_RADIUS, BED_SEARCH_VERTICAL, BED_SEARCH_RADIUS)
                )
                .filter(pos -> level.getBlockState(pos).getBlock() instanceof BedBlock)
                .map(BlockPos::immutable)
                .map(pos -> normalizeBedHead(level, pos))
                .filter(pos -> pos != null)
                .distinct()
                .filter(pos -> !isBedOccupied(level, pos))
                .filter(pos -> !isClaimedByAnotherNpc(level, pos))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                .orElse(null);
    }

    private boolean isClaimedByAnotherNpc(ServerLevel level, @Nullable BlockPos bedHead) {
        if (bedHead == null) {
            return false;
        }

        AABB searchBox = new AABB(bedHead).inflate(BED_SEARCH_RADIUS * 2.0D, 16.0D, BED_SEARCH_RADIUS * 2.0D);
        List<CyberNpcEntity> nearbyNpcs = level.getEntitiesOfClass(
                CyberNpcEntity.class,
                searchBox,
                other -> other != npc
                        && other.isAlive()
                        && other.getNpcType() == NpcType.WILD
        );

        for (CyberNpcEntity other : nearbyNpcs) {
            BlockPos otherClaim = other.getClaimedBedPos();
            if (otherClaim == null) {
                continue;
            }

            BlockPos normalized = normalizeBedHead(level, otherClaim);
            if (bedHead.equals(normalized)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBedOccupied(ServerLevel level, BlockPos head) {
        BlockState state = level.getBlockState(head);
        return state.getBlock() instanceof BedBlock
                && state.hasProperty(BedBlock.OCCUPIED)
                && state.getValue(BedBlock.OCCUPIED);
    }

    private void setBedOccupied(ServerLevel level, BlockPos head, boolean occupied) {
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
                    headState.setValue(BedBlock.OCCUPIED, occupied),
                    Block.UPDATE_CLIENTS
            );
        }

        Direction facing = headState.getValue(BedBlock.FACING);
        BlockPos foot = normalized.relative(facing.getOpposite());
        BlockState footState = level.getBlockState(foot);

        if (footState.getBlock() instanceof BedBlock
                && footState.hasProperty(BedBlock.OCCUPIED)
                && footState.getValue(BedBlock.OCCUPIED) != occupied) {
            level.setBlock(
                    foot,
                    footState.setValue(BedBlock.OCCUPIED, occupied),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Nullable
    private static BlockPos normalizeBedHead(ServerLevel level, BlockPos pos) {
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
                && headState.getValue(BedBlock.PART) == BedPart.HEAD) {
            return head.immutable();
        }

        return null;
    }
}
