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

    /**
     * Only NPCs with approved dialogue or a required live service can open
     * the dialogue UI. All other unfinished NPCs have no right-click menu.
     */
    public static boolean canInteract(CyberNpcEntity npc) {
        if (npc == null || npc.getNpcType() == NpcType.WILD) return false;
        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        return role == NpcServiceRole.POPE
                || role == NpcServiceRole.GUILD_RECEPTIONIST;
    }

    public static void open(ServerPlayer player, CyberNpcEntity npc) {
        if (!valid(player, npc) || !canInteract(npc)) return;
        String page = "root";
        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, "", pope(npc) ? popeGreeting()
                : guildGreeting(player));
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
        send(player, npc, page, "", popeIntroLine(page));
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
        List<DialogueView.Option> validOptions = options(player, npc, session.page());
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
                case "intro_curse" -> "intro_2";
                case "intro_wilderness" -> "intro_3";
                case "intro_north" -> "intro_4";
                case "intro_guild" -> "intro_5";
                case "intro_trade" -> "intro_6";
                case "intro_advice" -> "intro_7";
                default -> null;
            };
            if (next == null) return;
            SESSIONS.put(player.getUUID(),
                new Session(npc.getUUID(), next, player.level().getGameTime() + SESSION_LIFETIME));
            send(player, npc, next, validOptions.stream()
                    .filter(o -> o.id().equals(action)).findFirst().orElseThrow().label(),
                    popeIntroLine(next));
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
        String npcReply = "";
        switch (action) {
            case "back" -> {
                page = "root";
                if (pope(npc)) npcReply = popeGreeting();
            }
            case "pope_arrival", "pope_curse", "pope_north", "pope_begin",
                 "pope_dangers", "pope_trade" -> {
                if (!pope(npc)) return;
                npcReply = popeInformation(action);
            }
            case "who", "rumours", "how_are_you", "shop_about",
                 "guild_info", "mail", "guard" -> {
                // Other NPCs remain silent until their dialogue is written.
            }
            case "church" -> {
                if (pope(npc)) npcReply = "These doors are open to anyone in need of a quiet moment. I can offer guidance, but I cannot promise miracles.";
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
            case "guild_class" -> {
                // Opening the class screen replaces the dialogue screen.
                // Do not send a second dialogue packet over it.
                if (openGuildClass(player)) {
                    SESSIONS.remove(player.getUUID());
                    return;
                }
                player.sendSystemMessage(Component.literal(
                        "Guild class registration is not available right now."));
            }
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
        send(player, npc, page, playerLine, npcReply);
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
                npc.getName().getString(), role, playerLine, speech, options(player, npc, page))));
    }

    private static DialogueView.Option option(String id, String label) {
        return new DialogueView.Option(id, label);
    }

    private static List<DialogueView.Option> options(
            ServerPlayer player, CyberNpcEntity npc, String page) {
        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        List<DialogueView.Option> options = new ArrayList<>();
        if (page.startsWith("intro_")) {
            String next = switch (page) {
                case "intro_0" -> "intro_town";
                case "intro_1" -> "intro_curse";
                case "intro_2" -> "intro_wilderness";
                case "intro_3" -> "intro_north";
                case "intro_4" -> "intro_guild";
                case "intro_5" -> "intro_trade";
                case "intro_6" -> "intro_advice";
                default -> "intro_finish";
            };
            String label = switch (next) {
                case "intro_town" -> "Where am I?";
                case "intro_curse" -> "Why do people look different?";
                case "intro_wilderness" -> "What lies beyond town?";
                case "intro_north" -> "What's wrong with the north?";
                case "intro_guild" -> "Where should I begin?";
                case "intro_trade" -> "How do I pay for things?";
                case "intro_advice" -> "Any last advice?";
                default -> "Thank you. I'll be careful.";
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
                // The receptionist handles class registration for new
                // arrivals. CyberClasses validates eligibility server-side.
                if (ModList.get().isLoaded("cyberclasses")
                        && !player.getPersistentData().getCompound("CyberClasses")
                                .getBoolean("ClassChosen")) {
                    options.add(option("guild_class", "I'd like to register my class."));
                }
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
                options.add(option("pope_arrival", "Why was I summoned?"));
                options.add(option("pope_curse", "Why aren't we human?"));
                options.add(option("pope_north", "What lies to the north?"));
                options.add(option("pope_begin", "Where should I start?"));
                options.add(option("pope_dangers", "What dangers should I expect?"));
                options.add(option("pope_trade", "How does trading work?"));
                options.add(option("church", "Tell me about the church."));
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

    private static boolean pope(CyberNpcEntity npc) {
        return NpcServiceRole.fromRole(npc.getRole()) == NpcServiceRole.POPE;
    }

    private static String guildGreeting(ServerPlayer player) {
        return player.getPersistentData().getCompound("CyberClasses")
                .getBoolean("ClassChosen")
                ? "Welcome to the guild. Have a contract to register or a reward to collect?"
                : "New to the island? Register your class here. Starter supplies are optional.";
    }

    private static String popeGreeting() {
        return "Welcome back. If you have questions about the island or our town, ask. I'll share what I know.";
    }

    /**
     * Fixed, grounded first-arrival information from the island's Pope.
     * He witnessed the summoning but DID NOT cause it and DOES NOT know why.
     * The north warning is the island's story placement of Aquamirae content.
     * Keep the intro linear so the existing Skip/Finish controls stay reliable.
     */
    private static String popeIntroLine(String page) {
        return switch (page) {
            case "intro_0" -> "Easy now. I saw you arrive in that light, but I did not summon you. I wish I knew who did, or why.";
            case "intro_1" -> "You're on a vast island. Our town is a place to rest, trade, and find help before travelling into the wilds.";
            case "intro_2" -> "We were all human once. This island changes us over time, though none of us knows why. I remember people before they changed. They're still themselves.";
            case "intro_3" -> "Much of this island is dangerous. Take food and proper equipment. Above all, don't wander too far north.";
            case "intro_4" -> "Beyond the northern ice lie wrecked ships. Something they call the Shipbreaker moves through those waters. Stay clear of it.";
            case "intro_5" -> "Your first task should be to register a class at the Adventurers' Guild. They'll offer basic equipment, though you're free to refuse it.";
            case "intro_6" -> "Our shops take coins, not emeralds. You can earn them through contracts and trade. Don't set off empty-handed.";
            case "intro_7" -> "Strange beasts and dangerous magic roam this island. Start near town. Ask the guild for training and come back if you need guidance.";
            default -> "";
        };
    }

    /** Informational only: these responses never grant items or trigger quests. */
    private static String popeInformation(String action) {
        return switch (action) {
            case "pope_arrival" -> "I didn't perform the summoning, nor do I know who called you here. All I can say is that I saw the light.";
            case "pope_curse" -> "Every one of us was human once. The island slowly changes people, but it does not erase our memories. I have seen it happen and still cannot tell you why.";
            case "pope_north" -> "Avoid the frozen waters far to the north. A terror called the Shipbreaker haunts the wrecks. Don't go looking for it.";
            case "pope_begin" -> "Your first task is to register at the Adventurers' Guild. They can offer basic supplies, but accepting them is your choice.";
            case "pope_dangers" -> "Travellers speak of fierce beasts, strange creatures and dangerous magic. Some tales may be exaggerated. Be cautious all the same.";
            case "pope_trade" -> "Merchants trade in copper, silver and gold coins, not emeralds. Work and trade are good ways to earn your first few.";
            default -> "";
        };
    }

    private static boolean openGuildClass(ServerPlayer player) {
        if (!ModList.get().isLoaded("cyberclasses")) return false;
        try {
            Class<?> manager = Class.forName(
                    "com.cyberspectraa.cyberclasses.classdata.ClassCreationManager");
            return Boolean.TRUE.equals(manager
                    .getMethod("openAtGuild", ServerPlayer.class).invoke(null, player));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
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
