package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.item.TownRegisterItem;
import com.cyberspectraa.cybernpc.town.TownWorkSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-authoritative request to unregister the workstation in the player's
 * crosshair. There are no client-supplied block coordinates to forge.
 */
public record WorkplaceRemovePacket() {
    public static void encode(WorkplaceRemovePacket message, FriendlyByteBuf buffer) {}

    public static WorkplaceRemovePacket decode(FriendlyByteBuf buffer) {
        return new WorkplaceRemovePacket();
    }

    public static void handle(WorkplaceRemovePacket message,
                              Supplier<NetworkEvent.Context> supplied) {
        NetworkEvent.Context context = supplied.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || !(player.getMainHandItem().getItem() instanceof TownRegisterItem)
                    || !TownRegisterItem.isWorkMode(player.getMainHandItem())) {
                return;
            }

            // Ray-trace on the server with a limited reach. A client cannot
            // request removal of a far-away or unloaded workstation.
            if (!(player.pick(6.0D, 1.0F, false) instanceof BlockHitResult hit)
                    || hit.getType() != HitResult.Type.BLOCK) {
                player.displayClientMessage(Component.literal(
                        "Look directly at a registered workstation, then press J."), false);
                return;
            }

            String result = TownWorkSavedData.get(player.serverLevel())
                    .removeRegistered(player.serverLevel(), hit.getBlockPos(),
                            player.getMainHandItem());
            player.displayClientMessage(Component.literal(result), false);
        });
        context.setPacketHandled(true);
    }
}
