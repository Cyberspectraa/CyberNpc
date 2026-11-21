package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.dialogue.NpcShopService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server checks merchant session, distance, catalogue and all item payments. */
public record ShopTradePacket(int merchantId, boolean sell, int index) {
    public static void encode(ShopTradePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.merchantId());
        buffer.writeBoolean(packet.sell());
        buffer.writeVarInt(packet.index());
    }

    public static ShopTradePacket decode(FriendlyByteBuf buffer) {
        return new ShopTradePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(ShopTradePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                NpcShopService.purchase(player, packet.merchantId(), packet.sell(), packet.index());
            }
        });
        ctx.setPacketHandled(true);
    }
}
