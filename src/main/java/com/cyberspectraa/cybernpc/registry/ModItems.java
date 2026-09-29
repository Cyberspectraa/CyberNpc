package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CyberNpc.MOD_ID);

    public static final RegistryObject<Item> CYBER_NPC_SPAWN_EGG =
            ITEMS.register("cyber_npc_spawn_egg", () ->
                    new ForgeSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0x20252E,
                            0x2DC8D2,
                            new Item.Properties()
                    ));

    private ModItems() {
    }
}
