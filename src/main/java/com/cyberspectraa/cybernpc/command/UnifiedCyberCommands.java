package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
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
import java.util.stream.Collectors;

/**
 * One entry point for Cyber-series commands, without copying or taking
 * ownership of other mods' implementations. All /cyber subcommands redirect
 * to their original Brigadier node, preserving its arguments, completion,
 * nested permissions and behaviour. Original commands remain unchanged.
 *
 * LOWEST priority is essential: individual mods register their roots at
 * NORMAL priority. This bridge runs after them on EACH command-tree rebuild,
 * including integrated worlds and datapack reloads. Missing optional mods
 * simply don't receive an alias.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UnifiedCyberCommands {
    private UnifiedCyberCommands() {}

    private record Section(String alias, String original, String modId,
                           String description, List<String> expectedChildren) {}

    private static final List<Section> SECTIONS = List.of(
            new Section("npc", "cybernpc", "cybernpc",
                    "NPCs, shops, dialogue, Rift, intro markers and post office",
                    List.of("spawn", "admin", "shop", "discordmail", "intro", "rift")),
            new Section("quest", "cyberquest", "cyberquest",
                    "Journal, quest progress and quest/NPC administration",
                    List.of("journal", "status", "admin", "npc")),
            new Section("races", "cyberraces", "cyberraces",
                    "Race, character, evolution and Cyber Level",
                    List.of("race", "character", "evolution", "level")),
            new Section("classes", "cyberclasses", "cyberclasses",
                    "Class information, advancement and administration",
                    List.of("info", "advance", "set", "reset")),
            new Section("server", "cyberserver", "cyberserver",
                    "World story, regions, bosses, economy and Discord connection",
                    List.of("status", "state", "admin", "discordmail")),
            new Section("arrival", "arrival", "cyberserver",
                    "World arrival locations and intro testing (operators)",
                    List.of("setspawn", "test", "admin")),
            new Section("reset", "cyberresetall", "cyberraces",
                    "Reset race, class and progression for testing (operators)",
                    List.of()),
            new Section("balance", "bal", "cybernpc",
                    "View your bank balance", List.of())
    );

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> hub = Commands.literal("cyber")
                .executes(ctx -> showSections(ctx.getSource(), dispatcher))
                .then(Commands.literal("help")
                        .executes(ctx -> showSections(ctx.getSource(), dispatcher))
                        .then(Commands.argument("section", StringArgumentType.word())
                                .suggests((context, suggestions) -> {
                                    for (Section section : SECTIONS) {
                                        CommandNode<CommandSourceStack> target =
                                                target(dispatcher, section);
                                        if (target != null
                                                && target.canUse(context.getSource())) {
                                            suggestions.suggest(section.alias());
                                        }
                                    }
                                    return suggestions.buildFuture();
                                })
                                .executes(ctx -> showHelp(ctx.getSource(), dispatcher,
                                        StringArgumentType.getString(ctx, "section")))))
                .then(Commands.literal("doctor")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> checkRegistrations(ctx.getSource(), dispatcher)));

        // Link to the ACTUAL already-registered nodes. This is safer than
        // re-implementing another mod's execution methods or arguments.
        // Redirects retain Brigadier's original permission checks.
        for (Section section : SECTIONS) {
            CommandNode<CommandSourceStack> original = target(dispatcher, section);
            if (original != null) {
                hub.then(Commands.literal(section.alias())
                        .requires(original.getRequirement())
                        .redirect(original));
            }
        }

        dispatcher.register(hub);
    }

    private static CommandNode<CommandSourceStack> target(
            CommandDispatcher<CommandSourceStack> dispatcher, Section section) {
        return dispatcher.getRoot().getChild(section.original());
    }

    private static int showSections(CommandSourceStack source,
                                    CommandDispatcher<CommandSourceStack> dispatcher) {
        source.sendSuccess(() -> Component.literal(
                "Cyber Command Hub - use /cyber help <section> to browse commands")
                .withStyle(ChatFormatting.GOLD), false);

        int shown = 0;
        for (Section section : SECTIONS) {
            CommandNode<CommandSourceStack> original = target(dispatcher, section);
            if (original == null || !original.canUse(source)) continue;
            shown++;
            String line = "/cyber " + section.alias() + " - " + section.description();
            source.sendSuccess(() -> Component.literal(line)
                    .withStyle(ChatFormatting.GRAY), false);
        }

        if (source.hasPermission(2)) {
            source.sendSuccess(() -> Component.literal(
                    "Operator check: /cyber doctor")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        return Math.max(1, shown);
    }

    private static int showHelp(CommandSourceStack source,
                                CommandDispatcher<CommandSourceStack> dispatcher,
                                String alias) {
        for (Section section : SECTIONS) {
            if (!section.alias().equalsIgnoreCase(alias)) continue;
            CommandNode<CommandSourceStack> original = target(dispatcher, section);
            if (original == null || !original.canUse(source)) break;

            source.sendSuccess(() -> Component.literal(
                    "/cyber " + section.alias() + " - " + section.description())
                    .withStyle(ChatFormatting.GOLD), false);
            List<String> children = new ArrayList<>();
            for (CommandNode<CommandSourceStack> child : original.getChildren()) {
                if (!child.canUse(source)) continue;
                String argument = child.getName();
                children.add(child instanceof com.mojang.brigadier.tree.ArgumentCommandNode
                        ? "<" + argument + ">" : argument);
            }
            if (children.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                        "Usage: /cyber " + section.alias()
                                + " (original: /" + section.original() + ")"), false);
            } else {
                String subcommands = String.join(", ", children);
                source.sendSuccess(() -> Component.literal(
                        "Available subcommands: " + subcommands), false);
                source.sendSuccess(() -> Component.literal(
                        "Example: /cyber " + section.alias() + " "
                                + children.get(0)), false);
                source.sendSuccess(() -> Component.literal(
                        "Original prefix /" + section.original() + " still works."),
                        false);
            }
            return 1;
        }
        source.sendFailure(Component.literal(
                "Unknown or unavailable Cyber command section: " + alias
                        + ". Use /cyber to see sections you can access."));
        return 0;
    }

    /**
     * Non-destructive operator diagnostic. A structural audit is not the same
     * as running commands that spawn entities, alter quests or reset players.
     * It catches missing mod registrations and broken redirect targets safely.
     */
    private static int checkRegistrations(CommandSourceStack source,
                                          CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> hub = dispatcher.getRoot().getChild("cyber");
        int checked = 0;
        int missing = 0;
        List<String> problems = new ArrayList<>();

        for (Section section : SECTIONS) {
            if (!ModList.get().isLoaded(section.modId())) continue;
            checked++;
            CommandNode<CommandSourceStack> original = target(dispatcher, section);
            if (original == null) {
                problems.add("/" + section.original() + " not registered");
                missing++;
                continue;
            }
            CommandNode<CommandSourceStack> link =
                    hub == null ? null : hub.getChild(section.alias());
            if (link == null || link.getRedirect() != original) {
                problems.add("/cyber " + section.alias() + " redirect is missing");
                missing++;
            }
            for (String child : section.expectedChildren()) {
                if (original.getChild(child) == null) {
                    problems.add("/" + section.original() + " " + child + " is missing");
                    missing++;
                }
            }
        }

        final int checkedFinal = checked;
        final int missingFinal = missing;
        source.sendSuccess(() -> Component.literal(
                "Cyber command check: " + checkedFinal + " installed command groups; "
                        + missingFinal + " missing registrations/aliases. "
                        + "No command executed."), false);
        for (String issue : problems) {
            source.sendFailure(Component.literal(issue));
        }
        if (missing == 0) {
            source.sendSuccess(() -> Component.literal(
                    "Command trees and redirects are registered. Test critical commands "
                            + "in-game before considering their gameplay behaviour verified.")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return missing == 0 ? 1 : 0;
    }
}
