package com.cyberspectraa.cybernpc.dialogue;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lightweight, event-driven conversations for the seven authored Town NPCs.
 * Story IDs are never renamed: "mason" stays the persisted identity of Kai.
 * No dialogue, relationship or mood work runs in an entity tick.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID)
public final class TownCharacterDialogue {
    private static final String DATA_KEY = "CyberNpcTownCharacters";
    private static final ResourceLocation KATIE_QUEST =
            new ResourceLocation("cyberquest", "katie_yarn");

    public record Transition(String page, String speech) {}
    private record Topic(String name, String introduction, String detail) {}
    private record Profile(String name, String hello, String returnHello,
                           String mood, String town, String chat, List<Topic> topics) {}

    private static Topic topic(String name, String intro, String detail) {
        return new Topic(name, intro, detail);
    }

    private static final Map<String, Profile> PROFILES = Map.of(
        "mason", new Profile("Kai", "Oh. You're new.", "What do you want?",
            "I'm alright.", "It's a town. Not much else to say about it.",
            "Depends what you want to talk about.", List.of(
                topic("Choso and Jujutsu Kaisen", "Choso is my favourite. By a lot.",
                    "Gojo and Megumi are good too. But Choso is Choso."),
                topic("Other anime", "I've seen SK8 the Infinity, Banana Fish, Blue Lock and Sasaki and Miyano.",
                    "Reki and Langa. Ash. Sasaki. Those are some of my favourites."),
                topic("Music and musicals", "I listen to music a lot.",
                    "EPIC: The Musical and Mamma Mia! are both good."),
                topic("Games and mythology", "I play Fortnite, Roblox and HADES.",
                    "Greek mythology is interesting. I know quite a lot about it."),
                topic("Foxes and favourite characters", "Foxes are great.",
                    "Lucifer from Hazbin Hotel is one of my favourites too."))),
        "town_npc_1", new Profile("Katie", "Oh, you're the new person. Hi.", "Oh, hi again.",
            "Fine. What are you doing?", "There's usually something happening. Whether it's useful is another question.",
            "Are you free? Actually, what are you doing right now?", List.of(
                topic("Crocheting", "I crochet. I've been working on something lately.",
                    "It's harder to get anything finished when people keep interrupting."),
                topic("Disney films", "I really like Disney films.",
                    "I've got opinions. Quite a few, actually."),
                topic("Making plans", "Are you busy later?", "Good. I might need someone to come with me."))),
        "town_npc_2", new Profile("Elara", "You're new, aren't you? Have you explored much?",
            "Found anything interesting yet?", "Restless. I want to get out and see more.",
            "A useful place to return to. Not a good reason to stop exploring.",
            "Tell me about somewhere you've been.", List.of(
                topic("Old ruins", "Ruins are worth investigating. Carefully, preferably.",
                    "The difficult part is knowing what's history and what's just a story."),
                topic("Strange creatures", "I've heard of some unusual creatures around here.",
                    "I'd rather see evidence than trust a traveller's dramatic account."),
                topic("Maps and geography", "I like knowing where things are.",
                    "If you find somewhere useful, an actual location would be nice."))),
        "town_npc_3", new Profile("Rowan", "Hello. Welcome to town.", "Good to see you again.",
            "Not bad, thanks. And you?", "It's pleasant when things are quiet.",
            "We don't have to talk about anything in particular.", List.of(
                topic("Animals", "I've been watching the birds.",
                    "You learn a lot just by being patient with animals."),
                topic("Cooking", "I enjoy cooking. Nothing complicated, usually.",
                    "Food tastes better when you've made it yourself."),
                topic("Forests and fishing", "I spend time by the trees or the water when I can.",
                    "It's nice not having to rush everywhere."))),
        "town_npc_4", new Profile("Felix", "Ah, a new face. This town needed one.",
            "There you are. Miss me?", "Better now someone has come to entertain me.",
            "It has its moments. Usually when something goes wrong.",
            "I could tell you a story. The accurate version might be less exciting.", List.of(
                topic("Card games", "Fancy a game sometime?", "I never said I'd be easy to beat."),
                topic("Music and fashion", "Presentation matters. So does a good tune.",
                    "You can be practical and still look decent, you know."),
                topic("Stories and gossip", "I hear things. Not all of them are true.",
                    "I'll tell you what I know, but don't quote me on the dramatic bits."))),
        "town_npc_5", new Profile("Nadia", "New in town? Try not to break anything.",
            "Hello. Need something?", "Busy. Which is generally a good sign.",
            "Functional enough. Some things could be organised better.",
            "Tell me the problem. We can probably work it out.", List.of(
                topic("Engineering and redstone", "I like building things that actually work.",
                    "A neat mechanism is better than a complicated one nobody can maintain."),
                topic("Puzzles", "I enjoy a problem with a proper solution.",
                    "Guessing is fine as a start. Testing is better."),
                topic("Planning ahead", "Preparation saves a lot of trouble.",
                    "Especially when people insist on running straight into danger."))),
        "town_npc_6", new Profile("Orin", "Oh. Hello. I was thinking about something.",
            "Hello again. Still curious?", "Thoughtful, I suppose.",
            "Places always seem ordinary until you look closely.",
            "Do you ever wonder why things here are the way they are?", List.of(
                topic("Stars and the moon", "I like watching the night sky.",
                    "There's more to look at than most people notice."),
                topic("Magic and folklore", "Stories about magic fascinate me.",
                    "I don't assume every story is true. That doesn't stop me listening."),
                topic("Unusual objects", "Strange little objects are interesting.",
                    "Sometimes an object is just an object. Sometimes it isn't.")))
    );

