package com.cyberspectraa.cybernpc.town;

import com.cyberspectraa.cybernpc.intro.IntroSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Horizontal safety boundaries for naturally generated Wild NPC expeditions.
 *
 * Towns are the area players live in, not wilderness encounters. A bell has a
 * broad buffer covering the 256-block house registration radius, and each
 * registered house protects its surroundings if the town is sprawling.
 * World spawn and the configured CyberNpc first-arrival point are protected
 * even in a new world before a player creates a bell town.
 *
 * This only reads saved positions: no block scans, entity searches, chunk
 * tickets, forced chunk generation or changes to the current world.
 */
public final class TownWildSpawnProtection {
    public static final int TOWN_BELL_RADIUS = 320;
    public static final int REGISTERED_HOME_RADIUS = 144;
    public static final int WORLD_SPAWN_RADIUS = 192;
    public static final int INTRO_ARRIVAL_RADIUS = 192;

    public static boolean blocksNaturalWildSpawn(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.OVERWORLD
                && withinHorizontalRange(pos, level.getSharedSpawnPos(), WORLD_SPAWN_RADIUS)) {
            return true;
        }

        IntroSettings.Point arrival = IntroSettings.get(level.getServer()).arrival();
        if (arrival != null && arrival.dimension().equals(level.dimension())
                && withinHorizontalRange(pos, arrival.pos(), INTRO_ARRIVAL_RADIUS)) {
            return true;
        }

        return TownHomeSavedData.get(level).blocksNaturalWildSpawn(level, pos);
    }

    public static boolean withinHorizontalRange(BlockPos a, BlockPos b, int radius) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    private TownWildSpawnProtection() {}
}
