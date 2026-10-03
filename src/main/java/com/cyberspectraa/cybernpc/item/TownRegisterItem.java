package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.cyberspectraa.cybernpc.service.SpecialNpcSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class TownRegisterItem extends Item {
    private static final String ROOT = "CyberNpcTownRegister";
    private static final String SELECTED = "SelectedSpecialNpc";
    private static final String SELECTED_NAME = "SelectedName";

    public TownRegisterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!(target instanceof CyberNpcEntity npc)
                || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        NpcServiceRole role = NpcServiceRole.fromRole(npc.getRole());
        if (role == NpcServiceRole.NONE) {
            player.displayClientMessage(
                    Component.literal(
                            "The Town Register only binds special service NPCs."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        UUID specialId = SpecialNpcSavedData.get(level)
                .ensureRegistered(npc);

        stack.getOrCreateTagElement(ROOT)
                .putUUID(SELECTED, specialId);
        stack.getOrCreateTagElement(ROOT)
                .putString(
                        SELECTED_NAME,
                        npc.getCustomName() == null
                                ? role.displayName()
                                : npc.getCustomName().getString()
                );

        player.displayClientMessage(
                Component.literal(
                        "Selected "
                                + stack.getTagElement(ROOT)
                                .getString(SELECTED_NAME)
                                + ". Right-click a standing spot for Work; sneak-right-click for Home."
                ),
                true
        );

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        UUID specialId = selectedId(stack);

        if (specialId == null) {
            player.displayClientMessage(
                    Component.literal(
                            "Select a special service NPC with the Town Register first."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        SpecialNpcSavedData data = SpecialNpcSavedData.get(level);
        SpecialNpcSavedData.SpecialNpcRecord record =
                data.getRecord(specialId);

        if (record == null) {
            clearSelection(stack);
            player.displayClientMessage(
                    Component.literal(
                            "That special NPC record no longer exists. Select the NPC again."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        BlockPos standingPos = context.getClickedPos()
                .relative(context.getClickedFace());
        float facing = Mth.wrapDegrees(player.getYRot() + 180.0F);

        SpecialNpcSavedData.Anchor anchor =
                new SpecialNpcSavedData.Anchor(
                        level.dimension(),
                        standingPos,
                        facing
                );

        boolean home = player.isShiftKeyDown();
        boolean changed = home
                ? data.setHome(specialId, anchor)
                : data.setWork(specialId, anchor);

        if (changed) {
            String name = selectedName(stack, record.name());
            player.displayClientMessage(
                    Component.literal(
                            name + " "
                                    + (home ? "home" : "workplace")
                                    + " set to "
                                    + standingPos.getX() + ", "
                                    + standingPos.getY() + ", "
                                    + standingPos.getZ()
                                    + "."
                    ),
                    true
            );
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        String selected = selectedName(stack, "");
        if (!selected.isBlank()) {
            tooltip.add(
                    Component.literal("Selected: " + selected)
            );
        }

        tooltip.add(
                Component.literal(
                        "Right-click special NPC: select"
                )
        );
        tooltip.add(
                Component.literal(
                        "Right-click block: set Work"
                )
        );
        tooltip.add(
                Component.literal(
                        "Sneak + right-click block: set Home"
                )
        );
    }

    @Nullable
    private static UUID selectedId(ItemStack stack) {
        var tag = stack.getTagElement(ROOT);
        if (tag == null || !tag.hasUUID(SELECTED)) {
            return null;
        }
        return tag.getUUID(SELECTED);
    }

    private static String selectedName(
            ItemStack stack,
            String fallback
    ) {
        var tag = stack.getTagElement(ROOT);
        if (tag == null) {
            return fallback == null ? "" : fallback;
        }

        String name = tag.getString(SELECTED_NAME);
        return name.isBlank()
                ? (fallback == null ? "" : fallback)
                : name;
    }

    private static void clearSelection(ItemStack stack) {
        var tag = stack.getTagElement(ROOT);
        if (tag != null) {
            tag.remove(SELECTED);
            tag.remove(SELECTED_NAME);
        }
    }
}
