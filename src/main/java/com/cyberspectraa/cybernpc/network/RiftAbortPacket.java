package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.RiftCreditsScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Closes the unskippable credits only when the server explicitly resets them. */
public record RiftAbortPacket() {
    public static void encode(RiftAbortPacket p, FriendlyByteBuf b) {}
    public static RiftAbortPacket decode(FriendlyByteBuf b) {
        return new RiftAbortPacket();
    }
    public static void handle(RiftAbortPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> RiftCreditsScreen.closeFromServer()));
        context.setPacketHandled(true);
    }
}
