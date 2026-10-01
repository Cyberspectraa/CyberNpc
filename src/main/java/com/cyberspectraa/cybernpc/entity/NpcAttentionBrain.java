package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;

/**
 * Low-priority attention and curiosity. NPCs notice things players commonly
 * stop to look at, but the selected subject becomes a short-term intention
 * instead of being re-selected every tick.
 */
final class NpcAttentionBrain {
    private static final int SCAN_INTERVAL_MIN = 30;
    private static final int SCAN_INTERVAL_RANDOM = 50;
    private static final double PLAYER_NOTICE_RADIUS = 14.0D;
    private static final double FIGHT_NOTICE_RADIUS = 18.0D;
    private static final double INTEREST_NOTICE_RADIUS = 12.0D;
    private static final int BLOCK_SEARCH_RADIUS = 12;
    private static final int BLOCK_SEARCH_VERTICAL = 4;

    private final CyberNpcEntity npc;
    private final NpcPlayerInteractionController interactions;
    private final NpcIntentionController intentions;

    private int scanCooldown = 30;
    private boolean workstationUsed;

    NpcAttentionBrain(
            CyberNpcEntity npc,
            NpcPlayerInteractionController interactions,
            NpcIntentionController intentions
    ) {
        this.npc = npc;
        this.interactions = interactions;
        this.intentions = intentions;
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || !npc.canStartPlayerLikeLifeActivity()) {
            workstationUsed = false;
            return false;
        }

        if (isAttentionIntent(intentions.kind())) {
            return tickCurrent(level);
        }

