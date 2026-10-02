package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    CyberNpc.MOD_ID
            );

    public static final RegistryObject<CreativeModeTab> CYBERNPC_TAB =
            TABS.register("cybernpc", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "itemGroup.cybernpc"
                            ))
                            .icon(() ->
                                    ModItems.WILD_NPC_SPAWN_EGG
                                            .get()
                                            .getDefaultInstance()
                            )
                            .displayItems((parameters, output) -> {
                                // NPCs first: the original three plus specific
                                // service-role eggs.
                                output.accept(ModItems.MAIN_NPC_SPAWN_EGG.get());
                                output.accept(ModItems.QUEST_NPC_SPAWN_EGG.get());
                                output.accept(ModItems.WILD_NPC_SPAWN_EGG.get());
                                output.accept(ModItems.BANKER_NPC_SPAWN_EGG.get());
                                output.accept(ModItems.COURIER_NPC_SPAWN_EGG.get());

                                // Economy.
                                output.accept(ModItems.COPPER_COIN.get());
                                output.accept(ModItems.SILVER_COIN.get());
                                output.accept(ModItems.GOLD_COIN.get());
                                output.accept(ModItems.PLATINUM_COIN.get());
                                output.accept(ModItems.DRAGON_COIN.get());

                                // Postal system.
                                output.accept(ModItems.LETTER_PAPER.get());
                                output.accept(ModItems.ADDRESSED_LETTER.get());
                                output.accept(ModItems.SEALED_LETTER.get());
                                output.accept(ModItems.OPENED_LETTER.get());
                                output.accept(ModItems.DROP_BOX.get());
                                output.accept(ModItems.LETTER_BOX.get());

                                // Special-NPC and developer utilities last.
                                output.accept(ModItems.TOWN_REGISTER.get());
                                output.accept(ModItems.DEVELOPER_GLASSES.get());
                            })
                            .build()
            );

    private ModCreativeTabs() {
    }
}
