package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class CyberNpcEntity extends PathfinderMob {
    public static final int RANGED_STATE_NONE = 0;
    public static final int RANGED_STATE_BOW_DRAW = 1;
    public static final int RANGED_STATE_CROSSBOW_CHARGE = 2;
    public static final int RANGED_STATE_CROSSBOW_HOLD = 3;

    private static final double WILD_HELP_RADIUS = 20.0D;
    private static final double WILD_DISENGAGE_DISTANCE = 40.0D;
    private static final int WILD_DISENGAGE_TICKS = 100;
    private static final int WILD_MIN_AGGRESSION = 10;
    private static final int WILD_MAX_AGGRESSION = 80;

    private static final int MAX_HUNGER = 20;
    static final int HUNT_HUNGER_THRESHOLD = 12;
    private static final int STOP_EATING_HUNGER = 18;
    private static final int HUNGER_DECAY_TICKS = 1200;
    private static final int STARVATION_DAMAGE_TICKS = 80;
    private static final int HUNT_SEARCH_INTERVAL = 100;
    private static final int DROP_SEARCH_TIME = 120;
    private static final int COOK_STATION_SEARCH_INTERVAL = 100;
    private static final int COOK_WAIT_TIMEOUT = 1200;
    private static final int EATING_TICKS = 32;
    private static final int COOK_MODE_NONE = 0;
    private static final int COOK_MODE_FURNACE = 1;
    private static final int COOK_MODE_CAMPFIRE = 2;
    private static final double HUNT_RADIUS = 24.0D;
    private static final int COOK_SEARCH_RADIUS = 12;

    private static final EntityDataAccessor<String> DATA_ROLE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_CAN_WANDER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> DATA_NPC_TYPE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_COMBAT_ACTIVE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_RANGED_STATE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private int aggressionLevel = -1;
    private int provocation;
    private int outOfRangeTicks;
    private int hungerDecayTimer;

    private ItemStack storedSword = ItemStack.EMPTY;
    private ItemStack storedRangedWeapon = ItemStack.EMPTY;
    private ItemStack carriedRawFood = ItemStack.EMPTY;

    private boolean huntingTarget;
    private int huntSearchCooldown;
    private int dropSearchTicks;
    private BlockPos lastHuntKillPos;

    private BlockPos cookingTarget;
    private int cookingSearchCooldown;
    private int cookingMode = COOK_MODE_NONE;
    private int cookingWaitTicks;
    private int cookingOutputBaseline;
    private ItemStack expectedCookedFood = ItemStack.EMPTY;
    private ItemStack foodToEat = ItemStack.EMPTY;
    private int eatingTicks;
    private boolean utilityItemActive;

    private final WildNpcCorralBrain corralBrain;

    public CyberNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        corralBrain = new WildNpcCorralBrain(this);

        getNavigation().setCanFloat(true);
        if (getNavigation() instanceof GroundPathNavigation groundNavigation) {
            groundNavigation.setCanOpenDoors(true);
            groundNavigation.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    public static boolean checkCyberNpcSpawnRules(
            EntityType<CyberNpcEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), entityType);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ROLE, "Citizen");
        entityData.define(DATA_CAN_WANDER, true);
        entityData.define(DATA_NPC_TYPE, NpcType.MAIN.serializedName());
        entityData.define(DATA_COMBAT_ACTIVE, false);
        entityData.define(DATA_HUNGER, MAX_HUNGER);
        entityData.define(DATA_RANGED_STATE, RANGED_STATE_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new WildNpcCombatGoal(this));
        goalSelector.addGoal(5, new ConditionalRandomStrollGoal(this, 0.6D, 120));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public String getRole() {
        return entityData.get(DATA_ROLE);
    }

    public void setRole(String role) {
        String cleaned = role == null ? "" : role.trim();
        entityData.set(DATA_ROLE, cleaned.isEmpty() ? "Citizen" : cleaned);
    }

    public NpcType getNpcType() {
        return NpcType.fromSerializedName(entityData.get(DATA_NPC_TYPE));
    }

    public void setNpcType(NpcType npcType) {
        NpcType safeType = npcType == null ? NpcType.MAIN : npcType;
        entityData.set(DATA_NPC_TYPE, safeType.serializedName());

        if (safeType != NpcType.WILD) {
            setPersistenceRequired();
            calmWildNpc();
        }
    }

    public boolean canWander() {
        return entityData.get(DATA_CAN_WANDER);
    }

    public void setCanWander(boolean canWander) {
        entityData.set(DATA_CAN_WANDER, canWander);

        if (!canWander) {
            getNavigation().stop();
        }
    }

    public boolean isCombatActive() {
        return entityData.get(DATA_COMBAT_ACTIVE);
    }

    private void setCombatActive(boolean active) {
        entityData.set(DATA_COMBAT_ACTIVE, active);
        setAggressive(active);
    }

    public int getRangedState() {
        return entityData.get(DATA_RANGED_STATE);
    }

    private void setRangedState(int state) {
        entityData.set(DATA_RANGED_STATE, state);
    }

    public boolean isChargingCrossbow() {
        return getRangedState() == RANGED_STATE_CROSSBOW_CHARGE;
    }

    public boolean isAimingBow() {
        return getRangedState() == RANGED_STATE_BOW_DRAW;
    }

    public boolean isHoldingChargedCrossbow() {
        return getRangedState() == RANGED_STATE_CROSSBOW_HOLD;
    }

    public int getAggressionLevel() {
        return aggressionLevel;
    }

    public int getHunger() {
        return entityData.get(DATA_HUNGER);
    }

    public void setHunger(int hunger) {
        entityData.set(DATA_HUNGER, Mth.clamp(hunger, 0, MAX_HUNGER));
    }

    public String getHungerBar() {
        int filled = Mth.clamp(Mth.ceil(getHunger() / 2.0F), 0, 10);
        return "[" + "█".repeat(filled) + "░".repeat(10 - filled) + "] " + getHunger() + "/" + MAX_HUNGER;
    }

    public void ensureDefaultName() {
        if (getCustomName() != null) {
            setCustomNameVisible(true);
            return;
        }

        String name = switch (getNpcType()) {
            case MAIN -> "Main NPC";
            case QUEST -> "Quest NPC";
            case WILD -> CyberNpcNameGenerator.randomWildName(getRandom());
        };

        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
    }

    private void ensureWildProfile() {
        if (getNpcType() != NpcType.WILD) {
            return;
        }

        if (aggressionLevel < 0) {
            aggressionLevel = WILD_MIN_AGGRESSION
                    + getRandom().nextInt(WILD_MAX_AGGRESSION - WILD_MIN_AGGRESSION + 1);
        }

        if (storedSword.isEmpty()) {
            storedSword = CyberNpcWeaponPool.randomWildSword(getRandom());
        }

        if (storedRangedWeapon.isEmpty()) {
            storedRangedWeapon = CyberNpcWeaponPool.randomWildRangedWeapon(getRandom());
        }

        if (!isCombatActive() && !utilityItemActive && !getMainHandItem().isEmpty()) {
            stowWeapons();
        }
    }

    public ItemStack getStoredSword() {
        return storedSword;
    }

    public ItemStack getStoredRangedWeapon() {
        return storedRangedWeapon;
    }

    private void equipSword() {
        if (getNpcType() != NpcType.WILD || storedSword.isEmpty()) {
            return;
        }

        utilityItemActive = false;
        ItemStack held = getMainHandItem();
        if (!ItemStack.isSameItemSameTags(held, storedSword)) {
            stopUsingItem();
            setRangedState(RANGED_STATE_NONE);
            setItemSlot(EquipmentSlot.MAINHAND, storedSword.copy());
        }
    }

    private void equipRangedWeapon() {
        if (getNpcType() != NpcType.WILD || storedRangedWeapon.isEmpty()) {
            return;
        }

        utilityItemActive = false;
        ItemStack held = getMainHandItem();
        if (!ItemStack.isSameItemSameTags(held, storedRangedWeapon)) {
            stopUsingItem();
            setRangedState(RANGED_STATE_NONE);
            ItemStack copy = storedRangedWeapon.copy();
            if (copy.is(Items.CROSSBOW)) {
                CrossbowItem.setCharged(copy, false);
            }
            setItemSlot(EquipmentSlot.MAINHAND, copy);
        }
    }

    private void stowWeapons() {
        if (getNpcType() == NpcType.WILD) {
            stopUsingItem();
            setRangedState(RANGED_STATE_NONE);
            utilityItemActive = false;
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            setShiftKeyDown(false);
            setSprinting(false);
        }
    }

    void equipUtilityItem(ItemStack stack) {
        if (isCombatActive() || stack.isEmpty()) {
            return;
        }

        stopUsingItem();
        setRangedState(RANGED_STATE_NONE);
        utilityItemActive = true;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        setItemSlot(EquipmentSlot.MAINHAND, copy);
    }

    void clearUtilityItem() {
        if (!utilityItemActive) {
            return;
        }

        stopUsingItem();
        utilityItemActive = false;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    boolean hasCollectedRawFood() {
        return !carriedRawFood.isEmpty();
    }

    void beginCorralHunt(LivingEntity target) {
        beginWildCombat(target, false, true);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData,
            @Nullable CompoundTag dataTag
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, dataTag);

        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            setNpcType(NpcType.WILD);
            ensureDefaultName();
        }

        if (getNpcType() == NpcType.WILD) {
            setHunger(MAX_HUNGER);
            ensureWildProfile();
            stowWeapons();
        }

        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide || getNpcType() != NpcType.WILD) {
            return;
        }

        ensureWildProfile();

        if (onClimbable() && horizontalCollision) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x, Math.max(movement.y, 0.20D), movement.z);
        }

        tickHunger();
        tickWildCombat();
        tickHuntingAndFood();
    }

    private void tickHunger() {
        hungerDecayTimer++;

        if (hungerDecayTimer >= HUNGER_DECAY_TICKS) {
            hungerDecayTimer = 0;
            if (getHunger() > 0) {
                setHunger(getHunger() - 1);
            }
        }

        if (getHunger() <= 0 && tickCount % STARVATION_DAMAGE_TICKS == 0) {
            hurt(damageSources().starve(), 1.0F);
        }
    }

    private void tickWildCombat() {
        if (!isCombatActive()) {
            if (provocation > 0 && tickCount % 100 == 0) {
                provocation = Math.max(0, provocation - 10);
            }
            return;
        }

        LivingEntity target = getTarget();

        if (target == null) {
            finishCombatWithoutTarget();
            return;
        }

        if (!target.isAlive()) {
            if (huntingTarget) {
                lastHuntKillPos = target.blockPosition();
                dropSearchTicks = DROP_SEARCH_TIME;
                huntingTarget = false;
                finishCombatWithoutTarget();
            } else {
                calmWildNpc();
            }
            return;
        }

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            calmWildNpc();
            return;
        }

        if (distanceToSqr(target) > WILD_DISENGAGE_DISTANCE * WILD_DISENGAGE_DISTANCE) {
            outOfRangeTicks++;

            if (outOfRangeTicks >= WILD_DISENGAGE_TICKS) {
                if (huntingTarget) {
                    huntingTarget = false;
                    finishCombatWithoutTarget();
                    huntSearchCooldown = HUNT_SEARCH_INTERVAL;
                } else {
                    calmWildNpc();
                }
            }
        } else {
            outOfRangeTicks = 0;
        }
    }

    private void finishCombatWithoutTarget() {
        setTarget(null);
        setCombatActive(false);
        stowWeapons();
        getNavigation().stop();
        outOfRangeTicks = 0;
    }

    private void tickHuntingAndFood() {
        if (isCombatActive()) {
            return;
        }

        if (!foodToEat.isEmpty()) {
            tickEating();
            return;
        }

        if (cookingMode != COOK_MODE_NONE) {
            tickCooking();
            return;
        }

        if (!carriedRawFood.isEmpty() && getHunger() < STOP_EATING_HUNGER) {
            tickCooking();
            return;
        }

        if (dropSearchTicks > 0) {
            tickFoodPickup();
            return;
        }

        if (corralBrain.isBusy() || getHunger() <= 14) {
            if (corralBrain.tick()) {
                return;
            }
        }

        if (getHunger() <= HUNT_HUNGER_THRESHOLD) {
            tickPreySearch();
        }
    }

    private void tickPreySearch() {
        if (huntSearchCooldown > 0) {
            huntSearchCooldown--;
            return;
        }

        huntSearchCooldown = HUNT_SEARCH_INTERVAL;

        AABB searchBox = getBoundingBox().inflate(HUNT_RADIUS);
        List<LivingEntity> candidates = level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                this::canHuntPrey
        );

        LivingEntity prey = candidates.stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (prey != null) {
            beginWildCombat(prey, false, true);
        }
    }

    private boolean canHuntPrey(LivingEntity prey) {
        if (prey == this
                || !prey.isAlive()
                || !prey.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)) {
            return false;
        }

        if (prey instanceof AgeableMob ageableMob && ageableMob.isBaby()) {
            return false;
        }

        float confidenceHealth = getHealth() + 6.0F;
        float maximumPreyHealth = getMaxHealth() * 1.5F;

        return prey.getHealth() <= confidenceHealth
                && prey.getMaxHealth() <= maximumPreyHealth;
    }

    private void tickFoodPickup() {
        dropSearchTicks--;

        BlockPos searchCenter = lastHuntKillPos != null ? lastHuntKillPos : blockPosition();
        AABB searchBox = new AABB(searchCenter).inflate(8.0D, 4.0D, 8.0D);

        ItemEntity food = level().getEntitiesOfClass(
                        ItemEntity.class,
                        searchBox,
                        item -> item.isAlive() && CyberNpcHuntingData.isRawFood(item.getItem())
                ).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (food == null) {
            if (dropSearchTicks <= 0) {
                lastHuntKillPos = null;
                huntSearchCooldown = HUNT_SEARCH_INTERVAL;
            }
            return;
        }

        setSprinting(true);
        getNavigation().moveTo(food, 1.05D);

        if (distanceToSqr(food) <= 2.25D) {
            ItemStack found = food.getItem();

            if (carriedRawFood.isEmpty()) {
                carriedRawFood = found.copy();
                food.discard();
            } else if (ItemStack.isSameItemSameTags(carriedRawFood, found)) {
                carriedRawFood.grow(found.getCount());
                food.discard();
            } else {
                return;
            }

            setSprinting(false);
            dropSearchTicks = 0;
            lastHuntKillPos = null;
            resetCookingSearch();
            getNavigation().stop();
        }
    }

    private void tickCooking() {
        setShiftKeyDown(false);
        setSprinting(false);

        if (cookingMode == COOK_MODE_FURNACE) {
            tickFurnaceCooking();
            return;
        }

        if (cookingMode == COOK_MODE_CAMPFIRE) {
            tickCampfireCooking();
            return;
        }

        if (carriedRawFood.isEmpty() || getHunger() >= STOP_EATING_HUNGER) {
            resetCookingSearch();
            return;
        }

        if (cookingTarget == null || !isUsableCookingStation(cookingTarget)) {
            if (cookingSearchCooldown > 0) {
                cookingSearchCooldown--;
                return;
            }

            cookingSearchCooldown = COOK_STATION_SEARCH_INTERVAL;
            cookingTarget = findCookingStation();

            if (cookingTarget == null) {
                return;
            }
        }

        Vec3 cookingSpot = Vec3.atCenterOf(cookingTarget).add(0.0D, 1.0D, 0.0D);
        double distance = distanceToSqr(cookingSpot);

        if (distance > 4.0D) {
            getNavigation().moveTo(cookingSpot.x, cookingSpot.y, cookingSpot.z, 0.9D);
            return;
        }

        getNavigation().stop();

        if (!depositFoodIntoCookingStation(cookingTarget)) {
            cookingTarget = null;
            cookingSearchCooldown = COOK_STATION_SEARCH_INTERVAL;
        }
    }

    private boolean depositFoodIntoCookingStation(BlockPos pos) {
        ItemStack expected = CyberNpcHuntingData.cookOne(carriedRawFood);
        if (expected.isEmpty()) {
            return false;
        }

        var blockEntity = level().getBlockEntity(pos);

        if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack input = furnace.getItem(0);
            ItemStack output = furnace.getItem(2);

            if ((!input.isEmpty()
                    && (!ItemStack.isSameItemSameTags(input, carriedRawFood)
                    || input.getCount() >= input.getMaxStackSize()))
                    || (!output.isEmpty() && !ItemStack.isSameItemSameTags(output, expected))) {
                return false;
            }

            ItemStack fuel = furnace.getItem(1);
            var state = level().getBlockState(pos);
            boolean burning = state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT);

            if (!burning && (fuel.isEmpty() || !AbstractFurnaceBlockEntity.isFuel(fuel))) {
                return false;
            }

            cookingOutputBaseline = output.isEmpty() ? 0 : output.getCount();

            ItemStack oneRaw = carriedRawFood.copy();
            oneRaw.setCount(1);

            if (input.isEmpty()) {
                furnace.setItem(0, oneRaw);
            } else {
                ItemStack updated = input.copy();
                updated.grow(1);
                furnace.setItem(0, updated);
            }

            carriedRawFood.shrink(1);
            if (carriedRawFood.isEmpty()) {
                carriedRawFood = ItemStack.EMPTY;
            }

            furnace.setChanged();
            expectedCookedFood = expected.copy();
            expectedCookedFood.setCount(1);
            cookingMode = COOK_MODE_FURNACE;
            cookingWaitTicks = 0;
            return true;
        }

        if (blockEntity instanceof CampfireBlockEntity campfire) {
            var state = level().getBlockState(pos);
            if (!state.hasProperty(BlockStateProperties.LIT)
                    || !state.getValue(BlockStateProperties.LIT)
                    || campfire.getItems().stream().noneMatch(ItemStack::isEmpty)) {
                return false;
            }

            var recipe = campfire.getCookableRecipe(carriedRawFood);
            if (recipe.isEmpty()) {
                return false;
            }

            ItemStack oneRaw = carriedRawFood.copy();
            oneRaw.setCount(1);

            if (!campfire.placeFood(this, oneRaw, recipe.get().getCookingTime())) {
                return false;
            }

            carriedRawFood.shrink(1);
            if (carriedRawFood.isEmpty()) {
                carriedRawFood = ItemStack.EMPTY;
            }

            expectedCookedFood = expected.copy();
            expectedCookedFood.setCount(1);
            cookingMode = COOK_MODE_CAMPFIRE;
            cookingWaitTicks = 0;
            return true;
        }

        return false;
    }

    private void tickFurnaceCooking() {
        cookingWaitTicks++;

        if (cookingTarget == null
                || !(level().getBlockEntity(cookingTarget) instanceof AbstractFurnaceBlockEntity furnace)) {
            resetCookingState();
            return;
        }

        getNavigation().stop();
        ItemStack output = furnace.getItem(2);

        if (!output.isEmpty()
                && ItemStack.isSameItemSameTags(output, expectedCookedFood)
                && output.getCount() > cookingOutputBaseline) {
            ItemStack cooked = furnace.removeItem(2, 1);
            furnace.setChanged();
            beginEating(cooked);
            return;
        }

        if (!output.isEmpty()
                && ItemStack.isSameItemSameTags(output, expectedCookedFood)
                && output.getCount() < cookingOutputBaseline) {
            cookingOutputBaseline = output.getCount();
        }

        if (cookingWaitTicks > COOK_WAIT_TIMEOUT) {
            resetCookingState();
        }
    }

    private void tickCampfireCooking() {
        cookingWaitTicks++;

        if (cookingTarget == null) {
            resetCookingState();
            return;
        }

        getNavigation().stop();

        AABB pickupBox = new AABB(cookingTarget).inflate(3.0D, 2.0D, 3.0D);
        ItemEntity cookedDrop = level().getEntitiesOfClass(
                        ItemEntity.class,
                        pickupBox,
                        item -> item.isAlive()
                                && !item.getItem().isEmpty()
                                && ItemStack.isSameItemSameTags(item.getItem(), expectedCookedFood)
                ).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (cookedDrop != null) {
            ItemStack stack = cookedDrop.getItem();
            ItemStack one = stack.copy();
            one.setCount(1);
            stack.shrink(1);

            if (stack.isEmpty()) {
                cookedDrop.discard();
            }

            beginEating(one);
            return;
        }

        if (cookingWaitTicks > COOK_WAIT_TIMEOUT) {
            resetCookingState();
        }
    }

    private void beginEating(ItemStack cookedFood) {
        if (cookedFood.isEmpty()) {
            resetCookingState();
            return;
        }

        expectedCookedFood = ItemStack.EMPTY;
        cookingMode = COOK_MODE_NONE;
        cookingWaitTicks = 0;
        cookingOutputBaseline = 0;

        foodToEat = cookedFood.copy();
        foodToEat.setCount(1);
        eatingTicks = 0;

        equipUtilityItem(foodToEat);
        startUsingItem(InteractionHand.MAIN_HAND);
    }

    private void tickEating() {
        setShiftKeyDown(false);
        setSprinting(false);
        getNavigation().stop();

        if (foodToEat.isEmpty()) {
            clearUtilityItem();
            return;
        }

        if (!utilityItemActive) {
            equipUtilityItem(foodToEat);
            startUsingItem(InteractionHand.MAIN_HAND);
        }

        eatingTicks++;

        if (eatingTicks < EATING_TICKS) {
            return;
        }

        stopUsingItem();
        level().playSound(
                null,
                blockPosition(),
                SoundEvents.GENERIC_EAT,
                SoundSource.NEUTRAL,
                0.8F,
                0.95F + getRandom().nextFloat() * 0.1F
        );

        setHunger(getHunger() + CyberNpcHuntingData.hungerRestored(foodToEat));
        foodToEat = ItemStack.EMPTY;
        eatingTicks = 0;
        clearUtilityItem();
        resetCookingSearch();
    }

    @Nullable
    private BlockPos findCookingStation() {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-COOK_SEARCH_RADIUS, -4, -COOK_SEARCH_RADIUS),
                origin.offset(COOK_SEARCH_RADIUS, 4, COOK_SEARCH_RADIUS)
        )) {
            if (!isUsableCookingStation(pos)) {
                continue;
            }

            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }

        return best;
    }

    private boolean isUsableCookingStation(BlockPos pos) {
        if (carriedRawFood.isEmpty()) {
            return false;
        }

        ItemStack expected = CyberNpcHuntingData.cookOne(carriedRawFood);
        if (expected.isEmpty()) {
            return false;
        }

        var blockEntity = level().getBlockEntity(pos);

        if (blockEntity instanceof CampfireBlockEntity campfire) {
            var state = level().getBlockState(pos);
            return state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT)
                    && campfire.getItems().stream().anyMatch(ItemStack::isEmpty)
                    && campfire.getCookableRecipe(carriedRawFood).isPresent();
        }

        if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack input = furnace.getItem(0);
            ItemStack fuel = furnace.getItem(1);
            ItemStack output = furnace.getItem(2);
            var state = level().getBlockState(pos);

            boolean burning = state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT);

            boolean inputAccepts = input.isEmpty()
                    || (ItemStack.isSameItemSameTags(input, carriedRawFood)
                    && input.getCount() < input.getMaxStackSize());

            boolean outputAccepts = output.isEmpty()
                    || ItemStack.isSameItemSameTags(output, expected);

            return inputAccepts
                    && outputAccepts
                    && furnace.canPlaceItem(0, carriedRawFood)
                    && (burning || (!fuel.isEmpty() && AbstractFurnaceBlockEntity.isFuel(fuel)));
        }

        return false;
    }

    private void resetCookingSearch() {
        cookingTarget = null;
        cookingSearchCooldown = 0;
        if (cookingMode == COOK_MODE_NONE) {
            cookingWaitTicks = 0;
            cookingOutputBaseline = 0;
            expectedCookedFood = ItemStack.EMPTY;
        }
    }

    private void resetCookingState() {
        cookingMode = COOK_MODE_NONE;
        cookingWaitTicks = 0;
        cookingOutputBaseline = 0;
        expectedCookedFood = ItemStack.EMPTY;
        cookingTarget = null;
        cookingSearchCooldown = COOK_STATION_SEARCH_INTERVAL;
    }

    private boolean isBusyWithNeeds() {
        return getHunger() <= HUNT_HUNGER_THRESHOLD
                || (!carriedRawFood.isEmpty() && getHunger() < STOP_EATING_HUNGER)
                || !foodToEat.isEmpty()
                || cookingMode != COOK_MODE_NONE
                || dropSearchTicks > 0
                || cookingTarget != null
                || corralBrain.isBusy();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);

        if (!damaged || level().isClientSide || getNpcType() != NpcType.WILD) {
            return damaged;
        }

        if (source.getEntity() instanceof Player player
                && !player.isCreative()
                && !player.isSpectator()) {
            ensureWildProfile();

            if (!isCombatActive() || huntingTarget) {
                int addedProvocation = Math.max(8, Mth.ceil(amount * 6.0F));
                provocation = Mth.clamp(provocation + addedProvocation, 0, 100);

                int hostilityThreshold = 100 - aggressionLevel;

                if (provocation >= hostilityThreshold) {
                    huntingTarget = false;
                    dropSearchTicks = 0;
                    cookingTarget = null;
                    beginWildCombat(player, true, false);
                }
            }
        }

        return damaged;
    }

    private void beginWildCombat(LivingEntity target, boolean callForHelp, boolean isHunt) {
        corralBrain.interrupt();
        clearUtilityItem();
        setTarget(target);
        setCombatActive(true);
        huntingTarget = isHunt;
        provocation = isHunt ? provocation : 100;
        outOfRangeTicks = 0;
        getNavigation().stop();

        if (callForHelp) {
            alertNearbyWildNpcs(target);
        }
    }

    private void alertNearbyWildNpcs(LivingEntity target) {
        List<CyberNpcEntity> nearbyWildNpcs = level().getEntitiesOfClass(
                CyberNpcEntity.class,
                getBoundingBox().inflate(WILD_HELP_RADIUS),
                npc -> npc != this
                        && npc.isAlive()
                        && npc.getNpcType() == NpcType.WILD
        );

        for (CyberNpcEntity npc : nearbyWildNpcs) {
            npc.ensureWildProfile();
            npc.huntingTarget = false;
            npc.dropSearchTicks = 0;
            npc.cookingTarget = null;
            npc.beginWildCombat(target, false, false);
        }
    }

    private void calmWildNpc() {
        setTarget(null);
        setCombatActive(false);
        huntingTarget = false;
        stowWeapons();
        getNavigation().stop();
        provocation = 0;
        outOfRangeTicks = 0;
    }

    private void fireAdaptiveArrow(LivingEntity target, float speed, float inaccuracy, double baseDamage) {
        Arrow arrow = new Arrow(level(), this);

        Vec3 targetVelocity = target.getDeltaMovement();
        double directDistance = position().distanceTo(target.position());
        double flightTicks = Mth.clamp(directDistance / Math.max(speed, 0.1F), 1.0D, 30.0D);

        Vec3 predictedTarget = target.position()
                .add(targetVelocity.scale(flightTicks * 0.85D));

        double dx = predictedTarget.x - getX();
        double dz = predictedTarget.z - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double targetY = predictedTarget.y + target.getBbHeight() * 0.45D;

        double gravityCompensation = 0.5D * 0.05D * flightTicks * flightTicks;
        double dy = targetY - arrow.getY() + gravityCompensation;

        arrow.setBaseDamage(baseDamage);
        arrow.shoot(dx, dy, dz, speed, inaccuracy);
        level().addFreshEntity(arrow);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            String displayName = getCustomName() != null ? getCustomName().getString() : "Cyber NPC";

            if (getNpcType() == NpcType.WILD) {
                player.sendSystemMessage(Component.literal(
                        displayName + " — " + getRole() + " — Hunger " + getHungerBar()
                ));
            } else {
                player.sendSystemMessage(Component.literal(displayName + " — " + getRole()));
            }
        }

        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CyberNpcRole", getRole());
        tag.putBoolean("CyberNpcCanWander", canWander());
        tag.putString("CyberNpcType", getNpcType().serializedName());

        if (aggressionLevel >= 0) {
            tag.putInt("CyberNpcAggression", aggressionLevel);
        }

        tag.putInt("CyberNpcHunger", getHunger());

        if (!storedSword.isEmpty()) {
            tag.put("CyberNpcSword", storedSword.save(new CompoundTag()));
        }

        if (!storedRangedWeapon.isEmpty()) {
            tag.put("CyberNpcRangedWeapon", storedRangedWeapon.save(new CompoundTag()));
        }

        if (!carriedRawFood.isEmpty()) {
            tag.put("CyberNpcRawFood", carriedRawFood.save(new CompoundTag()));
        }

        if (!foodToEat.isEmpty()) {
            tag.put("CyberNpcCookedFood", foodToEat.save(new CompoundTag()));
        }

        if (cookingTarget != null && cookingMode != COOK_MODE_NONE) {
            tag.putLong("CyberNpcCookingTarget", cookingTarget.asLong());
            tag.putInt("CyberNpcCookingMode", cookingMode);
            tag.putInt("CyberNpcCookingWait", cookingWaitTicks);
            tag.putInt("CyberNpcCookingOutputBaseline", cookingOutputBaseline);

            if (!expectedCookedFood.isEmpty()) {
                tag.put("CyberNpcExpectedCookedFood", expectedCookedFood.save(new CompoundTag()));
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("CyberNpcRole")) {
            setRole(tag.getString("CyberNpcRole"));
        }

        if (tag.contains("CyberNpcCanWander")) {
            setCanWander(tag.getBoolean("CyberNpcCanWander"));
        }

        if (tag.contains("CyberNpcType")) {
            setNpcType(NpcType.fromSerializedName(tag.getString("CyberNpcType")));
        } else {
            setNpcType(NpcType.MAIN);
        }

        if (tag.contains("CyberNpcAggression")) {
            aggressionLevel = Mth.clamp(
                    tag.getInt("CyberNpcAggression"),
                    WILD_MIN_AGGRESSION,
                    WILD_MAX_AGGRESSION
            );
        } else {
            aggressionLevel = -1;
        }

        setHunger(tag.contains("CyberNpcHunger") ? tag.getInt("CyberNpcHunger") : MAX_HUNGER);

        storedSword = tag.contains("CyberNpcSword")
                ? ItemStack.of(tag.getCompound("CyberNpcSword"))
                : ItemStack.EMPTY;

        storedRangedWeapon = tag.contains("CyberNpcRangedWeapon")
                ? ItemStack.of(tag.getCompound("CyberNpcRangedWeapon"))
                : ItemStack.EMPTY;

        carriedRawFood = tag.contains("CyberNpcRawFood")
                ? ItemStack.of(tag.getCompound("CyberNpcRawFood"))
                : ItemStack.EMPTY;

        foodToEat = tag.contains("CyberNpcCookedFood")
                ? ItemStack.of(tag.getCompound("CyberNpcCookedFood"))
                : ItemStack.EMPTY;

        if (tag.contains("CyberNpcCookingTarget") && tag.contains("CyberNpcCookingMode")) {
            cookingTarget = BlockPos.of(tag.getLong("CyberNpcCookingTarget"));
            cookingMode = tag.getInt("CyberNpcCookingMode");
            cookingWaitTicks = tag.getInt("CyberNpcCookingWait");
            cookingOutputBaseline = tag.getInt("CyberNpcCookingOutputBaseline");
            expectedCookedFood = tag.contains("CyberNpcExpectedCookedFood")
                    ? ItemStack.of(tag.getCompound("CyberNpcExpectedCookedFood"))
                    : ItemStack.EMPTY;
        } else {
            cookingTarget = null;
            cookingMode = COOK_MODE_NONE;
            cookingWaitTicks = 0;
            cookingOutputBaseline = 0;
            expectedCookedFood = ItemStack.EMPTY;
        }

        if (storedSword.isEmpty() && tag.contains("CyberNpcStoredWeapon")) {
            ItemStack oldWeapon = ItemStack.of(tag.getCompound("CyberNpcStoredWeapon"));
            if (oldWeapon.getItem() instanceof SwordItem) {
                storedSword = oldWeapon;
            }
        }

        setCombatActive(false);
        setRangedState(RANGED_STATE_NONE);
        huntingTarget = false;
        provocation = 0;
        outOfRangeTicks = 0;
        huntSearchCooldown = 0;
        dropSearchTicks = 0;
        lastHuntKillPos = null;
        cookingSearchCooldown = 0;
        eatingTicks = 0;
        utilityItemActive = false;

        if (getNpcType() == NpcType.WILD) {
            ensureWildProfile();
            stowWeapons();
        }

        ensureDefaultName();
    }

    private static final class ConditionalRandomStrollGoal extends RandomStrollGoal {
        private final CyberNpcEntity npc;

        private ConditionalRandomStrollGoal(CyberNpcEntity npc, double speedModifier, int interval) {
            super(npc, speedModifier, interval);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return !npc.isCombatActive()
                    && !npc.isBusyWithNeeds()
                    && npc.canWander()
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.isCombatActive()
                    && !npc.isBusyWithNeeds()
                    && npc.canWander()
                    && super.canContinueToUse();
        }
    }

    private static final class WildNpcCombatGoal extends Goal {
        private static final double MELEE_DISTANCE_SQR = 20.25D;
        private static final double HUNT_STALK_MIN_SQR = 25.0D;
        private static final double HUNT_STALK_MAX_SQR = 196.0D;
        private static final double RANGED_STOP_DISTANCE_SQR = 100.0D;
        private static final double MAX_RANGED_DISTANCE_SQR = 576.0D;

        private final CyberNpcEntity npc;
        private int meleeCooldown;
        private int rangedCooldown;
        private int bowDrawTicks;
        private int crossbowChargeTicks;
        private int crossbowHoldTicks;

        private WildNpcCombatGoal(CyberNpcEntity npc) {
            this.npc = npc;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = npc.getTarget();
            return npc.getNpcType() == NpcType.WILD
                    && npc.isCombatActive()
                    && target != null
                    && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            resetRangedUse();
        }

        @Override
        public void stop() {
            npc.getNavigation().stop();
            npc.stowWeapons();
            resetTimers();
        }

        @Override
        public void tick() {
            LivingEntity target = npc.getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }

            npc.getLookControl().setLookAt(target, 45.0F, 45.0F);

            if (meleeCooldown > 0) {
                meleeCooldown--;
            }
            if (rangedCooldown > 0) {
                rangedCooldown--;
            }

            double distanceSqr = npc.distanceToSqr(target);
            boolean lineOfSight = npc.getSensing().hasLineOfSight(target);

            if (npc.huntingTarget
                    && distanceSqr >= HUNT_STALK_MIN_SQR
                    && distanceSqr <= HUNT_STALK_MAX_SQR) {
                npc.setShiftKeyDown(true);
                npc.setSprinting(false);
                npc.getNavigation().moveTo(target, 0.58D);
            } else {
                npc.setShiftKeyDown(false);
            }

            if (distanceSqr <= MELEE_DISTANCE_SQR) {
                tickMelee(target, distanceSqr);
                return;
            }

            tickRanged(target, distanceSqr, lineOfSight);
        }

        private void tickMelee(LivingEntity target, double distanceSqr) {
            resetRangedUse();
            npc.setShiftKeyDown(false);
            npc.setSprinting(true);
            npc.equipSword();
            npc.getNavigation().moveTo(target, 1.20D);

            if (distanceSqr <= MELEE_DISTANCE_SQR && meleeCooldown <= 0) {
                npc.doHurtTarget(target);
                meleeCooldown = 16;
            }
        }

        private void tickRanged(LivingEntity target, double distanceSqr, boolean lineOfSight) {
            npc.equipRangedWeapon();

            if (distanceSqr > MAX_RANGED_DISTANCE_SQR || !lineOfSight) {
                resetRangedUse();
                npc.setSprinting(!npc.huntingTarget);
                npc.getNavigation().moveTo(target, npc.huntingTarget ? 0.62D : 1.12D);
                return;
            }

            npc.setSprinting(false);

            if (distanceSqr > RANGED_STOP_DISTANCE_SQR) {
                npc.getNavigation().moveTo(target, npc.huntingTarget ? 0.62D : 0.92D);
            } else {
                npc.getNavigation().stop();
            }

            ItemStack ranged = npc.getMainHandItem();

            if (ranged.is(Items.CROSSBOW)) {
                tickCrossbow(target, ranged, distanceSqr);
            } else {
                tickBow(target, distanceSqr);
            }
        }

        private void tickBow(LivingEntity target, double distanceSqr) {
            if (rangedCooldown > 0) {
                resetRangedUse();
                return;
            }

            if (npc.getRangedState() != RANGED_STATE_BOW_DRAW) {
                resetRangedUse();
                npc.setRangedState(RANGED_STATE_BOW_DRAW);
                npc.startUsingItem(InteractionHand.MAIN_HAND);
                bowDrawTicks = 0;
            }

            bowDrawTicks++;

            double distance = Math.sqrt(distanceSqr);
            int desiredDrawTicks = Mth.clamp((int) Math.round(11.0D + distance * 0.35D), 12, 20);

            if (bowDrawTicks < desiredDrawTicks) {
                return;
            }

            float drawPower = Mth.clamp(bowDrawTicks / 20.0F, 0.65F, 1.0F);
            float speed = 1.65F + drawPower * 1.25F;
            float inaccuracy = Mth.clamp(5.0F - (float) distance * 0.08F, 1.5F, 4.5F);

            npc.stopUsingItem();
            npc.setRangedState(RANGED_STATE_NONE);
            npc.fireAdaptiveArrow(target, speed, inaccuracy, 3.0D + drawPower);
            npc.level().playSound(
                    null,
                    npc.blockPosition(),
                    SoundEvents.ARROW_SHOOT,
                    SoundSource.NEUTRAL,
                    1.0F,
                    1.0F / (npc.getRandom().nextFloat() * 0.4F + 0.8F)
            );

            bowDrawTicks = 0;
            rangedCooldown = 12;
        }

        private void tickCrossbow(LivingEntity target, ItemStack crossbow, double distanceSqr) {
            if (rangedCooldown > 0) {
                return;
            }

            int state = npc.getRangedState();

            if (state == RANGED_STATE_NONE) {
                CrossbowItem.setCharged(crossbow, false);
                npc.setRangedState(RANGED_STATE_CROSSBOW_CHARGE);
                npc.startUsingItem(InteractionHand.MAIN_HAND);
                crossbowChargeTicks = 0;
                crossbowHoldTicks = 0;

                npc.level().playSound(
                        null,
                        npc.blockPosition(),
                        SoundEvents.CROSSBOW_LOADING_START,
                        SoundSource.NEUTRAL,
                        0.8F,
                        1.0F
                );
                return;
            }

            if (state == RANGED_STATE_CROSSBOW_CHARGE) {
                crossbowChargeTicks++;
                int chargeDuration = CrossbowItem.getChargeDuration(crossbow);

                if (crossbowChargeTicks < chargeDuration) {
                    return;
                }

                npc.stopUsingItem();
                CrossbowItem.setCharged(crossbow, true);
                npc.setRangedState(RANGED_STATE_CROSSBOW_HOLD);
                crossbowHoldTicks = 6;

                npc.level().playSound(
                        null,
                        npc.blockPosition(),
                        SoundEvents.CROSSBOW_LOADING_END,
                        SoundSource.NEUTRAL,
                        0.9F,
                        1.0F
                );
                return;
            }

            if (state == RANGED_STATE_CROSSBOW_HOLD) {
                if (crossbowHoldTicks > 0) {
                    crossbowHoldTicks--;
                    return;
                }

                double distance = Math.sqrt(distanceSqr);
                float speed = 3.15F;
                float inaccuracy = Mth.clamp(3.0F - (float) distance * 0.04F, 0.8F, 2.5F);

                npc.fireAdaptiveArrow(target, speed, inaccuracy, 4.5D);
                npc.level().playSound(
                        null,
                        npc.blockPosition(),
                        SoundEvents.CROSSBOW_SHOOT,
                        SoundSource.NEUTRAL,
                        1.0F,
                        1.0F
                );

                CrossbowItem.setCharged(crossbow, false);
                npc.setRangedState(RANGED_STATE_NONE);
                crossbowChargeTicks = 0;
                crossbowHoldTicks = 0;
                rangedCooldown = 28;
            }
        }

        private void resetRangedUse() {
            npc.stopUsingItem();

            ItemStack held = npc.getMainHandItem();
            if (held.is(Items.CROSSBOW)) {
                CrossbowItem.setCharged(held, false);
            }

            npc.setRangedState(RANGED_STATE_NONE);
            bowDrawTicks = 0;
            crossbowChargeTicks = 0;
            crossbowHoldTicks = 0;
        }

        private void resetTimers() {
            resetRangedUse();
            meleeCooldown = 0;
            rangedCooldown = 0;
        }
    }
}
