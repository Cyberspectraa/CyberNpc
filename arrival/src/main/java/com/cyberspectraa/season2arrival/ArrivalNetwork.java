package com.cyberspectraa.season2arrival;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ArrivalNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Season2Arrival.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int nextId = 0;

    private ArrivalNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextId++,
                ArrivalVfxPacket.class,
                ArrivalVfxPacket::encode,
                ArrivalVfxPacket::decode,
                ArrivalVfxPacket::handle
        );
    }

    public static void sendArrival(ServerLevel level, double x, double y, double z) {
        ArrivalVfxPacket packet = new ArrivalVfxPacket(x, y, z, level.getMaxBuildHeight() - 1);

        for (ServerPlayer viewer : level.players()) {
            double dx = viewer.getX() - x;
            double dz = viewer.getZ() - z;
            if ((dx * dx + dz * dz) <= (192.0D * 192.0D)) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
            }
        }
    }
}
