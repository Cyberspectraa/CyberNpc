package com.cyberspectraa.cybernpc.town;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.intro.IntroSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Explicitly configured town boundary, NOT the much larger wild NPC
 * spawn protection radius. New players must choose a class at the Guild
 * before crossing the town boundary. Their starter gear is optional.
 *
 * Configure with /cybernpc intro setclassgate <radius> while standing at the
 * town centre. Without this admin configuration, no movement is restricted.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID)
public final class TownClassGateEvents {
    private static final Map<UUID, Vec3> LAST_SAFE = new HashMap<>();
    private static final Map<UUID, Long> WARNED = new HashMap<>();

    private TownClassGateEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 2 != 0) return;

        UUID id = player.getUUID();
        if (!shouldGate(player)) {
            LAST_SAFE.remove(id);
            return;
        }

        IntroSettings settings = IntroSettings.get(player.getServer());
        IntroSettings.Point center = settings.classGateCenter();
        if (center == null) return;
        double cx = center.pos().getX() + 0.5D;
        double cz = center.pos().getZ() + 0.5D;
        double radius = settings.classGateRadius();

        // Portal or modded teleports cannot bypass registration.
        if (!center.matches(player.serverLevel())) {
            returnToTown(player, center, null);
            return;
        }

        double dx = player.getX() - cx;
        double dz = player.getZ() - cz;
        if (dx * dx + dz * dz <= radius * radius) {
            // A last known position comfortably inside the boundary avoids
            // teleporting players into a wall or into midair.
            if (player.onGround() && dx * dx + dz * dz < (radius - 4) * (radius - 4)) {
                LAST_SAFE.put(id, player.position());
            }
            return;
        }

        returnToTown(player, center, LAST_SAFE.get(id));
    }

    private static boolean shouldGate(ServerPlayer player) {
        if (player.getServer() == null || !player.isAlive()
                || player.isCreative() || player.isSpectator()
                || player.hasPermissions(2)
                || !ModList.get().isLoaded("cyberclasses")) return false;
        IntroSettings settings = IntroSettings.get(player.getServer());
        if (!settings.classGateEnabled()) return false;
        if (!player.getPersistentData().getCompound("CyberIntro")
                .getBoolean("Completed")) return false;
        return !player.getPersistentData().getCompound("CyberClasses")
                .getBoolean("ClassChosen");
    }

    private static void returnToTown(ServerPlayer player,
                                    IntroSettings.Point center, Vec3 safe) {
        double x = safe == null ? center.pos().getX() + 0.5D : safe.x;
        double y = safe == null ? center.pos().getY() : safe.y;
        double z = safe == null ? center.pos().getZ() + 0.5D : safe.z;
        if (!center.matches(player.serverLevel())) {
            net.minecraft.server.level.ServerLevel destination =
                    player.getServer().getLevel(center.dimension());
            if (destination == null) return;
            player.teleportTo(destination, x, y, z, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(player.serverLevel(),
                    x, y, z, player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);

        long now = player.getServer().overworld().getGameTime();
        if (now >= WARNED.getOrDefault(player.getUUID(), 0L)) {
            player.displayClientMessage(Component.literal(
                    "Register a class at the Adventurers' Guild before leaving town.")
                    .withStyle(ChatFormatting.YELLOW), true);
            WARNED.put(player.getUUID(), now + 100L);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SAFE.remove(event.getEntity().getUUID());
        WARNED.remove(event.getEntity().getUUID());
    }
}
