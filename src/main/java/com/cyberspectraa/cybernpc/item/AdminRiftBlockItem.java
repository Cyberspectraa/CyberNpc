package com.cyberspectraa.cybernpc.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** Placement requires operator level 2 even when someone is given the item. */
public final class AdminRiftBlockItem extends BlockItem {
    public AdminRiftBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide) {
            if (!(context.getPlayer() instanceof ServerPlayer admin)
                    || !admin.hasPermissions(2)) {
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            Component.literal("Only an operator can place an Island Rift."), true);
                }
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }
}
