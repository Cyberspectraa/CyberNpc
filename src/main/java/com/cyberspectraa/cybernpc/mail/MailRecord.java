package com.cyberspectraa.cybernpc.mail;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

public final class MailRecord {
    private final long id;
    private final UUID senderId;
    private final String senderName;
    private final UUID recipientId;
    private final String recipientName;
    private final String message;

    private MailState state;
    private ResourceKey<Level> dimension;

    @Nullable
    private BlockPos pickupPos;

    @Nullable
    private UUID courierId;

    public MailRecord(
            long id,
            UUID senderId,
            String senderName,
            UUID recipientId,
            String recipientName,
            String message,
            MailState state,
            ResourceKey<Level> dimension,
            @Nullable BlockPos pickupPos
    ) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName == null ? "Unknown" : senderName;
        this.recipientId = recipientId;
        this.recipientName = recipientName == null ? "Unknown" : recipientName;
        this.message = message == null ? "" : message;
        this.state = state == null ? MailState.PENDING : state;
        this.dimension = dimension == null ? Level.OVERWORLD : dimension;
        this.pickupPos = pickupPos == null ? null : pickupPos.immutable();
    }

    public long id() {
        return id;
    }

    public UUID senderId() {
        return senderId;
    }

    public String senderName() {
        return senderName;
    }

    public UUID recipientId() {
        return recipientId;
    }

    public String recipientName() {
        return recipientName;
    }

    public String message() {
        return message;
    }

    public MailState state() {
        return state;
    }

    public void setState(MailState state) {
        this.state = state == null ? MailState.PENDING : state;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public void setDimension(ResourceKey<Level> dimension) {
        if (dimension != null) {
            this.dimension = dimension;
        }
    }

    @Nullable
    public BlockPos pickupPos() {
        return pickupPos;
    }

    public void setPickupPos(@Nullable BlockPos pickupPos) {
        this.pickupPos = pickupPos == null ? null : pickupPos.immutable();
    }

    @Nullable
    public UUID courierId() {
        return courierId;
    }

    public void setCourierId(@Nullable UUID courierId) {
        this.courierId = courierId;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Id", id);
        tag.putUUID("Sender", senderId);
        tag.putString("SenderName", senderName);
        tag.putUUID("Recipient", recipientId);
        tag.putString("RecipientName", recipientName);
        tag.putString("Message", message);
        tag.putInt("State", state.id());
        tag.putString("Dimension", dimension.location().toString());

        if (pickupPos != null) {
            tag.putLong("PickupPos", pickupPos.asLong());
        }
        if (courierId != null) {
            tag.putUUID("Courier", courierId);
        }

        return tag;
    }

    @Nullable
    public static MailRecord load(CompoundTag tag) {
        if (!tag.contains("Id")
                || !tag.hasUUID("Sender")
                || !tag.hasUUID("Recipient")) {
            return null;
        }

        ResourceLocation dimensionId = ResourceLocation.tryParse(
                tag.getString("Dimension")
        );

        ResourceKey<Level> dimension = dimensionId == null
                ? Level.OVERWORLD
                : ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        dimensionId
                );

        MailRecord record = new MailRecord(
                tag.getLong("Id"),
                tag.getUUID("Sender"),
                tag.getString("SenderName"),
                tag.getUUID("Recipient"),
                tag.getString("RecipientName"),
                tag.getString("Message"),
                MailState.fromId(tag.getInt("State")),
                dimension,
                tag.contains("PickupPos")
                        ? BlockPos.of(tag.getLong("PickupPos"))
                        : null
        );

        if (tag.hasUUID("Courier")) {
            record.setCourierId(tag.getUUID("Courier"));
        }

        return record;
    }
}
