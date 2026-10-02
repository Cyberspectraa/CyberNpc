package com.cyberspectraa.cybernpc.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Soft CyberServer bridge. CyberNpc can still load without CyberServer, while
 * using CyberServer's real configured arrival spawn and summon effect whenever
 * it is installed.
 */
public final class CyberServerCompat {
    private static final String MOD_ID = "cyberserver";

    private CyberServerCompat() {
    }

    public static ArrivalTarget resolveArrival(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BlockPos fallback = overworld.getSharedSpawnPos();
        float fallbackYaw = overworld.getSharedSpawnAngle();

        if (!ModList.get().isLoaded(MOD_ID)) {
            return new ArrivalTarget(overworld, fallback, fallbackYaw, false);
        }

        try {
            Class<?> dataClass = Class.forName(
                    "com.cyberspectraa.cyberserver.CyberServerSavedData"
            );
            Method get = dataClass.getMethod("get", MinecraftServer.class);
            Object data = get.invoke(null, server);

            boolean configured = (boolean) dataClass
                    .getMethod("isSpawnConfigured")
                    .invoke(data);

            if (!configured) {
                return new ArrivalTarget(
                        overworld,
                        fallback,
                        fallbackYaw,
                        false
                );
            }

            BlockPos pos = (BlockPos) dataClass
                    .getMethod("getSpawnPos")
                    .invoke(data);
            float yaw = (float) dataClass
                    .getMethod("getSpawnYaw")
                    .invoke(data);

            return new ArrivalTarget(overworld, pos, yaw, true);
        } catch (Throwable ignored) {
            return new ArrivalTarget(overworld, fallback, fallbackYaw, false);
        }
    }

    public static boolean startArrival(
            ServerLevel level,
            double x,
            double y,
            double z
    ) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }

        try {
            Class<?> eventsClass = Class.forName(
                    "com.cyberspectraa.cyberserver.CyberServerEvents"
            );
            Method startArrival = eventsClass.getMethod(
                    "startArrival",
                    ServerLevel.class,
                    double.class,
                    double.class,
                    double.class
            );
            startArrival.invoke(null, level, x, y, z);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public record ArrivalTarget(
            ServerLevel level,
            BlockPos pos,
            float yaw,
            boolean cyberServerConfigured
    ) {
    }
}
