package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/**
 * Optional client RPG HUD. All values are read from live client game state.
 * Never changes health, hunger, armour, Iron's mana or other game mechanics.
 * Hotbar, XP, mount health, air, status effects and crosshair stay vanilla.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerRpgHudOverlay {
    private static final int WIDTH = 258;
    private static final int HEIGHT = 115;
    private static final int PORTRAIT_WIDTH = 66;
    private static final int BAR_WIDTH = 163;

    private static final int EDGE = 0xE26F849A;
    private static final int PANEL = 0xDA101A29;
    private static final int PORTRAIT = 0xCB263447;
    private static final int WHITE = 0xFFECF2FA;
    private static final int MUTED = 0xFFACBDD0;
    private static final int HEALTH = 0xFFD95663;
    private static final int HEALTH_DARK = 0xFF482E38;
    private static final int MANA = 0xFF6594ED;
    private static final int MANA_DARK = 0xFF223752;
    private static final int FOOD = 0xFFE3B865;
    private static final int ARMOR = 0xFFAAC4D8;

    private static boolean shouldRender() {
        Minecraft mc = Minecraft.getInstance();
        return PlayerHudConfig.ENABLED.get()
                && !mc.options.hideGui
                && mc.screen == null
                && mc.player != null
                && !mc.player.isSpectator();
    }

    @SubscribeEvent
    public static void hideOriginalBars(RenderGuiOverlayEvent.Pre event) {
        if (!shouldRender() || !PlayerHudConfig.REPLACE_VANILLA.get()) return;
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())
                || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())
                || ("irons_spellbooks".equals(id.getNamespace())
                        && "mana_overlay".equals(id.getPath()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void draw(RenderGuiEvent.Post event) {
        if (!shouldRender()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;

        int x = Mth.clamp(PlayerHudConfig.X_OFFSET.get(),
                0, Math.max(0, gui.guiWidth() - WIDTH));
        int y = Mth.clamp(PlayerHudConfig.Y_OFFSET.get(),
                0, Math.max(0, gui.guiHeight() - HEIGHT));

        fillBorder(gui, x - 1, y - 1, WIDTH + 2, HEIGHT + 2,
                EDGE, PANEL);
        gui.fill(x + 4, y + 4, x + 4 + PORTRAIT_WIDTH, y + HEIGHT - 4,
                PORTRAIT);
        gui.fill(x + 4, y + 4, x + 4 + PORTRAIT_WIDTH, y + 5,
                0xFF7F93AC);

        if (PlayerHudConfig.CHARACTER_PREVIEW.get()) {
            drawPlayerPreview(gui, player, x, y);
        } else {
            gui.drawCenteredString(font, "CHARACTER",
                    x + 4 + PORTRAIT_WIDTH / 2, y + HEIGHT / 2 - 4, MUTED);
        }

        int right = x + 80;
        String name = font.plainSubstrByWidth(
                player.getName().getString(), BAR_WIDTH);
        gui.drawString(font, name, right, y + 7, WHITE, false);
        gui.fill(right, y + 20, right + BAR_WIDTH, y + 21, 0xFF4A5B70);

        int healthMaximum = Math.max(1, (int) Math.ceil(player.getMaxHealth()));
        float healthNow = Mth.clamp(player.getHealth(), 0.0F,
                (float) healthMaximum);
        String healthText = format(healthNow) + " / " + healthMaximum;
        drawBar(gui, font, right, y + 27, "HEALTH", healthText,
                healthNow / healthMaximum, HEALTH, HEALTH_DARK);

        var mana = IronsManaHudCompat.current(player);
        if (mana != null) {
            drawBar(gui, font, right, y + 57, "MANA",
                    mana.current() + " / " + mana.maximum(),
                    mana.current() / (float) mana.maximum(),
                    MANA, MANA_DARK);
        } else {
            gui.drawString(font, "MANA", right, y + 57, MUTED, false);
            gui.drawString(font, "Iron's Spells unavailable",
                    right, y + 70, MUTED, false);
        }

        int food = Mth.clamp(player.getFoodData().getFoodLevel(), 0, 20);
        int armour = Mth.clamp(player.getArmorValue(), 0, 20);
        gui.fill(right, y + 87, right + BAR_WIDTH, y + 88, 0xFF4A5B70);
        gui.drawString(font, "FOOD", right, y + 94, FOOD, false);
        drawRightText(gui, font, food + "/20", right + 74, y + 94, WHITE);
        gui.drawString(font, "ARMOUR", right + 84, y + 94, ARMOR, false);
        drawRightText(gui, font, armour + "/20",
                right + BAR_WIDTH, y + 94, WHITE);
    }

    private static String format(float amount) {
        if (amount == Math.floor(amount)) return Integer.toString((int) amount);
        return String.format(Locale.ROOT, "%.1f", amount);
    }

    private static void drawPlayerPreview(GuiGraphics gui, LocalPlayer player,
                                          int x, int y) {
        // InventoryScreen uses the player's actual entity renderer, so skins,
        // equipped armour, race render layers and animations are reused.
        // Clip oversized models; never leave a scissor enabled behind us.
        gui.enableScissor(x + 5, y + 6,
                x + 3 + PORTRAIT_WIDTH, y + HEIGHT - 5);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(gui,
                    x + 37, y + HEIGHT - 10,
                    26, 0.0F, 0.0F, player);
        } finally {
            gui.disableScissor();
        }
    }

    private static void drawBar(GuiGraphics gui, Font font, int x, int y,
                                String label, String value, float ratio,
                                int fill, int empty) {
        gui.drawString(font, label, x, y, fill, false);
        drawRightText(gui, font, value, x + BAR_WIDTH, y, WHITE);
        int top = y + 11;
        gui.fill(x, top, x + BAR_WIDTH, top + 10, 0xF2000000);
        gui.fill(x + 1, top + 1, x + BAR_WIDTH - 1, top + 9, empty);
        int filled = Math.round((BAR_WIDTH - 2) * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled > 0) {
            gui.fill(x + 1, top + 1, x + 1 + filled, top + 9, fill);
            gui.fill(x + 1, top + 1, x + 1 + filled, top + 3,
                    0x44FFFFFF);
        }
    }

    private static void drawRightText(GuiGraphics gui, Font font,
                                      String text, int right, int y, int colour) {
        gui.drawString(font, text, right - font.width(text), y, colour, false);
    }

    private static void fillBorder(GuiGraphics gui, int x, int y,
                                   int width, int height, int border, int inside) {
        gui.fill(x, y, x + width, y + height, border);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, inside);
    }

    private PlayerRpgHudOverlay() {}
}
