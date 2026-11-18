package com.cyberspectraa.cybernpc.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Overworld-scoped server SavedData. Every player's first Rift discovery is
 * stored once, across logouts, dimensions and respawns. Later quests and NPC
 * escorts can query it without adding new player NBT or duplicating flags.
 */
public final class RiftDiscoverySavedData extends SavedData {
    private static final String DATA_NAME = "cybernpc_rift_discovery";
    private final Map<UUID, Discovery> discovered = new HashMap<>();

    public record Discovery(String dimension, BlockPos position, long gameTime) { }

    public static RiftDiscoverySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                RiftDiscoverySavedData::load,
                RiftDiscoverySavedData::new,
                DATA_NAME
        );
    }

    public static RiftDiscoverySavedData load(CompoundTag tag) {
        RiftDiscoverySavedData data = new RiftDiscoverySavedData();
        ListTag entries = tag.getList("Discoveries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag row = entries.getCompound(i);
            if (!row.hasUUID("Player")) continue;
            UUID playerId = row.getUUID("Player");
            data.discovered.put(playerId, new Discovery(
                    row.getString("Dimension"),
                    BlockPos.of(row.getLong("Position")),
                    row.getLong("GameTime")
            ));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, Discovery> entry : discovered.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("Player", entry.getKey());
            row.putString("Dimension", entry.getValue().dimension());
            row.putLong("Position", entry.getValue().position().asLong());
            row.putLong("GameTime", entry.getValue().gameTime());
            entries.add(row);
        }
        tag.put("Discoveries", entries);
        return tag;
    }

    public static boolean hasDiscovered(ServerPlayer player) {
        return player != null && get(player.serverLevel())
                .discovered.containsKey(player.getUUID());
    }

    public Discovery getDiscovery(UUID playerId) {
        return discovered.get(playerId);
    }

    /**
     * @return true only the first time the specified player discovers any Rift
     */
    public boolean discover(ServerPlayer player, BlockPos pos) {
        if (player == null || discovered.containsKey(player.getUUID())) {
            return false;
        }
        discovered.put(player.getUUID(), new Discovery(
                player.serverLevel().dimension().location().toString(),
                pos.immutable(),
                player.serverLevel().getGameTime()
        ));
        setDirty();
        return true;
    }
}
