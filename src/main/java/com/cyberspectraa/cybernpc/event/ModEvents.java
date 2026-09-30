package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CYBER_NPC.get(), CyberNpcEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntities.CYBER_NPC.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CyberNpcEntity::checkCyberNpcSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }

    @SubscribeEvent
    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(ModItems.MAIN_NPC_SPAWN_EGG);
            event.accept(ModItems.QUEST_NPC_SPAWN_EGG);
            event.accept(ModItems.WILD_NPC_SPAWN_EGG);
        }

        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(ModItems.DEVELOPER_GLASSES);
        }
    }

    private ModEvents() {
    }
}
