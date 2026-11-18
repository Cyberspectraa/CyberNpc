package com.cyberspectraa.cybernpc.rift;

import com.cyberspectraa.cybernpc.block.IslandRiftBlock;
import com.cyberspectraa.cybernpc.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Visual-only Photon trigger. Keeps a short FX playing for players near a
 * loaded Rift, including players who arrive after the block was placed.
 * The block entity does not persist character data or run quest actions.
 */
public final class RiftBlockEntity extends BlockEntity {
    private static final long FX_INTERVAL_TICKS = 96L;
    private static final double EFFECT_VIEW_DISTANCE_SQ = 28.0D * 28.0D;

    public RiftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ISLAND_RIFT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos,
            BlockState state, RiftBlockEntity rift) {
        if (!(level instanceof ServerLevel serverLevel)
                || state.getValue(IslandRiftBlock.HALF) != DoubleBlockHalf.LOWER
                || serverLevel.getGameTime() % FX_INTERVAL_TICKS
                        != Math.floorMod(pos.asLong(), FX_INTERVAL_TICKS)) {
            return;
        }

        boolean nearPlayer = serverLevel.players().stream().anyMatch(player ->
                player.distanceToSqr(
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D)
                        <= EFFECT_VIEW_DISTANCE_SQ);
        if (nearPlayer) {
            RiftPhotonEffects.play(serverLevel, pos);
        }
    }
}
