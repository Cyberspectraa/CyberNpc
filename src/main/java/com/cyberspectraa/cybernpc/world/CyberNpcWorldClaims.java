package com.cyberspectraa.cybernpc.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CyberNpcWorldClaims extends SavedData {
    private static final String DATA_NAME = "cybernpc_world_claims";

    private final Map<Long, List<UUID>> cookingClaims = new HashMap<>();
    private final Map<Long, UUID> penClaims = new HashMap<>();

    public static CyberNpcWorldClaims get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                CyberNpcWorldClaims::load,
                CyberNpcWorldClaims::new,
                DATA_NAME
        );
    }

    public static CyberNpcWorldClaims load(CompoundTag tag) {
        CyberNpcWorldClaims data = new CyberNpcWorldClaims();

        ListTag cooking = tag.getList("CookingClaims", Tag.TAG_COMPOUND);
        for (int i = 0; i < cooking.size(); i++) {
            CompoundTag entry = cooking.getCompound(i);
            long pos = entry.getLong("Pos");
            ListTag ownersTag = entry.getList("Owners", Tag.TAG_COMPOUND);
            List<UUID> owners = new ArrayList<>();

            for (int j = 0; j < ownersTag.size(); j++) {
                CompoundTag ownerTag = ownersTag.getCompound(j);
                if (ownerTag.hasUUID("Id")) {
                    UUID owner = ownerTag.getUUID("Id");
                    if (!owners.contains(owner)) {
                        owners.add(owner);
                    }
                }
            }

            if (!owners.isEmpty()) {
                data.cookingClaims.put(pos, owners);
            }
        }

        ListTag pens = tag.getList("PenClaims", Tag.TAG_COMPOUND);
        for (int i = 0; i < pens.size(); i++) {
            CompoundTag entry = pens.getCompound(i);
            if (entry.hasUUID("Owner")) {
                data.penClaims.put(entry.getLong("Pos"), entry.getUUID("Owner"));
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag cooking = new ListTag();

        for (Map.Entry<Long, List<UUID>> entry : cookingClaims.entrySet()) {
            CompoundTag claim = new CompoundTag();
            claim.putLong("Pos", entry.getKey());

            ListTag owners = new ListTag();
            for (UUID owner : entry.getValue()) {
                CompoundTag ownerTag = new CompoundTag();
                ownerTag.putUUID("Id", owner);
                owners.add(ownerTag);
            }

            claim.put("Owners", owners);
            cooking.add(claim);
        }

        tag.put("CookingClaims", cooking);

        ListTag pens = new ListTag();
        for (Map.Entry<Long, UUID> entry : penClaims.entrySet()) {
            CompoundTag claim = new CompoundTag();
            claim.putLong("Pos", entry.getKey());
            claim.putUUID("Owner", entry.getValue());
            pens.add(claim);
        }

        tag.put("PenClaims", pens);
        return tag;
    }

    public boolean canClaimCooking(BlockPos pos, UUID owner, int capacity) {
        if (capacity <= 0) {
            return false;
        }

        List<UUID> owners = cookingClaims.get(pos.asLong());
        return owners == null || owners.contains(owner) || owners.size() < capacity;
    }

    public boolean claimCooking(BlockPos pos, UUID owner, int capacity) {
        if (capacity <= 0) {
            return false;
        }

        long key = pos.asLong();
        List<UUID> owners = cookingClaims.computeIfAbsent(key, ignored -> new ArrayList<>());

        if (owners.contains(owner)) {
            return true;
        }

        if (owners.size() >= capacity) {
            return false;
        }

        owners.add(owner);
        setDirty();
        return true;
    }

    public void releaseCooking(BlockPos pos, UUID owner) {
        long key = pos.asLong();
        List<UUID> owners = cookingClaims.get(key);

        if (owners == null || !owners.remove(owner)) {
            return;
        }

        if (owners.isEmpty()) {
            cookingClaims.remove(key);
        }

        setDirty();
    }

    public boolean ownsCooking(BlockPos pos, UUID owner) {
        List<UUID> owners = cookingClaims.get(pos.asLong());
        return owners != null && owners.contains(owner);
    }

    public int cookingClaimCount(BlockPos pos) {
        List<UUID> owners = cookingClaims.get(pos.asLong());
        return owners == null ? 0 : owners.size();
    }

    public boolean claimPen(BlockPos anchor, UUID owner) {
        long key = anchor.asLong();
        UUID existing = penClaims.get(key);

        if (existing != null) {
            return existing.equals(owner);
        }

        penClaims.put(key, owner);
        setDirty();
        return true;
    }

    public void releasePen(BlockPos anchor, UUID owner) {
        long key = anchor.asLong();
        UUID existing = penClaims.get(key);

        if (owner.equals(existing)) {
            penClaims.remove(key);
            setDirty();
        }
    }

    public boolean ownsPen(BlockPos anchor, UUID owner) {
        return owner.equals(penClaims.get(anchor.asLong()));
    }

    @Nullable
    public UUID getPenOwner(BlockPos anchor) {
        return penClaims.get(anchor.asLong());
    }

    public void releaseOwner(UUID owner) {
        boolean changed = false;

        for (Long key : new ArrayList<>(cookingClaims.keySet())) {
            List<UUID> owners = cookingClaims.get(key);
            if (owners != null && owners.remove(owner)) {
                changed = true;
                if (owners.isEmpty()) {
                    cookingClaims.remove(key);
                }
            }
        }

        List<Long> ownedPens = penClaims.entrySet().stream()
                .filter(entry -> owner.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        for (Long key : ownedPens) {
            penClaims.remove(key);
            changed = true;
        }

        if (changed) {
            setDirty();
        }
    }
}
