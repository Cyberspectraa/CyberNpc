package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CyberNpc.MOD_ID);

    public static final RegistryObject<EntityType<CyberNpcEntity>> CYBER_NPC =
            ENTITY_TYPES.register("cyber_npc", () ->
                    EntityType.Builder.of(CyberNpcEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build(new ResourceLocation(CyberNpc.MOD_ID, "cyber_npc").toString()));

    private ModEntities() {
    }
}
