package com.cyberspectraa.cybernpc.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Compatibility shell for Building Planner items created by CyberNpc 0.39.x.
 *
 * The planner is intentionally retired in 0.40.0. Keeping the registry entry
 * prevents old worlds/inventories from receiving a missing-item problem while
 * making it impossible to accidentally configure a second competing building
 * system.
 */
public final class BuildingPlannerItem extends Item {
    public BuildingPlannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.displayClientMessage(
                    Component.literal(
                            "The Building Planner was retired in CyberNpc 0.40. "
                                    + "Use the new item-frame Building Markers instead."
                    ).withStyle(ChatFormatting.YELLOW),
                    false
            );
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.literal("Retired setup tool")
                        .withStyle(ChatFormatting.RED)
        );
        tooltip.add(
                Component.literal(
                        "Use CyberNpc Building Markers in item frames."
                ).withStyle(ChatFormatting.GRAY)
        );
    }
}
