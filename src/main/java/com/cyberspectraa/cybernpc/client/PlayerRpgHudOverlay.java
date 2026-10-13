package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * Classic Minecraft-style pixel HUD inspired by the arrangement (not assets)
 * of Souls Combat HUD. Narrow stone-rimmed gauges and an unboxed player model.
 * No simulated stats; no change to core inventory/hotbar/crosshair rendering.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerRpgHudOverlay {
    private static final ResourceLocation FRAME =
            new ResourceLocation(CyberNpc.MOD_ID, "textures/gui/player_hud.png");
    private static final int ATLAS_W = 256;
    private static final int ATLAS_H = 96;
    private static final int HEALTH_WIDTH = 144;
    private static final int HUNGER_WIDTH = 152;
    private static final int MANA_WIDTH = 184;
    private static final int MAX_W = 253;
    private static final int MAX_H = 93;

    private static final int RED_DARK = 0xFF77322C;
    private static final int RED_LIGHT = 0xFFBD534A;
    private static final int GREEN_DARK = 0xFF2D693E;
    private static final int GREEN_LIGHT = 0xFF4F9A58;
    private static final int BLUE_DARK = 0xFF2C568D;
    private static final int BLUE_LIGHT = 0xFF417CB5;
    private static final int WHITE = 0xFFF5F0E1;

    private static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        return PlayerHudConfig.ENABLED.get()
                && !mc.options.hideGui
                && mc.screen == null
                && mc.player != null
                && !mc.player.isSpectator();
    }

    @SubscribeEvent
    public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
        if (!visible() || !PlayerHudConfig.REPLACE_VANILLA.get()) return;
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
            event.setCanceled(true);
        } else if ("irons_spellbooks".equals(id.getNamespace())
                && "mana_overlay".equals(id.getPath())
                && IronsManaHudCompat.current(Minecraft.getInstance().player) != null) {
            // Only suppress Iron's native mana if we can draw the true mana
            // ourselves. An unavailable optional API must not hide its HUD.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void draw(RenderGuiEvent.Post event) {
        if (!visible()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int x = Mth.clamp(PlayerHudConfig.X_OFFSET.get(), 0,
                Math.max(0, gui.guiWidth() - MAX_W));
        int y = Mth.clamp(PlayerHudConfig.Y_OFFSET.get(), 0,
                Math.max(0, gui.guiHeight() - MAX_H));

        // Unboxed paper doll, closer to the supplied pixel HUD reference than
        // a modern "dashboard" panel. Inventory renderer respects real player
        // skin, armor and whichever entity renderer mods currently provide.
        if (PlayerHudConfig.CHARACTER_PREVIEW.get()) {
            gui.enableScissor(x + 2, y + 1, x + 62, y + 83);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        gui, x + 31, y + 81, 34, 0.0F, 0.0F, player);
            } finally {
                gui.disableScissor();
            }
        }

        int bx = x + 62;
        int currentHealthMax = Math.max(1, Mth.ceil(player.getMaxHealth()));
        float health = Math.max(0, Math.min(player.getHealth(), currentHealthMax));
        gauge(gui, font, bx, y + 10, 0, HEALTH_WIDTH, health / currentHealthMax,
                number(health) + "/" + currentHealthMax, RED_DARK, RED_LIGHT);

        int hunger = Mth.clamp(player.getFoodData().getFoodLevel(), 0, 20);
        gauge(gui, font, bx, y + 27, 16, HUNGER_WIDTH, hunger / 20.0F,
                hunger + "/20", GREEN_DARK, GREEN_LIGHT);

        IronsManaHudCompat.Mana mana = IronsManaHudCompat.current(player);
        if (mana != null) {
            gauge(gui, font, bx, y + 44, 32, MANA_WIDTH,
                    mana.current() / (float) Math.max(1, mana.maximum()),
                    mana.current() + "/" + mana.maximum(),
                    BLUE_DARK, BLUE_LIGHT);
        }

        // Small hand-drawn pixel icons, like vanilla inventory sprites.
        // Display actual values rather than limiting modded armour to 20.
        gui.blit(FRAME, x + 8, y + 73, 0, 54, 12, 12, ATLAS_W, ATLAS_H);
        gui.drawString(font, Integer.toString(hunger), x + 21, y + 75, WHITE, true);
        gui.blit(FRAME, x + 40, y + 73, 17, 54, 12, 12, ATLAS_W, ATLAS_H);
        gui.drawString(font, Integer.toString(player.getArmorValue()),
                x + 53, y + 75, WHITE, true);
    }

    private static String number(float value) {
        if (value == Mth.floor(value)) return Integer.toString((int) value);
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void gauge(GuiGraphics gui, Font font,
                              int x, int y, int v, int width, float fraction,
                              String text, int base, int highlight) {
        // Entire background/frame is native 1:1 nearest-neighbour pixel art,
        // never scaled or blurred.
        gui.blit(FRAME, x, y, 0, v, width, 13, ATLAS_W, ATLAS_H);

        int length = Mth.clamp(Math.round((width - 15)
                * Mth.clamp(fraction, 0.0F, 1.0F)), 0, width - 15);
        if (length > 0) {
            gui.fill(x + 8, y + 4, x + 8 + length, y + 9, base);
            gui.fill(x + 8, y + 4, x + 8 + length, y + 5, highlight);
            // Subtle discrete Minecraft pixel shading; no smooth gradients.
            for (int dx = 12; dx < length; dx += 18) {
                gui.fill(x + 8 + dx, y + 8, x + 9 + dx, y + 9,
                        0x60201B12);
            }
        }

        gui.drawString(font, text, x + 13, y + 2, WHITE, true);
    }

    private PlayerRpgHudOverlay() {}
}
