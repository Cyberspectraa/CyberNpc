package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.economy.BankSavedData;
import com.cyberspectraa.cybernpc.economy.CurrencyValue;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class BankMailCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("bal")
                        .executes(context ->
                                balance(context.getSource())
                        )
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

    private BankMailCommands() {
    }
}
