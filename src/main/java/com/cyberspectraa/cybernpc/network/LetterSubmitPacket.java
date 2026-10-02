package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.mail.MailItemData;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record LetterSubmitPacket(
        String recipientName,
        String message,
        boolean offHand
) {
    private static final int MAX_RECIPIENT = 32;
    private static final int MAX_MESSAGE = 256;

    public static void encode(
            LetterSubmitPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeUtf(packet.recipientName, MAX_RECIPIENT);
        buffer.writeUtf(packet.message, MAX_MESSAGE);
        buffer.writeBoolean(packet.offHand);
    }

    public static LetterSubmitPacket decode(FriendlyByteBuf buffer) {
        return new LetterSubmitPacket(
                buffer.readUtf(MAX_RECIPIENT),
                buffer.readUtf(MAX_MESSAGE),
                buffer.readBoolean()
        );
    }

    public static void handle(
            LetterSubmitPacket packet,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();

        if (sender == null) {
            return;
        }

        InteractionHand hand = packet.offHand
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        ItemStack paper = sender.getItemInHand(hand);

        if (!paper.is(ModItems.LETTER_PAPER.get())) {
            sender.sendSystemMessage(Component.literal(
                    "You need to still be holding Letter Paper."
            ));
            return;
        }

        String recipientName = packet.recipientName == null
                ? ""
                : packet.recipientName.trim();
        String message = packet.message == null
                ? ""
                : packet.message.trim();

        if (recipientName.isEmpty()) {
            sender.sendSystemMessage(Component.literal(
                    "Choose who the letter is addressed to."
            ));
            return;
        }

        if (message.isEmpty()) {
            sender.sendSystemMessage(Component.literal(
                    "The letter cannot be empty."
            ));
            return;
        }

        if (message.length() > MAX_MESSAGE) {
            sender.sendSystemMessage(Component.literal(
                    "Letters can be at most 256 characters."
            ));
            return;
        }

        Optional<GameProfile> profile = sender.getServer()
                .getProfileCache()
                .get(recipientName);

        if (profile.isEmpty()) {
            sender.sendSystemMessage(Component.literal(
                    "No known player named " + recipientName + "."
            ));
            return;
        }

        GameProfile recipient = profile.get();
        ItemStack addressed = new ItemStack(
                ModItems.ADDRESSED_LETTER.get()
        );

        MailItemData.writeDraft(
                addressed,
                sender.getUUID(),
                sender.getGameProfile().getName(),
                recipient.getId(),
                recipient.getName(),
                message
        );

        if (!sender.getAbilities().instabuild) {
            paper.shrink(1);
        }

        if (!sender.getInventory().add(addressed)) {
            sender.drop(addressed, false);
        }

        sender.getInventory().setChanged();
        sender.sendSystemMessage(Component.literal(
                "Sealed a letter to " + recipient.getName()
                        + ". Put it in a Drop Box."
        ));
    }
}
