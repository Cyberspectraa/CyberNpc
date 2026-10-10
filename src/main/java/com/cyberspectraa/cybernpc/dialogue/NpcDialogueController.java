package com.cyberspectraa.cybernpc.dialogue;

import net.minecraft.commands.arguments.EntityAnchorArgument;

import com.cyberspectraa.cybernpc.compat.CyberServerCompat;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.intro.CyberIntroService;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.DialogueOpenPacket;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Single conversation controller for every CyberNpc service and ordinary NPC.
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
        send(player, npc, page, "", greeting(npc));
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
        send(player, npc, page, "", "Welcome, summoned one. The world has called you here. "
                + "I am the Pope, and I'll help you find your place in our town.");
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
            String response = switch (next) {
                case "intro_1" -> "You stand in a town built to shelter travellers. "
                    + "The church offers guidance, the square leads to its markets, "
                    + "and our guards protect the people.";
                case "intro_2" -> "At the Adventurers' Guild, speak to the receptionist "
                    + "and check their notice board for contracts. Your journal "
                    + "records the leads you discover.";
                default -> "The shopkeeper trades for physical copper, silver and "
                    + "gold coins. Earn coins by trading and completing tasks. "
                    + "Now go, and write your own story.";
            };
            SESSIONS.put(player.getUUID(),
                new Session(npc.getUUID(), next, player.level().getGameTime() + SESSION_LIFETIME));
            send(player, npc, next, validOptions.stream()
                    .filter(o -> o.id().equals(action)).findFirst().orElseThrow().label(), response);
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
        String reply;

        switch (action) {
            case "back" -> {
                page = "root";
                reply = greeting(npc);
            }
            case "who" -> reply = "I'm " + npc.getName().getString()
                    + ". " + (npc.getRole().isBlank()
                    ? "Just making a living here." : "I work as a " + npc.getRole() + ".");
            case "rumours" -> reply = "The roads are never as quiet as they seem. "
                    + "Ask around if you're looking for work.";
            case "how_are_you" -> reply = "Still standing, thank you. "
                    + "A peaceful day would be a welcome change.";
            case "work" -> {
                if (ModList.get().isLoaded("cyberquest")) {
                    invokeQuestWork(player, npc);
                    reply = "I've told you what I know. Check your journal for any new leads.";
                } else {
                    reply = "The guild may know of work for an adventurer.";
                }
            }
            case "shop" -> {
                page = "shop";
                reply = "All prices are paid with CyberNpc coins from your inventory. "
                        + "Your wallet: " + NpcShopService.walletDescription(player) + ".";

            }
            case "shop_about" -> reply = "I only accept CyberNpc coins, not emeralds or bank credit. "
                    + "10 copper = 1 silver, 10 silver = 1 gold; platinum and dragon coins "
                    + "work too. I'll give change in coins.";

            case "shop_sell" -> reply = NpcShopService.sellHeld(player);
            case "buy_bread", "buy_torches", "buy_beef", "buy_arrows",
                 "buy_apples", "buy_leather_cap", "buy_iron_pickaxe" ->
                    reply = NpcShopService.buy(player, action);
            case "guild_register" -> {
                boolean success = invokeGuild(player, "registerHeldContract");
                reply = success
                    ? "Your contract is registered. Follow the lead in your journal."
                    : "Hold an unregistered guild contract and ask me to register it.";
            }
            case "guild_claim" -> {
                boolean success = invokeGuild(player, "claimReadyContracts");
                reply = success
                    ? "The guild has recorded your completed contracts."
                    : "There are no completed guild contracts ready to claim.";
            }
            case "guild_card" -> {
                // CyberQuest opens its own membership screen. Do not overwrite it.
                if (invokeGuild(player, "issueOrShowGuildCard")) {
                    SESSIONS.remove(player.getUUID());
                    return;
                }
                reply = "Guild membership cards are issued here to adventurers.";
            }
            case "guild_info" -> reply = "Take a notice from the board and bring it here "
                    + "to register. Return after completing the work to receive payment.";
            case "mail" -> reply = "Letters travel through the post office. "
                    + "Address and seal them, then use a drop box to send them.";
            case "guard" -> reply = "Stay clear of trouble, traveller. "
                    + "The guards are watching the roads.";
            case "church" -> reply = "All are welcome beneath this roof. "
                    + "May your travels be safe.";
            default -> {
                return;
            }
        }

        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, playerLine, reply);
    }

    private static boolean valid(ServerPlayer player, CyberNpcEntity npc) {
        return player != null && npc != null && npc.isAlive()
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
                case "intro_town" -> "Where have I been summoned?";
                case "intro_guild" -> "How do I find work?";
                case "intro_trade" -> "How do shops and coins work?";
                default -> "I'm ready to begin.";
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
            options.add(option("back", "I'd like to ask something else."));
            return List.copyOf(options);
        }

        switch (role) {
            case GUILD_RECEPTIONIST -> {
                options.add(option("guild_register", "I'd like to register this contract."));
                options.add(option("guild_claim", "I'd like to collect my rewards."));
                options.add(option("guild_card", "May I see my guild card?"));
                options.add(option("guild_info", "How does the guild work?"));
            }
            case SHOPKEEPER -> {
                options.add(option("shop", "Show me what you have for sale."));
                options.add(option("shop_sell", "I'd like to sell these materials."));
                options.add(option("shop_about", "How do your prices work?"));
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
                options.add(option("church", "Could I have a blessing?"));
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

    private static String greeting(CyberNpcEntity npc) {
        return switch (NpcServiceRole.fromRole(npc.getRole())) {
            case GUILD_RECEPTIONIST -> "Welcome to the Adventurers' Guild. "
                    + "Are you here to register a contract or collect payment?";
            case SHOPKEEPER -> "Welcome! Take a look at what I've got for sale.";
            case BANKER -> "Greetings! The bank is no longer in service.";
            case COURIER -> "Letters, packages, deliveries. Can I help?";
            case GUARD -> "Halt a moment, traveller. Everything all right?";
            case POPE -> "Peace be with you. What troubles your heart?";
            default -> npc.getNpcType() == NpcType.WILD
                ? "Well met. Don't mind the road dust."
                : "Greetings, traveller. What can I do for you?";
        };
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
