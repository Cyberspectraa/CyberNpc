package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.RiftLedgerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Names and dates only; no private entity snapshots or player NBT. */
public record RiftLedgerPacket(List<String> npcs, List<String> players, int totalPlayers) {
    public static void encode(RiftLedgerPacket packet, FriendlyByteBuf buf) {
        write(buf, packet.npcs());
        write(buf, packet.players());
        buf.writeVarInt(packet.totalPlayers());
    }
    private static void write(FriendlyByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());
        for (String line : lines) buf.writeUtf(line, 256);
    }
    private static List<String> read(FriendlyByteBuf buf, int max) {
        int count = buf.readVarInt();
        if (count < 0 || count > max) throw new IllegalArgumentException("Invalid ledger length");
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) lines.add(buf.readUtf(256));
        return List.copyOf(lines);
    }
    public static RiftLedgerPacket decode(FriendlyByteBuf buf) {
        return new RiftLedgerPacket(read(buf, 7), read(buf, 2048), buf.readVarInt());
    }
    public static void handle(RiftLedgerPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RiftLedgerScreen.open(packet)));
        ctx.setPacketHandled(true);
    }
}
