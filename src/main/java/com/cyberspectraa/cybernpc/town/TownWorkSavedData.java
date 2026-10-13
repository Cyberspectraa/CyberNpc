package com.cyberspectraa.cybernpc.town;

import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * New opt-in workplace registry. Stored SEPARATELY from the legacy building
 * marker registry and town homes. One workstation has exactly one reserved
 * employee; a missing/unloaded worker does NOT free someone else's job.
 *
 * All calls that mutate saved data occur on Minecraft's server thread.
 */
public final class TownWorkSavedData extends SavedData {
    private static final String KEY = "cybernpc_town_work_v1";
    private static final double CLAIM_RADIUS_SQR = 96.0D * 96.0D;
    private static final double DISPLAY_RANGE_SQR = 28.0D * 28.0D;
    private static final DustParticleOptions HIGHLIGHT =
            new DustParticleOptions(new Vector3f(1.0F, 0.76F, 0.22F), 1.1F);
    private final Map<String, Station> stations = new LinkedHashMap<>();

    public static TownWorkSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                TownWorkSavedData::load, TownWorkSavedData::new, KEY);
    }

    private static TownWorkSavedData load(CompoundTag tag) {
        TownWorkSavedData data = new TownWorkSavedData();
        ListTag list = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Town") || !entry.contains("Dimension", Tag.TAG_STRING)
                    || !entry.contains("Job", Tag.TAG_STRING)) continue;
            String dimension = entry.getString("Dimension");
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            Station station = new Station(entry.getUUID("Town"), dimension,
                    pos, BlockPos.of(entry.getLong("Stand")), entry.getString("Job"));
            if (entry.hasUUID("Worker")) station.worker = entry.getUUID("Worker");
            data.stations.put(key(dimension, pos), station);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Station station : stations.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Town", station.townId);
            entry.putString("Dimension", station.dimension);
            entry.putLong("Pos", station.pos.asLong());
            entry.putLong("Stand", station.stand.asLong());
            entry.putString("Job", station.job);
            if (station.worker != null) entry.putUUID("Worker", station.worker);
            list.add(entry);
        }
        tag.put("Stations", list);
        return tag;
    }

    private static String key(String dimension, BlockPos pos) {
        return dimension + ":" + pos.asLong();
    }

    private static String dimension(ServerLevel level) {
        return level.dimension().location().toString();
    }

    @Nullable
    private static BlockPos standNear(ServerLevel level, BlockPos station, BlockPos visitor) {
        BlockPos best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = station.relative(direction);
            // Never load a new chunk just to register/visit a workplace.
            if (level.getChunkSource().getChunkNow(next.getX() >> 4, next.getZ() >> 4) == null) continue;
            if (canStand(level, next)) {
                double score = next.distSqr(visitor);
                if (score < bestDistance) {
                    bestDistance = score;
                    best = next;
                }
            }
        }
        return best;
    }

    private static boolean canStand(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockPos below = pos.below();
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null
                || level.getChunkSource().getChunkNow(below.getX() >> 4, below.getZ() >> 4) == null) return false;
        return level.getFluidState(pos).isEmpty() && level.getFluidState(above).isEmpty()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty()
                && !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    private boolean stationStillExists(ServerLevel level, Station station) {
        if (level.getChunkSource().getChunkNow(station.pos.getX() >> 4,
                station.pos.getZ() >> 4) == null) return true;
        String kind = TownJobTypes.forBlock(level.getBlockState(station.pos));
        return station.job.equals(kind);
    }

    /** Normal Town Register click in Work Mode creates or inspects a job. */
    public String register(ServerLevel level, BlockPos pos, BlockPos visitor) {
        String dim = dimension(level);
        TownHomeSavedData homes = TownHomeSavedData.get(level);
        UUID town = homes.townIdNear(level, pos);
        if (town == null) return "Register a town bell within 256 blocks first.";
        String job = TownJobTypes.forBlock(level.getBlockState(pos));
        if (job == null) return "That block is not a supported workstation.";
        String key = key(dim, pos);
        Station existing = stations.get(key);
        if (existing != null && !stationStillExists(level, existing)) {
            stations.remove(key);
            existing = null;
        }
        if (existing != null) {
            String current = existing.worker == null ? "vacant" : "occupied";
            return existing.job + " workstation: " + current + ". "
                    + "Sneak-click with a selected housed Main NPC to assign/unassign.";
        }
        BlockPos stand = standNear(level, pos, visitor);
        if (stand == null) return "No free, solid-floor space beside this workstation. "
                + "Leave at least one walkable block beside it.";
        Station station = new Station(town, dim, pos.immutable(), stand.immutable(), job);
        stations.put(key, station);
        setDirty();
        return job + " workstation registered. An unemployed resident of this "
                + "town will eventually claim it, or sneak-click with a selected NPC.";
    }

    /** Sneak-click in Work Mode assigns a chosen, housed Main NPC. */
    public String assign(ServerLevel level, BlockPos pos, @Nullable CyberNpcEntity npc) {
        Station station = stations.get(key(dimension(level), pos));
        if (station == null) return "Register this workstation with a normal click first.";
        if (!stationStillExists(level, station)) {
            stations.remove(key(dimension(level), pos));
            setDirty();
            return "The workstation changed or was removed. Register it again.";
        }
        if (npc == null || !eligibleResident(level, station, npc))
            return "Select a living ordinary Main NPC who owns a registered home in this town.";
        if (station.worker != null && station.worker.equals(npc.getUUID())) {
            station.worker = null;
            setDirty();
            return npc.getName().getString() + " left this " + station.job
                    + " job. The vacancy can be claimed again.";
        }
        if (station.worker != null) return "This workstation already has an employee. "
                + "Select its current worker and sneak-click to release the job.";
        if (BuildingSavedData.get(level).findAssignedWork(level, npc) != null) {
            return "This NPC has a legacy Work-marker assignment. That assignment was preserved.";
        }
        // Transferring to another new-style station is an explicit manual action.
        for (Station other : stations.values()) {
            if (npc.getUUID().equals(other.worker)) other.worker = null;
        }
        station.worker = npc.getUUID();
        setDirty();
        return npc.getName().getString() + " is now employed as "
                + station.job + ". Their home and existing NPC identity are unchanged.";
    }

    private static boolean eligibleResident(ServerLevel level, Station station, CyberNpcEntity npc) {
        return npc.isAlive() && npc.getNpcType() == NpcType.MAIN
                && !npc.isSpecialServiceNpc()
                && level == npc.level()
                && station.townId.equals(TownHomeSavedData.get(level)
                        .residentTownId(level, npc.getUUID()));
    }

    /** Scheduled once per minute; hire at most ONE nearby loaded resident. */
    public void autoHireOne(ServerLevel level) {
        if (level.getGameTime() < 400L || stations.isEmpty()) return;
        String dim = dimension(level);
        for (Station station : new ArrayList<>(stations.values())) {
            if (!station.dimension.equals(dim)) continue;
            if (level.getChunkSource().getChunkNow(station.pos.getX() >> 4,
                    station.pos.getZ() >> 4) == null) continue;
            if (!stationStillExists(level, station)) {
                stations.remove(key(dim, station.pos));
                setDirty();
                continue;
            }
            if (station.worker != null) continue;
            var area = new AABB(station.stand).inflate(96.0D, 32.0D, 96.0D);
            CyberNpcEntity nearest = null;
            double distance = CLAIM_RADIUS_SQR;
            for (CyberNpcEntity npc : level.getEntitiesOfClass(CyberNpcEntity.class, area)) {
                if (!eligibleResident(level, station, npc)
                        || BuildingSavedData.get(level).findAssignedWork(level, npc) != null
                        || hasNewJob(npc.getUUID())) continue;
                double d = npc.blockPosition().distSqr(station.stand);
                if (d < distance) {
                    distance = d;
                    nearest = npc;
                }
            }
            if (nearest != null) {
                station.worker = nearest.getUUID();
                setDirty();
                return;
            }
        }
    }

    private boolean hasNewJob(UUID worker) {
        for (Station site : stations.values()) if (worker.equals(site.worker)) return true;
        return false;
    }

    @Nullable
    public JobAssignment assignedJob(ServerLevel level, UUID worker) {
        if (worker == null || stations.isEmpty()) return null;
        String dim = dimension(level);
        for (Station station : stations.values()) {
            if (!worker.equals(station.worker) || !dim.equals(station.dimension)) continue;
            if (level.getChunkSource().getChunkNow(station.pos.getX() >> 4,
                    station.pos.getZ() >> 4) == null) return null;
            if (!stationStillExists(level, station)) return null;
            if (!canStand(level, station.stand)) return null;
            return new JobAssignment(station.pos, station.stand, station.job);
        }
        return null;
    }

    public record JobAssignment(BlockPos station, BlockPos stand, String job) {}

    public void onNpcDeath(CyberNpcEntity npc) {
        boolean changed = false;
        for (Station station : stations.values()) {
            if (npc.getUUID().equals(station.worker)) {
                station.worker = null;
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    /** Small, rate-limited amber workstation pips, visible only with Register. */
    public void highlight(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int shown = 0;
        for (Station station : stations.values()) {
            if (!station.dimension.equals(dimension(level))
                    || player.blockPosition().distSqr(station.pos) > DISPLAY_RANGE_SQR
                    || level.getChunkSource().getChunkNow(station.pos.getX() >> 4,
                    station.pos.getZ() >> 4) == null) continue;
            level.sendParticles(player, HIGHLIGHT, true,
                    station.pos.getX() + 0.5D, station.pos.getY() + 1.12D,
                    station.pos.getZ() + 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (++shown >= 8) break;
        }
    }

    private static final class Station {
        final UUID townId;
        final String dimension;
        final BlockPos pos;
        final BlockPos stand;
        final String job;
        @Nullable UUID worker;
        Station(UUID townId, String dimension, BlockPos pos, BlockPos stand, String job) {
            this.townId = townId;
            this.dimension = dimension;
            this.pos = pos;
            this.stand = stand;
            this.job = job;
        }
    }
    private TownWorkSavedData() {}
}
