package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.rift.NpcRiftDeparture;
import com.cyberspectraa.cybernpc.rift.RiftLedgerService;
import com.cyberspectraa.cybernpc.rift.RiftDepartureData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Merge into the original operator-only command tree before /cyber aliases. */
public final class RiftDepartureCommands {
    private RiftDepartureCommands() {}
    public static void register(RegisterCommandsEvent event) {
        var rift = Commands.literal("rift");
        rift.then(Commands.literal("ledger").executes(ctx -> {
            RiftLedgerService.open(ctx.getSource().getPlayerOrException());
            return 1;
        }));
        rift.then(Commands.literal("arm")
                .then(Commands.argument("target", EntityArgument.entity()).executes(ctx -> {
                    if (!(EntityArgument.getEntity(ctx, "target") instanceof CyberNpcEntity npc)) {
                        ctx.getSource().sendFailure(Component.literal("Select a Main Seven NPC."));
                        return 0;
                    }
                    String error = NpcRiftDeparture.arm(npc);
                    if (error != null) {
                        ctx.getSource().sendFailure(Component.literal(error));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Armed " + npc.getName().getString() + " for 60 seconds. "
                            + "Move them into the Rift or use rift cross."), false);
                    return 1;
                })));
        rift.then(Commands.literal("disarm")
                .then(Commands.argument("target", EntityArgument.entity()).executes(ctx -> {
                    if (!(EntityArgument.getEntity(ctx, "target") instanceof CyberNpcEntity npc)) return 0;
                    NpcRiftDeparture.disarm(npc);
                    ctx.getSource().sendSuccess(() -> Component.literal("NPC crossing disarmed."), false);
                    return 1;
                })));
        rift.then(Commands.literal("cross")
                .then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("rift", BlockPosArgument.blockPos())
                                .executes(ctx -> NpcRiftDeparture.cross(ctx.getSource(),
                                        EntityArgument.getEntity(ctx, "target"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "rift"))))));
        rift.then(Commands.literal("restore")
                .then(Commands.argument("character", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String id : RiftDepartureData.STORY_IDS) {
                                if (RiftDepartureData.get(ctx.getSource().getServer()).departed(id)) {
                                    String name = RiftDepartureData.name(id).toLowerCase(java.util.Locale.ROOT);
                                    if (name.startsWith(builder.getRemainingLowerCase())) builder.suggest(name);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("position", BlockPosArgument.blockPos())
                                .executes(ctx -> NpcRiftDeparture.restore(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "character"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "position"))))));
        event.getDispatcher().register(Commands.literal("cybernpc")
                .requires(source -> source.hasPermission(2)).then(rift));
    }
}
