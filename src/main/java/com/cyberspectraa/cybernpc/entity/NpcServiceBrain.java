package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.building.BuildingPointType;
import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.building.BuildingType;
import com.cyberspectraa.cybernpc.mail.MailItemData;
import com.cyberspectraa.cybernpc.mail.MailRecord;
import com.cyberspectraa.cybernpc.mail.MailSavedData;
import com.cyberspectraa.cybernpc.mail.MailState;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.cyberspectraa.cybernpc.registry.ModSounds;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.cyberspectraa.cybernpc.service.GuardPostSavedData;
import com.cyberspectraa.cybernpc.service.SpecialNpcSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

final class NpcServiceBrain {
    private static final int SEARCH_INTERVAL = 40;
    private static final int REPATH_INTERVAL = 10;
    private static final double SEARCH_RADIUS_SQR = 96.0D * 96.0D;
    private static final double ARRIVE_SQR = 3.0D * 3.0D;
    private static final double ANCHOR_ARRIVE_SQR = 3.5D * 3.5D;
    private static final double DIRECT_PLAYER_DELIVERY_RADIUS_SQR =
            48.0D * 48.0D;
    private static final int LETTER_BOX_CAPACITY = 9;
    private static final int SATCHEL_CAPACITY = 9;

    private static final int BREATH_CHECK_INTERVAL = 10;
    private static final int BREATH_REARM_COOLDOWN_TICKS = 160;
    private static final double BREATH_TRIGGER_DISTANCE = 2.6D;
    private static final double BREATH_RESET_DISTANCE = 3.6D;
    private static final float BREATH_VOLUME = 0.42F;

    private static final int THREAT_SCAN_INTERVAL = 10;
    private static final int BELL_ALERT_COOLDOWN_TICKS = 200;
    private static final int PANIC_AFTER_RING_TICKS = 100;
    private static final int BELL_SEARCH_RADIUS = 24;
    private static final int BELL_SEARCH_VERTICAL = 6;
    private static final double BELL_ARRIVE_SQR = 9.0D;

    private static final int GUARD_SCAN_INTERVAL = 20;
    private static final int GUARD_ATTACK_COOLDOWN = 12;
    private static final int GUARD_ALERT_TICKS = 600;
    private static final double GUARD_SCAN_RADIUS = 24.0D;
    private static final double GUARD_TOWN_RADIUS = 48.0D;
    private static final double GUARD_TOWN_RADIUS_SQR =
            GUARD_TOWN_RADIUS * GUARD_TOWN_RADIUS;
    private static final double GUARD_ATTACK_DISTANCE_SQR = 8.0D;
    private static final double GUARD_ALERT_RADIUS = 72.0D;
    private static final int GUARD_PATROL_MIN_RADIUS = 10;
    private static final int GUARD_PATROL_EXTRA_RADIUS = 24;
    private static final double GUARD_PATROL_REACHED_SQR = 6.25D;
    private static final int GUARD_POST_DUTY_MIN_TICKS = 600;
    private static final int GUARD_POST_DUTY_EXTRA_TICKS = 600;
    private static final int GUARD_POST_REST_MIN_TICKS = 160;
    private static final int GUARD_POST_REST_EXTRA_TICKS = 200;
    private static final double GUARD_POST_SNAP_SQR = 0.75D * 0.75D;

    private static final double POPE_CHURCH_SEARCH_RADIUS_SQR =
            96.0D * 96.0D;
    private static final double POPE_POINT_ARRIVE_SQR = 1.75D * 1.75D;
    private static final double POPE_VISITOR_RADIUS = 8.0D;
    private static final int POPE_ALTAR_HOLD_TICKS = 300;
    private static final int POPE_ROAM_PAUSE_MIN = 80;
    private static final int POPE_ROAM_PAUSE_EXTRA = 160;
    private static final int POPE_VISITOR_SCAN_INTERVAL = 40;
    private static final int POPE_VISITOR_HOLD_TICKS = 70;

    private final CyberNpcEntity npc;
    private final List<Long> satchelIds = new ArrayList<>();

    @Nullable
    private BlockPos pickupPos;

    private boolean pickedUp;
    private int searchCooldown;
    private int repathCooldown;
    private int breathCheckCooldown;
    private int breathRearmCooldown;
    private boolean breathPlayerClose;

    private int threatScanCooldown;
    private int bellAlertCooldown;
    @Nullable
    private UUID panicThreatId;
    @Nullable
    private BlockPos panicBellPos;
    private boolean panicBellRung;
    private int panicTicks;

    private int guardScanCooldown;
    private int guardAttackCooldown;
    private int guardAlertTicks;
    @Nullable
    private UUID guardTargetId;
    @Nullable
    private BlockPos guardAlarmBellPos;
    private boolean guardCallingBackup;
    @Nullable
    private BlockPos guardPatrolTarget;
    private int guardPatrolPauseTicks;
    @Nullable
    private GuardPostSavedData.GuardPost guardPostTarget;
    private int guardPostDutyTicks;
    private int guardPostRestTicks;
    @Nullable
    private BlockPos guardRespondBellPos;
    private int guardBellHoldTicks;

    @Nullable
    private BlockPos popeTarget;
    @Nullable
    private BlockPos popeActiveBed;
    @Nullable
    private UUID popeVisitorId;
    private int popePauseTicks;
    private int popeVisitorScanCooldown;
    private int popeVisitorHoldTicks;
    private long popeLastAltarDay = Long.MIN_VALUE;

    NpcServiceBrain(CyberNpcEntity npc) {
        this.npc = npc;
    }

