package com.cyberspectraa.cybernpc.intro;

import com.cyberspectraa.cybernpc.dialogue.NpcDialogueController;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.IntroScenePacket;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import javax.annotation.Nullable;
import java.util.*;

/**
 * One-at-a-time first arrivals. An ordinary server death deliberately triggers
 * the optional client-side Cinematic Respawn mod. CyberNpc does not fake a
 * death screen and does not replace that mod's cinematics.
 *
 * The death is special: inventories and XP are snapshotted and restored after
 * respawn. This cannot suppress every third-party mod's death side effects.
 */
@Mod.EventBusSubscriber(modid = "cybernpc")
public final class CyberIntroService {
    private static final String ROOT = "CyberIntro";
    private static final String PENDING = "Pending";
    private static final String DONE = "Completed";
    private static final String SCRIPTED_DEATH = "SummoningDeath";
    private static final int REVEAL_DELAY_TICKS = 80;
    private static final int POPE_TIMEOUT_TICKS = 800;
    private static final Map<UUID, Session> QUEUE = new LinkedHashMap<>();
    @Nullable private static UUID active;
    @Nullable private static UUID chosenPope;
    @Nullable private static UUID dialogueOpen;

    private enum Stage { CUSTOMISING, WAITING, DEAD, RESPAWNING, GREETING }

    private static final class Session {
        final UUID id;
        Stage stage;
        long stageTick;
        boolean queueScreenSent;
        Session(UUID id, Stage stage) { this.id = id; this.stage = stage; }
    }
    private CyberIntroService() {}

    public static int setArrival(net.minecraft.commands.CommandSourceStack source) {
        IntroSettings data = IntroSettings.get(source.getServer());
        data.setArrival(source.getLevel(), BlockPos.containing(source.getPosition()), source.getRotation().y);
        source.sendSuccess(() -> Component.literal("Summoning arrival saved. "
                + (data.ready() ? "The introduction is ready." : "Now set /cyberintro setpopewait.")), true);
        return 1;
    }

