package com.cyberspectraa.cybernpc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Players who have completed race and class selection remain concealed
 * while another newcomer is being welcomed by the Pope.
 * The server closes this screen before starting the built-in camera scene.
 */
public final class SummoningQueueScreen extends Screen {
    private long start = System.currentTimeMillis();

    public SummoningQueueScreen() {
        super(Component.literal("Awaiting Summoning"));
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof SummoningQueueScreen))
            minecraft.setScreen(new SummoningQueueScreen());
    }

    public static void closeFromServer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SummoningQueueScreen)
            minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Entirely hide the staging world behind a dark customiser-themed
        // backdrop, including first-person hands and inventory.
        g.fill(0, 0, width, height, 0xFF101016);
        g.fill(width/2-127, height/2-55, width/2+127, height/2+55, 0xFF272029);
        g.fill(width/2-125, height/2-53, width/2+125, height/2+53, 0xFF1A1520);
        g.drawCenteredString(font, "YOUR CHARACTER IS READY", width/2, height/2-32, 0xFFFFD59B);
        g.drawCenteredString(font, "Waiting for the summoning circle...", width/2, height/2-11, 0xFFE5E0D4);
        g.drawCenteredString(font, "The Pope is welcoming another adventurer.",
                width/2, height/2+4, 0xFFB6A8C6);
        int dots = 1 + (int)((System.currentTimeMillis()-start)/700 % 3);
        g.drawCenteredString(font, ".".repeat(dots), width/2, height/2+25, 0xFFFFD59B);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
