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
    private static final double LINK_RADIUS_SQR = 48.0D * 48.0D;

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

        if (!registerMarker(
                level,
                player,
                frame,
                marker
        )) {
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

        BuildingSavedData data = BuildingSavedData.get(level);
        BuildingSavedData.BuildingRecord building =
                data.getBuildingAt(
                        level.dimension(),
                        frame.blockPosition()
                );

        if (building == null) {
            building = data.findBuildingForRoom(
                    level.dimension(),
                    Set.of(),
                    frame.blockPosition(),
                    LINK_RADIUS_SQR
            );
        }

        if (building == null) {
            tell(
                    player,
                    marker.markerType().displayName()
                            + " marker is currently orphaned. "
                            + "Sneak-right-click it with an empty hand, then place it again.",
                    ChatFormatting.RED
            );
            return;
        }

        tell(
                player,
                marker.markerType().displayName()
                        + " → " + building.name()
                        + " [" + building.type().displayName() + "]",
                ChatFormatting.AQUA
        );
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
        BlockPos markerPos = frame.blockPosition();

        // Reusing the same frame after an interrupted setup should never
        // leave duplicate registrations behind.
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

        RoomScanner.ScanResult scan =
                RoomScanner.scan(level, markerPos);

        if (!scan.success()) {
            tell(
                    player,
                    type.displayName() + " marker failed: "
                            + scan.error(),
                    ChatFormatting.RED
            );
            return false;
        }

        if (type.isPrimary()) {
            BuildingSavedData.BuildingRecord existing =
                    data.getBuildingAt(
                            level.dimension(),
                            scan.start()
                    );

            if (existing != null
                    && existing.primaryMarkerId() != null) {
                tell(
                        player,
                        "This room already belongs to "
                                + existing.name()
                                + ". Remove its main marker first.",
                        ChatFormatting.RED
                );
                return false;
            }

            // 0.39.x planner records are replaced automatically the first
            // time a real room marker is installed in that old area.
            if (existing != null) {
                data.removeBuilding(existing.id());
            }

            UUID buildingId = data.createMarkerBuilding(
                    level.dimension(),
                    markerId,
                    scan.start(),
                    type.buildingType(),
                    scan,
                    level
            );

            BuildingSavedData.BuildingRecord created =
                    data.getRecord(buildingId);

            tell(
                    player,
                    (created == null
                            ? type.displayName()
                            : created.name())
                            + " registered automatically: "
                            + scan.cellCount() + " room blocks, "
                            + scan.entrances().size()
                            + " entrance"
                            + (scan.entrances().size() == 1 ? "" : "s")
                            + ".",
                    ChatFormatting.GREEN
            );
            return true;
        }

        BuildingSavedData.BuildingRecord linked =
                data.findBuildingForRoom(
                        level.dimension(),
                        scan.entrances(),
                        scan.start(),
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

        boolean overlapsMain = linked.zones().stream()
                .anyMatch(zone -> zone.contains(scan.start()));

        if (overlapsMain) {
            tell(
                    player,
                    "This is still the same open room as "
                            + linked.name()
                            + ". A Staff/Bedroom room needs a wall, "
                            + "door, trapdoor or gate separating it.",
                    ChatFormatting.RED
            );
            return false;
        }

        if (!data.addMarkerRoom(
                linked.id(),
                markerId,
                type.roomKind(),
                scan,
                level
        )) {
            tell(
                    player,
                    "That room could not be linked to "
                            + linked.name() + ".",
                    ChatFormatting.RED
            );
            return false;
        }

        tell(
                player,
                type.displayName() + " linked to "
                        + linked.name() + ": "
                        + scan.cellCount() + " room blocks"
                        + (scan.beds().isEmpty()
                        ? "."
                        : ", " + scan.beds().size()
                        + " bed"
                        + (scan.beds().size() == 1 ? "" : "s")
                        + " detected."),
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
        BuildingSavedData.BuildingRecord building =
                data.getBuildingAt(
                        level.dimension(),
                        frame.blockPosition()
                );

        if (building == null) {
            building = data.findBuildingForRoom(
                    level.dimension(),
                    Set.of(),
                    frame.blockPosition(),
                    LINK_RADIUS_SQR
            );
        }

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
                    "An Altar marker must belong to a Church.",
                    ChatFormatting.RED
            );
            return false;
        }

        float yaw = frame.getDirection()
                .getOpposite()
                .toYRot();

        BlockPos standingPos =
                findStandingPoint(level, frame.blockPosition());

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
                        + " linked to "
                        + building.name() + ".",
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

        // Clean SavedData before vanilla breaks the frame and potentially
        // clears its displayed stack.
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
        for (int offset = 0; offset <= 5; offset++) {
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
