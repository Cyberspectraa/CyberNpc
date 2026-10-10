package com.cyberspectraa.cybernpc.intro;

import com.cyberspectraa.cybernpc.dialogue.NpcDialogueController;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.IntroQueuePacket;
import com.cyberspectraa.cybernpc.network.IntroScenePacket;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One-at-a-time, death-free first arrival. The camera is wholly client-side,
 * while the server controls player concealment, the reveal, the queue,
 * the Pope's priority navigation, and conversation.
 *
 * No NPC dialogue lines are authored here; all existing Pope dialogue remains
 * in NpcDialogueController and must be approved separately by the mod owner.
 */
@Mod.EventBusSubscriber(modid = "cybernpc")
public final class CyberIntroService {
    private static final String ROOT = "CyberIntro";
    private static final String PENDING = "Pending";
    private static final String DONE = "Completed";
    private static final String LEGACY_DEATH = "SummoningDeath";

    /** 2.25-second title prelude; then preserve the original 6.5-second summoning. */
    public static final int TITLE_TICKS = 45;
    public static final int SUMMON_TICKS = 130;
    public static final int SCENE_TICKS = TITLE_TICKS + SUMMON_TICKS;
    public static final int REVEAL_TICKS = TITLE_TICKS + 62;
    private static final int SCENE_GRACE_TICKS = 35;
    private static final int POPE_TIMEOUT_TICKS = 800;

    private static final Map<UUID, Session> QUEUE = new LinkedHashMap<>();
    @Nullable private static UUID active;
    @Nullable private static UUID chosenPope;
    @Nullable private static UUID dialogueOpen;

    private enum Stage { CUSTOMISING, WAITING, CINEMATIC, GREETING }

    private static final class Session {
        final UUID id;
        Stage stage;
        long stageTick;
        boolean queueScreenSent;
        boolean revealed;
        boolean effectsStarted;
        boolean completionRequested;
        Session(UUID id, Stage stage) { this.id = id; this.stage = stage; }
    }

    private CyberIntroService() {}

    public static int setArrival(net.minecraft.commands.CommandSourceStack source) {
        IntroSettings data = IntroSettings.get(source.getServer());
        data.setArrival(source.getLevel(), BlockPos.containing(source.getPosition()), source.getRotation().y);
        source.sendSuccess(() -> Component.literal("Summoning arrival saved. "
                + (data.ready() ? "The introduction is ready." : "Now set /cybernpc intro setpopewait.")), true);
        return 1;
    }

    public static int setPopeWait(net.minecraft.commands.CommandSourceStack source) {
        IntroSettings data = IntroSettings.get(source.getServer());
        data.setPopeWait(source.getLevel(), BlockPos.containing(source.getPosition()), source.getRotation().y);
        source.sendSuccess(() -> Component.literal("Pope waiting point saved. "
                + (data.ready() ? "The introduction is ready." : "Now set /cybernpc intro setarrival.")), true);
        return 1;
    }

    private static CompoundTag state(ServerPlayer player) {
        return player.getPersistentData().getCompound(ROOT);
    }

    private static boolean created(ServerPlayer player) {
        return player.getPersistentData().getCompound("CyberRaces").getBoolean("CharacterCreated");
    }

    private static boolean customised(ServerPlayer player) {
        if (!created(player)) return false;
        return !ModList.get().isLoaded("cyberclasses")
                || player.getPersistentData().getCompound("CyberClasses").getBoolean("ClassChosen");
    }

    private static void mark(ServerPlayer player, boolean pending, boolean done) {
        CompoundTag data = state(player);
        data.putBoolean(PENDING, pending);
        data.putBoolean(DONE, done);
        player.getPersistentData().put(ROOT, data);
    }

    private static boolean configured(ServerPlayer player) {
        if (player.getServer() == null) return false;
        IntroSettings settings = IntroSettings.get(player.getServer());
        return settings.ready()
                && settings.arrival().dimension().equals(settings.popeWait().dimension())
                && player.getServer().getLevel(settings.arrival().dimension()) != null;
    }

    /** CyberServer may defer its legacy arrival teleport when we own the scene. */
    public static boolean usesIntroArrival(ServerPlayer player) {
        return configured(player);
    }

    /** Optional reflection hook from CyberRaces/CyberClasses. */
    public static boolean beginSummoning(ServerPlayer player) {
        if (!configured(player) || state(player).getBoolean(DONE)) return false;
        mark(player, true, false);
        Session session = QUEUE.computeIfAbsent(player.getUUID(),
                id -> new Session(id, Stage.CUSTOMISING));
        if (session.stage == Stage.CUSTOMISING && customised(player)) session.stage = Stage.WAITING;
        hold(player, true);
        return true;
    }

