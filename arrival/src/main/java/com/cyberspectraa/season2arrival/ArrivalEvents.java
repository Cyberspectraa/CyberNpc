package com.cyberspectraa.season2arrival;

import net.minecraft.core.BlockPos;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ArrivalEvents {
    private static final List<ArrivalSequence> ACTIVE_SEQUENCES = new ArrayList<>();

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

            playTimedSounds(level, sequence);
            sequence.age++;

            if (sequence.age > 64) {
                iterator.remove();
            }
        }
    }

    public static void startSequence(ServerLevel level, double x, double y, double z) {
        ACTIVE_SEQUENCES.add(new ArrivalSequence(level.dimension(), x, y, z));

        // One compact custom packet starts the complete client VFX. Photon renders the
        // world-height beam locally instead of the server spamming hundreds of particle packets.
        ArrivalNetwork.sendArrival(level, x, y, z);

        level.playSound(null, x, y, z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.25F);
        level.playSound(null, x, y, z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    public static void enforceExactWorldSpawn(MinecraftServer server, BlockPos spawn, float yaw) {
        ServerLevel overworld = server.overworld();
        overworld.setDefaultSpawnPos(spawn, yaw);
        overworld.getGameRules().getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
    }

    private static void playTimedSounds(ServerLevel level, ArrivalSequence sequence) {
        int age = sequence.age;

        if (age == 8) {
            level.playSound(null, sequence.x, sequence.y, sequence.z,
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.15F, 1.45F);
        }

        if (age == 17) {
            level.playSound(null, sequence.x, sequence.y, sequence.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.35F, 1.65F);
        }

        if (age == 27) {
            level.playSound(null, sequence.x, sequence.y, sequence.z,
                    SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 1.1F, 1.7F);
            level.playSound(null, sequence.x, sequence.y, sequence.z,
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.75F, 1.45F);
        }

        if (age == 55) {
            level.playSound(null, sequence.x, sequence.y, sequence.z,
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.75F, 1.55F);
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
