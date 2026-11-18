package com.cyberspectraa.cybernpc.block;

import com.cyberspectraa.cybernpc.rift.RiftDiscoverySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A one-block story Rift anchor. Actual dimension travel, NPC escorts and
 * departures are intentionally not enabled until the island quest is ready.
 */
public final class IslandRiftBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            box(1, 0, 1, 15, 4, 15),
            box(4, 4, 4, 12, 16, 12)
    );

    public IslandRiftBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            boolean discovered = RiftDiscoverySavedData.get(serverLevel)
                    .discover(serverPlayer, pos);

            if (discovered) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "You have discovered the Island Rift. " +
                        "Something beyond the island is calling..."));
            } else {
                serverPlayer.displayClientMessage(Component.literal(
                        "Island Rift discovered. Its destination is still unknown."), true);
            }

            serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.BLOCKS, 0.65F, 0.85F);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                    18, 0.25D, 0.45D, 0.25D, 0.06D);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level,
            BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.28D + random.nextDouble() * 0.44D;
            double y = pos.getY() + 0.3D + random.nextDouble() * 0.9D;
            double z = pos.getZ() + 0.28D + random.nextDouble() * 0.44D;
            level.addParticle(random.nextBoolean()
                            ? ParticleTypes.PORTAL : ParticleTypes.ENCHANT,
                    x, y, z, (random.nextDouble() - 0.5D) * 0.08D,
                    0.025D, (random.nextDouble() - 0.5D) * 0.08D);
        }

        if (random.nextInt(450) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.6D,
                    pos.getZ() + 0.5D, SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS, 0.12F, 0.8F, false);
        }
    }
}
