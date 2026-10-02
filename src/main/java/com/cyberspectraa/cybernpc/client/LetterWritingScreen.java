package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.LetterSubmitPacket;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LetterWritingScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 142;

    private final InteractionHand hand;
    private final List<String> knownOnlinePlayers = new ArrayList<>();

    private EditBox recipientBox;
    private EditBox messageBox;
    private int selectedPlayer = -1;
    private int left;
    private int top;

    public LetterWritingScreen(InteractionHand hand) {
        super(Component.literal("Write Letter"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;

        recipientBox = new EditBox(
                font,
                left + 42,
                top + 27,
                136,
                18,
                Component.literal("Recipient")
        );
        recipientBox.setMaxLength(32);
        addRenderableWidget(recipientBox);

        messageBox = new EditBox(
                font,
                left + 18,
                top + 65,
                PANEL_WIDTH - 36,
                20,
                Component.literal("Message")
        );
        messageBox.setMaxLength(256);
        addRenderableWidget(messageBox);

        addRenderableWidget(
                Button.builder(
                                Component.literal("<"),
                                button -> cycleRecipient(-1)
                        )
                        .bounds(left + 18, top + 27, 20, 18)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(">"),
                                button -> cycleRecipient(1)
                        )
                        .bounds(left + 182, top + 27, 20, 18)
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal("Seal Letter"),
                                button -> submit()
                        )
                        .bounds(left + 48, top + 101, 124, 20)
                        .build()
        );

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            String self = minecraft.player == null
                    ? ""
                    : minecraft.player.getGameProfile().getName();

            knownOnlinePlayers.addAll(
                    minecraft.getConnection()
                            .getOnlinePlayers()
                            .stream()
                            .map(PlayerInfo::getProfile)
                            .map(GameProfile::getName)
                            .filter(name -> !name.equalsIgnoreCase(self))
                            .sorted(Comparator.naturalOrder())
                            .toList()
            );
        }

        if (!knownOnlinePlayers.isEmpty()) {
            selectedPlayer = 0;
            recipientBox.setValue(knownOnlinePlayers.get(0));
        }

        setInitialFocus(messageBox);
    }

    private void cycleRecipient(int direction) {
        if (knownOnlinePlayers.isEmpty()) {
            return;
        }

        selectedPlayer += direction;
        if (selectedPlayer < 0) {
            selectedPlayer = knownOnlinePlayers.size() - 1;
        } else if (selectedPlayer >= knownOnlinePlayers.size()) {
            selectedPlayer = 0;
        }

        recipientBox.setValue(
                knownOnlinePlayers.get(selectedPlayer)
        );
    }

    private void submit() {
        String recipient = recipientBox.getValue().trim();
        String message = messageBox.getValue().trim();

        if (recipient.isEmpty() || message.isEmpty()) {
            return;
        }

        CyberNpcNetwork.CHANNEL.sendToServer(
                new LetterSubmitPacket(
                        recipient,
                        message,
                        hand == InteractionHand.OFF_HAND
                )
        );

        onClose();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        // Compact parchment-like card. No giant inventory/menu frame.
        graphics.fill(
                left - 3,
                top - 3,
                left + PANEL_WIDTH + 3,
                top + PANEL_HEIGHT + 3,
                0xFF3B2A1E
        );
        graphics.fill(
                left,
                top,
                left + PANEL_WIDTH,
                top + PANEL_HEIGHT,
                0xFFF1D7A1
        );
        graphics.fill(
                left + 8,
                top + 52,
                left + PANEL_WIDTH - 8,
                top + 54,
                0xFF9E7546
        );

        graphics.drawCenteredString(
                font,
                "Letter",
                left + PANEL_WIDTH / 2,
                top + 8,
                0xFF3B2A1E
        );

        graphics.drawString(
                font,
                "To:",
                left + 18,
                top + 32,
                0xFF3B2A1E,
                false
        );

        graphics.drawString(
                font,
                "Message",
                left + 18,
                top + 57,
                0xFF3B2A1E,
                false
        );

        graphics.drawCenteredString(
                font,
                "Use arrows for online players or type a known name.",
                left + PANEL_WIDTH / 2,
                top + 126,
                0xFF6A5135
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
