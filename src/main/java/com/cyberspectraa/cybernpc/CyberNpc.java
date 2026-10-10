package com.cyberspectraa.cybernpc;

import com.cyberspectraa.cybernpc.command.CyberNpcCommands;
import com.cyberspectraa.cybernpc.intro.CyberIntroService;
import net.minecraft.commands.Commands;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.registry.ModBlocks;
import com.cyberspectraa.cybernpc.registry.ModCreativeTabs;
import com.cyberspectraa.cybernpc.registry.ModEffects;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.cyberspectraa.cybernpc.registry.ModSounds;
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
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        CyberNpcNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(CyberNpcCommands::register);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) ->
                event.getDispatcher().register(
                    Commands.literal("cyberintro")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("setarrival").executes(
                                ctx -> CyberIntroService.setArrival(ctx.getSource())))
                        .then(Commands.literal("setpopewait").executes(
                                ctx -> CyberIntroService.setPopeWait(ctx.getSource())))
                ));
    }
}
