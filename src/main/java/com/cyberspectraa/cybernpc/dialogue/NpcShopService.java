package com.cyberspectraa.cybernpc.dialogue;

import com.cyberspectraa.cybernpc.economy.CurrencyValue;
import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Small, safe starter stock for Shopkeeper dialogue, paid with CyberNpc coins. */
public final class NpcShopService {
    public record Stock(String action, String label, Item item, int count, long cost) {}

    private static final List<Stock> GOODS = List.of(
        new Stock("buy_bread", "I'll take 3 bread (2s).", Items.BREAD, 3, 20),
        new Stock("buy_torches", "I'll take 16 torches (1s).", Items.TORCH, 16, 10),
        new Stock("buy_beef", "I'll take 4 beef (4s).", Items.COOKED_BEEF, 4, 40),
        new Stock("buy_arrows", "I'll take 8 arrows (2s).", Items.ARROW, 8, 20),
        // Placeholder stock. All offers live in this single list for the
        // planned shop/economy overhaul; keep <= 7 offers (+ Back = 8).
        new Stock("buy_apples", "I'll take 6 apples (1s).", Items.APPLE, 6, 10),
        new Stock("buy_leather_cap", "I'll take a leather cap (5s).", Items.LEATHER_HELMET, 1, 50),
        new Stock("buy_iron_pickaxe", "I'll take an iron pickaxe (18s).", Items.IRON_PICKAXE, 1, 180)
    );

    private NpcShopService() {}

    public static List<Stock> stock() {
        return GOODS;
    }

    public static String buy(ServerPlayer player, String action) {
        Stock offer = GOODS.stream().filter(x -> x.action().equals(action))
                .findFirst().orElse(null);
        if (offer == null) return "I don't have that item for sale.";

        long available = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            available += CurrencyValue.valueOf(player.getInventory().getItem(i));
        }
        if (available < offer.cost()) {
            return "You need " + CurrencyValue.format(offer.cost())
                    + " copper-worth of guild coins for those goods.";
        }

        // The server alone controls prices and payments. Change is returned
        // in real coins if a large denomination was used.
        long paid = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && paid < offer.cost(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            long unit = CurrencyValue.unitValueOf(stack);
            while (unit > 0 && !stack.isEmpty() && paid < offer.cost()) {
                stack.shrink(1);
                paid += unit;
            }
        }
        if (paid > offer.cost()) pay(player, paid - offer.cost());

        ItemStack goods = new ItemStack(offer.item(), offer.count());
        if (!player.getInventory().add(goods)) {
            player.drop(goods, false);
        }
        return "A fair trade. Enjoy your " + offer.count() + " "
                + offer.item().getDescription().getString() + ".";
    }

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
        return "Sold! Here's " + payment + " copper-worth of coins.";
    }

    private static void pay(ServerPlayer player, long amount) {
        for (ItemStack coins : CurrencyValue.makePayout(amount)) {
            if (!player.getInventory().add(coins)) player.drop(coins, false);
        }
    }
}
