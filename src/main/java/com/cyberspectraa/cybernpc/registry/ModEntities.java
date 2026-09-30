package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
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
                            .sized(0.6F, 1.75F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build(new ResourceLocation(CyberNpc.MOD_ID, "cyber_npc").toString()));

    public static final RegistryObject<EntityType<ZombieCyberNpcEntity>> ZOMBIE_CYBER_NPC =
            ENTITY_TYPES.register("zombie_cyber_npc", () ->
                    EntityType.Builder.of(ZombieCyberNpcEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.75F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build(new ResourceLocation(CyberNpc.MOD_ID, "zombie_cyber_npc").toString()));

    private ModEntities() {
    }
}
