package com.cyberspectraa.cybernpc.mail;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

public final class MailItemData {
    private static final String ROOT = "CyberNpcMail";

    public static void writeDraft(
            ItemStack stack,
            UUID sender,
            String senderName,
            UUID recipient,
            String recipientName,
            String message
    ) {
        CompoundTag tag = stack.getOrCreateTagElement(ROOT);
        tag.putUUID("Sender", sender);
        tag.putString("SenderName", senderName);
        tag.putUUID("Recipient", recipient);
        tag.putString("RecipientName", recipientName);
        tag.putString("Message", message);
        tag.remove("RecordId");
    }

    public static void writeRecordId(ItemStack stack, long id) {
        stack.getOrCreateTagElement(ROOT).putLong("RecordId", id);
    }

    public static long getRecordId(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT);
        return tag == null ? -1L : tag.getLong("RecordId");
    }

    @Nullable
    public static Draft readDraft(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT);
        if (tag == null
                || !tag.hasUUID("Sender")
                || !tag.hasUUID("Recipient")) {
            return null;
        }

        return new Draft(
                tag.getUUID("Sender"),
                tag.getString("SenderName"),
                tag.getUUID("Recipient"),
                tag.getString("RecipientName"),
                tag.getString("Message")
        );
    }

    public static void writeOpenedCopy(
            ItemStack stack,
            String senderName,
            String recipientName,
            String message
    ) {
        CompoundTag tag = stack.getOrCreateTagElement(ROOT);
        tag.putString("SenderName", senderName);
        tag.putString("RecipientName", recipientName);
        tag.putString("Message", message);
    }

    @Nullable
    public static OpenedCopy readOpenedCopy(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT);
        if (tag == null || !tag.contains("Message")) {
            return null;
        }

        return new OpenedCopy(
                tag.getString("SenderName"),
                tag.getString("RecipientName"),
                tag.getString("Message")
        );
    }

    public record Draft(
            UUID senderId,
            String senderName,
            UUID recipientId,
            String recipientName,
            String message
    ) {
    }

    public record OpenedCopy(
            String senderName,
            String recipientName,
            String message
    ) {
    }

    private MailItemData() {
    }
}
