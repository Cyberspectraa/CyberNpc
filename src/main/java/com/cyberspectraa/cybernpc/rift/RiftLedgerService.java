package com.cyberspectraa.cybernpc.rift;

import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.RiftLedgerPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RiftLedgerService {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'", java.util.Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);
    private RiftLedgerService() {}
    public static void open(ServerPlayer player) {
        if (!player.hasPermissions(2)) return;
        RiftDepartureData data = RiftDepartureData.get(player.serverLevel().getServer());
        List<String> npcs = new ArrayList<>();
        for (String id : RiftDepartureData.STORY_IDS) {
            CompoundTag record = data.npcRecord(id);
            npcs.add(RiftDepartureData.name(id) + " | " + (record.isEmpty()
                    ? "Not departed" : "Departed - " + date(record)));
        }
        List<String> players = data.playerRecords().stream()
                .sorted(Comparator.comparingLong((CompoundTag t) -> t.getLong("Time")).reversed()
                        .thenComparing(t -> t.getString("Name")))
                .limit(2048)
                .map(t -> t.getString("Name") + " | " + date(t)).toList();
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new RiftLedgerPacket(List.copyOf(npcs), players, data.playerCount()));
    }
    private static String date(CompoundTag record) {
        long time = record.getLong("Time");
        return time > 0L ? DATE.format(Instant.ofEpochMilli(time)) : "Earlier departure";
    }
}
