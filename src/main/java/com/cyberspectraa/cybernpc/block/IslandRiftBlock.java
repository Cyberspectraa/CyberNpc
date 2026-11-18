package com.cyberspectraa.cybernpc.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorative, admin-placed island Rift anchor.
 *
 * Intentionally has NO interaction or gameplay logic yet: no player discovery,
 * teleportation, quest progress, NPC followers or departures. When that phase
 * is implemented, it must be enabled deliberately and tested separately.
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

    // No use() override: right-click uses Block's ordinary no-op behaviour.
    // This is a visual prop only until the island finale is designed.

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
