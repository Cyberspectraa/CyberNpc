package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.mail.MailItemData;
import com.cyberspectraa.cybernpc.mail.MailRecord;
import com.cyberspectraa.cybernpc.mail.MailSavedData;
import com.cyberspectraa.cybernpc.mail.MailState;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.cyberspectraa.cybernpc.registry.ModSounds;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

final class NpcServiceBrain {
    private static final int SEARCH_INTERVAL = 40;
    private static final int REPATH_INTERVAL = 10;
    private static final double SEARCH_RADIUS_SQR = 96.0D * 96.0D;
    private static final double ARRIVE_SQR = 3.0D * 3.0D;
    private static final double HOME_ARRIVE_SQR = 4.0D * 4.0D;
    private static final int LETTER_BOX_CAPACITY = 9;

    // Craig-the-mailman style proximity easter egg. The sound is intentionally
    // subtle, only checks every half second, and requires the player to leave
    // the outer radius before it can arm again.
    private static final int BREATH_CHECK_INTERVAL = 10;
    private static final int BREATH_REARM_COOLDOWN_TICKS = 160;
    private static final double BREATH_TRIGGER_DISTANCE = 2.6D;
    private static final double BREATH_RESET_DISTANCE = 3.6D;
    private static final float BREATH_VOLUME = 0.42F;

    private final CyberNpcEntity npc;

    @Nullable
    private BlockPos homePos;

    private long activeMailId = -1L;
    private boolean pickedUp;
    private boolean returningHome;
    private int searchCooldown;
    private int repathCooldown;
    private int breathCheckCooldown;
    private int breathRearmCooldown;
    private boolean breathPlayerClose;

    NpcServiceBrain(CyberNpcEntity npc) {
        this.npc = npc;
    }

    void tick() {
        if (!(npc.level() instanceof ServerLevel level)
                || npc.getNpcType() == NpcType.WILD) {
            return;
        }

        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        if (role != NpcServiceRole.COURIER) {
            clearVisualLetter();
            breathPlayerClose = false;
            breathCheckCooldown = 0;
            breathRearmCooldown = 0;
            return;
        }

        tickBreathingEasterEgg(level);

        if (homePos == null) {
            homePos = npc.blockPosition().immutable();
        }

        if (activeMailId >= 0L) {
            tickActiveMail(level);
            return;
        }

        if (returningHome) {
            if (homePos == null
                    || npc.blockPosition().distSqr(homePos) <= HOME_ARRIVE_SQR) {
                returningHome = false;
                npc.getNavigation().stop();
                return;
            }

            navigateTo(homePos, 0.82D);
            return;
        }

        if (searchCooldown > 0) {
            searchCooldown--;
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
            if (homePos != null
                    && npc.blockPosition().distSqr(homePos) > HOME_ARRIVE_SQR) {
                returningHome = true;
            }
            return;
        }

        BlockPos pickup = claimed.pickupPos();
        if (pickup == null || !level.hasChunkAt(pickup)) {
            mail.returnToPending(claimed.id(), npc.getUUID());
            searchCooldown = SEARCH_INTERVAL * 2;
            return;
        }

        activeMailId = claimed.id();
        pickedUp = false;
        returningHome = false;
        repathCooldown = 0;
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

    private void tickActiveMail(ServerLevel level) {
        MailSavedData mail = MailSavedData.get(level);
        MailRecord record = mail.get(activeMailId);

        if (record == null
                || record.state() != MailState.IN_TRANSIT
                || record.courierId() == null
                || !record.courierId().equals(npc.getUUID())) {
            clearActiveMail(false);
            return;
        }

        if (!pickedUp) {
            BlockPos pickup = record.pickupPos();
            if (pickup == null) {
                mail.returnToPending(activeMailId, npc.getUUID());
                clearActiveMail(false);
                return;
            }

            if (npc.blockPosition().distSqr(pickup) <= ARRIVE_SQR) {
                pickedUp = true;
                showVisualLetter();
                npc.getNavigation().stop();
                repathCooldown = 0;
                return;
            }

            navigateTo(pickup, 0.78D);
            return;
        }

        MailSavedData.PostalAddress box = mail.getLetterBox(
                record.recipientId()
        );

        if (box != null
                && box.dimension().equals(level.dimension())
                && mail.boxedCount(record.recipientId()) < LETTER_BOX_CAPACITY
                && level.hasChunkAt(box.pos())) {
            if (npc.blockPosition().distSqr(box.pos()) <= ARRIVE_SQR) {
                mail.markBoxed(record.id());
                clearActiveMail(true);
                return;
            }

            navigateTo(box.pos(), 0.78D);
            return;
        }

        ServerPlayer recipient = level.getServer()
                .getPlayerList()
                .getPlayer(record.recipientId());

        if (recipient != null
                && recipient.level().dimension().equals(level.dimension())) {
            if (npc.distanceToSqr(recipient) <= ARRIVE_SQR) {
                ItemStack sealed = new ItemStack(
                        ModItems.SEALED_LETTER.get()
                );
                MailItemData.writeRecordId(sealed, record.id());

                if (!recipient.getInventory().add(sealed)) {
                    recipient.drop(sealed, false);
                }

                mail.markDelivered(record.id());
                clearActiveMail(true);
                return;
            }

            navigateTo(recipient.blockPosition(), 0.82D);
            return;
        }

        // No usable Letter Box and recipient is not currently available.
        // Keep the authoritative letter assigned to this courier and head home
        // rather than force-loading chunks or teleporting.
        if (homePos != null
                && npc.blockPosition().distSqr(homePos) > HOME_ARRIVE_SQR) {
            navigateTo(homePos, 0.76D);
        } else {
            npc.getNavigation().stop();
        }
    }

    private void navigateTo(BlockPos pos, double speed) {
        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }

        repathCooldown = REPATH_INTERVAL;

        boolean started = npc.getNavigation().moveTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                speed
        );

        if (!started && activeMailId < 0L) {
            returningHome = false;
        }
    }

    void release() {
        if (npc.level() instanceof ServerLevel level
                && activeMailId >= 0L) {
            MailSavedData.get(level).returnToPending(
                    activeMailId,
                    npc.getUUID()
            );
        }

        clearActiveMail(false);
        returningHome = false;
    }

    void setHome(BlockPos pos) {
        homePos = pos == null ? null : pos.immutable();
        returningHome = false;
    }

    @Nullable
    BlockPos getHome() {
        return homePos;
    }

    String status() {
        if (NpcServiceRole.fromRole(npc.getRole())
                != NpcServiceRole.COURIER) {
            return "inactive";
        }

        if (activeMailId >= 0L) {
            return pickedUp
                    ? "delivering letter #" + activeMailId
                    : "collecting letter #" + activeMailId;
        }

        if (returningHome) {
            return "returning to post office";
        }

        return "waiting for mail";
    }

    void addSaveData(CompoundTag tag) {
        if (homePos != null) {
            tag.putLong("CyberNpcServiceHome", homePos.asLong());
        }
    }

    void readSaveData(CompoundTag tag) {
        homePos = tag.contains("CyberNpcServiceHome")
                ? BlockPos.of(tag.getLong("CyberNpcServiceHome"))
                : null;

        // MailSavedData releases old in-transit assignments on world load, so
        // the courier also starts with a clean local task and searches again.
        activeMailId = -1L;
        pickedUp = false;
        returningHome = false;
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

    private void clearActiveMail(boolean returnHomeAfter) {
        activeMailId = -1L;
        pickedUp = false;
        repathCooldown = 0;
        clearVisualLetter();
        returningHome = returnHomeAfter;
    }

    private void showVisualLetter() {
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
}
