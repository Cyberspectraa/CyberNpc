package com.cyberspectraa.cybernpc.intro;

import com.cyberspectraa.cybernpc.dialogue.NpcDialogueController;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.cyberspectraa.cybernpc.network.IntroScenePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned first arrival, only after CyberRaces character creation. */
@Mod.EventBusSubscriber(modid = "cybernpc")
public final class CyberIntroService {
    private static final String ROOT = "CyberIntro";
    private static final String PENDING = "Pending";
    private static final String DONE = "Completed";
    private static final int REVEAL_TICKS = 70;
    private static final int CINEMATIC_TIMEOUT = 205;
    private static final int POPE_TIMEOUT = 900;
    private static final Map<UUID, Session> QUEUE = new LinkedHashMap<>();
    @Nullable private static UUID active;
    @Nullable private static UUID chosenPope;

    private enum Stage { CUSTOMISING, READY, CINEMATIC, GREETING }
    private static final class Session {
        final UUID player;
        Stage stage;
        long stageTick;
        boolean revealed;
        Session(UUID id, Stage stage) { player = id; this.stage = stage; }
    }
    private CyberIntroService() {}

    public static int setArrival(net.minecraft.commands.CommandSourceStack source) {
        IntroSettings data = IntroSettings.get(source.getServer());
        data.setArrival(source.getLevel(), BlockPos.containing(source.getPosition()), source.getRotation().y);
        source.sendSuccess(() -> Component.literal("Summoning arrival saved. " +
                (data.ready() ? "The introduction is ready." : "Now set /cyberintro setpopewait.")), true);
        return 1;
    }

    public static int setPopeWait(net.minecraft.commands.CommandSourceStack source) {
        IntroSettings data = IntroSettings.get(source.getServer());
        data.setPopeWait(source.getLevel(), BlockPos.containing(source.getPosition()), source.getRotation().y);
        source.sendSuccess(() -> Component.literal("Pope waiting point saved. " +
                (data.ready() ? "The introduction is ready." : "Now set /cyberintro setarrival.")), true);
        return 1;
    }

    private static boolean created(ServerPlayer player) {
        return player.getPersistentData().getCompound("CyberRaces").getBoolean("CharacterCreated");
    }
    private static CompoundTag state(ServerPlayer player) {
        return player.getPersistentData().getCompound(ROOT);
    }
    private static void setState(ServerPlayer player, boolean pending, boolean completed) {
        CompoundTag tag = state(player);
        tag.putBoolean(PENDING, pending);
        tag.putBoolean(DONE, completed);
        player.getPersistentData().put(ROOT, tag);
    }
    private static boolean configured(ServerPlayer player) {
        IntroSettings settings = IntroSettings.get(player.getServer());
        return settings.ready()
                && player.getServer().getLevel(settings.arrival().dimension()) != null
                && settings.arrival().dimension().equals(settings.popeWait().dimension());
    }

    /** Called from CyberRaces via optional reflection, while the player is still invisible. */
    public static boolean beginSummoning(ServerPlayer player) {
        if (!configured(player) || state(player).getBoolean(DONE)) return false;
        setState(player, true, false);
        Session session = QUEUE.computeIfAbsent(player.getUUID(),
                id -> new Session(id, Stage.READY));
        session.stage = Stage.READY;
        hide(player);
        return true;
    }

