package com.cyberspectraa.cybernpc.town;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.CyberNpcNameGenerator;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.InteractionHand;
import com.cyberspectraa.cybernpc.item.TownRegisterItem;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.UUID;

/**
 * New, opt-in town/home registry. Stored separately from the pre-0.44.46
 * building marker data: upgrading must not erase or move existing residents.
 * Each registered resident UUID is durable even while its entity is unloaded.
 */
public final class TownHomeSavedData extends SavedData {
    private static final String KEY = "cybernpc_town_homes_v1";
    // Broad enough to assign homes in sprawling player-built settlements.
    private static final double TOWN_RADIUS_SQR = 256.0D * 256.0D;
    private static final double VISIBLE_DOOR_RADIUS_SQR = 28.0D * 28.0D;
    private static final int MAX_HIGHLIGHTED_DOORS = 8;
    private static final DustParticleOptions CLAIMED_DOOR_GLOW =
            new DustParticleOptions(new Vector3f(0.22F, 0.95F, 0.65F), 1.25F);
    private final Map<UUID, Town> towns = new LinkedHashMap<>();
    private final Map<UUID, Home> homes = new LinkedHashMap<>();
    // Per-NPC floor rooms share the same saved housing registry; legacy
    // front-door homes remain intact and act as fallback until changed.
    private final Map<UUID, RoomHome> roomHomes = new LinkedHashMap<>();
    private static final int MAX_ROOMS_PER_NPC = 10;
    private int nextTown = 1;
    private int nextHome = 1;

