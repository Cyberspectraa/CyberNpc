package com.cyberspectraa.cybernpc.rift;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-wide departure index. Private entity snapshots stay on the server. */
public final class RiftDepartureData extends SavedData {
    public static final List<String> STORY_IDS = List.of("mason", "town_npc_1", "town_npc_2",
            "town_npc_3", "town_npc_4", "town_npc_5", "town_npc_6");
    private static final List<String> NAMES = List.of("Kai", "Katie", "Elara", "Rowan", "Felix", "Nadia", "Orin");
    private final Map<String, CompoundTag> npcs = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> players = new LinkedHashMap<>();

    public static String name(String id) {
        int index = STORY_IDS.indexOf(id);
        return index < 0 ? id : NAMES.get(index);
    }
    public static String storyId(String value) {
        for (String id : STORY_IDS) {
            if (id.equalsIgnoreCase(value) || name(id).equalsIgnoreCase(value)) return id;
        }
        return "";
    }
    public static RiftDepartureData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RiftDepartureData::load, RiftDepartureData::new, "cybernpc_rift_departures");
    }
    public static RiftDepartureData load(CompoundTag tag) {
        RiftDepartureData data = new RiftDepartureData();
        ListTag npcList = tag.getList("Npcs", Tag.TAG_COMPOUND);
        for (int i = 0; i < npcList.size(); i++) {
            CompoundTag entry = npcList.getCompound(i);
            String id = entry.getString("StoryId");
            if (STORY_IDS.contains(id) && entry.hasUUID("UUID")
                    && entry.contains("Entity", Tag.TAG_COMPOUND)) data.npcs.putIfAbsent(id, entry.copy());
        }
        ListTag playerList = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag entry = playerList.getCompound(i);
            if (entry.hasUUID("UUID")) data.players.put(entry.getUUID("UUID"), entry.copy());
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag npcList = new ListTag();
        npcs.values().forEach(entry -> npcList.add(entry.copy()));
        ListTag playerList = new ListTag();
        players.values().forEach(entry -> playerList.add(entry.copy()));
        tag.put("Npcs", npcList);
        tag.put("Players", playerList);
        return tag;
    }
    public boolean departed(String id) { return npcs.containsKey(id); }
    public CompoundTag npcRecord(String id) {
        CompoundTag entry = npcs.get(id);
        return entry == null ? new CompoundTag() : entry.copy();
    }
    public boolean recordNpc(CyberNpcEntity npc, CompoundTag snapshot) {
        String id = npc.getStoryNpcId();
        if (!STORY_IDS.contains(id) || departed(id)) return false;
        CompoundTag entry = new CompoundTag();
        entry.putString("StoryId", id);
        entry.putUUID("UUID", npc.getUUID());
        entry.putString("Name", npc.getName().getString());
        entry.putLong("Time", System.currentTimeMillis());
        entry.putLong("GameTime", npc.level().getGameTime());
        entry.putString("Dimension", npc.level().dimension().location().toString());
        entry.put("Entity", snapshot.copy());
        npcs.put(id, entry);
        setDirty();
        return true;
    }
    public void removeNpc(String id) {
        if (npcs.remove(id) != null) setDirty();
    }
    public void recordPlayer(ServerPlayer player, CompoundTag state) {
        if (!state.getBoolean("Departed")) return;
        UUID id = player.getUUID();
        String name = player.getGameProfile().getName();
        CompoundTag old = players.get(id);
        if (old != null && old.getString("Name").equals(name)
                && old.getLong("GameTime") == state.getLong("CompletedAtGameTime")
                && old.getLong("Time") == state.getLong("CompletedAtMillis")) return;
        CompoundTag entry = new CompoundTag();
        entry.putUUID("UUID", id);
        entry.putString("Name", name);
        entry.putLong("GameTime", state.getLong("CompletedAtGameTime"));
        entry.putLong("Time", state.getLong("CompletedAtMillis"));
        entry.putString("Dimension", state.getString("Dimension"));
        players.put(id, entry);
        setDirty();
    }
    public void removePlayer(UUID id) {
        if (players.remove(id) != null) setDirty();
    }
    public List<CompoundTag> playerRecords() {
        return players.values().stream().map(CompoundTag::copy).toList();
    }
    public int playerCount() { return players.size(); }
}
