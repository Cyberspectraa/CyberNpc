package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.RiftCreditsPacket;
import com.cyberspectraa.cybernpc.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

/**
 * A quiet, Minecraft-font farewell: fixed-position text, readable timing and
 * smooth crossfades, not animated particle fields or moving/scaling paragraphs.
 *
 * The server still owns the 72-second deadline, invisibility and disconnect.
 * The soundtrack belongs only to this screen and stops when it is removed.
 */
public final class RiftCreditsScreen extends Screen {
    // Every page uses the same clock. Crossfades overlap at these boundaries
    // so neither the heading nor the text abruptly pops to a new page.
    private static final int[] PAGE_TICKS = {
            0, 140, 320, 500, 620, 840, 1060, 1240, 1440
    };
    private static final float FADE_HALF_TICKS = 12F;
    private static final int BACKGROUND = 0xFF101010;
    private static final int FRAME = 0xFF39342C;
    private static final int HEADING = 0xFFE7D2A7;
    private static final int BODY = 0xFFE5E5E5;
    private static final int DETAIL = 0xFFA8A39B;

    private static final CreditEntry[] RESOURCE_PACKS = {
            new CreditEntry("Fresh Animations", "Resource pack - mob animations"),
            new CreditEntry("Fresh Animations: Player Extension",
                    "Resource pack - player animations"),
            new CreditEntry("Just Expressions", "Resource pack - player expressions")
    };
    private static final CreditEntry[] MODS = {
            new CreditEntry("Epic Knights: Shields, Armor and Weapons",
                    "Mod - medieval equipment"),
            new CreditEntry("Skin Layers 3D", "Mod - 3D player skins"),
            new CreditEntry("Photon", "Mod - visual effects editor"),
            new CreditEntry("Iron's Spells 'n Spellbooks", "Mod - spells and magic")
    };
    private static final CreditEntry[] TOOLS_AND_CREATORS = {
            new CreditEntry("Blockbench", "Tool - 3D modelling"),
            new CreditEntry("Voice Studio", "Tool - voice / audio"),
            new CreditEntry("StarlightSkins", "Minecraft skin creator"),
            new CreditEntry("Pixabay", "Website - stock media"),
            new CreditEntry("AtlasAudio", "Outro music - via Pixabay")
    };

    private final RiftCreditsPacket summary;
    private SimpleSoundInstance backgroundMusic;
    private int elapsed;

    private RiftCreditsScreen(RiftCreditsPacket summary) {
        super(Component.literal("The Fractured Island - Credits"));
        this.summary = summary;
    }

    public static void open(RiftCreditsPacket summary) {
        Minecraft.getInstance().setScreen(new RiftCreditsScreen(summary));
    }

