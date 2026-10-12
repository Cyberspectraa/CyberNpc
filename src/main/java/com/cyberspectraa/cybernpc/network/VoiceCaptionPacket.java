package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.PlayerSpeechBubbleClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Temporary, server-authorized live caption for existing player bubbles.
 * Never written to Minecraft chat or disk.
 */
public record VoiceCaptionPacket(UUID speaker, String text, boolean complete) {
    public static void encode(VoiceCaptionPacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.speaker());
        buf.writeUtf(p.text(), 180);
        buf.writeBoolean(p.complete());
    }

    public static VoiceCaptionPacket decode(FriendlyByteBuf buf) {
        return new VoiceCaptionPacket(buf.readUUID(), buf.readUtf(180), buf.readBoolean());
    }

    public static void handle(VoiceCaptionPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                PlayerSpeechBubbleClient.onVoiceCaption(p.speaker(), p.text(), p.complete())));
        context.setPacketHandled(true);
    }
}
