package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.item.TownRegisterItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Empty, bounded intent packet: server validates held register and mode. */
public record WorkplaceGroupTogglePacket() {
    public static void encode(WorkplaceGroupTogglePacket p, FriendlyByteBuf buf) {}
    public static WorkplaceGroupTogglePacket decode(FriendlyByteBuf buf) {
        return new WorkplaceGroupTogglePacket();
    }
    public static void handle(WorkplaceGroupTogglePacket p,
                              Supplier<NetworkEvent.Context> supplied) {
        NetworkEvent.Context context = supplied.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) TownRegisterItem.toggleMultiselect(player);
        });
        context.setPacketHandled(true);
    }
}
