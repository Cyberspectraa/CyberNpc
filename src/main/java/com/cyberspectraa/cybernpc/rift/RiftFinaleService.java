package com.cyberspectraa.cybernpc.rift;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.block.IslandRiftBlock;
import com.cyberspectraa.cybernpc.dialogue.TownCharacterDialogue;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.RiftCreditsPacket;
import com.cyberspectraa.cybernpc.network.RiftAbortPacket;
import com.cyberspectraa.cybernpc.network.RiftPromptPacket;
import com.cyberspectraa.cybernpc.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative, opt-in Rift finale. The Rift is still not a portal:
 * crossing its two-block tear offers a decision. A committed departure plays
 * private client credits, records a save-safe snapshot and ends the connection.
 *
 * No quest/relationship rewards, NPC despawns or world flags are changed.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID)
public final class RiftFinaleService {
    public static final int CREDITS_TICKS = 42 * 20;
    private static final int PROMPT_TICKS = 30 * 20;
    private static final String DATA = "CyberNpcRiftDeparture";
    private static final String TOWN_DATA = "CyberNpcTownCharacters";
    private static final String[] TOWN_IDS = {
            "mason", "town_npc_1", "town_npc_2", "town_npc_3",
            "town_npc_4", "town_npc_5", "town_npc_6"
    };
    private static final Map<UUID, Prompt> PROMPTS = new HashMap<>();
    private static final Map<UUID, Credits> ACTIVE = new HashMap<>();
    private static final Set<UUID> DECLINED_UNTIL_EXIT = new HashSet<>();
    // An admin can grant an offline UUID a one-time recovery on next login.
    private static final Set<UUID> RECOVERY = new HashSet<>();
    private static final Map<UUID, Long> RESUME_AT = new HashMap<>();

    private record Prompt(ResourceKey<Level> dimension, BlockPos lower, long expires) {}
    private record NpcVisit(String id, String name, int count, boolean friend) {}
    private static final class Credits {
        final Vec3 anchor;
        final boolean wasInvisible;
        final boolean wasInvulnerable;
        final boolean hadNoGravity;
        int elapsed;

        Credits(ServerPlayer player) {
            anchor = player.position();
            wasInvisible = player.isInvisible();
            wasInvulnerable = player.isInvulnerable();
            hadNoGravity = player.isNoGravity();
        }
    }

    private RiftFinaleService() {}

