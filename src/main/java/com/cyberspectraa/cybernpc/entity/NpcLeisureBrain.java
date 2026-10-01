package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * Low-priority "life" actions that make a Wild NPC feel less like a task
 * machine. Survival, danger, sleep, combat, companions and social commitments
 * always interrupt this brain.
 */
final class NpcLeisureBrain {
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_VERTICAL = 4;
    private static final double WORKSTATION_USE_DISTANCE_SQR = 9.0D;
    private static final double CAMPFIRE_WATCH_DISTANCE_SQR = 25.0D;

    // Minecraft's sunset begins around 12000 ticks. The slightly wider window
    // lets an NPC notice it naturally rather than on one exact tick.
    private static final long SUNSET_START = 11600L;
    private static final long SUNSET_END = 13200L;

    private final CyberNpcEntity npc;
    private final NpcPlayerInteractionController interactions;

    private LeisureAction action = LeisureAction.NONE;

    @Nullable
    private BlockPos targetPos;

    private int actionTicks;
    private int cooldown = 120;
    private boolean usedTarget;

    NpcLeisureBrain(
            CyberNpcEntity npc,
            NpcPlayerInteractionController interactions
    ) {
        this.npc = npc;
        this.interactions = interactions;
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || !npc.canDoLeisureActivity()) {
            interrupt();
            return false;
        }

        if (action != LeisureAction.NONE) {
            return tickActive(level);
        }

        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        cooldown = 180 + npc.getRandom().nextInt(321);

        if (canWatchSunset(level)
                && npc.getRandom().nextFloat() < sunsetInterest()) {
            startSunset();
            return true;
        }

        // Workstations are useful objects players naturally stop at. We use
        // their real right-click hook, then immediately close the invisible
        // FakePlayer menu. Actual recipe crafting is intentionally a separate
        // future brain rather than pretending that opening a menu crafted.
        if (npc.getRandom().nextFloat() < workstationInterest()) {
            BlockPos workstation = findWorkstation(level);
            if (workstation != null) {
                action = LeisureAction.USE_WORKSTATION;
                targetPos = workstation;
                actionTicks = 100 + npc.getRandom().nextInt(81);
                usedTarget = false;
                return true;
            }
        }

        if (npc.getRandom().nextFloat() < 0.28F) {
            BlockPos campfire = findCampfire(level);
            if (campfire != null) {
                action = LeisureAction.WATCH_CAMPFIRE;
                targetPos = campfire;
                actionTicks = 100 + npc.getRandom().nextInt(141);
                usedTarget = false;
                return true;
            }
        }

