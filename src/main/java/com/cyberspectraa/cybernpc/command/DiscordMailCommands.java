package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.mail.MailSavedData;
import com.cyberspectraa.cybernpc.registry.ModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;

/**
 * Operator-only choice of which physical Drop Box gets Discord submissions.
 * Never exposes or changes any Discord credentials.
 */
public final class DiscordMailCommands {
    private DiscordMailCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("cybernpc")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("discordmail")
                                .then(Commands.literal("bind")
                                        .executes(ctx -> bind(ctx.getSource())))
                                .then(Commands.literal("unbind")
                                        .executes(ctx -> unbind(ctx.getSource())))
                                .then(Commands.literal("status")
                                        .executes(ctx -> status(ctx.getSource()))))
        );
    }

    private static int bind(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(7D, 1F, false);
        if (!(hit instanceof BlockHitResult blockHit)
                || !player.serverLevel().getBlockState(blockHit.getBlockPos())
                        .is(ModBlocks.DROP_BOX.get())) {
            source.sendFailure(Component.literal(
                    "Look directly at a CyberNpc Drop Box within seven blocks and try again."));
            return 0;
        }

        BlockPos pos = blockHit.getBlockPos();
        MailSavedData mail = MailSavedData.get(player.serverLevel());
        mail.registerDropBox(player.serverLevel().dimension(), pos);
        mail.setDiscordDropBox(new MailSavedData.PostalAddress(
                player.serverLevel().dimension(), pos));
        source.sendSuccess(() -> Component.literal(
                "Discord letters will now enter this Drop Box at " + pos.toShortString()
                        + ". Keep the chunk loaded for courier collection."), true);
        return 1;
    }

    private static int unbind(CommandSourceStack source) {
        MailSavedData mail = MailSavedData.get(source.getServer().overworld());
        mail.setDiscordDropBox(null);
        source.sendSuccess(() -> Component.literal(
                "Discord Drop Box binding cleared; any available registered Drop Box may be used."), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        MailSavedData mail = MailSavedData.get(source.getServer().overworld());
        MailSavedData.PostalAddress bound = mail.getDiscordDropBox();
        String message = bound == null
                ? "Discord post office: automatic Drop Box selection."
                : "Discord post office: " + bound.dimension().location()
                        + " at " + bound.pos().toShortString()
                        + (mail.availableDiscordDropBox(source.getServer()) != null
                        ? " (ready)" : " (unloaded, missing or full — new letters wait).");
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
