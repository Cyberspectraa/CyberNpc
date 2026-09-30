package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.world.CyberNpcWorldClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WildNpcCorralBrain {
    private static final int CORRAL_SEARCH_RADIUS = 24;
    private static final int ENCLOSURE_RADIUS = 10;
    private static final int MAX_ENCLOSURE_CELLS = 160;
    private static final int MANAGEMENT_HUNGER_THRESHOLD = 14;
    private static final int CRITICAL_HUNGER = 4;
    private static final int BREED_CHECK_COOLDOWN = 600;
    private static final double REMOTE_RETURN_DISTANCE_SQR = 24.0D * 24.0D;

    private final CyberNpcEntity npc;

    @Nullable
    private BlockPos primaryGate;

    @Nullable
    private BlockPos penAnchor;

    private Set<BlockPos> penCells = Set.of();
    private Set<BlockPos> gates = Set.of();

    @Nullable
    private EntityType<?> livestockType;

    @Nullable
    private UUID leadAnimalA;

    @Nullable
    private UUID leadAnimalB;

    @Nullable
    private BlockPos activeEntryGate;

    @Nullable
    private BlockPos activeExitGate;

    private int searchCooldown;
    private int breedCooldown;
    private boolean busy;
    private boolean exitingPen;
    private String debugActivity = "No pen task";

    WildNpcCorralBrain(CyberNpcEntity npc) {
        this.npc = npc;
    }

    boolean isBusy() {
        return busy;
    }

    boolean isProtectedLivestock(LivingEntity entity) {
        return entity instanceof Animal animal
                && !penCells.isEmpty()
                && isInsidePen(animal);
    }

    String getDebugActivity() {
        return debugActivity;
    }

    String getClaimDebug() {
        if (penAnchor == null) {
            return "none";
        }

        int gateCount = gates.isEmpty() ? (primaryGate == null ? 0 : 1) : gates.size();
        return formatPos(penAnchor) + " (" + gateCount + " gate" + (gateCount == 1 ? "" : "s") + ")";
    }

    @Nullable
    BlockPos getPenAnchor() {
        return penAnchor;
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)) {
            busy = false;
            return false;
        }

        if (breedCooldown > 0) {
            breedCooldown--;
        }

        // Finish an in-progress livestock transfer even if hunger changes, so the
        // NPC never strands itself or its animals inside an open pen.
        if (hasActiveTransfer(level)) {
            busy = true;
            debugActivity = exitingPen ? "Leaving claimed pen" : "Leading livestock to claimed pen";
            return tickTransfer(level);
        }

        if (npc.isCombatActive()
                || npc.hasCollectedRawFood()
                || npc.getHunger() > MANAGEMENT_HUNGER_THRESHOLD) {
            stopLeading(level);
            busy = false;
            debugActivity = "Pen idle";
            return false;
        }

        if (!ensureCorral(level)) {
            busy = false;
            return false;
        }

        if (penCells.isEmpty()) {
            // We still own the saved pen, but it is too far away to have its
            // enclosure loaded/rebuilt. Travel back toward its primary gate.
            if (primaryGate != null) {
                Vec3 destination = Vec3.atBottomCenterOf(primaryGate);
                npc.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.95D);
                npc.setSprinting(true);
                busy = true;
                debugActivity = "Returning to claimed pen";
                return true;
            }

            return false;
        }

        List<Animal> insideAnimals = getAnimalsInside(level);
        livestockType = chooseLivestockType(level, insideAnimals);

        if (livestockType == null) {
            busy = false;
            debugActivity = "Claimed pen has no livestock plan";
            return false;
        }

        List<Animal> adultsInside = insideAnimals.stream()
                .filter(animal -> animal.getType() == livestockType)
                .filter(animal -> !animal.isBaby())
                .toList();

        if (adultsInside.size() >= 2) {
            closeAllGates(level);
            stopLeading(level);
            busy = false;
            debugActivity = "Maintaining breeding pair";

            if (breedCooldown <= 0) {
                Animal first = adultsInside.get(0);
                Animal second = adultsInside.get(1);

                if (first.canFallInLove() && second.canFallInLove()) {
                    ItemStack lure = findLure(first);
                    if (!lure.isEmpty()) {
                        npc.equipUtilityItem(lure);
                    }

                    first.setInLoveTime(600);
                    second.setInLoveTime(600);
                    breedCooldown = BREED_CHECK_COOLDOWN;
                    npc.clearUtilityItem();
                    debugActivity = "Breeding livestock";
                }
            }

            return false;
        }

        if (npc.getHunger() <= CRITICAL_HUNGER) {
            closeAllGates(level);
            stopLeading(level);
            busy = false;
            debugActivity = "Too hungry to stock pen";
            return false;
        }

        int needed = 2 - adultsInside.size();
        List<Animal> recruits = resolveOrChooseRecruits(level, needed);

        if (recruits.size() < needed) {
            stopLeading(level);
            closeAllGates(level);
            searchCooldown = 100;
            busy = false;
            debugActivity = "Waiting for livestock near claimed pen";
            return false;
        }

        leadAnimalA = recruits.size() > 0 ? recruits.get(0).getUUID() : null;
        leadAnimalB = recruits.size() > 1 ? recruits.get(1).getUUID() : null;
        activeEntryGate = selectEntryGate(recruits);
        activeExitGate = null;
        exitingPen = false;

        attachLeads(recruits);
        npc.equipUtilityItem(new ItemStack(Items.LEAD));
        busy = true;
        debugActivity = "Leading livestock to claimed pen";
        return tickTransfer(level);
    }

    void interrupt() {
        if (npc.level() instanceof ServerLevel level) {
            stopLeading(level);
            closeAllGates(level);
        }

        activeEntryGate = null;
        activeExitGate = null;
        exitingPen = false;
        busy = false;
        debugActivity = "Pen task interrupted";
    }

    void addSaveData(CompoundTag tag) {
        if (penAnchor != null) {
            tag.putLong("CyberNpcPenAnchor", penAnchor.asLong());
        }
        if (primaryGate != null) {
            tag.putLong("CyberNpcPenGate", primaryGate.asLong());
        }
    }

    void readSaveData(CompoundTag tag) {
        penAnchor = tag.contains("CyberNpcPenAnchor")
                ? BlockPos.of(tag.getLong("CyberNpcPenAnchor"))
                : null;
        primaryGate = tag.contains("CyberNpcPenGate")
                ? BlockPos.of(tag.getLong("CyberNpcPenGate"))
                : null;

        penCells = Set.of();
        gates = primaryGate == null ? Set.of() : Set.of(primaryGate);
        livestockType = null;
        leadAnimalA = null;
        leadAnimalB = null;
        activeEntryGate = null;
        activeExitGate = null;
        exitingPen = false;
        busy = false;
        searchCooldown = 0;
        debugActivity = penAnchor == null ? "No pen task" : "Claimed pen saved";
    }

    private boolean hasActiveTransfer(ServerLevel level) {
        if (exitingPen) {
            return true;
        }

        Animal a = resolveAnimal(level, leadAnimalA);
        Animal b = resolveAnimal(level, leadAnimalB);
        return a != null || b != null;
    }

    private boolean tickTransfer(ServerLevel level) {
        List<Animal> recruits = currentRecruits(level);

        if (exitingPen) {
            return tickExitPen(level);
        }

        if (recruits.isEmpty()) {
            finishTransfer(level);
            return false;
        }

        if (activeEntryGate == null || !gates.contains(activeEntryGate)) {
            activeEntryGate = selectEntryGate(recruits);
        }

        if (activeEntryGate == null) {
            finishTransfer(level);
            return false;
        }

        openGate(level, activeEntryGate);

        for (Animal animal : recruits) {
            if (animal.getLeashHolder() != npc) {
                animal.setLeashedTo(npc, true);
            }
        }

        boolean allInside = recruits.stream().allMatch(this::isInsidePen);
        if (allInside) {
            for (Animal animal : recruits) {
                animal.getNavigation().stop();
                if (animal.getLeashHolder() == npc) {
                    animal.dropLeash(true, false);
                }
            }

            npc.clearUtilityItem();
            exitingPen = true;
            activeExitGate = selectExitGate();
            debugActivity = "Leaving claimed pen";
            return tickExitPen(level);
        }

        BlockPos outside = outsideForGate(activeEntryGate);
        BlockPos inside = insideForGate(activeEntryGate);

        if (outside == null || inside == null) {
            finishTransfer(level);
            return false;
        }

        double toOutside = npc.distanceToSqr(Vec3.atBottomCenterOf(outside));
        if (!isNpcInsidePen() && toOutside > 6.25D) {
            npc.getNavigation().moveTo(
                    outside.getX() + 0.5D,
                    outside.getY(),
                    outside.getZ() + 0.5D,
                    0.92D
            );
        } else {
            npc.getNavigation().moveTo(
                    inside.getX() + 0.5D,
                    inside.getY(),
                    inside.getZ() + 0.5D,
                    0.82D
            );
        }

        npc.setSprinting(false);
        return true;
    }

    private boolean tickExitPen(ServerLevel level) {
        if (!isNpcInsidePen()) {
            finishTransfer(level);
            return false;
        }

        if (activeExitGate == null || !gates.contains(activeExitGate)) {
            activeExitGate = selectExitGate();
        }

        if (activeExitGate == null) {
            activeExitGate = primaryGate;
        }

        if (activeExitGate == null) {
            finishTransfer(level);
            return false;
        }

        openGate(level, activeExitGate);

        BlockPos outside = outsideForGate(activeExitGate);
        if (outside == null) {
            finishTransfer(level);
            return false;
        }

        npc.getNavigation().moveTo(
                outside.getX() + 0.5D,
                outside.getY(),
                outside.getZ() + 0.5D,
                0.95D
        );
        npc.setSprinting(false);

        if (!isNpcInsidePen() && npc.distanceToSqr(Vec3.atBottomCenterOf(outside)) < 9.0D) {
            finishTransfer(level);
            return false;
        }

        return true;
    }

    private void finishTransfer(ServerLevel level) {
        stopLeading(level);
        closeAllGates(level);
        clearRecruitIds();
        activeEntryGate = null;
        activeExitGate = null;
        exitingPen = false;
        busy = false;
        npc.getNavigation().stop();
        debugActivity = "Livestock secured";
    }

    private void attachLeads(List<Animal> recruits) {
        for (Animal animal : recruits) {
            if (animal.getLeashHolder() != npc) {
                if (animal.isLeashed()) {
                    animal.dropLeash(true, false);
                }
                animal.setLeashedTo(npc, true);
            }
        }
    }

    private List<Animal> currentRecruits(ServerLevel level) {
        List<Animal> animals = new ArrayList<>();
        Animal a = resolveAnimal(level, leadAnimalA);
        Animal b = resolveAnimal(level, leadAnimalB);

        if (isValidTransferAnimal(a)) {
            animals.add(a);
        }
        if (isValidTransferAnimal(b) && b != a) {
            animals.add(b);
        }

        return animals;
    }

    private boolean isValidTransferAnimal(@Nullable Animal animal) {
        return animal != null && animal.isAlive() && !animal.isBaby();
    }

    private List<Animal> resolveOrChooseRecruits(ServerLevel level, int needed) {
        List<Animal> resolved = currentRecruits(level);

        if (resolved.size() >= needed) {
            return resolved.subList(0, needed);
        }

        if (primaryGate == null) {
            return List.of();
        }

        AABB searchBox = new AABB(primaryGate).inflate(28.0D, 8.0D, 28.0D);
        List<Animal> candidates = level.getEntitiesOfClass(
                        Animal.class,
                        searchBox,
                        animal -> isValidRecruit(animal)
                                && animal.getType() == livestockType
                                && !resolved.contains(animal)
                ).stream()
                .sorted(Comparator.comparingDouble(npc::distanceToSqr))
                .toList();

        for (Animal candidate : candidates) {
            if (resolved.size() >= needed) {
                break;
            }
            resolved.add(candidate);
        }

        return resolved;
    }

    private boolean isValidRecruit(@Nullable Animal animal) {
        return animal != null
                && animal.isAlive()
                && !animal.isBaby()
                && animal.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)
                && !isInsidePen(animal)
                && (!animal.isLeashed() || animal.getLeashHolder() == npc);
    }

    @Nullable
    private Animal resolveAnimal(ServerLevel level, @Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }

        Entity entity = level.getEntity(uuid);
        return entity instanceof Animal animal ? animal : null;
    }

    @Nullable
    private EntityType<?> chooseLivestockType(ServerLevel level, List<Animal> insideAnimals) {
        Map<EntityType<?>, Integer> insideCounts = new HashMap<>();

        for (Animal animal : insideAnimals) {
            if (!animal.isBaby() && animal.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)) {
                insideCounts.merge(animal.getType(), 1, Integer::sum);
            }
        }

        if (!insideCounts.isEmpty()) {
            return insideCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        if (primaryGate == null) {
            return null;
        }

        AABB searchBox = new AABB(primaryGate).inflate(28.0D, 8.0D, 28.0D);
        Map<EntityType<?>, List<Animal>> groups = new HashMap<>();

        for (Animal animal : level.getEntitiesOfClass(
                Animal.class,
                searchBox,
                animal -> animal.isAlive()
                        && !animal.isBaby()
                        && animal.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)
                        && !isInsidePen(animal)
        )) {
            groups.computeIfAbsent(animal.getType(), ignored -> new ArrayList<>()).add(animal);
        }

        return groups.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .min(Comparator.comparingDouble(entry ->
                        entry.getValue().stream()
                                .sorted(Comparator.comparingDouble(npc::distanceToSqr))
                                .limit(2)
                                .mapToDouble(npc::distanceToSqr)
                                .sum()
                ))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private List<Animal> getAnimalsInside(ServerLevel level) {
        if (penCells.isEmpty()) {
            return List.of();
        }

        return level.getEntitiesOfClass(
                Animal.class,
                enclosureBox(),
                animal -> animal.isAlive() && isInsidePen(animal)
        );
    }

    private boolean isInsidePen(Animal animal) {
        if (penCells.isEmpty()) {
            return false;
        }

        BlockPos pos = animal.blockPosition();
        return isInsidePenPosition(pos);
    }

    private boolean isNpcInsidePen() {
        return isInsidePenPosition(npc.blockPosition());
    }

    private boolean isInsidePenPosition(BlockPos pos) {
        if (penCells.isEmpty()) {
            return false;
        }

        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            BlockPos test = new BlockPos(pos.getX(), pos.getY() + yOffset, pos.getZ());
            if (penCells.contains(test)) {
                return true;
            }
        }

        return penCells.stream().anyMatch(cell ->
                cell.getX() == pos.getX() && cell.getZ() == pos.getZ()
        );
    }

    private AABB enclosureBox() {
        int minX = penCells.stream().mapToInt(BlockPos::getX).min().orElse(npc.blockPosition().getX());
        int maxX = penCells.stream().mapToInt(BlockPos::getX).max().orElse(npc.blockPosition().getX());
        int minY = penCells.stream().mapToInt(BlockPos::getY).min().orElse(npc.blockPosition().getY());
        int maxY = penCells.stream().mapToInt(BlockPos::getY).max().orElse(npc.blockPosition().getY());
        int minZ = penCells.stream().mapToInt(BlockPos::getZ).min().orElse(npc.blockPosition().getZ());
        int maxZ = penCells.stream().mapToInt(BlockPos::getZ).max().orElse(npc.blockPosition().getZ());

        return new AABB(minX, minY - 1, minZ, maxX + 1, maxY + 3, maxZ + 1);
    }

    private boolean ensureCorral(ServerLevel level) {
        if (penAnchor != null && primaryGate != null) {
            CyberNpcWorldClaims claims = CyberNpcWorldClaims.get(level);
            if (!claims.claimPen(penAnchor, npc.getUUID())) {
                penAnchor = null;
                primaryGate = null;
                penCells = Set.of();
                gates = Set.of();
            } else if (npc.distanceToSqr(Vec3.atBottomCenterOf(primaryGate)) > REMOTE_RETURN_DISTANCE_SQR
                    && !level.hasChunkAt(primaryGate)) {
                penCells = Set.of();
                gates = Set.of(primaryGate);
                debugActivity = "Returning to claimed pen";
                return true;
            } else if (level.hasChunkAt(primaryGate)) {
                Corral rebuilt = inspectGate(level, primaryGate);
                if (rebuilt != null && rebuilt.anchor().equals(penAnchor)) {
                    applyCorral(rebuilt);
                    return true;
                }

                claims.releasePen(penAnchor, npc.getUUID());
                penAnchor = null;
                primaryGate = null;
                penCells = Set.of();
                gates = Set.of();
            }
        }

        if (searchCooldown > 0) {
            searchCooldown--;
            debugActivity = "Searching cooldown for pen";
            return false;
        }

        searchCooldown = 100;
        return findCorral(level);
    }

    private boolean findCorral(ServerLevel level) {
        BlockPos origin = npc.blockPosition();
        Corral best = null;
        double bestDistance = Double.MAX_VALUE;
        CyberNpcWorldClaims claims = CyberNpcWorldClaims.get(level);

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-CORRAL_SEARCH_RADIUS, -4, -CORRAL_SEARCH_RADIUS),
                origin.offset(CORRAL_SEARCH_RADIUS, 4, CORRAL_SEARCH_RADIUS)
        )) {
            if (!(level.getBlockState(pos).getBlock() instanceof FenceGateBlock)) {
                continue;
            }

            Corral candidate = inspectGate(level, pos.immutable());
            if (candidate == null) {
                continue;
            }

            if (!claims.claimPen(candidate.anchor(), npc.getUUID())) {
                continue;
            }

            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                if (best != null && !best.anchor().equals(candidate.anchor())) {
                    claims.releasePen(best.anchor(), npc.getUUID());
                }

                bestDistance = distance;
                best = candidate;
            } else if (best == null || !best.anchor().equals(candidate.anchor())) {
                claims.releasePen(candidate.anchor(), npc.getUUID());
            }
        }

        if (best == null) {
            debugActivity = "No claimable pen nearby";
            return false;
        }

        applyCorral(best);
        debugActivity = "Claimed pen";
        return true;
    }

    private void applyCorral(Corral corral) {
        primaryGate = corral.primaryGate();
        penAnchor = corral.anchor();
        penCells = corral.cells();
        gates = corral.gates();
    }

    @Nullable
    private Corral inspectGate(ServerLevel level, BlockPos gate) {
        BlockState gateState = level.getBlockState(gate);
        if (!(gateState.getBlock() instanceof FenceGateBlock)) {
            return null;
        }

        Direction facing = gateState.getValue(FenceGateBlock.FACING);
        Set<BlockPos> first = floodEnclosure(level, gate.relative(facing), gate);
        Set<BlockPos> second = floodEnclosure(level, gate.relative(facing.getOpposite()), gate);

        Set<BlockPos> chosen;
        if (first == null && second == null) {
            return null;
        } else if (first == null) {
            chosen = second;
        } else if (second == null) {
            chosen = first;
        } else {
            chosen = first.size() <= second.size() ? first : second;
        }

        if (chosen == null || chosen.size() < 4) {
            return null;
        }

        Set<BlockPos> discoveredGates = new LinkedHashSet<>();
        for (BlockPos cell : chosen) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos adjacent = cell.relative(direction);
                if (level.getBlockState(adjacent).getBlock() instanceof FenceGateBlock) {
                    discoveredGates.add(adjacent.immutable());
                }
            }
        }

        discoveredGates.add(gate.immutable());

        BlockPos anchor = chosen.stream()
                .min(Comparator
                        .comparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getY)
                        .thenComparingInt(BlockPos::getZ))
                .orElse(null);

        if (anchor == null) {
            return null;
        }

        BlockPos primary = discoveredGates.stream()
                .min(Comparator.comparingDouble(pos -> pos.distSqr(npc.blockPosition())))
                .orElse(gate.immutable());

        return new Corral(primary, anchor, Set.copyOf(chosen), Set.copyOf(discoveredGates));
    }

    @Nullable
    private Set<BlockPos> floodEnclosure(ServerLevel level, BlockPos start, BlockPos ignoredGate) {
        if (!isWalkableCell(level, start)) {
            return null;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            if (Math.abs(current.getX() - ignoredGate.getX()) >= ENCLOSURE_RADIUS
                    || Math.abs(current.getZ() - ignoredGate.getZ()) >= ENCLOSURE_RADIUS
                    || visited.size() > MAX_ENCLOSURE_CELLS) {
                return null;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);

                if (next.equals(ignoredGate) || visited.contains(next)) {
                    continue;
                }

                if (!isWalkableCell(level, next)) {
                    continue;
                }

                visited.add(next);
                queue.addLast(next);
            }
        }

        return visited;
    }

    private boolean isWalkableCell(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof WallBlock) {
            return false;
        }

        if (!state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        BlockPos above = pos.above();
        if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
            return false;
        }

        BlockPos below = pos.below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    @Nullable
    private BlockPos selectEntryGate(List<Animal> recruits) {
        if (gates.isEmpty()) {
            return primaryGate;
        }

        return gates.stream()
                .filter(gate -> outsideForGate(gate) != null && insideForGate(gate) != null)
                .min(Comparator.comparingDouble(gate -> recruits.stream()
                        .mapToDouble(animal -> animal.distanceToSqr(Vec3.atBottomCenterOf(outsideForGate(gate))))
                        .min()
                        .orElse(Double.MAX_VALUE)))
                .orElse(primaryGate);
    }

    @Nullable
    private BlockPos selectExitGate() {
        if (gates.isEmpty()) {
            return primaryGate;
        }

        return gates.stream()
                .filter(gate -> outsideForGate(gate) != null)
                .sorted(Comparator.comparingDouble(gate -> npc.distanceToSqr(Vec3.atBottomCenterOf(outsideForGate(gate)))))
                .filter(gate -> gates.size() <= 1 || !gate.equals(activeEntryGate))
                .findFirst()
                .orElseGet(() -> gates.stream()
                        .min(Comparator.comparingDouble(gate -> npc.distanceToSqr(Vec3.atBottomCenterOf(gate))))
                        .orElse(primaryGate));
    }

    @Nullable
    private BlockPos insideForGate(BlockPos gate) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = gate.relative(direction);
            if (penCells.contains(adjacent)) {
                return adjacent;
            }
        }
        return null;
    }

    @Nullable
    private BlockPos outsideForGate(BlockPos gate) {
        BlockPos inside = insideForGate(gate);
        if (inside == null) {
            return null;
        }

        int dx = Integer.signum(inside.getX() - gate.getX());
        int dz = Integer.signum(inside.getZ() - gate.getZ());
        BlockPos opposite = gate.offset(-dx, 0, -dz);

        if (!penCells.contains(opposite)) {
            return opposite;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = gate.relative(direction);
            if (!penCells.contains(adjacent)
                    && !(npc.level().getBlockState(adjacent).getBlock() instanceof FenceBlock)
                    && !(npc.level().getBlockState(adjacent).getBlock() instanceof WallBlock)) {
                return adjacent;
            }
        }

        return null;
    }

    private ItemStack findLure(Animal animal) {
        ItemStack[] candidates = {
                new ItemStack(Items.WHEAT),
                new ItemStack(Items.CARROT),
                new ItemStack(Items.POTATO),
                new ItemStack(Items.BEETROOT),
                new ItemStack(Items.WHEAT_SEEDS),
                new ItemStack(Items.BEETROOT_SEEDS),
                new ItemStack(Items.MELON_SEEDS),
                new ItemStack(Items.PUMPKIN_SEEDS),
                new ItemStack(Items.DANDELION)
        };

        for (ItemStack candidate : candidates) {
            if (animal.isFood(candidate)) {
                return candidate;
            }
        }

        return ItemStack.EMPTY;
    }

    private void openGate(ServerLevel level, @Nullable BlockPos gate) {
        setGateOpen(level, gate, true);
    }

    private void closeAllGates(ServerLevel level) {
        for (BlockPos gate : gates) {
            setGateOpen(level, gate, false);
        }

        if (gates.isEmpty() && primaryGate != null) {
            setGateOpen(level, primaryGate, false);
        }
    }

    private void setGateOpen(ServerLevel level, @Nullable BlockPos gate, boolean open) {
        if (gate == null || !level.hasChunkAt(gate)) {
            return;
        }

        BlockState state = level.getBlockState(gate);
        if (state.getBlock() instanceof FenceGateBlock
                && state.hasProperty(FenceGateBlock.OPEN)
                && state.getValue(FenceGateBlock.OPEN) != open) {
            level.setBlock(gate, state.setValue(FenceGateBlock.OPEN, open), Block.UPDATE_ALL);
        }
    }

    private void stopLeading(@Nullable ServerLevel level) {
        if (level != null) {
            Animal a = resolveAnimal(level, leadAnimalA);
            Animal b = resolveAnimal(level, leadAnimalB);

            if (a != null) {
                a.getNavigation().stop();
                if (a.getLeashHolder() == npc) {
                    a.dropLeash(true, false);
                }
            }

            if (b != null && b != a) {
                b.getNavigation().stop();
                if (b.getLeashHolder() == npc) {
                    b.dropLeash(true, false);
                }
            }
        }

        npc.clearUtilityItem();
    }

    @Nullable
    private ServerLevel levelOrNull() {
        return npc.level() instanceof ServerLevel level ? level : null;
    }

    private void clearRecruitIds() {
        leadAnimalA = null;
        leadAnimalB = null;
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record Corral(
            BlockPos primaryGate,
            BlockPos anchor,
            Set<BlockPos> cells,
            Set<BlockPos> gates
    ) {
    }
}
