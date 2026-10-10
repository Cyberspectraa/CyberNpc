package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.SummoningCinematicScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Starts/stops only the independent custom summoning camera. */
public record IntroScenePacket(boolean begin, BlockPos arrival, float yaw) {
    public static void encode(IntroScenePacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.begin());
        buf.writeBlockPos(p.arrival());
        buf.writeFloat(p.yaw());
    }
    public static IntroScenePacket decode(FriendlyByteBuf buf) {
        return new IntroScenePacket(buf.readBoolean(), buf.readBlockPos(), buf.readFloat());
    }
    public static void handle(IntroScenePacket p,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (p.begin()) SummoningCinematicScreen.open(p.arrival(), p.yaw());
            else SummoningCinematicScreen.closeFromServer();
        }));
        context.setPacketHandled(true);
    }
}
