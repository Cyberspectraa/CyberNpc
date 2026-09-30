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
import java.util.Set;
import java.util.UUID;

public final class CyberNpcWorldClaims extends SavedData {
    private static final String DATA_NAME = "cybernpc_world_claims";

    private final Map<Long, List<UUID>> cookingClaims = new HashMap<>();
    private final Map<Long, UUID> penClaims = new HashMap<>();
    private final Map<Long, PenCellClaim> penCellClaims = new HashMap<>();

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

        ListTag cells = tag.getList("PenCellClaims", Tag.TAG_COMPOUND);
        for (int i = 0; i < cells.size(); i++) {
            CompoundTag entry = cells.getCompound(i);
            if (entry.hasUUID("Owner")) {
                long pos = entry.getLong("Pos");
                long anchor = entry.contains("Anchor") ? entry.getLong("Anchor") : pos;
                data.penCellClaims.put(pos, new PenCellClaim(entry.getUUID("Owner"), anchor));
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

        ListTag cells = new ListTag();
        for (Map.Entry<Long, PenCellClaim> entry : penCellClaims.entrySet()) {
            CompoundTag claim = new CompoundTag();
            claim.putLong("Pos", entry.getKey());
            claim.putUUID("Owner", entry.getValue().owner());
            claim.putLong("Anchor", entry.getValue().anchor());
            cells.add(claim);
        }

        tag.put("PenCellClaims", cells);
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

    /**
     * Claims the pen anchor and every currently detected walkable cell atomically.
     * If any part of the enclosure belongs to a different NPC, the whole claim fails.
     */
    public boolean claimPen(BlockPos anchor, Set<BlockPos> cells, UUID owner) {
        long anchorKey = anchor.asLong();
        UUID existingAnchorOwner = penClaims.get(anchorKey);

        if (existingAnchorOwner != null && !existingAnchorOwner.equals(owner)) {
            return false;
        }

        for (BlockPos cell : cells) {
            PenCellClaim existing = penCellClaims.get(cell.asLong());
            if (existing != null && !existing.owner().equals(owner)) {
                return false;
            }
        }

        boolean changed = !owner.equals(existingAnchorOwner);
        penClaims.put(anchorKey, owner);

        for (BlockPos cell : cells) {
            long cellKey = cell.asLong();
            PenCellClaim previous = penCellClaims.put(cellKey, new PenCellClaim(owner, anchorKey));
            if (previous == null
                    || !previous.owner().equals(owner)
                    || previous.anchor() != anchorKey) {
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }

        return true;
    }

    public boolean claimPen(BlockPos anchor, UUID owner) {
        return claimPen(anchor, Set.of(), owner);
    }

    public void releasePen(BlockPos anchor, UUID owner) {
        long anchorKey = anchor.asLong();
        UUID existing = penClaims.get(anchorKey);
        boolean changed = false;

        if (owner.equals(existing)) {
            penClaims.remove(anchorKey);
            changed = true;
        }

        for (Long cellKey : new ArrayList<>(penCellClaims.keySet())) {
            PenCellClaim claim = penCellClaims.get(cellKey);
            if (claim != null && claim.anchor() == anchorKey && claim.owner().equals(owner)) {
                penCellClaims.remove(cellKey);
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    public boolean ownsPen(BlockPos anchor, UUID owner) {
        return owner.equals(penClaims.get(anchor.asLong()));
    }

    public boolean ownsPenCell(BlockPos pos, UUID owner) {
        PenCellClaim claim = penCellClaims.get(pos.asLong());
        return claim != null && claim.owner().equals(owner);
    }

    public int penClaimedCellCount(BlockPos anchor) {
        long anchorKey = anchor.asLong();
        int count = 0;

        for (PenCellClaim claim : penCellClaims.values()) {
            if (claim.anchor() == anchorKey) {
                count++;
            }
        }

        return count;
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

        for (Long key : new ArrayList<>(penClaims.keySet())) {
            if (owner.equals(penClaims.get(key))) {
                penClaims.remove(key);
                changed = true;
            }
        }

        for (Long key : new ArrayList<>(penCellClaims.keySet())) {
            PenCellClaim claim = penCellClaims.get(key);
            if (claim != null && owner.equals(claim.owner())) {
                penCellClaims.remove(key);
                changed = true;
            }
        }

        if (changed) {
            setDirty();
        }
    }

    private record PenCellClaim(UUID owner, long anchor) {
    }
}