    public static int setPopeWait(net.minecraft.commands.CommandSourceStack source) {
        IntroSettings data = IntroSettings.get(source.getServer());
        data.setPopeWait(source.getLevel(), BlockPos.containing(source.getPosition()), source.getRotation().y);
        source.sendSuccess(() -> Component.literal("Pope waiting point saved. "
                + (data.ready() ? "The introduction is ready." : "Now set /cyberintro setarrival.")), true);
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
        // CyberClasses is optional, but when installed class selection
        // must finish before anyone enters the summon queue.
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

    /** Optional reflection hook from CyberRaces and CyberClasses. */
    public static boolean beginSummoning(ServerPlayer player) {
        if (!configured(player) || state(player).getBoolean(DONE)) return false;
        mark(player, true, false);
        Session session = QUEUE.computeIfAbsent(player.getUUID(),
                id -> new Session(id, Stage.CUSTOMISING));
        if (session.stage == Stage.CUSTOMISING && customised(player))
            session.stage = Stage.WAITING;
        hide(player);
        return true;
    }

    public static boolean resumeIfPending(ServerPlayer player) {
        if (!configured(player) || !state(player).getBoolean(PENDING)
                || state(player).getBoolean(DONE)) return false;
        QUEUE.put(player.getUUID(), new Session(player.getUUID(),
                customised(player) ? Stage.WAITING : Stage.CUSTOMISING));
        hide(player);
        return true;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !configured(player)) return;
        if (state(player).getBoolean(DONE)) return;
        if (state(player).getBoolean(PENDING)) {
            resumeIfPending(player);
        } else if (!created(player)) {
            mark(player, true, false);
            QUEUE.put(player.getUUID(), new Session(player.getUUID(), Stage.CUSTOMISING));
            hide(player);
        } else {
            // Previously existing characters aren't forcibly killed on upgrade.
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
        CompoundTag previous = old.getPersistentData().getCompound(ROOT);
        if (previous.isEmpty()) return;
        fresh.getPersistentData().put(ROOT, previous.copy());
        if (!event.isWasDeath() || !previous.getBoolean(SCRIPTED_DEATH)) return;

        // Item drops are cancelled only for this one scripted death. Restore
        // the original inventory and XP from a snapshot taken before death.
        if (previous.contains("Inventory"))
            fresh.getInventory().load(previous.getList("Inventory", 10));
        fresh.experienceLevel = previous.getInt("VanillaLevel");
        fresh.totalExperience = previous.getInt("VanillaXp");
        fresh.experienceProgress = previous.getFloat("VanillaProgress");
        fresh.setHealth(fresh.getMaxHealth());
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && state(player).getBoolean(SCRIPTED_DEATH)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && state(player).getBoolean(SCRIPTED_DEATH)) event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = QUEUE.get(player.getUUID());
        if (session == null || session.stage != Stage.DEAD
                || !state(player).getBoolean(SCRIPTED_DEATH)) return;

        IntroSettings.Point arrival = IntroSettings.get(player.getServer()).arrival();
        if (arrival == null || player.getServer().getLevel(arrival.dimension()) == null) {
            finish(player);
            return;
        }
        ServerLevel level = player.getServer().getLevel(arrival.dimension());
        player.teleportTo(level, arrival.pos().getX() + 0.5D, arrival.pos().getY(),
                arrival.pos().getZ() + 0.5D, arrival.yaw(), 0F);
        player.setHealth(player.getMaxHealth());
        restoreCheckpoint(player);
        // Do not show the player during the respawn cinematic: the server
        // reveals them after the transition, at the summoning point.
        hide(player);
        session.stage = Stage.RESPAWNING;
        session.stageTick = player.getServer().overworld().getGameTime();
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
        long now = server.overworld().getGameTime();
        for (Session session : QUEUE.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.id);
            if (player == null) continue;
            if (session.stage == Stage.CUSTOMISING && customised(player))
                session.stage = Stage.WAITING;
            if (session.stage == Stage.CUSTOMISING || session.stage == Stage.WAITING) {
                hide(player);
                if (session.stage == Stage.WAITING && !session.queueScreenSent) {
                    session.queueScreenSent = true;
                    CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new IntroScenePacket(true, BlockPos.ZERO, 0F));
                }
            } else if (session.stage == Stage.RESPAWNING) {
                hide(player);
            } else if (session.stage == Stage.GREETING) {
                player.setInvulnerable(true);
                player.setNoGravity(true);
                player.setDeltaMovement(Vec3.ZERO);
            }
        }

        // Pope is called to the waiting position as soon as someone joins.
        if (active == null) {
            ensurePopeReady(server);
            for (Session session : QUEUE.values()) {
                if (session.stage == Stage.WAITING) {
                    ServerPlayer player = server.getPlayerList().getPlayer(session.id);
                    if (player != null) {
                        active = session.id;
                        chosenPope = null;
                        startDeath(player, session);
                    }
                    break;
                }
            }
        }
        if (active == null) return;
        Session session = QUEUE.get(active);
        ServerPlayer player = server.getPlayerList().getPlayer(active);
        if (session == null || player == null) return;

        if (session.stage == Stage.RESPAWNING
                && now - session.stageTick >= REVEAL_DELAY_TICKS) {
            player.setInvisible(false);
            player.level().playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1.15F);
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1D, player.getZ(),
                    80, 0.6D, 0.9D, 0.6D, 0.055D);
            session.stage = Stage.GREETING;
            session.stageTick = now;
        }
        if (session.stage == Stage.GREETING) {
            ensurePopeReady(server);
            if (!active.equals(dialogueOpen)) {
                CyberNpcEntity pope = getPope(server);
                if (pope != null && pope.level() == player.level()
                        && pope.distanceToSqr(player) <= 23.0D) {
                    NpcDialogueController.openIntro(player, pope);
                    dialogueOpen = active;
                } else if (now - session.stageTick >= POPE_TIMEOUT_TICKS) {
                    if (pope != null && pope.level() == player.level()) {
                        pope.teleportTo(player.getX() + 1.5D, player.getY(),
                                player.getZ() + 1.5D);
                        NpcDialogueController.openIntro(player, pope);
                        dialogueOpen = active;
                    } else finish(player);  // Never strand the player.
                }
            }
        }
    }

    private static void startDeath(ServerPlayer player, Session session) {
        if (!configured(player)) { finish(player); return; }
        IntroSettings.Point point = IntroSettings.get(player.getServer()).arrival();
        ServerLevel level = player.getServer().getLevel(point.dimension());
        if (level == null) { finish(player); return; }

        CompoundTag data = state(player);
        data.put("Inventory", player.getInventory().save(new ListTag()));
        data.putInt("VanillaLevel", player.experienceLevel);
        data.putInt("VanillaXp", player.totalExperience);
        data.putFloat("VanillaProgress", player.experienceProgress);
        data.putBoolean("HadRespawnPoint", player.getRespawnPosition() != null);
        if (player.getRespawnPosition() != null) {
            data.putString("OldRespawnDimension", player.getRespawnDimension().location().toString());
            data.putLong("OldRespawnPosition", player.getRespawnPosition().asLong());
            data.putFloat("OldRespawnAngle", player.getRespawnAngle());
            data.putBoolean("OldRespawnForced", player.isRespawnForced());
        }
        data.putBoolean(SCRIPTED_DEATH, true);
        player.getPersistentData().put(ROOT, data);

        // Close the waiting UI before the external death cinematic takes over.
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
        hide(player);
        player.teleportTo(level, point.pos().getX() + 0.5D, point.pos().getY(),
                point.pos().getZ() + 0.5D, point.yaw(), 0F);
        player.setRespawnPosition(point.dimension(), point.pos(), point.yaw(), true, false);
        session.stage = Stage.DEAD;
        session.stageTick = player.getServer().overworld().getGameTime();
        player.setInvulnerable(false);
        player.hurt(player.damageSources().outOfWorld(), Float.MAX_VALUE);
        // Vanilla death must occur. Cinematic Respawn (client-only) handles
        // the camera and automatic respawn; without it the normal death screen
        // remains available, and players can respawn manually.
    }

    private static void restoreCheckpoint(ServerPlayer player) {
        CompoundTag tag = state(player);
        if (!tag.getBoolean(SCRIPTED_DEATH)) return;
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
        tag.remove(SCRIPTED_DEATH);
        tag.remove("Inventory");
        tag.remove("VanillaLevel");
        tag.remove("VanillaXp");
        tag.remove("VanillaProgress");
        tag.remove("HadRespawnPoint");
        tag.remove("OldRespawnDimension");
        tag.remove("OldRespawnPosition");
        tag.remove("OldRespawnAngle");
        tag.remove("OldRespawnForced");
        player.getPersistentData().put(ROOT, tag);
    }

    private static void hide(ServerPlayer player) {
        if (player.isDeadOrDying()) return;
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0F;
    }

    public static boolean isGreeting(ServerPlayer player) {
        Session s = QUEUE.get(player.getUUID());
        return s != null && s.stage == Stage.GREETING
                && player.getUUID().equals(active);
    }

    public static void advanceFromScene(ServerPlayer player) {
        // Legacy custom cinematic packet: intentionally ignored.
        // The death/respawn animation is now owned by Cinematic Respawn.
    }

    public static void finish(ServerPlayer player) {
        QUEUE.remove(player.getUUID());
        if (state(player).getBoolean(SCRIPTED_DEATH)) restoreCheckpoint(player);
        mark(player, false, true);
        if (!player.isDeadOrDying()) {
            player.setInvisible(false);
            player.setInvulnerable(false);
            player.setNoGravity(false);
            player.setDeltaMovement(Vec3.ZERO);
        }
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
        if (player.getUUID().equals(active)) {
            active = null;
            chosenPope = null;
        }
        dialogueOpen = null;
    }

    /** Admin reset hook used by CyberRaces /cyberresetall. */
    public static void resetForRetest(ServerPlayer player) {
        QUEUE.remove(player.getUUID());
        if (state(player).getBoolean(SCRIPTED_DEATH)) restoreCheckpoint(player);
        player.getPersistentData().remove(ROOT);
        if (player.getUUID().equals(active)) {
            active = null;
            chosenPope = null;
            dialogueOpen = null;
        }
        if (configured(player)) {
            mark(player, true, false);
            QUEUE.put(player.getUUID(), new Session(player.getUUID(), Stage.CUSTOMISING));
            hide(player);
        }
        CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new IntroScenePacket(false, BlockPos.ZERO, 0F));
    }

    public static void skip(ServerPlayer player) {
        if (QUEUE.containsKey(player.getUUID())) finish(player);
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
                npc -> npc.isAlive()
                        && NpcServiceRole.fromRole(npc.getRole()) == NpcServiceRole.POPE);
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

    /** Pope AI override, before normal altar/bed behaviour. */
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
        if (distance > (approach ? 9D : 3D))
            pope.getNavigation().moveTo(target.x, target.y, target.z, 0.95D);
        else {
            pope.getNavigation().stop();
            pope.setSprinting(false);
            if (player != null && player.level() == level)
                pope.getLookControl().setLookAt(player, 25F, 25F);
        }
        return true;
    }
}
