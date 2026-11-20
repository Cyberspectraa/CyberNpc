package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.RiftChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The only way to begin the finale: a deliberate Yes. Escape always declines. */
public final class RiftDecisionScreen extends Screen {
    private boolean answered;

    private RiftDecisionScreen() {
        super(Component.literal("The Final Crossing"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof RiftCreditsScreen)) {
            client.setScreen(new RiftDecisionScreen());
        }
    }

    @Override
    protected void init() {
        super.init();
        int center = width / 2;
        int y = Math.min(height - 55, height / 2 + 45);
        addRenderableWidget(Button.builder(Component.literal("Enter the Rift"),
                b -> answer(true)).bounds(center - 150, y, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Stay on the Island"),
                b -> answer(false)).bounds(center + 5, y, 145, 20).build());
    }

    private void answer(boolean enter) {
        if (answered) return;
        answered = true;
        CyberNpcNetwork.CHANNEL.sendToServer(new RiftChoicePacket(enter));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        answer(false);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE50B0912);
        graphics.fill(width / 2 - 163, height / 2 - 68,
                width / 2 + 163, height / 2 + 70, 0xB619152B);
        graphics.fill(width / 2 - 163, height / 2 - 68,
                width / 2 + 163, height / 2 - 66, 0xFFAA79D5);
        graphics.drawCenteredString(font, "THE FINAL CROSSING", width / 2,
                height / 2 - 53, 0xFFDCC4FF);
        graphics.drawCenteredString(font, "This Rift will take you away from the island.",
                width / 2, height / 2 - 28, 0xFFE9E2F0);
        graphics.drawCenteredString(font, "Your journey here will end.",
                width / 2, height / 2 - 13, 0xFFE9E2F0);
        graphics.drawCenteredString(font, "There is no turning back.",
                width / 2, height / 2 + 7, 0xFFFFC59F);
        graphics.drawCenteredString(font, "Finish anything you wish to leave behind.",
                width / 2, height / 2 + 22, 0xFFB9ACCE);
        super.render(graphics, x, y, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
