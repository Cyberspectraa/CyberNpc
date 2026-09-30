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

    private static final int CLAIM_RESCAN_TICKS = 10;
    private static final int CLAIM_VALIDATE_TICKS = 40;

    private static final double REMOTE_RETURN_DISTANCE_SQR = 24.0D * 24.0D;
    private static final double RECRUIT_SEARCH_RADIUS = 18.0D;
    private static final double RECRUIT_MAX_NPC_DISTANCE_SQR = 22.0D * 22.0D;
    private static final double RECRUIT_MAX_PEN_DISTANCE_SQR = 20.0D * 20.0D;
    private static final int RECRUIT_APPROACH_TIMEOUT = 240;

    // Vanilla PathfinderMob snaps a leash at >10 blocks and drops a lead item.
    // Stay comfortably below that distance, and never auto-reattach after a break.
    private static final double LEASH_ATTACH_DISTANCE_SQR = 4.5D * 4.5D;
    private static final double LEASH_WAIT_DISTANCE_SQR = 7.0D * 7.0D;
    private static final double LEASH_ABORT_DISTANCE_SQR = 8.5D * 8.5D;

    private static final double HOLDING_NPC_REACHED_SQR = 2.5D * 2.5D;
    private static final double HOLDING_ANIMAL_REACHED_SQR = 4.0D * 4.0D;

    private static final int GATE_STALL_TICKS = 140;
    private static final int MAX_GATE_REALIGN_ATTEMPTS = 2;
    private static final double GATE_REALIGN_DISTANCE = 4.0D;
    private static final int PEN_HARVEST_COOLDOWN_TICKS = 6000;

    private final CyberNpcEntity npc;

    @Nullable
    private BlockPos primaryGate;

    @Nullable
    private BlockPos penAnchor;

    @Nullable
    private BlockPos holdingPoint;

    private Set<BlockPos> penCells = Set.of();
    private Set<BlockPos> gates = Set.of();

    @Nullable
    private EntityType<?> livestockType;

    @Nullable
    private UUID recruitAnimalId;

    @Nullable
    private BlockPos activeEntryGate;

    @Nullable
    private BlockPos activeExitGate;

    @Nullable
    private UUID failedRecruitId;

    private int failedRecruitCooldown;
    private int recruitApproachTicks;
    private int claimSearchCooldown;
    private int claimValidationCooldown;
    private int breedCooldown;
    private int penHarvestCooldown;
    private int transferStallTicks;
    private int gateRealignStage;
    private int gateRealignAttempts;
    private double lastAnimalHoldingDistance = Double.MAX_VALUE;

    private boolean leashAttachedByNpc;
    private boolean busy;
    private boolean exitingPen;
    private boolean deliveredAnimal;

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
        String hold = holdingPoint == null ? "?" : formatPos(holdingPoint);
        return formatPos(penAnchor)
                + " [" + penCells.size() + " cells, "
                + gateCount + " gate" + (gateCount == 1 ? "" : "s")
                + ", hold " + hold + "]";
    }

    @Nullable
    BlockPos getPenAnchor() {
        return penAnchor;
    }

    /**
     * Claim discovery is deliberately independent from hunger/pen management.
     * This makes a nearby unclaimed pen get reserved as soon as the NPC notices
     * it, similar to villagers reserving a workstation or bed.
     */
    void tickClaimDiscovery() {
        if (!(npc.level() instanceof ServerLevel level)) {
            return;
        }

        if (failedRecruitCooldown > 0) {
            failedRecruitCooldown--;
            if (failedRecruitCooldown == 0) {
                failedRecruitId = null;
            }
        }

        if (penHarvestCooldown > 0) {
            penHarvestCooldown--;
        }

        ensureCorral(level);
    }

    boolean tick() {
        if (!(npc.level() instanceof ServerLevel level)) {
            busy = false;
            return false;
        }

        if (breedCooldown > 0) {
            breedCooldown--;
        }

        if (penAnchor == null || primaryGate == null || penCells.isEmpty()) {
            ensureCorral(level);
        }

        // An active transfer must be finished cleanly even if hunger changes.
        if (hasActiveTransfer(level)) {
            busy = true;
            return tickTransfer(level);
        }

        if (npc.isCombatActive()
                || npc.hasCollectedRawFood()
                || npc.getHunger() > MANAGEMENT_HUNGER_THRESHOLD) {
            stopLeading(level);
            busy = false;
            debugActivity = penAnchor == null ? "No claimed pen" : "Claimed pen idle";
            return false;
        }

        if (penAnchor == null || primaryGate == null) {
            busy = false;
            debugActivity = "No claimed pen";
            return false;
        }

        if (penCells.isEmpty()) {
            Vec3 destination = Vec3.atBottomCenterOf(primaryGate);
            npc.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.95D);
            npc.setSprinting(true);
            busy = true;
            debugActivity = "Returning to claimed pen";
            return true;
        }

        List<Animal> insideAnimals = getAnimalsInside(level);
        livestockType = chooseLivestockType(level, insideAnimals);

        if (livestockType == null) {
            busy = false;
            debugActivity = "Claimed pen waiting for nearby livestock";
            return false;
        }

        List<Animal> adultsInside = insideAnimals.stream()
                .filter(animal -> animal.getType() == livestockType)
                .filter(animal -> !animal.isBaby())
                .toList();

        List<Animal> babiesInside = insideAnimals.stream()
                .filter(animal -> animal.getType() == livestockType)
                .filter(Animal::isBaby)
                .toList();

        // Once the herd has successfully produced a baby, a hungry NPC may take
        // exactly one adult as food. The cooldown prevents it from immediately
        // consuming the remaining adults while that baby is still growing.
        if (npc.getHunger() <= CyberNpcEntity.HUNT_HUNGER_THRESHOLD
                && !babiesInside.isEmpty()
                && adultsInside.size() >= 2
                && penHarvestCooldown <= 0) {
            Animal harvest = adultsInside.stream()
                    .filter(animal -> !animal.isInLove())
                    .min(Comparator.comparingDouble(npc::distanceToSqr))
                    .orElse(adultsInside.get(0));

            penHarvestCooldown = PEN_HARVEST_COOLDOWN_TICKS;
            closeAllGates(level);
            stopLeading(level);
            busy = false;
            debugActivity = "Harvesting one adult from successful herd";
            npc.beginCorralHunt(harvest);
            return true;
        }

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

        Animal recruit = chooseRecruit(level);
        if (recruit == null) {
            closeAllGates(level);
            busy = false;
            debugActivity = "Waiting for reachable livestock";
            return false;
        }

        recruitAnimalId = recruit.getUUID();
        activeEntryGate = selectEntryGate(recruit);
        activeExitGate = null;
        recruitApproachTicks = 0;
        transferStallTicks = 0;
        gateRealignStage = 0;
        gateRealignAttempts = 0;
        lastAnimalHoldingDistance = Double.MAX_VALUE;
        leashAttachedByNpc = false;
        exitingPen = false;
        deliveredAnimal = false;
        busy = true;
        debugActivity = "Approaching livestock";
        return tickTransfer(level);
    }

    void interrupt() {
        if (npc.level() instanceof ServerLevel level) {
            stopLeading(level);
            closeAllGates(level);
            clearDeliveredRestriction(level);
        }

        clearTransferState();
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
        if (penHarvestCooldown > 0) {
            tag.putInt("CyberNpcPenHarvestCooldown", penHarvestCooldown);
        }
    }

    void readSaveData(CompoundTag tag) {
        penAnchor = tag.contains("CyberNpcPenAnchor")
                ? BlockPos.of(tag.getLong("CyberNpcPenAnchor"))
                : null;
        primaryGate = tag.contains("CyberNpcPenGate")
                ? BlockPos.of(tag.getLong("CyberNpcPenGate"))
                : null;

        holdingPoint = null;
        penCells = Set.of();
        gates = primaryGate == null ? Set.of() : Set.of(primaryGate);
        livestockType = null;
        recruitAnimalId = null;
        activeEntryGate = null;
        activeExitGate = null;
        failedRecruitId = null;
        failedRecruitCooldown = 0;
        recruitApproachTicks = 0;
        claimSearchCooldown = 0;
        claimValidationCooldown = 0;
        breedCooldown = 0;
        penHarvestCooldown = tag.contains("CyberNpcPenHarvestCooldown")
                ? Math.max(0, tag.getInt("CyberNpcPenHarvestCooldown"))
                : 0;
        transferStallTicks = 0;
        gateRealignStage = 0;
        gateRealignAttempts = 0;
        lastAnimalHoldingDistance = Double.MAX_VALUE;
        leashAttachedByNpc = false;
        exitingPen = false;
        deliveredAnimal = false;
        busy = false;
        debugActivity = penAnchor == null ? "No pen task" : "Claimed pen saved";
    }

    private boolean hasActiveTransfer(ServerLevel level) {
        if (exitingPen) {
            return true;
        }

        return resolveRecruit(level) != null;
    }

    private boolean tickTransfer(ServerLevel level) {
        if (exitingPen) {
            debugActivity = "Leaving claimed pen";
            return tickExitPen(level);
        }

        Animal recruit = resolveRecruit(level);
        if (!isValidTransferAnimal(recruit)) {
            abortRecruit(level, false);
            return false;
        }

        if (activeEntryGate == null || !gates.contains(activeEntryGate)) {
            activeEntryGate = selectEntryGate(recruit);
        }

        if (activeEntryGate == null || holdingPoint == null) {
            abortRecruit(level, false);
            return false;
        }

        // If the leash we created vanished on its own, do not immediately create
        // another one. Vanilla drops a physical lead when a stretched leash snaps;
        // reattaching every tick was the source of the lead-item duplication bug.
        if (leashAttachedByNpc && recruit.getLeashHolder() != npc) {
            debugActivity = "Leash broke; abandoning this animal";
            abortRecruit(level, true);
            return false;
        }

        if (recruit.getLeashHolder() != npc) {
            return tickApproachRecruit(level, recruit);
        }

        double leashDistance = npc.distanceToSqr(recruit);

        if (leashDistance > LEASH_ABORT_DISTANCE_SQR) {
            recruit.dropLeash(true, false);
            leashAttachedByNpc = false;
            debugActivity = "Animal fell too far behind";
            abortRecruit(level, true);
            return false;
        }

        if (leashDistance > LEASH_WAIT_DISTANCE_SQR) {
            npc.getNavigation().stop();
            npc.setSprinting(false);
            debugActivity = "Waiting for leashed animal to catch up";
            return true;
        }

        if (gateRealignStage > 0) {
            return tickGateRealignment(level, recruit);
        }

        BlockPos outside = outsideForGate(activeEntryGate);
        BlockPos inside = insideForGate(activeEntryGate);

        if (outside == null || inside == null) {
            abortRecruit(level, false);
            return false;
        }

        // Keep every gate shut while approaching. Only open the chosen entrance
        // when the NPC is actually at it, reducing the chance of livestock escaping.
        if (!isNpcInsidePen()
                && npc.distanceToSqr(Vec3.atBottomCenterOf(outside)) > 6.25D) {
            closeAllGates(level);
            npc.getNavigation().moveTo(
                    outside.getX() + 0.5D,
                    outside.getY(),
                    outside.getZ() + 0.5D,
                    0.88D
            );
            npc.setSprinting(false);
            debugActivity = "Bringing livestock to pen gate";
            return true;
        }

        openOnlyGate(level, activeEntryGate);

        Vec3 hold = Vec3.atBottomCenterOf(holdingPoint);

        double animalHoldingDistance = recruit.distanceToSqr(hold);
        if (lastAnimalHoldingDistance - animalHoldingDistance > 0.75D) {
            transferStallTicks = 0;
        } else {
            transferStallTicks++;
        }
        lastAnimalHoldingDistance = animalHoldingDistance;

        if (transferStallTicks >= GATE_STALL_TICKS
                && (isNpcInsidePen()
                || npc.distanceToSqr(Vec3.atBottomCenterOf(activeEntryGate)) <= 36.0D)) {
            if (gateRealignAttempts >= MAX_GATE_REALIGN_ATTEMPTS) {
                debugActivity = "Animal would not enter pen; abandoning transfer";
                abortRecruit(level, true);
                return false;
            }

            gateRealignAttempts++;
            gateRealignStage = 1;
            transferStallTicks = 0;
            lastAnimalHoldingDistance = Double.MAX_VALUE;
            debugActivity = "Realigning livestock with pen gate";
            return tickGateRealignment(level, recruit);
        }

        npc.getNavigation().moveTo(hold.x, hold.y, hold.z, 0.78D);
        npc.setSprinting(false);
        debugActivity = "Taking livestock to back of pen";

        boolean npcAtHolding = npc.distanceToSqr(hold) <= HOLDING_NPC_REACHED_SQR;
        boolean animalAtHolding = animalHoldingDistance <= HOLDING_ANIMAL_REACHED_SQR;

        if (npcAtHolding && animalAtHolding && isInsidePen(recruit)) {
            recruit.dropLeash(true, false);
            leashAttachedByNpc = false;
            recruit.getNavigation().stop();

            // Hold it at the safest interior point only while the NPC exits.
            // The restriction is cleared after all gates are closed again.
            recruit.restrictTo(holdingPoint, 3);

            npc.clearUtilityItem();
            deliveredAnimal = true;
            exitingPen = true;
            activeExitGate = selectExitGate();

            closeAllGates(level);
            if (activeExitGate != null) {
                openGate(level, activeExitGate);
            }

            debugActivity = "Livestock secured at back; leaving pen";
            return tickExitPen(level);
        }

        return true;
    }

    private boolean tickGateRealignment(ServerLevel level, Animal recruit) {
        if (activeEntryGate == null) {
            abortRecruit(level, true);
            return false;
        }

        BlockPos outside = outsideForGate(activeEntryGate);
        BlockPos inside = insideForGate(activeEntryGate);
        if (outside == null || inside == null) {
            abortRecruit(level, true);
            return false;
        }

        openOnlyGate(level, activeEntryGate);
        npc.setSprinting(false);

        Vec3 gateCenter = Vec3.atBottomCenterOf(activeEntryGate);
        Vec3 outsideCenter = Vec3.atBottomCenterOf(outside);
        Vec3 outward = outsideCenter.subtract(gateCenter);
        outward = new Vec3(outward.x, 0.0D, outward.z);

        if (outward.lengthSqr() < 0.001D) {
            outward = outsideCenter.subtract(Vec3.atBottomCenterOf(inside));
            outward = new Vec3(outward.x, 0.0D, outward.z);
        }

        if (outward.lengthSqr() < 0.001D) {
            abortRecruit(level, true);
            return false;
        }

        Vec3 awayPoint = outsideCenter.add(outward.normalize().scale(GATE_REALIGN_DISTANCE));

        if (gateRealignStage == 1) {
            npc.getNavigation().moveTo(awayPoint.x, awayPoint.y, awayPoint.z, 0.86D);
            debugActivity = "Backing away to straighten livestock at gate";

            if (npc.distanceToSqr(awayPoint) <= 4.0D) {
                gateRealignStage = 2;
                npc.getNavigation().stop();
            }

            return true;
        }

        Vec3 insidePoint = Vec3.atBottomCenterOf(inside);
        npc.getNavigation().moveTo(insidePoint.x, insidePoint.y, insidePoint.z, 0.82D);
        debugActivity = "Walking back through gate with aligned livestock";

        if (isNpcInsidePen() || npc.distanceToSqr(insidePoint) <= 2.25D) {
            gateRealignStage = 0;
            transferStallTicks = 0;
            lastAnimalHoldingDistance = recruit.distanceToSqr(Vec3.atBottomCenterOf(holdingPoint));
        }

        return true;
    }

    private boolean tickApproachRecruit(ServerLevel level, Animal recruit) {
        if (recruit.isLeashed() && recruit.getLeashHolder() != npc) {
            debugActivity = "Animal already belongs to another lead";
            abortRecruit(level, true);
            return false;
        }

        recruitApproachTicks++;

        if (recruitApproachTicks > RECRUIT_APPROACH_TIMEOUT
                || npc.distanceToSqr(recruit) > RECRUIT_MAX_NPC_DISTANCE_SQR
                || distanceToPen(recruit.position()) > RECRUIT_MAX_PEN_DISTANCE_SQR) {
            debugActivity = "Animal is too far or unreachable";
            abortRecruit(level, true);
            return false;
        }

        closeAllGates(level);
        npc.clearUtilityItem();
        npc.setSprinting(false);

        double distance = npc.distanceToSqr(recruit);
        if (distance > LEASH_ATTACH_DISTANCE_SQR) {
            npc.getNavigation().moveTo(recruit, 0.95D);
            debugActivity = "Walking to livestock before attaching lead";
            return true;
        }

        if (!npc.getSensing().hasLineOfSight(recruit)) {
            npc.getNavigation().moveTo(recruit, 0.80D);
            debugActivity = "Finding clear access to livestock";
            return true;
        }

        npc.getNavigation().stop();
        recruit.setLeashedTo(npc, true);
        leashAttachedByNpc = true;
        recruitApproachTicks = 0;
        npc.equipUtilityItem(new ItemStack(Items.LEAD));
        debugActivity = "Lead attached; moving livestock to pen";
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

        openOnlyGate(level, activeExitGate);

        BlockPos inside = insideForGate(activeExitGate);
        BlockPos outside = outsideForGate(activeExitGate);

        if (inside == null || outside == null) {
            finishTransfer(level);
            return false;
        }

        Vec3 insideSpot = Vec3.atBottomCenterOf(inside);
        Vec3 outsideSpot = Vec3.atBottomCenterOf(outside);

        // Route to the inside face first. This prevents navigation from choosing
        // another fence edge or getting stuck trying to path straight through it.
        if (npc.distanceToSqr(insideSpot) > 2.25D) {
            npc.getNavigation().moveTo(insideSpot.x, insideSpot.y, insideSpot.z, 0.92D);
            npc.setSprinting(false);
            debugActivity = "Walking to pen exit gate";
            return true;
        }

        npc.getNavigation().moveTo(outsideSpot.x, outsideSpot.y, outsideSpot.z, 0.95D);
        npc.setSprinting(false);
        debugActivity = "Passing through pen exit gate";

        if (!isNpcInsidePen() && npc.distanceToSqr(outsideSpot) < 9.0D) {
            finishTransfer(level);
            return false;
        }

        return true;
    }

    private void finishTransfer(ServerLevel level) {
        stopLeading(level);
        closeAllGates(level);
        clearDeliveredRestriction(level);
        clearTransferState();
        busy = false;
        npc.getNavigation().stop();
        npc.setSprinting(false);
        debugActivity = "Livestock secured and gates closed";
    }

    private void abortRecruit(ServerLevel level, boolean blacklist) {
        Animal recruit = resolveRecruit(level);

        if (recruit != null && recruit.getLeashHolder() == npc) {
            // Explicitly suppress the vanilla lead-item drop.
            recruit.dropLeash(true, false);
        }

        if (blacklist && recruitAnimalId != null) {
            failedRecruitId = recruitAnimalId;
            failedRecruitCooldown = 1200;
        }

        npc.clearUtilityItem();
        npc.getNavigation().stop();
        closeAllGates(level);
        clearTransferState();
        busy = false;
    }

    private void clearTransferState() {
        recruitAnimalId = null;
        activeEntryGate = null;
        activeExitGate = null;
        recruitApproachTicks = 0;
        transferStallTicks = 0;
        gateRealignStage = 0;
        gateRealignAttempts = 0;
        lastAnimalHoldingDistance = Double.MAX_VALUE;
        leashAttachedByNpc = false;
        exitingPen = false;
        deliveredAnimal = false;
    }

    private void stopLeading(ServerLevel level) {
        Animal recruit = resolveRecruit(level);

        if (recruit != null && recruit.getLeashHolder() == npc) {
            recruit.dropLeash(true, false);
        }

        leashAttachedByNpc = false;
        npc.clearUtilityItem();
    }

    private void clearDeliveredRestriction(ServerLevel level) {
        if (!deliveredAnimal) {
            return;
        }

        Animal recruit = resolveRecruit(level);
        if (recruit != null) {
            recruit.clearRestriction();
            recruit.getNavigation().stop();
        }
    }

    @Nullable
    private Animal resolveRecruit(ServerLevel level) {
        if (recruitAnimalId == null) {
            return null;
        }

        Entity entity = level.getEntity(recruitAnimalId);
        return entity instanceof Animal animal ? animal : null;
    }

    private boolean isValidTransferAnimal(@Nullable Animal animal) {
        return animal != null
                && animal.isAlive()
                && !animal.isBaby()
                && animal.getType() == livestockType;
    }

    @Nullable
    private Animal chooseRecruit(ServerLevel level) {
        if (livestockType == null || primaryGate == null) {
            return null;
        }

        AABB searchBox = npc.getBoundingBox().inflate(
                RECRUIT_SEARCH_RADIUS,
                6.0D,
                RECRUIT_SEARCH_RADIUS
        );

        return level.getEntitiesOfClass(
                        Animal.class,
                        searchBox,
                        animal -> isValidRecruit(animal)
                                && animal.getType() == livestockType
                ).stream()
                .filter(animal -> distanceToPen(animal.position()) <= RECRUIT_MAX_PEN_DISTANCE_SQR)
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);
    }

    private boolean isValidRecruit(@Nullable Animal animal) {
        if (animal == null
                || !animal.isAlive()
                || animal.isBaby()
                || !animal.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)
                || isInsidePen(animal)
                || animal.isLeashed()) {
            return false;
        }

        return failedRecruitId == null
                || failedRecruitCooldown <= 0
                || !failedRecruitId.equals(animal.getUUID());
    }

    private double distanceToPen(Vec3 position) {
        if (penCells.isEmpty()) {
            return Double.MAX_VALUE;
        }

        return penCells.stream()
                .mapToDouble(cell -> position.distanceToSqr(Vec3.atBottomCenterOf(cell)))
                .min()
                .orElse(Double.MAX_VALUE);
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

        AABB searchBox = new AABB(primaryGate).inflate(
                RECRUIT_SEARCH_RADIUS,
                6.0D,
                RECRUIT_SEARCH_RADIUS
        );

        Map<EntityType<?>, List<Animal>> groups = new HashMap<>();

        for (Animal animal : level.getEntitiesOfClass(
                Animal.class,
                searchBox,
                animal -> animal.isAlive()
                        && !animal.isBaby()
                        && animal.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)
                        && !animal.isLeashed()
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
        return !penCells.isEmpty() && isInsidePenPosition(animal.blockPosition());
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
        int minX = penCells.stream().mapToInt(pos -> pos.getX()).min().orElse(npc.blockPosition().getX());
        int maxX = penCells.stream().mapToInt(pos -> pos.getX()).max().orElse(npc.blockPosition().getX());
        int minY = penCells.stream().mapToInt(pos -> pos.getY()).min().orElse(npc.blockPosition().getY());
        int maxY = penCells.stream().mapToInt(pos -> pos.getY()).max().orElse(npc.blockPosition().getY());
        int minZ = penCells.stream().mapToInt(pos -> pos.getZ()).min().orElse(npc.blockPosition().getZ());
        int maxZ = penCells.stream().mapToInt(pos -> pos.getZ()).max().orElse(npc.blockPosition().getZ());

        return new AABB(minX, minY - 1, minZ, maxX + 1, maxY + 3, maxZ + 1);
    }

    private boolean ensureCorral(ServerLevel level) {
        CyberNpcWorldClaims claims = CyberNpcWorldClaims.get(level);

        if (penAnchor != null && primaryGate != null) {
            if (!claims.claimPen(penAnchor, penCells, npc.getUUID())) {
                clearClaim();
            } else if (penCells.isEmpty()) {
                if (!level.hasChunkAt(primaryGate)) {
                    debugActivity = "Claimed pen is outside loaded area";
                    return true;
                }

                Corral rebuilt = inspectGate(level, primaryGate);
                if (rebuilt != null
                        && rebuilt.anchor().equals(penAnchor)
                        && claims.claimPen(rebuilt.anchor(), rebuilt.cells(), npc.getUUID())) {
                    applyCorral(rebuilt);
                    claimValidationCooldown = CLAIM_VALIDATE_TICKS;
                    return true;
                }

                claims.releasePen(penAnchor, npc.getUUID());
                clearClaim();
            } else {
                if (claimValidationCooldown > 0) {
                    claimValidationCooldown--;
                    return true;
                }

                claimValidationCooldown = CLAIM_VALIDATE_TICKS;

                if (!level.hasChunkAt(primaryGate)) {
                    return true;
                }

                Corral rebuilt = inspectGate(level, primaryGate);
                if (rebuilt != null
                        && rebuilt.anchor().equals(penAnchor)
                        && claims.claimPen(rebuilt.anchor(), rebuilt.cells(), npc.getUUID())) {
                    applyCorral(rebuilt);
                    return true;
                }

                claims.releasePen(penAnchor, npc.getUUID());
                clearClaim();
            }
        }

        if (claimSearchCooldown > 0) {
            claimSearchCooldown--;
            return false;
        }

        claimSearchCooldown = CLAIM_RESCAN_TICKS;
        return findCorral(level);
    }

    private void clearClaim() {
        penAnchor = null;
        primaryGate = null;
        holdingPoint = null;
        penCells = Set.of();
        gates = Set.of();
        livestockType = null;
        claimValidationCooldown = 0;
    }

    private boolean findCorral(ServerLevel level) {
        BlockPos origin = npc.blockPosition();
        Map<Long, Corral> unique = new HashMap<>();

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

            unique.putIfAbsent(candidate.anchor().asLong(), candidate);
        }

        List<Corral> candidates = unique.values().stream()
                .sorted(Comparator.comparingDouble(corral ->
                        corral.primaryGate().distSqr(origin)))
                .toList();

        CyberNpcWorldClaims claims = CyberNpcWorldClaims.get(level);

        for (Corral candidate : candidates) {
            if (claims.claimPen(candidate.anchor(), candidate.cells(), npc.getUUID())) {
                applyCorral(candidate);
                claimValidationCooldown = CLAIM_VALIDATE_TICKS;
                debugActivity = "Claimed entire pen";
                return true;
            }
        }

        debugActivity = "No unclaimed pen nearby";
        return false;
    }

    private void applyCorral(Corral corral) {
        primaryGate = corral.primaryGate();
        penAnchor = corral.anchor();
        holdingPoint = corral.holdingPoint();
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
                        .comparingInt((BlockPos pos) -> pos.getX())
                        .thenComparingInt(pos -> pos.getY())
                        .thenComparingInt(pos -> pos.getZ()))
                .orElse(null);

        if (anchor == null) {
            return null;
        }

        BlockPos primary = discoveredGates.stream()
                .min(Comparator.comparingDouble(pos -> pos.distSqr(npc.blockPosition())))
                .orElse(gate.immutable());

        BlockPos holding = findHoldingPoint(chosen, discoveredGates);
        if (holding == null) {
            holding = anchor;
        }

        return new Corral(
                primary,
                anchor,
                holding,
                Set.copyOf(chosen),
                Set.copyOf(discoveredGates)
        );
    }

    @Nullable
    private BlockPos findHoldingPoint(Set<BlockPos> cells, Set<BlockPos> discoveredGates) {
        if (cells.isEmpty()) {
            return null;
        }

        return cells.stream()
                .max(Comparator
                        .comparingDouble((BlockPos cell) -> distanceFromNearestGate(cell, discoveredGates))
                        .thenComparingDouble(cell -> -cell.distSqr(npc.blockPosition())))
                .orElse(null);
    }

    private double distanceFromNearestGate(BlockPos cell, Set<BlockPos> discoveredGates) {
        return discoveredGates.stream()
                .mapToDouble(cell::distSqr)
                .min()
                .orElse(0.0D);
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
    private BlockPos selectEntryGate(Animal recruit) {
        if (gates.isEmpty()) {
            return primaryGate;
        }

        return gates.stream()
                .filter(gate -> outsideForGate(gate) != null && insideForGate(gate) != null)
                .min(Comparator.comparingDouble(gate -> {
                    BlockPos outside = outsideForGate(gate);
                    return outside == null
                            ? Double.MAX_VALUE
                            : recruit.distanceToSqr(Vec3.atBottomCenterOf(outside));
                }))
                .orElse(primaryGate);
    }

    @Nullable
    private BlockPos selectExitGate() {
        if (gates.isEmpty()) {
            return primaryGate;
        }

        return gates.stream()
                .filter(gate -> outsideForGate(gate) != null && insideForGate(gate) != null)
                .min(Comparator.comparingDouble(gate -> {
                    BlockPos inside = insideForGate(gate);
                    return inside == null
                            ? Double.MAX_VALUE
                            : npc.distanceToSqr(Vec3.atBottomCenterOf(inside));
                }))
                .orElse(primaryGate);
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

    private void openOnlyGate(ServerLevel level, BlockPos gateToOpen) {
        for (BlockPos gate : gates) {
            setGateOpen(level, gate, gate.equals(gateToOpen));
        }

        if (gates.isEmpty() && primaryGate != null) {
            setGateOpen(level, primaryGate, primaryGate.equals(gateToOpen));
        }
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

    private static String formatPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private record Corral(
            BlockPos primaryGate,
            BlockPos anchor,
            BlockPos holdingPoint,
            Set<BlockPos> cells,
            Set<BlockPos> gates
    ) {
    }
}
