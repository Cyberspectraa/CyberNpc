package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class WildNpcCorralBrain {
    private static final int CORRAL_SEARCH_RADIUS = 18;
    private static final int ENCLOSURE_RADIUS = 8;
    private static final int MAX_ENCLOSURE_CELLS = 96;
    private static final int MANAGEMENT_HUNGER_THRESHOLD = 14;
    private static final int CRITICAL_HUNGER = 4;
    private static final int BREED_CHECK_COOLDOWN = 600;

    private final CyberNpcEntity npc;

    @Nullable
    private BlockPos gatePos;

    @Nullable
    private BlockPos insidePos;

    private Set<BlockPos> penCells = Set.of();

    @Nullable
    private EntityType<?> livestockType;

    @Nullable
    private UUID leadAnimalA;

    @Nullable
    private UUID leadAnimalB;

    private int searchCooldown;
    private int breedCooldown;
    private boolean busy;

    WildNpcCorralBrain(CyberNpcEntity npc) {
        this.npc = npc;
    }

    boolean isBusy() {
        return busy;
    }

    boolean isProtectedLivestock(LivingEntity entity) {
        return entity instanceof Animal animal
                && validateCurrentCorralForProtection()
                && isInsidePen(animal);
    }

    private boolean validateCurrentCorralForProtection() {
        return gatePos != null
                && insidePos != null
                && !penCells.isEmpty();
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || npc.isCombatActive()
                || npc.hasCollectedRawFood()
                || npc.getHunger() > MANAGEMENT_HUNGER_THRESHOLD) {
            stopLeading(levelOrNull());
            busy = false;
            return false;
        }

        if (breedCooldown > 0) {
            breedCooldown--;
        }

        if (!validateCurrentCorral(level)) {
            if (searchCooldown > 0) {
                searchCooldown--;
                busy = false;
                return false;
            }

            searchCooldown = 200;
            if (!findCorral(level)) {
                busy = false;
                return false;
            }
        }

        List<Animal> insideAnimals = getAnimalsInside(level);
        livestockType = chooseLivestockType(level, insideAnimals);

        if (livestockType == null) {
            busy = false;
            return false;
        }

        List<Animal> adultsInside = insideAnimals.stream()
                .filter(animal -> animal.getType() == livestockType)
                .filter(animal -> !animal.isBaby())
                .toList();

        if (adultsInside.size() >= 2) {
            stopLeading(level);
            closeGate(level);
            busy = false;

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
                }
            }

            return false;
        }

        if (npc.getHunger() <= CRITICAL_HUNGER) {
            stopLeading(level);
            closeGate(level);
            busy = false;
            return false;
        }

        int needed = 2 - adultsInside.size();
        List<Animal> recruits = resolveOrChooseRecruits(level, needed);

        if (recruits.size() < needed) {
            stopLeading(level);
            closeGate(level);
            searchCooldown = 200;
            busy = false;
            return false;
        }

        Animal lureReference = !adultsInside.isEmpty() ? adultsInside.get(0) : recruits.get(0);
        ItemStack lure = findLure(lureReference);

        if (lure.isEmpty()) {
            stopLeading(level);
            busy = false;
            return false;
        }

        npc.equipUtilityItem(lure);
        openGate(level);
        busy = leadAnimalsIntoPen(level, recruits);

        if (!busy) {
            closeGate(level);
            npc.clearUtilityItem();
            clearRecruitIds();
        }

        return true;
    }

    void interrupt() {
        if (npc.level() instanceof ServerLevel level) {
            closeGate(level);
        }

        npc.clearUtilityItem();
        clearRecruitIds();
        busy = false;
    }

    private boolean leadAnimalsIntoPen(ServerLevel level, List<Animal> recruits) {
        if (insidePos == null) {
            return false;
        }

        List<Animal> outside = recruits.stream()
                .filter(animal -> !isInsidePen(animal))
                .toList();

        if (outside.isEmpty()) {
            for (Animal animal : recruits) {
                animal.getNavigation().stop();
            }

            npc.getNavigation().stop();
            return false;
        }

        Animal farthest = outside.stream()
                .max(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(outside.get(0));

        double farthestDistance = npc.distanceToSqr(farthest);

        for (Animal animal : outside) {
            if (npc.distanceToSqr(animal) <= 144.0D) {
                animal.getNavigation().moveTo(npc, 1.05D);
            }
        }

        if (farthestDistance > 36.0D) {
            npc.getNavigation().moveTo(farthest, 0.90D);
            return true;
        }

        Vec3 destination = Vec3.atBottomCenterOf(insidePos);
        npc.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.82D);

        for (Animal animal : outside) {
            animal.getNavigation().moveTo(npc, 1.05D);
        }

        return true;
    }

    private List<Animal> resolveOrChooseRecruits(ServerLevel level, int needed) {
        List<Animal> resolved = new ArrayList<>();

        Animal a = resolveAnimal(level, leadAnimalA);
        Animal b = resolveAnimal(level, leadAnimalB);

        if (isValidRecruit(a)) {
            resolved.add(a);
        }
        if (isValidRecruit(b) && b != a) {
            resolved.add(b);
        }

        if (resolved.size() >= needed) {
            return resolved.subList(0, needed);
        }

        AABB searchBox = new AABB(gatePos).inflate(24.0D, 6.0D, 24.0D);
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

        leadAnimalA = resolved.size() > 0 ? resolved.get(0).getUUID() : null;
        leadAnimalB = resolved.size() > 1 ? resolved.get(1).getUUID() : null;
        return resolved;
    }

    private boolean isValidRecruit(@Nullable Animal animal) {
        return animal != null
                && animal.isAlive()
                && !animal.isBaby()
                && animal.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)
                && !isInsidePen(animal);
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

        AABB searchBox = new AABB(gatePos).inflate(24.0D, 6.0D, 24.0D);
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

        AABB box = enclosureBox();
        return level.getEntitiesOfClass(
                Animal.class,
                box,
                animal -> animal.isAlive() && isInsidePen(animal)
        );
    }

    private boolean isInsidePen(Animal animal) {
        if (penCells.isEmpty()) {
            return false;
        }

        BlockPos pos = animal.blockPosition();
        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            if (penCells.contains(new BlockPos(pos.getX(), insidePos.getY() + yOffset, pos.getZ()))) {
                return true;
            }
        }

        return penCells.stream().anyMatch(cell ->
                cell.getX() == pos.getX() && cell.getZ() == pos.getZ()
        );
    }

    private AABB enclosureBox() {
        int minX = penCells.stream().mapToInt(BlockPos::getX).min().orElse(insidePos.getX());
        int maxX = penCells.stream().mapToInt(BlockPos::getX).max().orElse(insidePos.getX());
        int minZ = penCells.stream().mapToInt(BlockPos::getZ).min().orElse(insidePos.getZ());
        int maxZ = penCells.stream().mapToInt(BlockPos::getZ).max().orElse(insidePos.getZ());
        int y = insidePos.getY();

        return new AABB(minX, y - 1, minZ, maxX + 1, y + 3, maxZ + 1);
    }

    private boolean validateCurrentCorral(ServerLevel level) {
        return gatePos != null
                && insidePos != null
                && !penCells.isEmpty()
                && level.getBlockState(gatePos).getBlock() instanceof FenceGateBlock;
    }

    private boolean findCorral(ServerLevel level) {
        BlockPos origin = npc.blockPosition();
        Corral best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-CORRAL_SEARCH_RADIUS, -3, -CORRAL_SEARCH_RADIUS),
                origin.offset(CORRAL_SEARCH_RADIUS, 3, CORRAL_SEARCH_RADIUS)
        )) {
            if (!(level.getBlockState(pos).getBlock() instanceof FenceGateBlock)) {
                continue;
            }

            Corral candidate = inspectGate(level, pos.immutable());
            if (candidate == null) {
                continue;
            }

            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        if (best == null) {
            gatePos = null;
            insidePos = null;
            penCells = Set.of();
            livestockType = null;
            clearRecruitIds();
            return false;
        }

        gatePos = best.gate();
        insidePos = best.inside();
        penCells = best.cells();
        livestockType = null;
        clearRecruitIds();
        return true;
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

        BlockPos inside = chosen.stream()
                .max(Comparator.comparingDouble(pos -> pos.distSqr(gate)))
                .orElse(null);

        return inside == null ? null : new Corral(gate, inside, Set.copyOf(chosen));
    }

    @Nullable
    private Set<BlockPos> floodEnclosure(ServerLevel level, BlockPos start, BlockPos gate) {
        if (!isWalkableCell(level, start)) {
            return null;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            if (Math.abs(current.getX() - gate.getX()) >= ENCLOSURE_RADIUS
                    || Math.abs(current.getZ() - gate.getZ()) >= ENCLOSURE_RADIUS
                    || visited.size() > MAX_ENCLOSURE_CELLS) {
                return null;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);

                if (next.equals(gate) || visited.contains(next)) {
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

    private void openGate(ServerLevel level) {
        setGateOpen(level, true);
    }

    private void closeGate(ServerLevel level) {
        setGateOpen(level, false);
    }

    private void setGateOpen(ServerLevel level, boolean open) {
        if (gatePos == null) {
            return;
        }

        BlockState state = level.getBlockState(gatePos);
        if (state.getBlock() instanceof FenceGateBlock
                && state.hasProperty(FenceGateBlock.OPEN)
                && state.getValue(FenceGateBlock.OPEN) != open) {
            level.setBlock(gatePos, state.setValue(FenceGateBlock.OPEN, open), Block.UPDATE_ALL);
        }
    }

    private void stopLeading(@Nullable ServerLevel level) {
        if (level != null) {
            Animal a = resolveAnimal(level, leadAnimalA);
            Animal b = resolveAnimal(level, leadAnimalB);

            if (a != null) {
                a.getNavigation().stop();
            }
            if (b != null) {
                b.getNavigation().stop();
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

    private record Corral(BlockPos gate, BlockPos inside, Set<BlockPos> cells) {
    }
}
