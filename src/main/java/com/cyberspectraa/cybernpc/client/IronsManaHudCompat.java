package com.cyberspectraa.cybernpc.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

/**
 * Optional Iron's Spells 'n Spellbooks client mana bridge.
 * Does not link against its classes when the mod is absent.
 */
final class IronsManaHudCompat {
    private static boolean reflectionAttempted;
    private static Method readCurrentMana;
    private static final ResourceLocation MAX_MANA_ID =
            new ResourceLocation("irons_spellbooks", "max_mana");

    static Mana current(LocalPlayer player) {
        if (player == null || !ModList.get().isLoaded("irons_spellbooks")) {
            return null;
        }
        if (!reflectionAttempted) {
            reflectionAttempted = true;
            try {
                Class<?> clientMana = Class.forName(
                        "io.redspace.ironsspellbooks.player.ClientMagicData");
                readCurrentMana = clientMana.getMethod("getPlayerMana");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                readCurrentMana = null;
            }
        }
        if (readCurrentMana == null) return null;
        try {
            Attribute maxAttribute = ForgeRegistries.ATTRIBUTES.getValue(MAX_MANA_ID);
            if (maxAttribute == null || player.getAttribute(maxAttribute) == null) return null;
            // A zero max mana is intentional for CyberClasses' ManaTier.NONE
            // (and for any future race/ability that grants no mana pool).
            // Do not turn that zero into a fake 1-point mana gauge.
            double effectiveMax = player.getAttributeValue(maxAttribute);
            if (!Double.isFinite(effectiveMax)) return null;
            if (effectiveMax <= 0.0D) return new Mana(0, 0);
            int max = Math.max(1, (int) Math.ceil(effectiveMax));
            int current = Math.max(0,
                    ((Number) readCurrentMana.invoke(null)).intValue());
            return new Mana(Math.min(current, max), max);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // No client crash if an optional mod changes its mana API.
            return null;
        }
    }

    record Mana(int current, int maximum) {}
    private IronsManaHudCompat() {}
}
