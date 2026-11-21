package com.cyberspectraa.cybernpc.dialogue;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.economy.CurrencyValue;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.ShopSnapshotPacket;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Each authored Shopkeeper owns their own NBT-backed, infinite-supply catalogue
 * of buy and sell offers. Pricing and payment are server authoritative.
 *
 * Old global demo stock was never attached to an NPC, so new merchants start
 * empty until an operator configures them. Never consume bank balances.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID)
public final class NpcShopService {
    public static final int MAX_OFFERS = 24;
    // Keep even the largest sell payouts bounded; one billion copper in
    // physical items would drop thousands of coin stacks and lag a server.
    public static final long MAX_PRICE = 100_000L;
    private static final String DATA = "CyberNpcShopV2";
    private static final long SESSION_TICKS = 2400L;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public record Offer(ItemStack sample, int count, long price) {
        public Offer {
            sample = sample.copy();
            sample.setCount(1);
        }

        public ItemStack delivery() {
            ItemStack stack = sample.copy();
            stack.setCount(count);
            return stack;
        }
    }

    private record Session(UUID merchant, long expiresAt) {}
    private NpcShopService() {}

    public static boolean isShopkeeper(CyberNpcEntity npc) {
        return npc != null && npc.isAlive() && npc.getNpcType() != NpcType.WILD
                && NpcServiceRole.fromRole(npc.getRole()) == NpcServiceRole.SHOPKEEPER;
    }

    private static boolean canTrade(ServerPlayer player, CyberNpcEntity npc) {
        return player != null && isShopkeeper(npc) && player.level() == npc.level()
                && player.distanceToSqr(npc) <= 36D;
    }

    private static List<Offer> offers(CyberNpcEntity npc, boolean selling) {
        CompoundTag shop = npc.getPersistentData().getCompound(DATA);
        ListTag list = shop.getList(selling ? "Sell" : "Buy", Tag.TAG_COMPOUND);
        List<Offer> result = new ArrayList<>();
        for (int i = 0; i < list.size() && result.size() < MAX_OFFERS; i++) {
            CompoundTag row = list.getCompound(i);
            ItemStack item = ItemStack.of(row.getCompound("Item"));
            int count = row.getInt("Count");
            long price = row.getLong("Price");
            if (item.isEmpty() || count <= 0 || count > item.getMaxStackSize()
                    || price <= 0 || price > MAX_PRICE) continue;
            result.add(new Offer(item, count, price));
        }
        return result;
    }

    private static void saveOffers(CyberNpcEntity npc, boolean selling, List<Offer> offers) {
        CompoundTag root = npc.getPersistentData().getCompound(DATA);
        ListTag list = new ListTag();
        for (Offer offer : offers) {
            CompoundTag row = new CompoundTag();
            row.put("Item", offer.sample().save(new CompoundTag()));
            row.putInt("Count", offer.count());
            row.putLong("Price", offer.price());
            list.add(row);
        }
        root.put(selling ? "Sell" : "Buy", list);
        npc.getPersistentData().put(DATA, root);
    }

    /** Operator configuration; held item is copied, never consumed. */
    public static String configure(ServerPlayer admin, CyberNpcEntity npc,
                                   boolean selling, int count, long price) {
        if (!isShopkeeper(npc)) return "Target must be a living Shopkeeper NPC.";
        ItemStack held = admin.getMainHandItem();
        if (held.isEmpty() || CurrencyValue.isCurrency(held)) {
            return "Hold the item to list; coins cannot be offered as goods.";
        }
        if (count < 1 || count > held.getMaxStackSize()
                || price <= 0 || price > MAX_PRICE) {
            return "Invalid bundle size or price.";
        }
        List<Offer> current = offers(npc, selling);
        if (current.size() >= MAX_OFFERS) return "That tab is full (24 offers max).";
        current.add(new Offer(held, count, price));
        saveOffers(npc, selling, current);
        return (selling ? "Sell-to-shop" : "Shop-sells") + " offer "
                + (current.size() - 1) + ": " + count + " x "
                + held.getHoverName().getString() + " for "
                + CurrencyValue.formatCoins(price) + ".";
    }

    public static String remove(CyberNpcEntity npc, boolean selling, int index) {
        if (!isShopkeeper(npc)) return "Target must be a Shopkeeper NPC.";
        List<Offer> current = offers(npc, selling);
        if (index < 0 || index >= current.size()) return "Offer number not found.";
        Offer removed = current.remove(index);
        saveOffers(npc, selling, current);
        return "Removed offer " + index + " (" + removed.sample().getHoverName().getString() + ").";
    }

    public static String list(CyberNpcEntity npc) {
        if (!isShopkeeper(npc)) return "Target must be a Shopkeeper NPC.";
        StringBuilder text = new StringBuilder("Shop: ").append(npc.getName().getString());
        for (boolean selling : new boolean[]{false, true}) {
            List<Offer> rows = offers(npc, selling);
            text.append("\n").append(selling ? "Sell (player to shop)" : "Buy (shop to player)")
                    .append(": ").append(rows.size()).append(" offers");
            for (int i = 0; i < rows.size(); i++) {
                Offer offer = rows.get(i);
                text.append("\n#").append(i).append(": ").append(offer.count())
                        .append(" x ").append(offer.sample().getHoverName().getString())
                        .append(" - ").append(CurrencyValue.formatCoins(offer.price()));
            }
        }
        return text.toString();
    }

    public static void open(ServerPlayer player, CyberNpcEntity npc) {
        if (!canTrade(player, npc)) return;
        SESSIONS.put(player.getUUID(),
                new Session(npc.getUUID(), player.level().getGameTime() + SESSION_TICKS));
        npc.getLookControl().setLookAt(player, 25F, 25F);
        send(player, npc, "");
    }

