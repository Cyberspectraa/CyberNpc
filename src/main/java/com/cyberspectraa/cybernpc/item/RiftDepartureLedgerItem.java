package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.rift.RiftLedgerService;
import com.cyberspectraa.cybernpc.rift.NpcRiftDeparture;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Developer ledger with deliberate sneak-use crossing authorisation; operator-only. */
public final class RiftDepartureLedgerItem extends Item {
    public RiftDepartureLedgerItem(Properties properties) { super(properties); }
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof CyberNpcEntity npc)) return InteractionResult.PASS;
        if (!player.level().isClientSide && player instanceof ServerPlayer admin) {
            if (!admin.hasPermissions(2)) {
                admin.displayClientMessage(Component.literal("Operator access required."), true);
                return InteractionResult.FAIL;
            }
            if (admin.isShiftKeyDown()) {
                admin.displayClientMessage(Component.literal(NpcRiftDeparture.toggleArm(npc)), false);
            } else {
                RiftLedgerService.open(admin);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.hasPermissions(2)) {
                serverPlayer.displayClientMessage(Component.literal("Operator access required."), true);
                return InteractionResultHolder.fail(stack);
            }
            RiftLedgerService.open(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
