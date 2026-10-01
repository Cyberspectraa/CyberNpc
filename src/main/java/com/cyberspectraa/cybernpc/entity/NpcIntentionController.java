package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Small high-level commitment layer for non-critical player-like behaviour.
 *
 * The important part is inertia: an NPC keeps one chosen intention for a
 * sensible amount of time instead of re-rolling an activity every tick. Higher
 * priority reactions may replace lower priority curiosity, while combat,
 * hunger, sleep and other hard needs clear the intention immediately.
 */
final class NpcIntentionController {
    private final CyberNpcEntity npc;

    private Intent kind = Intent.NONE;
    private int priority;
    private int ticksRemaining;

    @Nullable
    private UUID targetEntityId;

    @Nullable
    private BlockPos targetPos;

    private String reason = "No short-term intention";

    NpcIntentionController(CyberNpcEntity npc) {
        this.npc = npc;
    }

    void tick() {
        if (kind == Intent.NONE) {
            return;
        }

        if (!npc.canHoldPlayerLikeIntention()) {
            clear();
            return;
        }

        if (ticksRemaining > 0) {
            ticksRemaining--;
        }

        if (ticksRemaining <= 0) {
            clear();
        }
    }

    boolean request(
            Intent requested,
            int requestedPriority,
            int durationTicks,
            @Nullable UUID entityId,
            @Nullable BlockPos pos,
            String why
    ) {
        if (requested == null
                || requested == Intent.NONE
                || durationTicks <= 0
                || !npc.canHoldPlayerLikeIntention()) {
            return false;
        }

        if (kind != Intent.NONE
                && requestedPriority < priority
                && ticksRemaining > 0) {
            return false;
        }

        // Do not restart the exact same intention every scan. Refreshing the
        // target/reason is fine, but commitment time counts down naturally.
        if (kind == requested
                && equalTarget(targetEntityId, entityId)
                && equalPos(targetPos, pos)
                && ticksRemaining > 0) {
            targetEntityId = entityId;
            targetPos = pos == null ? null : pos.immutable();
            reason = why == null || why.isBlank() ? reason : why;
            return true;
        }

        kind = requested;
        priority = requestedPriority;
        ticksRemaining = durationTicks;
        targetEntityId = entityId;
        targetPos = pos == null ? null : pos.immutable();
        reason = why == null || why.isBlank()
                ? "Chose a short-term player-like activity"
                : why;
        return true;
    }

    boolean is(Intent expected) {
        return kind == expected && ticksRemaining > 0;
    }

    boolean isActive() {
        return kind != Intent.NONE && ticksRemaining > 0;
    }

    Intent kind() {
        return kind;
    }

    int ticksRemaining() {
        return ticksRemaining;
    }

    @Nullable
    UUID targetEntityId() {
        return targetEntityId;
    }

    @Nullable
    BlockPos targetPos() {
        return targetPos;
    }

    String reason() {
        return reason;
    }

    String debugSummary() {
        if (!isActive()) {
            return "none";
        }

        String target;
        if (targetEntityId != null) {
            String id = targetEntityId.toString();
            target = " entity:" + id.substring(0, Math.min(8, id.length()));
        } else if (targetPos != null) {
            target = " pos:"
                    + targetPos.getX() + ","
                    + targetPos.getY() + ","
                    + targetPos.getZ();
        } else {
            target = "";
        }

        return kind.displayName
                + " | " + Math.max(0, ticksRemaining / 20) + "s"
                + target;
    }

    void complete() {
        clear();
    }

    void clear() {
        kind = Intent.NONE;
        priority = 0;
        ticksRemaining = 0;
        targetEntityId = null;
        targetPos = null;
        reason = "No short-term intention";
    }

    private static boolean equalTarget(
            @Nullable UUID a,
            @Nullable UUID b
    ) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean equalPos(
            @Nullable BlockPos a,
            @Nullable BlockPos b
    ) {
        return a == null ? b == null : a.equals(b);
    }

    enum Intent {
        NONE("None"),
        WATCH_SUNSET("Watch sunset"),
        WATCH_PLAYER("Watch player"),
        OBSERVE_FIGHT("Observe fight"),
        INSPECT_ENTITY("Inspect something"),
        USE_WORKSTATION("Use workstation"),
        WATCH_CAMPFIRE("Watch campfire"),
        SEEK_WEATHER_SHELTER("Seek weather shelter"),
        WATCH_STORM("Watch storm"),
        REACT_LIGHTNING("React to lightning");

        private final String displayName;

        Intent(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }
}
