package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.LetterSubmitPacket;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LetterWritingScreen extends Screen {
    private static final int MAX_MESSAGE = 256;
    private static final int PLAYERS_PER_PAGE = 5;

    private final InteractionHand hand;
    private final List<String> onlinePlayers = new ArrayList<>();
    private final List<Button> playerButtons = new ArrayList<>();

    private EditBox recipientBox;
    private MultiLineEditBox messageBox;
    private Button previousButton;
    private Button nextButton;
    private Button sealButton;

    private int page;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public LetterWritingScreen(InteractionHand hand) {
        super(Component.literal("Write Letter"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(334, Math.max(286, width - 28));
        panelHeight = Math.min(216, Math.max(190, height - 28));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        loadOnlinePlayers();

        int selectorWidth = 104;
        int writingLeft = left + selectorWidth + 16;
        int writingWidth = panelWidth - selectorWidth - 28;

        recipientBox = new EditBox(
                font,
                writingLeft,
                top + 34,
                writingWidth,
                18,
                Component.literal("Recipient")
        );
        recipientBox.setMaxLength(32);
        recipientBox.setHint(Component.literal("Player name"));
        recipientBox.setResponder(value -> updateSealButton());
        addRenderableWidget(recipientBox);

        messageBox = new MultiLineEditBox(
                font,
                writingLeft,
                top + 72,
                writingWidth,
                86,
                Component.literal("Letter message"),
                Component.literal("Write your letter here...")
        );
        messageBox.setCharacterLimit(MAX_MESSAGE);
        messageBox.setValueListener(value -> updateSealButton());
        addRenderableWidget(messageBox);

        for (int i = 0; i < PLAYERS_PER_PAGE; i++) {
            final int slot = i;
            Button button = Button.builder(
                            Component.empty(),
                            pressed -> choosePlayer(slot)
                    )
                    .bounds(
                            left + 12,
                            top + 40 + i * 23,
                            selectorWidth - 20,
                            20
                    )
                    .build();
            playerButtons.add(button);
            addRenderableWidget(button);
        }

        previousButton = Button.builder(
                        Component.literal("Prev"),
                        pressed -> changePage(-1)
                )
                .bounds(
                        left + 12,
                        top + panelHeight - 40,
                        40,
                        18
                )
                .build();

        nextButton = Button.builder(
                        Component.literal("Next"),
                        pressed -> changePage(1)
                )
                .bounds(
                        left + 56,
                        top + panelHeight - 40,
                        40,
                        18
                )
                .build();

        addRenderableWidget(previousButton);
        addRenderableWidget(nextButton);

        sealButton = Button.builder(
                        Component.literal("Seal Letter"),
                        pressed -> submit()
                )
                .bounds(
                        writingLeft + Math.max(0, (writingWidth - 112) / 2),
                        top + panelHeight - 34,
                        112,
                        20
                )
                .build();
        addRenderableWidget(sealButton);

        refreshPlayerPage();
        updateSealButton();
        setInitialFocus(messageBox);
    }

    private void loadOnlinePlayers() {
        onlinePlayers.clear();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }

        String self = minecraft.player == null
                ? ""
                : minecraft.player.getGameProfile().getName();

        onlinePlayers.addAll(
                minecraft.getConnection()
                        .getOnlinePlayers()
                        .stream()
                        .map(PlayerInfo::getProfile)
                        .map(GameProfile::getName)
                        .filter(name -> !name.equalsIgnoreCase(self))
                        .sorted(Comparator.comparing(
                                name -> name.toLowerCase(
                                        java.util.Locale.ROOT
                                )
                        ))
                        .toList()
        );
    }

    private void choosePlayer(int slot) {
        int index = page * PLAYERS_PER_PAGE + slot;
        if (index < 0 || index >= onlinePlayers.size()) {
            return;
        }

        recipientBox.setValue(onlinePlayers.get(index));
        updateSealButton();
    }

    private void changePage(int direction) {
        int maxPage = onlinePlayers.isEmpty()
                ? 0
                : (onlinePlayers.size() - 1) / PLAYERS_PER_PAGE;

        page = Math.max(0, Math.min(maxPage, page + direction));
        refreshPlayerPage();
    }

    private void refreshPlayerPage() {
        int start = page * PLAYERS_PER_PAGE;

        for (int i = 0; i < playerButtons.size(); i++) {
            Button button = playerButtons.get(i);
            int index = start + i;

            if (index < onlinePlayers.size()) {
                button.setMessage(Component.literal(
                        onlinePlayers.get(index)
                ));
                button.visible = true;
                button.active = true;
            } else {
                button.setMessage(Component.empty());
                button.visible = false;
                button.active = false;
            }
        }

        int maxPage = onlinePlayers.isEmpty()
                ? 0
                : (onlinePlayers.size() - 1) / PLAYERS_PER_PAGE;

        previousButton.active = page > 0;
        nextButton.active = page < maxPage;
    }

    private void updateSealButton() {
        if (sealButton == null
                || recipientBox == null
                || messageBox == null) {
            return;
        }

        sealButton.active = !recipientBox.getValue().trim().isEmpty()
                && !messageBox.getValue().trim().isEmpty();
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

        // Dark leather-like outside edge.
        graphics.fill(
                left - 4,
                top - 4,
                left + panelWidth + 4,
                top + panelHeight + 4,
                0xFF3A281A
        );

        // Main parchment.
        graphics.fill(
                left,
                top,
                left + panelWidth,
                top + panelHeight,
                0xFFF0D5A0
        );

        // Slightly darker recipient selector column.
        graphics.fill(
                left + 6,
                top + 28,
                left + 108,
                top + panelHeight - 12,
                0xFFE4C389
        );

        // Divider.
        graphics.fill(
                left + 112,
                top + 25,
                left + 114,
                top + panelHeight - 14,
                0xFF9B7344
        );

        graphics.drawCenteredString(
                font,
                "Write Letter",
                left + panelWidth / 2,
                top + 10,
                0xFF3B2A1E
        );

        graphics.drawString(
                font,
                "Online Players",
                left + 14,
                top + 30,
                0xFF4B3521,
                false
        );

        if (onlinePlayers.isEmpty()) {
            graphics.drawWordWrap(
                    font,
                    Component.literal(
                            "No other players are online. You can still type a known player name."
                    ),
                    left + 14,
                    top + 52,
                    86,
                    0xFF6A5135
            );
        }

        int writingLeft = left + 120;
        graphics.drawString(
                font,
                "To",
                writingLeft,
                top + 24,
                0xFF4B3521,
                false
        );

        graphics.drawString(
                font,
                "Message",
                writingLeft,
                top + 60,
                0xFF4B3521,
                false
        );

        if (messageBox != null) {
            String counter = messageBox.getValue().length()
                    + "/" + MAX_MESSAGE;
            graphics.drawString(
                    font,
                    counter,
                    left + panelWidth - 12
                            - font.width(counter),
                    top + 162,
                    0xFF6A5135,
                    false
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
