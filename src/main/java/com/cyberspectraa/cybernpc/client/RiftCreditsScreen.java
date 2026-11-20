package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.network.RiftCreditsPacket;
import com.cyberspectraa.cybernpc.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

/**
 * Credits in the style of Minecraft's own ending: a few quiet cards, then one
 * steady scrolling credit roll. No overlapping cards, moving backgrounds,
 * synthetic glows, font animations or interpolated text scaling.
 *
 * The dedicated server still controls all 72 seconds of the finale, player
 * invisibility, the saved departure and the final disconnect.
 */
public final class RiftCreditsScreen extends Screen {
    private static final int INTRO_END = 150;
    private static final int STATS_END = 320;
    private static final int PEOPLE_END = 500;
    private static final int THANKS_END = 600;
    private static final int ROLL_END = 1240;
    private static final int END = 1440;

    private record CreditEntry(String name, String description) {}
    private record CreditGroup(String title, CreditEntry[] entries) {}

    private static final CreditGroup[] GROUPS = {
            new CreditGroup("RESOURCE PACKS", new CreditEntry[]{
                    new CreditEntry("Fresh Animations", "Mob animation resource pack"),
                    new CreditEntry("Fresh Animations: Player Extension",
                            "Player animation resource pack"),
                    new CreditEntry("Just Expressions", "Expression resource pack")
            }),
            new CreditGroup("MODS", new CreditEntry[]{
                    new CreditEntry("Epic Knights: Shields, Armor and Weapons",
                            "Medieval equipment mod"),
                    new CreditEntry("Skin Layers 3D", "3D player-skin mod"),
                    new CreditEntry("Photon", "Visual effects mod"),
                    new CreditEntry("Iron's Spells 'n Spellbooks", "Magic mod")
            }),
            new CreditGroup("TOOLS & WEBSITES", new CreditEntry[]{
                    new CreditEntry("Blockbench", "3D modelling tool"),
                    new CreditEntry("Voice Studio", "Voice and audio tool"),
                    new CreditEntry("StarlightSkins", "Minecraft skin creator"),
                    new CreditEntry("Pixabay", "Stock media website")
            }),
            new CreditGroup("MUSIC", new CreditEntry[]{
                    new CreditEntry("AtlasAudio", "Outro soundtrack via Pixabay")
            })
    };

    private final RiftCreditsPacket summary;
    private SimpleSoundInstance backgroundMusic;
    private int elapsed;

    private RiftCreditsScreen(RiftCreditsPacket summary) {
        super(Component.literal("Fractured Island - Credits"));
        this.summary = summary;
    }

    public static void open(RiftCreditsPacket summary) {
        Minecraft.getInstance().setScreen(new RiftCreditsScreen(summary));
    }

    /** The server may cancel a retest without leaving audio or a locked GUI. */
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
        RiftUiStyle.background(graphics, width, height);
        float time = elapsed + Math.max(0F, Math.min(1F, partialTick));

        // There is exactly ONE page at any instant. The final frames of one
        // page fade to black before the next page begins; none are superimposed.
        if (time < INTRO_END) {
            opening(graphics, RiftUiStyle.cardOpacity(time, 0, INTRO_END));
        } else if (time < STATS_END) {
            statistics(graphics, RiftUiStyle.cardOpacity(time, INTRO_END, STATS_END));
        } else if (time < PEOPLE_END) {
            townsfolk(graphics, RiftUiStyle.cardOpacity(time, STATS_END, PEOPLE_END));
        } else if (time < THANKS_END) {
            thanks(graphics, RiftUiStyle.cardOpacity(time, PEOPLE_END, THANKS_END));
        } else if (time < ROLL_END) {
            roll(graphics, time, RiftUiStyle.cardOpacity(time, THANKS_END, ROLL_END));
        } else if (time < END) {
            farewell(graphics, RiftUiStyle.cardOpacity(time, ROLL_END, END));
        }

