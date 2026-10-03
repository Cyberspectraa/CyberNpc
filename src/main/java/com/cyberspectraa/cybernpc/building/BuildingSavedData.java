package com.cyberspectraa.cybernpc.building;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.cyberspectraa.cybernpc.service.SpecialNpcSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BuildingSavedData extends SavedData {
    private static final String DATA_NAME = "cybernpc_buildings";
    public static final long MAX_ZONE_VOLUME = 2_000_000L;
    private static final long MAX_AUTO_SCAN_BLOCKS = 500_000L;

    private final Map<UUID, BuildingRecord> records = new LinkedHashMap<>();
    private int nextNumber = 1;

    private transient boolean indexDirty = true;
    private final transient Map<
            ResourceKey<Level>,
            Map<Long, List<BuildingRecord>>
            > chunkIndex = new HashMap<>();
    private final transient Map<
            ResourceKey<Level>,
            Map<Long, BuildingRecord>
            > entranceIndex = new HashMap<>();

    public static BuildingSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static BuildingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                BuildingSavedData::load,
                BuildingSavedData::new,
                DATA_NAME
        );
    }

    public static BuildingSavedData load(CompoundTag tag) {
        BuildingSavedData data = new BuildingSavedData();
        data.nextNumber = Math.max(1, tag.getInt("NextNumber"));

        ListTag list = tag.getList("Buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            BuildingRecord record = BuildingRecord.load(list.getCompound(i));
            if (record != null) {
                data.records.put(record.id(), record);
            }
        }

        if (data.nextNumber <= data.records.size()) {
            data.nextNumber = data.records.size() + 1;
        }

        data.indexDirty = true;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextNumber", nextNumber);

        ListTag list = new ListTag();
        for (BuildingRecord record : records.values()) {
            list.add(record.save());
        }
        tag.put("Buildings", list);
        return tag;
    }

    public Collection<BuildingRecord> records() {
        return records.values();
    }

    @Nullable
    public BuildingRecord getRecord(UUID id) {
        return id == null ? null : records.get(id);
    }

    public UUID createBuilding(
            ResourceKey<Level> dimension,
            BlockPos core,
            BuildingType type
    ) {
        BuildingType safeType = type == null ? BuildingType.HOME : type;
        UUID id = UUID.randomUUID();
        int number = nextNumber++;

        BuildingRecord record = new BuildingRecord(
                id,
                dimension,
                core.immutable(),
                safeType,
                safeType.displayName() + " #" + number
        );

        records.put(id, record);
        markChanged();
        return id;
    }

    public UUID createMarkerBuilding(
            ResourceKey<Level> dimension,
            UUID markerId,
            BlockPos core,
            BuildingType type,
            RoomScanner.ScanResult scan,
            ServerLevel level
    ) {
        UUID id = createBuilding(dimension, core, type);
        BuildingRecord record = records.get(id);

        if (record == null) {
            return id;
        }

        record.primaryMarkerId = markerId;
        record.zones.clear();
        record.zones.addAll(scan.zones());
        record.entrances.clear();
        record.entrances.addAll(scan.entrances());

        if (type == BuildingType.HOME || type == BuildingType.INN) {
            addDetectedBeds(record, scan.beds(), level, markerId);
        }

        markChanged();
        return id;
    }

    public boolean addMarkerRoom(
            UUID buildingId,
            UUID markerId,
            BuildingMarkerType.RoomKind kind,
            RoomScanner.ScanResult scan,
            ServerLevel level
    ) {
        BuildingRecord record = records.get(buildingId);
        if (record == null
                || markerId == null
                || kind == null
                || kind == BuildingMarkerType.RoomKind.MAIN) {
            return false;
        }

        record.rooms.removeIf(room ->
                room.markerId().equals(markerId)
        );

        record.rooms.add(new RoomRegion(
                markerId,
                kind,
                scan.start(),
                List.copyOf(scan.zones())
        ));
        record.entrances.addAll(scan.entrances());

        if (kind == BuildingMarkerType.RoomKind.BEDROOM) {
            addDetectedBeds(record, scan.beds(), level, markerId);
        }

        markChanged();
        return true;
    }

    public boolean addMarkerPoint(
            UUID buildingId,
            UUID markerId,
            BuildingPointType type,
            BlockPos pos,
            float yaw
    ) {
        BuildingRecord record = records.get(buildingId);
        if (record == null
                || markerId == null
                || type == null
                || pos == null) {
            return false;
        }

        record.points.removeIf(point ->
                markerId.equals(point.markerId())
        );
        record.points.add(new ActivityPoint(
                type,
                pos.immutable(),
                yaw,
                markerId
        ));
        markChanged();
        return true;
    }

    public boolean removeMarker(UUID markerId) {
        if (markerId == null) {
            return false;
        }

        UUID removeBuildingId = null;
        boolean changed = false;

        for (BuildingRecord record : records.values()) {
            if (markerId.equals(record.primaryMarkerId)) {
                removeBuildingId = record.id();
                break;
            }

            changed |= record.rooms.removeIf(room ->
                    markerId.equals(room.markerId())
            );
            changed |= record.points.removeIf(point ->
                    markerId.equals(point.markerId())
            );
        }

        if (removeBuildingId != null) {
            records.remove(removeBuildingId);
            changed = true;
        }

        if (changed) {
            markChanged();
        }

        return changed;
    }

    @Nullable
    public BuildingRecord findBuildingForRoom(
            ResourceKey<Level> dimension,
            Set<BlockPos> roomEntrances,
            BlockPos origin,
            double fallbackRadiusSqr
    ) {
        ensureIndexes();

        for (BlockPos entrance : roomEntrances) {
            BuildingRecord linked =
                    getEntranceBuilding(dimension, entrance);
            if (linked != null) {
                return linked;
            }
        }

        BuildingRecord best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BuildingRecord record : records.values()) {
            if (!record.dimension().equals(dimension)) {
                continue;
            }

            double distance = origin.distSqr(record.core());
            if (distance <= fallbackRadiusSqr
                    && distance < bestDistance) {
                best = record;
                bestDistance = distance;
            }
        }

        return best;
    }

    private void addDetectedBeds(
            BuildingRecord record,
            Set<BlockPos> beds,
            ServerLevel level,
            @Nullable UUID markerId
    ) {
        for (BlockPos bed : beds) {
            record.points.removeIf(existing ->
                    existing.type() == BuildingPointType.BED
                            && existing.pos().equals(bed)
            );

            BlockState state = level.getBlockState(bed);
            float yaw = state.hasProperty(BedBlock.FACING)
                    ? state.getValue(BedBlock.FACING).toYRot()
                    : 0.0F;

            record.points.add(new ActivityPoint(
                    BuildingPointType.BED,
                    bed,
                    yaw,
                    markerId
            ));
        }
    }

    public boolean removeBuilding(UUID id) {
        if (id == null || records.remove(id) == null) {
            return false;
        }

        markChanged();
        return true;
    }

    public boolean setType(UUID id, BuildingType type) {
        BuildingRecord record = records.get(id);
        if (record == null || type == null) {
            return false;
        }

        record.type = type;

        int marker = record.name.lastIndexOf(" #");
        String suffix = marker >= 0
                ? record.name.substring(marker)
                : "";
        record.name = type.displayName() + suffix;

        markChanged();
        return true;
    }

    public ZoneAddResult addZone(
            UUID id,
            BlockPos first,
            BlockPos second
    ) {
        BuildingRecord record = records.get(id);
        if (record == null || first == null || second == null) {
            return ZoneAddResult.NO_BUILDING;
        }

        Zone zone = Zone.between(first, second);
        if (zone.volume() > MAX_ZONE_VOLUME) {
            return ZoneAddResult.TOO_LARGE;
        }

        if (!record.zones.contains(zone)) {
            record.zones.add(zone);
            markChanged();
        }

        return ZoneAddResult.ADDED;
    }

    public boolean toggleEntrance(UUID id, BlockPos pos) {
        BuildingRecord record = records.get(id);
        if (record == null || pos == null) {
            return false;
        }

        BlockPos immutable = pos.immutable();
        boolean added;
        if (record.entrances.remove(immutable)) {
            added = false;
        } else {
            record.entrances.add(immutable);
            added = true;
        }

        markChanged();
        return added;
    }

    public boolean addPoint(
            UUID id,
            BuildingPointType type,
            BlockPos pos,
            float yaw
    ) {
        BuildingRecord record = records.get(id);
        if (record == null || type == null || pos == null) {
            return false;
        }

        ActivityPoint point = new ActivityPoint(
                type,
                pos.immutable(),
                yaw,
                null
        );

        record.points.removeIf(existing ->
                existing.type() == type
                        && existing.pos().equals(pos)
        );
        record.points.add(point);
        markChanged();
        return true;
    }

    public boolean toggleResident(UUID id, CyberNpcEntity npc) {
        BuildingRecord record = records.get(id);
        if (record == null || npc == null) {
            return false;
        }

        UUID identity = npcIdentity(npc);
        boolean added;
        if (record.residents.remove(identity)) {
            added = false;
        } else {
            // One normal home at a time. Removing the NPC from every other
            // resident list prevents conflicting night-time destinations.
            for (BuildingRecord other : records.values()) {
                if (other != record) {
                    other.residents.remove(identity);
                }
            }
            record.residents.add(identity);
            added = true;
        }

        markChanged();
        return added;
    }

    public boolean toggleWorker(UUID id, CyberNpcEntity npc) {
        BuildingRecord record = records.get(id);
        if (record == null || npc == null) {
            return false;
        }

        UUID identity = npcIdentity(npc);
        boolean added;
        if (record.workers.remove(identity)) {
            added = false;
        } else {
            record.workers.add(identity);
            added = true;
        }

        markChanged();
        return added;
    }

    public int rescanEntrances(ServerLevel level, UUID id) {
        BuildingRecord record = records.get(id);
        if (record == null
                || level == null
                || !record.dimension().equals(level.dimension())) {
            return -1;
        }

        long scanVolume = 0L;
        for (Zone zone : record.zones) {
            scanVolume += zone.expandedVolume(1);
            if (scanVolume > MAX_AUTO_SCAN_BLOCKS) {
                return -2;
            }
        }

        Set<BlockPos> found = new LinkedHashSet<>();
        Set<BlockPos> beds = new LinkedHashSet<>();

        for (Zone zone : record.zones) {
            BlockPos min = zone.min().offset(-1, -1, -1);
            BlockPos max = zone.max().offset(1, 1, 1);

            for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
                BlockState state = level.getBlockState(cursor);
                if (state.getBlock() instanceof DoorBlock) {
                    found.add(normalizeDoor(state, cursor));
                } else if (state.getBlock() instanceof FenceGateBlock) {
                    found.add(cursor.immutable());
                } else if (state.getBlock() instanceof BedBlock
                        && state.hasProperty(BedBlock.PART)
                        && state.getValue(BedBlock.PART) == BedPart.HEAD) {
                    beds.add(cursor.immutable());
                }
            }
        }

        record.entrances.clear();
        record.entrances.addAll(found);

        // Beds are a common building feature and should not need a separate
        // planner mode. Keep any manually placed semantic points, but ensure
        // every real bed head found inside the building has a BED point.
        for (BlockPos bed : beds) {
            boolean alreadyKnown = record.points.stream().anyMatch(
                    point -> point.type() == BuildingPointType.BED
                            && point.pos().equals(bed)
            );

            if (!alreadyKnown) {
                BlockState state = level.getBlockState(bed);
                float yaw = state.hasProperty(BedBlock.FACING)
                        ? state.getValue(BedBlock.FACING).toYRot()
                        : 0.0F;
                record.points.add(new ActivityPoint(
                        BuildingPointType.BED,
                        bed,
                        yaw,
                        null
                ));
            }
        }

        markChanged();
        return found.size();
    }

    @Nullable
    public BuildingRecord getBuildingAt(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        if (dimension == null || pos == null) {
            return null;
        }

        ensureIndexes();

        Map<Long, List<BuildingRecord>> byChunk =
                chunkIndex.get(dimension);
        if (byChunk != null) {
            long chunkKey = chunkKey(pos);
            List<BuildingRecord> candidates = byChunk.get(chunkKey);
            if (candidates != null) {
                for (BuildingRecord record : candidates) {
                    if (record.contains(pos)) {
                        return record;
                    }
                }
            }
        }

        // A just-created building has a core before it has its first zone.
        for (BuildingRecord record : records.values()) {
            if (record.dimension().equals(dimension)
                    && record.core().equals(pos)) {
                return record;
            }
        }

        return null;
    }

    @Nullable
    public BuildingRecord getEntranceBuilding(
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        if (dimension == null || pos == null) {
            return null;
        }

        ensureIndexes();
        Map<Long, BuildingRecord> byPos = entranceIndex.get(dimension);
        if (byPos == null) {
            return null;
        }

        BuildingRecord direct = byPos.get(pos.asLong());
        if (direct != null) {
            return direct;
        }

        direct = byPos.get(pos.below().asLong());
        if (direct != null) {
            return direct;
        }

        return byPos.get(pos.above().asLong());
    }

    public boolean canEnter(
            ServerLevel level,
            CyberNpcEntity npc,
            BuildingRecord record
    ) {
        if (level == null || npc == null || record == null) {
            return true;
        }

        if (!record.dimension().equals(level.dimension())) {
            return true;
        }

        if (record.type().isPublicAccess()) {
            return true;
        }

        UUID identity = npcIdentity(npc);
        if (record.residents.contains(identity)
                || record.workers.contains(identity)) {
            return true;
        }

        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        if (record.type() == BuildingType.GUARD_HOUSE
                && role == NpcServiceRole.GUARD) {
            return true;
        }

        UUID specialId = npc.getSpecialNpcId();
        if (specialId != null) {
            SpecialNpcSavedData.SpecialNpcRecord special =
                    SpecialNpcSavedData.get(level).getRecord(specialId);

            if (special != null) {
                if (special.home() != null
                        && special.home().dimension().equals(record.dimension())
                        && record.contains(special.home().pos())) {
                    return true;
                }

                if (special.work() != null
                        && special.work().dimension().equals(record.dimension())
                        && record.contains(special.work().pos())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canUseEntrance(
            ServerLevel level,
            CyberNpcEntity npc,
            BlockPos doorPos
    ) {
        BuildingRecord entrance =
                getEntranceBuilding(level.dimension(), doorPos);

        if (entrance == null) {
            return true;
        }

        BuildingRecord current =
                getBuildingAt(level.dimension(), npc.blockPosition());

        // Always let an NPC leave the building it is already inside.
        if (current != null && current.id().equals(entrance.id())) {
            return true;
        }

        return canEnter(level, npc, entrance);
    }

    public boolean canStandAt(
            ServerLevel level,
            CyberNpcEntity npc,
            BlockPos pos
    ) {
        BuildingRecord target =
                getBuildingAt(level.dimension(), pos);
        if (target == null) {
            return true;
        }

        BuildingRecord current =
                getBuildingAt(level.dimension(), npc.blockPosition());

        if (current != null && current.id().equals(target.id())) {
            return true;
        }

        return canEnter(level, npc, target);
    }

    @Nullable
    public BuildingRecord findResidentBuilding(
            ServerLevel level,
            CyberNpcEntity npc
    ) {
        if (level == null || npc == null) {
            return null;
        }

        UUID identity = npcIdentity(npc);
        for (BuildingRecord record : records.values()) {
            if (record.dimension().equals(level.dimension())
                    && record.residents.contains(identity)) {
                return record;
            }
        }

        return null;
    }

    @Nullable
    public BuildingRecord findWorkerBuilding(
            ServerLevel level,
            CyberNpcEntity npc
    ) {
        if (level == null || npc == null) {
            return null;
        }

        UUID identity = npcIdentity(npc);
        for (BuildingRecord record : records.values()) {
            if (record.dimension().equals(level.dimension())
                    && record.workers.contains(identity)) {
                return record;
            }
        }

        return null;
    }

    @Nullable
    public BuildingRecord findNearest(
            ResourceKey<Level> dimension,
            BuildingType type,
            BlockPos origin,
            double radiusSqr
    ) {
        BuildingRecord best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BuildingRecord record : records.values()) {
            if (!record.dimension().equals(dimension)
                    || record.type() != type) {
                continue;
            }

            double distance = origin.distSqr(record.core());
            if (distance <= radiusSqr && distance < bestDistance) {
                best = record;
                bestDistance = distance;
            }
        }

        return best;
    }

    @Nullable
    public ActivityPoint nearestPoint(
            BuildingRecord record,
            BuildingPointType type,
            BlockPos origin
    ) {
        if (record == null || type == null || origin == null) {
            return null;
        }

        ActivityPoint best = null;
        double bestDistance = Double.MAX_VALUE;

        for (ActivityPoint point : record.points) {
            if (point.type() != type) {
                continue;
            }

            double distance = origin.distSqr(point.pos());
            if (distance < bestDistance) {
                best = point;
                bestDistance = distance;
            }
        }

        return best;
    }

    @Nullable
    public BlockPos randomInteriorTarget(
            ServerLevel level,
            BuildingRecord record,
            RandomSource random
    ) {
        if (level == null
                || record == null
                || random == null
                || record.zones.isEmpty()
                || !record.dimension().equals(level.dimension())) {
            return null;
        }

        for (int attempt = 0; attempt < 24; attempt++) {
            Zone zone = record.zones.get(
                    random.nextInt(record.zones.size())
            );

            int x = randomBetween(random, zone.min().getX(), zone.max().getX());
            int z = randomBetween(random, zone.min().getZ(), zone.max().getZ());
            int yStart = randomBetween(
                    random,
                    zone.min().getY(),
                    zone.max().getY()
            );
            int height = zone.max().getY() - zone.min().getY() + 1;

            for (int yOffset = 0; yOffset < height; yOffset++) {
                int y = zone.min().getY()
                        + Math.floorMod(
                        yStart - zone.min().getY() + yOffset,
                        height
                );

                BlockPos candidate = new BlockPos(x, y, z);
                if (isStandable(level, candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static boolean isStandable(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.hasChunkAt(pos)
                || !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.above()).isEmpty()) {
            return false;
        }

        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockPos supportPos = pos.below();
        BlockState support = level.getBlockState(supportPos);

        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && support.isFaceSturdy(
                level,
                supportPos,
                Direction.UP
        );
    }

    private static int randomBetween(
            RandomSource random,
            int min,
            int max
    ) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static BlockPos normalizeDoor(
            BlockState state,
            BlockPos pos
    ) {
        if (state.getBlock() instanceof DoorBlock
                && state.hasProperty(
                BlockStateProperties.DOUBLE_BLOCK_HALF
        )
                && state.getValue(
                BlockStateProperties.DOUBLE_BLOCK_HALF
        ) == DoubleBlockHalf.UPPER) {
            return pos.below().immutable();
        }

        return pos.immutable();
    }

    public static UUID npcIdentity(CyberNpcEntity npc) {
        UUID specialId = npc.getSpecialNpcId();
        return specialId == null ? npc.getUUID() : specialId;
    }

    private void markChanged() {
        indexDirty = true;
        setDirty();
    }

    private void ensureIndexes() {
        if (!indexDirty) {
            return;
        }

        chunkIndex.clear();
        entranceIndex.clear();

        for (BuildingRecord record : records.values()) {
            Map<Long, List<BuildingRecord>> dimensionChunks =
                    chunkIndex.computeIfAbsent(
                            record.dimension(),
                            ignored -> new HashMap<>()
                    );

            for (Zone zone : record.zones) {
                int minChunkX = zone.min().getX() >> 4;
                int maxChunkX = zone.max().getX() >> 4;
                int minChunkZ = zone.min().getZ() >> 4;
                int maxChunkZ = zone.max().getZ() >> 4;

                for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                    for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                        long key = chunkKey(cx, cz);
                        List<BuildingRecord> candidates =
                                dimensionChunks.computeIfAbsent(
                                        key,
                                        ignored -> new ArrayList<>()
                                );
                        if (!candidates.contains(record)) {
                            candidates.add(record);
                        }
                    }
                }
            }

            Map<Long, BuildingRecord> dimensionEntrances =
                    entranceIndex.computeIfAbsent(
                            record.dimension(),
                            ignored -> new HashMap<>()
                    );

            for (BlockPos entrance : record.entrances) {
                dimensionEntrances.put(entrance.asLong(), record);
            }
        }

        indexDirty = false;
    }

    private static long chunkKey(BlockPos pos) {
        return chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static long chunkKey(int x, int z) {
        return ((long) x & 0xffffffffL)
                | (((long) z & 0xffffffffL) << 32);
    }

    public enum ZoneAddResult {
        ADDED,
        TOO_LARGE,
        NO_BUILDING
    }

    public static final class BuildingRecord {
        private final UUID id;
        private final ResourceKey<Level> dimension;
        private final BlockPos core;
        private BuildingType type;
        private String name;

        private final List<Zone> zones = new ArrayList<>();
        private final Set<BlockPos> entrances = new LinkedHashSet<>();
        private final List<ActivityPoint> points = new ArrayList<>();
        private final Set<UUID> residents = new LinkedHashSet<>();
        private final Set<UUID> workers = new LinkedHashSet<>();

        private BuildingRecord(
                UUID id,
                ResourceKey<Level> dimension,
                BlockPos core,
                BuildingType type,
                String name
        ) {
            this.id = id;
            this.dimension = dimension;
            this.core = core.immutable();
            this.type = type;
            this.name = name == null ? type.displayName() : name;
        }

        public UUID id() {
            return id;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public BlockPos core() {
            return core;
        }

        public BuildingType type() {
            return type;
        }

        public String name() {
            return name;
        }

        public List<Zone> zones() {
            return List.copyOf(zones);
        }

        public Set<BlockPos> entrances() {
            return Set.copyOf(entrances);
        }

        public List<ActivityPoint> points() {
            return List.copyOf(points);
        }

        public int residentCount() {
            return residents.size();
        }

        public int workerCount() {
            return workers.size();
        }

        public boolean contains(BlockPos pos) {
            for (Zone zone : zones) {
                if (zone.contains(pos)) {
                    return true;
                }
            }
            return false;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putString("Dimension", dimension.location().toString());
            tag.putLong("Core", core.asLong());
            tag.putString("Type", type.serializedName());
            tag.putString("Name", name);

            ListTag zoneList = new ListTag();
            for (Zone zone : zones) {
                zoneList.add(zone.save());
            }
            tag.put("Zones", zoneList);

            long[] entranceArray = new long[entrances.size()];
            int entranceIndex = 0;
            for (BlockPos entrance : entrances) {
                entranceArray[entranceIndex++] = entrance.asLong();
            }
            tag.putLongArray("Entrances", entranceArray);

            ListTag pointList = new ListTag();
            for (ActivityPoint point : points) {
                pointList.add(point.save());
            }
            tag.put("Points", pointList);

            tag.put("Residents", saveUuidSet(residents));
            tag.put("Workers", saveUuidSet(workers));
            return tag;
        }

        @Nullable
        private static BuildingRecord load(CompoundTag tag) {
            if (!tag.hasUUID("Id") || !tag.contains("Core")) {
                return null;
            }

            ResourceLocation dimensionId =
                    ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimensionId == null) {
                return null;
            }

            UUID id = tag.getUUID("Id");
            BuildingType type = BuildingType.fromSerializedName(
                    tag.getString("Type")
            );
            String name = tag.getString("Name");

            BuildingRecord record = new BuildingRecord(
                    id,
                    ResourceKey.create(Registries.DIMENSION, dimensionId),
                    BlockPos.of(tag.getLong("Core")),
                    type,
                    name.isBlank() ? type.displayName() : name
            );

            ListTag zones = tag.getList("Zones", Tag.TAG_COMPOUND);
            for (int i = 0; i < zones.size(); i++) {
                Zone zone = Zone.load(zones.getCompound(i));
                if (zone != null) {
                    record.zones.add(zone);
                }
            }

            for (long packed : tag.getLongArray("Entrances")) {
                record.entrances.add(BlockPos.of(packed));
            }

            ListTag points = tag.getList("Points", Tag.TAG_COMPOUND);
            for (int i = 0; i < points.size(); i++) {
                ActivityPoint point =
                        ActivityPoint.load(points.getCompound(i));
                if (point != null) {
                    record.points.add(point);
                }
            }

            loadUuidSet(tag.getList(
                    "Residents",
                    Tag.TAG_COMPOUND
            ), record.residents);
            loadUuidSet(tag.getList(
                    "Workers",
                    Tag.TAG_COMPOUND
            ), record.workers);

            return record;
        }
    }

    public record Zone(BlockPos min, BlockPos max) {
        public Zone {
            min = min.immutable();
            max = max.immutable();
        }

        public static Zone between(BlockPos first, BlockPos second) {
            return new Zone(
                    new BlockPos(
                            Math.min(first.getX(), second.getX()),
                            Math.min(first.getY(), second.getY()),
                            Math.min(first.getZ(), second.getZ())
                    ),
                    new BlockPos(
                            Math.max(first.getX(), second.getX()),
                            Math.max(first.getY(), second.getY()),
                            Math.max(first.getZ(), second.getZ())
                    )
            );
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX()
                    && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY()
                    && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ()
                    && pos.getZ() <= max.getZ();
        }

        public long volume() {
            return sizeX() * sizeY() * sizeZ();
        }

        private long expandedVolume(int amount) {
            return (sizeX() + amount * 2L)
                    * (sizeY() + amount * 2L)
                    * (sizeZ() + amount * 2L);
        }

        private long sizeX() {
            return (long) max.getX() - min.getX() + 1L;
        }

        private long sizeY() {
            return (long) max.getY() - min.getY() + 1L;
        }

        private long sizeZ() {
            return (long) max.getZ() - min.getZ() + 1L;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Min", min.asLong());
            tag.putLong("Max", max.asLong());
            return tag;
        }

        @Nullable
        private static Zone load(CompoundTag tag) {
            if (!tag.contains("Min") || !tag.contains("Max")) {
                return null;
            }
            return between(
                    BlockPos.of(tag.getLong("Min")),
                    BlockPos.of(tag.getLong("Max"))
            );
        }
    }

    public record ActivityPoint(
            BuildingPointType type,
            BlockPos pos,
            float yaw
    ) {
        public ActivityPoint {
            pos = pos.immutable();
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Type", type.serializedName());
            tag.putLong("Pos", pos.asLong());
            tag.putFloat("Yaw", yaw);
            return tag;
        }

        @Nullable
        private static ActivityPoint load(CompoundTag tag) {
            if (!tag.contains("Pos")) {
                return null;
            }

            return new ActivityPoint(
                    BuildingPointType.fromSerializedName(
                            tag.getString("Type")
                    ),
                    BlockPos.of(tag.getLong("Pos")),
                    tag.getFloat("Yaw")
            );
        }
    }

    private static ListTag saveUuidSet(Set<UUID> values) {
        ListTag list = new ListTag();
        for (UUID value : values) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", value);
            list.add(entry);
        }
        return list;
    }

    private static void loadUuidSet(
            ListTag list,
            Set<UUID> destination
    ) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("Id")) {
                destination.add(entry.getUUID("Id"));
            }
        }
    }
}
