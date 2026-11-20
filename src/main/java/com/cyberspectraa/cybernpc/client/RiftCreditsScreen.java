package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.RiftCreditsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Private, non-skippable 42-second farewell. The server owns the end time,
 * invisibility, saved record and disconnect. This screen displays no fake
 * statistics or invented claims about the next series.
 */
public final class RiftCreditsScreen extends Screen {
    private final RiftCreditsPacket summary;
    private int elapsed;

    private RiftCreditsScreen(RiftCreditsPacket summary) {
        super(Component.literal("Beyond the Rift"));
        this.summary = summary;
    }

    public static void open(RiftCreditsPacket summary) {
        Minecraft.getInstance().setScreen(new RiftCreditsScreen(summary));
    }

    @Override
    public void tick() {
        elapsed++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int mid = width / 2;
        int baseY = height / 2 - 31;
        graphics.fill(0, 0, width, height, 0xFF0B0911);
        // Slow, deterministic void motes; entirely visual and client-only.
        // Nothing depends on frame rate or world ticks except their motion.
        for (int i = 0; i < 48; i++) {
            int starX = Math.floorMod(i * 167 + elapsed / 5, Math.max(1, width));
            int starY = Math.floorMod(i * 89 + elapsed / 12, Math.max(1, height));
            graphics.fill(starX, starY, starX + 1, starY + 1,
                    i % 3 == 0 ? 0xFF66478F : 0xFF30243E);
        }
        graphics.fill(0, 0, width, 3, 0xFF8B62C2);
        graphics.fill(width / 2 - Math.min(120, width / 3), height - 24,
                width / 2 + Math.min(120, width / 3), height - 23, 0xFF47345F);

        int seconds = elapsed / 20;
        if (seconds < 7) {
            heading(graphics, mid, baseY, "THE FRACTURED ISLAND");
            line(graphics, mid, baseY + 25, "One island. Countless stories.");
            line(graphics, mid, baseY + 43, "And one journey that was yours.");
        } else if (seconds < 15) {
            heading(graphics, mid, baseY, "YOUR JOURNEY");
            line(graphics, mid, baseY + 22, "Time spent here: " + playtime(summary.playTicks()));
            line(graphics, mid, baseY + 38, "Creatures defeated: " + summary.mobKills());
            line(graphics, mid, baseY + 54, "Times fallen: " + summary.deaths());
        } else if (seconds < 23) {
            heading(graphics, mid, baseY, "THE PEOPLE YOU MET");
            line(graphics, mid, baseY + 22, "Conversations with the Seven: " + summary.conversations());
            line(graphics, mid, baseY + 38, "You spoke most with: " + summary.favouriteName());
            line(graphics, mid, baseY + 54, "Visits: " + summary.favouriteVisits());
            if (!summary.runnerUpName().isBlank() && summary.runnerUpVisits() > 0) {
                line(graphics, mid, baseY + 70,
                        "Also remembered: " + summary.runnerUpName());
            }
        } else if (seconds < 30) {
            heading(graphics, mid, baseY, "THE BONDS YOU MADE");
            line(graphics, mid, baseY + 25, "Town friendships formed: " + summary.friends() + " / 7");
            line(graphics, mid, baseY + 45, "Every conversation left its mark.");
            line(graphics, mid, baseY + 63, "Every choice became part of your story.");
        } else if (seconds < 36) {
            heading(graphics, mid, baseY, "THANK YOU FOR PLAYING");
            line(graphics, mid, baseY + 27, "Thank you for spending your time on the island.");
            line(graphics, mid, baseY + 46, "The memories you made here will stay with you.");
        } else {
            heading(graphics, mid, baseY, "BEYOND THE FRACTURE");
            line(graphics, mid, baseY + 27, "Beyond the island, another world awaits.");
            line(graphics, mid, baseY + 46, "Beneath unfamiliar skies, a new chapter begins.");
            line(graphics, mid, baseY + 70, "Until we meet again.");
        }

        graphics.drawCenteredString(font, "A CYBERSPECTRA STORY", mid,
                height - 42, 0xFF77688E);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void heading(GuiGraphics g, int x, int y, String text) {
        fittedText(g, x, y, text, (slideAlpha() << 24) | 0xD3A5FF);
    }

    private void line(GuiGraphics g, int x, int y, String text) {
        fittedText(g, x, y, text, (slideAlpha() << 24) | 0xE7DFF2);
    }

    /** Scale down long personal names and stats instead of clipping them. */
    private void fittedText(GuiGraphics g, int x, int y, String text, int color) {
        float scale = Math.min(1.0F,
                (float) Math.max(40, width - 24) / Math.max(1, font.width(text)));
        if (scale == 1.0F) {
            g.drawCenteredString(font, text, x, y, color);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0F);
        g.pose().scale(scale, scale, 1F);
        g.drawCenteredString(font, text, 0, 0, color);
        g.pose().popPose();
    }

    private int slideAlpha() {
        int[] bounds = {0, 140, 300, 460, 600, 720, 840};
        for (int i = 0; i < bounds.length - 1; i++) {
            if (elapsed < bounds[i + 1]) {
                int edge = Math.min(elapsed - bounds[i], bounds[i + 1] - elapsed);
                return Math.max(0, Math.min(255, edge * 255 / 17));
            }
        }
        return 255;
    }

    private static String playtime(int ticks) {
        long minutes = Math.max(0L, ticks / 1200L);
        long hours = minutes / 60L;
        return hours + "h " + (minutes % 60L) + "m";
    }

    @Override
    public void onClose() {
        // The irrevocable departure is controlled by the server, not Escape.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
