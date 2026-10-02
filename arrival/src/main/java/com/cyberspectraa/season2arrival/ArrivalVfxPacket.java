package com.cyberspectraa.season2arrival;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ArrivalVfxPacket(double x, double y, double z, int maxBuildY) {
    public static void encode(ArrivalVfxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.maxBuildY);
    }

    public static ArrivalVfxPacket decode(FriendlyByteBuf buffer) {
        return new ArrivalVfxPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt()
        );
    }

    public static void handle(ArrivalVfxPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ArrivalClientVfx.handle(packet)
        ));
        context.setPacketHandled(true);
    }
}
