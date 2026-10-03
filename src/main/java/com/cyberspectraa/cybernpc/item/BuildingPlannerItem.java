package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.building.BuildingPointType;
import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.building.BuildingType;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BuildingPlannerItem extends Item {
    private static final String ROOT = "CyberNpcBuildingPlanner";
    private static final String SELECTED = "SelectedBuilding";
    private static final String SELECTED_NAME = "SelectedName";
    private static final String MODE = "Mode";
    private static final String DRAFT_TYPE = "DraftType";
    private static final String CORNER = "Corner";
    private static final String CORNER_DIMENSION = "CornerDimension";

    private static final int VISUAL_INTERVAL = 10;
    private static final double VISUAL_RANGE_SQR = 128.0D * 128.0D;

    public BuildingPlannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected
    ) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!isSelected
                || !(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player)
                || player.tickCount % VISUAL_INTERVAL != 0) {
            return;
        }

        BuildingSavedData data = BuildingSavedData.get(serverLevel);
        BuildingSavedData.BuildingRecord record =
                data.getRecord(selectedId(stack));

        showPlannerStatus(player, stack, record);

        if (record != null
                && record.dimension().equals(serverLevel.dimension())
                && player.distanceToSqr(
                Vec3.atCenterOf(record.core())
        ) <= VISUAL_RANGE_SQR) {
            showBuildingParticles(player, record);
        }

        BlockPos corner = selectedCorner(stack, serverLevel);
        if (corner != null
                && player.distanceToSqr(Vec3.atCenterOf(corner))
                <= VISUAL_RANGE_SQR) {
            showCornerParticles(player, corner);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        BuildingSavedData data = BuildingSavedData.get(serverLevel);

        if (player.isShiftKeyDown()) {
            BuildingType next = currentDraftType(stack).next();
            setDraftType(stack, next);

            // When corner 1 is already set we are defining a brand-new
            // building, so changing the type must not mutate the currently
            // selected old building.
            if (!hasCorner(stack)) {
                BuildingSavedData.BuildingRecord record =
                        data.getRecord(selectedId(stack));
                if (record != null) {
                    data.setType(record.id(), next);
                    setSelected(stack, record.id(), record.name());
                    notifyChat(
                            player,
                            "Changed selected building to "
                                    + next.displayName() + "."
                    );
                } else {
                    notifyChat(
                            player,
                            "New building type: "
                                    + next.displayName() + "."
                    );
                }
            } else {
                notifyChat(
                        player,
                        "New building type: "
                                + next.displayName() + "."
                );
            }

            return InteractionResultHolder.consume(stack);
        }

        EditorMode next = currentMode(stack).next();
        setMode(stack, next);
        clearCorner(stack);

        notifyChat(
                player,
                "Planner tool: " + next.displayName
                        + " — " + next.shortHelp(null)
        );

        return InteractionResultHolder.consume(stack);
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
        BuildingSavedData data = BuildingSavedData.get(level);
        EditorMode mode = currentMode(stack);
        BlockPos clicked = context.getClickedPos();

        if (mode == EditorMode.BUILD) {
            return handleBuildClick(
                    level,
                    player,
                    stack,
                    data,
                    clicked
            );
        }

        BuildingSavedData.BuildingRecord record =
                data.getRecord(selectedId(stack));

        if (record == null) {
            clearSelection(stack);
            clearCorner(stack);
            notifyChat(
                    player,
                    "No building is selected. Switch to Build / Select and click a building."
            );
            return InteractionResult.CONSUME;
        }

        if (!record.dimension().equals(level.dimension())) {
            notifyChat(
                    player,
                    "The selected building is in another dimension."
            );
            return InteractionResult.CONSUME;
        }

        if (mode == EditorMode.ADD_AREA) {
            return handleAreaClick(
                    level,
                    player,
                    stack,
                    data,
                    record,
                    clicked
            );
        }

        if (mode == EditorMode.SPECIAL) {
            return handleSpecialClick(
                    context,
                    level,
                    player,
                    data,
                    record
            );
        }

        if (mode == EditorMode.RESIDENT
                || mode == EditorMode.WORKER) {
            notifyChat(
                    player,
                    "Right-click an NPC with the planner in your hand."
            );
            return InteractionResult.CONSUME;
        }

        if (mode == EditorMode.DELETE) {
            if (!player.isShiftKeyDown()) {
                notifyChat(
                        player,
                        "To prevent accidents, sneak + right-click inside the selected building to delete it."
                );
                return InteractionResult.CONSUME;
            }

            BuildingSavedData.BuildingRecord clickedRecord =
                    data.getBuildingAt(level.dimension(), clicked);

            if (clickedRecord == null
                    || !clickedRecord.id().equals(record.id())) {
                notifyChat(
                        player,
                        "Sneak + right-click inside "
                                + record.name()
                                + " to confirm deletion."
                );
                return InteractionResult.CONSUME;
            }

            String name = record.name();
            data.removeBuilding(record.id());
            clearSelection(stack);
            clearCorner(stack);

            notifyChat(player, "Deleted building: " + name + ".");
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!(player.level() instanceof ServerLevel level)
                || !(target instanceof CyberNpcEntity npc)) {
            return InteractionResult.PASS;
        }

        EditorMode mode = currentMode(stack);
        if (mode != EditorMode.RESIDENT
                && mode != EditorMode.WORKER) {
            return InteractionResult.PASS;
        }

        BuildingSavedData data = BuildingSavedData.get(level);
        BuildingSavedData.BuildingRecord record =
                data.getRecord(selectedId(stack));

        if (record == null) {
            notifyChat(
                    player,
                    "Select a building first."
            );
            return InteractionResult.CONSUME;
        }

        boolean added = mode == EditorMode.RESIDENT
                ? data.toggleResident(record.id(), npc)
                : data.toggleWorker(record.id(), npc);

        notifyChat(
                player,
                npc.getName().getString()
                        + (added ? " assigned as " : " removed as ")
                        + (mode == EditorMode.RESIDENT
                        ? "resident of "
                        : "worker at ")
                        + record.name() + "."
        );

        return InteractionResult.CONSUME;
    }

    private InteractionResult handleBuildClick(
            ServerLevel level,
            Player player,
            ItemStack stack,
            BuildingSavedData data,
            BlockPos clicked
    ) {
        // If corner 1 is already set, this click always finishes the new
        // building. That prevents the second corner accidentally selecting
        // another overlapping property instead.
        if (hasCorner(stack)) {
            BlockPos first = selectedCorner(stack, level);
            if (first == null) {
                clearCorner(stack);
                notifyChat(player, "Corner 1 was invalid and has been cleared.");
                return InteractionResult.CONSUME;
            }

            BuildingType type = currentDraftType(stack);
            UUID id = data.createBuilding(
                    level.dimension(),
                    first,
                    type
            );

            BuildingSavedData.ZoneAddResult result =
                    data.addZone(id, first, clicked);

            if (result != BuildingSavedData.ZoneAddResult.ADDED) {
                data.removeBuilding(id);
                clearCorner(stack);
                notifyChat(
                        player,
                        result == BuildingSavedData.ZoneAddResult.TOO_LARGE
                                ? "That area is too large. Use a smaller box."
                                : "The building could not be created."
                );
                return InteractionResult.CONSUME;
            }

            int doors = data.rescanEntrances(level, id);
            BuildingSavedData.BuildingRecord created =
                    data.getRecord(id);

            setSelected(
                    stack,
                    id,
                    created == null
                            ? type.displayName()
                            : created.name()
            );
            clearCorner(stack);

            notifyChat(
                    player,
                    "Created " + selectedName(stack)
                            + ". I detected "
                            + Math.max(0, doors)
                            + " door/gate"
                            + (doors == 1 ? "" : "s")
                            + " and any beds inside it automatically."
            );

            return InteractionResult.CONSUME;
        }

        BuildingSavedData.BuildingRecord existing =
                data.getBuildingAt(level.dimension(), clicked);

        if (existing == null) {
            existing = data.getEntranceBuilding(
                    level.dimension(),
                    clicked
            );
        }

        if (existing != null) {
            setSelected(
                    stack,
                    existing.id(),
                    existing.name()
            );
            setDraftType(stack, existing.type());
            clearCorner(stack);

            notifyChat(
                    player,
                    "Selected " + existing.name()
                            + " [" + existing.type().displayName() + "]."
            );
            return InteractionResult.CONSUME;
        }

        setCorner(stack, level, clicked);
        notifyChat(
                player,
                "New " + currentDraftType(stack).displayName()
                        + ": corner 1 set at "
                        + formatPos(clicked)
                        + ". Now click the opposite corner."
        );
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleAreaClick(
            ServerLevel level,
            Player player,
            ItemStack stack,
            BuildingSavedData data,
            BuildingSavedData.BuildingRecord record,
            BlockPos clicked
    ) {
        if (!hasCorner(stack)) {
            setCorner(stack, level, clicked);
            notifyChat(
                    player,
                    "Extra area corner 1 set at "
                            + formatPos(clicked)
                            + ". Click the opposite corner."
            );
            return InteractionResult.CONSUME;
        }

        BlockPos first = selectedCorner(stack, level);
        if (first == null) {
            clearCorner(stack);
            notifyChat(player, "Corner 1 was invalid and has been cleared.");
            return InteractionResult.CONSUME;
        }

        BuildingSavedData.ZoneAddResult result =
                data.addZone(record.id(), first, clicked);
        clearCorner(stack);

        if (result == BuildingSavedData.ZoneAddResult.TOO_LARGE) {
            notifyChat(
                    player,
                    "That area is too large. Split it into smaller areas."
            );
            return InteractionResult.CONSUME;
        }

        if (result != BuildingSavedData.ZoneAddResult.ADDED) {
            notifyChat(player, "That extra area could not be added.");
            return InteractionResult.CONSUME;
        }

        int doors = data.rescanEntrances(level, record.id());
        notifyChat(
                player,
                "Added another area to " + record.name()
                        + ". Doors and beds were rescanned automatically"
                        + (doors >= 0 ? " (" + doors + " entrances)." : ".")
        );
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleSpecialClick(
            UseOnContext context,
            ServerLevel level,
            Player player,
            BuildingSavedData data,
            BuildingSavedData.BuildingRecord record
    ) {
        BlockPos clicked = context.getClickedPos();
        BlockPos entrance = resolveEntrance(level, clicked);

        if (entrance != null) {
            boolean added = data.toggleEntrance(
                    record.id(),
                    entrance
            );
            notifyChat(
                    player,
                    (added ? "Added" : "Removed")
                            + " entrance at "
                            + formatPos(entrance) + "."
            );
            return InteractionResult.CONSUME;
        }

        BlockState state = level.getBlockState(clicked);
        if (state.getBlock() instanceof BedBlock) {
            float yaw = state.hasProperty(BedBlock.FACING)
                    ? state.getValue(BedBlock.FACING).toYRot()
                    : 0.0F;

            data.addPoint(
                    record.id(),
                    BuildingPointType.BED,
                    clicked,
                    yaw
            );
            notifyChat(
                    player,
                    "Bed spot added. Normally beds are detected automatically."
            );
            return InteractionResult.CONSUME;
        }

        BuildingPointType pointType =
                specialPointFor(record.type());

        if (pointType == null) {
            notifyChat(
                    player,
                    record.type().displayName()
                            + " does not need a manual special spot. "
                            + "Doors and beds are automatic."
            );
            return InteractionResult.CONSUME;
        }

        BlockPos standingPos =
                clicked.relative(context.getClickedFace());
        float yaw = Mth.wrapDegrees(player.getYRot() + 180.0F);

        data.addPoint(
                record.id(),
                pointType,
                standingPos,
                yaw
        );

        notifyChat(
                player,
                pointType.displayName()
                        + " spot added at "
                        + formatPos(standingPos) + "."
        );
        return InteractionResult.CONSUME;
    }

    @Nullable
    private static BuildingPointType specialPointFor(
            BuildingType type
    ) {
        if (type == BuildingType.CHURCH) {
            return BuildingPointType.ALTAR;
        }

        if (type == BuildingType.SHOP
                || type == BuildingType.BANK
                || type == BuildingType.POST_OFFICE
                || type == BuildingType.WORKSHOP) {
            return BuildingPointType.COUNTER;
        }

        return null;
    }

    @Nullable
    private static BlockPos resolveEntrance(
            ServerLevel level,
            BlockPos clicked
    ) {
        BlockPos[] candidates = new BlockPos[]{
                clicked,
                clicked.below(),
                clicked.above()
        };

        for (BlockPos candidate : candidates) {
            BlockState state = level.getBlockState(candidate);

            if (state.getBlock() instanceof DoorBlock) {
                if (state.hasProperty(
                        BlockStateProperties.DOUBLE_BLOCK_HALF
                )
                        && state.getValue(
                        BlockStateProperties.DOUBLE_BLOCK_HALF
                ) == DoubleBlockHalf.UPPER) {
                    return candidate.below().immutable();
                }
                return candidate.immutable();
            }

            if (state.getBlock() instanceof FenceGateBlock) {
                return candidate.immutable();
            }
        }

        return null;
    }

    private static void showPlannerStatus(
            ServerPlayer player,
            ItemStack stack,
            @Nullable BuildingSavedData.BuildingRecord record
    ) {
        EditorMode mode = currentMode(stack);
        BlockPos corner = selectedCorner(
                stack,
                (ServerLevel) player.level()
        );

        Component message = Component.literal("[Planner] ")
                .withStyle(ChatFormatting.GOLD);

        if (record == null) {
            message = message.copy()
                    .append(
                            Component.literal("No building selected")
                                    .withStyle(ChatFormatting.RED)
                    )
                    .append(Component.literal(" | "))
                    .append(
                            Component.literal(mode.displayName)
                                    .withStyle(ChatFormatting.AQUA)
                    )
                    .append(Component.literal(" | "))
                    .append(Component.literal(
                            corner != null
                                    ? "Corner 1: " + formatPos(corner)
                                    + " • new "
                                    + currentDraftType(stack).displayName()
                                    : mode.shortHelp(null)
                    ).withStyle(ChatFormatting.GRAY));

            player.displayClientMessage(message, true);
            return;
        }

        boolean hasBed = record.points().stream().anyMatch(
                point -> point.type() == BuildingPointType.BED
        );
        boolean hasAltar = record.points().stream().anyMatch(
                point -> point.type() == BuildingPointType.ALTAR
        );

        String features = record.zones().size() + " area"
                + (record.zones().size() == 1 ? "" : "s")
                + " • " + record.entrances().size() + " door"
                + (record.entrances().size() == 1 ? "" : "s")
                + " • " + record.residentCount() + " resident"
                + (record.residentCount() == 1 ? "" : "s");

        if (hasBed) {
            features += " • Bed ✓";
        }
        if (record.type() == BuildingType.CHURCH) {
            features += hasAltar ? " • Altar ✓" : " • Altar MISSING";
        }

        message = message.copy()
                .append(
                        Component.literal(record.name())
                                .withStyle(ChatFormatting.AQUA)
                )
                .append(Component.literal(" | "))
                .append(
                        Component.literal(mode.displayName)
                                .withStyle(ChatFormatting.GREEN)
                )
                .append(Component.literal(" | "))
                .append(
                        Component.literal(
                                corner != null
                                        ? "Corner 1: "
                                        + formatPos(corner)
                                        + " • click opposite corner"
                                        : features
                        ).withStyle(
                                record.type() == BuildingType.CHURCH
                                        && !hasAltar
                                        ? ChatFormatting.YELLOW
                                        : ChatFormatting.GRAY
                        )
                );

        player.displayClientMessage(message, true);
    }

    private static void showBuildingParticles(
            ServerPlayer player,
            BuildingSavedData.BuildingRecord record
    ) {
        for (BuildingSavedData.Zone zone : record.zones()) {
            showZoneBox(player, zone);
        }

        for (BlockPos entrance : record.entrances()) {
            sendParticle(
                    player,
                    ParticleTypes.HAPPY_VILLAGER,
                    entrance.getX() + 0.5D,
                    entrance.getY() + 1.0D,
                    entrance.getZ() + 0.5D
            );
        }

        for (BuildingSavedData.ActivityPoint point : record.points()) {
            ParticleOptions particle = switch (point.type()) {
                case ALTAR -> ParticleTypes.ENCHANT;
                case BED -> ParticleTypes.HEART;
                case COUNTER -> ParticleTypes.CRIT;
                case SOCIAL, SEATING -> ParticleTypes.NOTE;
            };

            sendParticle(
                    player,
                    particle,
                    point.pos().getX() + 0.5D,
                    point.pos().getY() + 0.6D,
                    point.pos().getZ() + 0.5D
            );
        }
    }

    private static void showZoneBox(
            ServerPlayer player,
            BuildingSavedData.Zone zone
    ) {
        double minX = zone.min().getX();
        double minY = zone.min().getY();
        double minZ = zone.min().getZ();
        double maxX = zone.max().getX() + 1.0D;
        double maxY = zone.max().getY() + 1.0D;
        double maxZ = zone.max().getZ() + 1.0D;

        showLine(player, minX, minY, minZ, maxX, minY, minZ);
        showLine(player, minX, minY, maxZ, maxX, minY, maxZ);
        showLine(player, minX, maxY, minZ, maxX, maxY, minZ);
        showLine(player, minX, maxY, maxZ, maxX, maxY, maxZ);

        showLine(player, minX, minY, minZ, minX, maxY, minZ);
        showLine(player, maxX, minY, minZ, maxX, maxY, minZ);
        showLine(player, minX, minY, maxZ, minX, maxY, maxZ);
        showLine(player, maxX, minY, maxZ, maxX, maxY, maxZ);

        showLine(player, minX, minY, minZ, minX, minY, maxZ);
        showLine(player, maxX, minY, minZ, maxX, minY, maxZ);
        showLine(player, minX, maxY, minZ, minX, maxY, maxZ);
        showLine(player, maxX, maxY, minZ, maxX, maxY, maxZ);
    }

    private static void showLine(
            ServerPlayer player,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2
    ) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int segments = Math.max(
                1,
                Math.min(10, Mth.ceil(length))
        );

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / (double) segments;
            sendParticle(
                    player,
                    ParticleTypes.END_ROD,
                    x1 + dx * t,
                    y1 + dy * t,
                    z1 + dz * t
            );
        }
    }

    private static void showCornerParticles(
            ServerPlayer player,
            BlockPos corner
    ) {
        for (int i = 0; i < 4; i++) {
            sendParticle(
                    player,
                    ParticleTypes.FLAME,
                    corner.getX() + 0.5D,
                    corner.getY() + 0.25D + i * 0.45D,
                    corner.getZ() + 0.5D
            );
        }
    }

    private static void sendParticle(
            ServerPlayer player,
            ParticleOptions particle,
            double x,
            double y,
            double z
    ) {
        player.connection.send(
                new ClientboundLevelParticlesPacket(
                        particle,
                        true,
                        x,
                        y,
                        z,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        1
                )
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
                Component.literal(
                        "Tool: " + currentMode(stack).displayName
                ).withStyle(ChatFormatting.AQUA)
        );

        String selected = selectedName(stack);
        if (!selected.isBlank()) {
            tooltip.add(
                    Component.literal("Selected: " + selected)
                            .withStyle(ChatFormatting.GREEN)
            );
        } else {
            tooltip.add(
                    Component.literal(
                            "New type: "
                                    + currentDraftType(stack).displayName()
                    ).withStyle(ChatFormatting.YELLOW)
            );
        }

        tooltip.add(
                Component.literal(
                        "Hold this item to see the selected building."
                ).withStyle(ChatFormatting.GRAY)
        );
        tooltip.add(
                Component.literal(
                        "Right-click air: next tool"
                ).withStyle(ChatFormatting.GRAY)
        );
        tooltip.add(
                Component.literal(
                        "Sneak + right-click air: change building type"
                ).withStyle(ChatFormatting.GRAY)
        );
    }

    private static EditorMode currentMode(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        if (root == null) {
            return EditorMode.BUILD;
        }

        return EditorMode.fromStored(root.getString(MODE));
    }

    private static void setMode(
            ItemStack stack,
            EditorMode mode
    ) {
        stack.getOrCreateTagElement(ROOT)
                .putString(MODE, mode.name());
    }

    private static BuildingType currentDraftType(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        return root == null
                ? BuildingType.HOME
                : BuildingType.fromSerializedName(
                root.getString(DRAFT_TYPE)
        );
    }

    private static void setDraftType(
            ItemStack stack,
            BuildingType type
    ) {
        stack.getOrCreateTagElement(ROOT)
                .putString(DRAFT_TYPE, type.serializedName());
    }

    @Nullable
    private static UUID selectedId(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        if (root == null || !root.hasUUID(SELECTED)) {
            return null;
        }
        return root.getUUID(SELECTED);
    }

    private static String selectedName(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        return root == null ? "" : root.getString(SELECTED_NAME);
    }

    private static void setSelected(
            ItemStack stack,
            UUID id,
            String name
    ) {
        var root = stack.getOrCreateTagElement(ROOT);
        root.putUUID(SELECTED, id);
        root.putString(
                SELECTED_NAME,
                name == null ? "Building" : name
        );
    }

    private static void clearSelection(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        if (root == null) {
            return;
        }

        root.remove(SELECTED);
        root.remove(SELECTED_NAME);
    }

    private static boolean hasCorner(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        return root != null && root.contains(CORNER);
    }

    private static void setCorner(
            ItemStack stack,
            ServerLevel level,
            BlockPos pos
    ) {
        var root = stack.getOrCreateTagElement(ROOT);
        root.putLong(CORNER, pos.asLong());
        root.putString(
                CORNER_DIMENSION,
                level.dimension().location().toString()
        );
    }

    @Nullable
    private static BlockPos selectedCorner(
            ItemStack stack,
            ServerLevel level
    ) {
        var root = stack.getTagElement(ROOT);
        if (root == null
                || !root.contains(CORNER)
                || !level.dimension().location().toString().equals(
                root.getString(CORNER_DIMENSION)
        )) {
            return null;
        }

        return BlockPos.of(root.getLong(CORNER));
    }

    private static void clearCorner(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        if (root == null) {
            return;
        }

        root.remove(CORNER);
        root.remove(CORNER_DIMENSION);
    }

    private static void notifyChat(
            Player player,
            String message
    ) {
        player.displayClientMessage(
                Component.literal(message),
                false
        );
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", "
                + pos.getY() + ", "
                + pos.getZ();
    }

    private enum EditorMode {
        BUILD("Build / Select"),
        ADD_AREA("Add Area"),
        SPECIAL("Special Spot"),
        RESIDENT("Residents"),
        WORKER("Workers"),
        DELETE("Delete");

        private final String displayName;

        EditorMode(String displayName) {
            this.displayName = displayName;
        }

        private EditorMode next() {
            EditorMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        private String shortHelp(
                @Nullable BuildingType type
        ) {
            return switch (this) {
                case BUILD ->
                        "click a building to select it, or click two corners to make one";
                case ADD_AREA ->
                        "click two corners to add another part to the selected building";
                case SPECIAL -> type == BuildingType.CHURCH
                        ? "click the floor where the Pope should stand at the altar"
                        : "click a special work spot; doors and beds are automatic";
                case RESIDENT ->
                        "right-click an NPC who lives here";
                case WORKER ->
                        "right-click an NPC who works here";
                case DELETE ->
                        "sneak + right-click inside the selected building";
            };
        }

        private static EditorMode fromStored(String value) {
            if (value == null || value.isBlank()) {
                return BUILD;
            }

            String normalized = value.trim().toUpperCase(Locale.ROOT);

            // Migrate 0.39.0 planner stacks automatically.
            return switch (normalized) {
                case "BUILD", "SELECT" -> BUILD;
                case "ADD_AREA", "ZONE" -> ADD_AREA;
                case "SPECIAL", "ENTRANCE", "ALTAR", "BED",
                        "COUNTER", "SOCIAL", "SEATING" -> SPECIAL;
                case "RESIDENT" -> RESIDENT;
                case "WORKER" -> WORKER;
                case "DELETE" -> DELETE;
                default -> BUILD;
            };
        }
    }
}
