package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
 * of Souls Combat HUD. Narrow stone-rimmed gauges and a fixed 2D skin face.
 * No simulated stats; no change to core inventory/hotbar/crosshair rendering.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerRpgHudOverlay {
    private static final ResourceLocation FRAME =
            new ResourceLocation(CyberNpc.MOD_ID, "textures/gui/player_hud.png");
    // Render directly from the Minecraft 1.20.1 textures rather than copying
    // vanilla pixels into our own atlas. Resource packs can replace them.
    private static final ResourceLocation VANILLA_ICONS =
            new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final ResourceLocation VANILLA_MANA_ORB =
            new ResourceLocation("minecraft", "textures/item/heart_of_the_sea.png");
    private static final int ATLAS_W = 256;
    private static final int ATLAS_H = 96;
    // Each original texture row has a different length. Stitch the rightmost
    // eight pixels at the new shared gauge width to preserve its end cap.
    private static final int HEALTH_FRAME_WIDTH = 144;
    private static final int HUNGER_FRAME_WIDTH = 152;
    private static final int MANA_FRAME_WIDTH = 184;
    private static final int GAUGE_WIDTH = 140;
    private static final int MAX_W = 214;
    private static final int MAX_H = 68;

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
            // Also suppress the native gauge for a recognised zero-capacity
            // pool; otherwise it would reappear when our blue bar is hidden.
            // An unavailable optional API must not hide its HUD.
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

        // A perfectly still, pixel-sharp skin portrait. Draw the eight-by-
        // eight face region and optional hat/hair layer directly from the
        // locally cached player skin. This avoids inventory model rotation,
        // breathing/idle animation and view-angle changes.
        if (PlayerHudConfig.CHARACTER_PREVIEW.get()) {
            drawStaticFace(gui, player, x + 6, y + 8);
        }

        // Give the nine-pixel icons a clear column between portrait and bars.
        // They cannot sit inside a five-pixel-high bar fill without covering
        // the decorative frame rails.
        int bx = x + 68;
        int currentHealthMax = Math.max(1, Mth.ceil(player.getMaxHealth()));
        float health = Math.max(0, Math.min(player.getHealth(), currentHealthMax));
        gauge(gui, font, bx, y + 8, 0, HEALTH_FRAME_WIDTH, HudIcon.HEALTH, health / currentHealthMax,
                number(health) + "/" + currentHealthMax, RED_DARK, RED_LIGHT);

        int hunger = Mth.clamp(player.getFoodData().getFoodLevel(), 0, 20);
        gauge(gui, font, bx, y + 24, 16, HUNGER_FRAME_WIDTH, HudIcon.FOOD, hunger / 20.0F,
                hunger + "/20", GREEN_DARK, GREEN_LIGHT);

        IronsManaHudCompat.Mana mana = IronsManaHudCompat.current(player);
        // A recognised zero-capacity mana pool means the character has no
        // mana, not that the optional integration is unavailable. Hide this
        // gauge while still suppressing Iron's own native gauge below.
        if (mana != null && mana.maximum() > 0) {
            gauge(gui, font, bx, y + 40, 32, MANA_FRAME_WIDTH, HudIcon.MANA,
                    mana.current() / (float) Math.max(1, mana.maximum()),
                    mana.current() + "/" + mana.maximum(),
                    BLUE_DARK, BLUE_LIGHT);
        }

        // Armor stays immediately below the face. Hunger is already shown
        // inside its own gauge, so do not repeat the same number below.
        // Preserve the actual modded armor value rather than capping at 20.
        // Vanilla full armour badge from Minecraft's own 1.20.1 HUD atlas.
        gui.blit(VANILLA_ICONS, x + 13, y + 52, 16, 9, 9, 9, 256, 256);
        gui.blit(VANILLA_ICONS, x + 13, y + 52, 34, 9, 9, 9, 256, 256);
        gui.drawString(font, Integer.toString(player.getArmorValue()),
                x + 28, y + 53, WHITE, true);
    }

    /**
     * Player skins use the standard 64x64 atlas layout:
     * (8,8) is the face and (40,8) is the translucent outer hat layer.
     * The UV rectangles are fixed; no 3D pose, mouse direction or tick
     * interpolation affects this sprite.
     */
    private static void drawStaticFace(GuiGraphics gui, LocalPlayer player, int x, int y) {
        final int size = 38;

        // Simple 1-pixel stepped stone border, consistent with Minecraft UI.
        gui.fill(x - 4, y - 4, x + size + 4, y + size + 4, 0xFF241E1B);
        gui.fill(x - 3, y - 3, x + size + 3, y + size + 3, 0xFFB4A48A);
        gui.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xFF6B6155);
        gui.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF302C29);

        ResourceLocation skin = player.getSkinTextureLocation();
        gui.blit(skin, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
        gui.blit(skin, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
    }

    private static String number(float value) {
        if (value == Mth.floor(value)) return Integer.toString((int) value);
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private enum HudIcon { HEALTH, FOOD, MANA }

    private static void gauge(GuiGraphics gui, Font font,
                              int x, int y, int v, int originalWidth, HudIcon icon,
                              float fraction, String text, int base, int highlight) {
        // Preserve the native pixel-art left trim and far-right end cap when
        // shortening the three differently sized source sprites to one width.
        // Never scale the artwork: nearest-neighbor 1:1 pixels stay crisp.
        gui.blit(FRAME, x, y, 0, v, GAUGE_WIDTH - 8, 13, ATLAS_W, ATLAS_H);
        gui.blit(FRAME, x + GAUGE_WIDTH - 8, y,
                originalWidth - 8, v, 8, 13, ATLAS_W, ATLAS_H);

        int length = Mth.clamp(Math.round((GAUGE_WIDTH - 16)
                * Mth.clamp(fraction, 0.0F, 1.0F)), 0, GAUGE_WIDTH - 16);
        if (length > 0) {
            gui.fill(x + 8, y + 4, x + 8 + length, y + 9, base);
            gui.fill(x + 8, y + 4, x + 8 + length, y + 5, highlight);
            for (int dx = 12; dx < length - 1; dx += 18) {
                gui.fill(x + 8 + dx, y + 8, x + 9 + dx, y + 9,
                        0x60201B12);
            }
        }

        // The vanilla HUD draws a background silhouette and then a full
        // heart/drumstick on top of it. Use both 9x9 sprites so outlines
        // remain correct even when a resource pack replaces icons.png.
        // Minecraft has no mana icon, so use its 16x16 Heart of the Sea
        // item art as a compact blue magical orb instead of our droplet.
        switch (icon) {
            case HEALTH -> {
                gui.blit(VANILLA_ICONS, x - 14, y + 2,
                        16, 0, 9, 9, 256, 256);
                gui.blit(VANILLA_ICONS, x - 14, y + 2,
                        52, 0, 9, 9, 256, 256);
            }
            case FOOD -> {
                gui.blit(VANILLA_ICONS, x - 14, y + 2,
                        16, 27, 9, 9, 256, 256);
                gui.blit(VANILLA_ICONS, x - 14, y + 2,
                        52, 27, 9, 9, 256, 256);
            }
            case MANA -> gui.blit(VANILLA_MANA_ORB, x - 16, y + 1,
                    12, 12, 0.0F, 0.0F, 16, 16, 16, 16);
        }
        gui.drawString(font, text, x + 13, y + 2, WHITE, true);
    }

    private PlayerRpgHudOverlay() {}
}
