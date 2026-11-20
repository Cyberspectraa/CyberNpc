package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.RiftDecisionScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server offers the irreversible departure; touching the Rift never accepts it. */
public record RiftPromptPacket() {
    public static void encode(RiftPromptPacket packet, FriendlyByteBuf buf) {}
    public static RiftPromptPacket decode(FriendlyByteBuf buf) {
        return new RiftPromptPacket();
    }
    public static void handle(RiftPromptPacket packet, Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context ctx = source.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> RiftDecisionScreen::open));
        ctx.setPacketHandled(true);
    }
}
