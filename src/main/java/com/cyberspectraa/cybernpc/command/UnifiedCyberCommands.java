package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;

/** Native shared operator root. No redirects or retired command aliases. */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UnifiedCyberCommands {
    private UnifiedCyberCommands() {}
    private record Group(String mod, List<String> paths) {}
    private static final List<Group> GROUPS = List.of(
            new Group("cybernpc", List.of("npc remove", "npc friendship", "npc intro setarrival",
                    "npc intro setpopewait", "npc rift reset", "npc rift recover",
                    "npc rift ledger", "npc discordmail status", "help", "doctor")),
            new Group("cyberraces", List.of("races level addxp", "races level setlevel", "reset")),
            new Group("cyberserver", List.of("server discordmail status", "server discordmail setup",
                    "arrival setspawn", "arrival test"))
    );

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("cadmin")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> help(ctx.getSource(), dispatcher, ""))
                .then(Commands.literal("help")
                        .executes(ctx -> help(ctx.getSource(), dispatcher, ""))
                        .then(Commands.argument("section", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("cadmin");
                                    if (root != null) for (CommandNode<CommandSourceStack> node : root.getChildren()) {
                                        if (node.canUse(ctx.getSource()) && node.getName().startsWith(builder.getRemainingLowerCase()))
                                            builder.suggest(node.getName());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> help(ctx.getSource(), dispatcher,
                                        StringArgumentType.getString(ctx, "section")))))
                .then(Commands.literal("doctor")
                        .executes(ctx -> doctor(ctx.getSource(), dispatcher))));
    }

    private static CommandNode<CommandSourceStack> find(CommandDispatcher<CommandSourceStack> dispatcher,
                                                        String path) {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot();
        for (String part : path.split(" ")) {
            if (part.isEmpty()) continue;
            node = node.getChild(part);
            if (node == null) return null;
        }
        return node;
    }

    private static int help(CommandSourceStack source, CommandDispatcher<CommandSourceStack> dispatcher,
                            String section) {
        CommandNode<CommandSourceStack> root = find(dispatcher, "cadmin" + (section.isEmpty() ? "" : " " + section));
        if (root == null || !root.canUse(source)) {
            source.sendFailure(Component.literal("Unknown /cadmin section."));
            return 0;
        }
        String prefix = "/cadmin" + (section.isEmpty() ? "" : " " + section);
        String[] usages = dispatcher.getAllUsage(root, source, true);
        source.sendSuccess(() -> Component.literal("Cyber admin commands:"), false);
        for (String usage : usages) {
            if (!usage.isEmpty() || !section.isEmpty())
                source.sendSuccess(() -> Component.literal(prefix + (usage.isEmpty() ? "" : " " + usage)), false);
        }
        return Math.max(1, usages.length);
    }

    private static int doctor(CommandSourceStack source, CommandDispatcher<CommandSourceStack> dispatcher) {
        List<String> issues = new ArrayList<>();
        int checked = 0;
        for (Group group : GROUPS) {
            if (!ModList.get().isLoaded(group.mod())) continue;
            for (String path : group.paths()) {
                checked++;
                if (find(dispatcher, "cadmin " + path) == null) issues.add("Missing /cadmin " + path);
            }
        }
        for (String old : List.of("cyber", "cybernpc", "cyberquest", "cyberraces", "cyberclasses",
                "cyberserver", "cyberresetall", "arrival")) {
            if (find(dispatcher, old) != null) issues.add("Old command root still present: /" + old);
        }
        final int count = checked;
        source.sendSuccess(() -> Component.literal("Checked " + count + " retained command paths; "
                + issues.size() + " issues. No gameplay commands executed."), false);
        for (String issue : issues) source.sendFailure(Component.literal(issue));
        return issues.isEmpty() ? 1 : 0;
    }
}
