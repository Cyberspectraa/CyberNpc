package com.cyberspectraa.season2arrival;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Tiny client fallback used only when Photon is not installed or its renderer throws.
 * It is intentionally cheaper than the original server-spammed beam.
 */
public final class ArrivalVanillaClientVfx {
    private ArrivalVanillaClientVfx() {
    }

    public static void play(ArrivalVfxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        double x = packet.x();
        double y = packet.y();
        double z = packet.z();

        for (int i = 0; i < 56; i++) {
            double angle = Math.PI * 2.0D * i / 56.0D;
            double radius = 2.6D;
            minecraft.level.addParticle(
                    ParticleTypes.END_ROD,
                    x + Math.cos(angle) * radius,
                    y + 0.08D,
                    z + Math.sin(angle) * radius,
                    0.0D, 0.01D, 0.0D
            );
        }

        for (double py = y; py <= packet.maxBuildY(); py += 3.5D) {
            minecraft.level.addParticle(
                    ParticleTypes.END_ROD,
                    true,
                    x, py, z,
                    0.0D, 0.035D, 0.0D
            );
        }

        for (int i = 0; i < 60; i++) {
            double vx = (minecraft.level.random.nextDouble() - 0.5D) * 0.18D;
            double vy = minecraft.level.random.nextDouble() * 0.20D;
            double vz = (minecraft.level.random.nextDouble() - 0.5D) * 0.18D;
            minecraft.level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    x, y + 1.0D, z,
                    vx, vy, vz
            );
        }
    }
}
