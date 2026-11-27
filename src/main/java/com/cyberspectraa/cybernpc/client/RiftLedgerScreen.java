package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.RiftLedgerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class RiftLedgerScreen extends Screen {
    private final RiftLedgerPacket snapshot;
    private boolean npcTab = true;
    private int page;
    private Button previous, next, npcButton, playerButton;
    private RiftLedgerScreen(RiftLedgerPacket snapshot) {
        super(Component.literal("Rift Departure Ledger"));
        this.snapshot = snapshot;
    }
    public static void open(RiftLedgerPacket snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof RiftCreditsScreen)
                && !(client.screen instanceof RiftDecisionScreen)) {
            client.setScreen(new RiftLedgerScreen(snapshot));
        }
    }
    private List<String> lines() { return npcTab ? snapshot.npcs() : snapshot.players(); }
    private int perPage() { return Math.max(1, (height - 112) / 13); }
    private int pages() { return Math.max(1, (lines().size() + perPage() - 1) / perPage()); }
    private void updateButtons() {
        page = Math.max(0, Math.min(page, pages() - 1));
        previous.active = page > 0;
        next.active = page + 1 < pages();
        npcButton.active = !npcTab;
        playerButton.active = npcTab;
    }
    @Override protected void init() {
        int center = width / 2;
        int w = Math.min(108, (width - 24) / 2);
        npcButton = addRenderableWidget(Button.builder(Component.literal("NPCs"), b -> {
            npcTab = true; page = 0; updateButtons();
        }).bounds(center - w - 2, 23, w, 20).build());
        playerButton = addRenderableWidget(Button.builder(Component.literal("Players"), b -> {
            npcTab = false; page = 0; updateButtons();
        }).bounds(center + 2, 23, w, 20).build());
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page--; updateButtons();
        }).bounds(center - 65, height - 49, 28, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++; updateButtons();
        }).bounds(center + 37, height - 49, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(center - 55, height - 24, 110, 20).build());
        updateButtons();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RiftUiStyle.background(graphics, width, height);
        RiftUiStyle.centered(graphics, font, width, width / 2, 9,
                "RIFT DEPARTURE LEDGER", RiftUiStyle.HEADING, 1F);
        List<String> rows = lines();
        int start = page * perPage();
        int left = Math.max(8, (width - 350) / 2);
        int right = width - left;
        for (int i = start; i < Math.min(rows.size(), start + perPage()); i++) {
            int y = 48 + (i - start) * 13;
            graphics.fill(left, y - 1, right, y + 11, i % 2 == 0 ? 0xFF24221F : 0xFF1B1917);
            String line = font.plainSubstrByWidth(rows.get(i), right - left - 8);
            graphics.drawString(font, line, left + 4, y + 1, RiftUiStyle.TEXT, false);
            if (!line.equals(rows.get(i)) && mouseX >= left && mouseX < right
                    && mouseY >= y - 1 && mouseY < y + 11) {
                graphics.renderTooltip(font, Component.literal(rows.get(i)), mouseX, mouseY);
            }
        }
        if (rows.isEmpty()) graphics.drawCenteredString(font, "No completed departures", width / 2, 54, RiftUiStyle.DETAIL);
        graphics.drawCenteredString(font, (page + 1) + " / " + pages(), width / 2, height - 43, RiftUiStyle.DETAIL);
        String count = npcTab ? "Seven named characters" : snapshot.totalPlayers() + " recorded departures";
        if (!npcTab && snapshot.totalPlayers() > rows.size()) count += " (latest " + rows.size() + " shown)";
        RiftUiStyle.centered(graphics, font, width, width / 2, height - 61, count, RiftUiStyle.DETAIL, 1F);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
}
