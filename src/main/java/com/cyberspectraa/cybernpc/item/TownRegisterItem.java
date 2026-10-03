package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.block.NpcMarkerBlock;
import com.cyberspectraa.cybernpc.building.BuildingPointType;
import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.building.NpcMarkerIds;
import com.cyberspectraa.cybernpc.building.NpcMarkerKind;
import com.cyberspectraa.cybernpc.building.NpcMarkerManager;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class TownRegisterItem extends Item {
    private static final String ROOT = "CyberNpcTownRegister";

    // Keep the old selected-ID key so existing Town Registers retain their
    // selected special NPC after the 0.41 upgrade.
    private static final String SELECTED = "SelectedSpecialNpc";
    private static final String SELECTED_ENTITY = "SelectedEntity";
    private static final String SELECTED_NAME = "SelectedName";
    private static final String SELECTED_SPECIAL = "SelectedIsSpecial";

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
        boolean special = role != NpcServiceRole.NONE;

        UUID identity = special
                ? SpecialNpcSavedData.get(level).ensureRegistered(npc)
                : npc.getUUID();

        if (identity == null) {
            player.displayClientMessage(
                    Component.literal(
                            "That NPC could not be selected."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        String name = npc.getCustomName() == null
                ? (special ? role.displayName() : npc.getRole())
                : npc.getCustomName().getString();

        var tag = stack.getOrCreateTagElement(ROOT);
        tag.putUUID(SELECTED, identity);
        tag.putUUID(SELECTED_ENTITY, npc.getUUID());
        tag.putString(SELECTED_NAME, name);
        tag.putBoolean(SELECTED_SPECIAL, special);

        player.displayClientMessage(
                Component.literal(
                        "Selected " + name
                                + ". Use the Town Register on a Building marker for their home/building, or on a Work marker for their job."
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
        UUID identity = selectedId(stack);

        if (identity == null) {
            player.displayClientMessage(
                    Component.literal(
                            "Select an NPC with the Town Register first."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);

        if (state.getBlock() instanceof NpcMarkerBlock markerBlock) {
            return useOnMarker(
                    level,
                    player,
                    stack,
                    identity,
                    clicked,
                    state,
                    markerBlock
            );
        }

        // Compatibility: old special-NPC Town Register behaviour still works
        // on ordinary blocks, but the new marker workflow is the recommended
        // setup path.
        if (isSelectedSpecial(stack, level, identity)) {
            return useLegacySpecialAnchor(
                    context,
                    level,
                    player,
                    stack,
                    identity
            );
        }

        player.displayClientMessage(
                Component.literal(
                        "Use the Town Register on a Building marker or Work marker."
                ),
                true
        );
        return InteractionResult.CONSUME;
    }

    private static InteractionResult useOnMarker(
            ServerLevel level,
            Player player,
            ItemStack stack,
            UUID identity,
            BlockPos markerPos,
            BlockState state,
            NpcMarkerBlock markerBlock
    ) {
        NpcMarkerKind kind = markerBlock.kind();
        UUID markerId = NpcMarkerIds.id(
                level.dimension(),
                markerPos,
                kind
        );
        BuildingSavedData buildings = BuildingSavedData.get(level);

        NpcMarkerManager.RegistrationResult refreshed;
        if (buildings.findBuildingForMarker(markerId) == null) {
            refreshed = NpcMarkerManager.register(
                    level,
                    markerPos,
                    kind,
                    state.getValue(
                            HorizontalDirectionalBlock.FACING
                    )
            );

            if (!refreshed.success()) {
                player.displayClientMessage(
                        Component.literal(refreshed.message()),
                        true
                );
                return InteractionResult.CONSUME;
            }
        } else {
            refreshed = NpcMarkerManager.status(
                    level,
                    markerPos,
                    kind
            );
        }

        if (kind == NpcMarkerKind.BUILDING) {
            BuildingSavedData.BuildingRecord building =
                    buildings.findBuildingForMarker(markerId);

            if (building == null
                    || !buildings.assignResident(
                    building.id(),
                    identity
            )) {
                player.displayClientMessage(
                        Component.literal(
                                "The NPC could not be linked to this Building."
                        ),
                        true
                );
                return InteractionResult.CONSUME;
            }

            if (isSelectedSpecial(stack, level, identity)) {
                setSpecialHome(
                        level,
                        identity,
                        building,
                        markerPos,
                        state
                );
            }

            player.displayClientMessage(
                    Component.literal(
                            selectedName(stack, "NPC")
                                    + " is now linked to "
                                    + building.name()
                                    + " as their building/home."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (kind == NpcMarkerKind.WORK) {
            BuildingSavedData.MarkerPoint work =
                    buildings.findMarkerPoint(markerId);

            if (work == null
                    || work.point().type()
                    != BuildingPointType.WORK
                    || !buildings.assignWorkMarker(
                    work.building().id(),
                    markerId,
                    identity
            )) {
                player.displayClientMessage(
                        Component.literal(
                                "The NPC could not be linked to this Work marker."
                        ),
                        true
                );
                return InteractionResult.CONSUME;
            }

            if (isSelectedSpecial(stack, level, identity)) {
                SpecialNpcSavedData.get(level).setWork(
                        identity,
                        new SpecialNpcSavedData.Anchor(
                                level.dimension(),
                                work.point().pos(),
                                work.point().yaw()
                        )
                );
            }

            player.displayClientMessage(
                    Component.literal(
                            selectedName(stack, "NPC")
                                    + " will work at this exact spot in "
                                    + work.building().name()
                                    + "."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (kind == NpcMarkerKind.BED) {
            player.displayClientMessage(
                    Component.literal(
                            refreshed.message()
                                    + " NPCs linked to the Building will use its available Bed markers."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(
                Component.literal(
                        refreshed.message()
                                + " This Room is stored as part of the same Building."
                ),
                true
        );
        return InteractionResult.CONSUME;
    }

    private static void setSpecialHome(
            ServerLevel level,
            UUID specialId,
            BuildingSavedData.BuildingRecord building,
            BlockPos markerPos,
            BlockState markerState
    ) {
        BuildingSavedData buildings = BuildingSavedData.get(level);
        BuildingSavedData.ActivityPoint bed =
                buildings.nearestPoint(
                        building,
                        BuildingPointType.BED,
                        markerPos
                );

        BlockPos homePos = bed == null
                ? markerPos
                : bed.pos();
        float yaw = bed == null
                ? markerState.getValue(
                HorizontalDirectionalBlock.FACING
        ).toYRot()
                : bed.yaw();

        SpecialNpcSavedData.get(level).setHome(
                specialId,
                new SpecialNpcSavedData.Anchor(
                        level.dimension(),
                        homePos,
                        yaw
                )
        );
    }

    private static InteractionResult useLegacySpecialAnchor(
            UseOnContext context,
            ServerLevel level,
            Player player,
            ItemStack stack,
            UUID specialId
    ) {
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
        float facing = Mth.wrapDegrees(
                player.getYRot() + 180.0F
        );

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
            player.displayClientMessage(
                    Component.literal(
                            selectedName(stack, record.name())
                                    + " "
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

    private static boolean isSelectedSpecial(
            ItemStack stack,
            ServerLevel level,
            UUID identity
    ) {
        var tag = stack.getTagElement(ROOT);

        if (tag != null
                && tag.contains(SELECTED_SPECIAL)) {
            return tag.getBoolean(SELECTED_SPECIAL);
        }

        // Old Town Registers do not have the boolean, so resolve against the
        // persistent special-NPC registry.
        return SpecialNpcSavedData.get(level)
                .getRecord(identity) != null;
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
                        "Right-click any CyberNpc NPC: select"
                )
        );
        tooltip.add(
                Component.literal(
                        "Use on Building marker: link home/building"
                )
        );
        tooltip.add(
                Component.literal(
                        "Use on Work marker: assign exact work spot"
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
            tag.remove(SELECTED_ENTITY);
            tag.remove(SELECTED_NAME);
            tag.remove(SELECTED_SPECIAL);
        }
    }
}