    /**
     * Replay the complete cinematic and Pope welcome for a finished character,
     * without clearing their race, class, XP or unlocks. The same FIFO queue
     * used by natural first-time arrivals also protects repeat testing.
     *
     * Results are explicit so CyberServer can show useful command feedback.
     */
    public static String testFullIntro(ServerPlayer player) {
        if (!configured(player)) return "SETUP_REQUIRED";
        if (!customised(player)) return "CUSTOMIZATION_INCOMPLETE";
        if (QUEUE.containsKey(player.getUUID())) return "ALREADY_IN_INTRO";

        mark(player, true, false);
        Session session = new Session(player.getUUID(), Stage.WAITING);
        QUEUE.put(player.getUUID(), session);
        hold(player, true);
        return active == null ? "STARTED" : "QUEUED";
    }

    /** Returning players with interrupted introductions must stay hidden. */
    public static boolean resumeIfPending(ServerPlayer player) {
        if (!configured(player) || !state(player).getBoolean(PENDING)
                || state(player).getBoolean(DONE)) return false;
        QUEUE.put(player.getUUID(), new Session(player.getUUID(),
                customised(player) ? Stage.WAITING : Stage.CUSTOMISING));
        hold(player, true);
        return true;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Migrate anyone who disconnected during the previous real-death
        // implementation, without causing another death or changing their XP.
        restoreOldScriptedDeath(player);
        if (!configured(player) || state(player).getBoolean(DONE)) return;
        if (state(player).getBoolean(PENDING)) {
            resumeIfPending(player);
        } else if (!created(player)) {
            mark(player, true, false);
            QUEUE.put(player.getUUID(), new Session(player.getUUID(), Stage.CUSTOMISING));
            hold(player, true);
        } else {
            // Existing characters on upgrade never become new arrivals.
            mark(player, false, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        QUEUE.remove(id);
        if (id.equals(active)) {
            active = null;
            chosenPope = null;
            dialogueOpen = null;
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer fresh)
                || !(event.getOriginal() instanceof ServerPlayer old)) return;
        CompoundTag oldTag = old.getPersistentData().getCompound(ROOT);
        if (!oldTag.isEmpty()) fresh.getPersistentData().put(ROOT, oldTag.copy());
        // Actual gameplay death is no longer part of a summoning.
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUE.clear();
        active = null;
        chosenPope = null;
        dialogueOpen = null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        QUEUE.entrySet().removeIf(e -> server.getPlayerList().getPlayer(e.getKey()) == null);
        if (active != null && !QUEUE.containsKey(active)) {
            active = null;
            chosenPope = null;
            dialogueOpen = null;
        }

        for (Session session : QUEUE.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.id);
            if (player == null) continue;
            if (session.stage == Stage.CUSTOMISING && customised(player)) {
                session.stage = Stage.WAITING;
            }
            if (session.stage == Stage.CUSTOMISING || session.stage == Stage.WAITING) {
                hold(player, true);
                if (session.stage == Stage.WAITING && !session.queueScreenSent) {
                    session.queueScreenSent = true;
                    sendQueue(player, true);
                }
            } else if (session.stage == Stage.CINEMATIC) {
                // This is essential: do not re-hide a player after the reveal.
                hold(player, !session.revealed);
            } else if (session.stage == Stage.GREETING) {
                hold(player, false);
            }
        }

        long now = server.overworld().getGameTime();
        if (active == null) {
            ensurePopeReady(server);
            for (Session session : QUEUE.values()) {
                if (session.stage == Stage.WAITING) {
                    ServerPlayer player = server.getPlayerList().getPlayer(session.id);
                    if (player != null) {
                        active = session.id;
                        chosenPope = null;
                        startCinematic(player, session);
                    }
                    break;
                }
            }
        }

        if (active == null) return;
        Session session = QUEUE.get(active);
        ServerPlayer player = server.getPlayerList().getPlayer(active);
        if (session == null || player == null) return;

        if (session.stage == Stage.CINEMATIC) {
            long elapsed = now - session.stageTick;
            if (elapsed >= TITLE_TICKS && !session.effectsStarted) {
                session.effectsStarted = true;
                startSummoningEffects(player);
            }
            if (elapsed >= REVEAL_TICKS && !session.revealed) reveal(player, session);
            // The client's final frame can arrive slightly before the server
            // reaches its own tick deadline. Remember the request and finish
            // at the first valid server tick instead of waiting for the
            // 35-tick emergency timeout.
            if ((session.completionRequested && elapsed >= SCENE_TICKS)
                    || elapsed >= SCENE_TICKS + SCENE_GRACE_TICKS) {
                advanceFromScene(player);
            }
        }
        if (session.stage == Stage.GREETING) {
            ensurePopeReady(server);
            if (!active.equals(dialogueOpen)) {
                CyberNpcEntity pope = getPope(server);
                if (pope != null && pope.level() == player.level()
                        && pope.distanceToSqr(player) <= 23D) {
                    NpcDialogueController.openIntro(player, pope);
                    dialogueOpen = active;
                } else if (now - session.stageTick >= POPE_TIMEOUT_TICKS) {
                    if (pope != null && pope.level() == player.level()) {
                        pope.teleportTo(player.getX() + 1.5D, player.getY(),
                                player.getZ() + 1.5D);
                        NpcDialogueController.openIntro(player, pope);
                        dialogueOpen = active;
                    } else {
                        finish(player); // Never strand someone without a Pope.
                    }
                }
            }
        }
    }

