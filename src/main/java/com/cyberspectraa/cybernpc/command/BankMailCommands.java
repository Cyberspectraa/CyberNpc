package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.economy.BankSavedData;
import com.cyberspectraa.cybernpc.economy.CurrencyValue;
import com.cyberspectraa.cybernpc.mail.MailItemData;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class BankMailCommands {
    private static final int MAX_LETTER_LENGTH = 256;

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("bal")
                        .executes(context -> balance(context.getSource()))
        );

        event.getDispatcher().register(
                Commands.literal("mail")
                        .then(Commands.literal("write")
                                .then(Commands.argument(
                                                "player",
                                                EntityArgument.player()
                                        )
                                        .then(Commands.argument(
                                                        "message",
                                                        StringArgumentType.greedyString()
                                                )
                                                .executes(context -> writeLetter(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(
                                                                context,
                                                                "player"
                                                        ),
                                                        StringArgumentType.getString(
                                                                context,
                                                                "message"
                                                        )
                                                ))))
                        .then(Commands.literal("help")
                                .executes(context -> mailHelp(
                                        context.getSource()
                                )))
        );
    }

    private static int balance(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal(
                    "Only a player can have a bank balance."
            ));
            return 0;
        }

        long balance = BankSavedData.get(player.serverLevel())
                .getBalance(player.getUUID());

        source.sendSuccess(
                () -> Component.literal(
                        "Bank balance: "
                                + CurrencyValue.format(balance)
                                + " credits"
                ),
                false
        );

        return 1;
    }

    private static int writeLetter(
            CommandSourceStack source,
            ServerPlayer recipient,
            String rawMessage
    ) {
        ServerPlayer sender;

        try {
            sender = source.getPlayerOrException();
        } catch (Exception ignored) {
            source.sendFailure(Component.literal(
                    "Only a player can write physical mail."
            ));
            return 0;
        }

        String message = rawMessage == null ? "" : rawMessage.trim();

        if (message.isEmpty()) {
            source.sendFailure(Component.literal(
                    "The letter cannot be empty."
            ));
            return 0;
        }

        if (message.length() > MAX_LETTER_LENGTH) {
            source.sendFailure(Component.literal(
                    "Letters can be at most "
                            + MAX_LETTER_LENGTH
                            + " characters."
            ));
            return 0;
        }

        int paperSlot = findLetterPaper(sender);
        if (paperSlot < 0) {
            source.sendFailure(Component.literal(
                    "You need Letter Paper to write a letter."
            ));
            return 0;
        }

        ItemStack paper = sender.getInventory().getItem(paperSlot);
        paper.shrink(1);

        ItemStack addressed = new ItemStack(
                ModItems.ADDRESSED_LETTER.get()
        );

        MailItemData.writeDraft(
                addressed,
                sender.getUUID(),
                sender.getGameProfile().getName(),
                recipient.getUUID(),
                recipient.getGameProfile().getName(),
                message
        );

        if (!sender.getInventory().add(addressed)) {
            sender.drop(addressed, false);
        }

        sender.getInventory().setChanged();

        source.sendSuccess(
                () -> Component.literal(
                        "Wrote a letter to "
                                + recipient.getGameProfile().getName()
                                + ". Post it in a Drop Box."
                ),
                false
        );

        return 1;
    }

    private static int mailHelp(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal(
                        "Mail: carry Letter Paper, use "
                                + "/mail write <player> <message>, "
                                + "then place the Addressed Letter in a Drop Box."
                ),
                false
        );
        return 1;
    }

    private static int findLetterPaper(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory()
                    .getItem(i)
                    .is(ModItems.LETTER_PAPER.get())) {
                return i;
            }
        }

        return -1;
    }

    private BankMailCommands() {
    }
}
