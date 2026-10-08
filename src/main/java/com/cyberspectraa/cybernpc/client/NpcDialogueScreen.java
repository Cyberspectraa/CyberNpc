package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.dialogue.DialogueView;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.DialogueChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.PacketDistributor;
import java.util.List;

/** Vanilla-book styled dialogue, with choices sent to the server. */
public final class NpcDialogueScreen extends Screen {
    private static final ResourceLocation BOOK =
            new ResourceLocation("minecraft", "textures/gui/book.png");
    private final DialogueView view;

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
        int x = (width - 192) / 2;
        int top = (height - 192) / 2;
        List<DialogueView.Option> options = view.options();
        int optionY = top + 94;
        int step = options.size() > 4 ? 16 : 19;
        int buttonHeight = options.size() > 4 ? 16 : 18;
        for (int i = 0; i < options.size(); i++) {
            DialogueView.Option choice = options.get(i);
            addRenderableWidget(Button.builder(Component.literal(choice.label()), b -> {
                CyberNpcNetwork.CHANNEL.sendToServer(
                        new DialogueChoicePacket(view.entityId(), choice.id()));
                if ("leave".equals(choice.id())) onClose();
            }).pos(x + 22, optionY + i * step).size(148, buttonHeight).build());
        }
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.94F));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = (width - 192) / 2;
        int y = (height - 192) / 2;
        graphics.blit(BOOK, x, y, 0, 0, 192, 192, 256, 256);
        int ink = 0x44301F;
        graphics.drawCenteredString(font,
                Component.literal(font.plainSubstrByWidth(view.name(), 140)),
                x + 97, y + 19, ink);
        graphics.drawCenteredString(font,
                Component.literal(font.plainSubstrByWidth(view.role(), 145)),
                x + 97, y + 32, 0x86603D);
        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(Component.literal(view.speech()), 144);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            graphics.drawString(font, lines.get(i),
                    x + 24, y + 47 + i * 10, ink, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
