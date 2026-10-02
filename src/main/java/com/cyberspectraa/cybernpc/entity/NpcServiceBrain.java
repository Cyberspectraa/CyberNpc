package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.mail.MailItemData;
import com.cyberspectraa.cybernpc.mail.MailRecord;
import com.cyberspectraa.cybernpc.mail.MailSavedData;
import com.cyberspectraa.cybernpc.mail.MailState;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.cyberspectraa.cybernpc.registry.ModSounds;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.cyberspectraa.cybernpc.service.SpecialNpcSavedData;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
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
        }
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
        }

        satchelIds.clear();
        pickupPos = null;
        pickedUp = false;
        repathCooldown = 0;
        clearVisualLetter();
    }

    String status() {
        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());

        if (role == NpcServiceRole.BANKER) {
            return "serving the bank";
        }

        if (role != NpcServiceRole.COURIER) {
            return "inactive";
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
