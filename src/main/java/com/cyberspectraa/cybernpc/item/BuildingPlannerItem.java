package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.building.BuildingPointType;
import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.building.BuildingType;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }

        BuildingSavedData data = BuildingSavedData.get(serverLevel);

        if (player.isShiftKeyDown()) {
            BuildingType next = currentDraftType(stack).next();
            setDraftType(stack, next);

            UUID selected = selectedId(stack);
            BuildingSavedData.BuildingRecord record =
                    data.getRecord(selected);
            if (record != null) {
                data.setType(record.id(), next);
                setSelected(stack, record.id(), next.displayName());
                player.displayClientMessage(
                        Component.literal(
                                "Building type: " + next.displayName()
                        ),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal(
                                "New building type: " + next.displayName()
                        ),
                        true
                );
            }

            return InteractionResultHolder.consume(stack);
        }

        EditorMode next = currentMode(stack).next();
        setMode(stack, next);
        clearCorner(stack);

        player.displayClientMessage(
                Component.literal(
                        "Building Planner mode: " + next.displayName
                ),
                true
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

        if (mode == EditorMode.SELECT) {
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
                player.displayClientMessage(
                        Component.literal(
                                "Selected " + existing.name()
                                        + " [" + existing.type().displayName()
                                        + "]"
                        ),
                        true
                );
                return InteractionResult.CONSUME;
            }

            BuildingType type = currentDraftType(stack);
            UUID id = data.createBuilding(
                    level.dimension(),
                    clicked,
                    type
            );
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

            player.displayClientMessage(
                    Component.literal(
                            "Created " + selectedName(stack)
                                    + ". Switch to Zone mode and mark its interior."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        UUID selected = selectedId(stack);
        BuildingSavedData.BuildingRecord record =
                data.getRecord(selected);

        if (record == null) {
            clearSelection(stack);
            player.displayClientMessage(
                    Component.literal(
                            "No building selected. Switch to Select/Create mode first."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (!record.dimension().equals(level.dimension())) {
            player.displayClientMessage(
                    Component.literal(
                            "The selected building is in another dimension."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (mode == EditorMode.ZONE) {
            return handleZoneClick(
                    level,
                    player,
                    stack,
                    data,
                    record,
                    clicked
            );
        }

        if (mode == EditorMode.ENTRANCE) {
            BlockPos entrance = resolveEntrance(level, clicked);
            if (entrance == null) {
                player.displayClientMessage(
                        Component.literal(
                                "Click a door or fence gate to register an entrance."
                        ),
                        true
                );
                return InteractionResult.CONSUME;
            }

            boolean added = data.toggleEntrance(
                    record.id(),
                    entrance
            );
            player.displayClientMessage(
                    Component.literal(
                            (added ? "Added" : "Removed")
                                    + " entrance at "
                                    + formatPos(entrance)
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (mode.pointType != null) {
            BlockPos point = mode.pointType == BuildingPointType.BED
                    ? clicked
                    : clicked.relative(context.getClickedFace());

            float yaw = Mth.wrapDegrees(
                    player.getYRot() + 180.0F
            );

            data.addPoint(
                    record.id(),
                    mode.pointType,
                    point,
                    yaw
            );

            player.displayClientMessage(
                    Component.literal(
                            mode.pointType.displayName()
                                    + " point added at "
                                    + formatPos(point)
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (mode == EditorMode.RESIDENT
                || mode == EditorMode.WORKER) {
            player.displayClientMessage(
                    Component.literal(
                            "In this mode, right-click an NPC instead of a block."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (mode == EditorMode.DELETE) {
            if (!player.isShiftKeyDown()) {
                player.displayClientMessage(
                        Component.literal(
                                "Sneak + right-click a block in Delete mode to remove the selected building."
                        ),
                        true
                );
                return InteractionResult.CONSUME;
            }

            String name = record.name();
            data.removeBuilding(record.id());
            clearSelection(stack);
            clearCorner(stack);

            player.displayClientMessage(
                    Component.literal(
                            "Removed building: " + name
                    ),
                    true
            );
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
            player.displayClientMessage(
                    Component.literal(
                            "Select a building first."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        boolean added = mode == EditorMode.RESIDENT
                ? data.toggleResident(record.id(), npc)
                : data.toggleWorker(record.id(), npc);

        player.displayClientMessage(
                Component.literal(
                        npc.getName().getString()
                                + (added ? " assigned as " : " removed as ")
                                + (mode == EditorMode.RESIDENT
                                ? "resident of "
                                : "worker at ")
                                + record.name()
                ),
                true
        );

        return InteractionResult.CONSUME;
    }

    private InteractionResult handleZoneClick(
            ServerLevel level,
            Player player,
            ItemStack stack,
            BuildingSavedData data,
            BuildingSavedData.BuildingRecord record,
            BlockPos clicked
    ) {
        var root = stack.getOrCreateTagElement(ROOT);

        if (!root.contains(CORNER)) {
            root.putLong(CORNER, clicked.asLong());
            root.putString(
                    CORNER_DIMENSION,
                    level.dimension().location().toString()
            );

            player.displayClientMessage(
                    Component.literal(
                            "Zone corner 1 set: " + formatPos(clicked)
                                    + ". Click the opposite corner."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (!level.dimension().location().toString().equals(
                root.getString(CORNER_DIMENSION)
        )) {
            clearCorner(stack);
            player.displayClientMessage(
                    Component.literal(
                            "Zone corners must be in the same dimension. Corner 1 cleared."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        BlockPos first = BlockPos.of(root.getLong(CORNER));
        BuildingSavedData.ZoneAddResult result =
                data.addZone(record.id(), first, clicked);
        clearCorner(stack);

        if (result == BuildingSavedData.ZoneAddResult.TOO_LARGE) {
            player.displayClientMessage(
                    Component.literal(
                            "That zone is too large. Split the building into several smaller zones."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (result != BuildingSavedData.ZoneAddResult.ADDED) {
            player.displayClientMessage(
                    Component.literal(
                            "The zone could not be added."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        int entrances = data.rescanEntrances(level, record.id());
        String suffix;
        if (entrances == -2) {
            suffix = " Entrance auto-scan skipped because the total area is very large; use Entrance mode.";
        } else {
            suffix = " " + Math.max(0, entrances)
                    + " door/gate entrance"
                    + (entrances == 1 ? "" : "s")
                    + " detected.";
        }

        player.displayClientMessage(
                Component.literal(
                        "Zone added to " + record.name() + "." + suffix
                ),
                true
        );
        return InteractionResult.CONSUME;
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

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.literal(
                        "Mode: " + currentMode(stack).displayName
                )
        );

        String selected = selectedName(stack);
        if (!selected.isBlank()) {
            tooltip.add(Component.literal("Selected: " + selected));
        }

        tooltip.add(
                Component.literal(
                        "Right-click air: next edit mode"
                )
        );
        tooltip.add(
                Component.literal(
                        "Sneak + right-click air: change building type"
                )
        );
        tooltip.add(
                Component.literal(
                        "Zone mode supports multiple boxes per building"
                )
        );
    }

    private static EditorMode currentMode(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        if (root == null) {
            return EditorMode.SELECT;
        }

        String stored = root.getString(MODE);
        if (stored.isBlank()) {
            return EditorMode.SELECT;
        }

        try {
            return EditorMode.valueOf(
                    stored.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ignored) {
            return EditorMode.SELECT;
        }
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

    private static void clearCorner(ItemStack stack) {
        var root = stack.getTagElement(ROOT);
        if (root == null) {
            return;
        }

        root.remove(CORNER);
        root.remove(CORNER_DIMENSION);
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", "
                + pos.getY() + ", "
                + pos.getZ();
    }

    private enum EditorMode {
        SELECT("Select/Create", null),
        ZONE("Zone", null),
        ENTRANCE("Entrance", null),
        ALTAR("Altar Point", BuildingPointType.ALTAR),
        BED("Bed Point", BuildingPointType.BED),
        COUNTER("Counter Point", BuildingPointType.COUNTER),
        SOCIAL("Social Point", BuildingPointType.SOCIAL),
        SEATING("Seating Point", BuildingPointType.SEATING),
        RESIDENT("Assign Resident", null),
        WORKER("Assign Worker", null),
        DELETE("Delete Building", null);

        private final String displayName;
        @Nullable
        private final BuildingPointType pointType;

        EditorMode(
                String displayName,
                @Nullable BuildingPointType pointType
        ) {
            this.displayName = displayName;
            this.pointType = pointType;
        }

        private EditorMode next() {
            EditorMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
