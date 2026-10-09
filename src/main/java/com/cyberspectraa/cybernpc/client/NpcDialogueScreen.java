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
 * Compact, bottom-anchored RPG dialogue bar with detached reply buttons.
 * The world stays visible while speaking, and only the current speaker
 * has a portrait. All dialogue options and purchases remain server-owned.
 */
public final class NpcDialogueScreen extends Screen {
    private static final ResourceLocation WOOD =
            new ResourceLocation("minecraft", "textures/block/dark_oak_planks.png");
    private static final ResourceLocation BOOK =
            new ResourceLocation("minecraft", "textures/gui/book.png");
    private static final int INK = 0xFF42301C;
    private static final int MUTED = 0xFF785A3C;
    private static final long ENTRY_MS = 280L;
    private static final long SWITCH_MS = 180L;
    private static final long LETTER_MS = 52L;

    private final DialogueView view;
    private String pendingReply = "";
    // Server replies often arrive in the very same tick as the player's choice.
    // Defer the visual switch, not the authoritative server action.
    private DialogueView queuedNpcReply;
    private long screenStarted = System.currentTimeMillis();
    private long speakerChanged = screenStarted;
    private boolean waitingForServer;
    private int messageLength;
    private final java.util.List<Button> replyButtons = new java.util.ArrayList<>();

    public NpcDialogueScreen(DialogueView view) {
        super(Component.literal("Conversation"));
        this.view = view;
    }

