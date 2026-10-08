package com.cyberspectraa.cybernpc.dialogue;

import com.cyberspectraa.cybernpc.economy.CurrencyValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Temporary shop stock. All purchases and payouts use physical CyberNpc coins
 * in player inventory; no emeralds, bank account, or virtual money is accepted.
 */
public final class NpcShopService {
    public record Stock(String action, String description, Item item, int count, long cost) {
        public String label() {
            return description + " (" + CurrencyValue.formatCoins(cost) + ")";
        }
    }

    private static final List<Stock> GOODS = List.of(
        new Stock("buy_bread", "3 bread", Items.BREAD, 3, 20),
        new Stock("buy_torches", "16 torches", Items.TORCH, 16, 10),
        new Stock("buy_beef", "4 cooked beef", Items.COOKED_BEEF, 4, 40),
        new Stock("buy_arrows", "8 arrows", Items.ARROW, 8, 20),
        new Stock("buy_apples", "6 apples", Items.APPLE, 6, 10),
        new Stock("buy_leather_cap", "Leather cap", Items.LEATHER_HELMET, 1, 50),
        new Stock("buy_iron_pickaxe", "Iron pickaxe", Items.IRON_PICKAXE, 1, 180)
    );

    private NpcShopService() {}

    public static List<Stock> stock() {
        return GOODS;
    }

    /** Only registered physical coins count toward a player's funds. */
    public static long wallet(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        long total = 0L;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            total += CurrencyValue.valueOf(inventory.getItem(slot));
        }
        return total;
    }

    public static String walletDescription(ServerPlayer player) {
        return CurrencyValue.formatCoins(wallet(player));
    }

    public static String buy(ServerPlayer player, String action) {
        Stock offer = GOODS.stream().filter(x -> x.action().equals(action))
                .findFirst().orElse(null);
        if (offer == null || offer.cost() <= 0L || offer.count() <= 0)
            return "I don't have that item for sale.";

        long available = wallet(player);
        if (available < offer.cost()) {
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.45F, 1.0F);
            return "That costs " + CurrencyValue.formatCoins(offer.cost())
                + ", but you only have " + CurrencyValue.formatCoins(available)
                + ". Bring more CyberNpc coins.";
        }

        // Plan the entire payment first: never take partial coins for a failed buy.
        // Prefer exact change in descending denomination order. When exact
        // payment isn't possible, use one larger physical coin and give change.
        long change = chargeCoins(player, offer.cost());
        if (change < 0L) {
            return "I couldn't count your coins. Please try again.";
        }

        if (change > 0L) pay(player, change);

        ItemStack goods = new ItemStack(offer.item(), offer.count());
        if (!player.getInventory().add(goods) && !goods.isEmpty()) {
            player.drop(goods, false);
        }
        player.getInventory().setChanged();
        player.playNotifySound(SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.45F, 1.0F);
        return "Sold for " + CurrencyValue.formatCoins(offer.cost())
                + ". You now have " + walletDescription(player) + ".";
    }

    /**
     * @return copper-value overpayment for change, or -1 on failure.
     * Nothing is removed unless the complete payment has been planned.
     */
    private static long chargeCoins(ServerPlayer player, long cost) {
        Inventory inv = player.getInventory();
        int slots = inv.getContainerSize();
        int[] toRemove = new int[slots];
        long remaining = cost;
        long paid = 0L;

        long[] denominations = {
            CurrencyValue.DRAGON, CurrencyValue.PLATINUM,
            CurrencyValue.GOLD, CurrencyValue.SILVER, CurrencyValue.COPPER
        };
        for (long denomination : denominations) {
            if (remaining < denomination) continue;
            for (int slot = 0; slot < slots && remaining >= denomination; slot++) {
                ItemStack stack = inv.getItem(slot);
                if (CurrencyValue.unitValueOf(stack) != denomination) continue;
                long count = Math.min((long) stack.getCount() - toRemove[slot],
                        remaining / denomination);
                if (count <= 0L) continue;
                toRemove[slot] += (int) count;
                long portion = count * denomination;
                paid += portion;
                remaining -= portion;
            }
        }

        if (remaining > 0L) {
            int bestSlot = -1;
            long bestValue = Long.MAX_VALUE;
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = inv.getItem(slot);
                long unit = CurrencyValue.unitValueOf(stack);
                if (unit >= remaining
                        && unit < bestValue
                        && stack.getCount() > toRemove[slot]) {
                    bestValue = unit;
                    bestSlot = slot;
                }
            }
            if (bestSlot < 0) return -1L;
            toRemove[bestSlot]++;
            paid += bestValue;
        }

        if (paid < cost) return -1L;
        // Revalidate before making any destructive changes.
        for (int slot = 0; slot < slots; slot++) {
            if (toRemove[slot] > 0) {
                ItemStack stack = inv.getItem(slot);
                if (!CurrencyValue.isCurrency(stack)
                        || stack.getCount() < toRemove[slot]) return -1L;
            }
        }
        for (int slot = 0; slot < slots; slot++) {
            if (toRemove[slot] > 0) inv.getItem(slot).shrink(toRemove[slot]);
        }
        inv.setChanged();
        return paid - cost;
    }

    /** Selling stays as a way to earn the same physical coins used for buying. */
    public static String sellHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        long unit = switch (net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getKey(held.getItem()) == null ? "" :
                net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(held.getItem()).toString()) {
            case "minecraft:wheat", "minecraft:coal", "minecraft:oak_log",
                    "minecraft:spruce_log", "minecraft:birch_log" -> 1;
            case "minecraft:leather", "minecraft:iron_ingot" -> 3;
            default -> 0;
        };
        if (unit == 0 || held.isEmpty()) {
            return "I'll buy wheat, coal, timber, leather or iron. Hold what you want to sell.";
        }

        long payment = unit * held.getCount();
        held.setCount(0);
        pay(player, payment);
        player.getInventory().setChanged();
        player.playNotifySound(SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.45F, 1.0F);
        return "Sold for " + CurrencyValue.formatCoins(payment)
                + ". Wallet: " + walletDescription(player) + ".";
    }

    private static void pay(ServerPlayer player, long amount) {
        for (ItemStack coins : CurrencyValue.makePayout(amount)) {
            if (!player.getInventory().add(coins) && !coins.isEmpty())
                player.drop(coins, false);
        }
    }
}
