package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.service.SpecialNpcSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SpecialNpcEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server =
                ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            SpecialNpcSavedData.get(server).tick(server);
        }
    }

    private SpecialNpcEvents() {
    }
}
