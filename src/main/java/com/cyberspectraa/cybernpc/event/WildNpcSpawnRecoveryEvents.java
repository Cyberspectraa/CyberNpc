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
    // One inexpensive opportunity per minute per loaded dimension. Local caps
    // make further attempts no-ops once the area has a small expedition presence.
    private static final long SPAWN_INTERVAL_TICKS = 1200L;
    private static final int LOCAL_WILD_CAP = 8;
    private static final int LOCAL_SCAN_RADIUS = 96;
    private static final int MIN_PLAYER_DISTANCE = 40;
    private static final int MAX_PLAYER_DISTANCE = 88;
    private static final int SEARCH_ATTEMPTS = 20;

    private WildNpcSpawnRecoveryEvents() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || (level.dimension() != Level.OVERWORLD
                    && level.dimension() != Level.NETHER)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || level.getGameTime() % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()
                    || level.random.nextFloat() >= 0.65F) {
                continue;
            }

            AABB local = player.getBoundingBox().inflate(LOCAL_SCAN_RADIUS);
            long existing = level.getEntitiesOfClass(CyberNpcEntity.class, local,
                    npc -> npc.isAlive() && npc.getNpcType() == NpcType.WILD).size();
            if (existing >= LOCAL_WILD_CAP) continue;

            int size = Math.min(2 + level.random.nextInt(3),
                    LOCAL_WILD_CAP - (int) existing);
            if (size < 2) continue;

            BlockPos site = findSite(level, player);
            if (site != null) {
                spawnExpedition(level, site, size);
            }
        }
    }

    private static BlockPos findSite(ServerLevel level, ServerPlayer player) {
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
            if (!level.hasChunkAt(column)) continue;

            if (level.dimension() == Level.OVERWORLD) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 28) continue;
                BlockPos pos = new BlockPos(x, y, z);
                if (safeSite(level, pos) && noNearbyNpc(level, pos)) return pos;
            } else {
                // A heightmap in the Nether normally returns the bedrock roof.
                // Search near the player's actual Y instead.
                int top = Math.min(level.getMaxBuildHeight() - 3, player.getBlockY() + 14);
                int bottom = Math.max(level.getMinBuildHeight() + 2, player.getBlockY() - 18);
                for (int y = top; y >= bottom; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (safeSite(level, pos) && noNearbyNpc(level, pos)) return pos;
                }
            }
        }
        return null;
    }

    private static boolean safeSite(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())
                || !level.hasChunkAt(pos.below())
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

    private static boolean noNearbyNpc(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(CyberNpcEntity.class,
                new AABB(pos).inflate(6.0D), npc -> npc.isAlive()).isEmpty();
    }

    private static void spawnExpedition(ServerLevel level, BlockPos centre, int size) {
        SpawnGroupData group = null;
        for (int index = 0; index < size; index++) {
            BlockPos spot = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                BlockPos candidate = index == 0 ? centre : centre.offset(
                        level.random.nextInt(7) - 3, 0,
                        level.random.nextInt(7) - 3);
                if (safeSite(level, candidate)) {
                    spot = candidate;
                    break;
                }
            }
            if (spot == null) break;

            CyberNpcEntity npc = ModEntities.CYBER_NPC.get().create(level);
            if (npc == null) break;
            npc.moveTo(spot.getX() + 0.5D, spot.getY(),
                    spot.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(npc)) continue;

            group = npc.finalizeSpawn(level, level.getCurrentDifficultyAt(spot),
                    MobSpawnType.NATURAL, group, null);
            if (!level.addFreshEntity(npc)) break;
        }
    }
}
