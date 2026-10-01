package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * Medium-priority reactions to the world itself. These are stronger than
 * ordinary curiosity but weaker than combat, hunger, sleep and immediate
 * survival logic.
 */
final class NpcEnvironmentalReactionBrain {
    private static final int WEATHER_SCAN_INTERVAL = 20;
    private static final int SHELTER_RADIUS = 14;
    private static final int SHELTER_VERTICAL = 5;
    private static final double SHELTER_REACHED_SQR = 4.0D;
    private static final double LIGHTNING_NOTICE_RADIUS = 30.0D;

    private final CyberNpcEntity npc;
    private final NpcIntentionController intentions;

    private int scanCooldown;

    NpcEnvironmentalReactionBrain(
            CyberNpcEntity npc,
            NpcIntentionController intentions
    ) {
        this.npc = npc;
        this.intentions = intentions;
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || !npc.canReactToEnvironment()) {
            return false;
        }

        if (isEnvironmentalIntent(intentions.kind())) {
            return tickCurrent(level);
        }

        if (scanCooldown > 0) {
            scanCooldown--;
            return false;
        }
        scanCooldown = WEATHER_SCAN_INTERVAL;

        if (tryReactToLightning(level)) {
            return tickCurrent(level);
        }

        if (tryReactToWeather(level)) {
            return tickCurrent(level);
        }

