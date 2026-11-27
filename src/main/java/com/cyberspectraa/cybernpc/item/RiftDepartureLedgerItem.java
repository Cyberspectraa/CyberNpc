package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.rift.RiftLedgerService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Read-only developer item; holding it does not grant operator access. */
public final class RiftDepartureLedgerItem extends Item {
    public RiftDepartureLedgerItem(Properties properties) { super(properties); }
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
