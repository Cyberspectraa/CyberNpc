package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.item.TownRegisterItem;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.WorkplaceGroupTogglePacket;
import com.cyberspectraa.cybernpc.network.WorkplaceRemovePacket;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Client-side keybinding, routed to server for authoritative register state. */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorkplaceMultiselectKey {
    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.cybernpc.group_workstations",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.cybernpc");

    public static final KeyMapping REMOVE = new KeyMapping(
            "key.cybernpc.remove_workstation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.cybernpc");

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (TOGGLE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null
                    && minecraft.player.getMainHandItem().getItem() instanceof TownRegisterItem
                    && TownRegisterItem.isWorkMode(minecraft.player.getMainHandItem())) {
                CyberNpcNetwork.CHANNEL.sendToServer(new WorkplaceGroupTogglePacket());
            }
        }
        while (REMOVE.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null
                    && minecraft.player.getMainHandItem().getItem() instanceof TownRegisterItem
                    && TownRegisterItem.isWorkMode(minecraft.player.getMainHandItem())
                    && minecraft.hitResult instanceof BlockHitResult) {
                CyberNpcNetwork.CHANNEL.sendToServer(new WorkplaceRemovePacket());
            }
        }
    }
    private WorkplaceMultiselectKey() {}
}
