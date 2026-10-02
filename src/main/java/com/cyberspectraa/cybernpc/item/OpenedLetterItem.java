package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.mail.MailItemData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class OpenedLetterItem extends Item {
    public OpenedLetterItem(Properties properties) {
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
            MailItemData.OpenedCopy copy = MailItemData.readOpenedCopy(stack);

            if (copy == null) {
                player.sendSystemMessage(Component.literal("The letter is blank."));
            } else {
                player.sendSystemMessage(Component.literal(
                        "Letter from " + copy.senderName()
                                + " to " + copy.recipientName() + ":"
                ));
                player.sendSystemMessage(Component.literal(copy.message()));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
