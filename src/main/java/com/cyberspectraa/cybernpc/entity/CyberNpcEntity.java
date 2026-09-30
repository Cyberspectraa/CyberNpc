package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.compat.IronSpellsCompat;
import com.cyberspectraa.cybernpc.effect.ZombificationEffect;
import com.cyberspectraa.cybernpc.registry.ModEffects;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.world.CyberNpcWorldClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class CyberNpcEntity extends PathfinderMob {
    public static final int RANGED_STATE_NONE = 0;
    public static final int RANGED_STATE_BOW_DRAW = 1;
    public static final int RANGED_STATE_CROSSBOW_CHARGE = 2;
    public static final int RANGED_STATE_CROSSBOW_HOLD = 3;

    private static final int MELEE_SWING_DURATION = 6;
    private static final double WILD_HELP_RADIUS = 24.0D;
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
    private static final int GROUND_FOOD_SEARCH_INTERVAL = 20;
    private static final int HUNT_TARGET_RECHECK_INTERVAL = 20;
    private static final int THREAT_SCAN_INTERVAL = 10;
    private static final int FLEE_SAFE_TICKS = 60;
    private static final double THREAT_SCAN_RADIUS = 20.0D;
    private static final double FLEE_RELEASE_DISTANCE = 30.0D;

    private static final int COMBAT_HELP_COOLDOWN_TICKS = 100;
    private static final int MAX_COMBAT_HELPERS = 3;
    private static final int SPRINT_STALL_TICKS = 8;
    private static final double SPRINT_MOVEMENT_EPSILON_SQR = 0.0025D;

    private static final int FOOD_CHEST_SEARCH_RADIUS = 16;
    private static final int FOOD_CHEST_SEARCH_INTERVAL = 80;
    private static final double FOOD_CHEST_USE_DISTANCE_SQR = 5.0D;

    private static final float EMERGENCY_HEALTH_THRESHOLD = 8.0F;
    private static final double EMERGENCY_EAT_SAFE_DISTANCE_SQR = 64.0D;

    private static final float ZOMBIFICATION_ON_HIT_CHANCE = 0.01F;
    private static final float ZOMBIE_KILL_CONVERSION_CHANCE = 0.05F;
    private static final double INFECTION_SUSPICION_RADIUS = 12.0D;
    private static final int INFECTION_SUSPICION_SCAN_INTERVAL = 20;
    private static final int INFECTION_AVOID_TICKS = 80;

    private static final EntityDataAccessor<String> DATA_ROLE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_CAN_WANDER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> DATA_NPC_TYPE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_WILD_CLASS =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_PERSONALITY =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_COMBAT_ACTIVE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_RANGED_STATE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_MELEE_SWING_TICKS =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_AGGRESSION =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<String> DATA_DEBUG_ACTIVITY =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_TARGET =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_PATH =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_CLAIMS =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_INVENTORY =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private int aggressionLevel = -1;
    private int provocation;
    private int outOfRangeTicks;
    private int hungerDecayTimer;

    private final WildNpcInventory inventory = new WildNpcInventory();

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
    private boolean emergencyEating;
    private boolean zombieConversionStarted;

    @Nullable
    private CyberNpcEntity suspiciousNpc;

    private int infectionSuspicionCooldown;
    private int infectionAvoidTicks;

    @Nullable
    private BlockPos claimedCookingStation;

    @Nullable
    private UUID groundFoodTargetId;

    @Nullable
    private LivingEntity fleeingThreat;

    private int groundFoodSearchCooldown;
    private int huntTargetRecheckCooldown;
    private int threatScanCooldown;
    private int fleeSafeTicks;
    private int fleeRepathCooldown;
    private int combatHelpCooldown;
    private int sprintStallTicks;

    @Nullable
    private BlockPos foodChestTarget;
    private int foodChestSearchCooldown;

    private final WildNpcCorralBrain corralBrain;
    private final WildNpcSleepBrain sleepBrain;

    public CyberNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        corralBrain = new WildNpcCorralBrain(this);
        sleepBrain = new WildNpcSleepBrain(this);

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
        entityData.define(DATA_WILD_CLASS, "");
        entityData.define(DATA_PERSONALITY, "");
        entityData.define(DATA_COMBAT_ACTIVE, false);
        entityData.define(DATA_HUNGER, MAX_HUNGER);
        entityData.define(DATA_RANGED_STATE, RANGED_STATE_NONE);
        entityData.define(DATA_MELEE_SWING_TICKS, 0);
        entityData.define(DATA_AGGRESSION, -1);
        entityData.define(DATA_DEBUG_ACTIVITY, "Idle");
        entityData.define(DATA_DEBUG_TARGET, "none");
        entityData.define(DATA_DEBUG_PATH, "none");
        entityData.define(DATA_DEBUG_CLAIMS, "none");
        entityData.define(DATA_DEBUG_INVENTORY, "empty");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new WildNpcCombatGoal(this));
        goalSelector.addGoal(5, new ConditionalRandomStrollGoal(this, 0.6D, 120));
        goalSelector.addGoal(6, new ConditionalLookAtPlayerGoal(this, 8.0F));
        goalSelector.addGoal(7, new ConditionalRandomLookAroundGoal(this));
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
            entityData.set(DATA_WILD_CLASS, "");
            entityData.set(DATA_PERSONALITY, "");
            setPersistenceRequired();
            sleepBrain.interrupt();
            calmWildNpc();
        }
    }

    public WildNpcClass getWildClass() {
        return WildNpcClass.fromSerializedName(entityData.get(DATA_WILD_CLASS));
    }

    public String getWildClassDisplayName() {
        return getWildClass().displayName();
    }

    private void setWildClass(WildNpcClass wildClass) {
        WildNpcClass safe = wildClass == null ? WildNpcClass.CLASSLESS : wildClass;
        entityData.set(DATA_WILD_CLASS, safe.serializedName());
    }

    public WildNpcPersonality getPersonality() {
        return WildNpcPersonality.fromSerializedName(entityData.get(DATA_PERSONALITY));
    }

    public String getPersonalityDisplayName() {
        return getPersonality().displayName();
    }

    private void setPersonality(WildNpcPersonality personality) {
        WildNpcPersonality safe = personality == null
                ? WildNpcPersonality.BALANCED
                : personality;
        entityData.set(DATA_PERSONALITY, safe.serializedName());
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

    private void startMeleeSwingAnimation() {
        entityData.set(DATA_MELEE_SWING_TICKS, MELEE_SWING_DURATION);
        swing(InteractionHand.MAIN_HAND, true);
    }

    private void tickMeleeSwingAnimation() {
        if (level().isClientSide) {
            return;
        }

        int remaining = entityData.get(DATA_MELEE_SWING_TICKS);
        if (remaining > 0) {
            entityData.set(DATA_MELEE_SWING_TICKS, remaining - 1);
        }
    }

    @Override
    public float getAttackAnim(float partialTick) {
        int remaining = entityData.get(DATA_MELEE_SWING_TICKS);

        if (remaining > 0) {
            float elapsed = MELEE_SWING_DURATION - remaining + partialTick;
            return Mth.clamp(elapsed / (float) MELEE_SWING_DURATION, 0.0F, 1.0F);
        }

        return super.getAttackAnim(partialTick);
    }

    public int getAggressionLevel() {
        return entityData.get(DATA_AGGRESSION);
    }

    public String getDebugActivity() {
        return entityData.get(DATA_DEBUG_ACTIVITY);
    }

    public String getDebugTarget() {
        return entityData.get(DATA_DEBUG_TARGET);
    }

    public String getDebugPath() {
        return entityData.get(DATA_DEBUG_PATH);
    }

    public String getDebugClaims() {
        return entityData.get(DATA_DEBUG_CLAIMS);
    }

    public String getDebugInventory() {
        return entityData.get(DATA_DEBUG_INVENTORY);
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

    public boolean isZombifying() {
        return hasEffect(ModEffects.ZOMBIFICATION.get());
    }

    public float getZombificationProgress() {
        MobEffectInstance effect = getEffect(ModEffects.ZOMBIFICATION.get());
        if (effect == null) {
            return 0.0F;
        }

        return Mth.clamp(
                1.0F - effect.getDuration() / (float) ZombificationEffect.DURATION_TICKS,
                0.0F,
                1.0F
        );
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

        if (entityData.get(DATA_WILD_CLASS).isBlank()) {
            setWildClass(WildNpcClass.randomSpawnClass(getRandom()));
        }

        if (entityData.get(DATA_PERSONALITY).isBlank()) {
            setPersonality(WildNpcPersonality.randomPersonality(getRandom()));
        }

        if (!getWildClass().isAvailable()) {
            setWildClass(WildNpcClass.CLASSLESS);
        }

        applyClassAttributes();

        if (aggressionLevel < 0) {
            aggressionLevel = WILD_MIN_AGGRESSION
                    + getRandom().nextInt(WILD_MAX_AGGRESSION - WILD_MIN_AGGRESSION + 1);

            if (getPersonality() == WildNpcPersonality.AGGRESSIVE) {
                aggressionLevel = Math.min(WILD_MAX_AGGRESSION, aggressionLevel + 15);
            } else if (getPersonality() == WildNpcPersonality.CAUTIOUS) {
                aggressionLevel = Math.max(WILD_MIN_AGGRESSION, aggressionLevel - 12);
            }

            entityData.set(DATA_AGGRESSION, aggressionLevel);
        } else if (entityData.get(DATA_AGGRESSION) != aggressionLevel) {
            entityData.set(DATA_AGGRESSION, aggressionLevel);
        }

        switch (getWildClass()) {
            case CLASSLESS, ARCHER -> {
                if (getStoredSword().isEmpty()) {
                    inventory.add(CyberNpcWeaponPool.randomWildSword(getRandom()));
                }
                if (getStoredRangedWeapon().isEmpty()) {
                    inventory.add(CyberNpcWeaponPool.randomWildRangedWeapon(getRandom()));
                }
            }
            case KNIGHT, ROGUE, MAGE -> {
                if (getStoredSword().isEmpty()) {
                    inventory.add(CyberNpcWeaponPool.randomWildSword(getRandom()));
                }
            }
        }

        if (!isCombatActive() && !utilityItemActive && !getMainHandItem().isEmpty()) {
            stowWeapons();
        }
    }

    private void applyClassAttributes() {
        double maxHealth;
        double movementSpeed;
        double armor;

        switch (getWildClass()) {
            case ARCHER -> {
                maxHealth = 20.0D;
                movementSpeed = 0.285D;
                armor = 1.0D;
            }
            case KNIGHT -> {
                maxHealth = 26.0D;
                movementSpeed = 0.255D;
                armor = 5.0D;
            }
            case ROGUE -> {
                maxHealth = 18.0D;
                movementSpeed = 0.31D;
                armor = 1.0D;
            }
            case MAGE -> {
                maxHealth = 22.0D;
                movementSpeed = 0.275D;
                armor = 2.0D;
            }
            default -> {
                maxHealth = 20.0D;
                movementSpeed = 0.27D;
                armor = 0.0D;
            }
        }

        var healthAttribute = getAttribute(Attributes.MAX_HEALTH);
        var speedAttribute = getAttribute(Attributes.MOVEMENT_SPEED);
        var armorAttribute = getAttribute(Attributes.ARMOR);

        if (healthAttribute != null && healthAttribute.getBaseValue() != maxHealth) {
            double previousMax = healthAttribute.getBaseValue();
            boolean wasFullHealth = getHealth() >= previousMax - 0.01D;
            healthAttribute.setBaseValue(maxHealth);

            if (wasFullHealth) {
                setHealth((float) maxHealth);
            } else if (getHealth() > maxHealth) {
                setHealth((float) maxHealth);
            }
        }

        if (speedAttribute != null && speedAttribute.getBaseValue() != movementSpeed) {
            speedAttribute.setBaseValue(movementSpeed);
        }

        if (armorAttribute != null && armorAttribute.getBaseValue() != armor) {
            armorAttribute.setBaseValue(armor);
        }
    }

    private float getEmergencyHealthThreshold() {
        float base = switch (getPersonality()) {
            case BRAVE -> 6.0F;
            case CAUTIOUS -> 10.0F;
            case AGGRESSIVE -> 5.0F;
            case TACTICAL -> 8.0F;
            default -> EMERGENCY_HEALTH_THRESHOLD;
        };

        if (getWildClass() == WildNpcClass.KNIGHT) {
            base -= 1.0F;
        } else if (getWildClass() == WildNpcClass.MAGE) {
            base += 1.0F;
        }

        return Math.max(4.0F, base);
    }

    private float getHelpHealthFraction() {
        return switch (getPersonality()) {
            case BRAVE -> 0.45F;
            case CAUTIOUS -> 0.75F;
            case AGGRESSIVE -> 0.40F;
            case TACTICAL -> 0.70F;
            default -> 0.60F;
        };
    }

    private double getThreatRequiredRatio() {
        double ratio = switch (getPersonality()) {
            case BRAVE -> 0.88D;
            case CAUTIOUS -> 1.18D;
            case AGGRESSIVE -> 0.82D;
            case TACTICAL -> 1.00D;
            default -> 0.95D;
        };

        if (getWildClass() == WildNpcClass.KNIGHT) {
            ratio -= 0.08D;
        } else if (getWildClass() == WildNpcClass.MAGE) {
            ratio += 0.05D;
        }

        return Math.max(0.70D, ratio);
    }

    public ItemStack getStoredSword() {
        return inventory.findFirst(this::isSwordStack);
    }

    public ItemStack getStoredRangedWeapon() {
        return inventory.findFirst(this::isRangedWeaponStack);
    }

    private boolean isSwordStack(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(CyberNpcWeaponPool.WILD_NPC_SWORDS)
                || stack.getItem() instanceof SwordItem);
    }

    private boolean isRangedWeaponStack(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(CyberNpcWeaponPool.WILD_NPC_RANGED_WEAPONS)
                || stack.is(Items.BOW)
                || stack.is(Items.CROSSBOW));
    }

    private boolean isRawFoodStack(ItemStack stack) {
        return CyberNpcHuntingData.isRawFood(stack);
    }

    private boolean isReadyFoodStack(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isEdible()
                && !CyberNpcHuntingData.isRawFood(stack);
    }

    private ItemStack getRawFoodStack() {
        return inventory.findFirst(this::isRawFoodStack);
    }

    private ItemStack getBestReadyFoodStack() {
        return inventory.findBest(this::isReadyFoodStack, this::emergencyFoodScore);
    }

    private boolean hasReadyFood() {
        return inventory.contains(this::isReadyFoodStack);
    }

    private ItemStack takeOneReadyFood() {
        return inventory.takeOneBest(this::isReadyFoodStack, this::normalFoodScore);
    }

    private ItemStack takeOneEmergencyFood() {
        return inventory.takeOneBest(this::isReadyFoodStack, this::emergencyFoodScore);
    }

    private ItemStack takeOneRawFood() {
        return inventory.takeOne(this::isRawFoodStack);
    }

    private ItemStack addToInventory(ItemStack stack) {
        return inventory.add(stack);
    }

    private void equipSword() {
        ItemStack storedSword = getStoredSword();
        if (getNpcType() != NpcType.WILD || storedSword.isEmpty()) {
            return;
        }

        utilityItemActive = false;
        ItemStack held = getMainHandItem();
        if (!ItemStack.isSameItemSameTags(held, storedSword)) {
            stopUsingItem();
            setRangedState(RANGED_STATE_NONE);
            ItemStack copy = storedSword.copy();
            copy.setCount(1);
            setItemSlot(EquipmentSlot.MAINHAND, copy);
        }
    }

    private void equipRangedWeapon() {
        ItemStack storedRangedWeapon = getStoredRangedWeapon();
        if (getNpcType() != NpcType.WILD || storedRangedWeapon.isEmpty()) {
            return;
        }

        utilityItemActive = false;
        ItemStack held = getMainHandItem();
        if (!ItemStack.isSameItemSameTags(held, storedRangedWeapon)) {
            stopUsingItem();
            setRangedState(RANGED_STATE_NONE);
            ItemStack copy = storedRangedWeapon.copy();
            copy.setCount(1);
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
        return !getRawFoodStack().isEmpty();
    }

    void beginCorralHunt(LivingEntity target) {
        beginWildCombat(target, false, true);
    }

    void prepareForSleep() {
        corralBrain.interrupt();
        clearUtilityItem();
        stowWeapons();
        getNavigation().stop();
        setSprinting(false);
        setShiftKeyDown(false);
    }

    @Nullable
    BlockPos getClaimedBedPos() {
        return sleepBrain.getClaimedBed();
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
        if (!level().isClientSide && isAlive() && isZombifying()) {
            MobEffectInstance infection = getEffect(ModEffects.ZOMBIFICATION.get());
            if (infection != null && infection.getDuration() <= 1) {
                completeZombification();
                return;
            }
        }

        super.tick();

        if (level().isClientSide || getNpcType() != NpcType.WILD) {
            return;
        }

        // Dead NPCs must never run another AI/claim tick after die() releases
        // their persistent world claims.
        if (!isAlive()) {
            setSprinting(false);
            setShiftKeyDown(false);
            return;
        }

        ensureWildProfile();

        if (isZombifying()) {
            clearHostileTargetsWhileZombifying();
        }

        if (combatHelpCooldown > 0) {
            combatHelpCooldown--;
        }

        // Pen ownership is discovered independently from hunger so nearby pens
        // are reserved immediately instead of only when livestock work starts.
        corralBrain.tickClaimDiscovery();

        tickMeleeSwingAnimation();

        if (onClimbable() && horizontalCollision) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x, Math.max(movement.y, 0.20D), movement.z);
        }

        tickHunger();

        if (!isZombifying() && tickInfectionSuspicion()) {
            finishAiTick();
            return;
        }

        if (tickEmergencyRecoveryWithoutThreat()) {
            finishAiTick();
            return;
        }

        if (tickThreatResponse()) {
            finishAiTick();
            return;
        }

        tickWildCombat();

        if (sleepBrain.tick()) {
            finishAiTick();
            return;
        }

        tickHuntingAndFood();
        finishAiTick();
    }

    private void finishAiTick() {
        fixStuckSprintAnimation();
        updateDebugState();
    }

    private void fixStuckSprintAnimation() {
        if (!isSprinting()) {
            sprintStallTicks = 0;
            return;
        }

        Vec3 motion = getDeltaMovement();
        double horizontalSpeedSqr = motion.x * motion.x + motion.z * motion.z;

        if (getNavigation().isDone() || horizontalSpeedSqr < SPRINT_MOVEMENT_EPSILON_SQR) {
            sprintStallTicks++;
            if (sprintStallTicks >= SPRINT_STALL_TICKS) {
                setSprinting(false);
                sprintStallTicks = 0;
            }
        } else {
            sprintStallTicks = 0;
        }
    }

    private void clearHostileTargetsWhileZombifying() {
        for (Mob mob : level().getEntitiesOfClass(
                Mob.class,
                getBoundingBox().inflate(32.0D, 16.0D, 32.0D),
                mob -> mob != this
                        && mob instanceof Enemy
                        && mob.getTarget() == this
        )) {
            mob.setTarget(null);
        }
    }

    private boolean tickEmergencyRecoveryWithoutThreat() {
        if (emergencyEating) {
            tickEating();
            return true;
        }

        ItemStack recoveryFood = getBestReadyFoodStack();

        if (getHealth() > getEmergencyHealthThreshold()
                || isCombatActive()
                || fleeingThreat != null
                || recoveryFood.isEmpty()) {
            return false;
        }

        boolean nearbyThreat = !level().getEntitiesOfClass(
                Mob.class,
                getBoundingBox().inflate(THREAT_SCAN_RADIUS, 10.0D, THREAT_SCAN_RADIUS),
                mob -> mob != this
                        && mob.isAlive()
                        && mob instanceof Enemy
                        && mob.getTarget() == this
        ).isEmpty();

        if (nearbyThreat) {
            return false;
        }

        beginEmergencyEating();
        if (emergencyEating) {
            tickEating();
            return true;
        }

        return false;
    }

    private boolean tickInfectionSuspicion() {
        if (isCombatActive()
                || fleeingThreat != null
                || emergencyEating
                || isSleeping()) {
            suspiciousNpc = null;
            infectionAvoidTicks = 0;
            return false;
        }

        if (suspiciousNpc != null) {
            if (!suspiciousNpc.isAlive()
                    || !suspiciousNpc.isZombifying()
                    || distanceToSqr(suspiciousNpc) > INFECTION_SUSPICION_RADIUS * INFECTION_SUSPICION_RADIUS
                    || infectionAvoidTicks <= 0) {
                suspiciousNpc = null;
                infectionAvoidTicks = 0;
            } else {
                infectionAvoidTicks--;
                moveAwayFromSuspiciousNpc(suspiciousNpc);
                return true;
            }
        }

        if (infectionSuspicionCooldown > 0) {
            infectionSuspicionCooldown--;
            return false;
        }

        infectionSuspicionCooldown = INFECTION_SUSPICION_SCAN_INTERVAL;

        CyberNpcEntity infected = level().getEntitiesOfClass(
                        CyberNpcEntity.class,
                        getBoundingBox().inflate(INFECTION_SUSPICION_RADIUS, 6.0D, INFECTION_SUSPICION_RADIUS),
                        other -> other != this
                                && other.isAlive()
                                && other.getNpcType() == NpcType.WILD
                                && other.isZombifying()
                ).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (infected == null) {
            return false;
        }

        float progress = infected.getZombificationProgress();
        double distance = Math.sqrt(distanceToSqr(infected));
        float distanceFactor = Mth.clamp(
                (float) (1.0D - distance / INFECTION_SUSPICION_RADIUS),
                0.15F,
                1.0F
        );

        float suspicionChance = Mth.clamp(
                0.05F + progress * 0.85F * distanceFactor,
                0.05F,
                0.92F
        );

        if (getRandom().nextFloat() < suspicionChance) {
            suspiciousNpc = infected;
            infectionAvoidTicks = INFECTION_AVOID_TICKS;
            moveAwayFromSuspiciousNpc(infected);
            return true;
        }

        return false;
    }

    private void moveAwayFromSuspiciousNpc(CyberNpcEntity infected) {
        setShiftKeyDown(false);
        setSprinting(false);

        Vec3 away = DefaultRandomPos.getPosAway(
                this,
                10,
                5,
                infected.position()
        );

        if (away != null) {
            getNavigation().moveTo(away.x, away.y, away.z, 0.95D);
        }
    }

    private void beginEmergencyEating() {
        ItemStack one = takeOneEmergencyFood();
        if (one.isEmpty()) {
            return;
        }

        setTarget(null);
        setCombatActive(false);
        huntingTarget = false;
        stowWeapons();
        clearUtilityItem();
        getNavigation().stop();
        setSprinting(false);
        setShiftKeyDown(false);

        emergencyEating = true;
        beginEating(one);
    }

    private int normalFoodScore(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) {
            return Integer.MIN_VALUE;
        }

        // Preserve rare recovery foods during ordinary hunger whenever possible.
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return -20_000;
        }
        if (stack.is(Items.GOLDEN_APPLE)) {
            return -10_000;
        }

        var food = stack.getItem().getFoodProperties();
        if (food == null) {
            return 0;
        }

        return food.getNutrition() * 100
                + Math.round(food.getSaturationModifier() * 100.0F);
    }

    private int emergencyFoodScore(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) {
            return Integer.MIN_VALUE;
        }

        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return 10_000;
        }
        if (stack.is(Items.GOLDEN_APPLE)) {
            return 9_000;
        }

        var food = stack.getItem().getFoodProperties();
        if (food == null) {
            return 0;
        }

        return food.getNutrition() * 100
                + Math.round(food.getSaturationModifier() * 100.0F);
    }

    private float emergencyHealAmount(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) {
            return 0.0F;
        }

        var food = stack.getItem().getFoodProperties();
        if (food == null) {
            return 1.0F;
        }

        return Math.max(1.0F, food.getNutrition() * 0.5F);
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

        if (huntingTarget) {
            target = maybeSwitchHuntTarget(target);
        } else if (target instanceof Mob hostile && hostile instanceof Enemy) {
            if (getHealth() <= 4.0F) {
                startFleeingFrom(hostile);
                return;
            }

            if (combatHelpCooldown <= 0
                    && (getHealth() <= getMaxHealth() * getHelpHealthFraction() || !shouldFightHostile(hostile))) {
                int helpers = alertNearbyWildNpcs(target, MAX_COMBAT_HELPERS);
                combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;

                if (helpers == 0 && !shouldFightHostile(hostile)) {
                    startFleeingFrom(hostile);
                    return;
                }
            }
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

        if (hasReadyFood() && getHunger() < STOP_EATING_HUNGER) {
            ItemStack one = takeOneReadyFood();
            if (!one.isEmpty()) {
                beginEating(one);
                return;
            }
        }

        if (cookingMode != COOK_MODE_NONE) {
            tickCooking();
            return;
        }

        if (!getRawFoodStack().isEmpty() && getHunger() < STOP_EATING_HUNGER) {
            tickCooking();
            return;
        }

        if (dropSearchTicks > 0) {
            tickFoodPickup();
            return;
        }

        if (tickFoodStorage()) {
            return;
        }

        if (tickGroundFoodPickup()) {
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
                || !prey.getType().is(CyberNpcHuntingData.WILD_NPC_PREY)
                || corralBrain.isProtectedLivestock(prey)) {
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

    private LivingEntity maybeSwitchHuntTarget(LivingEntity current) {
        if (huntTargetRecheckCooldown > 0) {
            huntTargetRecheckCooldown--;
            return current;
        }

        huntTargetRecheckCooldown = HUNT_TARGET_RECHECK_INTERVAL;

        AABB nearby = current.getBoundingBox().inflate(12.0D, 5.0D, 12.0D);
        List<LivingEntity> alternatives = level().getEntitiesOfClass(
                LivingEntity.class,
                nearby,
                candidate -> candidate.getType() == current.getType()
                        && canHuntPrey(candidate)
        );

        LivingEntity best = alternatives.stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(current);

        if (best != current) {
            double currentDistance = distanceToSqr(current);
            double bestDistance = distanceToSqr(best);
            boolean currentBlocked = !getSensing().hasLineOfSight(current);

            if (currentBlocked || bestDistance + 9.0D < currentDistance) {
                setTarget(best);
                return best;
            }
        }

        return current;
    }

    private boolean tickFoodStorage() {
        boolean hasCarriedFood = inventory.contains(stack ->
                isRawFoodStack(stack) || isReadyFoodStack(stack));

        boolean wantsStore = getHunger() >= STOP_EATING_HUNGER && hasCarriedFood;

        boolean wantsRetrieve = getHunger() <= HUNT_HUNGER_THRESHOLD
                && getRawFoodStack().isEmpty()
                && !hasReadyFood()
                && foodToEat.isEmpty()
                && cookingMode == COOK_MODE_NONE;

        if (!wantsStore && !wantsRetrieve) {
            foodChestTarget = null;
            return false;
        }

        Container chest = foodChestTarget == null ? null : getFoodChestContainer(foodChestTarget);

        if (chest == null
                || (wantsStore && !canStoreAnyFood(chest))
                || (wantsRetrieve && !containerHasUsableFood(chest))) {
            foodChestTarget = null;

            if (foodChestSearchCooldown > 0) {
                foodChestSearchCooldown--;
                return false;
            }

            foodChestSearchCooldown = FOOD_CHEST_SEARCH_INTERVAL;
            foodChestTarget = findFoodChest(wantsStore);

            if (foodChestTarget == null) {
                return false;
            }

            chest = getFoodChestContainer(foodChestTarget);
            if (chest == null) {
                foodChestTarget = null;
                return false;
            }
        }

        Vec3 chestPos = Vec3.atCenterOf(foodChestTarget);
        if (distanceToSqr(chestPos) > FOOD_CHEST_USE_DISTANCE_SQR) {
            setShiftKeyDown(false);
            setSprinting(false);
            getNavigation().moveTo(
                    chestPos.x,
                    chestPos.y,
                    chestPos.z,
                    0.90D
            );
            return true;
        }

        getNavigation().stop();
        setSprinting(false);
        setShiftKeyDown(false);

        if (wantsStore) {
            boolean changed = false;

            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()
                        || (!isRawFoodStack(stack) && !isReadyFoodStack(stack))) {
                    continue;
                }

                int before = stack.getCount();
                ItemStack remaining = insertIntoContainer(chest, stack);
                if (remaining.getCount() != before) {
                    inventory.setItem(slot, remaining);
                    changed = true;
                }
            }

            if (changed) {
                chest.setChanged();
                level().playSound(
                        null,
                        blockPosition(),
                        SoundEvents.CHEST_OPEN,
                        SoundSource.BLOCKS,
                        0.45F,
                        1.0F
                );
            }
        } else {
            retrieveFoodFromContainer(chest);
        }

        foodChestTarget = null;
        foodChestSearchCooldown = FOOD_CHEST_SEARCH_INTERVAL;
        return true;
    }

    @Nullable
    private BlockPos findFoodChest(boolean storing) {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-FOOD_CHEST_SEARCH_RADIUS, -4, -FOOD_CHEST_SEARCH_RADIUS),
                origin.offset(FOOD_CHEST_SEARCH_RADIUS, 4, FOOD_CHEST_SEARCH_RADIUS)
        )) {
            Container chest = getFoodChestContainer(pos);
            if (chest == null) {
                continue;
            }

            if (storing ? !canStoreAnyFood(chest) : !containerHasUsableFood(chest)) {
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

    @Nullable
    private Container getFoodChestContainer(BlockPos pos) {
        var state = level().getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) {
            return null;
        }

        return ChestBlock.getContainer(chestBlock, state, level(), pos, false);
    }

    private boolean canStoreAnyFood(Container chest) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()
                    && (isRawFoodStack(stack) || isReadyFoodStack(stack))
                    && canInsertIntoContainer(chest, stack)) {
                return true;
            }
        }

        return false;
    }

    private boolean canInsertIntoContainer(Container container, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack existing = container.getItem(slot);

            if (existing.isEmpty() && container.canPlaceItem(slot, stack)) {
                return true;
            }

            if (ItemStack.isSameItemSameTags(existing, stack)
                    && existing.getCount() < Math.min(existing.getMaxStackSize(), container.getMaxStackSize())
                    && container.canPlaceItem(slot, stack)) {
                return true;
            }
        }

        return false;
    }

    private ItemStack insertIntoContainer(Container container, ItemStack source) {
        ItemStack remaining = source.copy();

        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);

            if (!existing.isEmpty()
                    && ItemStack.isSameItemSameTags(existing, remaining)
                    && container.canPlaceItem(slot, remaining)) {
                int max = Math.min(existing.getMaxStackSize(), container.getMaxStackSize());
                int room = max - existing.getCount();

                if (room > 0) {
                    int moved = Math.min(room, remaining.getCount());
                    ItemStack updated = existing.copy();
                    updated.grow(moved);
                    container.setItem(slot, updated);
                    remaining.shrink(moved);
                }
            }
        }

        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);

            if (existing.isEmpty() && container.canPlaceItem(slot, remaining)) {
                int max = Math.min(remaining.getMaxStackSize(), container.getMaxStackSize());
                int moved = Math.min(max, remaining.getCount());
                ItemStack placed = remaining.copy();
                placed.setCount(moved);
                container.setItem(slot, placed);
                remaining.shrink(moved);
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private boolean containerHasUsableFood(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isUsefulGroundFood(stack) && inventory.canAdd(stack)) {
                return true;
            }
        }

        return false;
    }

    private void retrieveFoodFromContainer(Container container) {
        int readySlot = -1;
        int readyScore = Integer.MIN_VALUE;
        int rawSlot = -1;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);

            if (stack.isEmpty() || !inventory.canAdd(stack)) {
                continue;
            }

            if (isReadyFoodStack(stack)) {
                int score = getHealth() <= getEmergencyHealthThreshold()
                        ? emergencyFoodScore(stack)
                        : normalFoodScore(stack);
                if (score > readyScore) {
                    readyScore = score;
                    readySlot = slot;
                }
            }

            if (rawSlot < 0 && isRawFoodStack(stack)) {
                rawSlot = slot;
            }
        }

        int slot = readySlot >= 0 ? readySlot : rawSlot;
        if (slot < 0) {
            return;
        }

        ItemStack stack = container.getItem(slot);
        int amount = Math.min(4, stack.getCount());
        ItemStack taken = container.removeItem(slot, amount);

        if (taken.isEmpty()) {
            return;
        }

        boolean raw = isRawFoodStack(taken);
        ItemStack remainder = addToInventory(taken);

        if (!remainder.isEmpty()) {
            ItemStack chestRemainder = insertIntoContainer(container, remainder);
            if (!chestRemainder.isEmpty()) {
                spawnAtLocation(chestRemainder);
            }
        }

        if (raw) {
            resetCookingSearch();
        }

        container.setChanged();
        level().playSound(
                null,
                blockPosition(),
                SoundEvents.CHEST_OPEN,
                SoundSource.BLOCKS,
                0.45F,
                1.0F
        );
    }

    private boolean tickGroundFoodPickup() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || getHunger() >= STOP_EATING_HUNGER
                || !foodToEat.isEmpty()
                || cookingMode != COOK_MODE_NONE) {
            groundFoodTargetId = null;
            return false;
        }

        ItemEntity targetFood = null;
        if (groundFoodTargetId != null) {
            var entity = serverLevel.getEntity(groundFoodTargetId);
            if (entity instanceof ItemEntity item
                    && item.isAlive()
                    && isUsefulGroundFood(item.getItem())
                    && inventory.canAdd(item.getItem())) {
                targetFood = item;
            } else {
                groundFoodTargetId = null;
            }
        }

        if (targetFood == null) {
            if (groundFoodSearchCooldown > 0) {
                groundFoodSearchCooldown--;
                return false;
            }

            groundFoodSearchCooldown = GROUND_FOOD_SEARCH_INTERVAL;
            targetFood = level().getEntitiesOfClass(
                            ItemEntity.class,
                            getBoundingBox().inflate(16.0D, 6.0D, 16.0D),
                            item -> item.isAlive()
                                    && !item.hasPickUpDelay()
                                    && isUsefulGroundFood(item.getItem())
                                    && inventory.canAdd(item.getItem())
                    ).stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .orElse(null);

            if (targetFood == null) {
                return false;
            }

            groundFoodTargetId = targetFood.getUUID();
        }

        setShiftKeyDown(false);
        setSprinting(true);
        getNavigation().moveTo(targetFood, 1.05D);

        if (distanceToSqr(targetFood) > 2.25D) {
            return true;
        }

        ItemStack stack = targetFood.getItem();
        boolean raw = isRawFoodStack(stack);
        ItemStack remaining = addToInventory(stack);

        if (remaining.isEmpty()) {
            targetFood.discard();
        } else {
            targetFood.setItem(remaining);
        }

        if (raw && remaining.getCount() != stack.getCount()) {
            resetCookingSearch();
        }

        groundFoodTargetId = null;
        setSprinting(false);
        getNavigation().stop();
        return true;
    }

    private boolean isUsefulGroundFood(ItemStack stack) {
        return !stack.isEmpty()
                && (CyberNpcHuntingData.isRawFood(stack)
                || (stack.isEdible() && CyberNpcHuntingData.hungerRestored(stack) > 0));
    }

    private void tickFoodPickup() {
        dropSearchTicks--;

        BlockPos searchCenter = lastHuntKillPos != null ? lastHuntKillPos : blockPosition();
        AABB searchBox = new AABB(searchCenter).inflate(8.0D, 4.0D, 8.0D);

        ItemEntity food = level().getEntitiesOfClass(
                        ItemEntity.class,
                        searchBox,
                        item -> item.isAlive()
                                && isRawFoodStack(item.getItem())
                                && inventory.canAdd(item.getItem())
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
            int before = found.getCount();
            ItemStack remaining = addToInventory(found);

            if (remaining.isEmpty()) {
                food.discard();
            } else {
                food.setItem(remaining);
            }

            if (remaining.getCount() < before) {
                setSprinting(false);
                dropSearchTicks = 0;
                lastHuntKillPos = null;
                resetCookingSearch();
                getNavigation().stop();
            }
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

        if (getRawFoodStack().isEmpty() || getHunger() >= STOP_EATING_HUNGER) {
            resetCookingSearch();
            return;
        }

        if (cookingTarget == null || !isCookingTargetStillValid(cookingTarget)) {
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
            navigateTowardPersistentClaim(cookingTarget, 0.95D);
            return;
        }

        getNavigation().stop();

        if (!depositFoodIntoCookingStation(cookingTarget)) {
            cookingTarget = null;
            cookingSearchCooldown = COOK_STATION_SEARCH_INTERVAL;
        }
    }

    private void navigateTowardPersistentClaim(BlockPos target, double speed) {
        Vec3 destination = Vec3.atCenterOf(target).add(0.0D, 1.0D, 0.0D);
        double distanceSqr = distanceToSqr(destination);

        // Vanilla pathfinders do not build a useful full path into unloaded
        // distant chunks. Walk in reachable local legs toward the persistent
        // claim until the actual station is close enough to path to directly.
        if (distanceSqr <= 18.0D * 18.0D
                || (level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.hasChunkAt(target))) {
            if (getNavigation().moveTo(destination.x, destination.y, destination.z, speed)) {
                return;
            }
        }

        Vec3 waypoint = DefaultRandomPos.getPosTowards(
                this,
                16,
                7,
                destination,
                Math.PI / 2.0D
        );

        if (waypoint != null && waypoint.distanceToSqr(destination) < position().distanceToSqr(destination)) {
            getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, speed);
            return;
        }

        Vec3 direction = destination.subtract(position());
        if (direction.lengthSqr() > 0.001D) {
            Vec3 step = position().add(direction.normalize().scale(Math.min(12.0D, Math.sqrt(distanceSqr))));
            getNavigation().moveTo(step.x, step.y, step.z, speed);
        }
    }

    private boolean depositFoodIntoCookingStation(BlockPos pos) {
        ItemStack rawFood = getRawFoodStack();
        if (rawFood.isEmpty()) {
            return false;
        }

        ItemStack expected = CyberNpcHuntingData.cookOne(rawFood);
        if (expected.isEmpty()) {
            return false;
        }

        var blockEntity = level().getBlockEntity(pos);

        if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack input = furnace.getItem(0);
            ItemStack output = furnace.getItem(2);

            if ((!input.isEmpty()
                    && (!ItemStack.isSameItemSameTags(input, rawFood)
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

            ItemStack oneRaw = rawFood.copy();
            oneRaw.setCount(1);

            if (input.isEmpty()) {
                furnace.setItem(0, oneRaw);
            } else {
                ItemStack updated = input.copy();
                updated.grow(1);
                furnace.setItem(0, updated);
            }

            rawFood.shrink(1);
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

            var recipe = campfire.getCookableRecipe(rawFood);
            if (recipe.isEmpty()) {
                return false;
            }

            ItemStack oneRaw = rawFood.copy();
            oneRaw.setCount(1);

            if (!campfire.placeFood(this, oneRaw, recipe.get().getCookingTime())) {
                return false;
            }

            rawFood.shrink(1);

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

        ItemStack consumed = foodToEat.copy();
        foodToEat.finishUsingItem(level(), this);

        setHunger(getHunger() + CyberNpcHuntingData.hungerRestored(consumed));

        if (emergencyEating && getHealth() < getMaxHealth()) {
            heal(emergencyHealAmount(consumed));
        }

        foodToEat = ItemStack.EMPTY;
        eatingTicks = 0;
        emergencyEating = false;
        clearUtilityItem();
        resetCookingSearch();
    }

    @Nullable
    private BlockPos findCookingStation() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }

        CyberNpcWorldClaims claims = CyberNpcWorldClaims.get(serverLevel);

        if (claimedCookingStation != null) {
            if (!serverLevel.hasChunkAt(claimedCookingStation)) {
                return claimedCookingStation;
            }

            int capacity = cookingStationCapacity(claimedCookingStation);
            if (capacity > 0 && claims.claimCooking(claimedCookingStation, getUUID(), capacity)) {
                return claimedCookingStation;
            }

            claims.releaseCooking(claimedCookingStation, getUUID());
            claimedCookingStation = null;
        }

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

            int capacity = cookingStationCapacity(pos);
            if (capacity <= 0 || !claims.canClaimCooking(pos, getUUID(), capacity)) {
                continue;
            }

            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }

        if (best != null) {
            int capacity = cookingStationCapacity(best);
            if (claims.claimCooking(best, getUUID(), capacity)) {
                claimedCookingStation = best;
                return best;
            }
        }

        return null;
    }

    private boolean isCookingTargetStillValid(BlockPos pos) {
        if (claimedCookingStation != null && claimedCookingStation.equals(pos)) {
            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && !serverLevel.hasChunkAt(pos)) {
                return true;
            }

            return cookingStationCapacity(pos) > 0;
        }

        return isUsableCookingStation(pos);
    }

    private int cookingStationCapacity(BlockPos pos) {
        var blockEntity = level().getBlockEntity(pos);
        if (blockEntity instanceof CampfireBlockEntity) {
            return 4;
        }
        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            return 1;
        }
        return 0;
    }

    private boolean isUsableCookingStation(BlockPos pos) {
        ItemStack rawFood = getRawFoodStack();
        if (rawFood.isEmpty()) {
            return false;
        }

        ItemStack expected = CyberNpcHuntingData.cookOne(rawFood);
        if (expected.isEmpty()) {
            return false;
        }

        var blockEntity = level().getBlockEntity(pos);

        if (blockEntity instanceof CampfireBlockEntity campfire) {
            var state = level().getBlockState(pos);
            return state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT)
                    && campfire.getItems().stream().anyMatch(ItemStack::isEmpty)
                    && campfire.getCookableRecipe(rawFood).isPresent();
        }

        if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
            ItemStack input = furnace.getItem(0);
            ItemStack fuel = furnace.getItem(1);
            ItemStack output = furnace.getItem(2);
            var state = level().getBlockState(pos);

            boolean burning = state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT);

            boolean inputAccepts = input.isEmpty()
                    || (ItemStack.isSameItemSameTags(input, rawFood)
                    && input.getCount() < input.getMaxStackSize());

            boolean outputAccepts = output.isEmpty()
                    || ItemStack.isSameItemSameTags(output, expected);

            return inputAccepts
                    && outputAccepts
                    && furnace.canPlaceItem(0, rawFood)
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

    private boolean tickThreatResponse() {
        if (emergencyEating) {
            tickEating();
            return true;
        }

        if (fleeingThreat != null) {
            if (getHealth() <= getEmergencyHealthThreshold()
                    && hasReadyFood()
                    && (!fleeingThreat.isAlive()
                    || distanceToSqr(fleeingThreat) >= EMERGENCY_EAT_SAFE_DISTANCE_SQR)) {
                beginEmergencyEating();
                if (emergencyEating) {
                    tickEating();
                    return true;
                }
            }

            if (!fleeingThreat.isAlive()
                    || distanceToSqr(fleeingThreat) > FLEE_RELEASE_DISTANCE * FLEE_RELEASE_DISTANCE) {
                fleeSafeTicks++;
                if (fleeSafeTicks >= FLEE_SAFE_TICKS) {
                    fleeingThreat = null;
                    fleeSafeTicks = 0;
                    fleeRepathCooldown = 0;
                    setSprinting(false);
                    getNavigation().stop();
                    return false;
                }
            } else {
                fleeSafeTicks = 0;
            }

            tickFleeing();
            return true;
        }

        if (threatScanCooldown > 0) {
            threatScanCooldown--;
            return false;
        }

        threatScanCooldown = THREAT_SCAN_INTERVAL;

        Mob threat = level().getEntitiesOfClass(
                        Mob.class,
                        getBoundingBox().inflate(THREAT_SCAN_RADIUS, 10.0D, THREAT_SCAN_RADIUS),
                        mob -> mob != this
                                && mob.isAlive()
                                && mob instanceof Enemy
                                && mob.getTarget() == this
                ).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (threat == null) {
            return false;
        }

        if (getHealth() <= getEmergencyHealthThreshold()) {
            startFleeingFrom(threat);

            if (hasReadyFood()
                    && distanceToSqr(threat) >= EMERGENCY_EAT_SAFE_DISTANCE_SQR) {
                beginEmergencyEating();
                if (emergencyEating) {
                    tickEating();
                }
            } else {
                tickFleeing();
            }

            return true;
        }

        if (shouldFightHostile(threat)) {
            if (!isCombatActive() || huntingTarget || getTarget() != threat) {
                beginWildCombat(threat, false, false);
            }
            return false;
        }

        if (!(threat instanceof Creeper)
                && getHealth() > 4.0F
                && combatHelpCooldown <= 0) {
            int helpers = alertNearbyWildNpcs(threat, MAX_COMBAT_HELPERS);
            combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;

            if (helpers > 0) {
                beginWildCombat(threat, false, false);
                return false;
            }
        }

        startFleeingFrom(threat);
        tickFleeing();
        return true;
    }

    private boolean shouldFightHostile(Mob threat) {
        if (threat instanceof Creeper || getHealth() < getEmergencyHealthThreshold()) {
            return false;
        }

        var attackAttribute = threat.getAttribute(Attributes.ATTACK_DAMAGE);
        double attackDamage = attackAttribute == null ? 4.0D : attackAttribute.getValue();
        double threatPower = threat.getHealth() + attackDamage * 2.5D;
        double npcPower = getHealth()
                + (getStoredSword().isEmpty() ? 0.0D : 8.0D)
                + (getStoredRangedWeapon().isEmpty() ? 0.0D : 8.0D);

        return npcPower >= threatPower * getThreatRequiredRatio();
    }

    private void startFleeingFrom(LivingEntity threat) {
        sleepBrain.interrupt();
        corralBrain.interrupt();
        stopUsingItem();
        clearUtilityItem();
        setTarget(null);
        setCombatActive(false);
        huntingTarget = false;
        stowWeapons();
        fleeingThreat = threat;
        fleeSafeTicks = 0;
        fleeRepathCooldown = 0;
        getNavigation().stop();
        setSprinting(true);
    }

    private void tickFleeing() {
        if (fleeingThreat == null) {
            return;
        }

        // Fleeing is a full-speed escape, never a crouch-run.
        setShiftKeyDown(false);

        if (fleeRepathCooldown > 0) {
            fleeRepathCooldown--;
            return;
        }

        fleeRepathCooldown = 10;

        BlockPos shelter = findNearbyShelter(fleeingThreat);
        if (shelter != null) {
            Vec3 shelterSpot = Vec3.atBottomCenterOf(shelter);
            if (distanceToSqr(shelterSpot) > 3.0D) {
                getNavigation().moveTo(shelterSpot.x, shelterSpot.y, shelterSpot.z, 1.22D);
                setShiftKeyDown(false);
            } else {
                getNavigation().stop();
                setSprinting(false);
                setShiftKeyDown(false);
            }
            return;
        }

        BlockPos bed = sleepBrain.getClaimedBed();
        if (bed != null) {
            double threatToBed = fleeingThreat.distanceToSqr(Vec3.atBottomCenterOf(bed));
            if (threatToBed > 100.0D) {
                Vec3 bedSpot = Vec3.atBottomCenterOf(bed);
                if (distanceToSqr(bedSpot) > 4.0D) {
                    navigateTowardPersistentClaim(bed, 1.22D);
                    setShiftKeyDown(false);
                } else {
                    getNavigation().stop();
                    setSprinting(false);
                    setShiftKeyDown(false);
                }
                return;
            }
        }

        Vec3 away = DefaultRandomPos.getPosAway(this, 16, 7, fleeingThreat.position());
        if (away != null) {
            getNavigation().moveTo(away.x, away.y, away.z, 1.25D);
            setSprinting(true);
        }
    }

    @Nullable
    private BlockPos findNearbyShelter(LivingEntity threat) {
        BlockPos origin = blockPosition();
        double currentThreatDistance = threat.distanceToSqr(this);
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos candidate : BlockPos.betweenClosed(
                origin.offset(-10, -3, -10),
                origin.offset(10, 4, 10)
        )) {
            if (level().canSeeSky(candidate)
                    || !level().getBlockState(candidate).getCollisionShape(level(), candidate).isEmpty()
                    || !level().getBlockState(candidate.above()).getCollisionShape(level(), candidate.above()).isEmpty()
                    || level().getBlockState(candidate.below()).getCollisionShape(level(), candidate.below()).isEmpty()) {
                continue;
            }

            double threatDistance = threat.distanceToSqr(Vec3.atBottomCenterOf(candidate));
            if (threatDistance <= currentThreatDistance + 9.0D) {
                continue;
            }

            double travelDistance = candidate.distSqr(origin);
            double score = travelDistance - Math.min(threatDistance, 900.0D) * 0.12D;

            if (score < bestScore) {
                Path path = getNavigation().createPath(candidate, 0);
                if (path != null && (path.canReach() || path.getEndNode() != null)) {
                    best = candidate.immutable();
                    bestScore = score;
                }
            }
        }

        return best;
    }

    private void updateDebugState() {
        if (level().isClientSide || tickCount % 5 != 0) {
            return;
        }

        entityData.set(DATA_DEBUG_ACTIVITY, buildDebugActivity());

        LivingEntity target = getTarget();
        entityData.set(
                DATA_DEBUG_TARGET,
                target == null
                        ? (fleeingThreat == null ? "none" : "avoiding " + fleeingThreat.getName().getString())
                        : target.getName().getString() + " @ " + formatPos(target.blockPosition())
        );

        Path path = getNavigation().getPath();
        if (path == null || path.isDone() || path.getNodeCount() == 0) {
            entityData.set(DATA_DEBUG_PATH, "none");
        } else {
            BlockPos next = path.getNextNodePos();
            BlockPos destination = path.getTarget();
            entityData.set(
                    DATA_DEBUG_PATH,
                    "next " + formatPos(next)
                            + " -> " + formatPos(destination)
                            + " [" + (path.getNextNodeIndex() + 1) + "/" + path.getNodeCount() + "]"
                            + (path.canReach() ? "" : " partial")
            );
        }

        String bed = sleepBrain.getClaimedBed() == null
                ? "bed:none"
                : "bed:" + formatPos(sleepBrain.getClaimedBed());

        String station = "station:none";
        if (claimedCookingStation != null) {
            int count = 0;
            int capacity = cookingStationCapacity(claimedCookingStation);
            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                count = CyberNpcWorldClaims.get(serverLevel).cookingClaimCount(claimedCookingStation);
            }

            station = "station:" + formatPos(claimedCookingStation)
                    + (capacity > 0 ? " " + count + "/" + capacity : "");
        }

        entityData.set(
                DATA_DEBUG_CLAIMS,
                bed + " | " + station + " | pen:" + corralBrain.getClaimDebug()
        );

        entityData.set(
                DATA_DEBUG_INVENTORY,
                inventory.debugSummary()
                        + (foodToEat.isEmpty()
                        ? ""
                        : " | using:" + stackDebug(foodToEat))
        );
    }

    private String buildDebugActivity() {
        MobEffectInstance infection = getEffect(ModEffects.ZOMBIFICATION.get());
        if (infection != null) {
            int seconds = Math.max(0, Mth.ceil(infection.getDuration() / 20.0F));
            return "Zombifying — " + seconds + "s remaining";
        }
        if (suspiciousNpc != null && infectionAvoidTicks > 0) {
            return "Keeping distance from suspicious NPC";
        }
        if (fleeingThreat != null) {
            return "Fleeing/hiding from " + fleeingThreat.getName().getString();
        }
        if (isSleeping()) {
            return "Sleeping";
        }
        if (isCombatActive()) {
            LivingEntity target = getTarget();
            String name = target == null ? "target" : target.getName().getString();
            return huntingTarget ? "Hunting " + name : "Fighting " + name;
        }
        if (!foodToEat.isEmpty()) {
            return "Eating " + foodToEat.getHoverName().getString();
        }
        if (cookingMode == COOK_MODE_FURNACE) {
            return "Waiting for furnace/smoker";
        }
        if (cookingMode == COOK_MODE_CAMPFIRE) {
            return "Waiting for campfire";
        }
        if (!getRawFoodStack().isEmpty()) {
            return claimedCookingStation == null ? "Looking for cooking station" : "Returning to cooking station";
        }
        if (foodChestTarget != null) {
            return getHunger() >= STOP_EATING_HUNGER
                    ? "Storing surplus food in chest"
                    : "Retrieving food from chest";
        }
        if (groundFoodTargetId != null) {
            return "Picking up food";
        }
        if (corralBrain.isBusy()) {
            return corralBrain.getDebugActivity();
        }
        if (sleepBrain.isBusy()) {
            return "Going to claimed bed";
        }
        if (getHunger() <= HUNT_HUNGER_THRESHOLD) {
            return "Looking for food";
        }
        if (!getNavigation().isDone()) {
            return "Travelling";
        }
        return canWander() ? "Wandering/idle" : "Idle";
    }

    private static String stackDebug(ItemStack stack) {
        if (stack.isEmpty()) {
            return "none";
        }
        return stack.getHoverName().getString() + "x" + stack.getCount();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private boolean isBusyWithNeeds() {
        return getHunger() <= HUNT_HUNGER_THRESHOLD
                || (!getRawFoodStack().isEmpty() && getHunger() < STOP_EATING_HUNGER)
                || !foodToEat.isEmpty()
                || cookingMode != COOK_MODE_NONE
                || dropSearchTicks > 0
                || cookingTarget != null
                || corralBrain.isBusy()
                || sleepBrain.isBusy()
                || fleeingThreat != null
                || groundFoodTargetId != null
                || foodChestTarget != null
                || emergencyEating
                || (suspiciousNpc != null && infectionAvoidTicks > 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);

        if (!damaged || level().isClientSide || getNpcType() != NpcType.WILD) {
            return damaged;
        }

        sleepBrain.wakeUp();

        if (source.getEntity() instanceof Zombie zombie
                && !isZombifying()
                && getRandom().nextFloat() < ZOMBIFICATION_ON_HIT_CHANCE) {
            addEffect(new MobEffectInstance(
                    ModEffects.ZOMBIFICATION.get(),
                    ZombificationEffect.DURATION_TICKS,
                    0,
                    false,
                    true,
                    true
            ));

            zombie.setTarget(null);
            calmWildNpc();
            fleeingThreat = null;
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
        if (isZombifying() && target instanceof Enemy) {
            return;
        }

        fleeingThreat = null;
        fleeSafeTicks = 0;
        fleeRepathCooldown = 0;
        sleepBrain.interrupt();
        corralBrain.interrupt();
        clearUtilityItem();
        setTarget(target);
        setCombatActive(true);
        huntingTarget = isHunt;
        provocation = isHunt ? provocation : 100;
        outOfRangeTicks = 0;
        getNavigation().stop();

        if (callForHelp) {
            alertNearbyWildNpcs(target, MAX_COMBAT_HELPERS);
            combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;
        }
    }

    private int alertNearbyWildNpcs(LivingEntity target, int maxHelpers) {
        List<CyberNpcEntity> nearbyWildNpcs = level().getEntitiesOfClass(
                        CyberNpcEntity.class,
                        getBoundingBox().inflate(WILD_HELP_RADIUS),
                        npc -> npc != this
                                && npc.isAlive()
                                && npc.getNpcType() == NpcType.WILD
                                && npc.getHealth() >= 6.0F
                                && npc.getTarget() != target
                ).stream()
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .limit(Math.max(0, maxHelpers))
                .toList();

        for (CyberNpcEntity npc : nearbyWildNpcs) {
            npc.ensureWildProfile();
            npc.huntingTarget = false;
            npc.dropSearchTicks = 0;
            npc.cookingTarget = null;
            npc.combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;
            npc.beginWildCombat(target, false, false);
        }

        return nearbyWildNpcs.size();
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

    public void completeZombification() {
        if (zombieConversionStarted
                || level().isClientSide
                || !isAlive()
                || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        zombieConversionStarted = true;

        corralBrain.interrupt();
        sleepBrain.wakeUp();
        clearUtilityItem();
        stowWeapons();
        setTarget(null);
        setCombatActive(false);
        fleeingThreat = null;
        suspiciousNpc = null;
        setSprinting(false);
        setShiftKeyDown(false);
        releasePersistentClaims();

        ZombieCyberNpcEntity zombie = ModEntities.ZOMBIE_CYBER_NPC.get().create(serverLevel);
        if (zombie == null) {
            zombieConversionStarted = false;
            return;
        }

        zombie.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        zombie.finalizeSpawn(
                serverLevel,
                serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.CONVERSION,
                null,
                null
        );

        if (getCustomName() != null) {
            zombie.setCustomName(getCustomName().copy());
            zombie.setCustomNameVisible(isCustomNameVisible());
        }

        zombie.setPersistenceRequired();
        serverLevel.addFreshEntity(zombie);
        discard();
    }

    private void spawnZombieNpcAfterDeath() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        ZombieCyberNpcEntity zombie = ModEntities.ZOMBIE_CYBER_NPC.get().create(serverLevel);
        if (zombie == null) {
            return;
        }

        zombie.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        zombie.finalizeSpawn(
                serverLevel,
                serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.CONVERSION,
                null,
                null
        );

        if (getCustomName() != null) {
            zombie.setCustomName(getCustomName().copy());
            zombie.setCustomNameVisible(isCustomNameVisible());
        }

        zombie.setPersistenceRequired();
        serverLevel.addFreshEntity(zombie);
    }

    private void dropOwnedStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        spawnAtLocation(stack.copy());
    }

    @Override
    protected void dropEquipment() {
        // The 18-slot Wild NPC inventory is authoritative. Combat/utility hand
        // items are only presentation copies, so stow them before this runs.
        for (ItemStack stack : inventory.removeAll()) {
            dropOwnedStack(stack);
        }

        dropOwnedStack(foodToEat);
        foodToEat = ItemStack.EMPTY;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = getItemBySlot(slot);
            if (!equipped.isEmpty()) {
                dropOwnedStack(equipped);
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public void releasePersistentClaims() {
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            CyberNpcWorldClaims.get(serverLevel).releaseOwner(getUUID());
        }
        claimedCookingStation = null;
    }

    @Override
    public void die(DamageSource source) {
        boolean convertFromZombieKill = !level().isClientSide
                && source.getEntity() instanceof Zombie
                && getRandom().nextFloat() < ZOMBIE_KILL_CONVERSION_CHANCE;

        if (!level().isClientSide) {
            corralBrain.interrupt();
            sleepBrain.wakeUp();
            fleeingThreat = null;
            suspiciousNpc = null;
            emergencyEating = false;
            clearUtilityItem();
            stowWeapons();
            setSprinting(false);
            setShiftKeyDown(false);
            releasePersistentClaims();
        }

        super.die(source);

        if (convertFromZombieKill && !zombieConversionStarted) {
            zombieConversionStarted = true;
            spawnZombieNpcAfterDeath();
            discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            String displayName = getCustomName() != null ? getCustomName().getString() : "Cyber NPC";

            if (getNpcType() == NpcType.WILD) {
                player.sendSystemMessage(Component.literal(
                        displayName + " — " + getWildClassDisplayName() + " / "
                                + getPersonalityDisplayName() + " — Hunger " + getHungerBar()
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

        if (getNpcType() == NpcType.WILD) {
            tag.putString("CyberNpcWildClass", getWildClass().serializedName());
            tag.putString("CyberNpcPersonality", getPersonality().serializedName());
        }

        if (aggressionLevel >= 0) {
            tag.putInt("CyberNpcAggression", aggressionLevel);
        }

        tag.putInt("CyberNpcHunger", getHunger());
        tag.put("CyberNpcInventory", inventory.save());

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

        if (claimedCookingStation != null) {
            tag.putLong("CyberNpcClaimedCookingStation", claimedCookingStation.asLong());
        }

        corralBrain.addSaveData(tag);
        sleepBrain.addSaveData(tag);
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

        if (getNpcType() == NpcType.WILD) {
            if (tag.contains("CyberNpcWildClass")) {
                setWildClass(WildNpcClass.fromSerializedName(tag.getString("CyberNpcWildClass")));
            } else {
                setWildClass(WildNpcClass.randomSpawnClass(getRandom()));
            }

            if (!getWildClass().isAvailable()) {
                setWildClass(WildNpcClass.CLASSLESS);
            }

            if (tag.contains("CyberNpcPersonality")) {
                setPersonality(WildNpcPersonality.fromSerializedName(tag.getString("CyberNpcPersonality")));
            } else {
                setPersonality(WildNpcPersonality.randomPersonality(getRandom()));
            }
        } else {
            entityData.set(DATA_WILD_CLASS, "");
            entityData.set(DATA_PERSONALITY, "");
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
        entityData.set(DATA_AGGRESSION, aggressionLevel);

        setHunger(tag.contains("CyberNpcHunger") ? tag.getInt("CyberNpcHunger") : MAX_HUNGER);

        inventory.load(tag.getList("CyberNpcInventory", net.minecraft.nbt.Tag.TAG_COMPOUND));

        // One-time migration for worlds/NPCs created before v0.12.0.
        if (tag.contains("CyberNpcSword")) {
            addToInventory(ItemStack.of(tag.getCompound("CyberNpcSword")));
        }
        if (tag.contains("CyberNpcRangedWeapon")) {
            addToInventory(ItemStack.of(tag.getCompound("CyberNpcRangedWeapon")));
        }
        if (tag.contains("CyberNpcRawFood")) {
            addToInventory(ItemStack.of(tag.getCompound("CyberNpcRawFood")));
        }
        if (tag.contains("CyberNpcReadyFood")) {
            addToInventory(ItemStack.of(tag.getCompound("CyberNpcReadyFood")));
        }
        if (getStoredSword().isEmpty() && tag.contains("CyberNpcStoredWeapon")) {
            ItemStack oldWeapon = ItemStack.of(tag.getCompound("CyberNpcStoredWeapon"));
            if (oldWeapon.getItem() instanceof SwordItem) {
                addToInventory(oldWeapon);
            }
        }

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

        claimedCookingStation = tag.contains("CyberNpcClaimedCookingStation")
                ? BlockPos.of(tag.getLong("CyberNpcClaimedCookingStation"))
                : null;

        setCombatActive(false);
        setRangedState(RANGED_STATE_NONE);
        entityData.set(DATA_MELEE_SWING_TICKS, 0);
        huntingTarget = false;
        provocation = 0;
        outOfRangeTicks = 0;
        huntSearchCooldown = 0;
        dropSearchTicks = 0;
        lastHuntKillPos = null;
        cookingSearchCooldown = 0;
        eatingTicks = 0;
        utilityItemActive = false;
        emergencyEating = false;
        zombieConversionStarted = false;
        suspiciousNpc = null;
        infectionSuspicionCooldown = 0;
        infectionAvoidTicks = 0;
        groundFoodTargetId = null;
        groundFoodSearchCooldown = 0;
        foodChestTarget = null;
        foodChestSearchCooldown = 0;
        huntTargetRecheckCooldown = 0;
        threatScanCooldown = 0;
        fleeingThreat = null;
        fleeSafeTicks = 0;
        fleeRepathCooldown = 0;
        corralBrain.readSaveData(tag);
        sleepBrain.readSaveData(tag);

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

    private static final class ConditionalLookAtPlayerGoal extends LookAtPlayerGoal {
        private final CyberNpcEntity npc;

        private ConditionalLookAtPlayerGoal(CyberNpcEntity npc, float lookDistance) {
            super(npc, Player.class, lookDistance);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return !npc.isSleeping() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.isSleeping() && super.canContinueToUse();
        }
    }

    private static final class ConditionalRandomLookAroundGoal extends RandomLookAroundGoal {
        private final CyberNpcEntity npc;

        private ConditionalRandomLookAroundGoal(CyberNpcEntity npc) {
            super(npc);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return !npc.isSleeping() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.isSleeping() && super.canContinueToUse();
        }
    }

    private static final class WildNpcCombatGoal extends Goal {
        private static final double MELEE_ENGAGE_DISTANCE_SQR = 12.25D;
        private static final double MELEE_ATTACK_DISTANCE_SQR = 9.0D;
        private static final double MELEE_TOO_CLOSE_SQR = 4.84D;
        private static final int MELEE_ATTACK_COOLDOWN = 12;
        private static final int MELEE_RECOVERY_TICKS = 7;
        private static final double HUNT_STALK_MIN_SQR = 25.0D;
        private static final double HUNT_STALK_MAX_SQR = 196.0D;
        private static final double RANGED_STOP_DISTANCE_SQR = 100.0D;
        private static final double MAX_RANGED_DISTANCE_SQR = 576.0D;

        private final CyberNpcEntity npc;
        private int meleeCooldown;
        private int meleeRecoveryTicks;
        private int meleeStrafeDirection = 1;
        private int meleeStrafeTicks;
        private int rangedCooldown;
        private int mageCooldown;
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
            if (meleeRecoveryTicks > 0) {
                meleeRecoveryTicks--;
            }
            if (meleeStrafeTicks > 0) {
                meleeStrafeTicks--;
            } else {
                meleeStrafeTicks = 20 + npc.getRandom().nextInt(20);
                meleeStrafeDirection *= -1;
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

            switch (npc.getWildClass()) {
                case KNIGHT, ROGUE -> {
                    tickMelee(target, distanceSqr);
                    return;
                }
                case ARCHER -> {
                    if (distanceSqr <= 16.0D && !npc.getStoredSword().isEmpty()) {
                        tickMelee(target, distanceSqr);
                    } else {
                        tickRanged(target, distanceSqr, lineOfSight);
                    }
                    return;
                }
                case MAGE -> {
                    tickMage(target, distanceSqr, lineOfSight);
                    return;
                }
                default -> {
                    if (distanceSqr <= MELEE_ENGAGE_DISTANCE_SQR) {
                        tickMelee(target, distanceSqr);
                        return;
                    }

                    tickRanged(target, distanceSqr, lineOfSight);
                }
            }
        }

        private void tickMelee(LivingEntity target, double distanceSqr) {
            resetRangedUse();
            npc.setShiftKeyDown(false);
            npc.equipSword();

            WildNpcClass npcClass = npc.getWildClass();
            WildNpcPersonality personality = npc.getPersonality();

            int attackCooldown = switch (npcClass) {
                case ROGUE -> 9;
                case KNIGHT -> 13;
                default -> MELEE_ATTACK_COOLDOWN;
            };

            int recoveryTicks = switch (npcClass) {
                case ROGUE -> 4;
                case KNIGHT -> 6;
                default -> MELEE_RECOVERY_TICKS;
            };

            if (personality == WildNpcPersonality.AGGRESSIVE) {
                attackCooldown = Math.max(7, attackCooldown - 2);
                recoveryTicks = Math.max(3, recoveryTicks - 1);
            } else if (personality == WildNpcPersonality.CAUTIOUS) {
                attackCooldown += 2;
                recoveryTicks += 2;
            } else if (personality == WildNpcPersonality.TACTICAL) {
                recoveryTicks += 1;
            }

            double attackDistanceSqr = npcClass == WildNpcClass.ROGUE ? 8.41D : MELEE_ATTACK_DISTANCE_SQR;
            double tooCloseSqr = npcClass == WildNpcClass.KNIGHT ? 4.0D : MELEE_TOO_CLOSE_SQR;
            double approachSpeed = switch (npcClass) {
                case ROGUE -> 1.20D;
                case KNIGHT -> 1.02D;
                default -> 1.06D;
            };

            if (meleeRecoveryTicks > 0) {
                npc.setSprinting(false);
                moveForMeleeSpacing(target, true);
                return;
            }

            if (distanceSqr < tooCloseSqr) {
                npc.setSprinting(false);
                moveForMeleeSpacing(target, true);

                if (meleeCooldown <= 0) {
                    npc.startMeleeSwingAnimation();
                    npc.doHurtTarget(target);
                    meleeCooldown = attackCooldown;
                    meleeRecoveryTicks = recoveryTicks;
                }
                return;
            }

            if (distanceSqr > attackDistanceSqr) {
                boolean moving = npc.getNavigation().moveTo(target, approachSpeed);
                npc.setSprinting(moving
                        && distanceSqr > 10.24D
                        && npcClass != WildNpcClass.KNIGHT);
                return;
            }

            npc.setSprinting(false);

            if (meleeCooldown <= 0) {
                npc.startMeleeSwingAnimation();
                npc.doHurtTarget(target);
                meleeCooldown = attackCooldown;
                meleeRecoveryTicks = recoveryTicks;
                moveForMeleeSpacing(target, true);
            } else {
                moveForMeleeSpacing(target, false);
            }
        }

        private void moveForMeleeSpacing(LivingEntity target, boolean retreat) {
            Vec3 away = new Vec3(
                    npc.getX() - target.getX(),
                    0.0D,
                    npc.getZ() - target.getZ()
            );

            if (away.lengthSqr() < 0.001D) {
                Vec3 look = npc.getViewVector(1.0F);
                away = new Vec3(-look.x, 0.0D, -look.z);
            }

            if (away.lengthSqr() < 0.001D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }

            away = away.normalize();

            double sideStrength = npc.getWildClass() == WildNpcClass.ROGUE ? 1.25D : 0.75D;
            if (npc.getPersonality() == WildNpcPersonality.TACTICAL) {
                sideStrength += 0.35D;
            } else if (npc.getPersonality() == WildNpcPersonality.AGGRESSIVE) {
                sideStrength *= 0.75D;
            }

            Vec3 side = new Vec3(-away.z, 0.0D, away.x)
                    .scale(sideStrength * meleeStrafeDirection);

            double radius;
            if (npc.getWildClass() == WildNpcClass.KNIGHT) {
                radius = retreat ? 3.0D : 2.55D;
            } else if (npc.getWildClass() == WildNpcClass.ROGUE) {
                radius = retreat ? 4.2D : 3.1D;
            } else {
                radius = retreat ? 3.6D : 2.85D;
            }

            if (npc.getPersonality() == WildNpcPersonality.CAUTIOUS) {
                radius += 0.8D;
            } else if (npc.getPersonality() == WildNpcPersonality.AGGRESSIVE) {
                radius -= 0.45D;
            }

            Vec3 desired = target.position()
                    .add(away.scale(Math.max(2.2D, radius)))
                    .add(side);

            double speed = npc.getWildClass() == WildNpcClass.ROGUE
                    ? (retreat ? 1.20D : 0.96D)
                    : (retreat ? 1.08D : 0.82D);

            boolean moving = npc.getNavigation().moveTo(
                    desired.x,
                    npc.getY(),
                    desired.z,
                    speed
            );

            if (!moving && retreat) {
                Vec3 fallback = DefaultRandomPos.getPosAway(
                        npc,
                        8,
                        4,
                        target.position()
                );

                if (fallback != null) {
                    npc.getNavigation().moveTo(
                            fallback.x,
                            fallback.y,
                            fallback.z,
                            speed
                    );
                }
            }
        }

        private void moveForRangedSpacing(LivingEntity target, double radius) {
            Vec3 away = new Vec3(
                    npc.getX() - target.getX(),
                    0.0D,
                    npc.getZ() - target.getZ()
            );

            if (away.lengthSqr() < 0.001D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }

            away = away.normalize();
            Vec3 side = new Vec3(-away.z, 0.0D, away.x)
                    .scale(npc.getPersonality() == WildNpcPersonality.TACTICAL ? 1.5D : 0.8D);

            if (npc.getPersonality() == WildNpcPersonality.CAUTIOUS) {
                radius += 2.0D;
            } else if (npc.getPersonality() == WildNpcPersonality.AGGRESSIVE) {
                radius -= 1.5D;
            }

            Vec3 desired = target.position()
                    .add(away.scale(Math.max(6.0D, radius)))
                    .add(side);

            npc.getNavigation().moveTo(
                    desired.x,
                    npc.getY(),
                    desired.z,
                    npc.getWildClass() == WildNpcClass.ARCHER ? 1.05D : 0.95D
            );
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

            boolean archer = npc.getWildClass() == WildNpcClass.ARCHER;
            double preferredStopSqr = archer ? 144.0D : RANGED_STOP_DISTANCE_SQR;
            double tooCloseSqr = archer ? 64.0D : 0.0D;

            if (archer && distanceSqr < tooCloseSqr) {
                moveForRangedSpacing(target, 11.0D);
            } else if (distanceSqr > preferredStopSqr) {
                npc.getNavigation().moveTo(
                        target,
                        npc.huntingTarget ? 0.62D : (archer ? 1.00D : 0.92D)
                );
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

        private void tickMage(LivingEntity target, double distanceSqr, boolean lineOfSight) {
            resetRangedUse();
            npc.clearUtilityItem();
            npc.stowWeapons();
            npc.setSprinting(false);
            npc.setShiftKeyDown(false);

            if (!IronSpellsCompat.isLoaded()) {
                // Mage should never exist without Iron's Spells installed, but
                // fail safe to classless behavior rather than inventing magic.
                npc.setWildClass(WildNpcClass.CLASSLESS);
                tickMelee(target, distanceSqr);
                return;
            }

            if (mageCooldown > 0) {
                mageCooldown--;
            }

            if (!lineOfSight || distanceSqr > 324.0D) {
                npc.getNavigation().moveTo(target, 0.95D);
                return;
            }

            if (distanceSqr < 49.0D) {
                moveForRangedSpacing(target, 11.0D);
            } else if (distanceSqr > 196.0D) {
                npc.getNavigation().moveTo(target, 0.86D);
            } else if (npc.getPersonality() == WildNpcPersonality.TACTICAL) {
                moveForRangedSpacing(target, 11.5D);
            } else {
                npc.getNavigation().stop();
            }

            if (mageCooldown > 0) {
                return;
            }

            int spellLevel = npc.getPersonality() == WildNpcPersonality.AGGRESSIVE ? 2 : 1;
            IronSpellsCompat.CastResult result =
                    IronSpellsCompat.castAttackSpell(npc, target, spellLevel);

            if (result.success()) {
                mageCooldown = result.cooldownTicks();
            } else {
                // Never fall back to CyberNpc-created magic. If Iron's cannot
                // provide a valid spell, reposition and retry later.
                mageCooldown = 30;
                moveForRangedSpacing(target, 12.0D);
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
            rangedCooldown = npc.getWildClass() == WildNpcClass.ARCHER
                    ? (npc.getPersonality() == WildNpcPersonality.AGGRESSIVE ? 8 : 10)
                    : 12;
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
                rangedCooldown = npc.getWildClass() == WildNpcClass.ARCHER
                        ? (npc.getPersonality() == WildNpcPersonality.AGGRESSIVE ? 20 : 24)
                        : 28;
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
            meleeRecoveryTicks = 0;
            meleeStrafeTicks = 0;
            meleeStrafeDirection = 1;
            rangedCooldown = 0;
            mageCooldown = 0;
        }
    }
}
