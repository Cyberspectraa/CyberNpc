package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.mail.MailItemData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AddressedLetterItem extends Item {
    public AddressedLetterItem(Properties properties) {
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
            MailItemData.Draft draft = MailItemData.readDraft(stack);
            if (draft == null) {
                player.sendSystemMessage(Component.literal(
                        "This addressed letter has no valid address."
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                        "Addressed to " + draft.recipientName()
                                + ". Post it in a Drop Box."
                ));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
