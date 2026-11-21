package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.client.NpcShopScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server-authored, per-merchant catalogue and current wallet balance. */
public record ShopSnapshotPacket(int npcId, String merchant, long wallet, String message,
                                 List<Line> buy, List<Line> sell) {
    public record Line(ItemStack item, int count, long price) {}

    public static void encode(ShopSnapshotPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.npcId());
        buf.writeUtf(packet.merchant(), 128);
        buf.writeVarLong(packet.wallet());
        buf.writeUtf(packet.message(), 256);
        writeLines(buf, packet.buy());
        writeLines(buf, packet.sell());
    }

    private static void writeLines(FriendlyByteBuf buf, List<Line> lines) {
        buf.writeVarInt(lines.size());
        for (Line line : lines) {
            buf.writeItem(line.item());
            buf.writeVarInt(line.count());
            buf.writeVarLong(line.price());
        }
    }

    private static List<Line> readLines(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 24) throw new IllegalArgumentException("Invalid shop length");
        List<Line> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = buf.readItem();
            int bundle = buf.readVarInt();
            long price = buf.readVarLong();
            if (stack.isEmpty() || bundle <= 0 || bundle > 64
                    || price < 1 || price > 100_000L) {
                throw new IllegalArgumentException("Malformed shop offer");
            }
            result.add(new Line(stack, bundle, price));
        }
        return List.copyOf(result);
    }

    public static ShopSnapshotPacket decode(FriendlyByteBuf buf) {
        return new ShopSnapshotPacket(buf.readVarInt(), buf.readUtf(128),
                buf.readVarLong(), buf.readUtf(256), readLines(buf), readLines(buf));
    }

    public static void handle(ShopSnapshotPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> NpcShopScreen.show(packet)));
        ctx.setPacketHandled(true);
    }
}
