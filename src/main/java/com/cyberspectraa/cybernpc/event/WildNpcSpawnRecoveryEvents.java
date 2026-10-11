package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Supplements vanilla passive-mob spawning for Wild NPC expeditions.
 *
 * Vanilla CREATURE spawns are infrequent and usually blocked by the small
 * passive-mob cap in established/modded worlds. This server-only, deliberately
 * infrequent check gives those worlds a way to meet Wild NPCs without changing
 * their friendly mob category, bypassing gamerules, or changing other mobs.
 *
 * It uses NATURAL finalizeSpawn with shared SpawnGroupData, so existing pack
 * formation, CyberRaces biome selection, classes and one-off campfire placement
 * all follow exactly the same path as ordinary biome-modifier spawns.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WildNpcSpawnRecoveryEvents {
    // A bounded encounter opportunity every 30 seconds per dimension.
    // Local caps prevent expeditions from overwhelming populated worlds.
    private static final long SPAWN_INTERVAL_TICKS = 600L;
    private static final int LOCAL_WILD_CAP = 12;
    private static final int LOCAL_SCAN_RADIUS = 96;
    private static final int MIN_PLAYER_DISTANCE = 36;
    private static final int MAX_PLAYER_DISTANCE = 88;
    private static final int SEARCH_ATTEMPTS = 36;
    private static final int MAX_PLAYERS_PER_PASS = 2;
    private static final int STARTUP_GRACE_TICKS = 200;

    private WildNpcSpawnRecoveryEvents() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || (level.dimension() != Level.OVERWORLD
                    && level.dimension() != Level.NETHER)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || level.getServer().getTickCount() < STARTUP_GRACE_TICKS
                || level.getGameTime() % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }

        // Never scan every online player's surroundings in a single tick.
        // Rotate which players get checked so multiplayer encounters remain fair.
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        int first = (int) ((level.getGameTime() / SPAWN_INTERVAL_TICKS) % players.size());
        int checks = Math.min(MAX_PLAYERS_PER_PASS, players.size());
        for (int index = 0; index < checks; index++) {
            ServerPlayer player = players.get((first + index) % players.size());
            if (player.isSpectator() || !player.isAlive()
                    || level.random.nextFloat() >= 0.80F) {
                continue;
            }

            AABB local = player.getBoundingBox().inflate(LOCAL_SCAN_RADIUS);
            long existing = level.getEntitiesOfClass(CyberNpcEntity.class, local,
                    npc -> npc.isAlive() && npc.getNpcType() == NpcType.WILD).size();
            if (existing >= LOCAL_WILD_CAP) continue;

            // Keep the Nether's smaller 2-3 member packs, even for
            // supplemental encounters; Overworld packs stay at 2-4.
            int size = Math.min(2 + level.random.nextInt(
                    level.dimension() == Level.NETHER ? 2 : 3),
                    LOCAL_WILD_CAP - (int) existing);
            if (size < 2) continue;

            BlockPos site = findSite(level, player);
            if (site != null) {
                spawnExpedition(level, site, size);
            }
        }
    }

    private static BlockPos findSite(ServerLevel level, ServerPlayer player) {
        // Do not read a heightmap in a chunk that is not fully available yet.
        if (!isFullyLoaded(level, player.blockPosition())) return null;
        // Surface heightmaps miss cave expeditions entirely. When the player
        // is well below the local surface, search walkable cave floors near
        // their actual Y rather than placing the pack far above them.
        boolean underground = level.dimension() == Level.OVERWORLD
                && player.getBlockY() + 20
                    < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            player.getBlockX(), player.getBlockZ());
        for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
            int dx = level.random.nextInt(MAX_PLAYER_DISTANCE * 2 + 1)
                    - MAX_PLAYER_DISTANCE;
            int dz = level.random.nextInt(MAX_PLAYER_DISTANCE * 2 + 1)
                    - MAX_PLAYER_DISTANCE;
            int distanceSq = dx * dx + dz * dz;
            if (distanceSq < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE
                    || distanceSq > MAX_PLAYER_DISTANCE * MAX_PLAYER_DISTANCE) {
                continue;
            }

            int x = player.getBlockX() + dx;
            int z = player.getBlockZ() + dz;
            BlockPos column = new BlockPos(x, player.getBlockY(), z);
            // Never generate chunks or populate unexplored territory just to
            // force a spawn. Exploration and normal chunk loading remain in charge.
            if (!isFullyLoaded(level, column)) continue;

            if (level.dimension() == Level.OVERWORLD && !underground) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 28) continue;
                BlockPos pos = new BlockPos(x, y, z);
                if (safeSite(level, pos) && noNearbyNpc(level, pos)) return pos;
            } else {
                // The Nether heightmap points at its bedrock roof; caves
                // likewise need a local vertical scan to find real floors.
                int above = level.dimension() == Level.NETHER ? 14 : 16;
                int below = level.dimension() == Level.NETHER ? 18 : 16;
                int top = Math.min(level.getMaxBuildHeight() - 3, player.getBlockY() + above);
                int bottom = Math.max(level.getMinBuildHeight() + 2, player.getBlockY() - below);
                for (int y = top; y >= bottom; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (safeSite(level, pos) && noNearbyNpc(level, pos)) return pos;
                }
            }
        }
        return null;
    }

    private static boolean safeSite(ServerLevel level, BlockPos pos) {
        if (!isFullyLoaded(level, pos) || !isFullyLoaded(level, pos.above())
                || !isFullyLoaded(level, pos.below())
                || !level.getWorldBorder().isWithinBounds(pos)
                || pos.getY() < level.getMinBuildHeight() + 2
                || pos.getY() >= level.getMaxBuildHeight() - 2) {
            return false;
        }
        BlockState ground = level.getBlockState(pos.below());
        // Restrict supplementation to plausible wild ground rather than inside
        // constructed buildings or on rooftops, leaves or artificial platforms.
        if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND)
                || ground.is(BlockTags.BASE_STONE_OVERWORLD)
                || ground.is(BlockTags.BASE_STONE_NETHER)
                || ground.is(Blocks.GRAVEL) || ground.is(Blocks.MUD)
                || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.BASALT)
                || ground.is(Blocks.BLACKSTONE) || ground.is(Blocks.NETHERRACK)
                || ground.is(Blocks.CRIMSON_NYLIUM)
                || ground.is(Blocks.WARPED_NYLIUM)
                || ground.is(Blocks.SOUL_SOIL))) {
            return false;
        }
        if (!ground.isFaceSturdy(level, pos.below(), Direction.UP)
                || !CyberNpcEntity.checkCyberNpcSpawnRules(
                        ModEntities.CYBER_NPC.get(), level, MobSpawnType.NATURAL,
                        pos, level.random)) {
            return false;
        }
        // Do not create a group in a player's immediate view or on top of
        // existing NPCs, regardless of which player triggered the attempt.
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)
                    < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE) return false;
        }
        return true;
    }

    private static boolean isFullyLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static boolean noNearbyNpc(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(CyberNpcEntity.class,
                new AABB(pos).inflate(6.0D), npc -> npc.isAlive()).isEmpty();
    }

    private static void spawnExpedition(ServerLevel level, BlockPos centre, int size) {
        SpawnGroupData group = null;
        for (int index = 0; index < size; index++) {
            CyberNpcEntity npc = ModEntities.CYBER_NPC.get().create(level);
            if (npc == null) break;

            float yaw = level.random.nextFloat() * 360.0F;
            BlockPos spot = index == 0 ? centre : findPackMemberSite(level, centre, npc, yaw);
            if (spot == null) break;

            npc.moveTo(spot.getX() + 0.5D, spot.getY(),
                    spot.getZ() + 0.5D, yaw, 0.0F);
            if (!level.noCollision(npc)) break;

            // Finalize only once a valid site is found, preserving the shared
            // pack ID, race habitat seed, gear/class rolls and camp leader.
            group = npc.finalizeSpawn(level, level.getCurrentDifficultyAt(spot),
                    MobSpawnType.NATURAL, group, null);
            if (!level.addFreshEntity(npc)) break;
        }
    }

    private static BlockPos findPackMemberSite(ServerLevel level, BlockPos centre,
                                                CyberNpcEntity npc, float yaw) {
        // A fixed Y makes packs collapse into singletons on slopes and in
        // caves. Check a small vertical neighbourhood without digging or
        // generating chunks; the ordinary spawn/collision checks still apply.
        for (int attempt = 0; attempt < 18; attempt++) {
            int dx = level.random.nextInt(9) - 4;
            int dz = level.random.nextInt(9) - 4;
            if (dx == 0 && dz == 0) continue;
            for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
                BlockPos candidate = centre.offset(dx, dy, dz);
                if (!safeSite(level, candidate)) continue;
                npc.moveTo(candidate.getX() + 0.5D, candidate.getY(),
                        candidate.getZ() + 0.5D, yaw, 0.0F);
                if (level.noCollision(npc)) return candidate;
            }
        }
        return null;
    }
}
