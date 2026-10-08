package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.NpcDialogueScreen;
import com.cyberspectraa.cybernpc.dialogue.DialogueView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record DialogueOpenPacket(DialogueView view) {
    public static void encode(DialogueOpenPacket p, FriendlyByteBuf buf) {
        DialogueView.encode(p.view(), buf);
    }

    public static DialogueOpenPacket decode(FriendlyByteBuf buf) {
        return new DialogueOpenPacket(DialogueView.decode(buf));
    }

    public static void handle(DialogueOpenPacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> NpcDialogueScreen.open(p.view()));
        ctx.setPacketHandled(true);
    }
}
