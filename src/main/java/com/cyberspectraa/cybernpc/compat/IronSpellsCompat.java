package com.cyberspectraa.cybernpc.compat;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public final class IronSpellsCompat {
    public static final String MOD_ID = "irons_spellbooks";
    private static final List<String> ATTACK_SPELLS = List.of(
            "irons_spellbooks:firebolt",
            "irons_spellbooks:magic_missile",
            "irons_spellbooks:icicle"
    );

    private static boolean attemptedInit;
    private static boolean ready;
    private static Method getSpell;
    private static Method isEnabled;
    private static Method getCastType;
    private static Method getSpellCooldown;
    private static Method onServerPreCast;
    private static Method onCast;
    private static Method onServerCastComplete;
    private static Constructor<?> magicDataConstructor;
    private static Object mobCastSource;

    private IronSpellsCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static CastResult castAttackSpell(CyberNpcEntity caster, LivingEntity target, int spellLevel) {
        if (!isLoaded() || caster.level().isClientSide || !initialize()) {
            return CastResult.failed();
        }

        faceTarget(caster, target);

        int start = caster.getRandom().nextInt(ATTACK_SPELLS.size());
        for (int offset = 0; offset < ATTACK_SPELLS.size(); offset++) {
            String id = ATTACK_SPELLS.get((start + offset) % ATTACK_SPELLS.size());

            try {
                Object spell = getSpell.invoke(null, id);
                if (spell == null) {
                    continue;
                }

                if (isEnabled != null && !((Boolean) isEnabled.invoke(spell))) {
                    continue;
                }

                Object castType = getCastType.invoke(spell);
                if (castType == null || !"INSTANT".equals(String.valueOf(castType))) {
                    continue;
                }

                Object magicData = magicDataConstructor.newInstance(true);
                int safeLevel = Math.max(1, spellLevel);

                onServerPreCast.invoke(spell, caster.level(), safeLevel, caster, magicData);
                onCast.invoke(spell, caster.level(), safeLevel, caster, mobCastSource, magicData);
                onServerCastComplete.invoke(spell, caster.level(), safeLevel, caster, magicData, false);

                int cooldown = ((Number) getSpellCooldown.invoke(spell)).intValue();
                return new CastResult(true, Mth.clamp(cooldown, 16, 80), id);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try another verified instant projectile spell.
            }
        }

        return CastResult.failed();
    }

    private static synchronized boolean initialize() {
        if (attemptedInit) {
            return ready;
        }
        attemptedInit = true;

        try {
            Class<?> spellRegistryClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.registry.SpellRegistry"
            );
            Class<?> abstractSpellClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.spells.AbstractSpell"
            );
            Class<?> magicDataClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.magic.MagicData"
            );
            Class<?> castSourceClass = Class.forName(
                    "io.redspace.ironsspellbooks.api.spells.CastSource"
            );

            getSpell = spellRegistryClass.getMethod("getSpell", String.class);
            isEnabled = abstractSpellClass.getMethod("isEnabled");
            getCastType = abstractSpellClass.getMethod("getCastType");
            getSpellCooldown = abstractSpellClass.getMethod("getSpellCooldown");
            onServerPreCast = abstractSpellClass.getMethod(
                    "onServerPreCast",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    magicDataClass
            );
            onCast = abstractSpellClass.getMethod(
                    "onCast",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    castSourceClass,
                    magicDataClass
            );
            onServerCastComplete = abstractSpellClass.getMethod(
                    "onServerCastComplete",
                    Level.class,
                    int.class,
                    LivingEntity.class,
                    magicDataClass,
                    boolean.class
            );
            magicDataConstructor = magicDataClass.getConstructor(boolean.class);

            @SuppressWarnings({"rawtypes", "unchecked"})
            Object source = Enum.valueOf((Class<? extends Enum>) castSourceClass, "MOB");
            mobCastSource = source;
            ready = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            ready = false;
        }

        return ready;
    }

    private static void faceTarget(CyberNpcEntity caster, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(caster.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float yaw = (float) (Math.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(delta.y, horizontal) * (180.0D / Math.PI));

        caster.setYRot(yaw);
        caster.setYHeadRot(yaw);
        caster.setXRot(pitch);
        caster.getLookControl().setLookAt(target, 90.0F, 90.0F);
    }

    public record CastResult(boolean success, int cooldownTicks, String spellId) {
        static CastResult failed() {
            return new CastResult(false, 20, "");
        }
    }
}
