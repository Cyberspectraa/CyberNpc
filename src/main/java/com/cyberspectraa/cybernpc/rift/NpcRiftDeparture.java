package com.cyberspectraa.cybernpc.rift;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.block.IslandRiftBlock;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.registry.ModBlocks;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID)
public final class NpcRiftDeparture {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private record Arm(UUID entity, long expires) {}
    private static final Map<String, Arm> ARMED = new HashMap<>();
    private static UUID restoring;
    private NpcRiftDeparture() {}

    private static boolean eligible(CyberNpcEntity npc) {
        return RiftDepartureData.STORY_IDS.contains(npc.getStoryNpcId())
                && npc.getNpcType() != NpcType.WILD && !npc.isSpecialServiceNpc()
                && npc.getSpecialNpcId() == null && npc.isAlive()
                && !npc.isZombifying() && !npc.isSleeping()
                && !npc.isPassenger() && !npc.isVehicle() && !npc.isLeashed();
    }
    private static boolean anotherLoaded(MinecraftServer server, String id, UUID except) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CyberNpcEntity npc && !npc.isRemoved()
                        && id.equals(npc.getStoryNpcId()) && !npc.getUUID().equals(except)) return true;
            }
        }
        return false;
    }
    public static String arm(CyberNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level) || !eligible(npc))
            return "Select a living, awake Main Seven NPC without a mount, leash or service assignment.";
        String id = npc.getStoryNpcId();
        MinecraftServer server = level.getServer();
        if (RiftDepartureData.get(server).departed(id)) return "That character has already departed.";
        if (anotherLoaded(server, id, npc.getUUID())) return "Multiple copies are loaded. Resolve duplicates first.";
        ARMED.put(id, new Arm(npc.getUUID(), server.overworld().getGameTime() + 1200L));
        return null;
    }
    public static void disarm(CyberNpcEntity npc) {
        Arm arm = ARMED.get(npc.getStoryNpcId());
        if (arm != null && arm.entity().equals(npc.getUUID())) ARMED.remove(npc.getStoryNpcId());
    }
    private static boolean armed(CyberNpcEntity npc, MinecraftServer server) {
        Arm arm = ARMED.get(npc.getStoryNpcId());
        return arm != null && arm.entity().equals(npc.getUUID())
                && arm.expires() >= server.overworld().getGameTime();
    }
    private static BlockPos lower(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.ISLAND_RIFT.get())) return null;
        BlockPos bottom = state.getValue(IslandRiftBlock.HALF) == DoubleBlockHalf.UPPER
                ? pos.below() : pos;
        BlockState a = level.getBlockState(bottom), b = level.getBlockState(bottom.above());
        return a.is(ModBlocks.ISLAND_RIFT.get()) && b.is(ModBlocks.ISLAND_RIFT.get())
                && a.getValue(IslandRiftBlock.HALF) == DoubleBlockHalf.LOWER
                && b.getValue(IslandRiftBlock.HALF) == DoubleBlockHalf.UPPER
                && a.getValue(IslandRiftBlock.FACING) == b.getValue(IslandRiftBlock.FACING)
                ? bottom : null;
    }
    public static boolean touch(CyberNpcEntity npc, BlockPos touched) {
        if (!(npc.level() instanceof ServerLevel level) || !armed(npc, level.getServer())
                || !eligible(npc) || lower(level, touched) == null) return false;
        if (!level.getBlockState(touched).getShape(level, touched, CollisionContext.of(npc))
                .bounds().move(touched).intersects(npc.getBoundingBox())) return false;
        String id = npc.getStoryNpcId();
        MinecraftServer server = level.getServer();
        RiftDepartureData data = RiftDepartureData.get(server);
        if (data.departed(id) || anotherLoaded(server, id, npc.getUUID())) {
            disarm(npc);
            return false;
        }
        CompoundTag snapshot;
        try {
            snapshot = npc.saveWithoutId(new CompoundTag());
        } catch (RuntimeException ex) {
            disarm(npc);
            LOGGER.error("Could not snapshot {}", id, ex);
            return false;
        }
        if (!data.recordNpc(npc, snapshot)) return false;
        disarm(npc);
        // Flush departure index before discard. Join guard blocks a stale chunk copy.
        // This cannot make Minecraft's separate world/entity files fully transactional.
        server.overworld().getDataStorage().save();
        npc.releasePersistentClaims();
        npc.discard();
        return true;
    }
    /** Explicit contact test aid; no automatic escort or destination teleport. */
    public static int cross(CommandSourceStack source, Entity target, BlockPos pos) {
        if (!(target instanceof CyberNpcEntity npc) || npc.level() != source.getLevel()
                || !eligible(npc) || !armed(npc, source.getServer())) {
            source.sendFailure(Component.literal("Arm an eligible NPC in this dimension first."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos bottom = lower(level, pos);
        if (bottom == null || npc.position().distanceToSqr(Vec3.atCenterOf(bottom)) > 256.0
                || anotherLoaded(source.getServer(), npc.getStoryNpcId(), npc.getUUID())
                || RiftDepartureData.get(source.getServer()).departed(npc.getStoryNpcId())) {
            source.sendFailure(Component.literal("Use an intact Rift within 16 blocks of the NPC, with no duplicate."));
            return 0;
        }
        Vec3 old = npc.position(), destination = Vec3.atBottomCenterOf(bottom);
        if (!level.noCollision(npc, npc.getBoundingBox().move(destination.subtract(old)))) {
            source.sendFailure(Component.literal("Rift entrance is obstructed."));
            return 0;
        }
        npc.getNavigation().stop();
        npc.setPos(destination.x, destination.y, destination.z);
        npc.setDeltaMovement(Vec3.ZERO);
        if (touch(npc, bottom)) {
            source.sendSuccess(() -> Component.literal("Departure recorded. Open the ledger."), false);
            return 1;
        }
        npc.setPos(old.x, old.y, old.z);
        source.sendFailure(Component.literal("Crossing failed; NPC returned to its starting position."));
        return 0;
    }
    public static int restore(CommandSourceStack source, String value, BlockPos target) {
        String id = RiftDepartureData.storyId(value);
        MinecraftServer server = source.getServer();
        RiftDepartureData data = RiftDepartureData.get(server);
        CompoundTag entry = data.npcRecord(id);
        if (!entry.hasUUID("UUID") || !entry.contains("Entity")) {
            source.sendFailure(Component.literal("No departed NPC record for " + value));
            return 0;
        }
        UUID uuid = entry.getUUID("UUID");
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) != null) {
                source.sendFailure(Component.literal("That entity UUID is already loaded."));
                return 0;
            }
        }
        if (anotherLoaded(server, id, uuid)) {
            source.sendFailure(Component.literal("A copy of that character is already loaded."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        CyberNpcEntity npc = ModEntities.CYBER_NPC.get().create(level);
        if (npc == null) return 0;
        try {
            npc.load(entry.getCompound("Entity"));
            if (!id.equals(npc.getStoryNpcId()) || !uuid.equals(npc.getUUID())) {
                source.sendFailure(Component.literal("Snapshot identity mismatch; record retained."));
                return 0;
            }
            Vec3 pos = Vec3.atBottomCenterOf(target);
            npc.moveTo(pos.x, pos.y, pos.z, npc.getYRot(), npc.getXRot());
            npc.setDeltaMovement(Vec3.ZERO);
            npc.fallDistance = 0F;
            if (!level.hasChunkAt(target) || !level.noCollision(npc)
                    || level.getBlockState(target).is(ModBlocks.ISLAND_RIFT.get())) {
                source.sendFailure(Component.literal("Choose a clear, loaded recovery position outside the Rift."));
                return 0;
            }
            restoring = uuid;
            if (!level.addFreshEntity(npc)) {
                source.sendFailure(Component.literal("Could not restore NPC; snapshot retained."));
                return 0;
            }
            data.removeNpc(id);
            server.overworld().getDataStorage().save();
            source.sendSuccess(() -> Component.literal("Restored " + RiftDepartureData.name(id)
                    + " from saved entity data. Departure cleared."), false);
            return 1;
        } catch (RuntimeException ex) {
            LOGGER.error("Could not restore {}", id, ex);
            source.sendFailure(Component.literal("Recovery failed; check server log."));
            return 0;
        } finally {
            restoring = null;
        }
    }
    @SubscribeEvent public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getEntity() instanceof CyberNpcEntity npc
                && !npc.getUUID().equals(restoring)
                && RiftDepartureData.STORY_IDS.contains(npc.getStoryNpcId())
                && RiftDepartureData.get(level.getServer()).departed(npc.getStoryNpcId())) {
            event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void onStop(ServerStoppedEvent event) {
        ARMED.clear();
        restoring = null;
    }
}
