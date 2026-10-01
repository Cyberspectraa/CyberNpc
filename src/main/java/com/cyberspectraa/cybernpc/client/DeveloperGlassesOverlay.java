package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcAppearance;
import com.cyberspectraa.cybernpc.entity.WildNpcClass;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ProjectileUtil;
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

@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeveloperGlassesOverlay {
    private static final double DEBUG_RANGE = 64.0D;
    private static final int PANEL_WIDTH = 360;
    private static final int TEXT_WIDTH = PANEL_WIDTH - 18;

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

        CyberNpcEntity npc = findLookedAtNpc(minecraft);
        GuiGraphics gui = event.getGuiGraphics();
        Font font = minecraft.font;

        int x = 8;
        int y = 8;

        if (npc == null) {
            int height = 39;
            drawPanel(gui, x, y, PANEL_WIDTH, height);
            gui.drawString(font, "CYBERNPC // DEV GLASSES", x + 8, y + 7, 0x55FFFF, true);
            gui.drawString(font, "Look at a Wild NPC to inspect it.", x + 8, y + 20, 0xE0E0E0, false);
            gui.drawString(
                    font,
                    keyHint() + "  Cycle tab",
                    x + 8,
                    y + 29,
                    0x9A9A9A,
                    false
            );
            return;
        }

        Tab tab = Tab.values()[currentTab];
        List<DebugLine> lines = buildLines(npc, tab);

        int headerHeight = 41;
        int bodyHeight = Math.max(46, lines.size() * 11 + 10);
        int footerHeight = 16;
        int height = headerHeight + bodyHeight + footerHeight;

        drawPanel(gui, x, y, PANEL_WIDTH, height);

        gui.drawString(font, "CYBERNPC // " + tab.displayName.toUpperCase(), x + 8, y + 7, 0x55FFFF, true);
        gui.drawString(
                font,
                trim(font, npc.getName().getString(), TEXT_WIDTH),
                x + 8,
                y + 19,
                0xFFFFFF,
                true
        );

        drawTabs(gui, font, x + 8, y + 31);

        int lineY = y + headerHeight + 4;
        for (DebugLine line : lines) {
            gui.drawString(
                    font,
                    trim(font, line.text, TEXT_WIDTH),
                    x + 8,
                    lineY,
                    line.color,
                    false
            );
            lineY += 11;
        }

        gui.fill(x + 6, y + height - footerHeight, x + PANEL_WIDTH - 6, y + height - footerHeight + 1, 0x553C3C3C);
        gui.drawString(
                font,
                keyHint() + "  Next tab",
                x + 8,
                y + height - 11,
                0x9A9A9A,
                false
        );
    }

    private static List<DebugLine> buildLines(CyberNpcEntity npc, Tab tab) {
        List<DebugLine> lines = new ArrayList<>();

        switch (tab) {
            case OVERVIEW -> {
                lines.add(new DebugLine(
                        "Class: " + npc.getWildClassDisplayName()
                                + "    Personality: " + npc.getPersonalityDisplayName(),
                        classColor(npc.getWildClass())
                ));
                lines.add(new DebugLine(
                        "Gear: " + npc.getGearTierDisplayName()
                                + (npc.getWildClass().hasMagicSchool()
                                ? "    School: " + npc.getMageSchoolDisplayName()
                                : ""),
                        0xD7E8FF
                ));
                lines.add(new DebugLine(
                        String.format("Health: %.1f / %.1f", npc.getHealth(), npc.getMaxHealth()),
                        healthColor(npc)
                ));
                lines.add(new DebugLine(
                        "Hunger: " + npc.getHunger() + " / 20",
                        npc.getHunger() <= 6 ? 0xFF7777 : 0xE8E8E8
                ));
                lines.add(new DebugLine("Activity: " + npc.getDebugActivity(), 0xFFFF77));
                lines.add(new DebugLine("Why: " + npc.getDebugReason(), 0xB8D8FF));
                lines.add(new DebugLine(
                        npc.isZombifying()
                                ? String.format("Zombification: %.0f%%", npc.getZombificationProgress() * 100.0F)
                                : "Zombification: none",
                        npc.isZombifying() ? 0x7FCB65 : 0xB8B8B8
                ));
            }
            case APPEARANCE -> {
                lines.add(new DebugLine(
                        "Gender: " + npc.getAppearanceGender().displayName()
                                + "    Model: "
                                + (npc.isSlimModel() ? "Slim / Alex" : "Classic / Steve"),
                        0xF5D7FF
                ));
                lines.add(new DebugLine(
                        "Skin tone: " + npc.getSkinToneDisplayName(),
                        0xFFD7C2
                ));
                lines.add(new DebugLine(
                        "Eyes: " + npc.getEyeStyleDisplayName(),
                        0xBFE8FF
                ));
                lines.add(new DebugLine(
                        "Hair: " + npc.getHairStyleDisplayName(),
                        0xE1BC88
                ));
                lines.add(new DebugLine(
                        "Hair colour: " + NpcAppearance.hairColorDisplayName(),
                        0xE1BC88
                ));
                lines.add(new DebugLine(
                        "Appearance ID: "
                                + npc.getAppearanceGender().serializedName()
                                + "-" + npc.getSkinToneIndex()
                                + "-" + npc.getEyeStyleIndex()
                                + "-" + npc.getHairStyleIndex(),
                        0x8F8F8F
                ));
            }
            case RELATIONSHIPS -> {
                lines.add(new DebugLine(
                        "Party: " + npc.getDebugParty(),
                        0xB9E6FF
                ));

                String social = npc.getDebugRelationships();
                if (social == null
                        || social.isBlank()
                        || "none".equals(social)) {
                    lines.add(new DebugLine(
                            "No meaningful relationships recorded yet.",
                            0x8F8F8F
                    ));
                } else {
                    for (String relation : social.split("\\|")) {
                        int color = relation.startsWith("Player ")
                                ? 0xFFD98A
                                : 0xD8C5FF;
                        lines.add(new DebugLine(relation, color));
                    }
                }
            }
            case COMBAT -> {
                lines.add(new DebugLine(
                        "Style: " + combatStyle(npc.getWildClass()),
                        classColor(npc.getWildClass())
                ));
                lines.add(new DebugLine("Personality: " + npc.getPersonalityDisplayName(), 0xDADADA));
                lines.add(new DebugLine(
                        "Gear: " + npc.getGearTierDisplayName()
                                + (npc.getWildClass().hasMagicSchool()
                                ? "    School: " + npc.getMageSchoolDisplayName()
                                : ""),
                        0xD7E8FF
                ));
                lines.add(new DebugLine(
                        npc.isSpellCastingVisual()
                                ? "Casting: " + npc.getCastingSpellId()
                                : "Casting: none",
                        npc.isSpellCastingVisual() ? 0xFF8EF3 : 0x8F8F8F
                ));
                lines.add(new DebugLine("Aggression: " + npc.getAggressionLevel() + " / 80", 0xE8E8E8));
                lines.add(new DebugLine("Target: " + npc.getDebugTarget(), 0xFFB866));
                lines.add(new DebugLine("Confidence: " + npc.getDebugConfidence(), 0x9FE3FF));
                lines.add(new DebugLine("State: " + npc.getDebugActivity(), 0xFFFF77));
                lines.add(new DebugLine(
                        String.format("Health: %.1f / %.1f", npc.getHealth(), npc.getMaxHealth()),
                        healthColor(npc)
                ));
            }
            case SPELLS -> {
                if (!npc.getWildClass().usesSpellBook()) {
                    lines.add(new DebugLine("This NPC has no spellbook.", 0x8F8F8F));
                } else {
                    lines.add(new DebugLine(
                            "School: " + npc.getMageSchoolDisplayName(),
                            0xFF8EF3
                    ));
                    String spellDebug = npc.getDebugSpells();
                    if (spellDebug == null || spellDebug.isBlank()) {
                        lines.add(new DebugLine("No spell data.", 0x8F8F8F));
                    } else {
                        for (String spell : spellDebug.split("\\|")) {
                            lines.add(new DebugLine(spell, 0xD7E8FF));
                        }
                    }
                }
            }
            case SURVIVAL -> {
                lines.add(new DebugLine("Hunger: " + npc.getHunger() + " / 20", 0xE8E8E8));
                lines.add(new DebugLine("Inventory: " + npc.getDebugInventory(), 0xD7E8FF));
                lines.add(new DebugLine("Claims: " + npc.getDebugClaims(), 0xB9E6B9));
                lines.add(new DebugLine("Current task: " + npc.getDebugActivity(), 0xFFFF77));
            }
            case DEBUG -> {
                lines.add(new DebugLine("Activity: " + npc.getDebugActivity(), 0xFFFF77));
                lines.add(new DebugLine("Why: " + npc.getDebugReason(), 0xB8D8FF));
                lines.add(new DebugLine("Target: " + npc.getDebugTarget(), 0xFFB866));
                lines.add(new DebugLine("Path: " + npc.getDebugPath(), 0xB7D7FF));
                lines.add(new DebugLine("Claims: " + npc.getDebugClaims(), 0xB9E6B9));
                lines.add(new DebugLine("Inventory: " + npc.getDebugInventory(), 0xD7E8FF));
                lines.add(new DebugLine(
                        "Class: " + npc.getWildClass().serializedName()
                                + " | personality:" + npc.getPersonality().serializedName()
                                + " | gear:" + npc.getGearTier().serializedName()
                                + (npc.getWildClass().hasMagicSchool()
                                ? " | school:" + npc.getMageSchool().serializedName()
                                : ""),
                        0x8F8F8F
                ));
            }
        }

        return lines;
    }

    private static void drawPanel(GuiGraphics gui, int x, int y, int width, int height) {
        gui.fill(x, y, x + width, y + height, 0xD20B0F14);
        gui.fill(x, y, x + width, y + 2, 0xFF35DDE8);
        gui.fill(x, y, x + 2, y + height, 0x8835DDE8);
        gui.fill(x + width - 2, y, x + width, y + height, 0x4435DDE8);
    }

    private static void drawTabs(GuiGraphics gui, Font font, int x, int y) {
        int cursor = x;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            boolean active = i == currentTab;
            int color = active ? 0x55FFFF : 0x858585;

            gui.drawString(font, tab.displayName, cursor, y, color, active);
            cursor += font.width(tab.displayName) + 12;
        }
    }

    private static String combatStyle(WildNpcClass npcClass) {
        return switch (npcClass) {
            case ARCHER -> "Ranged control / kiting";
            case KNIGHT -> "Armored committed melee";
            case ROGUE -> "Fast flanking melee";
            case BERSERKER -> "Heavy axe pressure / low-health rage";
            case BEAST_TAMER -> "Melee + coordinated wolf pack";
            case HORSE_TAMER -> "Mounted melee / horse handling";
            case MAGE -> "Iron's Spells caster / distance control";
            case CLERIC -> "Holy support / healing + ranged pressure";
            case SPELLBLADE -> "Sword + spell hybrid";
            default -> "Mixed sword + ranged";
        };
    }

    private static int classColor(WildNpcClass npcClass) {
        return switch (npcClass) {
            case ARCHER -> 0x7FD67F;
            case KNIGHT -> 0x8DB9FF;
            case ROGUE -> 0xD39BFF;
            case BERSERKER -> 0xFF7A6E;
            case BEAST_TAMER -> 0xE0B36A;
            case HORSE_TAMER -> 0xC99A68;
            case MAGE -> 0xFF8EF3;
            case CLERIC -> 0xFFF0A6;
            case SPELLBLADE -> 0x9FD8FF;
            default -> 0xDADADA;
        };
    }

    private static int healthColor(CyberNpcEntity npc) {
        float fraction = npc.getMaxHealth() <= 0.0F
                ? 0.0F
                : npc.getHealth() / npc.getMaxHealth();

        if (fraction <= 0.30F) {
            return 0xFF6868;
        }
        if (fraction <= 0.60F) {
            return 0xFFD36A;
        }
        return 0x7CFF8A;
    }

    private static String keyHint() {
        return "[" + CYCLE_TAB.getTranslatedKeyMessage().getString() + "]";
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int allowed = Math.max(0, maxWidth - font.width(ellipsis));
        return font.plainSubstrByWidth(text, allowed) + ellipsis;
    }

    private static boolean isWearingGlasses(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DEVELOPER_GLASSES.get());
    }

    private static CyberNpcEntity findLookedAtNpc(Minecraft minecraft) {
        Vec3 start = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(DEBUG_RANGE));

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                start,
                end,
                minecraft.player.getBoundingBox().expandTowards(look.scale(DEBUG_RANGE)).inflate(1.0D),
                entity -> entity instanceof CyberNpcEntity,
                DEBUG_RANGE * DEBUG_RANGE
        );

        return hit != null && hit.getEntity() instanceof CyberNpcEntity npc ? npc : null;
    }

    private enum Tab {
        OVERVIEW("Overview"),
        APPEARANCE("Appearance"),
        RELATIONSHIPS("Relationships"),
        COMBAT("Combat"),
        SPELLS("Spells"),
        SURVIVAL("Survival"),
        DEBUG("Debug");

        private final String displayName;

        Tab(String displayName) {
            this.displayName = displayName;
        }
    }

    private record DebugLine(String text, int color) {
    }

    private DeveloperGlassesOverlay() {
    }
}
