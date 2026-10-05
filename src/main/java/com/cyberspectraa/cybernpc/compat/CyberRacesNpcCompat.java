package com.cyberspectraa.cybernpc.compat;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcReactionIcon;
import com.cyberspectraa.cybernpc.entity.WildNpcPersonality;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Optional CyberRaces integration without making CyberNpc depend on
 * CyberRaces at compile time. Race identity is read from the stable persistent
 * data written by CyberRaces, while active racial AI is delegated back to
 * CyberRaces through a cached reflection bridge.
 */
public final class CyberRacesNpcCompat {
    private static final String PLAYER_ROOT = "CyberRaces";
    private static final String NPC_ROOT = "CyberRacesWildNpc";
    private static final String RACE_KEY = "Race";

    private static boolean aiBridgeResolved;
    private static Method combatAiMethod;
    private static Method idleAiMethod;

    private CyberRacesNpcCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("cyberraces");
    }

    @Nullable
    public static String raceId(LivingEntity entity) {
        if (entity == null) {
            return null;
        }

        CompoundTag persistent = entity.getPersistentData();

        String npcRace = readRace(persistent, NPC_ROOT);
        if (npcRace != null) {
            return npcRace;
        }

        return readRace(persistent, PLAYER_ROOT);
    }

    public static boolean hasRace(LivingEntity entity) {
        return raceId(entity) != null;
    }

    public static boolean isRace(LivingEntity entity, String raceId) {
        String actual = raceId(entity);
        return actual != null
                && raceId != null
                && actual.equals(raceId.toLowerCase(Locale.ROOT));
    }

    public static boolean sameRace(LivingEntity first, LivingEntity second) {
        String firstRace = raceId(first);
        String secondRace = raceId(second);

        return firstRace != null
                && firstRace.equals(secondRace);
    }

    /**
     * Small fictional-race social affinity used only for friendship speed,
     * reactions and party preference. It never creates automatic hostility.
     */
    public static int socialAffinity(
            CyberNpcEntity source,
            LivingEntity target
    ) {
        if (source == null || target == null) {
            return 0;
        }

        String sourceRace = raceId(source);
        String targetRace = raceId(target);

        if (sourceRace == null || targetRace == null) {
            return 0;
        }

        if (sourceRace.equals(targetRace)) {
            return switch (source.getPersonality()) {
                case LOYAL -> 5;
                case PROTECTIVE -> 4;
                case CAUTIOUS, SKITTISH, STUBBORN, PATIENT, BALANCED -> 3;
                case BRAVE, TACTICAL, AGGRESSIVE -> 2;
                case RECKLESS, OPPORTUNISTIC -> 1;
            };
        }

        if (isPair(sourceRace, targetRace, "fairy", "nymph")) {
            return 3;
        }
        if (isPair(sourceRace, targetRace, "elf", "nymph")
                || isPair(sourceRace, targetRace, "dwarf", "halfling")
                || isPair(sourceRace, targetRace, "orc", "goblin")
                || bothBeastfolk(sourceRace, targetRace)) {
            return 2;
        }
        if (isPair(sourceRace, targetRace, "dragonborn", "tiefling")
                || "human".equals(sourceRace)
                || "human".equals(targetRace)) {
            return 1;
        }

        // A small classic fantasy rivalry. This slows friendship slightly but
        // never makes the NPCs attack one another.
        if (isPair(sourceRace, targetRace, "dwarf", "elf")) {
            return -1;
        }

        return 0;
    }

    public static float partyInviteMultiplier(
            CyberNpcEntity inviter,
            CyberNpcEntity invitee
    ) {
        if (inviter == null || invitee == null) {
            return 1.0F;
        }

        boolean same = sameRace(inviter, invitee);
        WildNpcPersonality personality = inviter.getPersonality();

        float multiplier;

        if (same) {
            multiplier = switch (personality) {
                case LOYAL -> 1.75F;
                case SKITTISH -> 1.70F;
                case CAUTIOUS -> 1.60F;
                case STUBBORN -> 1.50F;
                case PROTECTIVE -> 1.45F;
                case BALANCED -> 1.30F;
                case PATIENT, AGGRESSIVE -> 1.25F;
                case BRAVE -> 1.20F;
                case TACTICAL -> 1.15F;
                case RECKLESS -> 1.05F;
                case OPPORTUNISTIC -> 0.95F;
            };
        } else {
            multiplier = switch (personality) {
                case OPPORTUNISTIC -> 1.08F;
                case RECKLESS, PATIENT -> 1.00F;
                case BRAVE, TACTICAL, BALANCED -> 0.95F;
                case PROTECTIVE -> 0.88F;
                case AGGRESSIVE -> 0.85F;
                case LOYAL -> 0.78F;
                case CAUTIOUS -> 0.72F;
                case STUBBORN -> 0.70F;
                case SKITTISH -> 0.65F;
            };
        }

        multiplier += socialAffinity(inviter, invitee) * 0.07F;
        return Math.max(0.45F, Math.min(1.90F, multiplier));
    }

    public static boolean strongSameRaceBond(
            CyberNpcEntity source,
            CyberNpcEntity target
    ) {
        return sameRace(source, target)
                && socialAffinity(source, target) >= 3;
    }

    public static NpcReactionIcon encounterReaction(
            CyberNpcEntity source,
            LivingEntity target,
            int playerReputation
    ) {
        int affinity = socialAffinity(source, target);
        boolean same = sameRace(source, target);

        if (playerReputation <= -30) {
            return source.getPersonality() == WildNpcPersonality.SKITTISH
                    ? NpcReactionIcon.SCARED
                    : NpcReactionIcon.ANNOYED;
        }

        if (same) {
            return switch (source.getPersonality()) {
                case SKITTISH, CAUTIOUS -> NpcReactionIcon.FRIENDLY;
                case AGGRESSIVE, STUBBORN -> NpcReactionIcon.GREETING;
                default -> source.getRandom().nextBoolean()
                        ? NpcReactionIcon.GREETING
                        : NpcReactionIcon.FRIENDLY;
            };
        }

        if (affinity >= 2) {
            return source.getRandom().nextBoolean()
                    ? NpcReactionIcon.HAPPY
                    : NpcReactionIcon.FRIENDLY;
        }

        if (affinity < 0) {
            return source.getPersonality() == WildNpcPersonality.AGGRESSIVE
                    || source.getPersonality() == WildNpcPersonality.STUBBORN
                    ? NpcReactionIcon.ANNOYED
                    : NpcReactionIcon.CONFUSED;
        }

        return source.getRandom().nextBoolean()
                ? NpcReactionIcon.THINKING
                : NpcReactionIcon.SURPRISED;
    }

    public static boolean tickCombatRacialAi(CyberNpcEntity npc) {
        return invokeAi(combatAiMethod(), npc);
    }

    public static boolean tickIdleRacialAi(CyberNpcEntity npc) {
        return invokeAi(idleAiMethod(), npc);
    }

    private static boolean invokeAi(Method method, CyberNpcEntity npc) {
        if (method == null || npc == null) {
            return false;
        }

        try {
            Object value = method.invoke(null, npc);
            return value instanceof Boolean result && result;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    @Nullable
    private static Method combatAiMethod() {
        resolveAiBridge();
        return combatAiMethod;
    }

    @Nullable
    private static Method idleAiMethod() {
        resolveAiBridge();
        return idleAiMethod;
    }

    private static void resolveAiBridge() {
        if (aiBridgeResolved) {
            return;
        }

        aiBridgeResolved = true;

        if (!isLoaded()) {
            return;
        }

        try {
            Class<?> bridge = Class.forName(
                    "com.cyberspectraa.cyberraces.compat.CyberNpcRacialAi"
            );

            combatAiMethod = bridge.getMethod(
                    "tickCombat",
                    LivingEntity.class
            );

            idleAiMethod = bridge.getMethod(
                    "tickIdle",
                    LivingEntity.class
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            combatAiMethod = null;
            idleAiMethod = null;
        }
    }

    @Nullable
    private static String readRace(
            CompoundTag persistent,
            String rootKey
    ) {
        if (!persistent.contains(rootKey)) {
            return null;
        }

        CompoundTag root = persistent.getCompound(rootKey);
        if (!root.contains(RACE_KEY)) {
            return null;
        }

        String value = root.getString(RACE_KEY)
                .trim()
                .toLowerCase(Locale.ROOT);

        return value.isBlank() ? null : value;
    }

    private static boolean isPair(
            String first,
            String second,
            String a,
            String b
    ) {
        return (a.equals(first) && b.equals(second))
                || (b.equals(first) && a.equals(second));
    }

    private static boolean bothBeastfolk(
            String first,
            String second
    ) {
        return isBeastfolk(first)
                && isBeastfolk(second)
                && !first.equals(second);
    }

    private static boolean isBeastfolk(String race) {
        return "catfolk".equals(race)
                || "dogfolk".equals(race)
                || "foxfolk".equals(race);
    }
}
