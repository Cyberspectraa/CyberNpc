package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
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

@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class DeveloperGlassesOverlay {
    private static final double DEBUG_RANGE = 64.0D;

    // Intentionally tiny. This is a glanceable Minecraft HUD card, not a
    // desktop debug window.
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 98;
    private static final int HEADER_HEIGHT = 23;
    private static final int FOOTER_HEIGHT = 18;
    private static final int BODY_TOP = HEADER_HEIGHT + 3;
    private static final int BODY_BOTTOM = PANEL_HEIGHT - FOOTER_HEIGHT - 3;

    private static final int OUTLINE = 0xF0222222;
    private static final int PANEL = 0xF0C6C6C6;
    private static final int PANEL_HI = 0xF0FFFFFF;
    private static final int PANEL_LO = 0xF0555555;
    private static final int WELL = 0xF08B8B8B;
    private static final int WELL_DARK = 0xF0373737;

    private static final int TEXT = 0x303030;
    private static final int DIM = 0x5B5B5B;
    private static final int WHITE = 0xFFFFFF;
    private static final int GOLD = 0xFFAA00;
    private static final int GREEN = 0x3E7D3E;
    private static final int BLUE = 0x315F8F;
    private static final int RED = 0xB43A32;
    private static final int PURPLE = 0x7A428C;

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
            currentTab = (currentTab + 1) % Page.values().length;
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

        CyberNpcEntity npc = findLookedAtNpc(minecraft);
        if (npc == null) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        Font font = minecraft.font;

        int x = 6;
        int y = 6;

        drawPanel(gui, x, y, PANEL_WIDTH, PANEL_HEIGHT);
        drawHeader(gui, font, npc, x, y);
        drawBodyWell(gui, x, y);
        drawFooter(gui, font, x, y);

        // Hard clipping is deliberate. Text can be malformed, translated or
        // unexpectedly long and it still cannot escape the body rectangle.
        int bodyX = x + 7;
        int bodyY = y + BODY_TOP + 2;
        int bodyRight = x + PANEL_WIDTH - 7;
        int bodyBottom = y + BODY_BOTTOM - 1;

        gui.enableScissor(bodyX, bodyY, bodyRight, bodyBottom);
        try {
            switch (Page.values()[currentTab]) {
                case INFO -> drawInfo(gui, font, npc, bodyX, bodyY);
                case AI -> drawAi(gui, font, npc, bodyX, bodyY);
                case SOCIAL -> drawSocial(gui, font, npc, bodyX, bodyY);
                case DATA -> drawData(gui, font, npc, bodyX, bodyY);
            }
        } finally {
            gui.disableScissor();
        }
    }

    private static void drawHeader(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        gui.fill(
                x + 4,
                y + 4,
                x + PANEL_WIDTH - 4,
                y + HEADER_HEIGHT,
                0xF03B3B3B
        );
        gui.fill(
                x + 5,
                y + 5,
                x + PANEL_WIDTH - 5,
                y + 6,
                0xFF747474
        );

        gui.drawString(
                font,
                fit(font, npc.getName().getString(), 103),
                x + 8,
                y + 8,
                WHITE,
                true
        );

        String page = Page.values()[currentTab].label;
        gui.drawString(
                font,
                page,
                x + PANEL_WIDTH - 25 - font.width(page),
                y + 8,
                GOLD,
                true
        );

        gui.drawString(
                font,
                "[V]",
                x + PANEL_WIDTH - 22,
                y + 8,
                0xC8C8C8,
                false
        );
    }

    private static void drawInfo(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        row(gui, font, "Class", npc.getWildClassDisplayName(), x, y, BLUE);
        row(
                gui,
                font,
                "Trait",
                npc.getPersonalityDisplayName(),
                x,
                y + 11,
                PURPLE
        );

        gui.drawString(font, "HP", x, y + 24, DIM, false);
        miniBar(
                gui,
                x + 24,
                y + 25,
                8,
                npc.getMaxHealth() <= 0.0F
                        ? 0.0F
                        : npc.getHealth() / npc.getMaxHealth(),
                RED
        );

        gui.drawString(font, "Food", x, y + 35, DIM, false);
        miniBar(
                gui,
                x + 24,
                y + 36,
                8,
                npc.getHunger() / 20.0F,
                GOLD
        );

        row(
                gui,
                font,
                "Now",
                npc.getDebugActivity(),
                x,
                y + 47,
                GREEN
        );
    }

    private static void drawAi(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        row(
                gui,
                font,
                "Intent",
                npc.getDebugIntention(),
                x,
                y,
                GREEN
        );
        row(
                gui,
                font,
                "Why",
                npc.getDebugReason(),
                x,
                y + 12,
                TEXT
        );
        row(
                gui,
                font,
                "Target",
                npc.getDebugTarget(),
                x,
                y + 24,
                RED
        );

        if ("Horse Tamer".equals(npc.getWildClassDisplayName())) {
            row(
                    gui,
                    font,
                    "Explore",
                    npc.getDebugExplorerState(),
                    x,
                    y + 36,
                    BLUE
            );
        } else {
            row(
                    gui,
                    font,
                    "Path",
                    npc.getDebugPath(),
                    x,
                    y + 36,
                    BLUE
            );
        }

        row(
                gui,
                font,
                "Conf.",
                npc.getDebugConfidence(),
                x,
                y + 48,
                PURPLE
        );
    }

    private static void drawSocial(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        row(gui, font, "Party", npc.getDebugParty(), x, y, BLUE);
        row(
                gui,
                font,
                "Places",
                npc.getDebugDiscoveries(),
                x,
                y + 12,
                GREEN
        );

        String social = npc.getDebugRelationships();
        if (social == null
                || social.isBlank()
                || "none".equalsIgnoreCase(social)) {
            row(gui, font, "Links", "none yet", x, y + 26, DIM);
            return;
        }

        String[] entries = social.split("\\|");
        int lineY = y + 26;
        int shown = Math.min(3, entries.length);

        for (int i = 0; i < shown; i++) {
            gui.fill(x, lineY + 3, x + 4, lineY + 7, GREEN);
            gui.drawString(
                    font,
                    fit(font, entries[i].trim(), 145),
                    x + 8,
                    lineY,
                    TEXT,
                    false
            );
            lineY += 11;
        }
    }

    private static void drawData(
            GuiGraphics gui,
            Font font,
            CyberNpcEntity npc,
            int x,
            int y
    ) {
        row(
                gui,
                font,
                "Gear",
                npc.getGearTierDisplayName(),
                x,
                y,
                GOLD
        );
        row(
                gui,
                font,
                "Bag",
                npc.getDebugInventory(),
                x,
                y + 12,
                TEXT
        );
        row(
                gui,
                font,
                "Claim",
                npc.getDebugClaims(),
                x,
                y + 24,
                GREEN
        );

        int slotY = y + 36;
        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
        };

        int slotX = x;
        for (EquipmentSlot slot : slots) {
            drawTinySlot(
                    gui,
                    font,
                    npc.getItemBySlot(slot),
                    slotX,
                    slotY
            );
            slotX += 21;
        }
    }

    private static void row(
            GuiGraphics gui,
            Font font,
            String label,
            String value,
            int x,
            int y,
            int valueColor
    ) {
        String left = label + ":";
        int labelWidth = Math.max(34, font.width(left) + 5);
        gui.drawString(font, left, x, y, DIM, false);

        int available = PANEL_WIDTH - 20 - labelWidth;
        gui.drawString(
                font,
                fit(font, value, available),
                x + labelWidth,
                y,
                valueColor,
                false
        );
    }

    private static void miniBar(
            GuiGraphics gui,
            int x,
            int y,
            int segments,
            float fraction,
            int color
    ) {
        int filled = Mth.clamp(
                Mth.ceil(Mth.clamp(fraction, 0.0F, 1.0F) * segments),
                0,
                segments
        );

        for (int i = 0; i < segments; i++) {
            int px = x + i * 13;
            gui.fill(px, y, px + 11, y + 7, WELL_DARK);
            gui.fill(
                    px + 1,
                    y + 1,
                    px + 10,
                    y + 6,
                    i < filled ? color : 0x666666
            );
        }
    }

    private static void drawTinySlot(
            GuiGraphics gui,
            Font font,
            ItemStack stack,
            int x,
            int y
    ) {
        gui.fill(x, y, x + 19, y + 19, WELL_DARK);
        gui.fill(x + 1, y + 1, x + 18, y + 18, WELL);
        gui.fill(x + 1, y + 1, x + 18, y + 2, PANEL_HI);

        if (!stack.isEmpty()) {
            gui.renderItem(stack, x + 2, y + 2);
            gui.renderItemDecorations(font, stack, x + 2, y + 2);
        }
    }

    private static void drawFooter(
            GuiGraphics gui,
            Font font,
            int x,
            int y
    ) {
        int top = y + PANEL_HEIGHT - FOOTER_HEIGHT;
        int innerX = x + 6;
        int usable = PANEL_WIDTH - 12;
        int buttonWidth = usable / Page.values().length;

        for (int i = 0; i < Page.values().length; i++) {
            int bx = innerX + i * buttonWidth;
            boolean selected = i == currentTab;

            gui.fill(
                    bx,
                    top + 3,
                    bx + buttonWidth - 2,
                    y + PANEL_HEIGHT - 5,
                    WELL_DARK
            );
            gui.fill(
                    bx + 1,
                    top + 4,
                    bx + buttonWidth - 3,
                    y + PANEL_HEIGHT - 6,
                    selected ? 0xF0717171 : 0xF04B4B4B
            );

            String label = Page.values()[i].shortLabel;
            int tx = bx + (buttonWidth - 2 - font.width(label)) / 2;
            gui.drawString(
                    font,
                    label,
                    tx,
                    top + 6,
                    selected ? WHITE : 0xBEBEBE,
                    selected
            );
        }
    }

    private static void drawBodyWell(
            GuiGraphics gui,
            int x,
            int y
    ) {
        int left = x + 5;
        int top = y + BODY_TOP;
        int right = x + PANEL_WIDTH - 5;
        int bottom = y + BODY_BOTTOM;

        gui.fill(left, top, right, bottom, WELL_DARK);
        gui.fill(left + 1, top + 1, right - 1, bottom - 1, 0xE8AAAAAA);
    }

    private static void drawPanel(
            GuiGraphics gui,
            int x,
            int y,
            int width,
            int height
    ) {
        gui.fill(x, y, x + width, y + height, OUTLINE);
        gui.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL);
        gui.fill(x + 3, y + 3, x + width - 3, y + 4, PANEL_HI);
        gui.fill(x + 3, y + 3, x + 4, y + height - 3, PANEL_HI);
        gui.fill(
                x + 3,
                y + height - 4,
                x + width - 3,
                y + height - 3,
                PANEL_LO
        );
        gui.fill(
                x + width - 4,
                y + 3,
                x + width - 3,
                y + height - 3,
                PANEL_LO
        );
    }

    private static String fit(
            Font font,
            String text,
            int maxWidth
    ) {
        if (text == null || text.isBlank()) {
            return "-";
        }

        if (maxWidth <= 0) {
            return "";
        }

        if (font.width(text) <= maxWidth) {
            return text;
        }

        String dots = "..";
        int allowed = Math.max(0, maxWidth - font.width(dots));
        return font.plainSubstrByWidth(text, allowed) + dots;
    }

    private static boolean isWearingGlasses(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player
                .getItemBySlot(EquipmentSlot.HEAD)
                .is(ModItems.DEVELOPER_GLASSES.get());
    }

    private static CyberNpcEntity findLookedAtNpc(
            Minecraft minecraft
    ) {
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

        return hit != null
                && hit.getEntity() instanceof CyberNpcEntity npc
                ? npc
                : null;
    }

    private enum Page {
        INFO("Info", "INFO"),
        AI("AI", "AI"),
        SOCIAL("Social", "SOC"),
        DATA("Data", "DATA");

        private final String label;
        private final String shortLabel;

        Page(String label, String shortLabel) {
            this.label = label;
            this.shortLabel = shortLabel;
        }
    }

    private DeveloperGlassesOverlay() {
    }
}