    private TownCharacterDialogue() {}

    public static boolean recognises(CyberNpcEntity npc) {
        return npc != null && PROFILES.containsKey(npc.getStoryNpcId());
    }

    public static String displayName(String stableStoryId) {
        Profile profile = PROFILES.get(stableStoryId);
        return profile == null ? null : profile.name();
    }

    public static String originalName(String stableStoryId) {
        return switch (stableStoryId) {
            case "mason" -> "Mason";
            case "town_npc_1" -> "Town NPC 1";
            case "town_npc_2" -> "Town NPC 2";
            case "town_npc_3" -> "Town NPC 3";
            case "town_npc_4" -> "Town NPC 4";
            case "town_npc_5" -> "Town NPC 5";
            case "town_npc_6" -> "Town NPC 6";
            default -> null;
        };
    }

    public static String greeting(ServerPlayer player, CyberNpcEntity npc) {
        Profile profile = PROFILES.get(npc.getStoryNpcId());
        CompoundTag state = state(player, npc.getStoryNpcId());
        if (state.getLong("Cooldown") > player.level().getGameTime()) {
            return "Not right now.";
        }
        int visits = state.getInt("Visits");
        state.putInt("Visits", Math.min(visits + 1, Integer.MAX_VALUE));
        return visits == 0 ? profile.hello() : profile.returnHello();
    }

    public static List<DialogueView.Option> options(ServerPlayer player,
                                                     CyberNpcEntity npc, String page) {
        String id = npc.getStoryNpcId();
        Profile profile = PROFILES.get(id);
        if (profile == null) return List.of();
        List<DialogueView.Option> out = new ArrayList<>();
        if (state(player, id).getLong("Cooldown") > player.level().getGameTime()
                || "walk".equals(page)) {
            out.add(option("leave", "Leave them alone."));
            return List.copyOf(out);
        }
        if ("topics".equals(page)) {
            for (int i = 0; i < profile.topics().size(); i++) {
                out.add(option("talk_" + i, profile.topics().get(i).name()));
            }
            out.add(option("back", "Something else."));
        } else if (page.startsWith("talk_")) {
            out.add(option("more", "Tell me more."));
            out.add(option("agree", "I like that too."));
            out.add(option("disagree", "Not really my thing."));
            out.add(option("insult", "That's rubbish."));
            out.add(option("topics", "Another topic."));
            out.add(option("back", "Something else."));
        } else if ("yarn_offer".equals(page)) {
            out.add(option("accept_yarn", "Yeah, I can make some."));
            out.add(option("back", "Maybe another time."));
        } else {
            out.add(option("topics", "What do you like talking about?"));
            out.add(option("mood", "How are you?"));
            out.add(option("town", "What do you think of this place?"));
            out.add(option("chat", "Anything on your mind?"));
            if ("town_npc_1".equals(id)) {
                if (questActive(player)) {
                    out.add(option("yarn_turnin", "I've been making that yarn."));
                } else if (!questCompleted(player) && state(player, id).getBoolean("Seen_0")) {
                    out.add(option("yarn_offer", "You mentioned needing some yarn?"));
                }
            }
        }
        out.add(option("leave", "See you."));
        return List.copyOf(out);
    }

