package com.cyberspectraa.cybernpc.compat;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

/**
 * Optional/reflection-safe bridge into CyberRaces' shared Cyber progression.
 * CyberRaces owns the XP curve and stored progression data; CyberNpc only
 * decides when Wild NPC gameplay should award or initialise that progression.
 */
public final class CyberProgressionCompat {
    private static final String MANAGER =
            "com.cyberspectraa.cyberraces.progression.ProgressionManager";

    private static boolean resolved;
    private static Method awardCombatKillMethod;
    private static Method getLevelMethod;
    private static Method setLevelMethod;

    private CyberProgressionCompat() {
    }

    public static void awardCombatKill(
            LivingEntity killer,
            LivingEntity victim
    ) {
        Method method = awardCombatKill();
        if (method == null || killer == null || victim == null) {
            return;
        }

        try {
            method.invoke(null, killer, victim);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static int getLevel(LivingEntity entity) {
        Method method = getLevel();
        if (method == null || entity == null) {
            return 1;
        }

        try {
            Object value = method.invoke(null, entity);
            return value instanceof Integer level
                    ? Math.max(1, level)
                    : 1;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 1;
        }
    }

    public static void setLevel(
            LivingEntity entity,
            int level
    ) {
        Method method = setLevel();
        if (method == null || entity == null) {
            return;
        }

        try {
            method.invoke(null, entity, Math.max(1, level));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static Method awardCombatKill() {
        resolve();
        return awardCombatKillMethod;
    }

    private static Method getLevel() {
        resolve();
        return getLevelMethod;
    }

    private static Method setLevel() {
        resolve();
        return setLevelMethod;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        resolved = true;

        try {
            Class<?> manager = Class.forName(MANAGER);
            awardCombatKillMethod = manager.getMethod(
                    "awardCombatKill",
                    LivingEntity.class,
                    LivingEntity.class
            );
            getLevelMethod = manager.getMethod(
                    "getLevel",
                    LivingEntity.class
            );
            setLevelMethod = manager.getMethod(
                    "setLevel",
                    LivingEntity.class,
                    int.class
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            awardCombatKillMethod = null;
            getLevelMethod = null;
            setLevelMethod = null;
        }
    }
}
