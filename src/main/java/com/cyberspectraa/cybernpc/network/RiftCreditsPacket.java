package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.RiftCreditsScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Small per-player snapshot; never send persistent NBT or private quest data. */
public record RiftCreditsPacket(int playTicks, int deaths, int mobKills,
                                int conversations, int friends,
                                String favouriteName, int favouriteVisits,
                                String runnerUpName, int runnerUpVisits) {
    public static void encode(RiftCreditsPacket p, FriendlyByteBuf b) {
        b.writeVarInt(p.playTicks());
        b.writeVarInt(p.deaths());
        b.writeVarInt(p.mobKills());
        b.writeVarInt(p.conversations());
        b.writeVarInt(p.friends());
        b.writeUtf(p.favouriteName());
        b.writeVarInt(p.favouriteVisits());
        b.writeUtf(p.runnerUpName());
        b.writeVarInt(p.runnerUpVisits());
    }
    public static RiftCreditsPacket decode(FriendlyByteBuf b) {
        return new RiftCreditsPacket(b.readVarInt(), b.readVarInt(), b.readVarInt(),
                b.readVarInt(), b.readVarInt(), b.readUtf(256), b.readVarInt(),
                b.readUtf(256), b.readVarInt());
    }
    public static void handle(RiftCreditsPacket packet,
                              Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context ctx = source.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> RiftCreditsScreen.open(packet)));
        ctx.setPacketHandled(true);
    }
}