    void tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || npc.getNpcType() == NpcType.WILD) {
            return;
        }

        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        if (role == NpcServiceRole.NONE) {
            clearVisualLetter();
            return;
        }

        SpecialNpcSavedData data = SpecialNpcSavedData.get(level);
        SpecialNpcSavedData.SpecialNpcRecord record =
                data.getRecord(npc.getSpecialNpcId());

        if (record == null) {
            return;
        }

        if (bellAlertCooldown > 0) {
            bellAlertCooldown--;
        }

        if (role != NpcServiceRole.GUARD
                && tickSpecialNpcDanger(level)) {
            if (role == NpcServiceRole.POPE) {
                wakePope(level);
            }
            return;
        }

        if (role == NpcServiceRole.BANKER) {
            tickBanker(level, data, record);
            return;
        }

        if (role == NpcServiceRole.COURIER) {
            tickBreathingEasterEgg(level);
            tickCourier(level, data, record);
            return;
        }

        if (role == NpcServiceRole.GUARD) {
            tickGuard(level, data, record);
            return;
        }

        if (role == NpcServiceRole.POPE) {
            tickPope(level, data, record);
        }
    }

    private boolean tickSpecialNpcDanger(ServerLevel level) {
        if (panicThreatId == null) {
            if (bellAlertCooldown > 0) {
                return false;
            }

            if (threatScanCooldown > 0) {
                threatScanCooldown--;
                return false;
            }
            threatScanCooldown = THREAT_SCAN_INTERVAL;

            LivingEntity threat = findImmediateThreat(level);
            if (threat == null) {
                return false;
            }

            panicThreatId = threat.getUUID();
            panicBellPos = findNearestBell(level);
            panicBellRung = false;
            panicTicks = PANIC_AFTER_RING_TICKS;
            npc.getNavigation().stop();
        }

        LivingEntity threat = resolveLiving(level, panicThreatId);
        if (threat == null || !threat.isAlive()) {
            clearPanic();
            return false;
        }

        if (!panicBellRung) {
            if (panicBellPos == null) {
                GuardAlarmSystem.alertGuards(
                        level,
                        npc.blockPosition(),
                        threat
                );
                bellAlertCooldown = BELL_ALERT_COOLDOWN_TICKS;
                panicBellRung = true;
            } else if (npc.blockPosition().distSqr(panicBellPos)
                    <= BELL_ARRIVE_SQR) {
                npc.getNavigation().stop();
                npc.setSprinting(false);
                if (GuardAlarmSystem.ringBellAndAlert(
                        level,
                        panicBellPos,
                        npc,
                        threat
                )) {
                    bellAlertCooldown =
                            BELL_ALERT_COOLDOWN_TICKS;
                    panicBellRung = true;
                } else {
                    panicBellPos = null;
                }
            } else {
                npc.setSprinting(true);
                navigateTo(panicBellPos, 1.15D);
                return true;
            }
        }

        if (panicTicks > 0) {
            panicTicks--;
            fleeFromThreat(threat);
            return true;
        }

        clearPanic();
        return false;
    }

    @Nullable
    private LivingEntity findImmediateThreat(ServerLevel level) {
        LivingEntity attacker = npc.getLastHurtByMob();
        if (isAlarmThreat(attacker)) {
            return attacker;
        }

        return level.getEntitiesOfClass(
                        Mob.class,
                        npc.getBoundingBox().inflate(12.0D, 6.0D, 12.0D),
                        mob -> mob != npc
                                && mob.isAlive()
                                && mob instanceof Enemy
                                && mob.getTarget() == npc
                )
                .stream()
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);
    }

    private boolean isAlarmThreat(@Nullable LivingEntity threat) {
        if (threat == null || threat == npc || !threat.isAlive()) {
            return false;
        }

        if (threat instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        return true;
    }

    private void fleeFromThreat(LivingEntity threat) {
        Vec3 away = DefaultRandomPos.getPosAway(
                npc,
                12,
                5,
                threat.position()
        );

        if (away != null) {
            npc.setSprinting(true);
            npc.getNavigation().moveTo(
                    away.x,
                    away.y,
                    away.z,
                    1.10D
            );
        }
    }

    private void clearPanic() {
        panicThreatId = null;
        panicBellPos = null;
        panicBellRung = false;
        panicTicks = 0;
        npc.setSprinting(false);
    }

    @Nullable
    private BlockPos findNearestBell(ServerLevel level) {
        BlockPos origin = npc.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(
                        -BELL_SEARCH_RADIUS,
                        -BELL_SEARCH_VERTICAL,
                        -BELL_SEARCH_RADIUS
                ),
                origin.offset(
                        BELL_SEARCH_RADIUS,
                        BELL_SEARCH_VERTICAL,
                        BELL_SEARCH_RADIUS
                )
        )) {
            if (!level.getBlockState(pos).is(Blocks.BELL)) {
                continue;
            }

            double distance = origin.distSqr(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }

        return best;
    }

    private void broadcastGuardAlert(
            ServerLevel level,
            BlockPos source,
            LivingEntity threat
    ) {
        GuardAlarmSystem.alertGuards(level, source, threat);
    }

    private void tickPope(
            ServerLevel level,
            SpecialNpcSavedData data,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        clearVisualLetter();

        BuildingSavedData buildings = BuildingSavedData.get(level);
        BuildingSavedData.BuildingRecord church =
                resolvePopeChurch(level, buildings, record);

        if (church == null) {
            wakePope(level);
            popeTarget = null;
            popeVisitorId = null;
            popeVisitorHoldTicks = 0;

            SpecialNpcSavedData.Anchor desired = level.isNight()
                    ? firstNonNull(record.home(), record.work())
                    : firstNonNull(record.work(), record.home());

            if (desired != null
                    && moveToAnchor(level, desired, 0.72D)
                    && record.returningToWork()
                    && desired == record.work()) {
                data.markReachedWork(record.specialId());
            }
            return;
        }

        if (level.isNight()) {
            tickPopeNight(level, buildings, church);
            return;
        }

        wakePope(level);

        if (record.returningToWork()) {
            BlockPos arrival = chooseChurchArrival(
                    buildings,
                    church,
                    record.work()
            );
            if (arrival != null
                    && npc.blockPosition().distSqr(arrival)
                    > POPE_POINT_ARRIVE_SQR) {
                navigateTo(arrival, 0.76D);
                return;
            }
            data.markReachedWork(record.specialId());
        }

        long minecraftDay = Math.floorDiv(
                level.getDayTime(),
                24_000L
        );
        long timeOfDay = Math.floorMod(
                level.getDayTime(),
                24_000L
        );

        // Each morning the Pope deliberately visits the altar before
        // returning to normal church roaming.
        if (timeOfDay < 3_000L
                && popeLastAltarDay != minecraftDay) {
            BuildingSavedData.ActivityPoint altar =
                    buildings.nearestPoint(
                            church,
                            BuildingPointType.ALTAR,
                            npc.blockPosition()
                    );

            if (altar != null) {
                if (!altar.pos().equals(popeTarget)) {
                    popeTarget = altar.pos();
                    popePauseTicks = 0;
                }

                if (npc.blockPosition().distSqr(altar.pos())
                        > POPE_POINT_ARRIVE_SQR) {
                    navigateTo(altar.pos(), 0.72D);
                    return;
                }

                npc.getNavigation().stop();
                npc.setSprinting(false);
                faceYaw(altar.yaw());

                if (popePauseTicks <= 0) {
                    popePauseTicks = POPE_ALTAR_HOLD_TICKS;
                }

                popePauseTicks--;
                if (popePauseTicks <= 0) {
                    popeLastAltarDay = minecraftDay;
                    popeTarget = null;
                }
                return;
            }

            // No altar point was configured, so do not retry every tick.
            popeLastAltarDay = minecraftDay;
        }

        if (tickPopeVisitorInteraction(level, church)) {
            return;
        }

        if (popePauseTicks > 0) {
            popePauseTicks--;
            npc.getNavigation().stop();
            npc.setSprinting(false);
            return;
        }

        if (popeTarget == null
                || !church.contains(popeTarget)
                || npc.blockPosition().distSqr(popeTarget)
                <= POPE_POINT_ARRIVE_SQR) {
            popeTarget = choosePopeRoamTarget(
                    level,
                    buildings,
                    church
            );

            if (popeTarget == null) {
                npc.getNavigation().stop();
                return;
            }

            if (npc.blockPosition().distSqr(popeTarget)
                    <= POPE_POINT_ARRIVE_SQR) {
                popePauseTicks = POPE_ROAM_PAUSE_MIN
                        + npc.getRandom().nextInt(
                        POPE_ROAM_PAUSE_EXTRA + 1
                );
                npc.getNavigation().stop();
                return;
            }
        }

        navigateTo(popeTarget, 0.68D);
    }

    @Nullable
    private BuildingSavedData.BuildingRecord resolvePopeChurch(
            ServerLevel level,
            BuildingSavedData buildings,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        SpecialNpcSavedData.Anchor work = record.work();
        if (work != null
                && work.dimension().equals(level.dimension())) {
            BuildingSavedData.BuildingRecord atWork =
                    buildings.getBuildingAt(
                            level.dimension(),
                            work.pos()
                    );
            if (atWork != null
                    && atWork.type() == BuildingType.CHURCH) {
                return atWork;
            }

            BuildingSavedData.BuildingRecord nearWork =
                    buildings.findNearest(
                            level.dimension(),
                            BuildingType.CHURCH,
                            work.pos(),
                            POPE_CHURCH_SEARCH_RADIUS_SQR
                    );
            if (nearWork != null) {
                return nearWork;
            }
        }

        BuildingSavedData.BuildingRecord current =
                buildings.getBuildingAt(
                        level.dimension(),
                        npc.blockPosition()
                );
        if (current != null
                && current.type() == BuildingType.CHURCH) {
            return current;
        }

        return buildings.findNearest(
                level.dimension(),
                BuildingType.CHURCH,
                npc.blockPosition(),
                POPE_CHURCH_SEARCH_RADIUS_SQR
        );
    }

    @Nullable
    private BlockPos chooseChurchArrival(
            BuildingSavedData buildings,
            BuildingSavedData.BuildingRecord church,
            @Nullable SpecialNpcSavedData.Anchor work
    ) {
        if (work != null && church.contains(work.pos())) {
            return work.pos();
        }

        BuildingSavedData.ActivityPoint altar =
                buildings.nearestPoint(
                        church,
                        BuildingPointType.ALTAR,
                        npc.blockPosition()
                );
        if (altar != null) {
            return altar.pos();
        }

        return church.core();
    }

    private void tickPopeNight(
            ServerLevel level,
            BuildingSavedData buildings,
            BuildingSavedData.BuildingRecord church
    ) {
        popeVisitorId = null;
        popeVisitorHoldTicks = 0;
        popePauseTicks = 0;

        if (npc.isSleeping()) {
            npc.getNavigation().stop();
            npc.setSprinting(false);
            return;
        }

        BuildingSavedData.ActivityPoint bedPoint =
                buildings.nearestPoint(
                        church,
                        BuildingPointType.BED,
                        npc.blockPosition()
                );

        BlockPos bedHead = bedPoint == null
                ? null
                : normalizePopeBedHead(level, bedPoint.pos());

        if (bedHead != null && !isPopeBedOccupied(level, bedHead)) {
            popeTarget = bedHead;
            Vec3 bedCenter = Vec3.atBottomCenterOf(bedHead);

            if (npc.distanceToSqr(bedCenter)
                    > POPE_POINT_ARRIVE_SQR) {
                navigateTo(bedHead, 0.72D);
                return;
            }

            npc.getNavigation().stop();
            npc.setSprinting(false);
            setPopeBedOccupied(level, bedHead, true);
            popeActiveBed = bedHead;
            npc.startSleeping(bedHead);
            popeTarget = null;
            return;
        }

        if (popeTarget == null
                || !church.contains(popeTarget)
                || npc.blockPosition().distSqr(popeTarget)
                <= POPE_POINT_ARRIVE_SQR) {
            popeTarget = buildings.randomInteriorTarget(
                    level,
                    church,
                    npc.getRandom()
            );
        }

        if (popeTarget == null) {
            npc.getNavigation().stop();
            return;
        }

        if (npc.blockPosition().distSqr(popeTarget)
                <= POPE_POINT_ARRIVE_SQR) {
            npc.getNavigation().stop();
            return;
        }

        navigateTo(popeTarget, 0.68D);
    }

    private boolean tickPopeVisitorInteraction(
            ServerLevel level,
            BuildingSavedData.BuildingRecord church
    ) {
        if (popeVisitorHoldTicks > 0 && popeVisitorId != null) {
            var entity = level.getEntity(popeVisitorId);
            if (entity instanceof CyberNpcEntity visitor
                    && visitor.isAlive()
                    && church.contains(visitor.blockPosition())
                    && npc.distanceToSqr(visitor)
                    <= POPE_VISITOR_RADIUS * POPE_VISITOR_RADIUS) {
                popeVisitorHoldTicks--;
                npc.getNavigation().stop();
                npc.setSprinting(false);
                npc.getLookControl().setLookAt(
                        visitor,
                        30.0F,
                        30.0F
                );
                visitor.getLookControl().setLookAt(
                        npc,
                        30.0F,
                        30.0F
                );
                return true;
            }

            popeVisitorHoldTicks = 0;
            popeVisitorId = null;
        }

        if (popeVisitorScanCooldown > 0) {
            popeVisitorScanCooldown--;
            return false;
        }
        popeVisitorScanCooldown = POPE_VISITOR_SCAN_INTERVAL;

        CyberNpcEntity visitor = level.getEntitiesOfClass(
                        CyberNpcEntity.class,
                        npc.getBoundingBox().inflate(
                                POPE_VISITOR_RADIUS,
                                4.0D,
                                POPE_VISITOR_RADIUS
                        ),
                        other -> other != npc
                                && other.isAlive()
                                && church.contains(
                                other.blockPosition()
                        )
                )
                .stream()
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);

        if (visitor == null) {
            return false;
        }

        popeVisitorId = visitor.getUUID();
        popeVisitorHoldTicks = POPE_VISITOR_HOLD_TICKS;
        popeTarget = null;
        npc.getNavigation().stop();
        npc.setSprinting(false);
        npc.getLookControl().setLookAt(visitor, 30.0F, 30.0F);
        visitor.getLookControl().setLookAt(npc, 30.0F, 30.0F);
        npc.swing(InteractionHand.MAIN_HAND, true);
        return true;
    }

    @Nullable
    private BlockPos choosePopeRoamTarget(
            ServerLevel level,
            BuildingSavedData buildings,
            BuildingSavedData.BuildingRecord church
    ) {
        List<BuildingSavedData.ActivityPoint> socialPoints =
                church.points().stream()
                        .filter(point ->
                                point.type() == BuildingPointType.SOCIAL
                                        || point.type()
                                        == BuildingPointType.SEATING
                        )
                        .toList();

        if (!socialPoints.isEmpty()
                && npc.getRandom().nextFloat() < 0.35F) {
            return socialPoints.get(
                    npc.getRandom().nextInt(socialPoints.size())
            ).pos();
        }

        return buildings.randomInteriorTarget(
                level,
                church,
                npc.getRandom()
        );
    }

    private void wakePope(ServerLevel level) {
        if (npc.isSleeping()) {
            npc.stopSleeping();
        }

        if (popeActiveBed != null) {
            setPopeBedOccupied(level, popeActiveBed, false);
            popeActiveBed = null;
        }
    }

    private void faceYaw(float yaw) {
        npc.setYRot(yaw);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
    }

    private static boolean isPopeBedOccupied(
            ServerLevel level,
            BlockPos head
    ) {
        BlockState state = level.getBlockState(head);
        return state.getBlock() instanceof BedBlock
                && state.hasProperty(BedBlock.OCCUPIED)
                && state.getValue(BedBlock.OCCUPIED);
    }

    private static void setPopeBedOccupied(
            ServerLevel level,
            BlockPos head,
            boolean occupied
    ) {
        BlockPos normalized = normalizePopeBedHead(level, head);
        if (normalized == null) {
            return;
        }

        BlockState headState = level.getBlockState(normalized);
        if (!(headState.getBlock() instanceof BedBlock)
                || !headState.hasProperty(BedBlock.OCCUPIED)
                || !headState.hasProperty(BedBlock.FACING)) {
            return;
        }

        if (headState.getValue(BedBlock.OCCUPIED) != occupied) {
            level.setBlock(
                    normalized,
                    headState.setValue(BedBlock.OCCUPIED, occupied),
                    Block.UPDATE_CLIENTS
            );
        }

        Direction facing = headState.getValue(BedBlock.FACING);
        BlockPos foot = normalized.relative(facing.getOpposite());
        BlockState footState = level.getBlockState(foot);

        if (footState.getBlock() instanceof BedBlock
                && footState.hasProperty(BedBlock.OCCUPIED)
                && footState.getValue(BedBlock.OCCUPIED)
                != occupied) {
            level.setBlock(
                    foot,
                    footState.setValue(BedBlock.OCCUPIED, occupied),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Nullable
    private static BlockPos normalizePopeBedHead(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)
                || !state.hasProperty(BedBlock.PART)
                || !state.hasProperty(BedBlock.FACING)) {
            return null;
        }

        if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return pos.immutable();
        }

        Direction facing = state.getValue(BedBlock.FACING);
        BlockPos head = pos.relative(facing);
        BlockState headState = level.getBlockState(head);

        if (headState.getBlock() instanceof BedBlock
                && headState.hasProperty(BedBlock.PART)
                && headState.getValue(BedBlock.PART)
                == BedPart.HEAD) {
            return head.immutable();
        }

        return null;
    }

    private void tickGuard(
            ServerLevel level,
            SpecialNpcSavedData data,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        clearVisualLetter();
        npc.ensureGuardEquipment();

        if (guardAttackCooldown > 0) {
            guardAttackCooldown--;
        }
        if (guardAlertTicks > 0) {
            guardAlertTicks--;
        }

        LivingEntity target = resolveLiving(level, guardTargetId);
        if (target == null || !target.isAlive()) {
            guardTargetId = null;
            guardCallingBackup = false;
            guardAlarmBellPos = null;
        }

        if (guardCallingBackup && guardTargetId != null) {
            target = resolveLiving(level, guardTargetId);

            if (target == null || !target.isAlive()) {
                guardCallingBackup = false;
                guardAlarmBellPos = null;
                guardTargetId = null;
            } else if (guardAlarmBellPos == null) {
                broadcastGuardAlert(
                        level,
                        npc.blockPosition(),
                        target
                );
                guardCallingBackup = false;
            } else if (npc.blockPosition().distSqr(guardAlarmBellPos)
                    <= BELL_ARRIVE_SQR) {
                npc.getNavigation().stop();
                npc.setSprinting(false);
                if (GuardAlarmSystem.ringBellAndAlert(
                        level,
                        guardAlarmBellPos,
                        npc,
                        target
                )) {
                    guardCallingBackup = false;
                    guardAlarmBellPos = null;
                } else {
                    guardAlarmBellPos = null;
                }
            } else {
                npc.setSprinting(true);
                navigateTo(guardAlarmBellPos, 1.10D);
                return;
            }
        }

        target = resolveLiving(level, guardTargetId);
        if (target != null && target.isAlive()) {
            if (tickGuardCombat(level, record, target)) {
                return;
            }
            guardTargetId = null;
        }

        if (guardTargetId == null
                && guardRespondBellPos != null) {
            if (npc.blockPosition().distSqr(guardRespondBellPos)
                    > BELL_ARRIVE_SQR) {
                releaseGuardPost(level);
                npc.setSprinting(true);
                navigateTo(guardRespondBellPos, 1.08D);
                return;
            }

            npc.getNavigation().stop();
            npc.setSprinting(false);

            if (guardBellHoldTicks > 0) {
                guardBellHoldTicks--;

                if (guardScanCooldown > 0) {
                    guardScanCooldown--;
                } else {
                    guardScanCooldown = 10;
                    LivingEntity nearbyThreat =
                            findGuardThreat(level, record);
                    if (nearbyThreat != null) {
                        guardTargetId =
                                nearbyThreat.getUUID();
                        guardRespondBellPos = null;
                        guardBellHoldTicks = 0;
                        return;
                    }
                }

                return;
            }

            guardRespondBellPos = null;
        }

        if (guardScanCooldown > 0) {
            guardScanCooldown--;
        } else {
            guardScanCooldown = GUARD_SCAN_INTERVAL;
            LivingEntity found = findGuardThreat(level, record);

            if (found != null) {
                guardTargetId = found.getUUID();
                guardPatrolTarget = null;
                guardPatrolPauseTicks = 0;

                if (shouldCallBackup(level, record, found)) {
                    guardCallingBackup = true;
                    guardAlarmBellPos = findNearestBell(level);
                }

                return;
            }
        }

        if (tickGuardPostDuty(level, record)) {
            return;
        }

        tickGuardPatrol(level, data, record);
    }

    private boolean tickGuardCombat(
            ServerLevel level,
            SpecialNpcSavedData.SpecialNpcRecord record,
            LivingEntity target
    ) {
        SpecialNpcSavedData.Anchor work = record.work();

        if (work != null
                && work.dimension().equals(level.dimension())
                && target.blockPosition().distSqr(work.pos())
                > GUARD_TOWN_RADIUS_SQR) {
            npc.getNavigation().stop();
            npc.setSprinting(false);
            return false;
        }

        double distanceSqr = npc.distanceToSqr(target);
        npc.getLookControl().setLookAt(target, 45.0F, 45.0F);

        if (distanceSqr > GUARD_ATTACK_DISTANCE_SQR) {
            boolean moving = npc.getNavigation().moveTo(
                    target,
                    1.08D
            );
            npc.setSprinting(
                    moving && distanceSqr > 36.0D
            );
            return true;
        }

        npc.getNavigation().stop();
        npc.setSprinting(false);

        if (guardAttackCooldown <= 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            npc.doHurtTarget(target);
            guardAttackCooldown = GUARD_ATTACK_COOLDOWN;
        }

        return true;
    }

    @Nullable
    private LivingEntity findGuardThreat(
            ServerLevel level,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        LivingEntity attacker = npc.getLastHurtByMob();
        if (isAlarmThreat(attacker)
                && isInsideGuardTown(level, record, attacker.blockPosition())) {
            return attacker;
        }

        return level.getEntitiesOfClass(
                        Mob.class,
                        npc.getBoundingBox().inflate(
                                GUARD_SCAN_RADIUS,
                                8.0D,
                                GUARD_SCAN_RADIUS
                        ),
                        mob -> mob != npc
                                && mob.isAlive()
                                && mob instanceof Enemy
                                && isInsideGuardTown(
                                level,
                                record,
                                mob.blockPosition()
                        )
                )
                .stream()
                .min(Comparator.comparingDouble(npc::distanceToSqr))
                .orElse(null);
    }

    private boolean shouldCallBackup(
            ServerLevel level,
            SpecialNpcSavedData.SpecialNpcRecord record,
            LivingEntity threat
    ) {
        if (threat instanceof Player) {
            return true;
        }

        int nearbyThreats = level.getEntitiesOfClass(
                Mob.class,
                threat.getBoundingBox().inflate(10.0D, 5.0D, 10.0D),
                mob -> mob.isAlive()
                        && mob instanceof Enemy
                        && isInsideGuardTown(
                        level,
                        record,
                        mob.blockPosition()
                )
        ).size();

        if (nearbyThreats > 1) {
            return true;
        }

        return !(threat instanceof Zombie
                || threat instanceof AbstractSkeleton
                || threat instanceof Spider);
    }

    private boolean isInsideGuardTown(
            ServerLevel level,
            SpecialNpcSavedData.SpecialNpcRecord record,
            BlockPos pos
    ) {
        SpecialNpcSavedData.Anchor work = record.work();

        return work == null
                || !work.dimension().equals(level.dimension())
                || pos.distSqr(work.pos()) <= GUARD_TOWN_RADIUS_SQR;
    }

    private boolean tickGuardPostDuty(
            ServerLevel level,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        SpecialNpcSavedData.Anchor work = record.work();
        if (work == null
                || !work.dimension().equals(level.dimension())) {
            releaseGuardPost(level);
            return false;
        }

        GuardPostSavedData posts = GuardPostSavedData.get(level);
        UUID guardId = npc.getSpecialNpcId() == null
                ? npc.getUUID()
                : npc.getSpecialNpcId();

        if (guardPostTarget != null
                && (!posts.exists(guardPostTarget)
                || !guardPostTarget.dimension().equals(level.dimension())
                || guardPostTarget.pos().distSqr(work.pos())
                > GUARD_TOWN_RADIUS_SQR)) {
            releaseGuardPost(level);
        }

        if (guardPostRestTicks > 0) {
            guardPostRestTicks--;
            return false;
        }

        if (guardPostTarget == null) {
            guardPostTarget = posts.claimNearest(
                    level.dimension(),
                    work.pos(),
                    guardId,
                    GUARD_TOWN_RADIUS_SQR,
                    level.getGameTime(),
                    level.getGameTime() + 6_000L
            );

            if (guardPostTarget == null) {
                return false;
            }

            guardPostDutyTicks = 0;
            guardPatrolTarget = null;
            guardPatrolPauseTicks = 0;
        }

        Vec3 postCenter = Vec3.atBottomCenterOf(
                guardPostTarget.pos()
        );

        if (npc.distanceToSqr(postCenter) > GUARD_POST_SNAP_SQR) {
            navigateTo(guardPostTarget.pos(), 0.86D);
            return true;
        }

        // A guard post is an exact standing marker, not a loose patrol target.
        // Once pathfinding gets the guard close enough, center their feet on
        // the marker and keep them locked there for the duty interval.
        npc.getNavigation().stop();
        npc.setSprinting(false);
        npc.setDeltaMovement(Vec3.ZERO);
        npc.moveTo(
                postCenter.x,
                postCenter.y,
                postCenter.z,
                guardPostTarget.yaw(),
                npc.getXRot()
        );
        npc.setYRot(guardPostTarget.yaw());
        npc.setYHeadRot(guardPostTarget.yaw());
        npc.setYBodyRot(guardPostTarget.yaw());

        if (guardPostDutyTicks <= 0) {
            guardPostDutyTicks = GUARD_POST_DUTY_MIN_TICKS
                    + npc.getRandom().nextInt(
                    GUARD_POST_DUTY_EXTRA_TICKS + 1
            );
        }

        guardPostDutyTicks--;
        if (guardPostDutyTicks <= 0) {
            releaseGuardPost(level);
            guardPostRestTicks = GUARD_POST_REST_MIN_TICKS
                    + npc.getRandom().nextInt(
                    GUARD_POST_REST_EXTRA_TICKS + 1
            );
            return false;
        }

        return true;
    }

    private void releaseGuardPost(ServerLevel level) {
        UUID guardId = npc.getSpecialNpcId() == null
                ? npc.getUUID()
                : npc.getSpecialNpcId();

        GuardPostSavedData.get(level).releaseGuard(guardId);
        guardPostTarget = null;
        guardPostDutyTicks = 0;
    }

    private void tickGuardPatrol(
            ServerLevel level,
            SpecialNpcSavedData data,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        if (record.returningToWork() && record.work() != null) {
            if (moveToAnchor(level, record.work(), 0.86D)) {
                data.markReachedWork(record.specialId());
            }
            return;
        }

        SpecialNpcSavedData.Anchor work = record.work();
        if (work == null
                || !work.dimension().equals(level.dimension())) {
            npc.getNavigation().stop();
            return;
        }

        if (guardPatrolPauseTicks > 0) {
            guardPatrolPauseTicks--;
            npc.getNavigation().stop();
            npc.setSprinting(false);
            return;
        }

        if (guardPatrolTarget != null
                && npc.blockPosition().distSqr(guardPatrolTarget)
                <= GUARD_PATROL_REACHED_SQR) {
            guardPatrolTarget = null;
            guardPatrolPauseTicks =
                    60 + npc.getRandom().nextInt(81);
            npc.getNavigation().stop();
            return;
        }

        if (guardPatrolTarget == null) {
            guardPatrolTarget = chooseGuardPatrolTarget(
                    level,
                    data,
                    work
            );
        }

        if (guardPatrolTarget == null) {
            moveToAnchor(level, work, 0.72D);
            return;
        }

        navigateTo(guardPatrolTarget, 0.82D);
    }

    @Nullable
    private BlockPos chooseGuardPatrolTarget(
            ServerLevel level,
            SpecialNpcSavedData data,
            SpecialNpcSavedData.Anchor work
    ) {
        for (int attempt = 0; attempt < 14; attempt++) {
            double angle = npc.getRandom().nextDouble()
                    * Math.PI * 2.0D;
            double radius = GUARD_PATROL_MIN_RADIUS
                    + npc.getRandom().nextInt(
                    GUARD_PATROL_EXTRA_RADIUS + 1
            );

            int x = work.pos().getX()
                    + (int) Math.round(Math.cos(angle) * radius);
            int z = work.pos().getZ()
                    + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    x,
                    z
            );

            BlockPos candidate = new BlockPos(x, y, z);

            if (isGuardPatrolCandidate(
                    level,
                    data,
                    candidate
            )) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isGuardPatrolCandidate(
            ServerLevel level,
            SpecialNpcSavedData data,
            BlockPos pos
    ) {
        if (!level.canSeeSky(pos.above())
                || !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.above()).isEmpty()
                || !level.getBlockState(pos)
                .getCollisionShape(level, pos)
                .isEmpty()
                || !level.getBlockState(pos.above())
                .getCollisionShape(level, pos.above())
                .isEmpty()
                || level.getBlockState(pos.below())
                .getCollisionShape(level, pos.below())
                .isEmpty()) {
            return false;
        }

        for (SpecialNpcSavedData.SpecialNpcRecord other
                : data.records()) {
            SpecialNpcSavedData.Anchor home = other.home();
            if (home != null
                    && home.dimension().equals(level.dimension())
                    && pos.distSqr(home.pos()) <= 49.0D) {
                return false;
            }
        }

        BlockPos min = pos.offset(-5, -2, -5);
        BlockPos max = pos.offset(5, 2, 5);

        for (BlockPos nearby : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(nearby).is(BlockTags.BEDS)) {
                return false;
            }
        }

        return true;
    }

    void receiveGuardAlert(
            @Nullable LivingEntity threat,
            BlockPos bellPos
    ) {
        if (NpcServiceRole.fromRole(npc.getRole())
                != NpcServiceRole.GUARD) {
            return;
        }

        if (threat != null && threat.isAlive()) {
            guardTargetId = threat.getUUID();
        }

        guardRespondBellPos = bellPos.immutable();
        guardBellHoldTicks = 120;
        guardAlertTicks = GUARD_ALERT_TICKS;
        guardCallingBackup = false;
        guardAlarmBellPos = null;
        guardPatrolTarget = null;
        guardPatrolPauseTicks = 0;

        if (npc.level() instanceof ServerLevel serverLevel) {
            releaseGuardPost(serverLevel);
        }

        npc.getNavigation().stop();
    }

    @Nullable
    private LivingEntity resolveLiving(
            ServerLevel level,
            @Nullable UUID id
    ) {
        if (id == null) {
            return null;
        }

        var entity = level.getEntity(id);
        return entity instanceof LivingEntity living
                ? living
                : null;
    }

    private void tickBanker(
            ServerLevel level,
            SpecialNpcSavedData data,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        clearVisualLetter();

        SpecialNpcSavedData.Anchor desired =
                record.returningToWork()
                        ? record.work()
                        : (level.isNight()
                        ? firstNonNull(record.home(), record.work())
                        : firstNonNull(record.work(), record.home()));

        if (desired == null) {
            npc.getNavigation().stop();
            return;
        }

        if (moveToAnchor(level, desired, 0.72D)) {
            if (record.returningToWork()
                    && desired == record.work()) {
                data.markReachedWork(record.specialId());
            }
        }
    }

    private void tickCourier(
            ServerLevel level,
            SpecialNpcSavedData data,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        cleanSatchel(level);

        if (!satchelIds.isEmpty()) {
            tickCourierRoute(level, data, record);
            return;
        }

        clearVisualLetter();
        pickedUp = false;
        pickupPos = null;

        if (record.returningToWork()) {
            if (record.work() != null
                    && moveToAnchor(level, record.work(), 0.82D)) {
                data.markReachedWork(record.specialId());
            }
            return;
        }

        if (level.isNight()) {
            SpecialNpcSavedData.Anchor home =
                    firstNonNull(record.home(), record.work());

            if (home != null) {
                moveToAnchor(level, home, 0.76D);
            }
            return;
        }

        SpecialNpcSavedData.Anchor work =
                firstNonNull(record.work(), record.home());

        if (work != null && !isAtAnchor(level, work)) {
            moveToAnchor(level, work, 0.82D);
            return;
        }

        if (searchCooldown > 0) {
            searchCooldown--;
            faceAnchor(work);
            return;
        }
        searchCooldown = SEARCH_INTERVAL;

        MailSavedData mail = MailSavedData.get(level);
        MailRecord claimed = mail.claimNearestPending(
                level.dimension(),
                npc.blockPosition(),
                npc.getUUID(),
                SEARCH_RADIUS_SQR
        );

        if (claimed == null) {
            faceAnchor(work);
            return;
        }

        BlockPos pickup = claimed.pickupPos();
        if (pickup == null
                || !mail.isDropBoxRegistered(
                level.dimension(),
                pickup
        )) {
            mail.returnToPending(claimed.id(), npc.getUUID());
            return;
        }

        satchelIds.add(claimed.id());
        pickupPos = pickup.immutable();
        pickedUp = false;
        repathCooldown = 0;
    }

    private void tickCourierRoute(
            ServerLevel level,
            SpecialNpcSavedData data,
            SpecialNpcSavedData.SpecialNpcRecord record
    ) {
        MailSavedData mail = MailSavedData.get(level);

        if (!pickedUp) {
            if (pickupPos == null
                    || !mail.isDropBoxRegistered(
                    level.dimension(),
                    pickupPos
            )) {
                releaseSatchel(level);
                return;
            }

            if (npc.blockPosition().distSqr(pickupPos) <= ARRIVE_SQR) {
                List<MailRecord> extras = mail.claimPendingAt(
                        level.dimension(),
                        pickupPos,
                        npc.getUUID(),
                        Math.max(
                                0,
                                SATCHEL_CAPACITY - satchelIds.size()
                        )
                );

                for (MailRecord extra : extras) {
                    if (!satchelIds.contains(extra.id())) {
                        satchelIds.add(extra.id());
                    }
                }

                pickedUp = true;
                showVisualLetter();
                npc.getNavigation().stop();
                npc.swing(InteractionHand.MAIN_HAND);
                level.playSound(
                        null,
                        npc.blockPosition(),
                        SoundEvents.BARREL_OPEN,
                        SoundSource.NEUTRAL,
                        0.45F,
                        1.15F
                );
                return;
            }

            navigateTo(pickupPos, 0.80D);
            return;
        }

        showVisualLetter();

        DeliveryTarget target = findNearestDelivery(level, mail);
        if (target != null) {
            if (target.player != null) {
                if (npc.distanceToSqr(target.player) <= ARRIVE_SQR) {
                    deliverToPlayer(level, mail, target);
                } else {
                    navigateTo(target.player.blockPosition(), 0.82D);
                }
                return;
            }

            if (target.boxPos != null) {
                if (npc.blockPosition().distSqr(target.boxPos) <= ARRIVE_SQR) {
                    deliverToBox(level, mail, target);
                } else {
                    navigateTo(target.boxPos, 0.80D);
                }
                return;
            }
        }

        SpecialNpcSavedData.Anchor work =
                firstNonNull(record.work(), record.home());

        if (work == null) {
            npc.getNavigation().stop();
            return;
        }

        if (moveToAnchor(level, work, 0.76D)) {
            // Keep unavailable mail safe, but release it back to the queue once
            // the Courier reaches the Post Office so another route can try later.
            releaseSatchel(level);
            data.markReachedWork(record.specialId());
        }
    }

    @Nullable
    private DeliveryTarget findNearestDelivery(
            ServerLevel level,
            MailSavedData mail
    ) {
        DeliveryTarget best = null;
        double bestDistance = Double.MAX_VALUE;

        for (long id : satchelIds) {
            MailRecord record = mail.get(id);
            if (!isOwnedTransit(record)) {
                continue;
            }

            MailSavedData.PostalAddress box =
                    mail.getLetterBox(record.recipientId());

            if (box != null
                    && box.dimension().equals(level.dimension())
                    && mail.boxedCount(record.recipientId())
                    < LETTER_BOX_CAPACITY
                    && level.hasChunkAt(box.pos())) {
                double distance = npc.blockPosition().distSqr(box.pos());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = DeliveryTarget.forBox(id, box.pos());
                }
                continue;
            }

            ServerPlayer player = level.getServer()
                    .getPlayerList()
                    .getPlayer(record.recipientId());

            if (player != null
                    && player.level().dimension().equals(level.dimension())) {
                double distance = npc.distanceToSqr(player);
                if (distance <= DIRECT_PLAYER_DELIVERY_RADIUS_SQR
                        && distance < bestDistance) {
                    bestDistance = distance;
                    best = DeliveryTarget.forPlayer(id, player);
                }
            }
        }

        return best;
    }

    private void deliverToBox(
            ServerLevel level,
            MailSavedData mail,
            DeliveryTarget target
    ) {
        MailRecord record = mail.get(target.mailId);
        if (!isOwnedTransit(record) || target.boxPos == null) {
            satchelIds.remove(target.mailId);
            return;
        }

        mail.markBoxed(record.id());
        satchelIds.remove(record.id());
        npc.swing(InteractionHand.MAIN_HAND);
        level.playSound(
                null,
                target.boxPos,
                SoundEvents.BARREL_CLOSE,
                SoundSource.NEUTRAL,
                0.50F,
                1.25F
        );

        if (satchelIds.isEmpty()) {
            clearVisualLetter();
        }
    }

    private void deliverToPlayer(
            ServerLevel level,
            MailSavedData mail,
            DeliveryTarget target
    ) {
        MailRecord record = mail.get(target.mailId);
        ServerPlayer player = target.player;

        if (!isOwnedTransit(record) || player == null) {
            satchelIds.remove(target.mailId);
            return;
        }

        ItemStack sealed = new ItemStack(ModItems.SEALED_LETTER.get());
        MailItemData.writeRecordId(sealed, record.id());

        if (!player.getInventory().add(sealed)) {
            player.drop(sealed, false);
        }

        mail.markDelivered(record.id());
        satchelIds.remove(record.id());
        npc.swing(InteractionHand.MAIN_HAND);
        level.playSound(
                null,
                npc.blockPosition(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.NEUTRAL,
                0.45F,
                1.05F
        );

        if (satchelIds.isEmpty()) {
            clearVisualLetter();
        }
    }

    private void cleanSatchel(ServerLevel level) {
        MailSavedData mail = MailSavedData.get(level);
        Iterator<Long> iterator = satchelIds.iterator();

        while (iterator.hasNext()) {
            MailRecord record = mail.get(iterator.next());
            if (!isOwnedTransit(record)) {
                iterator.remove();
            }
        }

        if (satchelIds.isEmpty()) {
            pickedUp = false;
            pickupPos = null;
            clearVisualLetter();
        }
    }

    private boolean isOwnedTransit(@Nullable MailRecord record) {
        return record != null
                && record.state() == MailState.IN_TRANSIT
                && npc.getUUID().equals(record.courierId());
    }

    private void tickBreathingEasterEgg(ServerLevel level) {
        if (breathRearmCooldown > 0) {
            breathRearmCooldown--;
        }

        if (breathCheckCooldown > 0) {
            breathCheckCooldown--;
            return;
        }

        breathCheckCooldown = BREATH_CHECK_INTERVAL;

        Player nearby = level.getNearestPlayer(
                npc,
                BREATH_RESET_DISTANCE
        );

        if (nearby == null) {
            breathPlayerClose = false;
            return;
        }

        double distanceSqr = npc.distanceToSqr(nearby);

        if (distanceSqr <= BREATH_TRIGGER_DISTANCE
                * BREATH_TRIGGER_DISTANCE) {
            if (!breathPlayerClose && breathRearmCooldown <= 0) {
                level.playSound(
                        null,
                        npc.getX(),
                        npc.getY() + 1.1D,
                        npc.getZ(),
                        ModSounds.COURIER_BREATHING.get(),
                        SoundSource.NEUTRAL,
                        BREATH_VOLUME,
                        1.0F
                );

                breathRearmCooldown =
                        BREATH_REARM_COOLDOWN_TICKS;
            }

            breathPlayerClose = true;
        }
    }

    private boolean moveToAnchor(
            ServerLevel level,
            SpecialNpcSavedData.Anchor anchor,
            double speed
    ) {
        if (!anchor.dimension().equals(level.dimension())) {
            npc.getNavigation().stop();
            return false;
        }

        if (isAtAnchor(level, anchor)) {
            npc.getNavigation().stop();
            npc.setSprinting(false);
            npc.setYRot(anchor.yaw());
            npc.setYHeadRot(anchor.yaw());
            npc.setYBodyRot(anchor.yaw());
            return true;
        }

        navigateTo(anchor.pos(), speed);
        return false;
    }

    private boolean isAtAnchor(
            ServerLevel level,
            SpecialNpcSavedData.Anchor anchor
    ) {
        return anchor.dimension().equals(level.dimension())
                && npc.blockPosition().distSqr(anchor.pos())
                <= ANCHOR_ARRIVE_SQR;
    }

    private void faceAnchor(@Nullable SpecialNpcSavedData.Anchor anchor) {
        if (anchor == null) {
            return;
        }

        npc.setYRot(anchor.yaw());
        npc.setYHeadRot(anchor.yaw());
        npc.setYBodyRot(anchor.yaw());
    }

    private void navigateTo(BlockPos pos, double speed) {
        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }

        repathCooldown = REPATH_INTERVAL;

        npc.getNavigation().moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                speed
        );
    }

    void release() {
        if (npc.level() instanceof ServerLevel level) {
            MailSavedData.get(level).releaseCourier(npc.getUUID());
            releaseGuardPost(level);
            wakePope(level);
        }

        satchelIds.clear();
        pickupPos = null;
        pickedUp = false;
        repathCooldown = 0;
        clearPanic();
        guardTargetId = null;
        guardAlarmBellPos = null;
        guardCallingBackup = false;
        guardPatrolTarget = null;
        guardPatrolPauseTicks = 0;
        guardPostTarget = null;
        guardPostDutyTicks = 0;
        guardPostRestTicks = 0;
        guardRespondBellPos = null;
        guardBellHoldTicks = 0;
        popeTarget = null;
        popeActiveBed = null;
        popeVisitorId = null;
        popePauseTicks = 0;
        popeVisitorScanCooldown = 0;
        popeVisitorHoldTicks = 0;
        popeLastAltarDay = Long.MIN_VALUE;
        clearVisualLetter();
    }

    String status() {
        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());

        if (role == NpcServiceRole.BANKER) {
            return panicThreatId != null
                    ? "calling for guards"
                    : "serving the bank";
        }

        if (role == NpcServiceRole.GUARD) {
            if (guardCallingBackup) {
                return "ringing the alarm bell";
            }
            if (guardTargetId != null) {
                return "responding to danger";
            }
            if (guardRespondBellPos != null) {
                return "answering the town bell";
            }
            if (guardPostTarget != null) {
                return "standing guard";
            }
            return "patrolling the town";
        }

        if (role == NpcServiceRole.POPE) {
            if (npc.isSleeping()) {
                return "sleeping in the church";
            }
            if (popeVisitorHoldTicks > 0) {
                return "speaking with a church visitor";
            }
            if (popeTarget != null) {
                return "moving through the church";
            }
            return "serving the church";
        }

        if (role != NpcServiceRole.COURIER) {
            return "inactive";
        }

        if (panicThreatId != null) {
            return "calling for guards";
        }

        if (!satchelIds.isEmpty()) {
            return pickedUp
                    ? "delivering " + satchelIds.size()
                    + " letter" + (satchelIds.size() == 1 ? "" : "s")
                    : "collecting mail";
        }

        return "waiting at the post office";
    }

    void addSaveData(CompoundTag tag) {
        // Home/work and current task ownership live in authoritative world
        // SavedData, not inside the entity. This keeps death/respawn safe.
    }

    void readSaveData(CompoundTag tag) {
        satchelIds.clear();
        pickupPos = null;
        pickedUp = false;
        searchCooldown = Math.floorMod(
                npc.getUUID().hashCode(),
                SEARCH_INTERVAL
        );
        repathCooldown = 0;
        breathCheckCooldown = Math.floorMod(
                npc.getUUID().hashCode(),
                BREATH_CHECK_INTERVAL
        );
        breathRearmCooldown = 0;
        breathPlayerClose = false;
        threatScanCooldown = Math.floorMod(
                npc.getUUID().hashCode() * 7,
                THREAT_SCAN_INTERVAL
        );
        bellAlertCooldown = 0;
        clearPanic();
        guardScanCooldown = Math.floorMod(
                npc.getUUID().hashCode() * 11,
                GUARD_SCAN_INTERVAL
        );
        guardAttackCooldown = 0;
        guardAlertTicks = 0;
        guardTargetId = null;
        guardAlarmBellPos = null;
        guardCallingBackup = false;
        guardPatrolTarget = null;
        guardPatrolPauseTicks = 0;
        guardPostTarget = null;
        guardPostDutyTicks = 0;
        guardPostRestTicks = 0;
        guardRespondBellPos = null;
        guardBellHoldTicks = 0;
        popeTarget = null;
        popeActiveBed = null;
        popeVisitorId = null;
        popePauseTicks = 0;
        popeVisitorScanCooldown = 0;
        popeVisitorHoldTicks = 0;
        popeLastAltarDay = Long.MIN_VALUE;
        if (npc.isSleeping()) {
            npc.stopSleeping();
        }
        clearVisualLetter();
    }

    private void releaseSatchel(ServerLevel level) {
        MailSavedData.get(level).releaseCourier(npc.getUUID());
        satchelIds.clear();
        pickupPos = null;
        pickedUp = false;
        clearVisualLetter();
    }

    private void showVisualLetter() {
        if (satchelIds.isEmpty()) {
            clearVisualLetter();
            return;
        }

        ItemStack visual = new ItemStack(ModItems.SEALED_LETTER.get());
        npc.setItemSlot(EquipmentSlot.MAINHAND, visual);
    }

    private void clearVisualLetter() {
        ItemStack held = npc.getMainHandItem();
        if (held.is(ModItems.SEALED_LETTER.get())
                && MailItemData.getRecordId(held) < 0L) {
            npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Nullable
    private static SpecialNpcSavedData.Anchor firstNonNull(
            @Nullable SpecialNpcSavedData.Anchor first,
            @Nullable SpecialNpcSavedData.Anchor second
    ) {
        return first != null ? first : second;
    }

    private static final class DeliveryTarget {
        private final long mailId;
        @Nullable
        private final BlockPos boxPos;
        @Nullable
        private final ServerPlayer player;

        private DeliveryTarget(
                long mailId,
                @Nullable BlockPos boxPos,
                @Nullable ServerPlayer player
        ) {
            this.mailId = mailId;
            this.boxPos = boxPos;
            this.player = player;
        }

        private static DeliveryTarget forBox(
                long mailId,
                BlockPos pos
        ) {
            return new DeliveryTarget(
                    mailId,
                    pos.immutable(),
                    null
            );
        }

        private static DeliveryTarget forPlayer(
                long mailId,
                ServerPlayer player
        ) {
            return new DeliveryTarget(mailId, null, player);
        }
    }
}