        RiftUiStyle.footer(graphics, font, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void opening(GuiGraphics graphics, float opacity) {
        text(graphics, height / 2 - 21, "THE FRACTURED ISLAND",
                RiftUiStyle.HEADING, opacity);
        text(graphics, height / 2 + 9, "Thank you for playing.",
                RiftUiStyle.TEXT, opacity);
        text(graphics, height / 2 + 31, "A CYBERSPECTRA SERIES",
                RiftUiStyle.DETAIL, opacity);
    }

    private void statistics(GuiGraphics graphics, float opacity) {
        int y = height / 2;
        text(graphics, y - 51, "YOUR TIME HERE", RiftUiStyle.HEADING, opacity);
        text(graphics, y - 17, "Time played: " + playtime(summary.playTicks()),
                RiftUiStyle.TEXT, opacity);
        text(graphics, y + 6, "Creatures defeated: " + summary.mobKills(),
                RiftUiStyle.TEXT, opacity);
        text(graphics, y + 29, "Deaths: " + summary.deaths(),
                RiftUiStyle.TEXT, opacity);
    }

    private void townsfolk(GuiGraphics graphics, float opacity) {
        int y = height / 2;
        text(graphics, y - 57, "THE PEOPLE YOU MET", RiftUiStyle.HEADING, opacity);
        text(graphics, y - 28, "Conversations with the Seven: " + summary.conversations(),
                RiftUiStyle.TEXT, opacity);
        text(graphics, y - 6, "Most visited: " + summary.favouriteName(),
                RiftUiStyle.TEXT, opacity);
        text(graphics, y + 12, summary.favouriteVisits() + " visits",
                RiftUiStyle.DETAIL, opacity);
        if (!summary.runnerUpName().isBlank() && summary.runnerUpVisits() > 0) {
            text(graphics, y + 30, "Also visited: " + summary.runnerUpName(),
                    RiftUiStyle.DETAIL, opacity);
        }
        text(graphics, y + 49, "Town friendships: " + summary.friends() + " / 7",
                RiftUiStyle.DETAIL, opacity);
    }

    private void thanks(GuiGraphics graphics, float opacity) {
        int y = height / 2;
        text(graphics, y - 24, "THANK YOU", RiftUiStyle.HEADING, opacity);
        text(graphics, y + 5, "For spending your time here.",
                RiftUiStyle.TEXT, opacity);
        text(graphics, y + 28, "And for making it to the Rift.",
                RiftUiStyle.DETAIL, opacity);
    }

    /**
     * Classic vertically scrolling credits. Movement uses a fractional render
     * tick, not integer text coordinates or one-page-at-a-time jumps.
     *
     * Starting below the viewing area and ending above it ensures every entry
     * passes through the visible window at the same steady rate.
     */
    private void roll(GuiGraphics graphics, float time, float opacity) {
        if (opacity <= 0.01F) return;
        int top = Math.max(46, Math.min(62, height / 5));
        int bottom = Math.max(top + 32, height - 40);
        int middle = width / 2;
        int totalHeight = rollHeight();

        // A scroll from below the clipping window to completely above it.
        float progress = Math.max(0F, Math.min(1F,
                (time - THANKS_END) / (ROLL_END - THANKS_END)));
        float travel = totalHeight + bottom - top + 28F;
        float origin = bottom + 10F - progress * travel;

        text(graphics, top - 25, "CREDITS", RiftUiStyle.HEADING, opacity);
        graphics.enableScissor(0, top, width, bottom);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0F, origin, 0F);
            int y = 0;
            for (CreditGroup group : GROUPS) {
                RiftUiStyle.centered(graphics, font, width, middle, y,
                        group.title(), RiftUiStyle.HEADING, opacity);
                y += 29;
                for (CreditEntry entry : group.entries()) {
                    RiftUiStyle.centered(graphics, font, width, middle, y,
                            entry.name(), RiftUiStyle.TEXT, opacity);
                    RiftUiStyle.centered(graphics, font, width, middle, y + 12,
                            entry.description(), RiftUiStyle.DETAIL, opacity);
                    y += 30;
                }
                y += 17;
            }
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private static int rollHeight() {
        int height = 0;
        for (CreditGroup group : GROUPS) {
            height += 29 + 30 * group.entries().length + 17;
        }
        return height;
    }

    private void farewell(GuiGraphics graphics, float opacity) {
        int y = height / 2;
        text(graphics, y - 33, "BEYOND THE RIFT", RiftUiStyle.HEADING, opacity);
        text(graphics, y - 5, "The island is behind you now.",
                RiftUiStyle.TEXT, opacity);
        text(graphics, y + 16, "What waits on the other side?",
                RiftUiStyle.TEXT, opacity);
        text(graphics, y + 45, "TO BE CONTINUED",
                RiftUiStyle.DETAIL, opacity);
    }

    private void text(GuiGraphics graphics, int y, String value, int color, float opacity) {
        RiftUiStyle.centered(graphics, font, width, width / 2,
                y, value, color, opacity);
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
