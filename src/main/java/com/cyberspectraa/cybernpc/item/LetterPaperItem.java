package com.cyberspectraa.cybernpc.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class LetterPaperItem extends Item {
    public LetterPaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.sendSystemMessage(Component.literal(
                    "Use /mail write <player> <message> while carrying Letter Paper."
            ));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
