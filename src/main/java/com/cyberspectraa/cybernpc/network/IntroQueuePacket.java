package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.SummoningQueueScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Waiting room is separate from the camera scene; neither closes dialogue. */
public record IntroQueuePacket(boolean waiting) {
    public static void encode(IntroQueuePacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.waiting());
    }
    public static IntroQueuePacket decode(FriendlyByteBuf buf) {
        return new IntroQueuePacket(buf.readBoolean());
    }
    public static void handle(IntroQueuePacket p,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (p.waiting()) SummoningQueueScreen.open();
            else SummoningQueueScreen.closeFromServer();
        });
        context.setPacketHandled(true);
    }
}
