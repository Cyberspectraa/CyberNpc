package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.item.CyberNpcSpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CyberNpc.MOD_ID);

    public static final RegistryObject<Item> MAIN_NPC_SPAWN_EGG =
            ITEMS.register("main_npc_spawn_egg", () ->
                    new CyberNpcSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0x20252E,
                            0xE7C85A,
                            NpcType.MAIN,
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> QUEST_NPC_SPAWN_EGG =
            ITEMS.register("quest_npc_spawn_egg", () ->
                    new CyberNpcSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0x20252E,
                            0x5A8FE7,
                            NpcType.QUEST,
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> WILD_NPC_SPAWN_EGG =
            ITEMS.register("wild_npc_spawn_egg", () ->
                    new CyberNpcSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0x20252E,
                            0x6EBB6E,
                            NpcType.WILD,
                            new Item.Properties()
                    ));

    private ModItems() {
    }
}
