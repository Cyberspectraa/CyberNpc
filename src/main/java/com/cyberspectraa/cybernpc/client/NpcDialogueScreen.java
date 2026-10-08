package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.dialogue.DialogueView;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.DialogueChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * A bottom-of-screen, two-portrait conversation panel.
 *
 * NPC and player portraits are live Minecraft entity renders, not generated
 * images or generic dummy icons. Text is rendered on a vanilla-book-derived
 * paper surface. Every offered reply is an actual first-person dialogue
 * choice; server responses replace the NPC's line after validation.
 */
public final class NpcDialogueScreen extends Screen {
    private static final ResourceLocation WOOD =
            new ResourceLocation("minecraft", "textures/block/dark_oak_planks.png");
    private static final ResourceLocation BOOK =
            new ResourceLocation("minecraft", "textures/gui/book.png");
    private static final int INK = 0xFF42301C;
    private static final int MUTED = 0xFF785A3C;

    private final DialogueView view;
    private String pendingReply = "";

    public NpcDialogueScreen(DialogueView view) {
        super(Component.literal("Conversation"));
        this.view = view;
    }

    public static void open(DialogueView view) {
        Minecraft.getInstance().setScreen(new NpcDialogueScreen(view));
    }

    @Override
    protected void init() {
        super.init();
        int w = panelWidth();
        int x = (width - w) / 2;
        int y = panelTop();
        List<DialogueView.Option> options = view.options();
        int buttonWidth = (w - 29) / 2;
        int step = 20;
        for (int i = 0; i < options.size(); i++) {
            DialogueView.Option choice = options.get(i);
            int col = i % 2;
            int row = i / 2;
            addRenderableWidget(Button.builder(Component.literal(choice.label()), b -> {
                if ("leave".equals(choice.id())) {
                    onClose();
                    CyberNpcNetwork.CHANNEL.sendToServer(
                        new DialogueChoicePacket(view.entityId(), "leave"));
                } else {
                    pendingReply = choice.label();
                    CyberNpcNetwork.CHANNEL.sendToServer(
                        new DialogueChoicePacket(view.entityId(), choice.id()));
                }
            }).pos(x + 9 + col * (buttonWidth + 9), y + 119 + row * step)
              .size(buttonWidth, 18).build());
        }
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.03F));
    }

    private int panelWidth() {
        return Math.max(220, Math.min(width - 12, 568));
    }

    private int panelHeight() {
        return Math.min(height - 8, 188);
    }

    private int panelTop() {
        return height - panelHeight() - 4;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int w = panelWidth();
        int h = panelHeight();
        int x = (width - w) / 2;
        int y = panelTop();

        // Wood frame texture is tiled at actual vanilla 16x16 pixel scale.
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xEF1F130D);
        for (int px = x; px < x + w; px += 16) {
            for (int py = y; py < y + h; py += 16) {
                int tw = Math.min(16, x + w - px);
                int th = Math.min(16, y + h - py);
                g.blit(WOOD, px, py, 0, 0, tw, th, 16, 16);
            }
        }
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xB023170F);
        g.fill(x + 4, y + 4, x + w - 4, y + 6, 0xFF977044);

        int portraitY = y + 29;
        int npcX = x + 10;
        int playerX = x + w - 72;

        portraitFrame(g, npcX, portraitY);
        portraitFrame(g, playerX, portraitY);
        drawPortraits(g, npcX, playerX, portraitY, mouseX, mouseY);

        String name = font.plainSubstrByWidth(view.name(), 85);
        String playerName = Minecraft.getInstance().player == null
                ? "You" : font.plainSubstrByWidth(
                    Minecraft.getInstance().player.getGameProfile().getName(), 82);
        g.drawCenteredString(font, name, npcX + 29, y + 14, 0xFFFFDFA7);
        g.drawCenteredString(font, playerName, playerX + 29, y + 14, 0xFFFFDFA7);

        // Dialogue between the actual speaker portraits.
        int textX = npcX + 69;
        int textW = Math.max(64, playerX - textX - 7);
        int textY = y + 28;
        int textBottom = y + 105;
        paperPanel(g, textX, textY, textW, textBottom - textY);

        String yourLine = !pendingReply.isEmpty()
                ? pendingReply : view.playerLine();
        int cursor = textY + 5;
        if (!yourLine.isEmpty()) {
            g.drawString(font, "You:", textX + 6, cursor, MUTED, false);
            cursor += 11;
            List<FormattedCharSequence> lines = font.split(
                Component.literal(yourLine), Math.max(50, textW - 12));
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.drawString(font, lines.get(i), textX + 6, cursor, INK, false);
                cursor += 10;
            }
            cursor += 2;
        }

        g.drawString(font, font.plainSubstrByWidth(view.name() + ":", textW - 12),
                textX + 6, cursor, MUTED, false);
        cursor += 11;
        List<FormattedCharSequence> lines = font.split(
                Component.literal(pendingReply.isEmpty()
                        ? view.speech() : "..."),
                Math.max(50, textW - 12));
        for (FormattedCharSequence line : lines) {
            if (cursor + 9 > textBottom - 2) break;
            g.drawString(font, line, textX + 6, cursor, INK, false);
            cursor += 10;
        }

        g.drawCenteredString(font, "Choose your reply", x + w / 2,
                y + 108, 0xFFFFD79A);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private static void portraitFrame(GuiGraphics g, int x, int y) {
        g.fill(x - 2, y - 2, x + 60, y + 79, 0xFF21150E);
        g.fill(x, y, x + 58, y + 77, 0xFF735135);
        g.fill(x + 2, y + 2, x + 56, y + 74, 0xFF2F291F);
        g.fill(x + 1, y + 74, x + 57, y + 77, 0xFFC39A5A);
    }

    private void drawPortraits(GuiGraphics g, int npcX, int playerX,
                               int top, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity npc = mc.level.getEntity(view.entityId());
        LocalPlayer player = mc.player;

        // Minecraft's own GUI entity renderer draws both real faces, armour
        // and skins. Portrait clipping keeps the panel tidy on GUI scales.
        if (npc instanceof LivingEntity living) {
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, npcX + 3, top + 3, npcX + 55, top + 73,
                    29, 0.0F, mouseX, mouseY, living);
            } catch (RuntimeException ignored) {
                g.drawCenteredString(font, "NPC", npcX + 29, top + 36, 0xFFDDBB8E);
            }
        }
        if (player != null) {
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, playerX + 3, top + 3, playerX + 55, top + 73,
                    29, 0.0F, mouseX, mouseY, player);
            } catch (RuntimeException ignored) {
                g.drawCenteredString(font, "You", playerX + 29, top + 36, 0xFFDDBB8E);
            }
        }
    }

    private static void paperPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF3F2918);
        for (int xx = x; xx < x + w; xx += 16) {
            for (int yy = y; yy < y + h; yy += 16) {
                g.blit(BOOK, xx, yy, Math.min(16, x + w - xx),
                    Math.min(16, y + h - yy), 57.0F, 66.0F,
                    Math.min(16, x + w - xx), Math.min(16, y + h - yy),
                    256, 256);
            }
        }
        g.fill(x, y, x + w, y + 1, 0xFFC8A575);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF8D693F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
