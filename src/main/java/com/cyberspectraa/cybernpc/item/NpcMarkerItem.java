package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.block.NpcMarkerBlock;
import com.cyberspectraa.cybernpc.building.NpcMarkerKind;
import com.cyberspectraa.cybernpc.building.NpcMarkerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class NpcMarkerItem extends BlockItem {
    private final NpcMarkerKind kind;

    public NpcMarkerItem(
            NpcMarkerBlock block,
            NpcMarkerKind kind,
            Properties properties
    ) {
        super(block, properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();

        if (player != null && player.isShiftKeyDown()) {
            BlockPos clicked = context.getClickedPos();
            BlockPos[] candidates = new BlockPos[]{
                    clicked,
                    clicked.above(),
                    clicked.below(),
                    clicked.relative(context.getClickedFace())
            };

            for (BlockPos pos : candidates) {
                if (level.getBlockState(pos).getBlock()
                        instanceof NpcMarkerBlock) {
                    if (!level.isClientSide) {
                        level.destroyBlock(pos, false, player);
                        player.displayClientMessage(
                                Component.literal(
                                        "NPC marker removed."
                                ),
                                true
                        );
                    }

                    return InteractionResult.sidedSuccess(
                            level.isClientSide
                    );
                }
            }
        }

        InteractionResult result = super.useOn(context);

        if (result.consumesAction()
                && player != null
                && level instanceof ServerLevel serverLevel) {
            BlockPos clicked = context.getClickedPos();
            BlockPos placed = level.getBlockState(clicked)
                    .getBlock() instanceof NpcMarkerBlock
                    ? clicked
                    : clicked.relative(context.getClickedFace());

            if (level.getBlockState(placed).getBlock()
                    instanceof NpcMarkerBlock markerBlock) {
                NpcMarkerManager.RegistrationResult status =
                        NpcMarkerManager.status(
                                serverLevel,
                                placed,
                                markerBlock.kind()
                        );

                player.displayClientMessage(
                        Component.literal(status.message())
                                .withStyle(
                                        status.success()
                                                ? ChatFormatting.GREEN
                                                : ChatFormatting.RED
                                ),
                        true
                );
            }
        }

        return result;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.literal(kind.displayName() + " Marker")
                        .withStyle(ChatFormatting.AQUA)
        );

        switch (kind) {
            case BUILDING -> tooltip.add(
                    Component.literal(
                            "Place inside a building to scan its interior."
                    ).withStyle(ChatFormatting.GRAY)
            );
            case ROOM -> tooltip.add(
                    Component.literal(
                            "Place in a missed room to add it to the nearby Building."
                    ).withStyle(ChatFormatting.GRAY)
            );
            case BED -> tooltip.add(
                    Component.literal(
                            "Links the nearest real bed to the nearby Building."
                    ).withStyle(ChatFormatting.GRAY)
            );
            case WORK -> tooltip.add(
                    Component.literal(
                            "Exact NPC work standing spot. Face the direction the NPC should face."
                    ).withStyle(ChatFormatting.GRAY)
            );
        }

        tooltip.add(
                Component.literal(
                        "Sneak + right-click nearby to remove a marker."
                ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
