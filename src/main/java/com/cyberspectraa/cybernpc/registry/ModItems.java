package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.building.BuildingMarkerType;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.item.AddressedLetterItem;
import com.cyberspectraa.cybernpc.item.BuildingMarkerItem;
import com.cyberspectraa.cybernpc.item.BuildingPlannerItem;
import com.cyberspectraa.cybernpc.item.CyberNpcSpawnEggItem;
import com.cyberspectraa.cybernpc.item.DeveloperGlassesItem;
import com.cyberspectraa.cybernpc.item.LetterPaperItem;
import com.cyberspectraa.cybernpc.item.GuardPostMarkerItem;
import com.cyberspectraa.cybernpc.item.OpenedLetterItem;
import com.cyberspectraa.cybernpc.item.SealedLetterItem;
import com.cyberspectraa.cybernpc.item.SpecialNpcRemovalStickItem;
import com.cyberspectraa.cybernpc.item.TownRegisterItem;
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

    public static final RegistryObject<Item> GUARD_NPC_SPAWN_EGG =
            ITEMS.register("guard_npc_spawn_egg", () ->
                    new CyberNpcSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0x34495E,
                            0xD7D7D7,
                            NpcType.MAIN,
                            "Guard",
                            false,
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> POPE_NPC_SPAWN_EGG =
            ITEMS.register("pope_npc_spawn_egg", () ->
                    new CyberNpcSpawnEggItem(
                            ModEntities.CYBER_NPC,
                            0xF3F3F3,
                            0xD4AF37,
                            NpcType.MAIN,
                            "Pope",
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

    public static final RegistryObject<Item> GUARD_POST =
            ITEMS.register("guard_post", () ->
                    new GuardPostMarkerItem(
                            new Item.Properties().stacksTo(64)
                    ));

    public static final RegistryObject<Item> TOWN_REGISTER =
            ITEMS.register("town_register", () ->
                    new TownRegisterItem(
                            new Item.Properties().stacksTo(1)
                    ));

    // Legacy 0.39.x planner is kept registered so existing worlds/items
    // load safely, but normal building setup now uses item-frame markers.
    public static final RegistryObject<Item> BUILDING_PLANNER =
            ITEMS.register("building_planner", () ->
                    new BuildingPlannerItem(
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> HOME_MARKER =
            marker("home_marker", BuildingMarkerType.HOME);
    public static final RegistryObject<Item> SHOP_MARKER =
            marker("shop_marker", BuildingMarkerType.SHOP);
    public static final RegistryObject<Item> CHURCH_MARKER =
            marker("church_marker", BuildingMarkerType.CHURCH);
    public static final RegistryObject<Item> BANK_MARKER =
            marker("bank_marker", BuildingMarkerType.BANK);
    public static final RegistryObject<Item> POST_OFFICE_MARKER =
            marker("post_office_marker", BuildingMarkerType.POST_OFFICE);
    public static final RegistryObject<Item> INN_MARKER =
            marker("inn_marker", BuildingMarkerType.INN);

    public static final RegistryObject<Item> STAFF_ONLY_MARKER =
            marker("staff_only_marker", BuildingMarkerType.STAFF_ONLY);
    public static final RegistryObject<Item> BEDROOM_MARKER =
            marker("bedroom_marker", BuildingMarkerType.BEDROOM);
    public static final RegistryObject<Item> ALTAR_MARKER =
            marker("altar_marker", BuildingMarkerType.ALTAR);
    public static final RegistryObject<Item> COUNTER_MARKER =
            marker("counter_marker", BuildingMarkerType.COUNTER);

    public static final RegistryObject<Item> SPECIAL_NPC_REMOVAL_STICK =
            ITEMS.register("special_npc_removal_stick", () ->
                    new SpecialNpcRemovalStickItem(
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> DEVELOPER_GLASSES =
            ITEMS.register("developer_glasses", () ->
                    new DeveloperGlassesItem(new Item.Properties()));

    private static RegistryObject<Item> marker(
            String name,
            BuildingMarkerType type
    ) {
        return ITEMS.register(
                name,
                () -> new BuildingMarkerItem(
                        type,
                        new Item.Properties().stacksTo(16)
                )
        );
    }

    private ModItems() {
    }
}