        return false;
    }

    boolean ownsCurrentIntention() {
        return isEnvironmentalIntent(intentions.kind());
    }

    String getActivity() {
        return switch (intentions.kind()) {
            case SEEK_WEATHER_SHELTER -> "Getting out of the weather";
            case WATCH_STORM -> "Watching the weather from shelter";
            case REACT_LIGHTNING -> "Reacting to nearby lightning";
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
    }

    private boolean tickCurrent(ServerLevel level) {
        return switch (intentions.kind()) {
            case SEEK_WEATHER_SHELTER -> tickSeekShelter(level);
            case WATCH_STORM -> tickWatchStorm(level);
            case REACT_LIGHTNING -> tickLightningReaction(level);
            default -> false;
        };
    }

    private boolean tryReactToLightning(ServerLevel level) {
        LightningBolt bolt = level.getEntitiesOfClass(
                        LightningBolt.class,
                        npc.getBoundingBox().inflate(LIGHTNING_NOTICE_RADIUS),
                        entity -> entity.isAlive()
                ).stream()
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);

        if (bolt == null
                || npc.getRandom().nextFloat() > lightningInterest()) {
            return false;
        }

        return intentions.request(
                NpcIntentionController.Intent.REACT_LIGHTNING,
                90,
                35 + npc.getRandom().nextInt(46),
                null,
                bolt.blockPosition(),
                "A nearby lightning strike was sudden enough to interrupt normal activity"
        );
    }

    private boolean tryReactToWeather(ServerLevel level) {
        boolean thunder = level.isThundering();
        boolean rainingHere = level.isRainingAt(npc.blockPosition());

        if (!thunder && !rainingHere) {
            return false;
        }

        float shelterChance = thunder
                ? thunderShelterInterest()
                : rainShelterInterest();

        if (npc.getRandom().nextFloat() > shelterChance) {
            return false;
        }

        BlockPos shelter = findNearbyShelter(level);
        if (shelter == null) {
            // If no reachable cover exists, acknowledge the storm instead of
            // running aimlessly.
            return intentions.request(
                    NpcIntentionController.Intent.WATCH_STORM,
                    thunder ? 60 : 42,
                    60 + npc.getRandom().nextInt(101),
                    null,
                    npc.blockPosition(),
                    thunder
                            ? "A thunderstorm is active but no nearby shelter is reachable, so this NPC is watching its surroundings"
                            : "Rain is falling and no nearby shelter is reachable, so this NPC is briefly reacting to the weather"
            );
        }

        return intentions.request(
                NpcIntentionController.Intent.SEEK_WEATHER_SHELTER,
                thunder ? 78 : 62,
                thunder ? 260 : 200,
                null,
                shelter,
                thunder
                        ? "Thunder and rain make nearby cover preferable to staying exposed"
                        : "Rain is uncomfortable enough that this NPC chose nearby cover"
        );
    }

    private boolean tickSeekShelter(ServerLevel level) {
        if (!level.isRaining() && !level.isThundering()) {
            intentions.complete();
            return false;
        }

        BlockPos shelter = intentions.targetPos();
        if (shelter == null || !isShelter(level, shelter)) {
            BlockPos replacement = findNearbyShelter(level);
            if (replacement == null) {
                intentions.complete();
                return false;
            }

            intentions.request(
                    NpcIntentionController.Intent.SEEK_WEATHER_SHELTER,
                    level.isThundering() ? 78 : 62,
                    160,
                    null,
                    replacement,
                    intentions.reason()
            );
            shelter = replacement;
        }

        Vec3 center = Vec3.atBottomCenterOf(shelter);
        npc.getLookControl().setLookAt(center.x, center.y + 1.0D, center.z, 18.0F, 14.0F);

        if (npc.distanceToSqr(center) > SHELTER_REACHED_SQR) {
            npc.getNavigation().moveTo(
                    center.x,
                    center.y,
                    center.z,
                    level.isThundering() ? 1.02D : 0.82D
            );
            npc.setSprinting(level.isThundering()
                    && npc.distanceToSqr(center) > 64.0D);
            return true;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);
        intentions.request(
                NpcIntentionController.Intent.WATCH_STORM,
                level.isThundering() ? 60 : 42,
                100 + npc.getRandom().nextInt(141),
                null,
                shelter,
                "Reached cover and is staying there briefly instead of immediately wandering back into the weather"
        );
        return true;
    }

    private boolean tickWatchStorm(ServerLevel level) {
        if (!level.isRaining() && !level.isThundering()) {
            intentions.complete();
            return false;
        }

        BlockPos pos = intentions.targetPos();
        if (pos != null && isShelter(level, pos)
                && npc.distanceToSqr(Vec3.atBottomCenterOf(pos)) > 9.0D) {
            npc.getNavigation().moveTo(
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    0.72D
            );
        } else {
            npc.getNavigation().stop();
        }

        npc.setSprinting(false);

        // A sheltered NPC occasionally looks outward/up as if checking the
        // rain or thunder instead of staring motionless at the floor.
        double side = ((npc.tickCount / 30) & 1) == 0 ? 12.0D : -12.0D;
        npc.getLookControl().setLookAt(
                npc.getX() + side,
                npc.getEyeY() + (level.isThundering() ? 6.0D : 3.0D),
                npc.getZ() + 8.0D,
                10.0F,
                8.0F
        );
        return true;
    }

    private boolean tickLightningReaction(ServerLevel level) {
        BlockPos strike = intentions.targetPos();
        if (strike == null) {
            intentions.complete();
            return false;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);
        npc.getLookControl().setLookAt(
                strike.getX() + 0.5D,
                strike.getY() + 4.0D,
                strike.getZ() + 0.5D,
                36.0F,
                30.0F
        );
        return true;
    }

    @Nullable
    private BlockPos findNearbyShelter(ServerLevel level) {
        BlockPos origin = npc.blockPosition();

        return BlockPos.betweenClosedStream(
                        origin.offset(-SHELTER_RADIUS, -SHELTER_VERTICAL, -SHELTER_RADIUS),
                        origin.offset(SHELTER_RADIUS, SHELTER_VERTICAL, SHELTER_RADIUS)
                )
                .filter(pos -> isShelter(level, pos))
                .map(BlockPos::immutable)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(origin)))
                .filter(pos -> {
                    Path path = npc.getNavigation().createPath(pos, 0);
                    return path != null
                            && (path.canReach() || path.getEndNode() != null);
                })
                .findFirst()
                .orElse(null);
    }

    private static boolean isShelter(ServerLevel level, BlockPos feet) {
        return !level.isRainingAt(feet)
                && !level.canSeeSky(feet.above())
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(feet.above()).isEmpty()
                && level.getBlockState(feet)
                .getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above())
                .getCollisionShape(level, feet.above()).isEmpty()
                && !level.getBlockState(feet.below())
                .getCollisionShape(level, feet.below()).isEmpty();
    }

    private static boolean isEnvironmentalIntent(NpcIntentionController.Intent intent) {
        return switch (intent) {
            case SEEK_WEATHER_SHELTER,
                 WATCH_STORM,
                 REACT_LIGHTNING -> true;
            default -> false;
        };
    }

    private float rainShelterInterest() {
        return switch (npc.getPersonality()) {
            case CAUTIOUS, PATIENT -> 0.82F;
            case BALANCED, LOYAL, PROTECTIVE -> 0.68F;
            case TACTICAL, STUBBORN -> 0.58F;
            case OPPORTUNISTIC, SKITTISH -> 0.52F;
            case BRAVE -> 0.38F;
            case AGGRESSIVE -> 0.26F;
            case RECKLESS -> 0.16F;
        };
    }

    private float thunderShelterInterest() {
        return switch (npc.getPersonality()) {
            case CAUTIOUS, SKITTISH -> 0.96F;
            case PATIENT, BALANCED, LOYAL, PROTECTIVE -> 0.88F;
            case TACTICAL, OPPORTUNISTIC -> 0.80F;
            case STUBBORN -> 0.68F;
            case BRAVE -> 0.58F;
            case AGGRESSIVE -> 0.48F;
            case RECKLESS -> 0.32F;
        };
    }

    private float lightningInterest() {
        return switch (npc.getPersonality()) {
            case SKITTISH, CAUTIOUS -> 0.95F;
            case BALANCED, PATIENT, TACTICAL -> 0.86F;
            case PROTECTIVE, LOYAL -> 0.80F;
            case BRAVE, OPPORTUNISTIC -> 0.70F;
            case STUBBORN -> 0.58F;
            case AGGRESSIVE -> 0.48F;
            case RECKLESS -> 0.36F;
        };
    }
}
