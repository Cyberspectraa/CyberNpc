package com.cyberspectraa.cybernpc.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

abstract class PostalBlockBase extends HorizontalDirectionalBlock {
    enum Kind {
        DROP_BOX,
        LETTER_BOX
    }

    private static final VoxelShape DROP_NORTH = Shapes.or(
            Block.box(2, 0, 2, 14, 14, 14),
            Block.box(1, 14, 1, 15, 16, 15),
            Block.box(3, 8, 1, 13, 12, 2)
    );
    private static final VoxelShape DROP_SOUTH = Shapes.or(
            Block.box(2, 0, 2, 14, 14, 14),
            Block.box(1, 14, 1, 15, 16, 15),
            Block.box(3, 8, 14, 13, 12, 15)
    );
    private static final VoxelShape DROP_WEST = Shapes.or(
            Block.box(2, 0, 2, 14, 14, 14),
            Block.box(1, 14, 1, 15, 16, 15),
            Block.box(1, 8, 3, 2, 12, 13)
    );
    private static final VoxelShape DROP_EAST = Shapes.or(
            Block.box(2, 0, 2, 14, 14, 14),
            Block.box(1, 14, 1, 15, 16, 15),
            Block.box(14, 8, 3, 15, 12, 13)
    );

    private static final VoxelShape LETTER_NORTH = Shapes.or(
            Block.box(2, 0, 3, 14, 2, 13),
            Block.box(1, 2, 2, 15, 13, 14),
            Block.box(0, 13, 1, 16, 15, 15),
            Block.box(3, 5, 1, 13, 12, 2)
    );
    private static final VoxelShape LETTER_SOUTH = Shapes.or(
            Block.box(2, 0, 3, 14, 2, 13),
            Block.box(1, 2, 2, 15, 13, 14),
            Block.box(0, 13, 1, 16, 15, 15),
            Block.box(3, 5, 14, 13, 12, 15)
    );
    private static final VoxelShape LETTER_WEST = Shapes.or(
            Block.box(3, 0, 2, 13, 2, 14),
            Block.box(2, 2, 1, 14, 13, 15),
            Block.box(1, 13, 0, 15, 15, 16),
            Block.box(1, 5, 3, 2, 12, 13)
    );
    private static final VoxelShape LETTER_EAST = Shapes.or(
            Block.box(3, 0, 2, 13, 2, 14),
            Block.box(2, 2, 1, 14, 13, 15),
            Block.box(1, 13, 0, 15, 15, 16),
            Block.box(14, 5, 3, 15, 12, 13)
    );

    private final Kind kind;

    protected PostalBlockBase(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeFor(state);
    }

    private VoxelShape shapeFor(BlockState state) {
        Direction direction = state.getValue(FACING);

        if (kind == Kind.DROP_BOX) {
            return switch (direction) {
                case SOUTH -> DROP_SOUTH;
                case WEST -> DROP_WEST;
                case EAST -> DROP_EAST;
                default -> DROP_NORTH;
            };
        }

        return switch (direction) {
            case SOUTH -> LETTER_SOUTH;
            case WEST -> LETTER_WEST;
            case EAST -> LETTER_EAST;
            default -> LETTER_NORTH;
        };
    }
}
