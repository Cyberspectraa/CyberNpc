package com.cyberspectraa.season2arrival;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ArrivalEvents {
    private static final List<ArrivalSequence> ACTIVE_SEQUENCES = new ArrayList<>();

    private static final DustParticleOptions GOLD =
            new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.28F), 1.6F);
    private static final DustParticleOptions PALE_GOLD =
            new DustParticleOptions(new Vector3f(1.0F, 0.96F, 0.72F), 1.25F);

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ArrivalCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ArrivalSavedData data = ArrivalSavedData.get(server);
        if (!data.isSpawnConfigured() || data.hasArrived(player.getUUID())) {
            return;
        }

        // Mark first so a reconnect/crash cannot replay the one-time arrival automatically.
        data.markArrived(player.getUUID());

        ServerLevel level = server.overworld();
        BlockPos spawn = data.getSpawnPos();
        double x = spawn.getX() + 0.5D;
        double y = spawn.getY();
        double z = spawn.getZ() + 0.5D;

        player.teleportTo(level, x, y, z, data.getSpawnYaw(), 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;

        startSequence(level, x, y, z);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE_SEQUENCES.isEmpty()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            ACTIVE_SEQUENCES.clear();
            return;
        }

        Iterator<ArrivalSequence> iterator = ACTIVE_SEQUENCES.iterator();
        while (iterator.hasNext()) {
            ArrivalSequence sequence = iterator.next();
            ServerLevel level = server.getLevel(sequence.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }

            renderSequence(level, sequence);
            sequence.age++;

            if (sequence.age > 58) {
                iterator.remove();
            }
        }
    }

    public static void startSequence(ServerLevel level, double x, double y, double z) {
        ACTIVE_SEQUENCES.add(new ArrivalSequence(level.dimension(), x, y, z));

        level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.25F);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F);

        sendParticle(level, ParticleTypes.FLASH, x, y + 1.0D, z, 1, 0, 0, 0, 0);
        sendParticle(level, ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.8D, z, 38, 1.2F, 1.4F, 1.2F, 0.12F);
    }

    public static void enforceExactWorldSpawn(MinecraftServer server, BlockPos spawn, float yaw) {
        ServerLevel overworld = server.overworld();
        overworld.setDefaultSpawnPos(spawn, yaw);
        overworld.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
    }

    private static void renderSequence(ServerLevel level, ArrivalSequence sequence) {
        int age = sequence.age;
        double x = sequence.x;
        double y = sequence.y;
        double z = sequence.z;

        // A contracting ritual circle at the player's feet.
        if (age <= 34) {
            double radius = Math.max(0.65D, 2.75D - age * 0.055D);
            double rotation = age * 0.22D;
            for (int i = 0; i < 16; i++) {
                double angle = rotation + (Math.PI * 2.0D * i / 16.0D);
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                sendParticle(level, i % 2 == 0 ? GOLD : PALE_GOLD,
                        px, y + 0.08D, pz, 1, 0, 0, 0, 0);
            }
        }

        // A rising double helix gives the base of the beam some motion.
        if (age <= 42) {
            for (int i = 0; i < 7; i++) {
                double py = y + 0.35D + i * 0.72D;
                double angle = age * 0.36D + i * 0.92D;
                double radius = 0.72D - Math.min(age, 35) * 0.010D;
                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;
                sendParticle(level, ParticleTypes.END_ROD, x + dx, py, z + dz,
                        1, 0.01F, 0.02F, 0.01F, 0.0F);
                sendParticle(level, PALE_GOLD, x - dx, py, z - dz,
                        1, 0, 0, 0, 0);
            }
        }

        // The world-height beam. Pulsing instead of sending it every tick keeps packet load low.
        if (age <= 46 && age % 3 == 0) {
            double top = level.getMaxBuildHeight() - 1.0D;
            double step = 5.0D;
            for (double py = y + 0.5D; py <= top; py += step) {
                sendParticle(level, age % 6 == 0 ? PALE_GOLD : GOLD,
                        x, py, z, 2, 0.10F, 1.65F, 0.10F, 0.0F);

                if (((int) (py - y)) % 20 == 0) {
                    sendParticle(level, ParticleTypes.END_ROD,
                            x, py, z, 1, 0.04F, 0.8F, 0.04F, 0.01F);
                }
            }
        }

        if (age == 10) {
            level.playSound(null, x, y, z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.15F, 1.4F);
        }

        if (age == 30) {
            level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 1.0F, 1.65F);
            sendParticle(level, ParticleTypes.ELECTRIC_SPARK, x, y + 1.0D, z,
                    44, 1.7F, 2.6F, 1.7F, 0.18F);
        }

        if (age == 47) {
            level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.35F);
            sendParticle(level, ParticleTypes.FLASH, x, y + 1.0D, z,
                    1, 0, 0, 0, 0);
            sendParticle(level, ParticleTypes.END_ROD, x, y + 1.0D, z,
                    56, 1.8F, 2.8F, 1.8F, 0.10F);
            sendParticle(level, ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0D, z,
                    42, 1.5F, 2.3F, 1.5F, 0.12F);
        }
    }

    private static void sendParticle(ServerLevel level, ParticleOptions particle,
                                     double x, double y, double z, int count,
                                     float xDist, float yDist, float zDist, float speed) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                particle, true, x, y, z, xDist, yDist, zDist, speed, count
        );

        for (ServerPlayer viewer : level.players()) {
            // Keep this local to spawn while still allowing the tall forced beam to be seen.
            double dx = viewer.getX() - x;
            double dz = viewer.getZ() - z;
            if ((dx * dx + dz * dz) <= (192.0D * 192.0D)) {
                viewer.connection.send(packet);
            }
        }
    }

    private static final class ArrivalSequence {
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final double x;
        private final double y;
        private final double z;
        private int age;

        private ArrivalSequence(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