    public static TownHomeSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                TownHomeSavedData::load, TownHomeSavedData::new, KEY);
    }

    private static TownHomeSavedData load(CompoundTag tag) {
        TownHomeSavedData data = new TownHomeSavedData();
        data.nextTown = Math.max(1, tag.getInt("NextTown"));
        data.nextHome = Math.max(1, tag.getInt("NextHome"));
        ListTag townList = tag.getList("Towns", Tag.TAG_COMPOUND);
        for (int i = 0; i < townList.size(); i++) {
            CompoundTag t = townList.getCompound(i);
            if (!t.hasUUID("Id")) continue;
            Town town = new Town(t.getUUID("Id"), t.getString("Dimension"),
                    BlockPos.of(t.getLong("Bell")), t.getString("Name"));
            town.populate = t.getBoolean("Populate");
            data.towns.put(town.id, town);
        }
        ListTag homeList = tag.getList("Homes", Tag.TAG_COMPOUND);
        for (int i = 0; i < homeList.size(); i++) {
            CompoundTag t = homeList.getCompound(i);
            if (!t.hasUUID("Id") || !t.hasUUID("Town")
                    || !data.towns.containsKey(t.getUUID("Town"))) continue;
            Home home = new Home(t.getUUID("Id"), t.getUUID("Town"),
                    BlockPos.of(t.getLong("Door")), BlockPos.of(t.getLong("Inside")),
                    t.getString("Kind"), Math.max(1, t.getInt("Target")));
            ListTag beds = t.getList("Beds", Tag.TAG_LONG);
            for (int b = 0; b < beds.size(); b++) {
                home.beds.add(((net.minecraft.nbt.LongTag) beds.get(b)).getAsLong());
            }
            ListTag residents = t.getList("Residents", Tag.TAG_COMPOUND);
            for (int b = 0; b < residents.size(); b++) {
                CompoundTag resident = residents.getCompound(b);
                if (resident.hasUUID("Npc")) {
                    home.residents.put(resident.getUUID("Npc"),
                            resident.getLong("Bed"));
                }
            }
            data.homes.put(home.id, home);
        }
        // Added as an optional field: existing worlds keep their original
        // front-door houses, beds, residents and town references unchanged.
        ListTag roomsByNpc = tag.getList("NpcRoomHomes", Tag.TAG_COMPOUND);
        for (int i = 0; i < roomsByNpc.size(); i++) {
            CompoundTag row = roomsByNpc.getCompound(i);
            if (!row.hasUUID("Npc")) continue;
            RoomHome roomHome = new RoomHome(row.getString("Dimension"));
            ListTag claims = row.getList("Rooms", Tag.TAG_COMPOUND);
            for (int k = 0; k < claims.size() && k < MAX_ROOMS_PER_NPC; k++) {
                CompoundTag room = claims.getCompound(k);
                if (!room.contains("Inside", Tag.TAG_LONG)) continue;
                Set<Long> floor = readCellSet(room, "Floor", 1600);
                if (floor.isEmpty()) continue;
                roomHome.rooms.add(new RoomClaim(BlockPos.of(room.getLong("Inside")),
                        floor, readCellSet(room, "Doors", 32),
                        readCellSet(room, "Beds", 1600)));
            }
            if (row.contains("AssignedBed", Tag.TAG_LONG)) {
                roomHome.assignedBed = row.getLong("AssignedBed");
            }
            if (!roomHome.rooms.isEmpty() && !roomHome.dimension.isBlank()) {
                data.roomHomes.put(row.getUUID("Npc"), roomHome);
            }
        }
        data.nextTown = Math.max(data.nextTown, data.towns.size() + 1);
        data.nextHome = Math.max(data.nextHome, data.homes.size() + 1);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextTown", nextTown);
        tag.putInt("NextHome", nextHome);
        ListTag townList = new ListTag();
        for (Town town : towns.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", town.id);
            t.putString("Dimension", town.dimension);
            t.putLong("Bell", town.bell.asLong());
            t.putString("Name", town.name);
            t.putBoolean("Populate", town.populate);
            townList.add(t);
        }
        tag.put("Towns", townList);
        ListTag homeList = new ListTag();
        for (Home home : homes.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", home.id);
            t.putUUID("Town", home.townId);
            t.putLong("Door", home.door.asLong());
            t.putLong("Inside", home.inside.asLong());
            t.putString("Kind", home.kind);
            t.putInt("Target", home.target);
            ListTag beds = new ListTag();
            for (long bed : home.beds) beds.add(net.minecraft.nbt.LongTag.valueOf(bed));
            t.put("Beds", beds);
            ListTag people = new ListTag();
            for (Map.Entry<UUID, Long> entry : home.residents.entrySet()) {
                CompoundTag person = new CompoundTag();
                person.putUUID("Npc", entry.getKey());
                person.putLong("Bed", entry.getValue());
                people.add(person);
            }
            t.put("Residents", people);
            homeList.add(t);
        }
        tag.put("Homes", homeList);
        ListTag roomHomesTag = new ListTag();
        for (Map.Entry<UUID, RoomHome> entry : roomHomes.entrySet()) {
            RoomHome home = entry.getValue();
            if (home.rooms.isEmpty()) continue;
            CompoundTag row = new CompoundTag();
            row.putUUID("Npc", entry.getKey());
            row.putString("Dimension", home.dimension);
            if (home.assignedBed != null) row.putLong("AssignedBed", home.assignedBed);
            ListTag claims = new ListTag();
            for (RoomClaim room : home.rooms) {
                CompoundTag claim = new CompoundTag();
                claim.putLong("Inside", room.inside.asLong());
                claim.putLongArray("Floor", toArray(room.floor));
                claim.putLongArray("Doors", toArray(room.doors));
                claim.putLongArray("Beds", toArray(room.beds));
                claims.add(claim);
            }
            row.put("Rooms", claims);
            roomHomesTag.add(row);
        }
        tag.put("NpcRoomHomes", roomHomesTag);
        return tag;
    }

    /**
     * Pure saved-position check used by BOTH Forge's natural spawn placement
     * rule and CyberNpc's supplemental expedition generator. No world scans,
     * chunk loading or changes to legacy resident data.
     */
    public boolean blocksNaturalWildSpawn(ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().location().toString();

        for (Town town : towns.values()) {
            if (town.dimension.equals(dimension)
                    && TownWildSpawnProtection.withinHorizontalRange(
                            pos, town.bell, TownWildSpawnProtection.TOWN_BELL_RADIUS)) {
                return true;
            }
        }
        for (Home home : homes.values()) {
            Town town = towns.get(home.townId);
            if (town != null && town.dimension.equals(dimension)
                    && TownWildSpawnProtection.withinHorizontalRange(
                            pos, home.door, TownWildSpawnProtection.REGISTERED_HOME_RADIUS)) {
                return true;
            }
        }
        return false;
    }

    /** Bell click creates a town; sneak click toggles controlled population. */
    public String useBell(ServerLevel level, BlockPos bell, boolean sneaking) {
        Town town = null;
        for (Town candidate : towns.values()) {
            if (candidate.dimension.equals(level.dimension().location().toString())
                    && candidate.bell.equals(bell)) {
                town = candidate;
                break;
            }
        }
        if (town == null) {
            if (sneaking) return "Click this bell normally to establish the town first.";
            town = new Town(UUID.randomUUID(), level.dimension().location().toString(),
                    bell.immutable(), "Town #" + nextTown++);
            towns.put(town.id, town);
            setDirty();
            return town.name + " established. Click a home's front door with the Town Register.";
        }
        if (sneaking) {
            town.populate = !town.populate;
            setDirty();
        }
        int houseCount = 0;
        int beds = 0;
        int people = 0;
        for (Home home : homes.values()) {
            if (!home.townId.equals(town.id)) continue;
            houseCount++;
            beds += home.beds.size();
            people += home.residents.size();
        }
        return town.name + " | Houses: " + houseCount + " | Beds: " + beds
                + " | Residents: " + people + " | Auto population: "
                + (town.populate ? "ON" : "OFF")
                + (sneaking ? " (changed)" : " (sneak-click bell to toggle)");
    }

    @Nullable
    private Town nearestTown(ServerLevel level, BlockPos pos) {
        Town best = null;
        double distance = TOWN_RADIUS_SQR;
        for (Town candidate : towns.values()) {
            if (!candidate.dimension.equals(level.dimension().location().toString())) continue;
            double d = candidate.bell.distSqr(pos);
            if (d < distance) {
                distance = d;
                best = candidate;
            }
        }
        return best;
    }

    public String useDoor(ServerLevel level, BlockPos lowerDoor,
                          @Nullable CyberNpcEntity selected, boolean sneaking) {
        Town town = nearestTown(level, lowerDoor);
        if (town == null) return "Create a town first: click its bell with the Town Register (within 256 blocks).";
        Home home = null;
        for (Home candidate : homes.values()) {
            Town ownerTown = towns.get(candidate.townId);
            if (ownerTown != null
                    && ownerTown.dimension.equals(level.dimension().location().toString())
                    && sameEntrance(level, candidate.door, lowerDoor)) {
                // Either half of a double entrance is the same registered home.
                home = candidate;
                break;
            }
        }
        if (sneaking) {
            if (selected == null || selected.getNpcType() != NpcType.MAIN
                    || selected.isSpecialServiceNpc() || !selected.isAlive()) {
                return "Select an ordinary Main NPC using the Town Register, then sneak-click a registered door to assign their bed.";
            }
            if (home == null) return "Register this house with a normal click first.";
            if (home.residents.containsKey(selected.getUUID()))
                return "That NPC already belongs to this house.";
            Long free = findFreeBed(home);
            if (free == null) return "No available beds in this house.";
            for (Home other : homes.values()) other.residents.remove(selected.getUUID());
            home.residents.put(selected.getUUID(), free);
            selected.getPersistentData().putUUID("CyberNpcTownHome", home.id);
            setDirty();
            return selected.getName().getString() + " assigned to "
                    + town.name + " / Home " + (indexOfHome(home) + 1) + ".";
        }

        // The house is identified by its PRIMARY door, not any passage/room
        // reachable through the roofed-town flood scan. Block traversal via
        // all other registered front doors and split shared covered spaces by
        // their nearest entrance.
        Set<BlockPos> otherDoors = new LinkedHashSet<>();
        String dimension = level.dimension().location().toString();
        for (Home other : homes.values()) {
            Town ownerTown = towns.get(other.townId);
            if (other != home && ownerTown != null
                    && ownerTown.dimension.equals(dimension)) {
                otherDoors.add(other.door);
            }
        }
        BlockPos primaryDoor = home == null ? lowerDoor : home.door;
        HouseInteriorScanner.Result scan =
                HouseInteriorScanner.scan(level, primaryDoor, otherDoors);
        if (!scan.success()) return scan.message();

        Set<Long> detected = new LinkedHashSet<>(scan.beds());
        // Other registered entrances win beds closer to their own doors.
        // An occupied bed is NEVER transferred without explicitly moving its
        // NPC first, preserving legacy saves and permanent sleeping positions.
        Map<Home, Set<Long>> transfers = new LinkedHashMap<>();
        int skipped = 0;
        int reassigned = 0;
        for (Long bed : scan.beds()) {
            BlockPos bedPos = BlockPos.of(bed);
            double ownDistance = horizontalBedDistance(bedPos, primaryDoor);
            Home nearest = null;
            double nearestDistance = ownDistance;
            for (Home other : homes.values()) {
                Town ownerTown = towns.get(other.townId);
                if (other == home || ownerTown == null
                        || !ownerTown.dimension.equals(dimension)) continue;
                double competing = horizontalBedDistance(bedPos, other.door);
                if (competing + 4.0D < nearestDistance) {
                    nearestDistance = competing;
                    nearest = other;
                }
            }
            if (nearest != null
                    && (home == null || !home.residents.containsValue(bed))) {
                detected.remove(bed);
                skipped++;
                continue;
            }
            boolean occupiedElsewhere = false;
            for (Home other : homes.values()) {
                if (other == home || !other.beds.contains(bed)) continue;
                if (other.residents.containsValue(bed)) {
                    occupiedElsewhere = true;
                    break;
                }
                transfers.computeIfAbsent(other, ignored -> new LinkedHashSet<>())
                        .add(bed);
            }
            if (occupiedElsewhere) {
                detected.remove(bed);
                skipped++;
                for (Set<Long> move : transfers.values()) move.remove(bed);
            }
        }
        if (detected.isEmpty()) {
            return "No exclusive beds found for this front door. The scan "
                    + "reached neighbouring homes, or their beds are already "
                    + "assigned to existing residents. Register each house at "
                    + "its own entrance; occupied beds are never stolen.";
        }

        if (home == null) {
            String kind = chooseHouseholdKind(level, detected.size(), homes.size());
            home = new Home(UUID.randomUUID(), town.id,
                    lowerDoor.immutable(), scan.inside(), kind,
                    householdTarget(kind, detected.size()));
            homes.put(home.id, home);
            nextHome++;
        }

        // Repair already-registered houses that accidentally included other
        // homes' unoccupied beds in older versions. No NPC or occupied bed
        // assignment is modified by a repair.
        for (Map.Entry<Home, Set<Long>> transfer : transfers.entrySet()) {
            Home former = transfer.getKey();
            for (Long bed : transfer.getValue()) {
                if (detected.contains(bed) && former.beds.remove(bed)) {
                    reassigned++;
                }
            }
            former.target = Math.max(former.residents.size(),
                    householdTarget(former.kind, former.beds.size()));
        }

        if (!scan.partial()) {
            home.inside = scan.inside();
            home.beds.clear();
        }
        // If distant chunks were unloaded, preserve only pre-existing beds
        // that aren't known to belong to a different registered door.
        home.beds.addAll(detected);
        final Home registeredHome = home;
        home.beds.removeIf(bed -> {
            BlockPos bedPos = BlockPos.of(bed);
            double ours = horizontalBedDistance(bedPos, registeredHome.door);
            for (Home other : homes.values()) {
                Town ownerTown = towns.get(other.townId);
                if (other != registeredHome && ownerTown != null
                        && ownerTown.dimension.equals(dimension)
                        && horizontalBedDistance(bedPos, other.door) + 4.0D < ours
                        && !registeredHome.residents.containsValue(bed)) {
                    return true;
                }
            }
            return false;
        });
        reassignBeds(home);
        home.target = Math.max(home.residents.size(),
                householdTarget(home.kind, home.beds.size()));
        setDirty();
        return "Home " + (indexOfHome(home) + 1) + " registered in " + town.name
                + ": " + home.beds.size() + " beds, " + home.residents.size()
                + " residents, " + home.kind.toLowerCase(java.util.Locale.ROOT)
                + " household (target " + home.target + ")."
                + (skipped > 0 ? " Excluded " + skipped + " neighbouring-house beds." : "")
                + (reassigned > 0 ? " Corrected " + reassigned + " previously shared beds." : "")
                + (scan.partial()
                        ? " Partial scan: distant chunks are unloaded; existing valid assignments preserved."
                        : " Each available bed belongs to only this house.")
                + " Sneak-click the bell to enable gradual move-ins.";
    }

    private static double horizontalBedDistance(BlockPos bed, BlockPos door) {
        long dx = (long) bed.getX() - door.getX();
        long dz = (long) bed.getZ() - door.getZ();
        long dy = (long) bed.getY() - door.getY();
        // Height matters for stacked flats/apartments with separate doors.
        return (double) (dx * dx + dz * dz + dy * dy);
    }

    /** Both leaves of one double doorway must resolve to one house. */
    private static boolean sameEntrance(ServerLevel level, BlockPos a, BlockPos b) {
        if (a.equals(b)) return true;
        if (a.getY() != b.getY() || Math.abs(a.getX() - b.getX()) > 1
                || Math.abs(a.getZ() - b.getZ()) > 1
                || level.getChunkSource().getChunkNow(a.getX() >> 4, a.getZ() >> 4) == null
                || level.getChunkSource().getChunkNow(b.getX() >> 4, b.getZ() >> 4) == null) {
            return false;
        }
        BlockState first = level.getBlockState(a);
        BlockState second = level.getBlockState(b);
        if (!(first.getBlock() instanceof DoorBlock)
                || !(second.getBlock() instanceof DoorBlock)) return false;
        Direction facing = first.getValue(DoorBlock.FACING);
        // Side-by-side neighbours are not necessarily a double door. Only
        // opposite-hinge leaves facing the SAME way count as one entrance.
        if (facing != second.getValue(DoorBlock.FACING)
                || first.getValue(DoorBlock.HINGE) == second.getValue(DoorBlock.HINGE)) {
            return false;
        }
        return facing.getAxis() == Direction.Axis.Z ? a.getZ() == b.getZ()
                : a.getX() == b.getX();
    }

    private int indexOfHome(Home home) {
        int count = 0;
        for (Home h : homes.values()) {
            if (h.townId.equals(home.townId)) {
                if (h == home) return count;
                count++;
            }
        }
        return count;
    }

    private static String chooseHouseholdKind(ServerLevel level, int beds, int index) {
        if (beds == 1) return "SINGLE";
        // Mix all four arrangements; larger households only where beds allow.
        int roll = (index + level.random.nextInt(4)) % 4;
        return switch (roll) {
            case 0 -> "SINGLE";
            case 1 -> "COUPLE";
            case 2 -> "FAMILY";
            default -> "ROOMMATES";
        };
    }

    private static int householdTarget(String kind, int beds) {
        return Math.min(beds, switch (kind) {
            case "SINGLE" -> 1;
            case "COUPLE" -> 2;
            case "FAMILY" -> Math.min(4, Math.max(2, beds));
            default -> Math.min(3, Math.max(2, beds));
        });
    }

    private static void reassignBeds(Home home) {
        Set<Long> assigned = new LinkedHashSet<>();
        for (Map.Entry<UUID, Long> entry : home.residents.entrySet()) {
            if (!home.beds.contains(entry.getValue()) || !assigned.add(entry.getValue()))
                entry.setValue(Long.MIN_VALUE);
        }
        for (Map.Entry<UUID, Long> entry : home.residents.entrySet()) {
            if (entry.getValue() != Long.MIN_VALUE) continue;
            for (Long bed : home.beds) {
                if (assigned.add(bed)) {
                    entry.setValue(bed);
                    break;
                }
            }
        }
    }

    @Nullable
    private static Long findFreeBed(Home home) {
        for (long bed : home.beds) {
            if (!home.residents.containsValue(bed)) return bed;
        }
        return null;
    }

    /**
     * Visible, emerald-coloured door frame glimmers for the Town Register's
     * holder only. The server owns registration truth, so there are no client
     * caches that might claim a door belonging to another saved world.
     *
     * Called at most once every 12 ticks per holder. Never forces chunks.
     */
    public void showClaimedDoorHighlights(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (homes.isEmpty() || player.isSpectator() || !player.isAlive()
                || (!(player.getMainHandItem().getItem() instanceof TownRegisterItem)
                && !(player.getOffhandItem().getItem() instanceof TownRegisterItem))) {
            return;
        }

        int shown = 0;
        String dimension = level.dimension().location().toString();
        for (Home home : homes.values()) {
            if (shown >= MAX_HIGHLIGHTED_DOORS) break;
            Town town = towns.get(home.townId);
            if (town == null || !town.dimension.equals(dimension)
                    || player.distanceToSqr(home.door.getX() + 0.5D,
                            home.door.getY() + 1.0D, home.door.getZ() + 0.5D)
                            > VISIBLE_DOOR_RADIUS_SQR
                    || level.getChunkSource().getChunkNow(
                            home.door.getX() >> 4, home.door.getZ() >> 4) == null) {
                continue;
            }
            BlockState state = level.getBlockState(home.door);
            if (!(state.getBlock() instanceof DoorBlock)) continue;
            Direction facing = state.getValue(DoorBlock.FACING);
            // A thin outline is placed on the player's side of the doorway,
            // rather than in the door block or extending through the wall.
            boolean alongX = facing.getAxis() == Direction.Axis.Z;
            double planeX = home.door.getX() + 0.5D;
            double planeZ = home.door.getZ() + 0.5D;
            if (alongX) {
                planeZ = home.door.getZ()
                        + (player.getZ() < home.door.getZ() + 0.5D ? 0.15D : 0.85D);
            } else {
                planeX = home.door.getX()
                        + (player.getX() < home.door.getX() + 0.5D ? 0.15D : 0.85D);
            }
            for (int height = 0; height < 5; height++) {
                double y = home.door.getY() + 0.14D + height * 0.43D;
                for (double side : new double[]{0.12D, 0.88D}) {
                    double x = alongX ? home.door.getX() + side : planeX;
                    double z = alongX ? planeZ : home.door.getZ() + side;
                    level.sendParticles(player, CLAIMED_DOOR_GLOW, true,
                            x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            // Glowing upper lintel makes a registered door easily identifiable
            // even when viewed at a distance or around a corner.
            for (double across : new double[]{0.35D, 0.65D}) {
                level.sendParticles(player, CLAIMED_DOOR_GLOW, true,
                        alongX ? home.door.getX() + across : planeX,
                        home.door.getY() + 1.96D,
                        alongX ? planeZ : home.door.getZ() + across,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            shown++;
        }
    }

    /**
     * One selected NPC owns up to ten explicitly clicked rooms, all at their
     * original floor heights. No new house entity or alternate save file is
     * created. Each room has its own exact footprint, doorways and beds.
     */
    public String addFloorRoom(ServerLevel level, UUID npcId, FloorRoomScanner.Result scan) {
        String dimension = level.dimension().location().toString();
        RoomHome home = roomHomes.get(npcId);
        if (home != null && !home.dimension.equals(dimension)) {
            return "This NPC already has rooms in another dimension. Remove those rooms before moving them.";
        }
        if (home != null) {
            for (RoomClaim existing : home.rooms) {
                if (existing.floor.contains(scan.inside().asLong())) {
                    return "This room is already assigned to this NPC (" + home.rooms.size()
                            + "/10). Sneak-click its floor to remove it.";
                }
                for (Long cell : scan.cells()) {
                    if (existing.floor.contains(cell)) {
                        return "This overlaps another saved room. Click a separate room beyond a door.";
                    }
                }
            }
            if (home.rooms.size() >= MAX_ROOMS_PER_NPC) {
                return "This NPC already has the maximum of 10 rooms. Sneak-click a room to remove it first.";
            }
        }
        if (home == null) {
            home = new RoomHome(dimension);
            roomHomes.put(npcId, home);
        }
        home.rooms.add(new RoomClaim(scan.inside(), scan.cells(), scan.doors(), scan.beds()));
        refreshAssignedBed(npcId, home);
        setDirty();
        return "Room " + home.rooms.size() + "/10 saved (" + scan.cells().size()
                + " floor spaces, " + scan.doors().size() + " doors, "
                + scan.beds().size() + " beds)."
                + (home.assignedBed == null
                ? " No free bed yet; the NPC can still use the room as a home."
                : " Sleeping bed assigned.");
    }

    /** Remove a registered room without rescanning: walls may have changed. */
    public String removeFloorRoom(ServerLevel level, UUID npcId, BlockPos floorPos) {
        RoomHome home = roomHomes.get(npcId);
        if (home == null || !home.dimension.equals(level.dimension().location().toString())) {
            return "This NPC has no saved rooms in this dimension.";
        }
        boolean removed = home.rooms.removeIf(room -> room.floor.contains(floorPos.asLong()));
        if (!removed) return "This floor is not part of any room saved for that NPC.";
        if (home.rooms.isEmpty()) {
            roomHomes.remove(npcId);
            setDirty();
            return "Last saved room removed. Older house assignments remain available as fallback.";
        }
        refreshAssignedBed(npcId, home);
        setDirty();
        return "Room removed. " + home.rooms.size() + "/10 rooms remain.";
    }

    public int roomCount(UUID npcId) {
        RoomHome home = roomHomes.get(npcId);
        return home == null ? 0 : home.rooms.size();
    }

    private void refreshAssignedBed(UUID npcId, RoomHome home) {
        Set<Long> available = new LinkedHashSet<>();
        for (RoomClaim room : home.rooms) available.addAll(room.beds);
        if (home.assignedBed != null && available.contains(home.assignedBed)
                && !bedClaimedByAnother(npcId, home.assignedBed)) return;
        home.assignedBed = null;
        for (Long bed : available) {
            if (!bedClaimedByAnother(npcId, bed)) {
                home.assignedBed = bed;
                return;
            }
        }
    }

    private boolean bedClaimedByAnother(UUID owner, long bed) {
        for (Home home : homes.values()) {
            for (Map.Entry<UUID, Long> resident : home.residents.entrySet()) {
                if (!resident.getKey().equals(owner) && resident.getValue() == bed) return true;
            }
        }
        for (Map.Entry<UUID, RoomHome> member : roomHomes.entrySet()) {
            if (!member.getKey().equals(owner)
                    && member.getValue().assignedBed != null
                    && member.getValue().assignedBed == bed) return true;
        }
        return false;
    }

    private static Set<Long> readCellSet(CompoundTag tag, String key, int max) {
        Set<Long> values = new LinkedHashSet<>();
        long[] saved = tag.getLongArray(key);
        for (int i = 0; i < saved.length && i < max; i++) values.add(saved[i]);
        return values;
    }

    private static long[] toArray(Set<Long> values) {
        long[] array = new long[values.size()];
        int i = 0;
        for (long value : values) array[i++] = value;
        return array;
    }

    private record RoomClaim(BlockPos inside, Set<Long> floor,
                             Set<Long> doors, Set<Long> beds) { }

    private static final class RoomHome {
        final String dimension;
        final List<RoomClaim> rooms = new ArrayList<>();
        @Nullable Long assignedBed;

        RoomHome(String dimension) {
            this.dimension = dimension;
        }
    }

    /** A home is either in the new registry or still on legacy marker data. */
    @Nullable
    public HomeAssignment assignedHome(ServerLevel level, UUID npcId) {
        if (npcId == null) return null;
        String dimension = level.dimension().location().toString();
        RoomHome rooms = roomHomes.get(npcId);
        if (rooms != null && !rooms.rooms.isEmpty()
                && rooms.dimension.equals(dimension)) {
            return new HomeAssignment(rooms.assignedBed == null ? null
                    : BlockPos.of(rooms.assignedBed), rooms.rooms.get(0).inside);
        }
        for (Home home : homes.values()) {
            Town town = towns.get(home.townId);
            if (town == null || !town.dimension.equals(dimension)) continue;
            Long bed = home.residents.get(npcId);
            if (bed != null) {
                return new HomeAssignment(
                        bed == Long.MIN_VALUE ? null : BlockPos.of(bed),
                        home.inside);
            }
        }
        return null;
    }

    /**
     * Read-only job integration. A worker must belong to a REGISTERED home
     * in the same town/dimension; legacy home-marker assignments are untouched.
     */
    @Nullable
    public UUID residentTownId(ServerLevel level, UUID npcId) {
        if (npcId == null) return null;
        String dimension = level.dimension().location().toString();
        RoomHome rooms = roomHomes.get(npcId);
        if (rooms != null && rooms.dimension.equals(dimension) && !rooms.rooms.isEmpty()) {
            Town town = nearestTown(level, rooms.rooms.get(0).inside);
            return town == null ? null : town.id;
        }
        for (Home home : homes.values()) {
            Town town = towns.get(home.townId);
            if (town != null && town.dimension.equals(dimension)
                    && home.residents.containsKey(npcId)) return town.id;
        }
        return null;
    }

    @Nullable
    public UUID townIdNear(ServerLevel level, BlockPos pos) {
        Town town = nearestTown(level, pos);
        return town == null ? null : town.id;
    }

    public record HomeAssignment(@Nullable BlockPos bed, BlockPos inside) {}

    /** One opportunity per minute per loaded dimension, after world startup. */
    public void populateOne(ServerLevel level) {
        if (level.getGameTime() < 400L) return;
        String dimension = level.dimension().location().toString();
        for (Home home : homes.values()) {
            Town town = towns.get(home.townId);
            if (town == null || !town.populate || !town.dimension.equals(dimension)
                    || home.residents.size() >= home.target) continue;
            Long free = findFreeBed(home);
            if (free == null
                    || level.getChunkSource().getChunkNow(home.inside.getX() >> 4, home.inside.getZ() >> 4) == null
                    || level.getChunkSource().getChunkNow(home.door.getX() >> 4, home.door.getZ() >> 4) == null) {
                continue;
            }
            boolean playerNearby = false;
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && player.blockPosition().distSqr(home.door) < 80.0D * 80.0D) {
                    playerNearby = true;
                    break;
                }
            }
            if (!playerNearby || !HouseInteriorScanner.standable(level, home.inside, home.door)) continue;
            BlockState bedState = level.getBlockState(BlockPos.of(free));
            if (!(bedState.getBlock() instanceof BedBlock)
                    || !bedState.hasProperty(BedBlock.PART)
                    || bedState.getValue(BedBlock.PART) != BedPart.HEAD) continue;

            CyberNpcEntity npc = ModEntities.CYBER_NPC.get().create(level);
            if (npc == null) return;
            npc.moveTo(home.inside.getX() + 0.5D, home.inside.getY(),
                    home.inside.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(npc)) continue;
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(home.inside),
                    MobSpawnType.EVENT, null, null);
            npc.setNpcType(NpcType.MAIN);
            npc.setRole("Citizen");
            npc.setCustomName(Component.literal(CyberNpcNameGenerator.randomWildName(
                    level.random, npc.getAppearanceGender())));
            npc.setCustomNameVisible(true);
            npc.setPersistenceRequired();
            npc.getPersistentData().putUUID("CyberNpcTownHome", home.id);
            npc.getPersistentData().putUUID("CyberNpcTownId", town.id);
            npc.getPersistentData().putString("CyberNpcHouseholdKind", home.kind);
            if (!level.addFreshEntity(npc)) continue;
            home.residents.put(npc.getUUID(), free);
            // Relationships are saved on the NPCs as well as the household
            // membership on this registry. No dialogue or Wild-NPC AI changes.
            for (UUID otherId : home.residents.keySet()) {
                if (otherId.equals(npc.getUUID())) continue;
                if (level.getEntity(otherId) instanceof CyberNpcEntity other) {
                    npc.seedTownHouseholdBond(other, home.kind);
                }
            }
            setDirty();
            return;
        }
    }

    public void onNpcDeath(CyberNpcEntity npc) {
        boolean modified = roomHomes.remove(npc.getUUID()) != null;
        for (Home home : homes.values()) {
            if (home.residents.remove(npc.getUUID()) != null) {
                modified = true;
            }
        }
        if (modified) setDirty();
    }

    private static final class Town {
        final UUID id;
        final String dimension;
        final BlockPos bell;
        final String name;
        boolean populate;
        Town(UUID id, String dimension, BlockPos bell, String name) {
            this.id = id;
            this.dimension = dimension;
            this.bell = bell;
            this.name = name;
        }
    }

    private static final class Home {
        final UUID id;
        final UUID townId;
        final BlockPos door;
        BlockPos inside;
        final String kind;
        int target;
        final Set<Long> beds = new LinkedHashSet<>();
        final Map<UUID, Long> residents = new LinkedHashMap<>();

        Home(UUID id, UUID townId, BlockPos door, BlockPos inside,
             String kind, int target) {
            this.id = id;
            this.townId = townId;
            this.door = door;
            this.inside = inside;
            this.kind = kind;
            this.target = target;
        }
    }

    private TownHomeSavedData() {}
}
