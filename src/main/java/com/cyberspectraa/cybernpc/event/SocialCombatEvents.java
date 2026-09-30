package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SocialCombatEvents {
    private static final double HELP_NOTICE_RADIUS = 24.0D;

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide
                || event.getEntity() instanceof CyberNpcEntity
                || !(event.getSource().getEntity() instanceof Player player)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        rewardPlayerHelp(player, event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide
                || event.getEntity() instanceof CyberNpcEntity
                || !(event.getSource().getEntity() instanceof Player player)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        rewardPlayerHelp(player, event.getEntity(), true);
    }

    private static void rewardPlayerHelp(
            Player player,
            LivingEntity threat,
            boolean killed
    ) {
        AABB search = threat.getBoundingBox().inflate(HELP_NOTICE_RADIUS);

        for (CyberNpcEntity npc : threat.level().getEntitiesOfClass(
                CyberNpcEntity.class,
                search,
                candidate -> candidate.isAlive()
                        && candidate.isTrackingThreat(threat)
        )) {
            npc.recordPlayerCombatHelp(player, threat, killed);
        }
    }

    private SocialCombatEvents() {
    }
}
