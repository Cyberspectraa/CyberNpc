package com.cyberspectraa.cybernpc.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Small shared palette for the final crossing and its credits.
 *
 * Use Minecraft's pixel font, a dark neutral background, muted parchment
 * colours and two single-purpose rules. No decorative textures or shaders.
 */
final class RiftUiStyle {
    static final int BACKGROUND = 0xFF101010;
    static final int BORDER = 0xFF39342C;
    static final int HEADING = 0xFFE7D2A7;
    static final int TEXT = 0xFFE5E5E5;
    static final int DETAIL = 0xFFA8A39B;
    static final int CAUTION = 0xFFE3B87A;

    private RiftUiStyle() {}

    static void background(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fill(0, 0, width, 2, BORDER);
        graphics.fill(0, height - 2, width, height, BORDER);
    }

    static void footer(GuiGraphics graphics, Font font, int width, int height) {
        int middle = width / 2;
        graphics.fill(middle - Math.min(72, width / 4), height - 30,
                middle + Math.min(72, width / 4), height - 29, BORDER);
        graphics.drawCenteredString(font, "FRACTURED ISLAND",
                middle, height - 23, DETAIL);
    }

    static void centered(GuiGraphics graphics, Font font, int width,
                         int centerX, int y, String value, int rgb, float opacity) {
        int alpha = Math.round(255F * Math.max(0F, Math.min(1F, opacity)));
        if (alpha <= 3) return;
        int color = (alpha << 24) | (rgb & 0xFFFFFF);
        // Only fit long credits when strictly required by the screen width.
        // Font size and position never change over the course of a fade.
        float scale = Math.min(1F,
                (float) Math.max(40, width - 24) / Math.max(1, font.width(value)));
        if (scale >= 1F) {
            graphics.drawCenteredString(font, value, centerX, y, color);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, y, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawCenteredString(font, value, 0, 0, color);
            graphics.pose().popPose();
        }
    }

    static float smooth(float value) {
        float t = Math.max(0F, Math.min(1F, value));
        return t * t * (3F - 2F * t);
    }

    /**
     * Fade through an empty, dark pause at every cut. Two pages are never
     * visible together and therefore cannot produce double-exposed text.
     */
    static float cardOpacity(float time, int start, int end) {
        final float fade = 18F;
        final float gap = 6F;
        float fadeIn = smooth((time - start - gap) / fade);
        float fadeOut = smooth((end - gap - time) / fade);
        return Math.min(fadeIn, fadeOut);
    }
}
