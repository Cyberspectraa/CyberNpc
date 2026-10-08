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
        new Stock("buy_bread", "3 Bread - 2 silver", Items.BREAD, 3, 20),
        new Stock("buy_torches", "16 Torches - 1 silver", Items.TORCH, 16, 10),
        new Stock("buy_beef", "4 Cooked Beef - 4 silver", Items.COOKED_BEEF, 4, 40),
        new Stock("buy_arrows", "8 Arrows - 2 silver", Items.ARROW, 8, 20)
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
