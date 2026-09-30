package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.client.render.CyberNpcRenderer;
import com.cyberspectraa.cybernpc.client.render.ZombieCyberNpcRenderer;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CYBER_NPC.get(), CyberNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.ZOMBIE_CYBER_NPC.get(), ZombieCyberNpcRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DeveloperGlassesOverlay.CYCLE_TAB);
    }

    private ClientModEvents() {
    }
}