    private static void sendQueue(ServerPlayer player, boolean waiting) {
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroQueuePacket(waiting));
    }

    private static void startCinematic(ServerPlayer player, Session session) {
        IntroSettings.Point arrival = IntroSettings.get(player.getServer()).arrival();
        if (arrival == null) { finish(player); return; }
        ServerLevel level = player.getServer().getLevel(arrival.dimension());
        if (level == null) { finish(player); return; }
        hold(player, true);
        // The queue screen closes, then the world camera takes over.
        sendQueue(player, false);
        player.teleportTo(level, arrival.pos().getX() + 0.5D,
                arrival.pos().getY(), arrival.pos().getZ() + 0.5D,
                arrival.yaw(), 0F);
        session.stage = Stage.CINEMATIC;
        session.stageTick = player.getServer().overworld().getGameTime();
        session.revealed = false;
        session.effectsStarted = false;
        session.completionRequested = false;
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(true, arrival.pos(), arrival.yaw()));
        // Summoning particles and sounds begin AFTER the title card, so the
        // existing 6.5-second effect is not hidden behind the opening text.
        ensurePopeReady(player.getServer());
    }

    private static void startSummoningEffects(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        // Reuse the optional CyberServer visual system without changing it.
        if (ModList.get().isLoaded("cyberserver")) {
            try {
                Class<?> fx = Class.forName(
                        "com.cyberspectraa.cyberserver.CyberServerEvents");
                fx.getMethod("startArrival", ServerLevel.class,
                        double.class, double.class, double.class)
                        .invoke(null, level, player.getX(), player.getY(), player.getZ());
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Without the optional CyberServer FX, our own reveal remains.
            }
        }
    }

    private static void reveal(ServerPlayer player, Session session) {
        if (session.revealed) return;
        session.revealed = true;
        player.setInvisible(false);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1D,
                player.getZ(), 95, 0.6D, 0.9D, 0.6D, 0.07D);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1D,
                player.getZ(), 105, 0.65D, 1.0D, 0.65D, 0.28D);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 1.24F);
    }

    /**
     * A valid client completion is accepted only after the title prelude
     * and complete 6.5-second summoning. Slow clients get a grace
     * period before the server advances them to avoid an indefinite freeze.
     */
    public static void advanceFromScene(ServerPlayer player) {
        Session session = QUEUE.get(player.getUUID());
        if (session == null || session.stage != Stage.CINEMATIC
                || !player.getUUID().equals(active)) return;
        long elapsed = player.getServer().overworld().getGameTime() - session.stageTick;
        if (elapsed < SCENE_TICKS) {
            session.completionRequested = true;
            return;
        }
        reveal(player, session);
        session.stage = Stage.GREETING;
        session.stageTick = player.getServer().overworld().getGameTime();
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
    }

    private static void hold(ServerPlayer player, boolean concealed) {
        player.setInvisible(concealed);
        player.setInvulnerable(true);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
    }

    public static boolean isGreeting(ServerPlayer player) {
        Session session = QUEUE.get(player.getUUID());
        return session != null && session.stage == Stage.GREETING
                && player.getUUID().equals(active);
    }

    public static void finish(ServerPlayer player) {
        QUEUE.remove(player.getUUID());
        mark(player, false, true);
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
        sendQueue(player, false);
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
        if (player.getUUID().equals(active)) {
            active = null;
            chosenPope = null;
        }
        dialogueOpen = null;
    }

    /** Admin testing reset hook; CyberRaces still owns /cyberresetall. */
    public static void resetForRetest(ServerPlayer player) {
        QUEUE.remove(player.getUUID());
        player.getPersistentData().remove(ROOT);
        if (player.getUUID().equals(active)) {
            active = null;
            chosenPope = null;
            dialogueOpen = null;
        }
        if (configured(player)) {
            mark(player, true, false);
            QUEUE.put(player.getUUID(), new Session(player.getUUID(), Stage.CUSTOMISING));
            hold(player, true);
        }
        sendQueue(player, false);
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
    }

    /** Skipping the cinematic skips the optional welcome as well. */
    public static void skip(ServerPlayer player) {
        if (QUEUE.containsKey(player.getUUID())) finish(player);
    }

    /**
     * Recovery for saves produced by the previous Cinematic Respawn version.
     * These snapshots no longer get created after this update.
     */
    private static void restoreOldScriptedDeath(ServerPlayer player) {
        CompoundTag tag = state(player);
        if (!tag.getBoolean(LEGACY_DEATH)) return;
        if (tag.contains("Inventory"))
            player.getInventory().load(tag.getList("Inventory", 10));
        player.experienceLevel = tag.getInt("VanillaLevel");
        player.totalExperience = tag.getInt("VanillaXp");
        player.experienceProgress = tag.getFloat("VanillaProgress");
        if (tag.getBoolean("HadRespawnPoint")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("OldRespawnDimension"));
            if (id != null) {
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, id);
                player.setRespawnPosition(dimension, BlockPos.of(tag.getLong("OldRespawnPosition")),
                        tag.getFloat("OldRespawnAngle"), tag.getBoolean("OldRespawnForced"), false);
            }
        } else {
            player.setRespawnPosition(Level.OVERWORLD, null, 0F, false, false);
        }
        for (String key : new String[]{LEGACY_DEATH, "Inventory", "VanillaLevel",
                "VanillaXp", "VanillaProgress", "HadRespawnPoint", "OldRespawnDimension",
                "OldRespawnPosition", "OldRespawnAngle", "OldRespawnForced"}) {
            tag.remove(key);
        }
        player.getPersistentData().put(ROOT, tag);
    }

    private static void ensurePopeReady(MinecraftServer server) {
        if (!QUEUE.isEmpty()) getPope(server);
    }

    @Nullable private static CyberNpcEntity getPope(MinecraftServer server) {
        IntroSettings.Point wait = IntroSettings.get(server).popeWait();
        if (wait == null) return null;
        ServerLevel level = server.getLevel(wait.dimension());
        if (level == null) return null;
        level.getChunkAt(wait.pos());
        if (chosenPope != null) {
            var found = level.getEntity(chosenPope);
            if (found instanceof CyberNpcEntity npc && npc.isAlive()) return npc;
            chosenPope = null;
        }
        var nearby = level.getEntitiesOfClass(CyberNpcEntity.class,
                new AABB(wait.pos()).inflate(96, 25, 96),
                npc -> npc.isAlive() && NpcServiceRole.fromRole(npc.getRole()) == NpcServiceRole.POPE);
        CyberNpcEntity pope = nearby.stream().min(
                Comparator.comparingDouble(n -> n.blockPosition().distSqr(wait.pos())))
                .orElse(null);
        if (pope == null) {
            pope = ModEntities.CYBER_NPC.get().create(level);
            if (pope == null) return null;
            pope.moveTo(wait.pos().getX() + 0.5D, wait.pos().getY(),
                    wait.pos().getZ() + 0.5D, wait.yaw(), 0);
            pope.setNpcType(NpcType.MAIN);
            pope.setRole("Pope");
            pope.setCustomName(Component.literal("Pope"));
            pope.setPersistenceRequired();
            if (!level.addFreshEntity(pope)) return null;
        }
        chosenPope = pope.getUUID();
        return pope;
    }

    /** Priority Pope duty, before normal church/altar/bed behaviour. */
    public static boolean tickPope(CyberNpcEntity pope, ServerLevel level) {
        if (QUEUE.isEmpty() || !IntroSettings.get(level.getServer()).ready()) return false;
        CyberNpcEntity selected = getPope(level.getServer());
        if (selected == null || !selected.getUUID().equals(pope.getUUID())) return false;
        IntroSettings.Point wait = IntroSettings.get(level.getServer()).popeWait();
        if (!wait.matches(level)) return false;
        ServerPlayer player = active == null ? null
                : level.getServer().getPlayerList().getPlayer(active);
        Session session = active == null ? null : QUEUE.get(active);
        boolean approach = player != null && player.level() == level
                && session != null && session.stage == Stage.GREETING;
        Vec3 target = approach ? player.position() : Vec3.atBottomCenterOf(wait.pos());
        double distance = pope.position().distanceToSqr(target);
        if (distance > (approach ? 9D : 3D)) {
            pope.getNavigation().moveTo(target.x, target.y, target.z, 0.95D);
        } else {
            pope.getNavigation().stop();
            pope.setSprinting(false);
            if (player != null && player.level() == level)
                pope.getLookControl().setLookAt(player, 25F, 25F);
        }
        return true;
    }
}