    public static Transition advance(ServerPlayer player, CyberNpcEntity npc,
                                     String currentPage, String action) {
        String id = npc.getStoryNpcId();
        Profile profile = PROFILES.get(id);
        CompoundTag state = state(player, id);
        if ("back".equals(action)) return new Transition("root", profile.returnHello());
        if ("topics".equals(action)) return new Transition("topics", "Go on.");
        if ("mood".equals(action)) return new Transition("root", profile.mood());
        if ("town".equals(action)) return new Transition("root", profile.town());
        if ("chat".equals(action)) return new Transition("root", profile.chat());
        if (action.startsWith("talk_")) {
            int topicId = topicIndex(action, profile);
            if (topicId < 0) return new Transition("root", profile.returnHello());
            if (!state.getBoolean("Seen_" + topicId)) {
                state.putBoolean("Seen_" + topicId, true);
                adjust(state, "Trust", 1);
            }
            return new Transition(action, profile.topics().get(topicId).introduction());
        }
        if (currentPage.startsWith("talk_")) {
            int topicId = topicIndex(currentPage, profile);
            if (topicId < 0) return new Transition("root", profile.returnHello());
            Topic topic = profile.topics().get(topicId);
            switch (action) {
                case "more" -> { return new Transition(currentPage, topic.detail()); }
                case "agree" -> {
                    if (!state.getBoolean("Agreed_" + topicId)) {
                        state.putBoolean("Agreed_" + topicId, true);
                        adjust(state, "Affection", 1);
                    }
                    return new Transition(currentPage, "Fair enough.");
                }
                case "disagree" -> { return new Transition(currentPage, "Alright. Different tastes."); }
                case "insult" -> {
                    long now = player.level().getGameTime();
                    if (now >= state.getLong("NextInsult")) {
                        adjust(state, "Affection", -2);
                        state.putLong("NextInsult", now + 1200L);
                    }
                    int chance = 40 + ("mason".equals(id) && topicId == 0 ? 30 : 0)
                            - state.getInt("Affection") * 3;
                    chance = Math.max(10, Math.min(90, chance));
                    if (player.getRandom().nextInt(100) < chance) {
                        state.putLong("Cooldown", now + 600L);
                        // A single navigation request, not a new ticking AI goal.
                        npc.getNavigation().moveTo(npc.getX() + 3.0D, npc.getY(),
                                npc.getZ() + 2.0D, 0.95D);
                        return new Transition("walk", "...");
                    }
                    return new Transition(currentPage, "Okay.");
                }
                default -> { return new Transition("root", profile.returnHello()); }
            }
        }
        if ("town_npc_1".equals(id)) {
            if ("yarn_offer".equals(action))
                return new Transition("yarn_offer", "I'm running low on yarn. Can you craft me a stack? Sixty-four should do.");
            if ("accept_yarn".equals(action))
                return new Transition("root", startQuest(player)
                        ? "Thanks. You'll need string."
                        : "Can't sort that out right now. Maybe check your journal.");
            if ("yarn_turnin".equals(action))
                return new Transition("root", finishQuest(player)
                        ? "You actually made all of it. Thanks!"
                        : "Not sixty-four yet. Let me know when you're done.");
        }
        return new Transition("root", profile.returnHello());
    }

    private static int topicIndex(String value, Profile profile) {
        try {
            int index = Integer.parseInt(value.substring(5));
            return index >= 0 && index < profile.topics().size() ? index : -1;
        } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
            return -1;
        }
    }

    private static DialogueView.Option option(String id, String label) {
        return new DialogueView.Option(id, label);
    }

    private static CompoundTag state(ServerPlayer player, String id) {
        CompoundTag root = player.getPersistentData().getCompound(DATA_KEY);
        CompoundTag state = root.getCompound(id);
        root.put(id, state);
        player.getPersistentData().put(DATA_KEY, root);
        return state;
    }

    private static void adjust(CompoundTag state, String key, int delta) {
        state.putInt(key, Math.max(-20, Math.min(20, state.getInt(key) + delta)));
    }

    private static boolean questActive(ServerPlayer player) {
        return questState(player, "isActive");
    }

    private static boolean questCompleted(ServerPlayer player) {
        return questState(player, "isCompleted");
    }

    private static boolean questState(ServerPlayer player, String method) {
        if (!ModList.get().isLoaded("cyberquest")) return false;
        try {
            Class<?> type = Class.forName("com.cyberspectraa.cyberquest.player.PlayerQuestData");
            Method target = type.getMethod(method, ServerPlayer.class, ResourceLocation.class);
            return Boolean.TRUE.equals(target.invoke(null, player, KATIE_QUEST));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean startQuest(ServerPlayer player) {
        return questOperation(player, "start", false);
    }

    private static boolean finishQuest(ServerPlayer player) {
        return questOperation(player, "turnIn", false);
    }

    private static boolean questOperation(ServerPlayer player, String name, boolean force) {
        if (!ModList.get().isLoaded("cyberquest")) return false;
        try {
            Class<?> type = Class.forName("com.cyberspectraa.cyberquest.quest.QuestManager");
            Method method = type.getMethod(name, ServerPlayer.class, ResourceLocation.class, boolean.class);
            return Boolean.TRUE.equals(method.invoke(null, player, KATIE_QUEST, force));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    @SubscribeEvent
    public static void preserveOnRespawn(PlayerEvent.Clone event) {
        CompoundTag original = event.getOriginal().getPersistentData();
        if (original.contains(DATA_KEY)) {
            event.getEntity().getPersistentData().put(DATA_KEY,
                    original.getCompound(DATA_KEY).copy());
        }
    }
}