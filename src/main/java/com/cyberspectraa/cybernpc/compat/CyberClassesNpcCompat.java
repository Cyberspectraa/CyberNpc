package com.cyberspectraa.cybernpc.compat;

import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Reflection bridge to CyberClasses so CyberNpc does not keep a second copy
 * of the class definitions. CyberClasses is the source of truth for class ids,
 * names, NPC spawn weights and magic availability; CyberNpc owns only AI and
 * equipment behaviour for the selected id.
 */
public final class CyberClassesNpcCompat {
    private static final String REGISTRY =
            "com.cyberspectraa.cyberclasses.classdata.NpcClassRegistry";

    private static boolean resolved;
    private static Method normalizeMethod;
    private static Method displayNameMethod;
    private static Method randomMethod;
    private static Method availableMethod;
    private static Method spellBookMethod;
    private static Method magicSchoolMethod;

    private CyberClassesNpcCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("cyberclasses");
    }

    public static String normalize(String id) {
        Object result = invoke(normalize(), id);
        if (result instanceof String value && !value.isBlank()) {
            return value;
        }

        if ("horse_tamer".equalsIgnoreCase(id)) {
            return "ranger";
        }

        return id == null || id.isBlank()
                ? "classless"
                : id.trim().toLowerCase(Locale.ROOT);
    }

    public static String displayName(String id) {
        Object result = invoke(displayName(), normalize(id));
        if (result instanceof String value && !value.isBlank()) {
            return value;
        }

        String value = normalize(id);
        return Character.toUpperCase(value.charAt(0))
                + value.substring(1);
    }

    public static String randomClassId(RandomSource random) {
        Object result = invoke(random(), random);
        return result instanceof String value && !value.isBlank()
                ? normalize(value)
                : "classless";
    }

    public static boolean isAvailable(String id) {
        Object result = invoke(available(), normalize(id));
        return !(result instanceof Boolean value) || value;
    }

    public static boolean usesSpellBook(String id) {
        Object result = invoke(spellBook(), normalize(id));
        return result instanceof Boolean value && value;
    }

    public static boolean hasMagicSchool(String id) {
        Object result = invoke(magicSchool(), normalize(id));
        return result instanceof Boolean value && value;
    }

    private static Object invoke(Method method, Object argument) {
        if (method == null) {
            return null;
        }

        try {
            return method.invoke(null, argument);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method normalize() {
        resolve();
        return normalizeMethod;
    }

    private static Method displayName() {
        resolve();
        return displayNameMethod;
    }

    private static Method random() {
        resolve();
        return randomMethod;
    }

    private static Method available() {
        resolve();
        return availableMethod;
    }

    private static Method spellBook() {
        resolve();
        return spellBookMethod;
    }

    private static Method magicSchool() {
        resolve();
        return magicSchoolMethod;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }

        resolved = true;

        if (!isLoaded()) {
            return;
        }

        try {
            Class<?> registry = Class.forName(REGISTRY);
            normalizeMethod = registry.getMethod("normalize", String.class);
            displayNameMethod = registry.getMethod("displayName", String.class);
            randomMethod = registry.getMethod("randomClassId", RandomSource.class);
            availableMethod = registry.getMethod("isAvailable", String.class);
            spellBookMethod = registry.getMethod("usesSpellBook", String.class);
            magicSchoolMethod = registry.getMethod("hasMagicSchool", String.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            normalizeMethod = null;
            displayNameMethod = null;
            randomMethod = null;
            availableMethod = null;
            spellBookMethod = null;
            magicSchoolMethod = null;
        }
    }
}
