package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeveloperGlassesOverlay {
    private static final double DEBUG_RANGE = 64.0D;

    @SubscribeEvent
    public static void renderOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.level == null
                || !minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.DEVELOPER_GLASSES.get())) {
            return;
        }

        CyberNpcEntity npc = findLookedAtNpc(minecraft);
        GuiGraphics gui = event.getGuiGraphics();

        int x = 8;
        int y = 8;

        if (npc == null) {
            gui.fill(x - 4, y - 4, x + 202, y + 19, 0xA0101010);
            gui.drawString(minecraft.font, "CyberNpc Dev Glasses", x, y, 0x55FFFF, true);
            gui.drawString(minecraft.font, "Look at an NPC to inspect its AI.", x, y + 10, 0xDDDDDD, false);
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("CyberNpc Dev Glasses");
        lines.add(npc.getName().getString() + "  [" + npc.getNpcType().serializedName() + "]");
        lines.add(String.format("Health: %.1f / %.1f   Hunger: %d / 20", npc.getHealth(), npc.getMaxHealth(), npc.getHunger()));
        lines.add("Role: " + npc.getRole() + "   Aggression: " + npc.getAggressionLevel());
        lines.add("Activity: " + npc.getDebugActivity());
        lines.add("Target: " + npc.getDebugTarget());
        lines.add("Path: " + npc.getDebugPath());
        lines.add("Claims: " + npc.getDebugClaims());
        lines.add("Inventory: " + npc.getDebugInventory());

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }

        int height = lines.size() * 10 + 8;
        gui.fill(x - 4, y - 4, x + width + 6, y + height, 0xB0101010);

        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0x55FFFF : (i == 4 ? 0xFFFF55 : 0xFFFFFF);
            gui.drawString(minecraft.font, lines.get(i), x, y + i * 10, color, true);
        }
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

    private DeveloperGlassesOverlay() {
    }
}
