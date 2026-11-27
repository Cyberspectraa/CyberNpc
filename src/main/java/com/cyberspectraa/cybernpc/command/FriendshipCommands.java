package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.dialogue.TownCharacterDialogue;
import com.cyberspectraa.cybernpc.rift.RiftDepartureData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Locale;

public final class FriendshipCommands {
    private FriendshipCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cybernpc")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("friendship")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("character", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String prefix = builder.getRemainingLowerCase();
                                            if ("all".startsWith(prefix)) builder.suggest("all");
                                            for (String id : RiftDepartureData.STORY_IDS) {
                                                String name = RiftDepartureData.name(id).toLowerCase(Locale.ROOT);
                                                if (name.startsWith(prefix)) builder.suggest(name);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> apply(ctx, 2, 1, true))
                                        .then(Commands.argument("trust", IntegerArgumentType.integer(-10, 10))
                                                .then(Commands.argument("affection", IntegerArgumentType.integer(-10, 10))
                                                        .executes(ctx -> apply(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "trust"),
                                                                IntegerArgumentType.getInteger(ctx, "affection"),
                                                                false))))))));
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, int trust, int affection,
                             boolean minimum) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String requested = StringArgumentType.getString(ctx, "character");
        String id = RiftDepartureData.storyId(requested);
        List<String> targets = "all".equalsIgnoreCase(requested)
                ? RiftDepartureData.STORY_IDS : id.isEmpty() ? List.of() : List.of(id);
        if (targets.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Choose a Main Seven name or all."));
            return 0;
        }
        int count = 0;
        for (String target : targets) {
            if (TownCharacterDialogue.setTestFriendship(player, target, trust, affection, minimum)) count++;
        }
        final int changed = count;
        String values = (minimum ? "at least " : "") + "Trust " + trust + ", Affection " + affection;
        ctx.getSource().sendSuccess(() -> Component.literal("Friendship updated for "
                + player.getGameProfile().getName() + ": " + changed + " character(s), " + values
                + ". Reopen dialogue to test. Quest progress is unchanged."), true);
        return count;
    }
}
