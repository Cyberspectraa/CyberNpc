package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.registry.ModEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HostileMobEvents {
    private static final double DEFAULT_NOTICE_RANGE = 32.0D;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || mob instanceof CyberNpcEntity
                || mob.level().isClientSide
                || mob.getType() == EntityType.WARDEN
                || !(mob instanceof Enemy)
                || !mob.isAlive()
                || mob.tickCount % 10 != 0) {
            return;
        }

        LivingEntity current = mob.getTarget();

        if (current instanceof CyberNpcEntity infected
                && infected.hasEffect(ModEffects.ZOMBIFICATION.get())) {
            mob.setTarget(null);
            current = null;
        }

        if (current != null && current.isAlive()) {
            return;
        }

        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = DEFAULT_NOTICE_RANGE;
        }

        if (mob instanceof EnderMan enderman) {
            CyberNpcEntity staredBy = findNpcStaringAtEnderman(enderman, followRange);
            if (staredBy != null) {
                enderman.setBeingStaredAt();
                enderman.setPersistentAngerTarget(staredBy.getUUID());
                enderman.startPersistentAngerTimer();
                enderman.setTarget(staredBy);
            }
            return;
        }

        AABB search = mob.getBoundingBox().inflate(followRange, Math.min(16.0D, followRange), followRange);
        List<CyberNpcEntity> candidates = mob.level().getEntitiesOfClass(
                CyberNpcEntity.class,
                search,
                npc -> npc.isAlive()
                        && !npc.isSpectator()
                        && !npc.hasEffect(ModEffects.ZOMBIFICATION.get())
        );

        CyberNpcEntity nearest = candidates.stream()
                .filter(mob.getSensing()::hasLineOfSight)
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);

        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private static CyberNpcEntity findNpcStaringAtEnderman(EnderMan enderman, double range) {
        AABB search = enderman.getBoundingBox().inflate(range, Math.min(20.0D, range), range);

        return enderman.level().getEntitiesOfClass(
                        CyberNpcEntity.class,
                        search,
                        npc -> npc.isAlive()
                                && !npc.isSleeping()
                                && !npc.hasEffect(ModEffects.ZOMBIFICATION.get())
                ).stream()
                .filter(npc -> isLookingAt(npc, enderman))
                .min(Comparator.comparingDouble(enderman::distanceToSqr))
                .orElse(null);
    }

    private static boolean isLookingAt(CyberNpcEntity npc, EnderMan enderman) {
        Vec3 view = npc.getViewVector(1.0F).normalize();
        Vec3 toEnderman = new Vec3(
                enderman.getX() - npc.getX(),
                enderman.getEyeY() - npc.getEyeY(),
                enderman.getZ() - npc.getZ()
        );

        double distance = toEnderman.length();
        if (distance < 0.0001D) {
            return true;
        }

        Vec3 direction = toEnderman.normalize();
        double dot = view.dot(direction);
        double threshold = 1.0D - 0.025D / distance;

        return dot > Mth.clamp(threshold, -1.0D, 1.0D)
                && npc.hasLineOfSight(enderman);
    }

    private HostileMobEvents() {
    }
}
