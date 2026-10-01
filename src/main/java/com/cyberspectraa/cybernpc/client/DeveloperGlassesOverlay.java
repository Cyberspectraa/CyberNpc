package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.WildNpcClass;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class DeveloperGlassesOverlay {
    private static final double DEBUG_RANGE = 64.0D;

    private static final int PANEL_WIDTH = 252;
    private static final int PANEL_HEIGHT = 154;
    private static final int HEADER_HEIGHT = 31;
    private static final int FOOTER_HEIGHT = 22;

    private static final int BORDER_DARK = 0xF02B2B2B;
    private static final int PANEL = 0xE8C6C6C6;
    private static final int PANEL_LIGHT = 0xE8FFFFFF;
    private static final int PANEL_SHADOW = 0xE8555555;
    private static final int CONTENT = 0xE8A8A8A8;
    private static final int SLOT = 0xE88B8B8B;
    private static final int SLOT_LIGHT = 0xE8FFFFFF;
    private static final int SLOT_SHADOW = 0xE8373737;

    private static final int TEXT = 0x303030;
    private static final int TEXT_DIM = 0x555555;
    private static final int GOLD = 0xFFAA00;
    private static final int GREEN = 0x55AA55;
    private static final int RED = 0xCC3333;
    private static final int BLUE = 0x3366AA;
    private static final int PURPLE = 0x8844AA;

    public static final KeyMapping CYCLE_TAB = new KeyMapping(
            "key.cybernpc.developer_glasses_cycle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.cybernpc"
    );

    private static int currentTab;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.screen != null
                || !isWearingGlasses(minecraft)) {
            return;
        }

        while (CYCLE_TAB.consumeClick()) {
            currentTab = (currentTab + 1) % Tab.values().length;
        }
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || !isWearingGlasses(minecraft)) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        Font font = minecraft.font;
        CyberNpcEntity npc = findLookedAtNpc(minecraft);

        int x = 8;
        int y = 8;

        if (npc == null) {
            drawEmptyCard(gui, font, x, y);
            return;
        }

        drawMinecraftPanel(gui, x, y, PANEL_WIDTH, PANEL_HEIGHT);

        Tab tab = Tab.values()[currentTab];
        drawHeader(gui, font, npc, tab, x, y);
        drawContentBackground(
                gui,
                x + 6,
                y + HEADER_HEIGHT + 3,
                PANEL_WIDTH - 12,
                PANEL_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - 7
        );

        switch (tab) {
            case STATUS -> drawStatusPage(gui, font, npc, x, y);
            case MIND -> drawMindPage(gui, font, npc, x, y);
            case SOCIAL -> drawSocialPage(gui, font, npc, x, y);
            case GEAR -> drawGearPage(gui, font, npc, x, y);
        }

        drawFooter(gui, font, x, y);
    }

    private static void drawEmptyCard(
            GuiGraphics gui,
            Font font,
            int x,
            int y
    ) {
        int width = 228;
        int height = 58;
        drawMinecraftPanel(gui, x, y, width, height);

        gui.drawString(font, "Developer Glasses", x + 10, y + 9, GOLD, true);
        gui.drawString(font, "Look at a CyberNpc", x + 10, y + 27, TEXT, false);
        gui.drawString(
                font,
                keyHint() + " changes page",
                x + 10,
                y + 40,
                TEXT_DIM,
                false
        );
    }

    private static void drawHeader(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            Tab tab,
            int x,
            int y
    ) {
        gui.fill(x + 4, y + 4, x + PANEL_WIDTH - 4, y + 27, 0xE83A3A3A);
        gui.fill(x + 5, y + 5, x + PANEL_WIDTH - 5, y + 6, 0xFF777777);
        gui.drawString(
                font,
                trim(font, npc.getName().getString(), 142),
                x + 10,
                y + 9,
                0xFFFFFF,
                true
        );
        gui.drawString(
                font,
                tab.displayName,
                x + PANEL_WIDTH - 10 - font.width(tab.displayName),
                y + 9,
                GOLD,
                true
        );
        gui.drawString(
                font,
                npc.getWildClassDisplayName() + "  •  " + npc.getPersonalityDisplayName(),
                x + 10,
                y + 19,
                0xCFCFCF,
                false
        );
    }

    private static void drawStatusPage(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        int left = x + 13;
        int top = y + 40;

        gui.drawString(font, "HEALTH", left, top, TEXT_DIM, false);
        drawSegmentBar(
                gui,
                left + 49,
                top + 1,
                10,
                npc.getMaxHealth() <= 0.0F
                        ? 0.0F
                        : npc.getHealth() / npc.getMaxHealth(),
                RED
        );
        gui.drawString(
                font,
                String.format("%.0f/%.0f", npc.getHealth(), npc.getMaxHealth()),
                left + 169,
                top,
                TEXT,
                false
        );

        top += 17;
        gui.drawString(font, "HUNGER", left, top, TEXT_DIM, false);
        drawSegmentBar(gui, left + 49, top + 1, 10, npc.getHunger() / 20.0F, GOLD);
        gui.drawString(
                font,
                npc.getHunger() + "/20",
                left + 169,
                top,
                npc.getHunger() <= 6 ? RED : TEXT,
                false
        );

        top += 20;
        drawLabelValue(gui, font, "Doing", npc.getDebugActivity(), left, top, BLUE);
        top += 16;
        drawLabelValue(gui, font, "Intent", npc.getDebugIntention(), left, top, GREEN);

        top += 18;
        gui.drawString(font, "GEAR", left, top, TEXT_DIM, false);
        gui.drawString(
                font,
                npc.getGearTierDisplayName()
                        + (npc.getWildClass().hasMagicSchool()
                        ? "  •  " + npc.getMageSchoolDisplayName()
                        : ""),
                left + 42,
                top,
                classColor(npc.getWildClass()),
                false
        );

        if (npc.isZombifying()) {
            top += 16;
            gui.drawString(
                    font,
                    String.format(
                            "Zombifying: %.0f%%",
                            npc.getZombificationProgress() * 100.0F
                    ),
                    left,
                    top,
                    0x336633,
                    true
            );
        }
    }

    private static void drawMindPage(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        int left = x + 13;
        int top = y + 40;
        int valueWidth = 171;

        drawLabelValue(gui, font, "Doing", npc.getDebugActivity(), left, top, BLUE);
        top += 17;
        drawLabelValue(gui, font, "Intent", npc.getDebugIntention(), left, top, GREEN);
        top += 19;

        gui.drawString(font, "WHY", left, top, TEXT_DIM, false);
        top += 11;
        top = drawWrapped(
                gui,
                font,
                npc.getDebugReason(),
                left,
                top,
                PANEL_WIDTH - 27,
                2,
                TEXT
        );

        top += 4;
        drawLabelValue(
                gui,
                font,
                "Target",
                trim(font, npc.getDebugTarget(), valueWidth),
                left,
                top,
                RED
        );
        top += 16;
        drawLabelValue(
                gui,
                font,
                "Path",
                trim(font, npc.getDebugPath(), valueWidth),
                left,
                top,
                TEXT
        );
        top += 16;
        drawLabelValue(
                gui,
                font,
                "Conf.",
                trim(font, npc.getDebugConfidence(), valueWidth),
                left,
                top,
                PURPLE
        );
    }

    private static void drawSocialPage(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        int left = x + 13;
        int top = y + 40;

        drawLabelValue(gui, font, "Party", npc.getDebugParty(), left, top, BLUE);
        top += 20;

        gui.drawString(font, "RELATIONSHIPS", left, top, TEXT_DIM, false);
        top += 13;

        String social = npc.getDebugRelationships();
        if (social == null
                || social.isBlank()
                || "none".equalsIgnoreCase(social)) {
            gui.drawString(font, "No notable relationships yet.", left, top, TEXT, false);
            return;
        }

        String[] relations = social.split("\\|");
        int shown = 0;
        for (String relation : relations) {
            if (shown >= 5) {
                gui.drawString(
                        font,
                        "+" + (relations.length - shown) + " more",
                        left,
                        top,
                        TEXT_DIM,
                        false
                );
                break;
            }

            drawBullet(
                    gui,
                    font,
                    trim(font, relation.trim(), PANEL_WIDTH - 39),
                    left,
                    top,
                    relation.startsWith("Player ") ? GOLD : GREEN
            );
            top += 15;
            shown++;
        }
    }

    private static void drawGearPage(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        int left = x + 13;
        int top = y + 39;

        gui.drawString(font, "EQUIPMENT", left, top, TEXT_DIM, false);
        top += 13;

        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
        };

        int slotX = left;
        for (EquipmentSlot slot : slots) {
            drawItemSlot(gui, font, npc.getItemBySlot(slot), slotX, top);
            slotX += 22;
        }

        gui.drawString(
                font,
                "armor                 hands",
                left,
                top + 22,
                TEXT_DIM,
                false
        );

        top += 40;
        drawLabelValue(
                gui,
                font,
                "Bag",
                trim(font, npc.getDebugInventory(), 183),
                left,
                top,
                TEXT
        );
        top += 18;
        drawLabelValue(
                gui,
                font,
                "Claims",
                trim(font, npc.getDebugClaims(), 171),
                left,
                top,
                GREEN
        );

        if (npc.getWildClass().usesSpellBook()) {
            top += 18;
            gui.drawString(font, "SPELLS", left, top, TEXT_DIM, false);
            top += 12;

            String spells = npc.getDebugSpells();
            if (spells == null || spells.isBlank()) {
                gui.drawString(font, "No spell data.", left, top, TEXT, false);
            } else {
                String[] entries = spells.split("\\|");
                int count = Math.min(2, entries.length);
                for (int i = 0; i < count; i++) {
                    gui.drawString(
                            font,
                            trim(font, entries[i], PANEL_WIDTH - 28),
                            left,
                            top,
                            PURPLE,
                            false
                    );
                    top += 12;
                }
            }
        }
    }

    private static void drawFooter(
            GuiGraphics gui,
            Font font,
            int x,
            int y
    ) {
        int footerY = y + PANEL_HEIGHT - FOOTER_HEIGHT;
        gui.fill(
                x + 5,
                footerY,
                x + PANEL_WIDTH - 5,
                y + PANEL_HEIGHT - 5,
                0xE83A3A3A
        );

        int cursor = x + 10;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            boolean active = i == currentTab;
            int labelWidth = font.width(tab.shortName) + 12;

            drawTabButton(
                    gui,
                    cursor,
                    footerY + 3,
                    labelWidth,
                    14,
                    active
            );

            gui.drawString(
                    font,
                    tab.shortName,
                    cursor + 6,
                    footerY + 6,
                    active ? 0xFFFFFF : 0xB8B8B8,
                    active
            );

            cursor += labelWidth + 4;
        }

        gui.drawString(
                font,
                keyHint(),
                x + PANEL_WIDTH - 9 - font.width(keyHint()),
                footerY + 6,
                GOLD,
                true
        );
    }

    private static void drawMinecraftPanel(
            GuiGraphics gui,
            int x,
            int y,
            int width,
            int height
    ) {
        gui.fill(x, y, x + width, y + height, BORDER_DARK);
        gui.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
        gui.fill(x + 3, y + 3, x + width - 3, y + 4, PANEL_LIGHT);
        gui.fill(x + 3, y + 3, x + 4, y + height - 3, PANEL_LIGHT);
        gui.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, PANEL_SHADOW);
        gui.fill(x + width - 4, y + 3, x + width - 3, y + height - 3, PANEL_SHADOW);
    }

    private static void drawContentBackground(
            GuiGraphics gui,
            int x,
            int y,
            int width,
            int height
    ) {
        gui.fill(x, y, x + width, y + height, SLOT_SHADOW);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, CONTENT);
        gui.fill(x + 1, y + 1, x + width - 1, y + 2, 0xE8D6D6D6);
    }

    private static void drawTabButton(
            GuiGraphics gui,
            int x,
            int y,
            int width,
            int height,
            boolean active
    ) {
        int fill = active ? 0xE8787878 : 0xE84A4A4A;
        gui.fill(x, y, x + width, y + height, SLOT_SHADOW);
        gui.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        gui.fill(x + 1, y + 1, x + width - 1, y + 2, active ? SLOT_LIGHT : 0xE8707070);
    }

    private static void drawItemSlot(
            GuiGraphics gui,
            Font font,
            ItemStack stack,
            int x,
            int y
    ) {
        gui.fill(x, y, x + 20, y + 20, SLOT_SHADOW);
        gui.fill(x + 1, y + 1, x + 19, y + 19, SLOT);
        gui.fill(x + 1, y + 1, x + 19, y + 2, SLOT_LIGHT);
        gui.fill(x + 1, y + 1, x + 2, y + 19, SLOT_LIGHT);

        if (!stack.isEmpty()) {
            gui.renderItem(stack, x + 2, y + 2);
            gui.renderItemDecorations(font, stack, x + 2, y + 2);
        }
    }

    private static void drawSegmentBar(
            GuiGraphics gui,
            int x,
            int y,
            int segments,
            float fraction,
            int activeColor
    ) {
        int filled = Mth.clamp(
                Mth.ceil(Mth.clamp(fraction, 0.0F, 1.0F) * segments),
                0,
                segments
        );

        for (int i = 0; i < segments; i++) {
            int px = x + i * 11;
            gui.fill(px, y, px + 9, y + 8, SLOT_SHADOW);
            gui.fill(
                    px + 1,
                    y + 1,
                    px + 8,
                    y + 7,
                    i < filled ? activeColor : 0x666666
            );
            if (i < filled) {
                gui.fill(px + 2, y + 2, px + 7, y + 3, 0x55FFFFFF);
            }
        }
    }

    private static void drawLabelValue(
            GuiGraphics gui,
            Font font,
            String label,
            String value,
            int x,
            int y,
            int valueColor
    ) {
        gui.drawString(font, label.toUpperCase(), x, y, TEXT_DIM, false);
        int labelWidth = Math.max(42, font.width(label.toUpperCase()) + 8);
        gui.drawString(
                font,
                trim(font, value == null ? "none" : value, PANEL_WIDTH - labelWidth - 28),
                x + labelWidth,
                y,
                valueColor,
                false
        );
    }

    private static void drawBullet(
            GuiGraphics gui,
            Font font,
            String value,
            int x,
            int y,
            int color
    ) {
        gui.fill(x, y + 3, x + 5, y + 8, color);
        gui.drawString(font, value, x + 10, y + 1, TEXT, false);
    }

    private static int drawWrapped(
            GuiGraphics gui,
            Font font,
            String text,
            int x,
            int y,
            int maxWidth,
            int maxLines,
            int color
    ) {
        List<String> lines = wrap(font, text == null ? "" : text, maxWidth, maxLines);
        for (String line : lines) {
            gui.drawString(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    private static List<String> wrap(
            Font font,
            String text,
            int maxWidth,
            int maxLines
    ) {
        List<String> lines = new ArrayList<>();
        String remaining = text.trim();

        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }

            String fit = font.plainSubstrByWidth(remaining, maxWidth);
            int split = fit.lastIndexOf(' ');
            if (split <= 0) {
                split = fit.length();
            }

            String line = remaining.substring(0, split).trim();
            remaining = remaining.substring(Math.min(remaining.length(), split)).trim();

            if (lines.size() == maxLines - 1 && !remaining.isEmpty()) {
                line = trim(font, line + "...", maxWidth);
                remaining = "";
            }

            lines.add(line);
        }

        if (lines.isEmpty()) {
            lines.add("none");
        }

        return lines;
    }

    private static String keyHint() {
        return "[" + CYCLE_TAB.getTranslatedKeyMessage().getString() + "]";
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "none";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int allowed = Math.max(0, maxWidth - font.width(ellipsis));
        return font.plainSubstrByWidth(text, allowed) + ellipsis;
    }

    private static int classColor(WildNpcClass npcClass) {
        return switch (npcClass) {
            case ARCHER -> 0x2E7D32;
            case KNIGHT -> 0x355C9A;
            case ROGUE -> 0x6A3D8E;
            case BERSERKER -> 0x9B3A32;
            case HORSE_TAMER -> 0x795548;
            case MAGE -> 0x8E3C8E;
            case CLERIC -> 0x9A7B16;
            case SPELLBLADE -> 0x336A8B;
            default -> 0x3F3F3F;
        };
    }

    private static boolean isWearingGlasses(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player
                .getItemBySlot(EquipmentSlot.HEAD)
                .is(ModItems.DEVELOPER_GLASSES.get());
    }

    private static CyberNpcEntity findLookedAtNpc(Minecraft minecraft) {
        Vec3 start = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(DEBUG_RANGE));

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                start,
                end,
                minecraft.player
                        .getBoundingBox()
                        .expandTowards(look.scale(DEBUG_RANGE))
                        .inflate(1.0D),
                entity -> entity instanceof CyberNpcEntity,
                DEBUG_RANGE * DEBUG_RANGE
        );

        return hit != null && hit.getEntity() instanceof CyberNpcEntity npc
                ? npc
                : null;
    }

    private enum Tab {
        STATUS("Status", "Status"),
        MIND("Mind", "Mind"),
        SOCIAL("Social", "Social"),
        GEAR("Gear", "Gear");

        private final String displayName;
        private final String shortName;

        Tab(String displayName, String shortName) {
            this.displayName = displayName;
            this.shortName = shortName;
        }
    }

    private DeveloperGlassesOverlay() {
    }
}
