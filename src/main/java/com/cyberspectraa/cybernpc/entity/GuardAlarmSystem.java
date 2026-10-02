package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public final class GuardAlarmSystem {
    private static final double ALERT_RADIUS = 72.0D;

    public static boolean ringBellAndAlert(
            ServerLevel level,
            BlockPos bellPos,
            @Nullable Entity ringer,
            @Nullable LivingEntity threat
    ) {
        BlockState state = level.getBlockState(bellPos);
        if (!(state.getBlock() instanceof BellBlock bell)) {
            return false;
        }

        boolean rang = bell.attemptToRing(
                ringer,
                level,
                bellPos,
                null
        );

        if (rang) {
            alertGuards(level, bellPos, threat);
        }

        return rang;
    }

    public static void alertGuards(
            ServerLevel level,
            BlockPos source,
            @Nullable LivingEntity threat
    ) {
        AABB area = new AABB(
                source.getX() - ALERT_RADIUS,
                source.getY() - 24.0D,
                source.getZ() - ALERT_RADIUS,
                source.getX() + ALERT_RADIUS + 1.0D,
                source.getY() + 24.0D,
                source.getZ() + ALERT_RADIUS + 1.0D
        );

        for (CyberNpcEntity guard : level.getEntitiesOfClass(
                CyberNpcEntity.class,
                area,
                candidate -> candidate.isAlive()
                        && NpcServiceRole.fromRole(candidate.getRole())
                        == NpcServiceRole.GUARD
        )) {
            guard.receiveGuardAlert(threat, source);
        }
    }

    private GuardAlarmSystem() {
    }
}
