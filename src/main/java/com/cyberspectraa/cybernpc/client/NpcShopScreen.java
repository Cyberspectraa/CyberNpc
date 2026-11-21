package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.economy.CurrencyValue;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.ShopSnapshotPacket;
import com.cyberspectraa.cybernpc.network.ShopTradePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact vanilla-style shop ledger: a merchant name, player wallet,
 * Minecraft item sprites, prices, quantities and separate Buy/Sell tabs.
 *
 * Client is only a viewer; the server owns offers, sessions and transactions.
 */
public final class NpcShopScreen extends Screen {
    private static final int PANEL_BG = 0xF3272018;
    private static final int PANEL_EDGE = 0xFF785A37;
    private static final int SLOT_BG = 0xFF39352E;
    private static final int TEXT = 0xFFEDE2C6;
    private static final int SUBTEXT = 0xFFB8A98B;

    private final ShopSnapshotPacket snapshot;
    private final boolean selling;
    private final int page;
    private final List<Button> rowButtons = new ArrayList<>();
    private int panelX, panelY, panelW, panelH, rows;
    private boolean waiting;
    private int waitingTicks;

    private NpcShopScreen(ShopSnapshotPacket snapshot, boolean selling, int page) {
        super(Component.literal("Shopkeeper"));
        this.snapshot = snapshot;
        this.selling = selling;
        this.page = page;
    }

    public static void show(ShopSnapshotPacket snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof NpcShopScreen old
                && old.snapshot.npcId() == snapshot.npcId()) {
            client.setScreen(new NpcShopScreen(snapshot, old.selling, old.page));
        } else {
            client.setScreen(new NpcShopScreen(snapshot, false, 0));
        }
    }

    public static void closeFromServer(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof NpcShopScreen) client.setScreen(null);
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }

    private List<ShopSnapshotPacket.Line> stock() {
        return selling ? snapshot.sell() : snapshot.buy();
    }

    private int pageCount() {
        return Math.max(1, (stock().size() + rows - 1) / rows);
    }

    @Override
    protected void init() {
        super.init();
        rowButtons.clear();
        rows = Math.max(2, Math.min(5, (height - 120) / 28));
        panelW = Math.min(302, Math.max(176, width - 18));
        panelH = 104 + rows * 28;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        addRenderableWidget(Button.builder(Component.literal("Buy"),
                b -> changeTab(false)).bounds(panelX + 10, panelY + 26, 52, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Sell"),
                b -> changeTab(true)).bounds(panelX + 67, panelY + 26, 52, 20).build());

        int start = Math.min(page, pageCount() - 1) * rows;
        for (int i = 0; i < rows; i++) {
            int absoluteIndex = start + i;
            Button button = Button.builder(Component.literal(selling ? "Sell" : "Buy"),
                    b -> trade(absoluteIndex))
                    .bounds(panelX + panelW - 60, panelY + 52 + 28 * i, 49, 20)
                    .build();
            button.active = absoluteIndex < stock().size();
            if (button.active) {
                ShopSnapshotPacket.Line offer = stock().get(absoluteIndex);
                button.setTooltip(Tooltip.create(Component.literal(offer.count() + " x "
                        + offer.item().getHoverName().getString() + " - "
                        + CurrencyValue.formatCoins(offer.price()))));
            }
            rowButtons.add(addRenderableWidget(button));
        }

        Button back = addRenderableWidget(Button.builder(Component.literal("<"),
                b -> changePage(-1)).bounds(panelX + 12, panelY + panelH - 24, 22, 18).build());
        Button next = addRenderableWidget(Button.builder(Component.literal(">"),
                b -> changePage(1)).bounds(panelX + 38, panelY + panelH - 24, 22, 18).build());
        back.active = page > 0;
        next.active = page + 1 < pageCount();
        addRenderableWidget(Button.builder(Component.literal("Done"),
                b -> onClose()).bounds(panelX + panelW - 70,
                        panelY + panelH - 25, 58, 20).build());
    }

    private void changeTab(boolean tab) {
        if (waiting || selling == tab) return;
        Minecraft.getInstance().setScreen(new NpcShopScreen(snapshot, tab, 0));
    }

    private void changePage(int direction) {
        if (waiting) return;
        int nextPage = page + direction;
        if (nextPage >= 0 && nextPage < pageCount()) {
            Minecraft.getInstance().setScreen(new NpcShopScreen(snapshot, selling, nextPage));
        }
    }

    private void trade(int index) {
        if (waiting || index < 0 || index >= stock().size()) return;
        waiting = true;
        for (Button button : rowButtons) button.active = false;
        CyberNpcNetwork.CHANNEL.sendToServer(
                new ShopTradePacket(snapshot.npcId(), selling, index));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // World remains visible behind a modest Minecraft-style shop panel.
        graphics.fill(panelX - 2, panelY - 2, panelX + panelW + 2,
                panelY + panelH + 2, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.fill(panelX + 7, panelY + 49,
                panelX + panelW - 7, panelY + 50, PANEL_EDGE);
        graphics.fill(panelX + 7, panelY + panelH - 49,
                panelX + panelW - 7, panelY + panelH - 48, PANEL_EDGE);

        String merchantTitle = font.plainSubstrByWidth(snapshot.merchant() + "'s Shop",
                Math.max(60, panelW - 24));
        graphics.drawString(font, merchantTitle, panelX + 10, panelY + 8, TEXT, false);
        String wallet = "Coins: " + CurrencyValue.formatCoins(snapshot.wallet());
        graphics.drawString(font, font.plainSubstrByWidth(wallet,
                Math.max(80, panelW - 136)), panelX + 129, panelY + 32, SUBTEXT, false);

        List<ShopSnapshotPacket.Line> stock = stock();
        int pageIndex = Math.min(page, pageCount() - 1);
        int start = pageIndex * rows;
        for (int i = 0; i < rows && i + start < stock.size(); i++) {
            ShopSnapshotPacket.Line offer = stock.get(start + i);
            int x = panelX + 11, y = panelY + 52 + 28 * i;
            graphics.fill(x, y + 1, x + 19, y + 20, SLOT_BG);
            graphics.renderItem(offer.item(), x + 2, y + 2);
            String label = offer.count() + " x " + offer.item().getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(label,
                    Math.max(65, panelW - 104)), x + 25, y + 2, TEXT, false);
            graphics.drawString(font,
                    font.plainSubstrByWidth(CurrencyValue.formatCoins(offer.price()),
                            Math.max(65, panelW - 104)), x + 25, y + 13, SUBTEXT, false);
        }
        if (stock.isEmpty()) {
            graphics.drawCenteredString(font,
                    selling ? "This shop isn't buying anything yet."
                            : "This shop has no goods listed yet.",
                    width / 2, panelY + 64, SUBTEXT);
        }
        String message = waiting ? "Trading..."
                : (snapshot.message().isBlank()
                ? (selling ? "Sell items from your inventory." : "Buy using physical coins.")
                : snapshot.message());
        graphics.drawString(font, font.plainSubstrByWidth(message,
                Math.max(80, panelW - 20)), panelX + 10, panelY + panelH - 39,
                SUBTEXT, false);
        graphics.drawString(font, (pageIndex + 1) + "/" + pageCount(),
                panelX + 67, panelY + panelH - 20, SUBTEXT, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        if (waiting && ++waitingTicks >= 160) {
            closeFromServer("The Shopkeeper did not respond. Please try again.");
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
