package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.dialogue.NpcDialogueController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record DialogueChoicePacket(int npcId, String action) {
    public static void encode(DialogueChoicePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.npcId());
        buf.writeUtf(p.action(), 40);
    }

    public static DialogueChoicePacket decode(FriendlyByteBuf buf) {
        return new DialogueChoicePacket(buf.readVarInt(), buf.readUtf(40));
    }

    public static void handle(DialogueChoicePacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            NpcDialogueController.choose(player, p.npcId(), p.action());
        }
        ctx.setPacketHandled(true);
    }
}
