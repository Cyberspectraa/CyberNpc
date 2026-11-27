package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.rift.RiftLedgerService;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class RiftDepartureCommands {
    private RiftDepartureCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("npc")
                        .then(Commands.literal("rift")
                                .then(Commands.literal("ledger").executes(ctx -> {
                                    RiftLedgerService.open(ctx.getSource().getPlayerOrException());
                                    return 1;
                                })))));
    }
}
