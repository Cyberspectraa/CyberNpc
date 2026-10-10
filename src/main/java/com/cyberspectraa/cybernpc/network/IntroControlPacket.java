package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.intro.CyberIntroService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client may request finishing its scene, but server owns reveal and intro progress. */
public record IntroControlPacket(boolean skip) {
    public static void encode(IntroControlPacket p, FriendlyByteBuf buf) { buf.writeBoolean(p.skip()); }
    public static IntroControlPacket decode(FriendlyByteBuf buf) { return new IntroControlPacket(buf.readBoolean()); }
    public static void handle(IntroControlPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            if (p.skip()) CyberIntroService.skip(player);
            else CyberIntroService.advanceFromScene(player);
        }
        ctx.setPacketHandled(true);
    }
}