    public static void open(DialogueView view) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof NpcDialogueScreen current
                && current.waitingForServer
                && current.view.entityId() == view.entityId()) {
            current.queuedNpcReply = view;
            return;
        }
        minecraft.setScreen(new NpcDialogueScreen(view));
    }

    @Override
    protected void init() {
        super.init();
        replyButtons.clear();
        screenStarted = System.currentTimeMillis();
        speakerChanged = screenStarted;
        int w = panelWidth();
        int x = (width - w) / 2;
        List<DialogueView.Option> options = view.options();
        int cols = replyColumns();
        int rows = (options.size() + cols - 1) / cols;
        int spacing = 20;
        int buttonGap = 6;
        int buttonWidth = (w - 16 - (cols - 1) * buttonGap) / cols;
        // Reply buttons float above the bottom dialogue bar. They do not
        // enlarge the bar or cause an opaque panel to cover the gameplay.
        int replyTop = panelTop() - 12 - rows * spacing;
        for (int i = 0; i < options.size(); i++) {
            DialogueView.Option choice = options.get(i);
            int col = i % cols;
            int row = i / cols;
            Button button = Button.builder(Component.literal(choice.label()), b -> {
                if (waitingForServer) return;
                if ("leave".equals(choice.id())) {
                    CyberNpcNetwork.CHANNEL.sendToServer(
                            new DialogueChoicePacket(view.entityId(), "leave"));
                    onClose();
                } else {
                    pendingReply = choice.label();
                    waitingForServer = true;
                    messageLength = 0;
                    speakerChanged = System.currentTimeMillis();
                    CyberNpcNetwork.CHANNEL.sendToServer(
                            new DialogueChoicePacket(view.entityId(), choice.id()));
                    for (Button other : replyButtons) other.active = false;
                }
            }).pos(x + 8 + col * (buttonWidth + buttonGap),
                    replyTop + row * spacing).size(buttonWidth, 18).build();
            replyButtons.add(addRenderableWidget(button));
        }
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.03F));
    }

    private int panelWidth() {
        return Math.max(100, Math.min(width - 14, 520));
    }

    private int panelHeight() {
        return Math.min(height - 12, 96);
    }

    private int panelTop() {
        return height - panelHeight() - 5;
    }

    private int replyColumns() {
        // Small GUI scales can use a single column rather than placing
        // two unreadably narrow buttons beside each other.
        return panelWidth() < 330 ? 1 : 2;
    }

    private int replyTop() {
        int cols = replyColumns();
        int rows = (view.options().size() + cols - 1) / cols;
        return panelTop() - 12 - rows * 20;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Do not dim the whole world; the compact HUD occupies the bottom only.
        int w = panelWidth();
        int h = panelHeight();
        int x = (width - w) / 2;
        int y = panelTop();
        long now = System.currentTimeMillis();
        float entry = ease(Math.min(1.0F, (now - screenStarted) / (float) ENTRY_MS));
        int rise = Math.round((1.0F - entry) * 18.0F);
        y += rise;

        // Narrow wooden dialogue bar anchored to the bottom of the screen.
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xEF1F130D);
        for (int px = x; px < x + w; px += 16) {
            for (int py = y; py < y + h; py += 16) {
                g.blit(WOOD, px, py, 0, 0, Math.min(16, x + w - px),
                        Math.min(16, y + h - py), 16, 16);
            }
        }
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xB023170F);
        g.fill(x + 4, y + 4, x + w - 4, y + 6, 0xFF977044);

        boolean playerSpeaking = waitingForServer;
        String speaker = playerSpeaking ? playerName() : view.name();
        String speech = playerSpeaking ? pendingReply : view.speech();
        // NPC faces remain on the left; the player's portrait and dialogue
        // switch to opposite sides for a readable back-and-forth conversation.
        int portraitX = playerSpeaking ? x + w - 69 : x + 11;
        int portraitY = y + 10;
        int textX = playerSpeaking ? x + 10 : x + 81;
        int textWidth = Math.max(65, w - 91);
        int textY = y + 25;

        // Only the active speaker receives a portrait or a nameplate.
        float transition = ease(Math.min(1.0F, (now - speakerChanged) / (float) SWITCH_MS));
        int slide = Math.round((1.0F - transition) * 9.0F);
        portraitFrame(g, portraitX, portraitY);
        drawSpeakerPortrait(g, playerSpeaking,
                portraitX + (playerSpeaking ? slide : -slide), portraitY,
                mouseX, mouseY);
        g.drawString(font, font.plainSubstrByWidth(speaker, Math.max(20, textWidth - 6)),
                textX + 6, y + 11, 0xFFFFDFA7, false);

        int paperHeight = 62;
        paperPanel(g, textX, textY, textWidth, paperHeight);
        g.enableScissor(textX + 5, textY + 5, textX + textWidth - 5,
                textY + paperHeight - 5);
        try {
            // Typewriter effect, restarted whenever the speaking character changes.
            int count = (int) Math.min(speech.length(),
                    Math.max(0L, (now - speakerChanged) / LETTER_MS));
            // Short, quiet text blips, never one loud effect per letter or
            // a burst of sounds when the player clicks to skip the animation.
            if (count > messageLength && count < speech.length()
                    && count % 3 == 0
                    && !Character.isWhitespace(speech.charAt(count - 1))) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.65F, 0.14F));
            }
            messageLength = count;
            String visible = speech.substring(0, count);
            List<FormattedCharSequence> lines = font.split(Component.literal(visible),
                    Math.max(60, textWidth - 14));
            int lineY = textY + 7;
            int maxVisibleLines = Math.max(1, (paperHeight - 14) / 11);
            // Reveal the newest text as it types even if a long speech wraps
            // beyond the available height of the compact dialogue bar.
            int firstLine = Math.max(0, lines.size() - maxVisibleLines);
            for (int i = firstLine; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), textX + 7, lineY, INK, false);
                lineY += 11;
            }
        } finally {
            g.disableScissor();
        }

        boolean showReplies = !waitingForServer
                && messageLength >= view.speech().length() && entry >= 1.0F;
        // The reply header is separated from the main dialogue bar. While
        // the player is talking, this area becomes a click-to-continue hint.
        if (playerSpeaking) {
            String hint = countDone(speech)
                    ? (queuedNpcReply != null ? "Click to continue"
                        : "Waiting for a response...")
                    : "Your reply";
            g.drawCenteredString(font, hint, x + w / 2, panelTop() - 13, 0xFFFFD79A);
        } else if (showReplies && !replyButtons.isEmpty()) {
            g.drawCenteredString(font, "Choose your reply",
                    x + w / 2, replyTop() - 13, 0xFFFFD79A);
        }
        for (Button button : replyButtons) {
            button.visible = showReplies;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private boolean countDone(String speech) {
        return messageLength >= speech.length();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            String currentLine = waitingForServer ? pendingReply : view.speech();
            if (messageLength < currentLine.length()) {
                // First click completes the typewriter line; it never skips
                // over the player's actual reply.
                speakerChanged = System.currentTimeMillis()
                        - currentLine.length() * LETTER_MS;
                messageLength = currentLine.length();
                return true;
            }
            if (waitingForServer) {
                // Only a second click after the full player line is visible
                // reveals the already validated, server-provided NPC answer.
                if (queuedNpcReply != null) {
                    DialogueView next = queuedNpcReply;
                    queuedNpcReply = null;
                    Minecraft.getInstance().setScreen(new NpcDialogueScreen(next));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String playerName() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? "You" : player.getGameProfile().getName();
    }

    private static float ease(float amount) {
        return 1.0F - (1.0F - amount) * (1.0F - amount);
    }

    private static void portraitFrame(GuiGraphics g, int x, int y) {
        g.fill(x - 2, y - 2, x + 60, y + 79, 0xFF21150E);
        g.fill(x, y, x + 58, y + 77, 0xFF735135);
        g.fill(x + 2, y + 2, x + 56, y + 74, 0xFF2F291F);
        g.fill(x + 1, y + 74, x + 57, y + 77, 0xFFC39A5A);
    }

    private void drawSpeakerPortrait(GuiGraphics g, boolean playerSpeaking,
                                     int x, int top, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity speaker = playerSpeaking ? mc.player : mc.level.getEntity(view.entityId());
        if (!(speaker instanceof LivingEntity living)) {
            g.drawCenteredString(font, playerSpeaking ? "You" : "NPC",
                    x + 29, top + 36, 0xFFDDBB8E);
            return;
        }
        try {
            g.enableScissor(x + 3, top + 3, x + 55, top + 73);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        g, x + 29, top + 101, 43,
                        (float) (x + 29 - mouseX),
                        (float) (top + 45 - mouseY), living);
            } finally {
                g.disableScissor();
            }
        } catch (RuntimeException ignored) {
            g.drawCenteredString(font, playerSpeaking ? "You" : "NPC",
                    x + 29, top + 36, 0xFFDDBB8E);
        }
    }

    private static void paperPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF3F2918);
        for (int xx = x; xx < x + w; xx += 16) {
            for (int yy = y; yy < y + h; yy += 16) {
                int bw = Math.min(16, x + w - xx);
                int bh = Math.min(16, y + h - yy);
                g.blit(BOOK, xx, yy, bw, bh, 57.0F, 66.0F,
                        bw, bh, 256, 256);
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
