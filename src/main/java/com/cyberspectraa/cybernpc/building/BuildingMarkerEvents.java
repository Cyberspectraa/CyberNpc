package com.cyberspectraa.cybernpc.building;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.item.BuildingMarkerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BuildingMarkerEvents {
    private static final double LINK_RADIUS_SQR = 64.0D * 64.0D;

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        if (!(event.getTarget() instanceof ItemFrame frame)) {
            return;
        }

        ItemStack held = event.getEntity()
                .getItemInHand(event.getHand());
        ItemStack framed = frame.getItem();

        boolean holdingMarker =
                held.getItem() instanceof BuildingMarkerItem;
        boolean frameHasMarker =
                framed.getItem() instanceof BuildingMarkerItem;

        if (!holdingMarker && !frameHasMarker) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (frameHasMarker) {
            handleExistingMarker(
                    level,
                    player,
                    frame,
                    framed
            );
            return;
        }

        if (!framed.isEmpty()) {
            tell(
                    player,
                    "Use an empty item frame for a CyberNpc building marker.",
                    ChatFormatting.RED
            );
            return;
        }

        BuildingMarkerItem marker =
                (BuildingMarkerItem) held.getItem();

        if (!registerMarker(level, player, frame, marker)) {
            return;
        }

        ItemStack displayed = new ItemStack(marker);
        frame.setItem(displayed);
        frame.setInvisible(true);
        frame.setRotation(0);

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
    }

    private static void handleExistingMarker(
            ServerLevel level,
            ServerPlayer player,
            ItemFrame frame,
            ItemStack framed
    ) {
        BuildingMarkerItem marker =
                (BuildingMarkerItem) framed.getItem();

        if (player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty()) {
            BuildingSavedData.get(level)
                    .removeMarker(frame.getUUID());

            ItemStack returned = framed.copy();
            returned.setCount(1);
            frame.setItem(ItemStack.EMPTY);
            frame.setInvisible(false);

            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }

            tell(
                    player,
                    "Removed "
                            + marker.markerType().displayName()
                            + " marker.",
                    ChatFormatting.YELLOW
            );
            return;
        }

        // Right-clicking an existing marker also refreshes it. This converts
        // 0.40.0 room-scan markers to the new no-scan fixed-area system with
        // one click instead of forcing the player to rebuild the setup.
        if (registerMarker(level, player, frame, marker)) {
            frame.setInvisible(true);
            frame.setRotation(0);
        }
    }

    private static boolean registerMarker(
            ServerLevel level,
            ServerPlayer player,
            ItemFrame frame,
            BuildingMarkerItem marker
    ) {
        BuildingMarkerType type = marker.markerType();
        BuildingSavedData data = BuildingSavedData.get(level);
        UUID markerId = frame.getUUID();

        // Marker position is shifted one block in front of the frame where
        // possible, which normally means into the room rather than the wall.
        BlockPos areaCenter = findStandingPoint(
                level,
                frame.blockPosition().relative(frame.getDirection())
        );

        // Re-registering the same frame is always safe.
        data.removeMarker(markerId);

        if (type.isPointMarker()) {
            return registerPointMarker(
                    level,
                    player,
                    frame,
                    type,
                    data
            );
        }

        BuildingSavedData.Zone zone =
                BuildingSavedData.Zone.around(
                        areaCenter,
                        type.horizontalRadius(),
                        type.verticalRadius()
                );

        if (type.isPrimary()) {
            // Remove only old pre-marker planner data at this exact place.
            // Never delete another real marker building just because two
            // simple influence areas overlap.
            BuildingSavedData.BuildingRecord legacy =
                    data.getBuildingAt(
                            level.dimension(),
                            areaCenter
                    );

            if (legacy != null
                    && legacy.primaryMarkerId() == null) {
                data.removeBuilding(legacy.id());
            }

            UUID buildingId =
                    data.createSimpleMarkerBuilding(
                            level.dimension(),
                            markerId,
                            areaCenter,
                            type.buildingType(),
                            zone,
                            level
                    );

            BuildingSavedData.BuildingRecord created =
                    data.getRecord(buildingId);

            tell(
                    player,
                    (created == null
                            ? type.displayName()
                            : created.name())
                            + " is ready. Done.",
                    ChatFormatting.GREEN
            );
            return true;
        }

        BuildingSavedData.BuildingRecord linked =
                data.findBuildingForRoom(
                        level.dimension(),
                        Set.of(),
                        areaCenter,
                        LINK_RADIUS_SQR
                );

        if (linked == null) {
            tell(
                    player,
                    "Place the main building marker first "
                            + "(for example Church or Home).",
                    ChatFormatting.RED
            );
            return false;
        }

        if (!data.addSimpleMarkerRoom(
                linked.id(),
                markerId,
                type.roomKind(),
                areaCenter,
                zone,
                level
        )) {
            tell(
                    player,
                    "That marker could not be linked to "
                            + linked.name() + ".",
                    ChatFormatting.RED
            );
            return false;
        }

        tell(
                player,
                type.displayName()
                        + " added to " + linked.name()
                        + ". Done.",
                ChatFormatting.GREEN
        );
        return true;
    }

    private static boolean registerPointMarker(
            ServerLevel level,
            ServerPlayer player,
            ItemFrame frame,
            BuildingMarkerType type,
            BuildingSavedData data
    ) {
        BlockPos standingPos =
                findStandingPoint(
                        level,
                        frame.blockPosition()
                                .relative(frame.getDirection())
                );

        BuildingSavedData.BuildingRecord building =
                data.findBuildingForRoom(
                        level.dimension(),
                        Set.of(),
                        standingPos,
                        LINK_RADIUS_SQR
                );

        if (building == null) {
            tell(
                    player,
                    "Place the main building marker first.",
                    ChatFormatting.RED
            );
            return false;
        }

        if (type == BuildingMarkerType.ALTAR
                && building.type() != BuildingType.CHURCH) {
            tell(
                    player,
                    "Put the Altar marker near a Church marker.",
                    ChatFormatting.RED
            );
            return false;
        }

        float yaw = frame.getDirection()
                .getOpposite()
                .toYRot();

        if (!data.addMarkerPoint(
                building.id(),
                frame.getUUID(),
                type.pointType(),
                standingPos,
                yaw
        )) {
            return false;
        }

        tell(
                player,
                type.displayName()
                        + " added to "
                        + building.name() + ". Done.",
                ChatFormatting.GREEN
        );
        return true;
    }

    @SubscribeEvent
    public static void onAttackEntity(
            AttackEntityEvent event
    ) {
        if (!(event.getTarget() instanceof ItemFrame frame)
                || !(event.getEntity() instanceof ServerPlayer)
                || !(event.getEntity().level()
                instanceof ServerLevel level)
                || !(frame.getItem().getItem()
                instanceof BuildingMarkerItem)) {
            return;
        }

        BuildingSavedData.get(level)
                .removeMarker(frame.getUUID());
    }

    @SubscribeEvent
    public static void onEntityLeave(
            EntityLeaveLevelEvent event
    ) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame frame)
                || !(event.getLevel() instanceof ServerLevel level)
                || !(frame.getItem().getItem()
                instanceof BuildingMarkerItem)) {
            return;
        }

        Entity.RemovalReason reason =
                frame.getRemovalReason();

        if (reason != null && reason.shouldDestroy()) {
            BuildingSavedData.get(level)
                    .removeMarker(frame.getUUID());
        }
    }

    private static BlockPos findStandingPoint(
            ServerLevel level,
            BlockPos markerPos
    ) {
        for (int offset = 0; offset <= 6; offset++) {
            BlockPos candidate = markerPos.below(offset);
            BlockPos supportPos = candidate.below();

            if (level.getBlockState(candidate)
                    .getCollisionShape(level, candidate)
                    .isEmpty()
                    && level.getBlockState(candidate.above())
                    .getCollisionShape(level, candidate.above())
                    .isEmpty()
                    && level.getBlockState(supportPos)
                    .isFaceSturdy(
                            level,
                            supportPos,
                            Direction.UP
                    )) {
                return candidate.immutable();
            }
        }

        return markerPos.immutable();
    }

    private static void tell(
            ServerPlayer player,
            String text,
            ChatFormatting colour
    ) {
        player.displayClientMessage(
                Component.literal(text).withStyle(colour),
                false
        );
    }

    private BuildingMarkerEvents() {
    }
}
