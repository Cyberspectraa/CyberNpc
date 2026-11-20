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
            line(graphics, mid, baseY + 46, "The world you leave behind will remember you.");
        } else {
            heading(graphics, mid, baseY, "BEYOND THE FRACTURE");
            line(graphics, mid, baseY + 27, "Where the Rift leads is not yet known.");
            line(graphics, mid, baseY + 46, "But this is not where your story ends...");
            line(graphics, mid, baseY + 70, "Until the next chapter.");
        }

        graphics.drawCenteredString(font, "A CYBERSPECTRA STORY", mid,
                height - 42, 0xFF77688E);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void heading(GuiGraphics g, int x, int y, String text) {
        g.drawCenteredString(font, text, x, y, 0xFFD3A5FF);
    }

    private void line(GuiGraphics g, int x, int y, String text) {
        g.drawCenteredString(font, text, x, y, 0xFFE7DFF2);
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
