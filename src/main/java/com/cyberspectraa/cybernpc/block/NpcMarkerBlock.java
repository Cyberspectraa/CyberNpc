package com.cyberspectraa.cybernpc.block;

import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.building.NpcMarkerIds;
import com.cyberspectraa.cybernpc.building.NpcMarkerKind;
import com.cyberspectraa.cybernpc.building.NpcMarkerManager;
import com.cyberspectraa.cybernpc.item.TownRegisterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NpcMarkerBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape OUTLINE =
            box(4.0D, 0.0D, 4.0D, 12.0D, 3.0D, 12.0D);

    private final NpcMarkerKind kind;

    public NpcMarkerBlock(
            NpcMarkerKind kind,
            Properties properties
    ) {
        super(properties);
        this.kind = kind;
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH)
        );
    }

    public NpcMarkerKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    net.minecraft.world.level.block.Block,
                    BlockState
            > builder
    ) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection()
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return OUTLINE;
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (player.getItemInHand(hand).getItem()
                instanceof TownRegisterItem) {
            return InteractionResult.PASS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        var data = BuildingSavedData.get(serverLevel);
        var markerId = NpcMarkerIds.id(
                serverLevel.dimension(),
                pos,
                kind
        );

        NpcMarkerManager.RegistrationResult result =
                data.findBuildingForMarker(markerId) == null
                        ? NpcMarkerManager.register(
                        serverLevel,
                        pos,
                        kind,
                        state.getValue(FACING)
                )
                        : NpcMarkerManager.status(
                        serverLevel,
                        pos,
                        kind
                );

        player.displayClientMessage(
                Component.literal(result.message()),
                true
        );

        return InteractionResult.CONSUME;
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel
                && !oldState.is(this)) {
            NpcMarkerManager.register(
                    serverLevel,
                    pos,
                    kind,
                    state.getValue(FACING)
            );
        }
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel
                && !newState.is(this)) {
            NpcMarkerManager.unregister(
                    serverLevel,
                    pos,
                    kind
            );
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }
}