    /** Invoked when a player's body physically intersects either Rift half. */
    public static void touch(ServerPlayer player, BlockPos touched) {
        if (player.isSpectator() || !player.isAlive() || player.getServer() == null) return;
        UUID id = player.getUUID();
        if (ACTIVE.containsKey(id) || PROMPTS.containsKey(id)
                || DECLINED_UNTIL_EXIT.contains(id)) return;
        CompoundTag state = state(player);
        if (state.getBoolean("Departed") || state.getBoolean("CreditsInProgress")
                || player.getPersistentData().getCompound("CyberIntro").getBoolean("Pending")) {
            return;
        }
        BlockPos lower = touched;
        BlockState block = player.level().getBlockState(touched);
        if (!block.is(ModBlocks.ISLAND_RIFT.get())) return;
        if (block.getValue(IslandRiftBlock.HALF) == DoubleBlockHalf.UPPER) {
            lower = touched.below();
        }
        if (!validRift(player, lower)) return;
        long now = player.getServer().overworld().getGameTime();
        PROMPTS.put(id, new Prompt(player.level().dimension(), lower, now + PROMPT_TICKS));
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new RiftPromptPacket());
    }

    /** Never trust a client choice without a currently offered, nearby Rift. */
    public static void choose(ServerPlayer player, boolean accept) {
        Prompt offered = PROMPTS.remove(player.getUUID());
        if (offered == null) return;
        if (!accept) {
            DECLINED_UNTIL_EXIT.add(player.getUUID());
            return;
        }
        long now = player.getServer().overworld().getGameTime();
        if (now > offered.expires() || !player.level().dimension().equals(offered.dimension())
                || !validRift(player, offered.lower())
                || player.distanceToSqr(offered.lower().getX() + 0.5D,
                        offered.lower().getY() + 1D,
                        offered.lower().getZ() + 0.5D) > 9D) {
            // A delayed/spoofed Yes packet cannot trigger a distant farewell.
            return;
        }
        if (state(player).getBoolean("Departed")
                || player.getPersistentData().getCompound("CyberIntro").getBoolean("Pending")) return;
        startCredits(player);
    }

    private static boolean validRift(ServerPlayer player, BlockPos lower) {
        BlockState bottom = player.level().getBlockState(lower);
        BlockState upper = player.level().getBlockState(lower.above());
        return bottom.is(ModBlocks.ISLAND_RIFT.get())
                && bottom.getValue(IslandRiftBlock.HALF) == DoubleBlockHalf.LOWER
                && upper.is(ModBlocks.ISLAND_RIFT.get())
                && upper.getValue(IslandRiftBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    private static void startCredits(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        CompoundTag tag = state(player);
        if (tag.getBoolean("Departed")) return;
        Credits credits = new Credits(player);
        ACTIVE.put(player.getUUID(), credits);

        // Persist acceptance immediately. If the server stops or a client
        // crashes during credits, they resume on their next login.
        tag.putBoolean("CreditsInProgress", true);
        tag.putString("Dimension", player.level().dimension().location().toString());
        tag.putLong("RiftPosition", player.blockPosition().asLong());
        tag.putBoolean("PreviousInvisible", credits.wasInvisible);
        tag.putBoolean("PreviousInvulnerable", credits.wasInvulnerable);
        tag.putBoolean("PreviousNoGravity", credits.hadNoGravity);

        List<NpcVisit> visits = visits(player);
        NpcVisit top = visits.get(0), second = visits.get(1);
        int friends = (int) visits.stream().filter(NpcVisit::friend).count();
        int contacts = visits.stream().mapToInt(NpcVisit::count).sum();
        int playTicks = Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)));
        int deaths = Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)));
        int mobKills = Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)));
        tag.putString("MostTalkedNpc", top.count() > 0 ? top.id() : "");
        tag.putInt("MostTalkedVisits", top.count());
        tag.putInt("TownConversations", contacts);
        tag.putInt("TownFriends", friends);
        tag.putInt("PlayTicks", playTicks);
        tag.putInt("Deaths", deaths);
        tag.putInt("MobKills", mobKills);
        player.getPersistentData().put(DATA, tag);

        lock(player, credits);
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new RiftCreditsPacket(playTicks, deaths, mobKills, contacts, friends,
                        top.count() > 0 ? top.name() : "No one yet", top.count(),
                        second.count() > 0 ? second.name() : "", second.count()));
    }

    private static CompoundTag state(ServerPlayer player) {
        return player.getPersistentData().getCompound(DATA);
    }

    private static List<NpcVisit> visits(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(TOWN_DATA);
        List<NpcVisit> result = new ArrayList<>();
        for (String id : TOWN_IDS) {
            CompoundTag saved = root.getCompound(id);
            String name = TownCharacterDialogue.displayName(id);
            if (name == null || name.isBlank()) {
                name = switch (id) {
                    case "mason" -> "Kai";
                    case "town_npc_1" -> "Katie";
                    case "town_npc_2" -> "Elara";
                    case "town_npc_3" -> "Rowan";
                    case "town_npc_4" -> "Felix";
                    case "town_npc_5" -> "Nadia";
                    default -> "Orin";
                };
            }
            result.add(new NpcVisit(id, name, Math.max(0, saved.getInt("Visits")),
                    saved.getInt("Trust") >= 2 && saved.getInt("Affection") >= 1));
        }
        result.sort(Comparator.comparingInt(NpcVisit::count).reversed()
                .thenComparing(NpcVisit::id));
        return result;
    }

    private static void lock(ServerPlayer player, Credits credits) {
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
        // The GUI consumes movement input, but reject any abnormal movement
        // packets during the irreversible farewell.
        if (player.position().distanceToSqr(credits.anchor) > 0.16D) {
            player.teleportTo(credits.anchor.x, credits.anchor.y, credits.anchor.z);
        }
    }

    private static void release(ServerPlayer player, Credits credits) {
        player.setInvisible(credits.wasInvisible);
        player.setInvulnerable(credits.wasInvulnerable);
        player.setNoGravity(credits.hadNoGravity);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        long now = server.overworld().getGameTime();
        if (!PROMPTS.isEmpty() && now % 5L == 0L) {
            // If the player is teleported away or leaves without answering,
            // expire that offer immediately instead of trapping re-entry.
            PROMPTS.entrySet().removeIf(e -> {
                ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
                return now > e.getValue().expires() || player == null
                        || !player.level().dimension().equals(e.getValue().dimension())
                        || !touchingRift(player);
            });
        }
        if (!DECLINED_UNTIL_EXIT.isEmpty() && now % 5L == 0L) {
            DECLINED_UNTIL_EXIT.removeIf(id -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                return player == null || !touchingRift(player);
            });
        }

        if (!RESUME_AT.isEmpty()) {
            List<UUID> ready = new ArrayList<>();
            RESUME_AT.forEach((id, start) -> { if (now >= start) ready.add(id); });
            for (UUID id : ready) {
                RESUME_AT.remove(id);
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null && state(player).getBoolean("CreditsInProgress")
                        && !state(player).getBoolean("Departed")) {
                    // Reset stale flags saved by a crash before snapshotting
                    // a new session. The departure decision remains final.
                    CompoundTag tag = state(player);
                    player.setInvisible(tag.getBoolean("PreviousInvisible"));
                    player.setInvulnerable(tag.getBoolean("PreviousInvulnerable"));
                    player.setNoGravity(tag.getBoolean("PreviousNoGravity"));
                    startCredits(player);
                }
            }
        }
        if (!ACTIVE.isEmpty()) {
            for (UUID id : new ArrayList<>(ACTIVE.keySet())) {
                Credits credits = ACTIVE.get(id);
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (credits == null || player == null) {
                    ACTIVE.remove(id);
                    continue;
                }
                if (++credits.elapsed < CREDITS_TICKS) {
                    lock(player, credits);
                } else {
                    ACTIVE.remove(id);
                    CompoundTag tag = state(player);
                    tag.putBoolean("CreditsInProgress", false);
                    tag.putBoolean("Departed", true);
                    tag.putLong("CompletedAtGameTime", now);
                    player.getPersistentData().put(DATA, tag);
                    release(player, credits);
                    player.connection.disconnect(Component.literal(
                            "Thank you for playing The Fractured Island. Your story continues beyond the Rift."));
                }
            }
        }
    }

    private static boolean touchingRift(ServerPlayer player) {
        BlockPos b = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                b.offset(-1, -1, -1), b.offset(1, 2, 1))) {
            if (player.level().getBlockState(pos).is(ModBlocks.ISLAND_RIFT.get())
                    && player.getBoundingBox().intersects(new AABB(pos))) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (RECOVERY.remove(id)) {
            reset(player);
            return;
        }
        CompoundTag tag = state(player);
        if (tag.getBoolean("Departed")) {
            if (player.getServer() != null && player.getServer().isDedicatedServer()) {
                player.connection.disconnect(Component.literal(
                        "You have left The Fractured Island. Your journey beyond the Rift has begun."));
            } else {
                // Never permanently lock an owner out of an integrated test world.
                player.sendSystemMessage(Component.literal(
                        "Rift departure recorded. An operator can use /cybernpc rift reset to replay it."));
            }
        } else if (tag.getBoolean("CreditsInProgress") && player.getServer() != null) {
            // Delay the opening packet until the client has loaded the level.
            RESUME_AT.put(id, player.getServer().overworld().getGameTime() + 40L);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        PROMPTS.remove(id);
        DECLINED_UNTIL_EXIT.remove(id);
        RESUME_AT.remove(id);
        Credits credits = ACTIVE.remove(id);
        if (credits != null) release(player, credits);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer fresh)) return;
        CompoundTag old = event.getOriginal().getPersistentData().getCompound(DATA);
        if (!old.isEmpty()) fresh.getPersistentData().put(DATA, old.copy());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PROMPTS.clear();
        ACTIVE.clear();
        DECLINED_UNTIL_EXIT.clear();
        RESUME_AT.clear();
        RECOVERY.clear();
    }

    /** Admin-only recovery for a present player, or next login by UUID. */
    public static void reset(ServerPlayer player) {
        UUID id = player.getUUID();
        PROMPTS.remove(id);
        DECLINED_UNTIL_EXIT.remove(id);
        RESUME_AT.remove(id);
        Credits credits = ACTIVE.remove(id);
        if (credits != null) {
            release(player, credits);
            CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new RiftAbortPacket());
        }
        player.getPersistentData().remove(DATA);
    }

    public static void permitRecovery(UUID id) {
        RECOVERY.add(id);
    }
}
