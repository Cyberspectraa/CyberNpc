package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.dialogue.NpcDialogueController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the server when a player exits a dialogue with Escape or Close.
 * Does not execute a story choice or alter any quest or friendship data.
 */
public record DialogueClosePacket(int npcId) {
    public static void encode(DialogueClosePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.npcId());
    }

    public static DialogueClosePacket decode(FriendlyByteBuf buf) {
        return new DialogueClosePacket(buf.readVarInt());
    }

    public static void handle(DialogueClosePacket p,
                              Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            NpcDialogueController.close(player, p.npcId());
        }
        context.setPacketHandled(true);
    }
}
