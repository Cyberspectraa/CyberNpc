package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.dialogue.NpcShopService;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Creator controls for per-NPC stock; only operators can run these commands.
 * Add commands copy the executor's main-hand item (including NBT).
 * Prices are copper-equivalent values; no virtual wallet is introduced.
 */
public final class ShopAdminCommands {
    private ShopAdminCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("cybernpc")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("shop")
                                .then(Commands.literal("list")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(ctx -> list(ctx.getSource(),
                                                        EntityArgument.getEntity(ctx, "target")))))
                                .then(Commands.literal("addbuy")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("price", LongArgumentType.longArg(
                                                        1L, NpcShopService.MAX_PRICE))
                                                        .then(Commands.argument("count",
                                                                IntegerArgumentType.integer(1, 64))
                                                                .executes(ctx -> add(ctx.getSource(),
                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                        false,
                                                                        LongArgumentType.getLong(ctx, "price"),
                                                                        IntegerArgumentType.getInteger(ctx, "count")))))))
                                .then(Commands.literal("addsell")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("price", LongArgumentType.longArg(
                                                        1L, NpcShopService.MAX_PRICE))
                                                        .then(Commands.argument("count",
                                                                IntegerArgumentType.integer(1, 64))
                                                                .executes(ctx -> add(ctx.getSource(),
                                                                        EntityArgument.getEntity(ctx, "target"),
                                                                        true,
                                                                        LongArgumentType.getLong(ctx, "price"),
                                                                        IntegerArgumentType.getInteger(ctx, "count")))))))
                                .then(Commands.literal("removebuy")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("index",
                                                        IntegerArgumentType.integer(0))
                                                        .executes(ctx -> remove(ctx.getSource(),
                                                                EntityArgument.getEntity(ctx, "target"),
                                                                false,
                                                                IntegerArgumentType.getInteger(ctx, "index"))))))
                                .then(Commands.literal("removesell")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("index",
                                                        IntegerArgumentType.integer(0))
                                                        .executes(ctx -> remove(ctx.getSource(),
                                                                EntityArgument.getEntity(ctx, "target"),
                                                                true,
                                                                IntegerArgumentType.getInteger(ctx, "index"))))))
                        )
        );
    }

    private static CyberNpcEntity shopkeeper(CommandSourceStack source, Entity target) {
        if (target instanceof CyberNpcEntity npc && NpcShopService.isShopkeeper(npc)) {
            return npc;
        }
        source.sendFailure(Component.literal("Target a living CyberNpc with the Shopkeeper role."));
        return null;
    }

    private static int list(CommandSourceStack source, Entity target) {
        CyberNpcEntity npc = shopkeeper(source, target);
        if (npc == null) return 0;
        source.sendSystemMessage(Component.literal(NpcShopService.list(npc)));
        return 1;
    }

    private static int add(CommandSourceStack source, Entity target, boolean selling,
                           long price, int count) throws
            com.mojang.brigadier.exceptions.CommandSyntaxException {
        CyberNpcEntity npc = shopkeeper(source, target);
        if (npc == null) return 0;
        ServerPlayer player = source.getPlayerOrException();
        String result = NpcShopService.configure(player, npc, selling, count, price);
        source.sendSuccess(() -> Component.literal(result), false);
        return 1;
    }

    private static int remove(CommandSourceStack source, Entity target,
                              boolean selling, int index) {
        CyberNpcEntity npc = shopkeeper(source, target);
        if (npc == null) return 0;
        String result = NpcShopService.remove(npc, selling, index);
        source.sendSuccess(() -> Component.literal(result), false);
        return 1;
    }
}
