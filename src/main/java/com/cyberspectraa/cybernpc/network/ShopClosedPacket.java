package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.NpcShopScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Dismiss a stale merchant window rather than leaving a disabled trade UI. */
public record ShopClosedPacket(String message) {
    public static void encode(ShopClosedPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.message(), 256);
    }

    public static ShopClosedPacket decode(FriendlyByteBuf buf) {
        return new ShopClosedPacket(buf.readUtf(256));
    }

    public static void handle(ShopClosedPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> NpcShopScreen.closeFromServer(p.message())));
        ctx.setPacketHandled(true);
    }
}