        if (intentions.isActive()) {
            return false;
        }

        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }

        scanCooldown = SCAN_INTERVAL_MIN
                + npc.getRandom().nextInt(SCAN_INTERVAL_RANDOM + 1);

        if (tryObserveFight(level)) {
            return tickCurrent(level);
        }

        if (tryNoticePlayer(level)) {
            return tickCurrent(level);
        }

        if (trySunset(level)) {
            return tickCurrent(level);
        }

        if (tryInspectEntity(level)) {
            return tickCurrent(level);
        }

        if (tryWorkstation(level)) {
            return tickCurrent(level);
        }

        if (tryCampfire(level)) {
            return tickCurrent(level);
        }

        return false;
    }

    boolean ownsCurrentIntention() {
        return isAttentionIntent(intentions.kind());
    }

    String getActivity() {
        return switch (intentions.kind()) {
            case WATCH_SUNSET -> "Watching the sunset";
            case WATCH_PLAYER -> "Watching a nearby player";
            case OBSERVE_FIGHT -> "Watching a nearby fight";
            case INSPECT_ENTITY -> "Inspecting something interesting";
            case USE_WORKSTATION -> workstationUsed
                    ? "Looking over a workstation"
                    : "Going to use a workstation";
            case WATCH_CAMPFIRE -> "Watching a campfire";
            default -> "Idle";
        };
    }

    String getReason() {
        return intentions.reason();
    }

    void interrupt() {
        if (ownsCurrentIntention()) {
            intentions.clear();
        }
        workstationUsed = false;
    }

    private boolean tickCurrent(ServerLevel level) {
        return switch (intentions.kind()) {
            case WATCH_SUNSET -> tickSunset(level);
            case WATCH_PLAYER -> tickWatchEntity(level, true, 30.0F, 24.0F);
            case OBSERVE_FIGHT -> tickWatchEntity(level, false, 30.0F, 24.0F);
            case INSPECT_ENTITY -> tickInspectEntity(level);
            case USE_WORKSTATION -> tickWorkstation(level);
            case WATCH_CAMPFIRE -> tickCampfire(level);
            default -> false;
        };
    }

    private boolean tryObserveFight(ServerLevel level) {
        CyberNpcEntity fightingNpc = level.getEntitiesOfClass(
                        CyberNpcEntity.class,
                        npc.getBoundingBox().inflate(FIGHT_NOTICE_RADIUS),
                        other -> other != npc
                                && other.isAlive()
                                && other.isCombatActive()
                ).stream()
                .filter(other -> npc.getSensing().hasLineOfSight(other))
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);

        if (fightingNpc == null
                || npc.getRandom().nextFloat() > fightInterest()) {
            return false;
        }

        return intentions.request(
                NpcIntentionController.Intent.OBSERVE_FIGHT,
                35,
                80 + npc.getRandom().nextInt(101),
                fightingNpc.getUUID(),
                null,
                "A nearby fight caught this NPC's attention, so it is watching from a safe distance"
        );
    }

    private boolean tryNoticePlayer(ServerLevel level) {
        Player player = level.getEntitiesOfClass(
                        Player.class,
                        npc.getBoundingBox().inflate(PLAYER_NOTICE_RADIUS),
                        candidate -> candidate.isAlive()
                                && !candidate.isCreative()
                                && !candidate.isSpectator()
                                && (candidate.isSprinting()
                                || candidate.isShiftKeyDown()
                                || candidate.isUsingItem())
                ).stream()
                .filter(candidate -> npc.getSensing().hasLineOfSight(candidate))
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);

        if (player == null
                || npc.getRandom().nextFloat() > playerInterest()) {
            return false;
        }

        String action = player.isUsingItem()
                ? "using an item"
                : player.isSprinting()
                ? "moving quickly"
                : "sneaking";

        return intentions.request(
                NpcIntentionController.Intent.WATCH_PLAYER,
                25,
                60 + npc.getRandom().nextInt(101),
                player.getUUID(),
                null,
                "A nearby player is " + action + ", which was interesting enough to watch for a moment"
        );
    }

    private boolean trySunset(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        if (time < 11600L
                || time > 13200L
                || level.isRaining()
                || !level.canSeeSky(npc.blockPosition().above())
                || npc.getRandom().nextFloat() > sunsetInterest()) {
            return false;
        }

        return intentions.request(
                NpcIntentionController.Intent.WATCH_SUNSET,
                18,
                100 + npc.getRandom().nextInt(181),
                null,
                null,
                "The evening sky is visible and no stronger need is active, so this NPC chose to watch the sunset"
        );
    }

    private boolean tryInspectEntity(ServerLevel level) {
        LivingEntity target = level.getEntitiesOfClass(
                        LivingEntity.class,
                        npc.getBoundingBox().inflate(INTEREST_NOTICE_RADIUS),
                        candidate -> candidate != npc
                                && candidate.isAlive()
                                && isInterestingEntity(candidate)
                ).stream()
                .filter(candidate -> npc.getSensing().hasLineOfSight(candidate))
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);

        if (target == null
                || npc.getRandom().nextFloat() > curiosityInterest()) {
            return false;
        }

        return intentions.request(
                NpcIntentionController.Intent.INSPECT_ENTITY,
                16,
                60 + npc.getRandom().nextInt(101),
                target.getUUID(),
                null,
                "A nearby creature or villager looked interesting enough to inspect briefly"
        );
    }

    private boolean tryWorkstation(ServerLevel level) {
        if (npc.getRandom().nextFloat() > workstationInterest()) {
            return false;
        }

        BlockPos pos = findBlock(level, true);
        if (pos == null) {
            return false;
        }

        workstationUsed = false;
        return intentions.request(
                NpcIntentionController.Intent.USE_WORKSTATION,
                12,
                100 + npc.getRandom().nextInt(101),
                null,
                pos,
                "A nearby workstation caught this NPC's interest and there is time to look it over"
        );
    }

    private boolean tryCampfire(ServerLevel level) {
        if (npc.getRandom().nextFloat() > 0.25F) {
            return false;
        }

        BlockPos pos = findBlock(level, false);
        if (pos == null) {
            return false;
        }

        return intentions.request(
                NpcIntentionController.Intent.WATCH_CAMPFIRE,
                10,
                100 + npc.getRandom().nextInt(141),
                null,
                pos,
                "The nearby fire is a comfortable place to stop while nothing urgent needs attention"
        );
    }

    private boolean tickSunset(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        if (time < 11600L
                || time > 13200L
                || level.isRaining()
                || !level.canSeeSky(npc.blockPosition().above())) {
            intentions.complete();
            return false;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);
        npc.setShiftKeyDown(false);
        npc.getLookControl().setLookAt(
                npc.getX() - 48.0D,
                npc.getEyeY() + 5.0D,
                npc.getZ(),
                12.0F,
                8.0F
        );
        return true;
    }

    private boolean tickWatchEntity(
            ServerLevel level,
            boolean allowApproach,
            float yaw,
            float pitch
    ) {
        Entity target = getTargetEntity(level);
        if (!(target instanceof LivingEntity living)
                || !living.isAlive()
                || npc.distanceToSqr(living) > 24.0D * 24.0D) {
            intentions.complete();
            return false;
        }

        double distance = npc.distanceToSqr(living);
        if (allowApproach
                && distance > 49.0D
                && distance < 196.0D) {
            npc.getNavigation().moveTo(living, 0.55D);
        } else {
            npc.getNavigation().stop();
        }

        npc.setSprinting(false);
        npc.getLookControl().setLookAt(living, yaw, pitch);
        return true;
    }

    private boolean tickInspectEntity(ServerLevel level) {
        Entity target = getTargetEntity(level);
        if (!(target instanceof LivingEntity living)
                || !living.isAlive()
                || npc.distanceToSqr(living) > 18.0D * 18.0D) {
            intentions.complete();
            return false;
        }

        double distance = npc.distanceToSqr(living);
        if (distance > 25.0D) {
            npc.getNavigation().moveTo(living, 0.52D);
        } else {
            npc.getNavigation().stop();
        }

        npc.setSprinting(false);
        npc.getLookControl().setLookAt(living, 24.0F, 20.0F);
        return true;
    }

    private boolean tickWorkstation(ServerLevel level) {
        BlockPos pos = intentions.targetPos();
        if (pos == null || !isWorkstation(level, pos)) {
            workstationUsed = false;
            intentions.complete();
            return false;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        npc.getLookControl().setLookAt(center.x, center.y, center.z, 20.0F, 20.0F);

        if (npc.distanceToSqr(center) > 9.0D) {
            npc.getNavigation().moveTo(center.x, center.y, center.z, 0.72D);
            npc.setSprinting(false);
            return true;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);

        if (!workstationUsed) {
            InteractionResult result = interactions.rightClickBlock(pos);
            workstationUsed = result.consumesAction();
            if (!workstationUsed) {
                intentions.complete();
                return false;
            }
        }

        return true;
    }

    private boolean tickCampfire(ServerLevel level) {
        BlockPos pos = intentions.targetPos();
        if (pos == null || !isCampfire(level, pos)) {
            intentions.complete();
            return false;
        }

        Vec3 center = Vec3.atCenterOf(pos);
        npc.getLookControl().setLookAt(
                center.x,
                center.y + 0.25D,
                center.z,
                16.0F,
                12.0F
        );

        if (npc.distanceToSqr(center) > 25.0D) {
            npc.getNavigation().moveTo(center.x, center.y, center.z, 0.66D);
        } else {
            npc.getNavigation().stop();
        }

        npc.setSprinting(false);
        return true;
    }

    @Nullable
    private Entity getTargetEntity(ServerLevel level) {
        UUID id = intentions.targetEntityId();
        return id == null ? null : level.getEntity(id);
    }

    @Nullable
    private BlockPos findBlock(ServerLevel level, boolean workstation) {
        BlockPos origin = npc.blockPosition();

        return BlockPos.betweenClosedStream(
                        origin.offset(-BLOCK_SEARCH_RADIUS, -BLOCK_SEARCH_VERTICAL, -BLOCK_SEARCH_RADIUS),
                        origin.offset(BLOCK_SEARCH_RADIUS, BLOCK_SEARCH_VERTICAL, BLOCK_SEARCH_RADIUS)
                )
                .filter(pos -> workstation
                        ? isWorkstation(level, pos)
                        : isCampfire(level, pos))
                .map(BlockPos::immutable)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                .filter(pos -> {
                    Path path = npc.getNavigation().createPath(pos, 1);
                    return path != null
                            && (path.canReach() || path.getEndNode() != null);
                })
                .findFirst()
                .orElse(null);
    }

    private static boolean isWorkstation(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.SMITHING_TABLE)
                || state.is(Blocks.CARTOGRAPHY_TABLE)
                || state.is(Blocks.FLETCHING_TABLE)
                || state.is(Blocks.LOOM)
                || state.is(Blocks.STONECUTTER)
                || state.is(Blocks.GRINDSTONE);
    }

    private static boolean isCampfire(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private static boolean isInterestingEntity(LivingEntity entity) {
        return entity instanceof Villager
                || entity instanceof IronGolem
                || entity instanceof AbstractHorse
                || (entity instanceof AgeableMob ageable && ageable.isBaby());
    }

    private static boolean isAttentionIntent(NpcIntentionController.Intent intent) {
        return switch (intent) {
            case WATCH_SUNSET,
                 WATCH_PLAYER,
                 OBSERVE_FIGHT,
                 INSPECT_ENTITY,
                 USE_WORKSTATION,
                 WATCH_CAMPFIRE -> true;
            default -> false;
        };
    }

    private float fightInterest() {
        return switch (npc.getPersonality()) {
            case BRAVE, AGGRESSIVE, RECKLESS -> 0.82F;
            case TACTICAL, PROTECTIVE -> 0.72F;
            case BALANCED, LOYAL, OPPORTUNISTIC -> 0.55F;
            case STUBBORN, PATIENT -> 0.42F;
            case CAUTIOUS -> 0.30F;
            case SKITTISH -> 0.16F;
        };
    }

    private float playerInterest() {
        return switch (npc.getPersonality()) {
            case OPPORTUNISTIC, TACTICAL -> 0.68F;
            case BALANCED, PATIENT, LOYAL -> 0.52F;
            case BRAVE, PROTECTIVE, CAUTIOUS -> 0.44F;
            case STUBBORN -> 0.34F;
            case AGGRESSIVE, SKITTISH -> 0.28F;
            case RECKLESS -> 0.22F;
        };
    }

    private float sunsetInterest() {
        return switch (npc.getPersonality()) {
            case PATIENT -> 0.82F;
            case CAUTIOUS, BALANCED, LOYAL -> 0.60F;
            case PROTECTIVE, TACTICAL -> 0.48F;
            case BRAVE, OPPORTUNISTIC, STUBBORN -> 0.38F;
            case SKITTISH -> 0.32F;
            case AGGRESSIVE -> 0.22F;
            case RECKLESS -> 0.16F;
        };
    }

    private float curiosityInterest() {
        return switch (npc.getPersonality()) {
            case OPPORTUNISTIC -> 0.62F;
            case BALANCED, PATIENT, TACTICAL -> 0.46F;
            case BRAVE, LOYAL -> 0.38F;
            case PROTECTIVE, CAUTIOUS -> 0.32F;
            case STUBBORN -> 0.24F;
            case SKITTISH, AGGRESSIVE -> 0.18F;
            case RECKLESS -> 0.14F;
        };
    }

    private float workstationInterest() {
        return switch (npc.getPersonality()) {
            case TACTICAL, PATIENT -> 0.50F;
            case BALANCED, OPPORTUNISTIC -> 0.40F;
            case CAUTIOUS, PROTECTIVE, LOYAL -> 0.30F;
            case BRAVE, STUBBORN -> 0.24F;
            case AGGRESSIVE, SKITTISH -> 0.18F;
            case RECKLESS -> 0.12F;
        };
    }
}
