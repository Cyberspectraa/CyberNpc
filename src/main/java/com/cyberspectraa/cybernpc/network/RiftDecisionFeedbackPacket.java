package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.RiftDecisionScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The server explicitly reports a rejected entry rather than leaving a stalled prompt. */
public record RiftDecisionFeedbackPacket(String reason) {
    public static void encode(RiftDecisionFeedbackPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.reason(), 256);
    }

    public static RiftDecisionFeedbackPacket decode(FriendlyByteBuf b) {
        return new RiftDecisionFeedbackPacket(b.readUtf(256));
    }

    public static void handle(RiftDecisionFeedbackPacket p,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> RiftDecisionScreen.rejected(p.reason())));
        ctx.setPacketHandled(true);
    }
}