    /** Called before CyberRaces releases a returning character. */
    public static boolean resumeIfPending(ServerPlayer player) {
        if (!configured(player) || !state(player).getBoolean(PENDING)
                || state(player).getBoolean(DONE)) return false;
        QUEUE.put(player.getUUID(), new Session(player.getUUID(),
                created(player) ? Stage.READY : Stage.CUSTOMISING));
        hide(player);
        return true;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !configured(player)
                || state(player).getBoolean(DONE)) return;
        if (state(player).getBoolean(PENDING)) {
            resumeIfPending(player);
        } else if (!created(player)) {
            setState(player, true, false);
            QUEUE.put(player.getUUID(), new Session(player.getUUID(), Stage.CUSTOMISING));
            hide(player);
        } else {
            // Existing characters upgrading the mod are not treated as newcomers.
            setState(player, false, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        QUEUE.remove(id);
        if (id.equals(active)) { active = null; chosenPope = null; dialogueOpen = null; }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag tag = event.getOriginal().getPersistentData().getCompound(ROOT);
        if (!tag.isEmpty()) event.getEntity().getPersistentData().put(ROOT, tag.copy());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        QUEUE.entrySet().removeIf(entry -> server.getPlayerList().getPlayer(entry.getKey()) == null);
        if (active != null && !QUEUE.containsKey(active)) { active = null; chosenPope = null; dialogueOpen = null; }

        for (Session session : QUEUE.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.player);
            if (player == null) continue;
            if (session.stage == Stage.CUSTOMISING && created(player))
                session.stage = Stage.READY;
            // Keep creators and queued players hidden, but NEVER re-hide
            // someone after the scripted summoning reveal has occurred.
            if (session.stage == Stage.CUSTOMISING || session.stage == Stage.READY
                    || (session.stage == Stage.CINEMATIC && !session.revealed)) {
                hide(player);
            } else {
                player.setInvulnerable(true);
                player.setNoGravity(true);
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0F;
            }
        }

        if (active == null) {
            // The first player who has finished customisation gets the summoning
            // stage. Someone idling in the creator cannot block later arrivals.
            for (Session session : QUEUE.values()) {
                if (session.stage == Stage.READY) {
                    active = session.player;
                    chosenPope = null;
                    ServerPlayer player = server.getPlayerList().getPlayer(active);
                    if (player != null) startCinematic(player, session);
                    break;
                }
            }
        }
        if (active == null) {
            ensurePopeReady(server);
            return;
        }
        Session session = QUEUE.get(active);
        ServerPlayer player = server.getPlayerList().getPlayer(active);
        if (session == null || player == null) return;
        if (session.stage == Stage.CINEMATIC) {
            long elapsed = server.overworld().getGameTime() - session.stageTick;
            if (elapsed >= REVEAL_TICKS && !session.revealed) reveal(player, session);
            if (elapsed >= CINEMATIC_TIMEOUT) advanceFromScene(player);
        } else if (session.stage == Stage.GREETING) {
            ensurePopeReady(server);
            CyberNpcEntity pope = getPope(server);
            if (!player.getUUID().equals(dialogueOpen) && pope != null && pope.level() == player.level()
                    && pope.distanceToSqr(player) <= 23.0D) {
                NpcDialogueController.openIntro(player, pope);
                session.stageTick = server.overworld().getGameTime();
                // Wait here for the authenticated conversation to finish.
                session.stage = Stage.GREETING;
                // NPC dialogue is sent once; see 'dialogueOpen' below.
                dialogueOpen = active;
            } else if (!player.getUUID().equals(dialogueOpen)
                    && server.overworld().getGameTime() - session.stageTick > POPE_TIMEOUT) {
                if (pope != null && pope.level() == player.level()) {
                    pope.teleportTo(player.getX()+1.6, player.getY(), player.getZ()+1.6);
                    NpcDialogueController.openIntro(player, pope);
                    dialogueOpen = active;
                } else {
                    finish(player);
                }
            }
        }
    }

    @Nullable private static UUID dialogueOpen;

