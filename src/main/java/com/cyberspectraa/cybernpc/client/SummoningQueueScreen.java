package com.cyberspectraa.cybernpc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * While another newcomer is greeted by the Pope, show a restrained holding
 * page in the same Minecraft-font style as the Rift credits and opening title.
 * Never expose the concealed staging world behind this screen.
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RiftUiStyle.background(graphics, width, height);
        int center = width / 2;
        int middle = height / 2;
        RiftUiStyle.centered(graphics, font, width, center, middle - 37,
                "AWAITING SUMMONING", RiftUiStyle.HEADING, 1F);
        RiftUiStyle.centered(graphics, font, width, center, middle - 10,
                "Your character is ready.", RiftUiStyle.TEXT, 1F);
        RiftUiStyle.centered(graphics, font, width, center, middle + 11,
                "The Pope is welcoming another adventurer.",
                RiftUiStyle.DETAIL, 1F);
        int dots = 1 + (int) ((System.currentTimeMillis() - start) / 700L % 3L);
        RiftUiStyle.centered(graphics, font, width, center, middle + 32,
                ".".repeat(dots), RiftUiStyle.HEADING, 1F);
        RiftUiStyle.footer(graphics, font, width, height);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
