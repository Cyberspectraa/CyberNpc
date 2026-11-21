package com.cyberspectraa.cybernpc.mail;

import com.cyberspectraa.cybernpc.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MailSavedData extends SavedData {
    private static final String DATA_NAME = "cybernpc_mail";

    private final Map<Long, MailRecord> records = new HashMap<>();
    private final Map<UUID, PostalAddress> letterBoxes = new HashMap<>();
    private final Set<PostalAddress> dropBoxes = new HashSet<>();
    // Request identifiers are saved alongside mail, so reprocessing a file
    // after a restart never posts the same Discord message twice.
    private final Map<UUID, Long> discordRequestIds = new HashMap<>();
    private long nextId = 1L;

    public static MailSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        ServerLevel storageLevel = overworld == null ? level : overworld;

        return storageLevel.getDataStorage().computeIfAbsent(
                MailSavedData::load,
                MailSavedData::new,
                DATA_NAME
        );
    }

    public static MailSavedData load(CompoundTag tag) {
        MailSavedData data = new MailSavedData();
        data.nextId = Math.max(1L, tag.getLong("NextId"));

        ListTag recordsTag = tag.getList("Records", Tag.TAG_COMPOUND);
        for (int i = 0; i < recordsTag.size(); i++) {
            MailRecord record = MailRecord.load(recordsTag.getCompound(i));
            if (record == null) {
                continue;
            }
            data.records.put(record.id(), record);
            data.nextId = Math.max(data.nextId, record.id() + 1L);
        }

        ListTag boxesTag = tag.getList("LetterBoxes", Tag.TAG_COMPOUND);
        for (int i = 0; i < boxesTag.size(); i++) {
            CompoundTag row = boxesTag.getCompound(i);
            if (!row.hasUUID("Owner")) {
                continue;
            }

            PostalAddress address = PostalAddress.load(row);
            if (address != null) {
                data.letterBoxes.put(row.getUUID("Owner"), address);
            }
        }

        ListTag dropsTag = tag.getList("DropBoxes", Tag.TAG_COMPOUND);
        for (int i = 0; i < dropsTag.size(); i++) {
            PostalAddress address = PostalAddress.load(dropsTag.getCompound(i));
            if (address != null) {
                data.dropBoxes.add(address);
            }
        }

        ListTag requests = tag.getList("DiscordRequests", Tag.TAG_COMPOUND);
        for (int i = 0; i < requests.size(); i++) {
            CompoundTag row = requests.getCompound(i);
            if (row.hasUUID("RequestId") && row.contains("MailId", Tag.TAG_LONG)) {
                data.discordRequestIds.put(row.getUUID("RequestId"), row.getLong("MailId"));
            }
        }

        // Any courier assignment from before a crash/restart is released.
        // The authoritative record safely returns to its pending pickup.
        for (MailRecord record : data.records.values()) {
            if (record.state() == MailState.IN_TRANSIT) {
                record.setState(MailState.PENDING);
                record.setCourierId(null);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("NextId", nextId);

        ListTag recordsTag = new ListTag();
        records.values().stream()
                .sorted(Comparator.comparingLong(MailRecord::id))
                .forEach(record -> recordsTag.add(record.save()));
        tag.put("Records", recordsTag);

        ListTag boxesTag = new ListTag();
        for (Map.Entry<UUID, PostalAddress> entry : letterBoxes.entrySet()) {
            CompoundTag row = entry.getValue().save();
            row.putUUID("Owner", entry.getKey());
            boxesTag.add(row);
        }
        tag.put("LetterBoxes", boxesTag);

        ListTag dropsTag = new ListTag();
        for (PostalAddress address : dropBoxes) {
            dropsTag.add(address.save());
        }
        tag.put("DropBoxes", dropsTag);

        ListTag requests = new ListTag();
        discordRequestIds.forEach((requestId, mailId) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("RequestId", requestId);
            row.putLong("MailId", mailId);
            requests.add(row);
        });
        tag.put("DiscordRequests", requests);

        return tag;
    }

    /** Check whether a submission was already imported, including after restart. */
    @Nullable
    public Long discordMailId(UUID requestId) {
        return discordRequestIds.get(requestId);
    }

    /**
     * A Discord entry is ordinary postal mail with a stable external request
     * identity. This is server-only; the client never controls these values.
     */
    public long postDiscordOnce(UUID requestId, MailItemData.Draft draft,
                                PostalAddress pickup) {
        if (requestId == null || draft == null || pickup == null) return -1L;
        Long previous = discordRequestIds.get(requestId);
        if (previous != null) return previous;
        long id = post(draft, pickup.dimension(), pickup.pos());
        if (id >= 0L) {
            discordRequestIds.put(requestId, id);
            setDirty();
        }
        return id;
    }

    /**
     * Only queue into a loaded, real Drop Box with room for more mail.
     * Prefer the overworld, then a smaller queue and stable coordinates.
     * If no box is ready, Discord submissions stay on disk until one is.
     */
    @Nullable
    public PostalAddress availableDiscordDropBox(MinecraftServer server) {
        return dropBoxes.stream()
                .filter(address -> {
                    ServerLevel level = server.getLevel(address.dimension());
                    return level != null && level.hasChunkAt(address.pos())
                            && level.getBlockState(address.pos()).is(ModBlocks.DROP_BOX.get())
                            && pendingCountAt(address.dimension(), address.pos()) < 64;
                })
                .min(Comparator
                        .comparingInt((PostalAddress a) -> a.dimension().equals(Level.OVERWORLD) ? 0 : 1)
                        .thenComparingInt(a -> pendingCountAt(a.dimension(), a.pos()))
                        .thenComparing(a -> a.dimension().location().toString())
                        .thenComparingLong(a -> a.pos().asLong()))
                .orElse(null);
    }

    public long post(
            MailItemData.Draft draft,
            ResourceKey<Level> dimension,
            BlockPos pickupPos
    ) {
        if (draft == null || dimension == null || pickupPos == null) {
            return -1L;
        }

        long id = nextId++;
        MailRecord record = new MailRecord(
                id,
                draft.senderId(),
                draft.senderName(),
                draft.recipientId(),
                draft.recipientName(),
                draft.message(),
                MailState.PENDING,
                dimension,
                pickupPos
        );

        records.put(id, record);
        setDirty();
        return id;
    }

    @Nullable
    public MailRecord get(long id) {
        return records.get(id);
    }

    @Nullable
    public MailRecord claimNearestPending(
            ResourceKey<Level> dimension,
            BlockPos origin,
            UUID courierId,
            double maxDistanceSqr
    ) {
        MailRecord best = null;
        double bestDistance = maxDistanceSqr;

        for (MailRecord record : records.values()) {
            if (record.state() != MailState.PENDING
                    || !record.dimension().equals(dimension)
                    || record.pickupPos() == null
                    || !dropBoxes.contains(new PostalAddress(
                    record.dimension(),
                    record.pickupPos()
            ))) {
                continue;
            }

            double distance = record.pickupPos().distSqr(origin);
            if (distance > bestDistance) {
                continue;
            }

            bestDistance = distance;
            best = record;
        }

        if (best != null) {
            best.setState(MailState.IN_TRANSIT);
            best.setCourierId(courierId);
            setDirty();
        }

        return best;
    }

    public List<MailRecord> claimPendingAt(
            ResourceKey<Level> dimension,
            BlockPos pickupPos,
            UUID courierId,
            int max
    ) {
        if (dimension == null
                || pickupPos == null
                || courierId == null
                || max <= 0) {
            return List.of();
        }

        List<MailRecord> claimed = records.values().stream()
                .filter(record ->
                        record.state() == MailState.PENDING
                                && record.dimension().equals(dimension)
                                && pickupPos.equals(record.pickupPos())
                                && dropBoxes.contains(
                                new PostalAddress(
                                        record.dimension(),
                                        record.pickupPos()
                                )
                        )
                )
                .sorted(Comparator.comparingLong(MailRecord::id))
                .limit(max)
                .toList();

        if (claimed.isEmpty()) {
            return List.of();
        }

        for (MailRecord record : claimed) {
            record.setState(MailState.IN_TRANSIT);
            record.setCourierId(courierId);
        }

        setDirty();
        return claimed;
    }

    public void returnToPending(long id, UUID courierId) {
        MailRecord record = records.get(id);
        if (record == null
                || record.state() != MailState.IN_TRANSIT
                || (record.courierId() != null
                && !record.courierId().equals(courierId))) {
            return;
        }

        record.setState(MailState.PENDING);
        record.setCourierId(null);
        setDirty();
    }

    public void releaseCourier(UUID courierId) {
        boolean changed = false;

        for (MailRecord record : records.values()) {
            if (record.state() == MailState.IN_TRANSIT
                    && courierId.equals(record.courierId())) {
                record.setState(MailState.PENDING);
                record.setCourierId(null);
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    public void markBoxed(long id) {
        MailRecord record = records.get(id);
        if (record == null) {
            return;
        }

        record.setState(MailState.BOXED);
        record.setCourierId(null);
        setDirty();
    }

    public void markDelivered(long id) {
        MailRecord record = records.get(id);
        if (record == null) {
            return;
        }

        record.setState(MailState.DELIVERED);
        record.setCourierId(null);
        setDirty();
    }

    public void markRead(long id, UUID reader) {
        MailRecord record = records.get(id);
        if (record == null
                || reader == null
                || !record.recipientId().equals(reader)) {
            return;
        }

        record.setState(MailState.READ);
        record.setCourierId(null);
        setDirty();
    }

    public void registerDropBox(ResourceKey<Level> dimension, BlockPos pos) {
        PostalAddress address = new PostalAddress(dimension, pos);
        if (dropBoxes.add(address)) {
            setDirty();
        }
    }

    public void unregisterDropBox(ResourceKey<Level> dimension, BlockPos pos) {
        PostalAddress address = new PostalAddress(dimension, pos);
        if (dropBoxes.remove(address)) {
            setDirty();
        }
    }

    public boolean isDropBoxRegistered(ResourceKey<Level> dimension, BlockPos pos) {
        return dropBoxes.contains(new PostalAddress(dimension, pos));
    }

    public int pendingCountAt(ResourceKey<Level> dimension, BlockPos pos) {
        int count = 0;

        for (MailRecord record : records.values()) {
            if (record.state() == MailState.PENDING
                    && record.dimension().equals(dimension)
                    && pos.equals(record.pickupPos())) {
                count++;
            }
        }

        return count;
    }

    public List<MailRecord> removePendingAt(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        List<MailRecord> removed = new ArrayList<>();

        for (MailRecord record : new ArrayList<>(records.values())) {
            if (record.state() == MailState.PENDING
                    && record.dimension().equals(dimension)
                    && pos.equals(record.pickupPos())) {
                records.remove(record.id());
                removed.add(record);
            }
        }

        if (!removed.isEmpty()) {
            setDirty();
        }

        return removed;
    }

    public void registerLetterBox(
            UUID owner,
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        if (owner == null || dimension == null || pos == null) {
            return;
        }

        letterBoxes.put(owner, new PostalAddress(dimension, pos));
        setDirty();
    }

    public void unregisterLetterBox(
            UUID owner,
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        PostalAddress existing = letterBoxes.get(owner);
        if (existing != null
                && existing.dimension().equals(dimension)
                && existing.pos().equals(pos)) {
            letterBoxes.remove(owner);
            requeueBoxedMail(owner);
            setDirty();
        }
    }

    public void unregisterLetterBoxAt(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        UUID owner = null;

        for (Map.Entry<UUID, PostalAddress> entry : letterBoxes.entrySet()) {
            PostalAddress address = entry.getValue();
            if (address.dimension().equals(dimension)
                    && address.pos().equals(pos)) {
                owner = entry.getKey();
                break;
            }
        }

        if (owner != null) {
            letterBoxes.remove(owner);
            requeueBoxedMail(owner);
            setDirty();
        }
    }

    @Nullable
    public PostalAddress getLetterBox(UUID owner) {
        return letterBoxes.get(owner);
    }

    public List<MailRecord> boxedMailFor(UUID owner, int max) {
        return records.values().stream()
                .filter(record -> record.state() == MailState.BOXED
                        && record.recipientId().equals(owner))
                .sorted(Comparator.comparingLong(MailRecord::id))
                .limit(Math.max(0, max))
                .toList();
    }

    public int boxedCount(UUID owner) {
        int count = 0;
        for (MailRecord record : records.values()) {
            if (record.state() == MailState.BOXED
                    && record.recipientId().equals(owner)) {
                count++;
            }
        }
        return count;
    }

    private void requeueBoxedMail(UUID owner) {
        for (MailRecord record : records.values()) {
            if (record.state() == MailState.BOXED
                    && record.recipientId().equals(owner)) {
                record.setState(MailState.PENDING);
                record.setCourierId(null);
            }
        }
    }

    public record PostalAddress(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        public PostalAddress {
            pos = pos.immutable();
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.location().toString());
            tag.putLong("Pos", pos.asLong());
            return tag;
        }

        @Nullable
        static PostalAddress load(CompoundTag tag) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(
                    tag.getString("Dimension")
            );
            if (dimensionId == null || !tag.contains("Pos")) {
                return null;
            }

            ResourceKey<Level> dimension = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    dimensionId
            );

            return new PostalAddress(
                    dimension,
                    BlockPos.of(tag.getLong("Pos"))
            );
        }
    }
}
