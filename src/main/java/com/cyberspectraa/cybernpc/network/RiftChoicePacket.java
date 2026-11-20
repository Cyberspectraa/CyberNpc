package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.rift.RiftFinaleService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The server verifies a live, local offer before honouring Yes. */
public record RiftChoicePacket(boolean enter) {
    public static void encode(RiftChoicePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.enter());
    }
    public static RiftChoicePacket decode(FriendlyByteBuf buf) {
        return new RiftChoicePacket(buf.readBoolean());
    }
    public static void handle(RiftChoicePacket packet, Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context ctx = source.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) RiftFinaleService.choose(player, packet.enter());
        });
        ctx.setPacketHandled(true);
    }
}
