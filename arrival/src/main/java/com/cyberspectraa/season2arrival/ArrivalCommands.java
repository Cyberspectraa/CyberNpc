package com.cyberspectraa.season2arrival;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ArrivalCommands {
    private ArrivalCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("arrival")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("setspawn")
                                .executes(ctx -> setSpawn(ctx.getSource())))
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
                        .then(Commands.literal("test")
                                .executes(ctx -> test(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> test(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("beam")
                                .executes(ctx -> beam(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> beam(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("resetall")
                                .executes(ctx -> resetAll(ctx.getSource())))
                        .then(Commands.literal("mark")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> mark(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    private static int setSpawn(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = source.getServer();

        if (player.serverLevel() != server.overworld()) {
            source.sendFailure(Component.literal("Season 2 Arrival: the church/world spawn must be set in the Overworld."));
            return 0;
        }

        BlockPos pos = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        ArrivalSavedData data = ArrivalSavedData.get(server);
        data.setSpawn(pos, player.getYRot());
        ArrivalEvents.enforceExactWorldSpawn(server, pos, player.getYRot());

        source.sendSuccess(() -> Component.literal(
                "Season 2 Arrival spawn set exactly to block " +
                        pos.getX() + " " + pos.getY() + " " + pos.getZ() +
                        ". Vanilla spawnRadius is now 0."
        ), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ArrivalSavedData data = ArrivalSavedData.get(server);

        if (!data.isSpawnConfigured()) {
            source.sendSuccess(() -> Component.literal(
                    "Season 2 Arrival: no church spawn has been configured yet. Stand on it and run /arrival setspawn."
            ), false);
            return 1;
        }

        BlockPos pos = data.getSpawnPos();
        int radius = server.overworld().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SPAWN_RADIUS).get();
        source.sendSuccess(() -> Component.literal(
                "Season 2 Arrival: spawn=" + pos.getX() + " " + pos.getY() + " " + pos.getZ() +
                        ", spawnRadius=" + radius +
                        ", arrivedPlayers=" + data.arrivedCount()
        ), false);
        return 1;
    }

    private static int test(CommandSourceStack source, ServerPlayer target) {
        MinecraftServer server = source.getServer();
        ArrivalSavedData data = ArrivalSavedData.get(server);

        if (!data.isSpawnConfigured()) {
            source.sendFailure(Component.literal("Season 2 Arrival: set the church spawn first with /arrival setspawn."));
            return 0;
        }

        BlockPos spawn = data.getSpawnPos();
        double x = spawn.getX() + 0.5D;
        double y = spawn.getY();
        double z = spawn.getZ() + 0.5D;

        target.teleportTo(server.overworld(), x, y, z, data.getSpawnYaw(), 0.0F);
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        target.fallDistance = 0.0F;
        ArrivalEvents.startSequence(server.overworld(), x, y, z);

        source.sendSuccess(() -> Component.literal(
                "Played the full arrival test for " + target.getGameProfile().getName() +
                        ". Their first-join state was not changed."
        ), true);
        return 1;
    }

    private static int beam(CommandSourceStack source, ServerPlayer target) {
        ArrivalEvents.startSequence(target.serverLevel(), target.getX(), target.getY(), target.getZ());
        source.sendSuccess(() -> Component.literal(
                "Played the summoning beam at " + target.getGameProfile().getName() + "'s current position."
        ), false);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer target) {
        ArrivalSavedData data = ArrivalSavedData.get(source.getServer());
        data.reset(target.getUUID());
        source.sendSuccess(() -> Component.literal(
                "Reset first-arrival state for " + target.getGameProfile().getName() +
                        ". Their next login will trigger the real arrival sequence."
        ), true);
        return 1;
    }

    private static int resetAll(CommandSourceStack source) {
        ArrivalSavedData data = ArrivalSavedData.get(source.getServer());
        int count = data.resetAll();
        source.sendSuccess(() -> Component.literal(
                "Reset first-arrival state for " + count + " stored player(s)."
        ), true);
        return 1;
    }

    private static int mark(CommandSourceStack source, ServerPlayer target) {
        ArrivalSavedData data = ArrivalSavedData.get(source.getServer());
        data.markArrived(target.getUUID());
        source.sendSuccess(() -> Component.literal(
                "Marked " + target.getGameProfile().getName() + " as already summoned."
        ), true);
        return 1;
    }

    private static int info(CommandSourceStack source, ServerPlayer target) {
        ArrivalSavedData data = ArrivalSavedData.get(source.getServer());
        boolean arrived = data.hasArrived(target.getUUID());
        source.sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " first-arrival state: " +
                        (arrived ? "already summoned" : "not yet summoned")
        ), false);
        return 1;
    }
}
