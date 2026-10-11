package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A single, small wilderness camp for each successful natural Wild NPC pack.
 * Uses the existing CyberNpc campfire cooking AI (no duplicate food system).
 * Never touches existing buildings: only open air over ordinary natural soil
 * or stone is eligible, and no other placed blocks are replaced.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NaturalWildNpcCampEvents {
    private static final String ATTEMPTED = "CyberNpcNaturalCampAttempted";

    private NaturalWildNpcCampEvents() {}

    @SubscribeEvent
    public static void onWildNpcTick(LivingEvent.LivingTickEvent event) {
        // Never inspect or modify the world inside EntityJoinLevelEvent:
        // CREATURE mobs can join while initial spawn chunks are generating.
        // This runs only once a real, server-ticking leader is established.
        if (!(event.getEntity() instanceof CyberNpcEntity npc)
                || npc.getNpcType() != NpcType.WILD
                || !npc.getPersistentData().getBoolean(CyberNpcEntity.NATURAL_SPAWN_TAG)
                || !npc.getPersistentData().getBoolean(CyberNpcEntity.NATURAL_PACK_LEADER_TAG)
                || npc.getPersistentData().getBoolean(ATTEMPTED)
                || npc.tickCount < 80
                || npc.tickCount % 20 != 0
                || !(npc.level() instanceof ServerLevel level)
                || level.players().isEmpty()
                || level.getServer().getTickCount() < 100) {
            return;
        }
        // Persist the single attempt across chunk reloads, even when a camp
        // cannot safely be placed. No world queries are made before this point.
        npc.getPersistentData().putBoolean(ATTEMPTED, true);
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || level.dimension() == Level.END) {
            return;
        }

        BlockPos centre = npc.blockPosition();
        if (nearExistingCamp(level, centre)) return;

        // Only build at the edge of the pack, not in its walking space.
        // Deterministic randomized attempts spread sites around the leader.
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = level.random.nextInt(9) - 4;
            int dz = level.random.nextInt(9) - 4;
            if (dx * dx + dz * dz < 6) continue;
            for (int dy : new int[] {0, -1, 1, -2}) {
                BlockPos spot = centre.offset(dx, dy, dz);
                if (!isSafeSite(level, spot)) continue;
                BlockState camp = (level.dimension() == Level.NETHER
                        ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE)
                        .defaultBlockState();
                // A vanilla fire will not spread to adjacent buildings; the
                // site check additionally avoids flammable blocks.
                if (level.setBlock(spot, camp, Block.UPDATE_ALL)) {
                    npc.getPersistentData().putLong("CyberNpcNaturalCamp", spot.asLong());
                    return;
                }
            }
        }
    }

    private static boolean nearExistingCamp(ServerLevel level, BlockPos centre) {
        for (BlockPos nearby : BlockPos.betweenClosed(
                centre.offset(-12, -3, -12),
                centre.offset(12, 3, 12))) {
            if (!isFullyLoaded(level, nearby)) continue;
            Block block = level.getBlockState(nearby).getBlock();
            if (block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSafeSite(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() + 2
                || pos.getY() >= level.getMaxBuildHeight() - 2
                || !isFullyLoaded(level, pos)
                || !isFullyLoaded(level, pos.above())
                || !isFullyLoaded(level, pos.below())) return false;
        if (!level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.above()).isAir()
                || !level.getFluidState(pos).isEmpty()) return false;

        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isFaceSturdy(level, pos.below(), Direction.UP)
                || !isNaturalFloor(ground)) return false;

        // Keep fire clear of woodland, beds, player-placed decor and other
        // vulnerable surroundings; never excavate, clear or replace blocks.
        for (BlockPos around : BlockPos.betweenClosed(
                pos.offset(-1, -1, -1), pos.offset(1, 2, 1))) {
            if (around.equals(pos) || around.equals(pos.below())) continue;
            BlockState state = level.getBlockState(around);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                    || state.is(BlockTags.WOOL)
                    || state.is(Blocks.HAY_BLOCK)
                    || state.is(Blocks.TNT)
                    || state.is(Blocks.CHEST)
                    || state.is(Blocks.BARREL)
                    || state.is(Blocks.CRAFTING_TABLE)
                    || state.is(Blocks.FURNACE)
                    || state.is(Blocks.BEDROCK)) return false;
            // Do not construct immediately alongside a block entity (homes,
            // farms, shops and protected workshop machinery).
            if (level.getBlockEntity(around) != null) return false;
        }
        return level.getEntitiesOfClass(CyberNpcEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(0.65D),
                e -> e.isAlive()).isEmpty();
    }

    // getChunkNow never loads/generates a chunk or waits for worldgen to finish.
    // hasChunkAt alone is not a guarantee that querying block data is safe.
    private static boolean isFullyLoaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static boolean isNaturalFloor(BlockState state) {
        Block floor = state.getBlock();
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || floor == Blocks.GRAVEL
                || floor == Blocks.MUD
                || floor == Blocks.BASALT
                || floor == Blocks.BLACKSTONE
                || floor == Blocks.NETHERRACK
                || floor == Blocks.CRIMSON_NYLIUM
                || floor == Blocks.WARPED_NYLIUM;
    }
}
