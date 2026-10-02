package com.cyberspectraa.cybernpc.service;

import com.cyberspectraa.cybernpc.compat.CyberServerCompat;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpecialNpcSavedData extends SavedData {
    private static final String DATA_NAME = "cybernpc_special_npcs";
    private static final int RESPAWN_DELAY_TICKS = 120;
    private static final int MATERIALIZE_AFTER_BEAM_TICKS = 27;

    private final Map<UUID, SpecialNpcRecord> records = new HashMap<>();

    public static SpecialNpcSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static SpecialNpcSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SpecialNpcSavedData::load,
                SpecialNpcSavedData::new,
                DATA_NAME
        );
    }

    public static SpecialNpcSavedData load(CompoundTag tag) {
        SpecialNpcSavedData data = new SpecialNpcSavedData();
        ListTag list = tag.getList("Records", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            SpecialNpcRecord record = SpecialNpcRecord.load(
                    list.getCompound(i)
            );
            if (record != null) {
                data.records.put(record.specialId, record);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (SpecialNpcRecord record : records.values()) {
            list.add(record.save());
        }
        tag.put("Records", list);
        return tag;
    }

    public UUID ensureRegistered(CyberNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level)
                || NpcServiceRole.fromRole(npc.getRole())
                == NpcServiceRole.NONE) {
            return npc.getSpecialNpcId();
        }

        UUID specialId = npc.getSpecialNpcId();
        if (specialId == null) {
            specialId = UUID.randomUUID();
            npc.setSpecialNpcId(specialId);
        }

        SpecialNpcRecord record = records.get(specialId);
        Anchor here = new Anchor(
                level.dimension(),
                npc.blockPosition(),
                npc.getYRot()
        );

        if (record == null) {
            record = new SpecialNpcRecord(specialId);
            record.home = here;
            record.work = here;
            records.put(specialId, record);
        }

        record.role = npc.getRole();
        record.name = npc.getCustomName() == null
                ? npc.getRole()
                : npc.getCustomName().getString();
        record.entityId = npc.getUUID();
        record.awaitingRespawn = false;
        record.beamStarted = false;
        record.respawnAt = 0L;
        record.materializeAt = 0L;

        if (record.template.isEmpty()) {
            record.template = captureTemplate(npc);
        }

        setDirty();
        return specialId;
    }

    public void refreshTemplate(CyberNpcEntity npc) {
        UUID specialId = npc.getSpecialNpcId();
        if (specialId == null) {
            return;
        }

        SpecialNpcRecord record = records.get(specialId);
        if (record == null) {
            ensureRegistered(npc);
            record = records.get(specialId);
        }

        if (record != null) {
            record.role = npc.getRole();
            record.name = npc.getCustomName() == null
                    ? npc.getRole()
                    : npc.getCustomName().getString();
            record.template = captureTemplate(npc);
            record.entityId = npc.getUUID();
            setDirty();
        }
    }

    public void scheduleRespawn(CyberNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level)) {
            return;
        }

        UUID specialId = ensureRegistered(npc);
        if (specialId == null) {
            return;
        }

        SpecialNpcRecord record = records.get(specialId);
        if (record == null) {
            return;
        }

        record.template = captureTemplate(npc);
        record.entityId = null;
        record.awaitingRespawn = true;
        record.beamStarted = false;
        record.returningToWork = false;
        record.respawnAt = level.getServer().overworld().getGameTime()
                + RESPAWN_DELAY_TICKS;
        record.materializeAt = 0L;
        record.arrival = null;
        setDirty();
    }

    public void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();

        for (SpecialNpcRecord record : records.values()) {
            if (!record.awaitingRespawn) {
                continue;
            }

            if (!record.beamStarted && now >= record.respawnAt) {
                CyberServerCompat.ArrivalTarget target =
                        CyberServerCompat.resolveArrival(server);

                record.arrival = new Anchor(
                        target.level().dimension(),
                        target.pos(),
                        target.yaw()
                );
                record.beamStarted = true;
                record.materializeAt =
                        now + MATERIALIZE_AFTER_BEAM_TICKS;

                double x = target.pos().getX() + 0.5D;
                double y = target.pos().getY();
                double z = target.pos().getZ() + 0.5D;
                CyberServerCompat.startArrival(
                        target.level(),
                        x,
                        y,
                        z
                );

                setDirty();
                continue;
            }

            if (record.beamStarted
                    && now >= record.materializeAt) {
                materialize(server, record);
            }
        }
    }

    private void materialize(
            MinecraftServer server,
            SpecialNpcRecord record
    ) {
        Anchor arrival = record.arrival;
        if (arrival == null) {
            CyberServerCompat.ArrivalTarget fallback =
                    CyberServerCompat.resolveArrival(server);
            arrival = new Anchor(
                    fallback.level().dimension(),
                    fallback.pos(),
                    fallback.yaw()
            );
        }

        ServerLevel level = server.getLevel(arrival.dimension);
        if (level == null) {
            record.beamStarted = false;
            record.respawnAt = server.overworld().getGameTime() + 40L;
            return;
        }

        CyberNpcEntity npc = ModEntities.CYBER_NPC.get().create(level);
        if (npc == null) {
            record.beamStarted = false;
            record.respawnAt = server.overworld().getGameTime() + 40L;
            return;
        }

        if (!record.template.isEmpty()) {
            npc.load(record.template.copy());
        }

        npc.setSpecialNpcId(record.specialId);
        npc.setRole(record.role);
        if (record.name != null && !record.name.isBlank()) {
            npc.setCustomName(
                    net.minecraft.network.chat.Component.literal(
                            record.name
                    )
            );
            npc.setCustomNameVisible(true);
        }

        npc.moveTo(
                arrival.pos.getX() + 0.5D,
                arrival.pos.getY(),
                arrival.pos.getZ() + 0.5D,
                arrival.yaw,
                0.0F
        );
        npc.setDeltaMovement(Vec3.ZERO);
        npc.fallDistance = 0.0F;
        npc.setHealth(npc.getMaxHealth());
        npc.setPersistenceRequired();

        npc.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(arrival.pos),
                MobSpawnType.EVENT,
                null,
                null
        );

        if (!level.addFreshEntity(npc)) {
            record.beamStarted = false;
            record.respawnAt = server.overworld().getGameTime() + 40L;
            return;
        }

        record.entityId = npc.getUUID();
        record.awaitingRespawn = false;
        record.beamStarted = false;
        record.respawnAt = 0L;
        record.materializeAt = 0L;
        record.returningToWork = true;
        record.arrival = null;
        setDirty();
    }

    @Nullable
    public SpecialNpcRecord getRecord(UUID specialId) {
        return specialId == null ? null : records.get(specialId);
    }

    public boolean removePermanent(UUID specialId) {
        if (specialId == null) {
            return false;
        }

        SpecialNpcRecord removed = records.remove(specialId);
        if (removed == null) {
            return false;
        }

        // Removing the record also cancels any pending beam/respawn because
        // tick() only processes records still present in this map.
        setDirty();
        return true;
    }

    public boolean setHome(
            UUID specialId,
            Anchor anchor
    ) {
        SpecialNpcRecord record = records.get(specialId);
        if (record == null || anchor == null) {
            return false;
        }

        record.home = anchor;
        setDirty();
        return true;
    }

    public boolean setWork(
            UUID specialId,
            Anchor anchor
    ) {
        SpecialNpcRecord record = records.get(specialId);
        if (record == null || anchor == null) {
            return false;
        }

        record.work = anchor;
        record.returningToWork = true;
        setDirty();
        return true;
    }

    public void markReachedWork(UUID specialId) {
        SpecialNpcRecord record = records.get(specialId);
        if (record != null && record.returningToWork) {
            record.returningToWork = false;
            setDirty();
        }
    }

    public Collection<SpecialNpcRecord> records() {
        return records.values();
    }

    private static CompoundTag captureTemplate(CyberNpcEntity npc) {
        CompoundTag tag = new CompoundTag();
        npc.saveWithoutId(tag);

        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Rotation");
        tag.remove("Health");
        tag.remove("HurtTime");
        tag.remove("HurtByTimestamp");
        tag.remove("DeathTime");
        tag.remove("FallDistance");
        tag.remove("Fire");
        tag.remove("Air");

        return tag;
    }

    public static final class SpecialNpcRecord {
        private final UUID specialId;
        private String role = "Citizen";
        private String name = "";
        @Nullable
        private UUID entityId;
        @Nullable
        private Anchor home;
        @Nullable
        private Anchor work;
        private CompoundTag template = new CompoundTag();
        private boolean awaitingRespawn;
        private boolean beamStarted;
        private boolean returningToWork;
        private long respawnAt;
        private long materializeAt;
        @Nullable
        private Anchor arrival;

        private SpecialNpcRecord(UUID specialId) {
            this.specialId = specialId;
        }

        public UUID specialId() {
            return specialId;
        }

        public String role() {
            return role;
        }

        public String name() {
            return name;
        }

        @Nullable
        public UUID entityId() {
            return entityId;
        }

        @Nullable
        public Anchor home() {
            return home;
        }

        @Nullable
        public Anchor work() {
            return work;
        }

        public boolean awaitingRespawn() {
            return awaitingRespawn;
        }

        public boolean returningToWork() {
            return returningToWork;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("SpecialId", specialId);
            tag.putString("Role", role);
            tag.putString("Name", name);

            if (entityId != null) {
                tag.putUUID("EntityId", entityId);
            }
            if (home != null) {
                tag.put("Home", home.save());
            }
            if (work != null) {
                tag.put("Work", work.save());
            }
            if (!template.isEmpty()) {
                tag.put("Template", template.copy());
            }

            tag.putBoolean("AwaitingRespawn", awaitingRespawn);
            tag.putBoolean("BeamStarted", beamStarted);
            tag.putBoolean("ReturningToWork", returningToWork);
            tag.putLong("RespawnAt", respawnAt);
            tag.putLong("MaterializeAt", materializeAt);
            if (arrival != null) {
                tag.put("Arrival", arrival.save());
            }

            return tag;
        }

        @Nullable
        private static SpecialNpcRecord load(CompoundTag tag) {
            if (!tag.hasUUID("SpecialId")) {
                return null;
            }

            SpecialNpcRecord record = new SpecialNpcRecord(
                    tag.getUUID("SpecialId")
            );
            record.role = tag.getString("Role");
            record.name = tag.getString("Name");
            record.entityId = tag.hasUUID("EntityId")
                    ? tag.getUUID("EntityId")
                    : null;
            record.home = tag.contains("Home", Tag.TAG_COMPOUND)
                    ? Anchor.load(tag.getCompound("Home"))
                    : null;
            record.work = tag.contains("Work", Tag.TAG_COMPOUND)
                    ? Anchor.load(tag.getCompound("Work"))
                    : null;
            record.template = tag.contains("Template", Tag.TAG_COMPOUND)
                    ? tag.getCompound("Template").copy()
                    : new CompoundTag();
            record.awaitingRespawn =
                    tag.getBoolean("AwaitingRespawn");
            record.beamStarted = tag.getBoolean("BeamStarted");
            record.returningToWork =
                    tag.getBoolean("ReturningToWork");
            record.respawnAt = tag.getLong("RespawnAt");
            record.materializeAt = tag.getLong("MaterializeAt");
            record.arrival = tag.contains("Arrival", Tag.TAG_COMPOUND)
                    ? Anchor.load(tag.getCompound("Arrival"))
                    : null;
            return record;
        }
    }

    public record Anchor(
            ResourceKey<Level> dimension,
            BlockPos pos,
            float yaw
    ) {
        public Anchor {
            pos = pos.immutable();
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.location().toString());
            tag.putLong("Pos", pos.asLong());
            tag.putFloat("Yaw", yaw);
            return tag;
        }

        @Nullable
        static Anchor load(CompoundTag tag) {
            ResourceLocation id = ResourceLocation.tryParse(
                    tag.getString("Dimension")
            );
            if (id == null || !tag.contains("Pos")) {
                return null;
            }

            return new Anchor(
                    ResourceKey.create(Registries.DIMENSION, id),
                    BlockPos.of(tag.getLong("Pos")),
                    tag.getFloat("Yaw")
            );
        }
    }
}
