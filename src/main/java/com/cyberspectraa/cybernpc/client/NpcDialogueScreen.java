package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.dialogue.DialogueView;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.DialogueChoicePacket;
import com.cyberspectraa.cybernpc.network.DialogueClosePacket;
import com.cyberspectraa.cybernpc.network.IntroControlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Split-corner conversation UI:
 * - bottom-left: the current speaker's portrait and readable text
 * - bottom-right: four two-line response slots with pagination
 * - Main Seven only: the viewer's real saved Trust and Affection
 *
 * Gameplay stays visible in the middle. The server still owns the dialogue
 * session, options, quest decisions and relationship changes.
 */
public final class NpcDialogueScreen extends Screen {
    private static final ResourceLocation WOOD =
            new ResourceLocation("minecraft", "textures/block/dark_oak_planks.png");
    private static final ResourceLocation BOOK =
            new ResourceLocation("minecraft", "textures/gui/book.png");
    private static final int INK = 0xFF42301C;
    private static final int TEXT = 0xFFF5DFB5;
    private static final int MUTED = 0xFFC6A67E;
    private static final long ENTRY_MS = 280L;
    private static final long SWITCH_MS = 180L;
    private static final long LETTER_MS = 52L;
    private static final int ROW_HEIGHT = 30;
    private static final long RESPONSE_TIMEOUT_MS = 8000L;

    private final DialogueView view;
    private String pendingReply = "";
    // If the server responds in the same tick, keep the player's reply
    // readable until a second click; never skip the player voice/portrait.
    private DialogueView queuedNpcReply;
    private long screenStarted = System.currentTimeMillis();
    private long speakerChanged = screenStarted;
    private boolean waitingForServer;
    private int messageLength;
    // The original split design uses this same speaker/typewriter state
    // for both the NPC and the player's reply.
    private Button continuationButton;
    private long lastVoiceAtMillis;
    private int lastVoiceIndex;
    private int replyPage;
    private int textScroll;
    private int scrollLimit;
    private final List<Button> replyButtons = new ArrayList<>();
    private Button previousPageButton;
    private Button nextPageButton;

    public NpcDialogueScreen(DialogueView view) {
        super(Component.literal("Conversation"));
        this.view = view;
    }

