package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces exact vanilla minecraft:zombie spawns with CyberNpc's Wild zombie.
 *
 * Husks, Drowned, Zombie Villagers and other distinct zombie-family entities
 * are deliberately left alone.
 */
@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class VanillaZombieReplacementEvents {
    private VanillaZombieReplacementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getEntity().getType() != EntityType.ZOMBIE
                || !(event.getEntity() instanceof Zombie original)) {
            return;
        }

        ZombieCyberNpcEntity replacement =
                ModEntities.ZOMBIE_CYBER_NPC.get().create(level);

        if (replacement == null) {
            return;
        }

        /*
         * Copy the already-finalized vanilla zombie state so naturally rolled
         * armor, weapons, potion data, persistence and custom summon NBT are
         * retained. The new entity must get its own UUID.
         */
        CompoundTag tag = new CompoundTag();
        original.saveWithoutId(tag);
        tag.remove("UUID");
        tag.remove("UUIDMost");
        tag.remove("UUIDLeast");

        replacement.load(tag);
        replacement.moveTo(
                original.getX(),
                original.getY(),
                original.getZ(),
                original.getYRot(),
                original.getXRot()
        );
        replacement.setYHeadRot(original.getYHeadRot());

        replacement.randomizeNaturalWildAppearance();

        // Cancel the vanilla zombie before adding the replacement. The custom
        // entity type will re-fire this event but fails the exact type check.
        event.setCanceled(true);
        level.addFreshEntity(replacement);
    }
}
