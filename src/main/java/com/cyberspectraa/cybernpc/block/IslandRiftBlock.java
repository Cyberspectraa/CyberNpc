package com.cyberspectraa.cybernpc.block;

import com.cyberspectraa.cybernpc.registry.ModBlockEntities;
import com.cyberspectraa.cybernpc.rift.RiftBlockEntity;
import com.cyberspectraa.cybernpc.rift.RiftFinaleService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Two blocks high, one block wide: the series' final crossing.
 * Keep the existing model and mandatory Photon FX. Physically touching the
 * tear offers a server-verified confirmation; never begin on contact alone.
 * Preserve the existing island_rift block/item ID and save compatibility.
 */
public final class IslandRiftBlock extends Block implements EntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTH_SOUTH =
            box(2.0D, 0.0D, 6.0D, 14.0D, 16.0D, 10.0D);
    private static final VoxelShape EAST_WEST =
            box(6.0D, 0.0D, 2.0D, 10.0D, 16.0D, 14.0D);

    public IslandRiftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (above.getY() >= context.getLevel().getMaxBuildHeight()
                || !context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        // The ordinary admin-only BlockItem places the lower block first.
        // The upper half is kept in sync by updateShape.
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction.getAxis() == Direction.Axis.Y
                && (state.getValue(HALF) == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighbour.is(this)
                    && neighbour.getValue(HALF) != state.getValue(HALF)
                    ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X
                ? EAST_WEST : NORTH_SOUTH;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        // No collision: a player can physically cross into the tear to
        // receive an opt-in, irreversible departure confirmation.
        return Shapes.empty();
    }

    // Do not teleport on contact, or add a right-click menu to the Rift.
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            RiftFinaleService.touch(player, pos);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new RiftBlockEntity(pos, state) : null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide
                || state.getValue(HALF) != DoubleBlockHalf.LOWER
                || type != ModBlockEntities.ISLAND_RIFT.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, tile) -> {
            if (tile instanceof RiftBlockEntity rift) {
                RiftBlockEntity.serverTick(tickLevel, tickPos, tickState, rift);
            }
        };
    }
}