    /** Server reset is the only permitted interruption of the departure. */
    public static void closeFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof RiftCreditsScreen) {
            client.setScreen(null);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (backgroundMusic == null) {
            Minecraft client = Minecraft.getInstance();
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
        graphics.fill(0, 0, width, height, BACKGROUND);

        // Two simple pixel lines, the usual Minecraft font, no drifting
        // particles or ornamental glowing panels.
        graphics.fill(0, 0, width, 2, FRAME);
        graphics.fill(0, height - 2, width, height, FRAME);
        graphics.fill(width / 2 - Math.min(72, width / 4), height - 30,
                width / 2 + Math.min(72, width / 4), height - 29, FRAME);
        graphics.drawCenteredString(font, "FRACTURED ISLAND",
                width / 2, height - 23, DETAIL);

        float time = elapsed + Math.max(0F, Math.min(1F, partialTick));
        for (int page = 0; page < PAGE_TICKS.length - 1; page++) {
            float opacity = pageOpacity(time, page);
            if (opacity > 0F) renderPage(graphics, page, opacity);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** C1 smoothstep: both old and new cards move through 50% at a boundary. */
    private static float smooth(float value) {
        float t = Math.max(0F, Math.min(1F, value));
        return t * t * (3F - 2F * t);
    }

    private float pageOpacity(float time, int page) {
        float fadeIn = page == 0
                ? smooth(time / (2F * FADE_HALF_TICKS))
                : smooth((time - PAGE_TICKS[page] + FADE_HALF_TICKS)
                        / (2F * FADE_HALF_TICKS));
        float fadeOut = page == PAGE_TICKS.length - 2
                ? smooth((PAGE_TICKS[page + 1] - time) / (2F * FADE_HALF_TICKS))
                : 1F - smooth((time - PAGE_TICKS[page + 1] + FADE_HALF_TICKS)
                        / (2F * FADE_HALF_TICKS));
        return Math.min(fadeIn, fadeOut);
    }

    private void renderPage(GuiGraphics g, int page, float fade) {
        int mid = width / 2;
        int y = height / 2;
        switch (page) {
            case 0 -> {
                heading(g, mid, y - 24, "THE FRACTURED ISLAND", fade);
                body(g, mid, y + 4, "Thank you for playing.", fade);
                detail(g, mid, y + 25, "A CYBERSPECTRA SERIES", fade);
            }
            case 1 -> {
                heading(g, mid, y - 55, "YOUR TIME ON THE ISLAND", fade);
                body(g, mid, y - 17, "Time played: " + playtime(summary.playTicks()), fade);
                body(g, mid, y + 5, "Creatures defeated: " + summary.mobKills(), fade);
                body(g, mid, y + 27, "Deaths: " + summary.deaths(), fade);
            }
            case 2 -> {
                heading(g, mid, y - 57, "THE PEOPLE YOU MET", fade);
                body(g, mid, y - 27,
                        "Conversations with the Seven: " + summary.conversations(), fade);
                body(g, mid, y - 4,
                        "Most visited: " + summary.favouriteName(), fade);
                detail(g, mid, y + 10, summary.favouriteVisits() + " visits", fade);
                if (!summary.runnerUpName().isBlank() && summary.runnerUpVisits() > 0) {
                    detail(g, mid, y + 29,
                            "Also visited: " + summary.runnerUpName(), fade);
                }
                detail(g, mid, y + 49,
                        "Town friendships: " + summary.friends() + " / 7", fade);
            }
            case 3 -> {
                heading(g, mid, y - 25, "THANK YOU", fade);
                body(g, mid, y + 2, "For spending your time here.", fade);
                detail(g, mid, y + 24, "And for making it to the Rift.", fade);
            }
            case 4 -> credits(g, fade, "RESOURCE PACKS", RESOURCE_PACKS);
            case 5 -> credits(g, fade, "MINECRAFT MODS", MODS);
            case 6 -> credits(g, fade, "TOOLS, WEBSITES & CREATORS",
                    TOOLS_AND_CREATORS);
            case 7 -> {
                heading(g, mid, y - 33, "BEYOND THE RIFT", fade);
                body(g, mid, y - 4, "The island is behind you now.", fade);
                body(g, mid, y + 16, "What waits on the other side?", fade);
                detail(g, mid, y + 45, "TO BE CONTINUED", fade);
            }
            default -> { }
        }
    }

    private record CreditEntry(String name, String category) {}

    private void credits(GuiGraphics g, float fade, String heading,
                         CreditEntry[] entries) {
        int mid = width / 2;
        int y = height / 2;
        heading(g, mid, y - 69, heading, fade);
        int step = Math.min(24, Math.max(17, (height - 70) / 5));
        int top = y - 43;
        for (int i = 0; i < entries.length; i++) {
            CreditEntry entry = entries[i];
            body(g, mid, top + i * step, entry.name(), fade);
            detail(g, mid, top + i * step + 10, entry.category(), fade);
        }
    }

    private void heading(GuiGraphics g, int x, int y, String value, float fade) {
        drawFitted(g, x, y, value, HEADING, fade);
    }

    private void body(GuiGraphics g, int x, int y, String value, float fade) {
        drawFitted(g, x, y, value, BODY, fade);
    }

    private void detail(GuiGraphics g, int x, int y, String value, float fade) {
        drawFitted(g, x, y, value, DETAIL, fade);
    }

    private void drawFitted(GuiGraphics g, int x, int y,
                            String value, int rgb, float fade) {
        int color = ((Math.round(255F * fade) & 0xFF) << 24)
                | (rgb & 0xFFFFFF);
        // Scale long names ONLY when the window width demands it. There is
        // never a per-frame zoom or shift to make text appear animated.
        float scale = Math.min(1F,
                (float) Math.max(40, width - 24) / Math.max(1, font.width(value)));
        if (scale >= 1F) {
            g.drawCenteredString(font, value, x, y, color);
        } else {
            g.pose().pushPose();
            g.pose().translate(x, y, 0F);
            g.pose().scale(scale, scale, 1F);
            g.drawCenteredString(font, value, 0, 0, color);
            g.pose().popPose();
        }
    }

    private static String playtime(int ticks) {
        long minutes = Math.max(0L, ticks / 1200L);
        return minutes / 60L + "h " + minutes % 60L + "m";
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