    public static void open(DialogueView view) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SummoningCinematicScreen) {
            SummoningCinematicScreen.closeFromServer();
        }
        if (minecraft.screen instanceof NpcDialogueScreen current
                && current.waitingForServer
                && current.view.entityId() == view.entityId()) {
            current.queuedNpcReply = view;
            return;
        }
        minecraft.setScreen(new NpcDialogueScreen(view));
    }

    private int bottomGap() {
        return Math.max(8, Math.min(28, height / 16));
    }

    private int leftWidth() {
        return Math.min(420, Math.max(140, Math.round((width - 24) * 0.52F)));
    }

    private int rightWidth() {
        return Math.min(340, Math.max(104,
                Math.min(Math.round((width - 24) * 0.40F),
                        width - 36 - leftWidth())));
    }

    private int leftX() { return 10; }
    private int rightX() { return width - rightWidth() - 10; }
    private int speechHeight() { return Math.min(135, Math.max(99, height / 4)); }
    private int speechY() { return height - bottomGap() - speechHeight(); }
    private int rowsPerPage() {
        // Preserve enough vertical room for world visibility on small windows.
        return Math.min(4, Math.max(2, (height - 125) / ROW_HEIGHT));
    }
    private int replyHeight() {
        return 30 + Math.min(rowsPerPage(), Math.max(1, view.options().size()))
                * ROW_HEIGHT + 26;
    }
    private int compactReplyHeight() { return 67; }
    private int replyY() { return height - bottomGap() - replyHeight(); }
    private int pageCount() {
        return Math.max(1, (view.options().size() + rowsPerPage() - 1) / rowsPerPage());
    }

    @Override
    protected void init() {
        super.init();
        replyButtons.clear();
        screenStarted = System.currentTimeMillis();
        speakerChanged = screenStarted;
        replyPage = 0;
        textScroll = 0;
        scrollLimit = 0;

        int rows = rowsPerPage();
        for (int slot = 0; slot < rows; slot++) {
            final int slotIndex = slot;
            // Button owns hit testing/focus/navigation; text is drawn on top
            // in two wrapped lines. Tooltips retain the complete 80-char
            // reply on narrow GUI scales.
            Button button = Button.builder(Component.empty(),
                    clicked -> chooseSlot(slotIndex))
                    .pos(rightX() + 8, replyY() + 27 + slot * ROW_HEIGHT)
                    .size(rightWidth() - 16, ROW_HEIGHT - 3)
                    .build();
            replyButtons.add(addRenderableWidget(button));
        }
        previousPageButton = addRenderableWidget(Button.builder(
                Component.literal("<"), clicked -> selectPage(replyPage - 1))
                .pos(rightX() + 8, replyY() + replyHeight() - 24)
                .size(24, 18).build());
        nextPageButton = addRenderableWidget(Button.builder(
                Component.literal(">"), clicked -> selectPage(replyPage + 1))
                .pos(rightX() + rightWidth() - 32, replyY() + replyHeight() - 24)
                .size(24, 18).build());
        continuationButton = addRenderableWidget(Button.builder(
                Component.literal("Close"), b -> {
                    if (waitingForServer
                            && queuedNpcReply != null
                            && messageLength >= pendingReply.length()) {
                        showQueuedNpcReply();
                    } else {
                        onClose();
                    }
                })
                .pos(rightX() + rightWidth() - 87,
                        height - bottomGap() - 28)
                .size(78, 20).build());
        continuationButton.visible = false;
        refreshSlots();
    }

    private void showQueuedNpcReply() {
        if (queuedNpcReply == null) return;
        DialogueView next = queuedNpcReply;
        queuedNpcReply = null;
        Minecraft.getInstance().setScreen(new NpcDialogueScreen(next));
    }

    private void refreshSlots() {
        int start = replyPage * rowsPerPage();
        for (int slot = 0; slot < replyButtons.size(); slot++) {
            int index = start + slot;
            Button button = replyButtons.get(slot);
            if (index < view.options().size()) {
                DialogueView.Option option = view.options().get(index);
                button.setTooltip(Tooltip.create(Component.literal(option.label())));
                button.active = !waitingForServer;
            } else {
                button.active = false;
            }
        }
        previousPageButton.active = replyPage > 0;
        nextPageButton.active = replyPage + 1 < pageCount();
    }

    private void selectPage(int page) {
        if (waitingForServer || page < 0 || page >= pageCount()) return;
        replyPage = page;
        refreshSlots();
    }

    private void chooseSlot(int slot) {
        int index = replyPage * rowsPerPage() + slot;
        if (waitingForServer || index < 0 || index >= view.options().size()) return;
        DialogueView.Option choice = view.options().get(index);
        if ("leave".equals(choice.id())
                || "intro_finish".equals(choice.id())
                || "intro_skip".equals(choice.id())) {
            CyberNpcNetwork.CHANNEL.sendToServer(
                    new DialogueChoicePacket(view.entityId(), choice.id()));
            // Farewell/Finish is already a server-authoritative action.
            // Closing locally without a second Close packet avoids a race
            // that could regenerate the conversation after Farewell.
            Minecraft.getInstance().setScreen(null);
            return;
        }

        DialogueVoiceBlips.stopActive();
        pendingReply = choice.label();
        waitingForServer = true;
        // Return to the original split layout: the speaking character always
        // has the left portrait and parchment, including the player.
        messageLength = 0;
        lastVoiceIndex = 0;
        lastVoiceAtMillis = 0L;
        speakerChanged = System.currentTimeMillis();
        textScroll = 0;
        CyberNpcNetwork.CHANNEL.sendToServer(
                new DialogueChoicePacket(view.entityId(), choice.id()));
        refreshSlots();
    }

    private long letterDelay() {
        return "Citizen".equalsIgnoreCase(view.role()) ? 27L : LETTER_MS;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float entry = ease(Math.min(1.0F, (now - screenStarted) / (float) ENTRY_MS));
        boolean playerSpeaking = waitingForServer;
        String speaker = playerSpeaking ? playerName() : view.name();
        String speech = playerSpeaking ? pendingReply : view.speech();

        // Both boxes rise from the bottom independently, leaving the centre
        // and most of the top of the world untouched (no fullscreen dim).
        int rise = Math.round((1.0F - entry) * 12.0F);
        int lx = leftX(), ly = speechY() + rise;
        int lw = leftWidth(), lh = speechHeight();
        drawWoodPanel(g, lx, ly, lw, lh);

        boolean portrait = lw >= 205;
        int portraitX = lx + 7, portraitY = ly + 27;
        int textX = lx + (portrait ? 72 : 9);
        int textWidth = Math.max(56, lw - (portrait ? 80 : 18));
        int nameY = ly + 10;
        String shownName = font.plainSubstrByWidth(speaker, textWidth - 4);
        g.drawString(font, shownName, textX, nameY, TEXT, false);

        if (portrait) {
            portraitFrame(g, portraitX, portraitY, 56, Math.min(75, lh - 33));
            float switchIn = ease(Math.min(1.0F,
                    (now - speakerChanged) / (float) SWITCH_MS));
            int slide = Math.round((1.0F - switchIn) * 7.0F);
            drawSpeakerPortrait(g, playerSpeaking,
                    portraitX + (playerSpeaking ? slide : -slide),
                    portraitY, mouseX, mouseY);
        }

        DialogueView.Relationship relationship = view.relationship();
        int textY = ly + 29;
        // Bond values describe the NPC, not the player's own character.
        if (!playerSpeaking && relationship != null) {
            String status = bondLabel(relationship.trust(), relationship.affection());
            String bond = "Bond: " + status;
            String stats = "Trust " + signed(relationship.trust())
                    + "  |  Affection " + signed(relationship.affection());
            boolean oneLine = font.width(bond) + font.width(stats) + 13 <= textWidth;
            g.drawString(font, font.plainSubstrByWidth(bond, textWidth - 4),
                    textX, ly + 23, 0xFFE3C18A, false);
            if (oneLine) {
                g.drawString(font, stats,
                        textX + textWidth - font.width(stats) - 3, ly + 23,
                        MUTED, false);
            } else {
                g.drawString(font, font.plainSubstrByWidth(stats, textWidth - 4),
                        textX, ly + 35, MUTED, false);
            }
            textY = ly + (oneLine ? 37 : 48);
        }

        int paperHeight = Math.max(27, lh - (textY - ly) - 8);
        paperPanel(g, textX, textY, textWidth, paperHeight);
        g.enableScissor(textX + 5, textY + 4,
                textX + textWidth - 5, textY + paperHeight - 4);
        try {
            int count = (int) Math.min(speech.length(),
                    Math.max(0L, (now - speakerChanged) / letterDelay()));
            boolean played = playerSpeaking
                    ? DialogueVoiceBlips.playPlayer(
                            speech, messageLength, count,
                            lastVoiceIndex, now, lastVoiceAtMillis)
                    : DialogueVoiceBlips.play(
                            view, speech, messageLength, count,
                            lastVoiceIndex, now, lastVoiceAtMillis);
            if (played) {
                lastVoiceAtMillis = now;
                lastVoiceIndex = count;
            }
            messageLength = count;
            List<FormattedCharSequence> lines = font.split(
                    Component.literal(speech.substring(0, count)),
                    Math.max(36, textWidth - 14));
            int visibleLines = Math.max(1, (paperHeight - 10) / 11);
            scrollLimit = Math.max(0, lines.size() - visibleLines);
            textScroll = Math.max(0, Math.min(textScroll, scrollLimit));
            int firstLine = Math.max(0, scrollLimit - textScroll);
            for (int i = firstLine; i < Math.min(lines.size(), firstLine + visibleLines); i++) {
                g.drawString(font, lines.get(i), textX + 7,
                        textY + 6 + (i - firstLine) * 11, INK, false);
            }
        } finally {
            g.disableScissor();
        }

        int rx = rightX(), rw = rightWidth();
        boolean showReplies = !waitingForServer
                && messageLength >= view.speech().length()
                && entry >= 1.0F;

        if (waitingForServer || view.options().isEmpty()) {
            // Preserve the original left-side player portrait and reply.
            // The right side is a small state/control panel, never a second
            // copy of the chosen text or a giant empty wood background.
            int rh = compactReplyHeight();
            int ry = height - bottomGap() - rh + rise;
            drawWoodPanel(g, rx, ry, rw, rh);

            continuationButton.visible = true;
            if (waitingForServer) {
                boolean finished = messageLength >= pendingReply.length();
                g.drawString(font, "YOUR REPLY", rx + 10, ry + 10,
                        0xFFEFCB87, false);
                if (!finished) {
                    g.drawString(font, "Speaking...", rx + 10, ry + 29,
                            MUTED, false);
                } else if (queuedNpcReply != null) {
                    g.drawString(font, "Continue the conversation", rx + 10,
                            ry + 29, TEXT, false);
                } else if (now - speakerChanged >= RESPONSE_TIMEOUT_MS) {
                    g.drawString(font, "No reply received", rx + 10,
                            ry + 29, MUTED, false);
                } else {
                    g.drawString(font, "Waiting for a response...", rx + 10,
                            ry + 29, MUTED, false);
                }
                continuationButton.setMessage(Component.literal(
                        finished && queuedNpcReply != null
                                ? "Continue" : "Close"));
            } else {
                g.drawString(font, "CONVERSATION", rx + 10, ry + 10,
                        0xFFEFCB87, false);
                g.drawString(font, "No further replies available.", rx + 10,
                        ry + 29, TEXT, false);
                continuationButton.setMessage(Component.literal("Close"));
            }
            for (Button button : replyButtons) button.visible = false;
            previousPageButton.visible = false;
            nextPageButton.visible = false;
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        continuationButton.visible = false;
        int ry = replyY() + rise;
        int rh = replyHeight();
        drawWoodPanel(g, rx, ry, rw, rh);
        g.drawString(font, showReplies ? "YOUR REPLY" : "LISTENING...",
                rx + 10, ry + 10, 0xFFEFCB87, false);

        for (int slot = 0; slot < replyButtons.size(); slot++) {
            int index = replyPage * rowsPerPage() + slot;
            Button button = replyButtons.get(slot);
            button.visible = showReplies && index < view.options().size();
            button.active = showReplies && index < view.options().size();
        }
        boolean paging = showReplies && pageCount() > 1;
        previousPageButton.visible = paging;
        previousPageButton.active = paging && replyPage > 0;
        nextPageButton.visible = paging;
        nextPageButton.active = paging && replyPage + 1 < pageCount();

        super.render(g, mouseX, mouseY, partialTick);
        if (showReplies) {
            for (int slot = 0; slot < replyButtons.size(); slot++) {
                int index = replyPage * rowsPerPage() + slot;
                if (index >= view.options().size()) continue;
                Button b = replyButtons.get(slot);
                int labelColor = b.isHoveredOrFocused() ? 0xFFFFECBA : TEXT;
                List<FormattedCharSequence> parts = font.split(
                        Component.literal(view.options().get(index).label()),
                        Math.max(35, b.getWidth() - 15));
                int count = Math.min(2, parts.size());
                int firstY = b.getY() + (b.getHeight() - count * 10) / 2 + 1;
                for (int i = 0; i < count; i++) {
                    g.drawString(font, parts.get(i),
                            b.getX() + 7, firstY + 10 * i, labelColor, false);
                }
            }
            if (paging) {
                String page = (replyPage + 1) + " / " + pageCount();
                g.drawCenteredString(font, page, rx + rw / 2,
                        ry + rh - 19, MUTED);
            }
        }
        if (scrollLimit > 0 && messageLength >= speech.length()) {
            g.drawString(font, "Scroll to read", textX + 3,
                    textY + paperHeight + 1, MUTED, false);
        }
    }

    private static String signed(int n) { return n > 0 ? "+" + n : Integer.toString(n); }

    /** Descriptive only. The actual progression is Trust / Affection. */
    private static String bondLabel(int trust, int affection) {
        if (trust < 0 || affection < 0) return "Strained";
        if (trust >= 7 && affection >= 6) return "Trusted";
        if (trust >= 4 && affection >= 3) return "Close";
        if (trust >= 2 && affection >= 1) return "Friendly";
        if (trust >= 1 || affection >= 1) return "Familiar";
        return "New acquaintance";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Long dialogue remains readable instead of permanently cutting off
        // its first lines. Never scroll choices by changing server action IDs.
        if (mouseX >= leftX() && mouseX < leftX() + leftWidth()
                && mouseY >= speechY() && mouseY < speechY() + speechHeight()
                && scrollLimit > 0) {
            textScroll = Math.max(0, Math.min(scrollLimit,
                    textScroll + (delta > 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // A visible Close/Continue control must work on the FIRST click;
        // the skip-typewriter shortcut must not intercept its hit box.
        if (button == 0 && continuationButton != null
                && continuationButton.visible
                && mouseX >= continuationButton.getX()
                && mouseX < continuationButton.getX() + continuationButton.getWidth()
                && mouseY >= continuationButton.getY()
                && mouseY < continuationButton.getY() + continuationButton.getHeight()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0) {
            if (waitingForServer) {
                if (messageLength < pendingReply.length()) {
                    speakerChanged = System.currentTimeMillis()
                            - pendingReply.length() * letterDelay();
                    messageLength = pendingReply.length();
                    return true;
                }
                // Only a click on the continue button advances after speech;
                // a click anywhere else still works as before for convenience.
                if (queuedNpcReply != null) {
                    showQueuedNpcReply();
                    return true;
                }
            } else if (messageLength < view.speech().length()) {
                speakerChanged = System.currentTimeMillis()
                        - view.speech().length() * letterDelay();
                messageLength = view.speech().length();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        if (!waitingForServer && view.options().size() == 1
                && "Leave them alone.".equals(view.options().get(0).label())
                && System.currentTimeMillis() - screenStarted > 1600L) {
            CyberNpcNetwork.CHANNEL.sendToServer(
                    new DialogueChoicePacket(view.entityId(), "leave"));
            Minecraft.getInstance().setScreen(null);
        }
    }

    private String playerName() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? "You" : player.getGameProfile().getName();
    }

    private static float ease(float amount) {
        return 1.0F - (1.0F - amount) * (1.0F - amount);
    }

    private static void drawWoodPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xF41E160F);
        for (int px = x; px < x + w; px += 16) {
            for (int py = y; py < y + h; py += 16) {
                g.blit(WOOD, px, py, 0, 0,
                        Math.min(16, x + w - px), Math.min(16, y + h - py), 16, 16);
            }
        }
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xD3271E15);
        g.fill(x + 5, y + 5, x + w - 5, y + 7, 0xFF9F7F4F);
    }

    private static void portraitFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF21150E);
        g.fill(x, y, x + w, y + h, 0xFF735135);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF2F291F);
        g.fill(x + 1, y + h - 3, x + w - 1, y + h, 0xFFC39A5A);
    }

    private void drawSpeakerPortrait(GuiGraphics g, boolean playerSpeaking,
                                     int x, int top, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity speaker = playerSpeaking ? mc.player : mc.level.getEntity(view.entityId());
        if (!(speaker instanceof LivingEntity living)) {
            g.drawCenteredString(font, playerSpeaking ? "You" : "NPC",
                    x + 28, top + 34, 0xFFDDBB8E);
            return;
        }
        boolean showName = living.isCustomNameVisible();
        try {
            // Inventory portrait should not duplicate the NPC's floating
            // world nameplate inside the small frame.
            if (showName) living.setCustomNameVisible(false);
            g.enableScissor(x + 3, top + 3, x + 53, top + 69);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        g, x + 27, top + 92, 39,
                        (float) (x + 27 - mouseX),
                        (float) (top + 42 - mouseY), living);
            } finally {
                g.disableScissor();
            }
        } catch (RuntimeException ignored) {
        } finally {
            if (showName) living.setCustomNameVisible(true);
        }
        // Rendering failures leave the portrait's framed background intact.
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

    private boolean isIntro() {
        return view.options().stream().anyMatch(o -> o.id().equals("intro_skip"));
    }

    @Override
    public void removed() {
        DialogueVoiceBlips.stopActive();
        super.removed();
    }

    @Override
    public void onClose() {
        // End the server's look-at lock even when Escape is used rather
        // than a dedicated Farewell choice.
        CyberNpcNetwork.CHANNEL.sendToServer(new DialogueClosePacket(view.entityId()));
        if (isIntro()) CyberNpcNetwork.CHANNEL.sendToServer(new IntroControlPacket(true));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
