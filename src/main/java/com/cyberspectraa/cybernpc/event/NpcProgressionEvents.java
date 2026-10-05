package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.compat.CyberProgressionCompat;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = CyberNpc.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class NpcProgressionEvents {
    private NpcProgressionEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide
                || !(event.getSource().getEntity() instanceof CyberNpcEntity npc)
                || npc.getNpcType() != NpcType.WILD
                || !npc.isAlive()) {
            return;
        }

        CyberProgressionCompat.awardCombatKill(
            npc,
            event.getEntity()
        );

        // If this kill crossed Cyber Level 20, resolve the same shared
        // advancement tree a player would use. CyberNpc only chooses when;
        // CyberClasses remains the source of truth for available paths.
        npc.ensureWildClassAdvancement();
    }
}
