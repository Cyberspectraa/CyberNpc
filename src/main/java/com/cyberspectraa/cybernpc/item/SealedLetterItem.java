package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.mail.MailItemData;
import com.cyberspectraa.cybernpc.mail.MailRecord;
import com.cyberspectraa.cybernpc.mail.MailSavedData;
import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SealedLetterItem extends Item {
    public SealedLetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        long recordId = MailItemData.getRecordId(stack);
        MailRecord record = MailSavedData.get(serverLevel).get(recordId);

        if (record == null) {
            player.sendSystemMessage(Component.literal(
                    "This sealed letter is no longer registered with the post office."
            ));
            return InteractionResultHolder.success(stack);
        }

        if (!record.recipientId().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal(
                    "This sealed letter is addressed to " + record.recipientName() + "."
            ));
            return InteractionResultHolder.success(stack);
        }

        player.sendSystemMessage(Component.literal(
                "Letter from " + record.senderName() + ":"
        ));
        player.sendSystemMessage(Component.literal(record.message()));

        MailSavedData.get(serverLevel).markRead(recordId, player.getUUID());

        ItemStack opened = new ItemStack(ModItems.OPENED_LETTER.get());
        MailItemData.writeOpenedCopy(
                opened,
                record.senderName(),
                record.recipientName(),
                record.message()
        );
        player.setItemInHand(hand, opened);

        return InteractionResultHolder.success(opened);
    }
}
