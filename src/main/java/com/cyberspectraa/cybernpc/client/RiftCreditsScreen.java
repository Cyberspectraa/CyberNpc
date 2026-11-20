package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.RiftCreditsPacket;
import com.cyberspectraa.cybernpc.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

/**
 * Private, non-skippable Rift ending with the owner's provided AtlasAudio track.
 * The server owns player invisibility, locked movement and final disconnect.
 * This GUI stops its streaming music if it is removed, even by server reset.
 */
public final class RiftCreditsScreen extends Screen {
    private final RiftCreditsPacket summary;
    private SimpleSoundInstance backgroundMusic;
    private int elapsed;

    private RiftCreditsScreen(RiftCreditsPacket summary) {
        super(Component.literal("Beyond the Rift"));
        this.summary = summary;
    }

    public static void open(RiftCreditsPacket summary) {
        Minecraft.getInstance().setScreen(new RiftCreditsScreen(summary));
    }

    /** Server reset is the one allowed early exit during the final crossing. */
    public static void closeFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof RiftCreditsScreen) client.setScreen(null);
    }

    @Override
    protected void init() {
        super.init();
        if (backgroundMusic == null) {
            Minecraft client = Minecraft.getInstance();
            // Stop any normal biome music, but leave sound settings respected.
            client.getMusicManager().stopPlaying();
            backgroundMusic = SimpleSoundInstance.forMusic(ModSounds.RIFT_OUTRO_MUSIC.get());
            client.getSoundManager().play(backgroundMusic);
        }
    }

    @Override
    public void removed() {
        if (backgroundMusic != null) {
            Minecraft.getInstance().getSoundManager().stop(backgroundMusic);
            backgroundMusic = null;
        }
        super.removed();
    }

    @Override
    public void tick() {
        elapsed++;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int mid = width / 2;
        int baseY = height / 2 - 47;
        graphics.fill(0, 0, width, height, 0xFF0B0911);
        // Subtle, deterministic 1px rift motes instead of heavy UI shaders.
        for (int i = 0; i < 48; i++) {
            int starX = Math.floorMod(i * 167 + elapsed / 5, Math.max(1, width));
            int starY = Math.floorMod(i * 89 + elapsed / 12, Math.max(1, height));
            graphics.fill(starX, starY, starX + 1, starY + 1,
                    i % 3 == 0 ? 0xFF66478F : 0xFF30243E);
        }
        graphics.fill(0, 0, width, 3, 0xFF8B62C2);
        graphics.fill(mid - Math.min(120, width / 3), height - 24,
                mid + Math.min(120, width / 3), height - 23, 0xFF47345F);

        if (elapsed < 140) {
            heading(graphics, mid, baseY + 10, "THE FRACTURED ISLAND");
            line(graphics, mid, baseY + 35, "One island. Countless stories.");
            line(graphics, mid, baseY + 51, "And one journey that was yours.");
        } else if (elapsed < 300) {
            heading(graphics, mid, baseY + 10, "YOUR JOURNEY");
            line(graphics, mid, baseY + 35, "Time spent here: " + playtime(summary.playTicks()));
            line(graphics, mid, baseY + 51, "Creatures defeated: " + summary.mobKills());
            line(graphics, mid, baseY + 67, "Times fallen: " + summary.deaths());
        } else if (elapsed < 460) {
            heading(graphics, mid, baseY + 10, "THE PEOPLE YOU MET");
            line(graphics, mid, baseY + 35, "Conversations with the Seven: " + summary.conversations());
            line(graphics, mid, baseY + 51, "You spoke most with: " + summary.favouriteName());
            line(graphics, mid, baseY + 67, "Visits: " + summary.favouriteVisits());
            if (!summary.runnerUpName().isBlank() && summary.runnerUpVisits() > 0) {
                line(graphics, mid, baseY + 83, "Also remembered: " + summary.runnerUpName());
            }
        } else if (elapsed < 600) {
            heading(graphics, mid, baseY + 10, "THE BONDS YOU MADE");
            line(graphics, mid, baseY + 35, "Town friendships formed: " + summary.friends() + " / 7");
            line(graphics, mid, baseY + 51, "Every conversation left its mark.");
            line(graphics, mid, baseY + 67, "Every choice became part of your story.");
        } else if (elapsed < 720) {
            heading(graphics, mid, baseY + 10, "THANK YOU FOR PLAYING");
            line(graphics, mid, baseY + 35, "Thank you for spending your time on the island.");
            line(graphics, mid, baseY + 51, "The memories you made here will stay with you.");
        } else if (elapsed < 960) {
            heading(graphics, mid, baseY + 2, "SPECIAL THANKS - PART I");
            line(graphics, mid, baseY + 22, "Fresh Animations");
            line(graphics, mid, baseY + 36, "Fresh Animations: Player Extension");
            line(graphics, mid, baseY + 50, "Just Expressions");
            line(graphics, mid, baseY + 64, "Voice Studio");
            line(graphics, mid, baseY + 78, "StarlightSkins");
            line(graphics, mid, baseY + 92, "Pixabay");
        } else if (elapsed < 1200) {
            heading(graphics, mid, baseY + 2, "SPECIAL THANKS - PART II");
            line(graphics, mid, baseY + 22, "Blockbench");
            line(graphics, mid, baseY + 36, "Epic Knights: Shields, Armor and Weapons");
            line(graphics, mid, baseY + 50, "Skin Layers 3D");
            line(graphics, mid, baseY + 64, "Photon");
            line(graphics, mid, baseY + 78, "Iron's Spells 'n Spellbooks");
            line(graphics, mid, baseY + 92, "Outro music: AtlasAudio (via Pixabay)");
        } else {
            heading(graphics, mid, baseY + 10, "BEYOND THE FRACTURE");
            line(graphics, mid, baseY + 35, "Beyond the island, another world awaits.");
            line(graphics, mid, baseY + 51, "Beneath unfamiliar skies, a new chapter begins.");
            line(graphics, mid, baseY + 76, "Until we meet again.");
        }

        graphics.drawCenteredString(font, "A CYBERSPECTRA STORY",
                mid, height - 42, 0xFF77688E);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void heading(GuiGraphics g, int x, int y, String text) {
        fittedText(g, x, y, text, (slideAlpha() << 24) | 0xD3A5FF);
    }

    private void line(GuiGraphics g, int x, int y, String text) {
        fittedText(g, x, y, text, (slideAlpha() << 24) | 0xE7DFF2);
    }

    /** Prevent long mod names from clipping at large Minecraft GUI scales. */
    private void fittedText(GuiGraphics g, int x, int y, String text, int color) {
        float scale = Math.min(1.0F,
                (float) Math.max(40, width - 24) / Math.max(1, font.width(text)));
        if (scale == 1.0F) {
            g.drawCenteredString(font, text, x, y, color);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0F);
        g.pose().scale(scale, scale, 1F);
        g.drawCenteredString(font, text, 0, 0, color);
        g.pose().popPose();
    }

    private int slideAlpha() {
        int[] bounds = {0, 140, 300, 460, 600, 720, 960, 1200, 1440};
        for (int i = 0; i < bounds.length - 1; i++) {
            if (elapsed < bounds[i + 1]) {
                int edge = Math.min(elapsed - bounds[i], bounds[i + 1] - elapsed);
                return Math.max(0, Math.min(255, edge * 255 / 17));
            }
        }
        return 0;
    }

    private static String playtime(int ticks) {
        long minutes = Math.max(0L, ticks / 1200L);
        return (minutes / 60L) + "h " + (minutes % 60L) + "m";
    }

    @Override
    public void onClose() {
        // No Escape skip. The server ends or resets the farewell.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
