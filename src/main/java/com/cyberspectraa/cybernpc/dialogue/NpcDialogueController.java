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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
    // One speaking partner per NPC; only NPCs actually in a conversation
    // need to update their look direction. Never scan all players every tick.
    private static final Map<UUID, UUID> FACING = new HashMap<>();

    private record Session(UUID npcUuid, String page, long expiresAt) {}

    private NpcDialogueController() {}

    private static void clearSession(ServerPlayer player) {
        Session previous = SESSIONS.remove(player.getUUID());
        if (previous != null) {
            FACING.remove(previous.npcUuid(), player.getUUID());
        }
    }

    /** Used for explicit Close, Escape and leaving a conversation. */
    public static void close(ServerPlayer player, int npcId) {
        if (player == null) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        Entity target = player.level().getEntity(npcId);
        if (target instanceof CyberNpcEntity npc
                && npc.getUUID().equals(session.npcUuid())) {
            clearSession(player);
        }
    }

    /**
     * The relationship screen is an ordinary, non-pausing GUI. Idle and
     * service AI would otherwise turn the character away between lines.
     * Run after service AI (server side) so the head and torso steadily face
     * the active conversation partner, then release immediately on Close.
     */
    public static void tickConversationFacing(CyberNpcEntity npc) {
        if (npc == null || npc.getNpcType() == NpcType.WILD
                || !(npc.level() instanceof ServerLevel level)) return;
        UUID playerId = FACING.get(npc.getUUID());
        if (playerId == null) return;

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        Session session = SESSIONS.get(playerId);
        if (player == null || session == null
                || !npc.getUUID().equals(session.npcUuid())
                || level.getGameTime() > session.expiresAt()
                || !valid(player, npc)) {
            FACING.remove(npc.getUUID(), playerId);
            return;
        }

        npc.getLookControl().setLookAt(player, 30.0F, 30.0F);
        double dx = player.getX() - npc.getX();
        double dz = player.getZ() - npc.getZ();
        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float yaw = Mth.rotLerp(0.18F, npc.getYRot(), targetYaw);
        npc.setYRot(yaw);
        npc.setYBodyRot(Mth.rotLerp(0.18F, npc.yBodyRot, targetYaw));
        npc.setYHeadRot(Mth.rotLerp(0.22F, npc.getYHeadRot(), targetYaw));
    }

    /**
     * A stale or expired dialogue choice should never strand the local
     * interface in an endless waiting state. Regenerate safe root options
     * without advancing quests or awarding relationship points.
     */
    private static void recover(ServerPlayer player, int npcId) {
        Entity candidate = player.level().getEntity(npcId);
        if (candidate instanceof CyberNpcEntity npc
                && valid(player, npc) && canInteract(npc)) {
            clearSession(player);
            SESSIONS.put(player.getUUID(), new Session(
                    npc.getUUID(), "root",
                    player.level().getGameTime() + SESSION_LIFETIME));
            send(player, npc, "root", "",
                    "Sorry, I lost my train of thought. What were we discussing?");
            return;
        }
        CyberNpcNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new DialogueOpenPacket(new DialogueView(npcId,
                    "Conversation", "", "",
                    "The conversation has ended. Please approach the NPC to talk again.",
                    List.of(option("leave", "Close")), null)));
    }

    /**
     * Only NPCs with approved dialogue or a required live service can open
     * the dialogue UI. All other unfinished NPCs have no right-click menu.
     */
    public static boolean canInteract(CyberNpcEntity npc) {
        if (npc == null || npc.getNpcType() == NpcType.WILD) return false;
        if (TownCharacterDialogue.recognises(npc)) return true;
        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        return role == NpcServiceRole.POPE
                || role == NpcServiceRole.GUILD_RECEPTIONIST;
    }

    public static void open(ServerPlayer player, CyberNpcEntity npc) {
        if (!valid(player, npc) || !canInteract(npc)) return;
        clearSession(player);
        String page = "root";
        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, "", TownCharacterDialogue.recognises(npc)
                ? TownCharacterDialogue.greeting(player, npc)
                : (pope(npc) ? popeGreeting() : guildGreeting(player)));
    }

    public static void openIntro(ServerPlayer player, CyberNpcEntity npc) {
        if (!CyberIntroService.isGreeting(player) || !valid(player, npc)) return;
        // Restore normal first-person gameplay looking toward the Pope.
        // ServerPlayer.lookAt synchronises the orientation to the client;
        // it does not move the player's spawn position or camera entity.
        player.lookAt(EntityAnchorArgument.Anchor.EYES,
                npc, EntityAnchorArgument.Anchor.EYES);
        clearSession(player);
        String page = "intro_0";
        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, "", popeIntroLine(player, page));
    }

    public static void choose(ServerPlayer player, int npcId, String action) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || player.level().getGameTime() > session.expiresAt()) {
            clearSession(player);
            recover(player, npcId);
            return;
        }

        Entity target = player.level().getEntity(npcId);
        if (!(target instanceof CyberNpcEntity npc)
                || !npc.getUUID().equals(session.npcUuid()) || !valid(player, npc)) {
            clearSession(player);
            recover(player, npcId);
            return;
        }

        // Never execute a client-supplied action without confirming that the
        // currently active, server-generated page actually offered it.
        List<DialogueView.Option> validOptions = options(player, npc, session.page());
        if (validOptions.stream().noneMatch(o -> o.id().equals(action))) {
            // Choices can change because of a quest/relationship update.
            // Refresh current options; do not execute the outdated action.
            send(player, npc, session.page(), "",
                    "I didn't catch that. Would you ask again?");
            return;
        }

        if (session.page().startsWith("intro_")) {
            if (!CyberIntroService.isGreeting(player)) {
                recover(player, npcId);
                return;
            }
            if ("intro_skip".equals(action) || "intro_finish".equals(action)) {
                clearSession(player);
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
                    popeIntroLine(player, next));
            return;
        }

        if ("leave".equals(action)) {
            clearSession(player);
            return;
        }

        String playerLine = validOptions.stream()
            .filter(o -> o.id().equals(action))
            .findFirst().orElseThrow().label();
        String page = session.page();

        if (TownCharacterDialogue.recognises(npc)) {
            TownCharacterDialogue.Transition reply =
                    TownCharacterDialogue.advance(player, npc, page, action);
            SESSIONS.put(player.getUUID(), new Session(npc.getUUID(), reply.page(),
                    player.level().getGameTime() + SESSION_LIFETIME));
            send(player, npc, reply.page(), playerLine, reply.speech());
            return;
        }

        // Written dialogue is restored only for the authored Pope and
        // Guild Receptionist. Other NPC roles still have no right-click menu.
        String npcReply = "";
        switch (action) {
            case "back" -> {
                page = "root";
                if (pope(npc)) npcReply = popeGreeting();
                else if (guildReceptionist(npc)) npcReply = guildGreeting(player);
            }
            case "pope_arrival", "pope_curse", "pope_north", "pope_begin",
                 "pope_dangers", "pope_trade", "pope_spell", "pope_limits",
                 "pope_unchanged", "pope_change_later", "pope_past" -> {
                if (!pope(npc)) return;
                // Keep existing CyberQuest topic IDs; branch only Pope's
                // arrival and curse conversations, with server-checked replies.
                if ("pope_arrival".equals(action) || "pope_curse".equals(action)) {
                    page = action;
                }
                npcReply = popeInformation(player, action);
            }
            case "guild_info" -> {
                if (!guildReceptionist(npc)) return;
                npcReply = "Take a notice from the guild board and hold it in your main hand. "
                        + "I'll register it for you. You can have two at a time. "
                        + "Finish the work, then return here to collect your reward.";
            }
            case "who", "rumours", "how_are_you", "shop_about",
                 "mail", "guard" -> {
                // Other NPCs remain without authored spoken dialogue.
            }
            case "church" -> {
                if (!pope(npc)) return;
                page = "pope_arrival";
                npcReply = "This church is a sanctuary. I studied magic so people "
                        + "torn between worlds could arrive somewhere safe instead "
                        + "of alone in the wilds. The spell is imperfect, but "
                        + "these doors are open to everyone.";
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
                if (!guildReceptionist(npc)) return;
                // CyberClasses opens its own class creation screen and handles
                // the optional starter kit. Do not overwrite that screen.
                if (openGuildClass(player)) {
                    clearSession(player);
                    return;
                }
                npcReply = "I can't open class registration yet. You'll need to "
                        + "finish the arrival introduction first. If that's already "
                        + "done, please speak to the guild again.";
            }
            case "guild_register" -> {
                if (!guildReceptionist(npc)) return;
                boolean registered = invokeGuild(player, "registerHeldContract");
                npcReply = registered
                        ? "It's on the books. Your contract is now active. "
                            + "Check your journal for its objectives, and return to me when it's done."
                        : "I couldn't register that notice. Hold an unused guild "
                            + "contract in your main hand. If you already are, "
                            + "check your contract limit and the notice's validity.";
            }
            case "guild_claim" -> {
                if (!guildReceptionist(npc)) return;
                boolean claimed = invokeGuild(player, "claimReadyContracts");
                npcReply = claimed
                        ? "Well done! Your completed contract has been recorded, "
                            + "and the guild has paid your reward. Safe travels."
                        : "I don't have any finished contracts to pay out just yet. "
                            + "Check your journal and come back once the work is complete.";
            }
            case "guild_card" -> {
                if (!guildReceptionist(npc)) return;
                // The CyberQuest guild card screen replaces this conversation.
                if (invokeGuild(player, "issueOrShowGuildCard")) {
                    clearSession(player);
                    return;
                }
                npcReply = "I can't reach the guild records just now. "
                        + "Please try again in a moment.";
            }
            default -> {
                return;
            }
        }

        SESSIONS.put(player.getUUID(),
            new Session(npc.getUUID(), page, player.level().getGameTime() + SESSION_LIFETIME));
        npc.getLookControl().setLookAt(player, 25.0F, 25.0F);
        send(player, npc, page, playerLine, npcReply);
        // Only validated, authored dialogue choices may advance onboarding.
        invokeTownIntroduction(player, action);
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
        FACING.put(npc.getUUID(), player.getUUID());
        String role = npc.getRole().isBlank() ? "Citizen" : npc.getRole();
        String safeSpeech = speech == null || speech.isBlank()
                ? "What would you like to know?" : speech;
        CyberNpcNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new DialogueOpenPacket(new DialogueView(npc.getId(),
                npc.getName().getString(), role, playerLine, safeSpeech,
                options(player, npc, page),
                TownCharacterDialogue.relationship(player, npc))));
    }

    private static DialogueView.Option option(String id, String label) {
        return new DialogueView.Option(id, label);
    }

    private static List<DialogueView.Option> options(
            ServerPlayer player, CyberNpcEntity npc, String page) {
        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        List<DialogueView.Option> options = new ArrayList<>();
        if (TownCharacterDialogue.recognises(npc)) {
            return TownCharacterDialogue.options(player, npc, page);
        }
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
        if (pope(npc) && "pope_arrival".equals(page)) {
            options.add(option("pope_spell", "How did you learn to redirect arrivals?"));
            options.add(option("pope_limits", "Can your magic save everyone?"));
            options.add(option("church", "Why make the church a sanctuary?"));
            options.add(option("back", "Ask about something else."));
            options.add(option("leave", "Farewell."));
            return List.copyOf(options);
        }
        if (pope(npc) && "pope_curse".equals(page)) {
            options.add(option("pope_change_later", "Could the curse change me later?"));
            options.add(option("pope_unchanged", "Were you transformed when you arrived?"));
            options.add(option("pope_past", "Can you tell who was transformed?"));
            options.add(option("back", "Ask about something else."));
            options.add(option("leave", "Farewell."));
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
                options.add(option("pope_arrival", "How did I arrive at this church?"));
                options.add(option("pope_curse", "What happened to the changed people?"));
                options.add(option("pope_north", "What lies to the north?"));
                options.add(option("pope_begin", "Where should I start?"));
                options.add(option("pope_dangers", "What dangers should I expect?"));
                options.add(option("pope_trade", "How does trading work?"));
                options.add(option("church", "Why is this church a sanctuary?"));
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

    private static boolean guildReceptionist(CyberNpcEntity npc) {
        return NpcServiceRole.fromRole(npc.getRole())
                == NpcServiceRole.GUILD_RECEPTIONIST;
    }

    private static String guildGreeting(ServerPlayer player) {
        return player.getPersistentData().getCompound("CyberClasses")
                .getBoolean("ClassChosen")
                ? "Welcome back to the Adventurers' Guild! Need to register a "
                    + "contract, collect your payment, or look at your guild card?"
                : "Welcome to the Adventurers' Guild! I can register your "
                    + "first class. Starter equipment is offered, but "
                    + "accepting it is entirely your choice.";
    }

    private static String popeGreeting() {
        return "Welcome back. The church is always a safe place to rest. "
                + "If you have questions about the fracture or the island, "
                + "I'll tell you what I know.";
    }

    /**
     * The fracture pulls people into the island. The Pope does not summon:
     * after arriving unchanged, he learned to redirect some travellers
     * to the church rather than leaving them in the wilds.
     *
     * Preserve the linear introduction and its existing Skip/Finish controls.
     */
    private static String popeIntroLine(ServerPlayer player, String page) {
        return switch (page) {
            case "intro_0" -> "Easy now. You're safe. The fracture brought you "
                    + "to this island. My magic only guided your arrival "
                    + "into this church; I did not summon you.";
            case "intro_1" -> "You're on a vast island. I learned to guide "
                    + "new arrivals here after too many were stranded in "
                    + "dangerous places. My spell cannot catch everyone.";
            case "intro_2" -> popeCurseIntro(player);
            case "intro_3" -> "Beyond our town the wilderness is dangerous. "
                    + "Take food and proper equipment, and don't wander "
                    + "too far north until you're prepared.";
            case "intro_4" -> "Beyond the northern ice lie wrecked ships. "
                    + "Something they call the Shipbreaker moves through "
                    + "those waters. Stay clear of it.";
            case "intro_5" -> "Your first task should be to register a class "
                    + "at the Adventurers' Guild. They'll offer basic "
                    + "equipment, though you're free to refuse it.";
            case "intro_6" -> "Our shops take coins, not emeralds. You can "
                    + "earn them through contracts and trade. Don't set "
                    + "off empty-handed.";
            case "intro_7" -> "Strange beasts and dangerous magic roam this "
                    + "island. Start near town. Ask the guild for training "
                    + "and come back if you need guidance.";
            default -> "";
        };
    }

    /** A race alone doesn't prove whether the crossing changed someone. */
    private static String popeCurseIntro(ServerPlayer player) {
        String race = player.getPersistentData().getCompound("CyberRaces")
                .getString("Race");
        String explanation = "The fracture can change someone while they "
                + "cross between worlds, never after arrival. ";
        if ("human".equals(race)) {
            return explanation + "You look human, but I cannot know what "
                    + "you were before crossing.";
        }
        if (!race.isBlank()) {
            return explanation + "Your race alone cannot tell me whether "
                    + "you were changed. Only you know your past.";
        }
        return explanation + "I arrived unchanged, but others weren't so lucky.";
    }

    /** Informational only; no dialogue action triggers Rift or gameplay. */
    private static String popeInformation(ServerPlayer player, String action) {
        return switch (action) {
            case "pope_arrival" -> "The island's fracture pulls people here "
                    + "from other worlds. I cannot choose who comes. I only "
                    + "redirect some arrivals toward the safety of this church.";
            case "pope_curse" -> popeCurseIntro(player);
            case "pope_spell" -> "When I first came here, I saw newcomers "
                    + "appear alone in dangerous places. I spent years "
                    + "studying magic before I learned to anchor an arrival "
                    + "to this church. It is a shelter, not a summoning.";
            case "pope_limits" -> "My spell can influence where someone "
                    + "lands, not who is pulled here or what happens to them "
                    + "during the crossing. It cannot catch everyone; "
                    + "some still arrive in the wilds.";
            case "pope_unchanged" -> "I arrived exactly as I was before. "
                    + "Kai and several others in town did too. We believe "
                    + "we were simply fortunate; I know of no protection "
                    + "that made us different.";
            case "pope_change_later" -> "Not from the fracture's curse. "
                    + "If a person is transformed, it happens during the "
                    + "journey between worlds. Once here, their form is stable.";
            case "pope_past" -> "Not by looking at someone. Some travellers "
                    + "were born as they are; others crossed in a changed "
                    + "form. Their memories of their former lives are "
                    + "their own to share.";
            case "pope_north" -> "Avoid the frozen waters far to the north. "
                    + "A terror called the Shipbreaker haunts the wrecks. "
                    + "Don't go looking for it.";
            case "pope_begin" -> "Start at the Adventurers' Guild. "
                    + "Register a class and learn how to take contracts. "
                    + "They can offer basic supplies; accepting them is your choice.";
            case "pope_dangers" -> "Travellers speak of fierce beasts, "
                    + "strange creatures and dangerous magic. Some tales "
                    + "may be exaggerated. Be cautious all the same.";
            case "pope_trade" -> "Merchants trade in copper, silver and "
                    + "gold coins, not emeralds. Work and trade are good "
                    + "ways to earn your first few.";
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

    private static void invokeTownIntroduction(ServerPlayer player, String action) {
        if (!ModList.get().isLoaded("cyberquest")) return;
        try {
            Class<?> type = Class.forName(
                    "com.cyberspectraa.cyberquest.quest.TownIntroductionQuests");
            type.getMethod("recordDialogue", ServerPlayer.class, String.class)
                    .invoke(null, player, action);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // CyberQuest is optional; normal dialogue must work without it.
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
        if (event.getEntity() instanceof ServerPlayer player) {
            clearSession(player);
        }
    }
}
