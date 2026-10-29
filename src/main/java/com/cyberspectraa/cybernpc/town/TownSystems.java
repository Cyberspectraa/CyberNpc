package com.cyberspectraa.cybernpc.town;

/**
 * Temporary retirement of ordinary town housing, workplaces and guard posts.
 * Keep block/item IDs and SavedData readers intact for older worlds; gameplay
 * stops reading or changing those assignments until a redesigned system ships.
 * Special NPC service anchors and guard patrols remain active.
 */
public final class TownSystems {
    private TownSystems() {}

    public static boolean housingAndJobsEnabled() {
        return false;
    }

    public static boolean guardPostsEnabled() {
        return false;
    }
}
