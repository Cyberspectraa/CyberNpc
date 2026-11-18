package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.registry.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Render the irregular PNG silhouette as a transparent Minecraft cutout,
 * with Photon supplying all dynamic visual effects.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RiftClientRenderSetup {
    private RiftClientRenderSetup() {}

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                ItemBlockRenderTypes.setRenderLayer(
                        ModBlocks.ISLAND_RIFT.get(), RenderType.cutout()));
    }
}
