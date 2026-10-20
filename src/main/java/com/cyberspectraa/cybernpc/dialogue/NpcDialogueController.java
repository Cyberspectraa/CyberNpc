package com.cyberspectraa.cybernpc.dialogue;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.intro.CyberIntroService;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.DialogueOpenPacket;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative conversation controller for Main, Quest and service
 * NPCs only. Wild NPCs communicate through reaction bubbles, not dialogue.
 * The client renders the conversation but never decides rewards,
 * coin payments, quest registration or NPC identity.
 */
@Mod.EventBusSubscriber(modid = "cybernpc")
public final class NpcDialogueController {
    private static final long SESSION_LIFETIME = 2400L;
    private static final double MAX_RANGE_SQR = 36.0D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private record Session(UUID npcUuid, String page, long expiresAt) {}

    private NpcDialogueController() {}

    public static void open(ServerPlayer player, CyberNpcEntity npc) {
        if (!valid(player, npc)) return;
        String page = "root";
        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, "", "");
    }

    public static void openIntro(ServerPlayer player, CyberNpcEntity npc) {
        if (!CyberIntroService.isGreeting(player) || !valid(player, npc)) return;
        // Restore normal first-person gameplay looking toward the Pope.
        // ServerPlayer.lookAt synchronises the orientation to the client;
        // it does not move the player's spawn position or camera entity.
        player.lookAt(EntityAnchorArgument.Anchor.EYES,
                npc, EntityAnchorArgument.Anchor.EYES);
        String page = "intro_0";
        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, "", "");
    }

    public static void choose(ServerPlayer player, int npcId, String action) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || player.level().getGameTime() > session.expiresAt()) {
            SESSIONS.remove(player.getUUID());
            return;
        }

        Entity target = player.level().getEntity(npcId);
        if (!(target instanceof CyberNpcEntity npc)
                || !npc.getUUID().equals(session.npcUuid()) || !valid(player, npc)) {
            SESSIONS.remove(player.getUUID());
            return;
        }

        // Never execute a client-supplied action without confirming that the
        // currently active, server-generated page actually offered it.
        List<DialogueView.Option> validOptions = options(npc, session.page());
        if (validOptions.stream().noneMatch(o -> o.id().equals(action))) return;

        if (session.page().startsWith("intro_")) {
            if (!CyberIntroService.isGreeting(player)) return;
            if ("intro_skip".equals(action) || "intro_finish".equals(action)) {
                SESSIONS.remove(player.getUUID());
                CyberIntroService.finish(player);
                return;
            }
            String next = switch (action) {
                case "intro_town" -> "intro_1";
                case "intro_guild" -> "intro_2";
                case "intro_trade" -> "intro_3";
                default -> null;
            };
            if (next == null) return;
            SESSIONS.put(player.getUUID(),
                new Session(npc.getUUID(), next, player.level().getGameTime() + SESSION_LIFETIME));
            send(player, npc, next, validOptions.stream()
                    .filter(o -> o.id().equals(action)).findFirst().orElseThrow().label(), "");
            return;
        }

        if ("leave".equals(action)) {
            SESSIONS.remove(player.getUUID());
            return;
        }

        String playerLine = validOptions.stream()
            .filter(o -> o.id().equals(action))
            .findFirst().orElseThrow().label();
        String page = session.page();

        // This is an intentional blank-slate dialogue pass: NPCs no longer
        // deliver any written speech. Only player choices and server-side
        // actions stay available for the future dialogue rewrite.
        switch (action) {
            case "back" -> page = "root";
            case "who", "rumours", "how_are_you", "shop_about",
                 "guild_info", "mail", "guard", "church" -> {
                // Dialogue slot deliberately empty.
            }
            case "work" -> {
                if (ModList.get().isLoaded("cyberquest")) {
                    invokeQuestWork(player, npc);
                }
            }
            case "shop" -> {
                page = "shop";
                player.sendSystemMessage(Component.literal(
                        "Coins: " + NpcShopService.walletDescription(player)));
            }
            case "shop_sell" -> player.sendSystemMessage(
                    Component.literal(NpcShopService.sellHeld(player)));
            case "buy_bread", "buy_torches", "buy_beef", "buy_arrows",
                 "buy_apples", "buy_leather_cap", "buy_iron_pickaxe" ->
                    player.sendSystemMessage(
                            Component.literal(NpcShopService.buy(player, action)));
            case "guild_register" -> {
                boolean registered = invokeGuild(player, "registerHeldContract");
                player.sendSystemMessage(Component.literal(registered
                        ? "Contract registered." : "Hold a contract to register."));
            }
            case "guild_claim" -> {
                boolean claimed = invokeGuild(player, "claimReadyContracts");
                player.sendSystemMessage(Component.literal(claimed
                        ? "Contract payments collected." : "No completed contracts to collect."));
            }
            case "guild_card" -> {
                // The CyberQuest membership screen still opens as before.
                if (invokeGuild(player, "issueOrShowGuildCard")) {
                    SESSIONS.remove(player.getUUID());
                    return;
                }
                player.sendSystemMessage(Component.literal("Guild card unavailable."));
            }
            default -> {
                return;
            }
        }

        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, playerLine, "");
    }

    private static boolean valid(ServerPlayer player, CyberNpcEntity npc) {
        // Defence in depth: even another mod or a stale client dialogue
        // session cannot make a Wild NPC speak or execute dialogue actions.
        return player != null && npc != null && npc.isAlive()
            && npc.getNpcType() != NpcType.WILD
            && player.level() == npc.level()
            && player.distanceToSqr(npc) <= MAX_RANGE_SQR;
    }

    private static void send(ServerPlayer player, CyberNpcEntity npc,
                             String page, String playerLine, String speech) {
        String role = npc.getRole().isBlank() ? "Citizen" : npc.getRole();
        CyberNpcNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new DialogueOpenPacket(new DialogueView(npc.getId(),
                npc.getName().getString(), role, playerLine, speech, options(npc, page))));
    }

    private static DialogueView.Option option(String id, String label) {
        return new DialogueView.Option(id, label);
    }

    private static List<DialogueView.Option> options(CyberNpcEntity npc, String page) {
        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        List<DialogueView.Option> options = new ArrayList<>();
        if (page.startsWith("intro_")) {
            String next = switch (page) {
                case "intro_0" -> "intro_town";
                case "intro_1" -> "intro_guild";
                case "intro_2" -> "intro_trade";
                case "intro_3" -> "intro_finish";
                default -> "intro_finish";
            };
            String label = switch (next) {
                case "intro_town" -> "Where am I?";
                case "intro_guild" -> "Where can I find work?";
                case "intro_trade" -> "How do I pay for things?";
                default -> "Thanks. I'll take it from here.";
            };
            options.add(option(next, label));
            options.add(option("intro_skip", "Skip introduction"));
            return List.copyOf(options);
        }
        if ("shop".equals(page)) {
            if (role == NpcServiceRole.SHOPKEEPER) {
                for (NpcShopService.Stock item : NpcShopService.stock()) {
                    options.add(option(item.action(), item.label()));
                }
            }
            options.add(option("back", "Something else..."));
            return List.copyOf(options);
        }

        switch (role) {
            case GUILD_RECEPTIONIST -> {
                options.add(option("guild_register", "Can you register this contract?"));
                options.add(option("guild_claim", "I'm here for my payment."));
                options.add(option("guild_card", "Can I see my guild card?"));
                options.add(option("guild_info", "How does the guild work?"));
            }
            case SHOPKEEPER -> {
                options.add(option("shop", "What have you got for sale?"));
                options.add(option("shop_sell", "Can I sell these?"));
                options.add(option("shop_about", "What coins do you take?"));
            }
            case BANKER -> {
                // Legacy banker NPCs may still exist in older saves.
                // They now offer ordinary conversation, not bank services.
                options.add(option("who", "Who are you?"));
                options.add(option("rumours", "Have you heard any news?"));
            }
            case COURIER -> {
                options.add(option("mail", "How can I send a letter?"));
                options.add(option("rumours", "Have you heard any news?"));
            }
            case GUARD -> {
                options.add(option("guard", "Is the town safe?"));
                options.add(option("rumours", "Has there been any trouble?"));
            }
            case POPE -> {
                options.add(option("church", "Could you give me a blessing?"));
                options.add(option("rumours", "Have you heard any news?"));
            }
            default -> {
                options.add(option("who", "Who are you?"));
                if (npc.getNpcType() == NpcType.QUEST || !npc.getStoryNpcId().isBlank()
                    || npc.getPersistentData().contains("CyberQuestBindings")) {
                    options.add(option("work", "Do you have any work for me?"));
                }
                options.add(option("rumours", "Have you heard any rumours?"));
                options.add(option("how_are_you", "How have you been?"));
            }
        }
        options.add(option("leave", "Farewell."));
        return List.copyOf(options);
    }

    private static boolean invokeGuild(ServerPlayer player, String methodName) {
        if (!ModList.get().isLoaded("cyberquest")) return false;
        try {
            Class<?> type = Class.forName(
                "com.cyberspectraa.cyberquest.guild.GuildContractManager");
            Method method = type.getMethod(methodName, ServerPlayer.class);
            Object result = method.invoke(null, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static void invokeQuestWork(ServerPlayer player, CyberNpcEntity npc) {
        try {
            Class<?> type = Class.forName(
                "com.cyberspectraa.cyberquest.quest.QuestNpcInteraction");
            type.getMethod("handle", ServerPlayer.class, Entity.class)
                .invoke(null, player, npc);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Quests are optional, and plain NPC conversations work without them.
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}
