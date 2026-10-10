package com.cyberspectraa.cybernpc.command;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.intro.CyberIntroService;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class CyberNpcCommands {
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("cybernpc")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("mason")
                                .then(Commands.literal("spawn")
                                        .executes(context -> spawnMason(
                                                context.getSource()
                                        ))))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> spawn(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name")
                                        ))))
                        .then(Commands.literal("intro")
                                .then(Commands.literal("setarrival")
                                        .executes(ctx -> CyberIntroService.setArrival(ctx.getSource())))
                                .then(Commands.literal("setpopewait")
                                        .executes(ctx -> CyberIntroService.setPopeWait(ctx.getSource()))))
                        .then(Commands.literal("admin")
                        .then(Commands.literal("role")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("role", StringArgumentType.greedyString())
                                                .executes(context -> setRole(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        StringArgumentType.getString(context, "role")
                                                )))))
                        .then(Commands.literal("wander")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setWander(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        BoolArgumentType.getBool(context, "enabled")
                                                )))))
                        .then(Commands.literal("hunger")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 20))
                                                .executes(context -> setHunger(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "value")
                                                )))))
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(context -> remove(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target")
                                        ))))
        );
    }

    private static int spawn(CommandSourceStack source, String requestedName) {
        String name = requestedName.trim();

        if (name.isEmpty()) {
            source.sendFailure(Component.literal("NPC name cannot be empty."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        CyberNpcEntity npc = ModEntities.CYBER_NPC.get().create(level);

        if (npc == null) {
            source.sendFailure(Component.literal("CyberNpc could not create an NPC entity."));
            return 0;
        }

        Vec3 position = source.getPosition();
        npc.moveTo(position.x, position.y, position.z, source.getRotation().y, 0.0F);
        npc.setNpcType(NpcType.MAIN);
        npc.setCustomName(Component.literal(name));
        npc.setCustomNameVisible(true);
        npc.setRole("Citizen");

        if (!level.addFreshEntity(npc)) {
            source.sendFailure(Component.literal("CyberNpc could not add the NPC to the world."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Created NPC '" + name + "'."), true);
        return 1;
    }

    private static int spawnMason(
            CommandSourceStack source
    ) {
        ServerLevel level = source.getLevel();
        CyberNpcEntity npc =
                ModEntities.CYBER_NPC.get().create(level);

        if (npc == null) {
            source.sendFailure(
                    Component.literal(
                            "CyberNpc could not create Mason."
                    )
            );
            return 0;
        }

        Vec3 position = source.getPosition();
        npc.moveTo(
                position.x,
                position.y,
                position.z,
                source.getRotation().y,
                0.0F
        );

        npc.setNpcType(NpcType.MAIN);
        npc.setCustomName(Component.literal("Mason"));
        npc.setCustomNameVisible(true);
        npc.setRole("Citizen");
        npc.setCanWander(false);
        npc.setPersistenceRequired();

        npc.setStoryNpcId("mason");

        if (!level.addFreshEntity(npc)) {
            source.sendFailure(
                    Component.literal(
                            "CyberNpc could not add Mason to the world."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Created Mason as a persistent story NPC."
                ),
                true
        );

        return 1;
    }

    private static int setRole(CommandSourceStack source, Entity target, String role) {
        CyberNpcEntity npc = getNpcOrFail(source, target);

        if (npc == null) {
            return 0;
        }

        npc.setRole(role);
        source.sendSuccess(
                () -> Component.literal("Set " + npc.getName().getString() + "'s role to '" + npc.getRole() + "'."),
                true
        );
        return 1;
    }

    private static int setWander(CommandSourceStack source, Entity target, boolean enabled) {
        CyberNpcEntity npc = getNpcOrFail(source, target);

        if (npc == null) {
            return 0;
        }

        npc.setCanWander(enabled);
        source.sendSuccess(
                () -> Component.literal(npc.getName().getString() + " wandering: " + enabled),
                true
        );
        return 1;
    }

    private static int setHunger(CommandSourceStack source, Entity target, int value) {
        CyberNpcEntity npc = getNpcOrFail(source, target);

        if (npc == null) {
            return 0;
        }

        if (npc.getNpcType() != NpcType.WILD) {
            source.sendFailure(Component.literal("Only Wild NPCs use hunger."));
            return 0;
        }

        npc.setHunger(value);
        source.sendSuccess(
                () -> Component.literal(npc.getName().getString() + " hunger: " + npc.getHungerBar()),
                true
        );
        return 1;
    }

    private static int remove(CommandSourceStack source, Entity target) {
        CyberNpcEntity npc = getNpcOrFail(source, target);

        if (npc == null) {
            return 0;
        }

        String name = npc.getName().getString();
        npc.releasePersistentClaims();
        npc.discard();
        source.sendSuccess(() -> Component.literal("Removed NPC '" + name + "'."), true);
        return 1;
    }

    private static CyberNpcEntity getNpcOrFail(CommandSourceStack source, Entity target) {
        if (target instanceof CyberNpcEntity npc) {
            return npc;
        }

        source.sendFailure(Component.literal("The selected entity is not a CyberNpc."));
        return null;
    }

    private CyberNpcCommands() {
    }
}
