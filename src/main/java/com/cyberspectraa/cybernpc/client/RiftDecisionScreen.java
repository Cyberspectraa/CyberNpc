package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.RiftChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A simple Minecraft GUI, matching the Rift credits. This is a confirmation
 * screen, not a cutscene: no artificial gradients, glows or moving text.
 *
 * Keep the existing server-authoritative, opt-in Rift handoff unchanged.
 */
public final class RiftDecisionScreen extends Screen {
    private boolean answered;
    private boolean waiting;
    private int waitingTicks;
    private Button yesButton;
    private Button stayButton;
    private int buttonsTop;

    private RiftDecisionScreen() {
        super(Component.literal("The Final Crossing"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof RiftCreditsScreen)
                && !(client.screen instanceof RiftDecisionScreen)) {
            client.setScreen(new RiftDecisionScreen());
        }
    }

    @Override
    protected void init() {
        super.init();
        int center = width / 2;
        // Keep space for the shared footer even at GUI scale 4 on 720p.
        buttonsTop = Math.min(height - 62, height / 2 + 37);
        int buttonWidth = Math.min(136, Math.max(72, (width - 34) / 2));
        int gap = 8;
        stayButton = addRenderableWidget(Button.builder(
                Component.literal("Stay on the Island"), b -> answer(false))
                .bounds(center - gap / 2 - buttonWidth, buttonsTop, buttonWidth, 20).build());
        yesButton = addRenderableWidget(Button.builder(
                Component.literal("Enter the Rift"), b -> answer(true))
                .bounds(center + gap / 2, buttonsTop, buttonWidth, 20).build());
    }

    private void answer(boolean enter) {
        if (answered) return;
        answered = true;
        CyberNpcNetwork.CHANNEL.sendToServer(new RiftChoicePacket(enter));
        if (enter) {
            // Only the server can begin the farewell. Leave the screen visible
            // until its credits packet arrives; never silently fail.
            waiting = true;
            yesButton.active = false;
            stayButton.active = false;
            // Replace the buttons with the waiting caption instead of
            // showing both a confirmation choice and a pending transition.
            yesButton.visible = false;
            stayButton.visible = false;
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }

    /** A stale invitation must explain the failure rather than trapping UI. */
    public static void rejected(String reason) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof RiftDecisionScreen) {
            client.setScreen(null);
        }
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(reason), false);
        }
    }

    @Override
    public void tick() {
        if (waiting && ++waitingTicks >= 160) {
            rejected("The Rift did not respond. Step away and try entering again.");
        }
    }

    @Override
    public void onClose() {
        if (!answered) answer(false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RiftUiStyle.background(graphics, width, height);
        int center = width / 2;
        int midY = height / 2;

        RiftUiStyle.centered(graphics, font, width, center, midY - 57,
                "THE FRACTURED ISLAND", RiftUiStyle.HEADING, 1F);
        RiftUiStyle.centered(graphics, font, width, center, midY - 28,
                "Leave through the Rift?", RiftUiStyle.TEXT, 1F);
        RiftUiStyle.centered(graphics, font, width, center, midY - 9,
                "You cannot return once you leave.",
                RiftUiStyle.CAUTION, 1F);
        RiftUiStyle.centered(graphics, font, width, center,
                midY + (height < 190 ? 3 : 10),
                "The credits will play before you disconnect.",
                RiftUiStyle.DETAIL, 1F);

        if (waiting) {
            RiftUiStyle.centered(graphics, font, width, center,
                    buttonsTop + 6, "Opening the Rift...",
                    RiftUiStyle.DETAIL, 1F);
        }

        RiftUiStyle.footer(graphics, font, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
