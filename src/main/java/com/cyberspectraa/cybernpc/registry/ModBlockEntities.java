package com.cyberspectraa.cybernpc.registry;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.rift.RiftBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CyberNpc.MOD_ID);

    public static final RegistryObject<BlockEntityType<RiftBlockEntity>> ISLAND_RIFT =
            BLOCK_ENTITIES.register("island_rift", () ->
                    BlockEntityType.Builder.of(
                            RiftBlockEntity::new, ModBlocks.ISLAND_RIFT.get()).build(null));

    private ModBlockEntities() {}
}
