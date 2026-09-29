package com.cyberspectraa.cybernpc;

import com.cyberspectraa.cybernpc.command.CyberNpcCommands;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CyberNpc.MOD_ID)
public final class CyberNpc {
    public static final String MOD_ID = "cybernpc";

    public CyberNpc() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(CyberNpcCommands::register);
    }
}
