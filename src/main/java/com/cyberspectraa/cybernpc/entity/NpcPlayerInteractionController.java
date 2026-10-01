package com.cyberspectraa.cybernpc.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

/**
 * Routes CyberNpc "use" actions through Forge's real Player-based interaction
 * hooks. This is deliberately a use-only layer: it exposes no block breaking
 * and generic block interaction is performed with an empty hand, so it cannot
 * place blocks in this phase of the project.
 */
final class NpcPlayerInteractionController {
    static final double DEFAULT_USE_DISTANCE_SQR = 25.0D;

    private final CyberNpcEntity npc;

    NpcPlayerInteractionController(CyberNpcEntity npc) {
        this.npc = npc;
    }

    boolean canUseBlock(BlockPos pos) {
        if (!(npc.level() instanceof ServerLevel level)
                || npc.distanceToSqr(Vec3.atCenterOf(pos))
                > DEFAULT_USE_DISTANCE_SQR) {
            return false;
        }

        Vec3 from = npc.getEyePosition();
        Vec3 to = Vec3.atCenterOf(pos);
        BlockHitResult trace = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                npc
        ));

        return trace.getType() == HitResult.Type.BLOCK
                && trace.getBlockPos().equals(pos);
    }

    InteractionResult rightClickBlock(BlockPos pos) {
        if (!(npc.level() instanceof ServerLevel level)
                || !canUseBlock(pos)) {
            return InteractionResult.FAIL;
        }

        FakePlayer player = preparePlayer(level);
        InteractionHand hand = InteractionHand.MAIN_HAND;

        // Empty-hand block use is intentional. It allows doors, containers,
        // workstations, buttons, levers, bells and modded block use hooks while
        // guaranteeing this controller cannot place a BlockItem.
        player.setItemInHand(hand, ItemStack.EMPTY);

        BlockState state = level.getBlockState(pos);
        Vec3 hitLocation = Vec3.atCenterOf(pos);
        Direction face = nearestFace(hitLocation.subtract(npc.getEyePosition()));
        BlockHitResult hit = new BlockHitResult(
                hitLocation,
                face,
                pos,
                false
        );

        InteractionResult result;
        try {
            result = state.use(level, player, hand, hit);
        } finally {
            cleanupPlayer(player);
        }

        if (result.consumesAction()) {
            npc.swing(hand, true);
        }

        return result;
    }

    InteractionResult rightClickEntity(Entity target) {
        return rightClickEntity(target, ItemStack.EMPTY).result();
    }

    EntityUseResult rightClickEntity(Entity target, ItemStack heldItem) {
        if (!(npc.level() instanceof ServerLevel level)
                || target == null
                || !target.isAlive()
                || target.level() != npc.level()
                || npc.distanceToSqr(target)
                > DEFAULT_USE_DISTANCE_SQR
                || !npc.getSensing().hasLineOfSight(target)) {
            return new EntityUseResult(
                    InteractionResult.FAIL,
                    heldItem.copy()
            );
        }

        FakePlayer player = preparePlayer(level);
        InteractionHand hand = InteractionHand.MAIN_HAND;
        player.setItemInHand(hand, heldItem.copy());

        InteractionResult result;
        ItemStack remaining;
        try {
            result = target.interact(player, hand);
            remaining = player.getItemInHand(hand).copy();
        } finally {
            cleanupPlayer(player);
        }

        if (result.consumesAction()) {
            npc.swing(hand, true);
        }

        return new EntityUseResult(result, remaining);
    }

    /**
     * Uses the same empty-hand right click an owning player would use to toggle
     * a wolf. Direct state assignment is retained only as a compatibility
     * fallback if another mod consumes/rejects the interaction without putting
     * the wolf into the requested state.
     */
    void setOwnedWolfSitting(net.minecraft.world.entity.animal.Wolf wolf, boolean shouldSit) {
        if (wolf == null
                || !wolf.isAlive()
                || !wolf.isTame()
                || !npc.getUUID().equals(wolf.getOwnerUUID())
                || wolf.isOrderedToSit() == shouldSit) {
            return;
        }

        rightClickEntity(wolf);

        if (wolf.isOrderedToSit() != shouldSit) {
            wolf.setOrderedToSit(shouldSit);
        }
    }

    private FakePlayer preparePlayer(ServerLevel level) {
        GameProfile profile = new GameProfile(
                npc.getUUID(),
                "CyberNpc"
        );
        FakePlayer player = FakePlayerFactory.get(level, profile);

        player.moveTo(
                npc.getX(),
                npc.getY(),
                npc.getZ(),
                npc.getYRot(),
                npc.getXRot()
        );
        player.setYHeadRot(npc.getYHeadRot());
        player.setShiftKeyDown(npc.isShiftKeyDown());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        return player;
    }

    private static void cleanupPlayer(FakePlayer player) {
        if (player.isPassenger()) {
            player.stopRiding();
        }

        player.closeContainer();
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    private static Direction nearestFace(Vec3 fromNpcToBlock) {
        return Direction.getNearest(
                (float) -fromNpcToBlock.x,
                (float) -fromNpcToBlock.y,
                (float) -fromNpcToBlock.z
        );
    }

    record EntityUseResult(
            InteractionResult result,
            ItemStack remaining
    ) {
    }
}
