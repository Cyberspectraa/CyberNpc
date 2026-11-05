package com.cyberspectraa.cybernpc.dialogue;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Server-authoritative, data-driven branching dialogue for the seven Town NPCs.
 *
 * Data lives in data/<namespace>/town_dialogue/*.json and reloads with /reload.
 * Only the selected line / transition is evaluated, never an entity tick.
 * All state remains per-player and per stable story ID. "mason" is still Kai's ID.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TownCharacterDialogue {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_KEY = "CyberNpcTownCharacters";
    private static final ResourceLocation KATIE_QUEST =
            new ResourceLocation("cyberquest", "katie_yarn");
    private static volatile Map<String, Script> scripts = Map.of();
    private static final java.util.Set<String> KNOWN_IDS = java.util.Set.of(
            "mason", "town_npc_1", "town_npc_2", "town_npc_3",
            "town_npc_4", "town_npc_5", "town_npc_6");

    public record Transition(String page, String speech) {}
    private record Choice(String text, String to, String effect, String when, String action) {}
    private record Node(String speech, String remember, List<Choice> choices) {}
    private record Script(String id, String name, String first, String repeat,
                          String trusted, String cooldown, String redirect, String subjectChange,
                          Map<String, Node> nodes) {}

    private TownCharacterDialogue() {}

    @SubscribeEvent
    public static void onResources(AddReloadListenerEvent event) {
        event.addListener(new DialogueLoader());
    }

    /** A failure in one user-edited profile must not disable the others. */
    private static final class DialogueLoader extends SimpleJsonResourceReloadListener {
        DialogueLoader() {
            super(new GsonBuilder().create(), "town_dialogue");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources,
                             ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, Script> loaded = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
                try {
                    Script script = parse(entry.getValue().getAsJsonObject());
                    if (loaded.putIfAbsent(script.id(), script) != null) {
                        LOGGER.warn("Duplicate Town NPC dialogue for {}", script.id());
                    }
                } catch (RuntimeException ex) {
                    LOGGER.error("Could not load Town NPC dialogue {}", entry.getKey(), ex);
                }
            }
            scripts = Map.copyOf(loaded);
            LOGGER.info("Loaded {} authored Town NPC dialogue profiles", scripts.size());
        }
    }

    private static Script parse(JsonObject obj) {
        String id = required(obj, "story_id");
        if (!KNOWN_IDS.contains(id)) throw new IllegalArgumentException("Unknown town story ID: " + id);
        String name = required(obj, "name");
        JsonObject jsonNodes = obj.getAsJsonObject("nodes");
        if (jsonNodes == null || !jsonNodes.has("root")) {
            throw new IllegalArgumentException(id + ": missing root scene");
        }
        Map<String, Node> nodes = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : jsonNodes.entrySet()) {
            String key = entry.getKey();
            if (key.length() > 40 || key.isBlank()) {
                throw new IllegalArgumentException(id + ": invalid scene id " + key);
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            String speech = required(json, "speech");
            if (speech.length() > 1024) {
                throw new IllegalArgumentException(id + ": scene too long " + key);
            }
            String remember = optional(json, "remember");
            List<Choice> choices = new ArrayList<>();
            JsonArray array = json.getAsJsonArray("choices");
            if (array != null) {
                for (JsonElement item : array) {
                    JsonObject choice = item.getAsJsonObject();
                    String label = required(choice, "text");
                    String to = required(choice, "to");
                    if (label.length() > 80 || to.length() > 40) {
                        throw new IllegalArgumentException(id + ": invalid option at " + key);
                    }
                    choices.add(new Choice(label, to, optional(choice, "effect"),
                            optional(choice, "when"), optional(choice, "action")));
                }
            }
            if (choices.size() > 12) {
                throw new IllegalArgumentException(id + ": too many options in " + key);
            }
            nodes.put(key, new Node(speech, remember, List.copyOf(choices)));
        }
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            for (Choice c : entry.getValue().choices()) {
                if (!nodes.containsKey(c.to())) {
                    throw new IllegalArgumentException(id + ": unknown next scene " + c.to());
                }
            }
        }
        return new Script(id, name, required(obj, "first_greeting"),
                required(obj, "repeat_greeting"), required(obj, "trusted_greeting"),
                required(obj, "cooldown_greeting"), required(obj, "redirect"),
                required(obj, "subject_change"), Map.copyOf(nodes));
    }

    private static String required(JsonObject obj, String key) {
        String value = optional(obj, key);
        if (value.isBlank()) throw new IllegalArgumentException("Missing dialogue field: " + key);
        return value;
    }

    private static String optional(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive()
                ? obj.get(key).getAsString().trim() : "";
    }

    public static boolean recognises(CyberNpcEntity npc) {
        return npc != null && scripts.containsKey(npc.getStoryNpcId());
    }

    /** Keep the old ID; display renaming is handled by CyberNpcEntity on load. */
    public static String displayName(String stableStoryId) {
        Script script = scripts.get(stableStoryId);
        return script == null ? null : script.name();
    }

    public static String originalName(String stableStoryId) {
        if (stableStoryId == null) return null;
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
        Script script = scripts.get(npc.getStoryNpcId());
        if (script == null) return "";
        CompoundTag state = state(player, script.id());
        if (onCooldown(player, state)) return script.cooldown();
        int visits = state.getInt("Visits");
        state.putInt("Visits", visits == Integer.MAX_VALUE ? visits : visits + 1);
        if (visits == 0) return script.first();
        if (state.getInt("Trust") >= 2 && state.getInt("Affection") >= 1) {
            return script.trusted();
        }
        return script.repeat();
    }

    /**
     * The server recomputes the offered choices before accepting a response.
     * Only conditional choices from the current scene may become actions.
     */
    public static List<DialogueView.Option> options(ServerPlayer player,
                                                      CyberNpcEntity npc, String page) {
        Script script = scripts.get(npc.getStoryNpcId());
        if (script == null) return List.of();
        CompoundTag state = state(player, script.id());
        if (onCooldown(player, state) || "walked".equals(page)) {
            return List.of(new DialogueView.Option("leave", "Leave them alone."));
        }
        Node node = script.nodes().get(page);
        if (node == null) return List.of(new DialogueView.Option("leave", "Leave."));
        List<DialogueView.Option> options = new ArrayList<>();
        List<Choice> available = availableChoices(player, state, node);
        int maxReplies = "root".equals(page) ? 7 : 6;
        for (int i = 0; i < available.size() && options.size() < maxReplies; i++) {
            options.add(new DialogueView.Option("pick_" + i, available.get(i).text()));
        }
        if (!"root".equals(page)) {
            options.add(new DialogueView.Option("else", script.subjectChange()));
        }
        options.add(new DialogueView.Option("leave", "See you."));
        return List.copyOf(options);
    }

    private static List<Choice> availableChoices(ServerPlayer player, CompoundTag state, Node node) {
        List<Choice> result = new ArrayList<>();
        for (Choice choice : node.choices()) {
            if (allowed(player, state, choice.when())) result.add(choice);
        }
        return result;
    }

    private static boolean allowed(ServerPlayer player, CompoundTag state, String condition) {
        if (condition == null || condition.isBlank()) return true;
        if (condition.startsWith("flag:")) {
            return state.getCompound("Memories").getBoolean(condition.substring(5));
        }
        return switch (condition) {
            case "quest_available" -> ModList.get().isLoaded("cyberquest")
                    && !questActive(player) && !questCompleted(player);
            case "quest_active" -> questActive(player);
            case "quest_completed" -> questCompleted(player);
            default -> false; // Fail closed on unsupported datapack conditions.
        };
    }

    public static Transition advance(ServerPlayer player, CyberNpcEntity npc,
                                      String page, String action) {
        Script script = scripts.get(npc.getStoryNpcId());
        if (script == null) return new Transition("walked", "");
        CompoundTag state = state(player, script.id());
        if (onCooldown(player, state) || "walked".equals(page)) {
            return new Transition("walked", script.cooldown());
        }
        if ("else".equals(action)) return new Transition("root", script.redirect());
        Node node = script.nodes().get(page);
        if (node == null || !action.startsWith("pick_")) {
            return new Transition("root", script.redirect());
        }

        int index;
        try {
            index = Integer.parseInt(action.substring(5));
        } catch (NumberFormatException ex) {
            return new Transition("root", script.redirect());
        }
        List<Choice> available = availableChoices(player, state, node);
        if (index < 0 || index >= available.size()
                || index >= ("root".equals(page) ? 7 : 6)) {
            return new Transition(page, node.speech());
        }
        Choice choice = available.get(index);
        if (!allowed(player, state, choice.when())) {
            return new Transition(page, node.speech());
        }
        if (!choice.action().isBlank()) {
            if ("quest_start".equals(choice.action())) {
                if (!questOperation(player, "start")) {
                    return new Transition(page, "I can't start that right now. Check your journal.");
                }
            } else if ("quest_finish".equals(choice.action())) {
                if (questOperation(player, "turnIn")) {
                    return enter(script, state, "yarn_done");
                }
                return new Transition(page,
                        "Not yet. You need to craft all sixty-four. Check your journal.");
            } else {
                return new Transition(page, node.speech());
            }
        }

        applyRelationship(player, state, page, index, choice.effect());
        if (maybeWalkAway(player, npc, state, choice.effect())) {
            state.putLong("Cooldown", player.level().getGameTime() + 600L);
            walkAway(player, npc);
            return new Transition("walked", "..."); // The dialogue closes on the next reply.
        }
        return enter(script, state, choice.to());
    }

    private static Transition enter(Script script, CompoundTag state, String next) {
        if ("root".equals(next)) return new Transition("root", script.redirect());
        Node node = script.nodes().get(next);
        if (node == null) return new Transition("root", script.redirect());
        if (!node.remember().isBlank()) {
            state.getCompound("Memories").putBoolean(node.remember(), true);
        }
        return new Transition(next, node.speech());
    }

    private static void applyRelationship(ServerPlayer player, CompoundTag state,
                                           String page, int index, String effect) {
        if (effect.isBlank()) return;
        CompoundTag memories = state.getCompound("Memories");
        String key = "decision:" + page + ":" + index;
        long gameTime = player.level().getGameTime();
        boolean newChoice = !memories.getBoolean(key);
        if (newChoice) memories.putBoolean(key, true);
        switch (effect) {
            case "warm" -> {
                if (newChoice && canBondToday(state, gameTime)) change(state, "Affection", 1);
            }
            case "trust" -> {
                if (newChoice && canBondToday(state, gameTime)) change(state, "Trust", 1);
            }
            case "repair" -> {
                if (newChoice && state.getInt("Affection") < 0 && canBondToday(state, gameTime)) {
                    change(state, "Affection", 1);
                }
            }
            case "rude", "kai_choso", "kai_other" -> {
                // Repeated insults are not a friendship grinding mechanic.
                // A repeated insult can only lower affinity again on a later day.
                long day = gameTime / 24000L;
                if (newChoice || day > state.getLong("LastInsultDay")) {
                    change(state, "Affection", effect.equals("kai_choso") ? -2 : -1);
                    state.putLong("LastInsultDay", day);
                }
            }
            default -> { }
        }
    }

    private static boolean canBondToday(CompoundTag state, long now) {
        // One meaningful relationship improvement per character per game day.
        // Merely revisiting topics or spamming "I agree" cannot farm affection.
        long day = now / 24000L;
        if (state.contains("LastBondDay") && state.getLong("LastBondDay") == day) {
            return false;
        }
        state.putLong("LastBondDay", day);
        return true;
    }

    private static void change(CompoundTag state, String key, int delta) {
        state.putInt(key, Math.max(-10, Math.min(10, state.getInt(key) + delta)));
    }

    private static boolean maybeWalkAway(ServerPlayer player, CyberNpcEntity npc,
                                          CompoundTag state, String effect) {
        int chance = switch (effect) {
            case "kai_choso" -> 70;
            case "kai_other" -> 45;
            default -> 0;
        };
        if (chance == 0) return false;
        chance -= Math.max(0, state.getInt("Affection")) * 2;
        chance += Math.max(0, -state.getInt("Affection")) * 3;
        return player.getRandom().nextInt(100) < Math.max(15, Math.min(90, chance));
    }

    private static void walkAway(ServerPlayer player, CyberNpcEntity npc) {
        Vec3 destination = null;
        double best = npc.distanceToSqr(player);
        for (int i = 0; i < 5; i++) {
            Vec3 candidate = DefaultRandomPos.getPos(npc, 7, 3);
            if (candidate != null && candidate.distanceToSqr(player.position()) > best) {
                destination = candidate;
                best = candidate.distanceToSqr(player.position());
            }
        }
        if (destination != null) {
            npc.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.85D);
        } else {
            npc.getLookControl().setLookAt(npc.getX() + 3, npc.getEyeY(), npc.getZ() + 3);
        }
    }

    private static boolean onCooldown(ServerPlayer player, CompoundTag state) {
        return state.getLong("Cooldown") > player.level().getGameTime();
    }

    /** Previous saves contain these same fields, so no data migration is required. */
    private static CompoundTag state(ServerPlayer player, String id) {
        CompoundTag root = player.getPersistentData().getCompound(DATA_KEY);
        CompoundTag state = root.getCompound(id);
        if (!state.contains("Memories")) state.put("Memories", new CompoundTag());
        root.put(id, state);
        player.getPersistentData().put(DATA_KEY, root);
        return state;
    }

    private static boolean questActive(ServerPlayer player) {
        return questState(player, "isActive");
    }

    private static boolean questCompleted(ServerPlayer player) {
        return questState(player, "isCompleted");
    }

    private static boolean questState(ServerPlayer player, String name) {
        if (!ModList.get().isLoaded("cyberquest")) return false;
        try {
            Class<?> type = Class.forName("com.cyberspectraa.cyberquest.player.PlayerQuestData");
            Method method = type.getMethod(name, ServerPlayer.class, ResourceLocation.class);
            return Boolean.TRUE.equals(method.invoke(null, player, KATIE_QUEST));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    private static boolean questOperation(ServerPlayer player, String name) {
        if (!ModList.get().isLoaded("cyberquest")) return false;
        try {
            Class<?> type = Class.forName("com.cyberspectraa.cyberquest.quest.QuestManager");
            Method method = type.getMethod(name, ServerPlayer.class, ResourceLocation.class, boolean.class);
            return Boolean.TRUE.equals(method.invoke(null, player, KATIE_QUEST, false));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.warn("Couldn't perform Katie quest action {}", name, ex);
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