package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.mail.MailSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Read-only diagnostics for automatic postal routing. */
public final class DiscordMailCommands {
    private DiscordMailCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("npc")
                        .then(Commands.literal("discordmail")
                                .then(Commands.literal("status")
                                        .executes(ctx -> status(ctx.getSource()))))));
    }
    private static int status(CommandSourceStack source) {
        MailSavedData mail = MailSavedData.get(source.getServer().overworld());
        MailSavedData.PostalAddress ready = mail.availableDiscordDropBox(source.getServer());
        String message = ready == null
                ? "Automatic postal routing: no loaded Drop Box with space. New Discord mail waits."
                : "Automatic postal routing: available Drop Box in " + ready.dimension().location()
                        + " at " + ready.pos().toShortString() + ".";
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