    private static void send(ServerPlayer player, CyberNpcEntity npc, String feedback) {
        List<ShopSnapshotPacket.Line> buy = offers(npc, false).stream()
                .map(o -> new ShopSnapshotPacket.Line(o.sample(), o.count(), o.price()))
                .toList();
        List<ShopSnapshotPacket.Line> sell = offers(npc, true).stream()
                .map(o -> new ShopSnapshotPacket.Line(o.sample(), o.count(), o.price()))
                .toList();
        String merchant = npc.getName().getString();
        if (merchant.length() > 128) merchant = merchant.substring(0, 128);
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ShopSnapshotPacket(npc.getId(), merchant,
                        wallet(player), feedback, buy, sell));
    }

    public static void purchase(ServerPlayer player, int entityId, boolean selling, int index) {
        Session session = SESSIONS.get(player.getUUID());
        Entity target = player.level().getEntity(entityId);
        if (!(target instanceof CyberNpcEntity npc) || !canTrade(player, npc)
                || session == null || !session.merchant().equals(npc.getUUID())
                || player.level().getGameTime() > session.expiresAt()) {
            SESSIONS.remove(player.getUUID());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "You're no longer trading with that Shopkeeper. Open the shop again."));
            return;
        }
        List<Offer> catalogue = offers(npc, selling);
        String response;
        if (index < 0 || index >= catalogue.size()) {
            response = "That offer is no longer available.";
        } else {
            Offer offer = catalogue.get(index);
            response = selling ? sell(player, offer) : buy(player, offer);
        }
        // Rebuild fresh prices and wallet after every server-approved action.
        SESSIONS.put(player.getUUID(),
                new Session(npc.getUUID(), player.level().getGameTime() + SESSION_TICKS));
        send(player, npc, response);
    }

    public static long wallet(ServerPlayer player) {
        long total = 0L;
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            total += CurrencyValue.valueOf(inv.getItem(slot));
        }
        return total;
    }

    private static String buy(ServerPlayer player, Offer offer) {
        if (wallet(player) < offer.price()) return "Not enough coins for this purchase.";
        long change = chargeCoins(player, offer.price());
        if (change < 0) return "Payment could not be completed; no coins were taken.";
        if (change > 0) pay(player, change);
        ItemStack goods = offer.delivery();
        if (!player.getInventory().add(goods) && !goods.isEmpty()) player.drop(goods, false);
        player.getInventory().setChanged();
        player.playNotifySound(SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.45F, 1F);
        return "Bought " + offer.count() + " x " + offer.sample().getHoverName().getString() + ".";
    }

    private static String sell(ServerPlayer player, Offer offer) {
        Inventory inv = player.getInventory();
        int[] removal = new int[inv.getContainerSize()];
        int remaining = offer.count();
        // Selling never removes equipped armour or offhand; only main slots.
        for (int slot = 0; slot < Math.min(36, inv.getContainerSize()) && remaining > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!ItemStack.isSameItemSameTags(stack, offer.sample())) continue;
            int taken = Math.min(remaining, stack.getCount());
            removal[slot] = taken;
            remaining -= taken;
        }
        if (remaining != 0) return "You don't have enough matching items to sell.";
        for (int slot = 0; slot < removal.length; slot++) {
            if (removal[slot] > 0) inv.getItem(slot).shrink(removal[slot]);
        }
        pay(player, offer.price());
        inv.setChanged();
        player.playNotifySound(SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.45F, 1F);
        return "Sold " + offer.count() + " x " + offer.sample().getHoverName().getString() + ".";
    }

    /**
     * Plan before mutating, preferring the needed denominations and giving
     * change from larger physical coins when exact payment is impossible.
     */
    private static long chargeCoins(ServerPlayer player, long cost) {
        Inventory inv = player.getInventory();
        int slots = inv.getContainerSize();
        int[] remove = new int[slots];
        long remaining = cost, paid = 0L;
        long[] denominations = {CurrencyValue.DRAGON, CurrencyValue.PLATINUM,
                CurrencyValue.GOLD, CurrencyValue.SILVER, CurrencyValue.COPPER};
        for (long unit : denominations) {
            for (int slot = 0; slot < slots && remaining >= unit; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (CurrencyValue.unitValueOf(stack) != unit) continue;
                int count = (int) Math.min(stack.getCount() - remove[slot], remaining / unit);
                if (count <= 0) continue;
                remove[slot] += count;
                remaining -= count * unit;
                paid += count * unit;
            }
        }
        if (remaining > 0) {
            int best = -1;
            long value = Long.MAX_VALUE;
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = inv.getItem(slot);
                long unit = CurrencyValue.unitValueOf(stack);
                if (unit >= remaining && unit < value && stack.getCount() > remove[slot]) {
                    best = slot;
                    value = unit;
                }
            }
            if (best < 0) return -1;
            remove[best]++;
            paid += value;
        }
        if (paid < cost) return -1;
        for (int slot = 0; slot < slots; slot++) {
            if (remove[slot] > 0 && (!CurrencyValue.isCurrency(inv.getItem(slot))
                    || inv.getItem(slot).getCount() < remove[slot])) return -1;
        }
        for (int slot = 0; slot < slots; slot++) {
            if (remove[slot] > 0) inv.getItem(slot).shrink(remove[slot]);
        }
        inv.setChanged();
        return paid - cost;
    }

    private static void pay(ServerPlayer player, long amount) {
        for (ItemStack coins : CurrencyValue.makePayout(amount)) {
            if (!player.getInventory().add(coins) && !coins.isEmpty())
                player.drop(coins, false);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}
