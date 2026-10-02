package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.item.AddressedLetterItem;
import com.cyberspectraa.cybernpc.item.CyberNpcSpawnEggItem;
import com.cyberspectraa.cybernpc.item.DeveloperGlassesItem;
import com.cyberspectraa.cybernpc.item.LetterPaperItem;
import com.cyberspectraa.cybernpc.item.OpenedLetterItem;
import com.cyberspectraa.cybernpc.item.SealedLetterItem;
import net.minecraft.world.item.BlockItem;
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

    public static final RegistryObject<Item> BANKER_NPC_SPAWN_EGG =
            ITEMS.register("banker_npc_spawn_egg", () ->
                    new CyberNpcSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0x2C261A,
                            0xD9B84E,
                            NpcType.MAIN,
                            "Banker",
                            false,
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> COURIER_NPC_SPAWN_EGG =
            ITEMS.register("courier_npc_spawn_egg", () ->
                    new CyberNpcSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0x7B1F1F,
                            0xE8D6B3,
                            NpcType.MAIN,
                            "Courier",
                            false,
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> COPPER_COIN =
            ITEMS.register("copper_coin", () ->
                    new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> SILVER_COIN =
            ITEMS.register("silver_coin", () ->
                    new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> GOLD_COIN =
            ITEMS.register("gold_coin", () ->
                    new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> PLATINUM_COIN =
            ITEMS.register("platinum_coin", () ->
                    new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> DRAGON_COIN =
            ITEMS.register("dragon_coin", () ->
                    new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> LETTER_PAPER =
            ITEMS.register("letter_paper", () ->
                    new LetterPaperItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ADDRESSED_LETTER =
            ITEMS.register("addressed_letter", () ->
                    new AddressedLetterItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SEALED_LETTER =
            ITEMS.register("sealed_letter", () ->
                    new SealedLetterItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> OPENED_LETTER =
            ITEMS.register("opened_letter", () ->
                    new OpenedLetterItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DROP_BOX =
            ITEMS.register("drop_box", () ->
                    new BlockItem(
                            ModBlocks.DROP_BOX.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> LETTER_BOX =
            ITEMS.register("letter_box", () ->
                    new BlockItem(
                            ModBlocks.LETTER_BOX.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> DEVELOPER_GLASSES =
            ITEMS.register("developer_glasses", () ->
                    new DeveloperGlassesItem(new Item.Properties()));

    private ModItems() {
    }
}