    private static void startCinematic(ServerPlayer player, Session session) {
        IntroSettings.Point point = IntroSettings.get(player.getServer()).arrival();
        ServerLevel level = player.getServer().getLevel(point.dimension());
        if (level == null) { finish(player); return; }
        hide(player);
        player.teleportTo(level, point.pos().getX() + 0.5D, point.pos().getY(),
                point.pos().getZ() + 0.5D, point.yaw(), 0F);
        session.stage = Stage.CINEMATIC;
        session.stageTick = player.getServer().overworld().getGameTime();
        session.revealed = false;
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(true, point.pos(), point.yaw()));
        ensurePopeReady(player.getServer());
    }

    private static void hide(ServerPlayer player) {
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
    }

    private static void reveal(ServerPlayer player, Session session) {
        if (session.revealed) return;
        session.revealed = true;
        player.setInvisible(false);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY()+1,
                player.getZ(), 90, 0.65, 1.1, 0.65, 0.08);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY()+1,
                player.getZ(), 120, 0.8, 1.25, 0.8, 0.4);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1F, 1.25F);
    }

    public static void advanceFromScene(ServerPlayer player) {
        Session session = QUEUE.get(player.getUUID());
        if (session == null || session.stage != Stage.CINEMATIC
                || !player.getUUID().equals(active)) return;
        reveal(player, session);
        session.stage = Stage.GREETING;
        session.stageTick = player.getServer().overworld().getGameTime();
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
    }

    public static boolean isGreeting(ServerPlayer player) {
        Session s = QUEUE.get(player.getUUID());
        return s != null && s.stage == Stage.GREETING && player.getUUID().equals(active);
    }

    public static void finish(ServerPlayer player) {
        Session s = QUEUE.remove(player.getUUID());
        setState(player, false, true);
        if (s != null) {
            player.setInvisible(false);
            player.setInvulnerable(false);
            player.setNoGravity(false);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0F;
        }
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
        if (player.getUUID().equals(active)) {
            active = null;
            chosenPope = null;
        }
        dialogueOpen = null;
    }

    public static void skip(ServerPlayer player) {
        if (!QUEUE.containsKey(player.getUUID())) return;
        finish(player);
    }

    private static void ensurePopeReady(MinecraftServer server) {
        if (QUEUE.isEmpty()) return;
        getPope(server);
    }

    @Nullable
    private static CyberNpcEntity getPope(MinecraftServer server) {
        IntroSettings.Point wait = IntroSettings.get(server).popeWait();
        if (wait == null) return null;
        ServerLevel level = server.getLevel(wait.dimension());
        if (level == null) return null;
        level.getChunkAt(wait.pos());
        if (chosenPope != null) {
            var entity = level.getEntity(chosenPope);
            if (entity instanceof CyberNpcEntity npc && npc.isAlive()) return npc;
            chosenPope = null;
        }
        var nearby = level.getEntitiesOfClass(CyberNpcEntity.class,
                new AABB(wait.pos()).inflate(96, 25, 96),
                npc -> npc.isAlive() && NpcServiceRole.fromRole(npc.getRole()) == NpcServiceRole.POPE);
        CyberNpcEntity pope = nearby.stream()
                .min(java.util.Comparator.comparingDouble(n -> n.blockPosition().distSqr(wait.pos())))
                .orElse(null);
        if (pope == null) {
            // A single fallback Pope makes the two-command setup sufficient even
            // on a new world. Existing nearby Popes are always reused.
            pope = ModEntities.CYBER_NPC.get().create(level);
            if (pope == null) return null;
            pope.moveTo(wait.pos().getX()+0.5, wait.pos().getY(), wait.pos().getZ()+0.5,
                    wait.yaw(), 0);
            pope.setNpcType(NpcType.MAIN);
            pope.setRole("Pope");
            pope.setCustomName(Component.literal("Pope"));
            pope.setPersistenceRequired();
            if (!level.addFreshEntity(pope)) return null;
        }
        chosenPope = pope.getUUID();
        return pope;
    }

    /** Priority Pope duty invoked from existing NPC AI only during an introduction. */
    public static boolean tickPope(CyberNpcEntity pope, ServerLevel level) {
        if (QUEUE.isEmpty() || !IntroSettings.get(level.getServer()).ready()) return false;
        CyberNpcEntity selected = getPope(level.getServer());
        if (selected == null || !selected.getUUID().equals(pope.getUUID())) return false;
        IntroSettings.Point wait = IntroSettings.get(level.getServer()).popeWait();
        if (!wait.matches(level)) return false;
        ServerPlayer player = active == null ? null : level.getServer().getPlayerList().getPlayer(active);
        boolean approach = player != null && player.level() == level
                && QUEUE.get(active) != null && QUEUE.get(active).stage == Stage.GREETING;
        Vec3 target = approach ? player.position() : Vec3.atBottomCenterOf(wait.pos());
        double distance = pope.position().distanceToSqr(target);
        if (distance > (approach ? 9.0D : 3.0D)) {
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