        return false;
    }

    boolean isBusy() {
        return action != LeisureAction.NONE;
    }

    String getActivity() {
        return switch (action) {
            case WATCH_SUNSET -> "Watching the sunset";
            case USE_WORKSTATION -> usedTarget
                    ? "Looking over a workstation"
                    : "Going to use a workstation";
            case WATCH_CAMPFIRE -> "Watching a campfire";
            default -> "Idle";
        };
    }

    String getReason() {
        return switch (action) {
            case WATCH_SUNSET ->
                    "No urgent need is active and this NPC chose to stop and watch the evening sky";
            case USE_WORKSTATION ->
                    "No urgent need is active and a nearby workstation caught this NPC's interest";
            case WATCH_CAMPFIRE ->
                    "No urgent need is active and this NPC chose to spend a moment by the fire";
            default -> "No leisure activity is active";
        };
    }

    void interrupt() {
        if (action == LeisureAction.NONE) {
            return;
        }

        action = LeisureAction.NONE;
        targetPos = null;
        actionTicks = 0;
        usedTarget = false;
        cooldown = Math.max(cooldown, 100);
    }

    private boolean tickActive(ServerLevel level) {
        if (--actionTicks <= 0) {
            finish();
            return false;
        }

        return switch (action) {
            case WATCH_SUNSET -> tickSunset(level);
            case USE_WORKSTATION -> tickWorkstation(level);
            case WATCH_CAMPFIRE -> tickCampfire(level);
            default -> false;
        };
    }

    private boolean tickSunset(ServerLevel level) {
        if (!canWatchSunset(level)) {
            finish();
            return false;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);
        npc.setShiftKeyDown(false);

        // West is negative X in Minecraft. Keep the target high enough that the
        // head visibly tracks the horizon rather than the ground.
        npc.getLookControl().setLookAt(
                npc.getX() - 48.0D,
                npc.getEyeY() + 5.0D,
                npc.getZ(),
                12.0F,
                8.0F
        );
        return true;
    }

    private boolean tickWorkstation(ServerLevel level) {
        if (targetPos == null || !isWorkstation(level, targetPos)) {
            finish();
            return false;
        }

        Vec3 center = Vec3.atCenterOf(targetPos);
        npc.getLookControl().setLookAt(
                center.x,
                center.y,
                center.z,
                20.0F,
                20.0F
        );

        if (npc.distanceToSqr(center) > WORKSTATION_USE_DISTANCE_SQR) {
            npc.getNavigation().moveTo(
                    center.x,
                    center.y,
                    center.z,
                    0.72D
            );
            npc.setSprinting(false);
            return true;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);

        if (!usedTarget) {
            InteractionResult result = interactions.rightClickBlock(targetPos);
            usedTarget = result.consumesAction();
            if (!usedTarget) {
                finish();
                return false;
            }
        }

        return true;
    }

    private boolean tickCampfire(ServerLevel level) {
        if (targetPos == null || !isCampfire(level, targetPos)) {
            finish();
            return false;
        }

        Vec3 center = Vec3.atCenterOf(targetPos);
        npc.getLookControl().setLookAt(
                center.x,
                center.y + 0.25D,
                center.z,
                16.0F,
                12.0F
        );

        if (npc.distanceToSqr(center) > CAMPFIRE_WATCH_DISTANCE_SQR) {
            npc.getNavigation().moveTo(
                    center.x,
                    center.y,
                    center.z,
                    0.66D
            );
            npc.setSprinting(false);
            return true;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);
        return true;
    }

    private void startSunset() {
        action = LeisureAction.WATCH_SUNSET;
        targetPos = null;
        actionTicks = 100 + npc.getRandom().nextInt(181);
        usedTarget = false;
    }

    private void finish() {
        action = LeisureAction.NONE;
        targetPos = null;
        actionTicks = 0;
        usedTarget = false;
        cooldown = 220 + npc.getRandom().nextInt(381);
    }

    private boolean canWatchSunset(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return time >= SUNSET_START
                && time <= SUNSET_END
                && !level.isRaining()
                && level.canSeeSky(npc.blockPosition().above());
    }

    @Nullable
    private BlockPos findWorkstation(ServerLevel level) {
        BlockPos origin = npc.blockPosition();

        return BlockPos.betweenClosedStream(
                        origin.offset(-SEARCH_RADIUS, -SEARCH_VERTICAL, -SEARCH_RADIUS),
                        origin.offset(SEARCH_RADIUS, SEARCH_VERTICAL, SEARCH_RADIUS)
                )
                .filter(pos -> isWorkstation(level, pos))
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

    @Nullable
    private BlockPos findCampfire(ServerLevel level) {
        BlockPos origin = npc.blockPosition();

        return BlockPos.betweenClosedStream(
                        origin.offset(-SEARCH_RADIUS, -SEARCH_VERTICAL, -SEARCH_RADIUS),
                        origin.offset(SEARCH_RADIUS, SEARCH_VERTICAL, SEARCH_RADIUS)
                )
                .filter(pos -> isCampfire(level, pos))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                .orElse(null);
    }

    private static boolean isWorkstation(
            ServerLevel level,
            BlockPos pos
    ) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.SMITHING_TABLE)
                || state.is(Blocks.CARTOGRAPHY_TABLE)
                || state.is(Blocks.FLETCHING_TABLE)
                || state.is(Blocks.LOOM)
                || state.is(Blocks.STONECUTTER)
                || state.is(Blocks.GRINDSTONE);
    }

    private static boolean isCampfire(
            ServerLevel level,
            BlockPos pos
    ) {
        var state = level.getBlockState(pos);
        return state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private float sunsetInterest() {
        return switch (npc.getPersonality()) {
            case PATIENT -> 0.80F;
            case CAUTIOUS, BALANCED, LOYAL -> 0.58F;
            case PROTECTIVE, TACTICAL -> 0.48F;
            case BRAVE, OPPORTUNISTIC, STUBBORN -> 0.38F;
            case SKITTISH -> 0.32F;
            case AGGRESSIVE -> 0.24F;
            case RECKLESS -> 0.18F;
        };
    }

    private float workstationInterest() {
        return switch (npc.getPersonality()) {
            case TACTICAL, PATIENT -> 0.52F;
            case BALANCED, OPPORTUNISTIC -> 0.42F;
            case CAUTIOUS, PROTECTIVE, LOYAL -> 0.32F;
            case BRAVE, STUBBORN -> 0.26F;
            case AGGRESSIVE, SKITTISH -> 0.20F;
            case RECKLESS -> 0.14F;
        };
    }

    private enum LeisureAction {
        NONE,
        WATCH_SUNSET,
        USE_WORKSTATION,
        WATCH_CAMPFIRE
    }
}
