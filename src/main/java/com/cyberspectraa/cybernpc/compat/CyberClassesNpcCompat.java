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
    private static Method spellBookWithAdvancementMethod;
    private static Method magicSchoolMethod;
    private static Method magicSchoolWithAdvancementMethod;
    private static Method randomAdvancementMethod;
    private static Method normalizeAdvancementMethod;
    private static Method advancementDisplayNameMethod;
    private static Method effectiveDisplayNameMethod;
    private static Method manaTierMethod;

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

    public static boolean usesSpellBook(
            String classId,
            String advancementId
    ) {
        Object result = invoke(
                spellBookWithAdvancement(),
                normalize(classId),
                normalizeAdvancement(classId, advancementId)
        );
        return result instanceof Boolean value && value;
    }

    public static boolean hasMagicSchool(String id) {
        Object result = invoke(magicSchool(), normalize(id));
        return result instanceof Boolean value && value;
    }

    public static boolean hasMagicSchool(
            String classId,
            String advancementId
    ) {
        Object result = invoke(
                magicSchoolWithAdvancement(),
                normalize(classId),
                normalizeAdvancement(classId, advancementId)
        );
        return result instanceof Boolean value && value;
    }

    public static String randomAdvancementId(
            String classId,
            int level,
            RandomSource random
    ) {
        Object result = invoke(
                randomAdvancement(),
                normalize(classId),
                level,
                random
        );
        return result instanceof String value ? value : "";
    }

    public static String normalizeAdvancement(
            String classId,
            String advancementId
    ) {
        if (advancementId == null || advancementId.isBlank()) {
            return "";
        }

        Object result = invoke(
                normalizeAdvancementMethod(),
                normalize(classId),
                advancementId
        );

        return result instanceof String value ? value : "";
    }

    public static String advancementDisplayName(String advancementId) {
        Object result = invoke(
                advancementDisplayName(),
                advancementId == null ? "" : advancementId
        );
        return result instanceof String value ? value : "";
    }

    public static String effectiveDisplayName(
            String classId,
            String advancementId
    ) {
        Object result = invoke(
                effectiveDisplayName(),
                normalize(classId),
                advancementId == null ? "" : advancementId
        );

        return result instanceof String value && !value.isBlank()
                ? value
                : displayName(classId);
    }

    public static String manaTier(
            String classId,
            String advancementId
    ) {
        Object result = invoke(
                manaTier(),
                normalize(classId),
                advancementId == null ? "" : advancementId
        );

        return result instanceof String value ? value : "NONE";
    }

    private static Object invoke(Method method, Object... arguments) {
        if (method == null) {
            return null;
        }

        try {
            return method.invoke(null, arguments);
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

    private static Method spellBookWithAdvancement() {
        resolve();
        return spellBookWithAdvancementMethod;
    }

    private static Method magicSchool() {
        resolve();
        return magicSchoolMethod;
    }

    private static Method magicSchoolWithAdvancement() {
        resolve();
        return magicSchoolWithAdvancementMethod;
    }

    private static Method randomAdvancement() {
        resolve();
        return randomAdvancementMethod;
    }

    private static Method normalizeAdvancementMethod() {
        resolve();
        return normalizeAdvancementMethod;
    }

    private static Method advancementDisplayName() {
        resolve();
        return advancementDisplayNameMethod;
    }

    private static Method effectiveDisplayName() {
        resolve();
        return effectiveDisplayNameMethod;
    }

    private static Method manaTier() {
        resolve();
        return manaTierMethod;
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
            spellBookWithAdvancementMethod = registry.getMethod(
                    "usesSpellBook",
                    String.class,
                    String.class
            );
            magicSchoolMethod = registry.getMethod("hasMagicSchool", String.class);
            magicSchoolWithAdvancementMethod = registry.getMethod(
                    "hasMagicSchool",
                    String.class,
                    String.class
            );
            randomAdvancementMethod = registry.getMethod(
                    "randomAdvancementId",
                    String.class,
                    int.class,
                    RandomSource.class
            );
            normalizeAdvancementMethod = registry.getMethod(
                    "normalizeAdvancement",
                    String.class,
                    String.class
            );
            advancementDisplayNameMethod = registry.getMethod(
                    "advancementDisplayName",
                    String.class
            );
            effectiveDisplayNameMethod = registry.getMethod(
                    "effectiveDisplayName",
                    String.class,
                    String.class
            );
            manaTierMethod = registry.getMethod(
                    "manaTier",
                    String.class,
                    String.class
            );
        } catch (ReflectiveOperationException | LinkageError ignored) {
            normalizeMethod = null;
            displayNameMethod = null;
            randomMethod = null;
            availableMethod = null;
            spellBookMethod = null;
            spellBookWithAdvancementMethod = null;
            magicSchoolMethod = null;
            magicSchoolWithAdvancementMethod = null;
            randomAdvancementMethod = null;
            normalizeAdvancementMethod = null;
            advancementDisplayNameMethod = null;
            effectiveDisplayNameMethod = null;
            manaTierMethod = null;
        }
    }
}
