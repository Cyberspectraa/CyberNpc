package com.cyberspectraa.cybernpc.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BankSavedData extends SavedData {
    private static final String DATA_NAME = "cybernpc_bank";
    private final Map<UUID, Long> balances = new HashMap<>();

    public static BankSavedData get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        ServerLevel storageLevel = overworld == null ? level : overworld;

        return storageLevel.getDataStorage().computeIfAbsent(
                BankSavedData::load,
                BankSavedData::new,
                DATA_NAME
        );
    }

    public static BankSavedData load(CompoundTag tag) {
        BankSavedData data = new BankSavedData();
        ListTag entries = tag.getList("Balances", Tag.TAG_COMPOUND);

        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (!entry.hasUUID("Player")) {
                continue;
            }

            long value = Math.max(0L, entry.getLong("Balance"));
            if (value > 0L) {
                data.balances.put(entry.getUUID("Player"), value);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();

        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            if (entry.getValue() <= 0L) {
                continue;
            }

            CompoundTag row = new CompoundTag();
            row.putUUID("Player", entry.getKey());
            row.putLong("Balance", entry.getValue());
            entries.add(row);
        }

        tag.put("Balances", entries);
        return tag;
    }

    public long getBalance(UUID playerId) {
        return Math.max(0L, balances.getOrDefault(playerId, 0L));
    }

    public long deposit(UUID playerId, long amount) {
        if (playerId == null || amount <= 0L) {
            return getBalance(playerId);
        }

        long current = getBalance(playerId);
        long next;

        try {
            next = Math.addExact(current, amount);
        } catch (ArithmeticException ignored) {
            next = Long.MAX_VALUE;
        }

        balances.put(playerId, next);
        setDirty();
        return next;
    }

    public void setBalance(UUID playerId, long amount) {
        if (playerId == null) {
            return;
        }

        long safe = Math.max(0L, amount);
        if (safe == 0L) {
            balances.remove(playerId);
        } else {
            balances.put(playerId, safe);
        }
        setDirty();
    }
}
