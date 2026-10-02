package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class GuardPostMarkerItem extends BlockItem {
    public GuardPostMarkerItem(Properties properties) {
        super(ModBlocks.GUARD_POST.get(), properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();

        if (player != null && player.isShiftKeyDown()) {
            BlockPos clicked = context.getClickedPos();
            BlockPos[] candidates = new BlockPos[]{
                    clicked,
                    clicked.above(),
                    clicked.relative(context.getClickedFace())
            };

            for (BlockPos pos : candidates) {
                if (level.getBlockState(pos).is(ModBlocks.GUARD_POST.get())) {
                    if (!level.isClientSide) {
                        level.destroyBlock(pos, false, player);
                        player.displayClientMessage(
                                Component.literal("Guard Post removed."),
                                true
                        );
                    }
                    return InteractionResult.sidedSuccess(
                            level.isClientSide
                    );
                }
            }
        }

        InteractionResult result = super.useOn(context);

        if (result.consumesAction()
                && player != null
                && !level.isClientSide) {
            player.displayClientMessage(
                    Component.literal(
                            "Guard Post placed. Guards can rotate onto it."
                    ),
                    true
            );
        }

        return result;
    }
}
