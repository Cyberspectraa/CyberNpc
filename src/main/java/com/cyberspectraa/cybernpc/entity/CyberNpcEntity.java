package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.building.BuildingSavedData;
import com.cyberspectraa.cybernpc.compat.BetterHorsesCompat;
import com.cyberspectraa.cybernpc.compat.CyberClassesNpcCompat;
import com.cyberspectraa.cybernpc.compat.CyberProgressionCompat;
import com.cyberspectraa.cybernpc.compat.CyberRacesNpcCompat;
import com.cyberspectraa.cybernpc.compat.EpicKnightsCompat;
import com.cyberspectraa.cybernpc.compat.IronSpellsCompat;
import com.cyberspectraa.cybernpc.compat.TinkersConstructCompat;
import com.cyberspectraa.cybernpc.effect.ZombificationEffect;
import com.cyberspectraa.cybernpc.economy.BankSavedData;
import com.cyberspectraa.cybernpc.economy.CurrencyValue;
import com.cyberspectraa.cybernpc.registry.ModEffects;
import com.cyberspectraa.cybernpc.registry.ModEntities;
import com.cyberspectraa.cybernpc.registry.ModItems;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.cyberspectraa.cybernpc.service.SpecialNpcSavedData;
import com.cyberspectraa.cybernpc.world.CyberNpcWorldClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.enchantment.Enchantments;
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
import net.minecraft.world.entity.NeutralMob;
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
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class CyberNpcEntity extends PathfinderMob {
    public static final int RANGED_STATE_NONE = 0;
    public static final int RANGED_STATE_BOW_DRAW = 1;
    public static final int RANGED_STATE_CROSSBOW_CHARGE = 2;
    public static final int RANGED_STATE_CROSSBOW_HOLD = 3;

    public static final int SPELL_CAST_MODE_NONE = 0;
    public static final int SPELL_CAST_MODE_INSTANT = 1;
    public static final int SPELL_CAST_MODE_LONG = 2;

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
    private static final int THREAT_SCAN_INTERVAL = 4;
    private static final int COMBAT_TARGET_REFRESH_INTERVAL = 5;
    private static final double COMBAT_TARGET_SWITCH_ADVANTAGE_SQR = 1.0D;
    private static final int FLEE_SAFE_TICKS = 60;
    private static final double THREAT_SCAN_RADIUS = 20.0D;
    private static final double FLEE_RELEASE_DISTANCE = 30.0D;

    private static final int COMBAT_HELP_COOLDOWN_TICKS = 100;
    private static final int MAX_COMBAT_HELPERS = 3;
    private static final int PARTY_MAX_SIZE = 4;
    private static final int SOCIAL_SCAN_INTERVAL = 60;
    private static final double SOCIAL_RADIUS = 10.0D;
    private static final float SOCIAL_CONVERSATION_CHANCE = 0.34F;
    private static final int SOCIAL_CONVERSATION_COOLDOWN = 100;
    private static final float PARTY_INVITE_CHANCE = 0.38F;
    private static final int PARTY_INVITE_RESPONSE_TICKS = 28;
    private static final int PARTY_INVITE_COOLDOWN_TICKS = 500;
    private static final double NATURAL_SOCIAL_SEED_RADIUS = 16.0D;
    private static final float NATURAL_FRIEND_SEED_CHANCE = 0.55F;
    private static final float NATURAL_PARTY_SEED_CHANCE = 0.65F;
    private static final double PARTY_FOLLOW_DISTANCE = 8.0D;
    private static final double PARTY_FOLLOW_LIMIT = 40.0D;

    private static final double FRIEND_APPROACH_DISTANCE = 10.0D;
    private static final double FRIEND_APPROACH_LIMIT = 24.0D;
    private static final int FRIEND_SHARE_HUNGER_THRESHOLD = 6;

    private static final double HORSE_SEARCH_RADIUS = 18.0D;
    private static final int HORSE_SEARCH_INTERVAL = 200;
    private static final double HORSE_MOUNT_DISTANCE_SQR = 6.25D;
    private static final int HORSE_RIDE_MIN_TICKS = 600;
    private static final int HORSE_RIDE_RANDOM_TICKS = 600;
    private static final int HORSE_REMOUNT_COOLDOWN_TICKS = 600;
    private static final int HORSE_REPATH_INTERVAL = 40;
    private static final double HORSE_OWNER_RETURN_RADIUS_SQR = 24.0D * 24.0D;
    private static final double HORSE_NORMAL_NAV_SPEED = 1.12D;
    private static final double HORSE_PARTY_NAV_SPEED = 1.18D;
    private static final double HORSE_SPRINT_NAV_SPEED = 1.45D;
    private static final double HORSE_SPRINT_DISTANCE_SQR = 100.0D;

    private static final int RANGER_PATROL_MIN_RADIUS = 20;
    private static final int RANGER_PATROL_RANDOM_RADIUS = 14;
    private static final double RANGER_PATROL_REACHED_SQR = 36.0D;
    private static final double RANGER_HOME_RETURN_DISTANCE_SQR = 48.0D * 48.0D;
    private static final double RANGER_SLEEP_RETURN_DISTANCE_SQR = 12.0D * 12.0D;
    private static final int RANGER_PATROL_PAUSE_MIN_TICKS = 80;
    private static final int RANGER_PATROL_PAUSE_RANDOM_TICKS = 120;
    private static final double RANGER_LOCAL_NAV_STEP = 18.0D;
    private static final int RANGER_VILLAGE_CHECK_INTERVAL = 200;

    // Low-priority world scans are deliberately staggered between NPCs so a
    // naturally populated village does not make every resident perform the
    // same expensive query on the same server tick.
    private static final int BACKGROUND_CLAIM_SCAN_INTERVAL = 100;
    private static final int DEBUG_SYNC_INTERVAL = 20;

    public static final String NATURAL_SPAWN_TAG = "CyberNpcNaturalSpawn";
    public static final String CLASS_ADVANCEMENT_LOCK_TAG = "CyberNpcClassAdvancementLocked";

    private static final int CHAT_REACTION_COOLDOWN_TICKS = 40;

    private static final double FIGHT_CONFIDENCE = 55.0D;
    private static final double HELP_CONFIDENCE = 35.0D;
    private static final double FLEE_CONFIDENCE = 22.0D;

    // Conservative player-like gap limits requested for CyberNpc: one-block
    // gaps while walking and up to three blocks while sprint-jumping.
    private static final int WALK_GAP_JUMP_BLOCKS = 1;
    private static final int SPRINT_GAP_JUMP_BLOCKS = 3;
    private static final int GAP_JUMP_COOLDOWN_TICKS = 10;

    private static final int SCULK_SCAN_INTERVAL = 20;
    private static final int SCULK_STEALTH_HOLD_TICKS = 30;
    private static final int SCULK_SCAN_RADIUS = 16;

    private static final int WARDEN_SCAN_INTERVAL = 5;
    private static final double WARDEN_QUIET_AVOID_RADIUS = 22.0D;
    private static final double WARDEN_PANIC_RADIUS = 8.0D;
    private static final double QUIET_RETREAT_SPEED = 0.30D;

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

    private static final EntityDataAccessor<String> DATA_GEAR_TIER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_MAGE_SCHOOL =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_APPEARANCE_GENDER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> DATA_APPEARANCE_SKIN_TONE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_APPEARANCE_EYES =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_APPEARANCE_HAIR =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_SPELL_CAST_TICKS =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_SPELL_CAST_MODE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<String> DATA_CASTING_SPELL =
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

    private static final EntityDataAccessor<String> DATA_DEBUG_REASON =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_INTENTION =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_TARGET =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_PATH =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_CLAIMS =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_INVENTORY =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_CONFIDENCE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_SPELLS =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_PARTY =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DEBUG_RELATIONSHIPS =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_REACTION_ICON =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Long> DATA_REACTION_UNTIL =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.LONG);

    private int aggressionLevel = -1;
    private boolean classLoadoutInitialized;
    private String wildClassAdvancement = "";
    private int provocation;
    private int outOfRangeTicks;
    private int hungerDecayTimer;

    private final WildNpcInventory inventory = new WildNpcInventory();
    private final NpcSocialMemory socialMemory = new NpcSocialMemory();

    private int socialTickCooldown;
    private int socialConversationCooldown;
    private int socialConversationHoldTicks;
    private int partyInviteCooldown;
    private int pendingPartyInviteTicks;

    @Nullable
    private UUID socialConversationPartnerId;

    @Nullable
    private UUID pendingPartyInviteFrom;

    private boolean regroupingWithParty;
    private int chatReactionCooldown;
    private int racialAiControlTicks;
    private boolean naturalSocialSeedPending;
    private int naturalSocialSeedWaitTicks;

    @Nullable
    private UUID horseTargetId;

    @Nullable
    private BlockPos horseRideOrigin;

    private int horseSearchCooldown;
    // This is now a minimum ride commitment, not a timer that forces a
    // meaningless dismount when it expires.
    private int horseRideTicks;
    private int horseRemountCooldown;
    private int horseRepathCooldown;

    @Nullable
    private BlockPos rangerHomePos;

    @Nullable
    private BlockPos rangerPatrolTarget;

    private double rangerPatrolAngle = Double.NaN;
    private int rangerPatrolPauseTicks;
    private int rangerVillageCheckCooldown;
    private boolean rangerHomeIsVillage;
    private boolean rangerReturningHomeForSleep;
    private int backgroundClaimScanCooldown;

    private boolean chainmailMigrationChecked;

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
    private boolean suppressDeathLootForZombieConversion;

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
    private int gapJumpCooldown;
    private int sculkScanCooldown;
    private int sculkStealthTicks;
    private boolean sculkSneaking;
    private int wardenScanCooldown;

    @Nullable
    private Warden nearbyWarden;

    @Nullable
    private BlockPos foodChestTarget;
    private int foodChestSearchCooldown;

    private final WildNpcCorralBrain corralBrain;
    private final WildNpcSleepBrain sleepBrain;
    private final NpcPlayerInteractionController playerInteractions;
    private final NpcIntentionController intentions;
    private final NpcAttentionBrain attentionBrain;
    private final NpcEnvironmentalReactionBrain environmentBrain;
    private final NpcVocalizationController vocalizations;
    private final NpcServiceBrain serviceBrain;
    private final NpcBuildingBrain buildingBrain;
    private final NpcFenceAvoidanceBrain fenceAvoidanceBrain;

    @Nullable
    private UUID specialNpcId;

    public CyberNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        corralBrain = new WildNpcCorralBrain(this);
        sleepBrain = new WildNpcSleepBrain(this);
        playerInteractions = new NpcPlayerInteractionController(this);
        intentions = new NpcIntentionController(this);
        attentionBrain = new NpcAttentionBrain(this, playerInteractions, intentions);
        environmentBrain = new NpcEnvironmentalReactionBrain(this, intentions);
        vocalizations = new NpcVocalizationController(this);
        serviceBrain = new NpcServiceBrain(this);
        buildingBrain = new NpcBuildingBrain(this);
        fenceAvoidanceBrain = new NpcFenceAvoidanceBrain(this);

        getNavigation().setCanFloat(true);
        if (getNavigation() instanceof GroundPathNavigation groundNavigation) {
            groundNavigation.setCanOpenDoors(true);
            groundNavigation.setCanPassDoors(true);
        }

        // Explicitly make fences non-traversable for CyberNpc even when
        // another movement behaviour is trying to reach something beyond one.
        setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
        setPathfindingMalus(BlockPathTypes.TRAPDOOR, -1.0F);
        setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        setPathfindingMalus(BlockPathTypes.WATER, 1.0F);
        setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.5F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0F);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 10.0F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 12.0F);
        setPathfindingMalus(BlockPathTypes.DANGER_OTHER, 8.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.425D)
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
        entityData.define(DATA_GEAR_TIER, "");
        entityData.define(DATA_MAGE_SCHOOL, "");
        entityData.define(DATA_APPEARANCE_GENDER, "");
        entityData.define(DATA_APPEARANCE_SKIN_TONE, -1);
        entityData.define(DATA_APPEARANCE_EYES, -1);
        entityData.define(DATA_APPEARANCE_HAIR, -1);
        entityData.define(DATA_SPELL_CAST_TICKS, 0);
        entityData.define(DATA_SPELL_CAST_MODE, SPELL_CAST_MODE_NONE);
        entityData.define(DATA_CASTING_SPELL, "");
        entityData.define(DATA_COMBAT_ACTIVE, false);
        entityData.define(DATA_HUNGER, MAX_HUNGER);
        entityData.define(DATA_RANGED_STATE, RANGED_STATE_NONE);
        entityData.define(DATA_MELEE_SWING_TICKS, 0);
        entityData.define(DATA_AGGRESSION, -1);
        entityData.define(DATA_DEBUG_ACTIVITY, "Idle");
        entityData.define(DATA_DEBUG_REASON, "No higher-priority need");
        entityData.define(DATA_DEBUG_INTENTION, "none");
        entityData.define(DATA_DEBUG_TARGET, "none");
        entityData.define(DATA_DEBUG_PATH, "none");
        entityData.define(DATA_DEBUG_CLAIMS, "none");
        entityData.define(DATA_DEBUG_INVENTORY, "empty");
        entityData.define(DATA_DEBUG_CONFIDENCE, "none");
        entityData.define(DATA_DEBUG_SPELLS, "Not a Mage");
        entityData.define(DATA_DEBUG_PARTY, "none");
        entityData.define(DATA_DEBUG_RELATIONSHIPS, "none");
        entityData.define(DATA_REACTION_ICON, NpcReactionIcon.NONE.name());
        entityData.define(DATA_REACTION_UNTIL, 0L);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BuildingAwareOpenDoorGoal(this, true));
        goalSelector.addGoal(2, new WildNpcCombatGoal(this));
        goalSelector.addGoal(5, new ConditionalRandomStrollGoal(this, 1.0D, 120));
        goalSelector.addGoal(6, new ConditionalLookAtPlayerGoal(this, 8.0F));
        goalSelector.addGoal(7, new ConditionalRandomLookAroundGoal(this));
    }

    public String getRole() {
        return entityData.get(DATA_ROLE);
    }

    @Nullable
    public UUID getSpecialNpcId() {
        return specialNpcId;
    }

    public void setSpecialNpcId(@Nullable UUID specialNpcId) {
        this.specialNpcId = specialNpcId;
    }

    public boolean isSpecialServiceNpc() {
        return NpcServiceRole.fromRole(getRole())
                != NpcServiceRole.NONE;
    }

    public void setRole(String role) {
        NpcServiceRole previousRole =
                NpcServiceRole.fromRole(entityData.get(DATA_ROLE));

        String cleaned = role == null ? "" : role.trim();
        String resolved = cleaned.isEmpty() ? "Citizen" : cleaned;
        NpcServiceRole serviceRole = NpcServiceRole.fromRole(resolved);

        if (!level().isClientSide
                && previousRole != NpcServiceRole.NONE
                && previousRole != serviceRole) {
            serviceBrain.release();
        }

        entityData.set(DATA_ROLE, resolved);

        if (getNavigation() instanceof GroundPathNavigation groundNavigation) {
            boolean guard = serviceRole == NpcServiceRole.GUARD;
            groundNavigation.setCanOpenDoors(!guard);
            groundNavigation.setCanPassDoors(!guard);
        }

        if (serviceRole != NpcServiceRole.NONE) {
            setCanWander(false);
            setPersistenceRequired();

            if (serviceRole == NpcServiceRole.GUARD) {
                ensureGuardEquipment();
            }

            if (getCustomName() == null) {
                setCustomName(Component.literal(serviceRole.displayName()));
                setCustomNameVisible(true);
            }
        }
    }

    InteractionResult useServiceBlock(BlockPos pos) {
        return playerInteractions.rightClickBlock(pos);
    }

    void receiveGuardAlert(
            @Nullable LivingEntity threat,
            BlockPos bellPos
    ) {
        serviceBrain.receiveGuardAlert(threat, bellPos);
    }

    void ensureGuardEquipment() {
        if (NpcServiceRole.fromRole(getRole()) != NpcServiceRole.GUARD) {
            return;
        }

        EpicKnightsCompat.GuardGear epic =
                EpicKnightsCompat.createGuardGear();

        if (epic.complete()) {
            if (getMainHandItem().isEmpty()
                    || getMainHandItem().is(Items.IRON_SWORD)) {
                setItemSlot(
                        EquipmentSlot.MAINHAND,
                        epic.weapon().copy()
                );
            }

            if (getOffhandItem().isEmpty()
                    || getOffhandItem().is(Items.SHIELD)) {
                setItemSlot(
                        EquipmentSlot.OFFHAND,
                        epic.shield().copy()
                );
            }

            if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                    || getItemBySlot(EquipmentSlot.HEAD)
                    .is(Items.IRON_HELMET)) {
                setItemSlot(
                        EquipmentSlot.HEAD,
                        epic.helmet().copy()
                );
            }
            if (getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    || getItemBySlot(EquipmentSlot.CHEST)
                    .is(Items.IRON_CHESTPLATE)) {
                setItemSlot(
                        EquipmentSlot.CHEST,
                        epic.chest().copy()
                );
            }
            if (getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                    || getItemBySlot(EquipmentSlot.LEGS)
                    .is(Items.IRON_LEGGINGS)) {
                setItemSlot(
                        EquipmentSlot.LEGS,
                        epic.legs().copy()
                );
            }
            if (getItemBySlot(EquipmentSlot.FEET).isEmpty()
                    || getItemBySlot(EquipmentSlot.FEET)
                    .is(Items.IRON_BOOTS)) {
                setItemSlot(
                        EquipmentSlot.FEET,
                        epic.boots().copy()
                );
            }
        } else {
            if (getMainHandItem().isEmpty()) {
                setItemSlot(
                        EquipmentSlot.MAINHAND,
                        new ItemStack(Items.IRON_SWORD)
                );
            }

            if (getOffhandItem().isEmpty()) {
                setItemSlot(
                        EquipmentSlot.OFFHAND,
                        new ItemStack(Items.SHIELD)
                );
            }

            if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                setItemSlot(
                        EquipmentSlot.HEAD,
                        new ItemStack(Items.IRON_HELMET)
                );
            }
            if (getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                setItemSlot(
                        EquipmentSlot.CHEST,
                        new ItemStack(Items.IRON_CHESTPLATE)
                );
            }
            if (getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
                setItemSlot(
                        EquipmentSlot.LEGS,
                        new ItemStack(Items.IRON_LEGGINGS)
                );
            }
            if (getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
                setItemSlot(
                        EquipmentSlot.FEET,
                        new ItemStack(Items.IRON_BOOTS)
                );
            }
        }

        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getBaseValue() < 24.0D) {
            maxHealth.setBaseValue(24.0D);
        }

        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && attack.getBaseValue() < 3.0D) {
            attack.setBaseValue(3.0D);
        }
    }

    public NpcType getNpcType() {
        return NpcType.fromSerializedName(entityData.get(DATA_NPC_TYPE));
    }

    public void setNpcType(NpcType npcType) {
        NpcType safeType = npcType == null ? NpcType.MAIN : npcType;
        entityData.set(DATA_NPC_TYPE, safeType.serializedName());

        if (safeType != NpcType.WILD) {
            entityData.set(DATA_WILD_CLASS, "");
            wildClassAdvancement = "";
            entityData.set(DATA_PERSONALITY, "");
            entityData.set(DATA_GEAR_TIER, "");
            entityData.set(DATA_MAGE_SCHOOL, "");
            entityData.set(DATA_SPELL_CAST_TICKS, 0);
            entityData.set(DATA_SPELL_CAST_MODE, SPELL_CAST_MODE_NONE);
            entityData.set(DATA_CASTING_SPELL, "");
            entityData.set(DATA_REACTION_ICON, NpcReactionIcon.NONE.name());
            entityData.set(DATA_REACTION_UNTIL, 0L);
            setPersistenceRequired();
            sleepBrain.interrupt();
            calmWildNpc();
        }
    }

    public String getWildClass() {
        return CyberClassesNpcCompat.normalize(
                entityData.get(DATA_WILD_CLASS)
        );
    }

    public String getWildClassDisplayName() {
        return CyberClassesNpcCompat.effectiveDisplayName(
                getWildClass(),
                getWildClassAdvancement()
        );
    }

    public String getWildClassAdvancement() {
        return CyberClassesNpcCompat.normalizeAdvancement(
                getWildClass(),
                wildClassAdvancement
        );
    }

    public String getWildClassAdvancementDisplayName() {
        return CyberClassesNpcCompat.advancementDisplayName(
                getWildClassAdvancement()
        );
    }

    private void setWildClass(String wildClass) {
        String normalized =
                CyberClassesNpcCompat.normalize(wildClass);

        if (!normalized.equals(getWildClass())) {
            wildClassAdvancement = "";
        }

        entityData.set(
                DATA_WILD_CLASS,
                normalized
        );
    }

    private void setWildClassAdvancement(String advancementId) {
        wildClassAdvancement =
                CyberClassesNpcCompat.normalizeAdvancement(
                        getWildClass(),
                        advancementId
                );
    }

    public void ensureWildClassAdvancement() {
        if (getNpcType() != NpcType.WILD
                || getPersistentData().getBoolean(
                    CLASS_ADVANCEMENT_LOCK_TAG
                )
                || !getWildClassAdvancement().isBlank()) {
            return;
        }

        int level = CyberProgressionCompat.getLevel(this);

        if (level < 20) {
            return;
        }

        setWildClassAdvancement(
                CyberClassesNpcCompat.randomAdvancementId(
                        getWildClass(),
                        level,
                        getRandom()
                )
        );

        if (!getWildClassAdvancement().isBlank()) {
            // The new path can change armour/magic/loadout rules (for
            // example Arcane Archer), so rebuild once using the shared
            // CyberClasses definition on the next normal Wild profile tick.
            classLoadoutInitialized = false;
        }
    }

    private boolean isWildClass(String classId) {
        return getWildClass().equals(classId);
    }

    private boolean wildClassAvailable() {
        return CyberClassesNpcCompat.isAvailable(
                getWildClass()
        );
    }

    private boolean wildClassUsesSpellBook() {
        return CyberClassesNpcCompat.usesSpellBook(
                getWildClass(),
                getWildClassAdvancement()
        );
    }

    private boolean wildClassHasMagicSchool() {
        return CyberClassesNpcCompat.hasMagicSchool(
                getWildClass(),
                getWildClassAdvancement()
        );
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

    public WildNpcGearTier getGearTier() {
        return WildNpcGearTier.fromSerializedName(entityData.get(DATA_GEAR_TIER));
    }

    public String getGearTierDisplayName() {
        return getGearTier().displayName();
    }

    private void setGearTier(WildNpcGearTier tier) {
        WildNpcGearTier safe = tier == null ? WildNpcGearTier.STANDARD : tier;
        entityData.set(DATA_GEAR_TIER, safe.serializedName());
    }

    public MageSchool getMageSchool() {
        return MageSchool.fromSerializedName(entityData.get(DATA_MAGE_SCHOOL));
    }

    public String getMageSchoolDisplayName() {
        return wildClassHasMagicSchool()
                ? getMageSchool().displayName()
                : "None";
    }

    private void setMageSchool(MageSchool school) {
        entityData.set(
                DATA_MAGE_SCHOOL,
                school == null ? "" : school.serializedName()
        );
    }

    public NpcAppearance.Gender getAppearanceGender() {
        return NpcAppearance.Gender.fromSerializedName(
                entityData.get(DATA_APPEARANCE_GENDER)
        );
    }

    public boolean isSlimModel() {
        if ("mason".equalsIgnoreCase(
                getPersistentData().getString("CyberQuestNpcId")
        )) {
            return false;
        }

        if (NpcServiceRole.fromRole(getRole()) == NpcServiceRole.COURIER) {
            return false;
        }

        return getAppearanceGender().slim();
    }

    public int getSkinToneIndex() {
        return NpcAppearance.sanitizeSkinTone(
                entityData.get(DATA_APPEARANCE_SKIN_TONE)
        );
    }

    public int getEyeStyleIndex() {
        return NpcAppearance.sanitizeEyeStyle(
                entityData.get(DATA_APPEARANCE_EYES)
        );
    }

    public int getHairStyleIndex() {
        return NpcAppearance.sanitizeHairStyle(
                entityData.get(DATA_APPEARANCE_HAIR)
        );
    }

    public String getSkinToneDisplayName() {
        return NpcAppearance.skinToneDisplayName(getSkinToneIndex());
    }

    public String getEyeStyleDisplayName() {
        return NpcAppearance.eyeStyleDisplayName(getEyeStyleIndex());
    }

    public String getHairStyleDisplayName() {
        return NpcAppearance.hairStyleDisplayName(getHairStyleIndex());
    }

    private void ensureAppearance() {
        if (entityData.get(DATA_APPEARANCE_GENDER).isBlank()) {
            entityData.set(
                    DATA_APPEARANCE_GENDER,
                    NpcAppearance.Gender.random(getRandom()).serializedName()
            );
        }

        if (entityData.get(DATA_APPEARANCE_SKIN_TONE) < 0) {
            entityData.set(
                    DATA_APPEARANCE_SKIN_TONE,
                    getRandom().nextInt(NpcAppearance.skinToneCount())
            );
        }

        if (entityData.get(DATA_APPEARANCE_EYES) < 0) {
            entityData.set(
                    DATA_APPEARANCE_EYES,
                    getRandom().nextInt(NpcAppearance.eyeStyleCount())
            );
        }

        if (entityData.get(DATA_APPEARANCE_HAIR) < 0) {
            entityData.set(
                    DATA_APPEARANCE_HAIR,
                    getRandom().nextInt(NpcAppearance.hairStyleCount())
            );
        }
    }

    ListTag saveInventoryForZombieConversion() {
        return inventory.save();
    }

    ItemStack copyActiveFoodForZombieConversion() {
        return foodToEat.copy();
    }

    public boolean isSpellCastingVisual() {
        return entityData.get(DATA_SPELL_CAST_TICKS) > 0;
    }

    public int getSpellCastingVisualMode() {
        return entityData.get(DATA_SPELL_CAST_MODE);
    }

    public String getCastingSpellId() {
        return entityData.get(DATA_CASTING_SPELL);
    }

    public String getDebugParty() {
        return entityData.get(DATA_DEBUG_PARTY);
    }

    public String getDebugRelationships() {
        return entityData.get(DATA_DEBUG_RELATIONSHIPS);
    }

    public String getDebugRangerState() {
        if (!isWildClass("ranger")) {
            return "not ranger";
        }

        if (rangerReturningHomeForSleep) {
            return "returning home";
        }

        if (getVehicle() instanceof AbstractHorse) {
            return rangerPatrolPauseTicks > 0
                    ? "watching perimeter"
                    : "patrolling perimeter";
        }

        return horseTargetId != null
                ? "going to horse"
                : "local patrol";
    }

    public int getPlayerReputation(UUID playerId) {
        return socialMemory.getPlayerReputation(playerId);
    }

    public boolean isTrackingThreat(LivingEntity entity) {
        return entity != null
                && (getTarget() == entity || fleeingThreat == entity);
    }

    public void recordPlayerCombatHelp(
            Player player,
            LivingEntity threat,
            boolean killed
    ) {
        if (level().isClientSide
                || getNpcType() != NpcType.WILD
                || player == null
                || threat == null
                || !isTrackingThreat(threat)) {
            return;
        }

        socialMemory.adjustPlayerReputation(
                player.getUUID(),
                killed ? 6 : 1
        );

        if (killed) {
            showReaction(NpcReactionIcon.HAPPY, 50);
        } else if (getRandom().nextFloat() < 0.18F) {
            showReaction(NpcReactionIcon.FRIENDLY, 35);
        }
    }

    public boolean isFriendWith(CyberNpcEntity other) {
        return other != null
                && other != this
                && socialMemory.isFriend(other.getUUID());
    }

    public void reactToPlayerChat(
            Player player,
            NpcChatIntent intent,
            @Nullable CyberNpcEntity subject
    ) {
        if (level().isClientSide
                || getNpcType() != NpcType.WILD
                || player == null
                || intent == null
                || chatReactionCooldown > 0) {
            return;
        }

        boolean direct = subject == null || subject == this;
        boolean defendingFriend = subject != null
                && subject != this
                && isFriendWith(subject);

        if (!direct && !defendingFriend) {
            return;
        }

        chatReactionCooldown = CHAT_REACTION_COOLDOWN_TICKS;

        switch (intent) {
            case POSITIVE -> {
                int gain = direct ? 5 : 2;

                if (direct
                        && CyberRacesNpcCompat.socialAffinity(this, player) >= 3) {
                    gain++;
                }

                socialMemory.adjustPlayerReputation(
                        player.getUUID(),
                        gain
                );

                showReaction(
                        direct
                                ? NpcReactionIcon.FRIENDLY
                                : NpcReactionIcon.HAPPY,
                        55
                );
            }
            case NEGATIVE -> {
                int loss = direct ? -10 : -4;
                socialMemory.adjustPlayerReputation(
                        player.getUUID(),
                        loss
                );

                showReaction(
                        direct
                                ? (getRandom().nextBoolean()
                                ? NpcReactionIcon.SAD
                                : NpcReactionIcon.ANNOYED)
                                : NpcReactionIcon.ANNOYED,
                        65
                );
            }
            case THREAT -> {
                int loss = direct ? -20 : -9;
                socialMemory.adjustPlayerReputation(
                        player.getUUID(),
                        loss
                );

                double retaliationChance = chatThreatRetaliationChance(
                        player,
                        direct,
                        defendingFriend
                );

                if (getRandom().nextDouble() < retaliationChance) {
                    showReaction(NpcReactionIcon.ANGRY, 70);
                    huntingTarget = false;
                    dropSearchTicks = 0;
                    cookingTarget = null;
                    beginWildCombat(player, true, false);
                } else {
                    showReaction(
                            getPersonality() == WildNpcPersonality.SKITTISH
                                    || getPersonality() == WildNpcPersonality.CAUTIOUS
                                    ? NpcReactionIcon.SCARED
                                    : NpcReactionIcon.DANGER,
                            70
                    );
                }
            }
        }
    }

    private double chatThreatRetaliationChance(
            Player player,
            boolean direct,
            boolean defendingFriend
    ) {
        double chance = 0.16D + getAggressionLevel() / 180.0D;

        chance += switch (getPersonality()) {
            case AGGRESSIVE -> 0.26D;
            case RECKLESS -> 0.22D;
            case BRAVE -> 0.14D;
            case STUBBORN -> 0.10D;
            case PROTECTIVE, LOYAL -> defendingFriend ? 0.24D : 0.08D;
            case SKITTISH -> -0.22D;
            case CAUTIOUS -> -0.14D;
            case PATIENT -> -0.08D;
            default -> 0.0D;
        };

        int reputation = socialMemory.getPlayerReputation(
                player.getUUID()
        );

        if (reputation <= -25) {
            chance += 0.18D;
        } else if (reputation >= 25) {
            chance -= 0.12D;
        }

        if (!direct && !defendingFriend) {
            chance = 0.0D;
        }

        return Mth.clamp(chance, 0.05D, 0.85D);
    }

    public NpcReactionIcon getReactionIcon() {
        if (getNpcType() != NpcType.WILD
                || level().getGameTime() >= entityData.get(DATA_REACTION_UNTIL)) {
            return NpcReactionIcon.NONE;
        }

        return NpcReactionIcon.fromSerializedName(
                entityData.get(DATA_REACTION_ICON)
        );
    }

    public boolean hasActiveReaction() {
        return getReactionIcon() != NpcReactionIcon.NONE;
    }

    public void showReaction(
            NpcReactionIcon icon,
            int durationTicks
    ) {
        if (level().isClientSide
                || getNpcType() != NpcType.WILD
                || icon == null
                || icon == NpcReactionIcon.NONE
                || durationTicks <= 0) {
            return;
        }

        NpcReactionIcon current = getReactionIcon();
        long currentUntil = entityData.get(DATA_REACTION_UNTIL);

        if (current != NpcReactionIcon.NONE
                && current.priority() > icon.priority()
                && level().getGameTime() + 10L < currentUntil) {
            return;
        }

        entityData.set(DATA_REACTION_ICON, icon.name());
        entityData.set(
                DATA_REACTION_UNTIL,
                level().getGameTime() + durationTicks
        );
        vocalizations.react(icon);
    }

    public boolean canWander() {
        return entityData.get(DATA_CAN_WANDER);
    }

    private boolean canUseBuildingDoor(BlockPos pos) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return true;
        }

        return BuildingSavedData.get(serverLevel)
                .canUseEntrance(serverLevel, this, pos);
    }

    private boolean isBuildingDestinationAllowed(BlockPos pos) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return true;
        }

        return BuildingSavedData.get(serverLevel)
                .canStandAt(serverLevel, this, pos);
    }

    private boolean isCurrentWanderPathBuildingAllowed() {
        Path path = getNavigation().getPath();
        if (path == null || path.isDone()) {
            return true;
        }

        for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
            var node = path.getNode(i);
            BlockPos nodePos = new BlockPos(node.x, node.y, node.z);

            if (!isBuildingDestinationAllowed(nodePos)) {
                return false;
            }
        }

        return true;
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

    public String getDebugReason() {
        return entityData.get(DATA_DEBUG_REASON);
    }

    public String getDebugIntention() {
        return entityData.get(DATA_DEBUG_INTENTION);
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

    public String getDebugConfidence() {
        return entityData.get(DATA_DEBUG_CONFIDENCE);
    }

    public String getDebugSpells() {
        return entityData.get(DATA_DEBUG_SPELLS);
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

        // Appearance is persistent and must be chosen before the name so new
        // Wild NPCs always receive a name from the matching gender pool.
        ensureAppearance();

        String name = switch (getNpcType()) {
            case MAIN -> "Main NPC";
            case QUEST -> "Quest NPC";
            case WILD -> CyberNpcNameGenerator.randomWildName(
                    getRandom(),
                    getAppearanceGender()
            );
        };

        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
    }

    private void ensureWildProfile() {
        if (getNpcType() != NpcType.WILD) {
            return;
        }

        if (entityData.get(DATA_WILD_CLASS).isBlank()) {
            setWildClass(CyberClassesNpcCompat.randomClassId(getRandom()));
        }

        if (entityData.get(DATA_PERSONALITY).isBlank()) {
            setPersonality(WildNpcPersonality.randomPersonality(getRandom()));
        }

        if (!wildClassAvailable()) {
            setWildClass("classless");
            setMageSchool(null);
            classLoadoutInitialized = false;
        }

        if (entityData.get(DATA_GEAR_TIER).isBlank()) {
            setGearTier(WildNpcGearTier.randomTier(getRandom()));
        }

        if (wildClassHasMagicSchool()) {
            if (isWildClass("cleric") || isWildClass("bard")) {
                setMageSchool(MageSchool.HOLY);
            } else if (isWildClass("druid")) {
                setMageSchool(MageSchool.NATURE);
            } else if (entityData.get(DATA_MAGE_SCHOOL).isBlank()) {
                setMageSchool(MageSchool.randomSchool(getRandom()));
            }
        } else {
            setMageSchool(null);
        }

        if (!classLoadoutInitialized) {
            initializeClassLoadout();
            classLoadoutInitialized = true;
        }

        applyClassAttributes();
        replaceLegacyChainmailArmor();

        if (aggressionLevel < 0) {
            aggressionLevel = WILD_MIN_AGGRESSION
                    + getRandom().nextInt(WILD_MAX_AGGRESSION - WILD_MIN_AGGRESSION + 1);

            aggressionLevel = Mth.clamp(
                    aggressionLevel + getPersonality().aggressionModifier(),
                    WILD_MIN_AGGRESSION,
                    WILD_MAX_AGGRESSION
            );

            entityData.set(DATA_AGGRESSION, aggressionLevel);
        } else if (entityData.get(DATA_AGGRESSION) != aggressionLevel) {
            entityData.set(DATA_AGGRESSION, aggressionLevel);
        }

        if (isWildClass("classless")) {
            if (getStoredSword().isEmpty()) {
                inventory.add(CyberNpcWeaponPool.randomWildSword(getRandom()));
            }
            if (getStoredRangedWeapon().isEmpty()) {
                inventory.add(CyberNpcWeaponPool.randomWildRangedWeapon(getRandom()));
            }
        }

        if (!isCombatActive()
                && !utilityItemActive
                && !isSpellCastingVisual()
                && !getMainHandItem().isEmpty()) {
            stowWeapons();
        }
    }

    private void initializeClassLoadout() {
        if (isWildClass("classless")) {
            initializeClasslessLoadout();
            return;
        }

        inventory.removeMatching(stack ->
                isMeleeWeaponStack(stack)
                        || isRangedWeaponStack(stack)
                        || isMageSpellBook(stack)
        );

        clearArmorSlots();

        switch (getWildClass()) {
            case "archer" -> initializeArcherLoadout();
            case "knight" -> initializeKnightLoadout();
            case "rogue" -> initializeRogueLoadout();
            case "berserker" -> initializeBerserkerLoadout();
            case "ranger" -> initializeRangerLoadout();
            case "mage" -> initializeMageLoadout();
            case "cleric" -> initializeClericLoadout();
            case "spellblade" -> initializeSpellbladeLoadout();
            case "monk" -> initializeMonkLoadout();
            case "druid" -> initializeDruidLoadout();
            case "bard" -> initializeBardLoadout();
            case "alchemist" -> initializeAlchemistLoadout();
            default -> initializeClasslessLoadout();
        }

        applyClassAdvancementLoadout();
    }

    private void initializeClasslessLoadout() {
        // Classless stays the ordinary/high-frequency NPC, but it still owns
        // visible equipment. Rare gear tiers remain uncommon and provide the
        // occasional tougher "normal" NPC without turning it into a class.
        WildNpcGearTier tier = getGearTier();

        if (tier != WildNpcGearTier.STANDARD) {
            inventory.removeMatching(stack ->
                    isSwordStack(stack) || isRangedWeaponStack(stack)
            );

            ItemStack sword = switch (tier) {
                case FINE -> new ItemStack(Items.IRON_SWORD);
                case RARE -> new ItemStack(Items.DIAMOND_SWORD);
                case ELITE -> new ItemStack(Items.NETHERITE_SWORD);
                default -> CyberNpcWeaponPool.randomWildSword(getRandom());
            };

            ItemStack ranged = getRandom().nextFloat() < 0.30F
                    ? new ItemStack(Items.CROSSBOW)
                    : new ItemStack(Items.BOW);

            sword = maybeTinkersSword(sword);
        applyWeaponEnchantments(sword, tier);
            applyWeaponEnchantments(ranged, tier);
            inventory.add(sword);
            inventory.add(ranged);
        }

        switch (tier) {
            case STANDARD -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case FINE -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case RARE -> equipArmorSet(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS
            );
            case ELITE -> equipArmorSet(
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS
            );
        }
    }

    private void initializeArcherLoadout() {
        WildNpcGearTier tier = getGearTier();

        ItemStack sword = switch (tier) {
            case ELITE, RARE -> new ItemStack(Items.DIAMOND_SWORD);
            case FINE -> new ItemStack(Items.IRON_SWORD);
            default -> new ItemStack(Items.STONE_SWORD);
        };

        ItemStack ranged = getRandom().nextFloat() < 0.35F
                ? new ItemStack(Items.CROSSBOW)
                : new ItemStack(Items.BOW);

        sword = maybeTinkersSword(sword);
        applyWeaponEnchantments(sword, tier);
        applyWeaponEnchantments(ranged, tier);
        inventory.add(sword);
        inventory.add(ranged);

        switch (tier) {
            case STANDARD -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case FINE -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case RARE -> equipArmorSet(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS
            );
            case ELITE -> equipArmorSet(
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS
            );
        }
    }

    private void initializeKnightLoadout() {
        WildNpcGearTier tier = getGearTier();

        ItemStack sword = switch (tier) {
            case ELITE -> new ItemStack(Items.NETHERITE_SWORD);
            case RARE -> new ItemStack(Items.DIAMOND_SWORD);
            default -> new ItemStack(Items.IRON_SWORD);
        };

        sword = maybeTinkersSword(sword);
        applyWeaponEnchantments(sword, tier);
        inventory.add(sword);

        switch (tier) {
            case STANDARD -> equipArmorSet(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS
            );
            case FINE -> equipArmorSet(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS
            );
            case RARE -> equipArmorSet(
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS
            );
            case ELITE -> equipArmorSet(
                    Items.NETHERITE_HELMET,
                    Items.NETHERITE_CHESTPLATE,
                    Items.NETHERITE_LEGGINGS,
                    Items.NETHERITE_BOOTS
            );
        }
    }

    private void initializeRogueLoadout() {
        WildNpcGearTier tier = getGearTier();

        ItemStack sword = switch (tier) {
            case ELITE -> new ItemStack(Items.DIAMOND_SWORD);
            case RARE, FINE -> new ItemStack(Items.IRON_SWORD);
            default -> new ItemStack(Items.STONE_SWORD);
        };

        sword = maybeTinkersSword(sword);
        applyWeaponEnchantments(sword, tier);
        inventory.add(sword);

        switch (tier) {
            case STANDARD -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case FINE -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case RARE -> equipArmorSet(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS
            );
            case ELITE -> equipArmorSet(
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS
            );
        }
    }

    private void initializeBerserkerLoadout() {
        WildNpcGearTier tier = getGearTier();

        ItemStack axe = switch (tier) {
            case STANDARD -> new ItemStack(Items.STONE_AXE);
            case FINE -> new ItemStack(Items.IRON_AXE);
            case RARE -> new ItemStack(Items.DIAMOND_AXE);
            case ELITE -> new ItemStack(Items.NETHERITE_AXE);
        };

        applyWeaponEnchantments(axe, tier);
        inventory.add(axe);

        // Berserkers trade protection for mobility and damage.
        switch (tier) {
            case STANDARD -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case FINE -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case RARE -> equipArmorSet(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS
            );
            case ELITE -> equipArmorSet(
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS
            );
        }
    }

    private void initializeRangerLoadout() {
        WildNpcGearTier tier = getGearTier();

        ItemStack sword = switch (tier) {
            case STANDARD -> new ItemStack(Items.STONE_SWORD);
            case FINE -> new ItemStack(Items.IRON_SWORD);
            case RARE -> new ItemStack(Items.DIAMOND_SWORD);
            case ELITE -> new ItemStack(Items.NETHERITE_SWORD);
        };

        sword = maybeTinkersSword(sword);
        applyWeaponEnchantments(sword, tier);
        inventory.add(sword);

        if (BetterHorsesCompat.isLoaded()) {
            ItemStack upgradedSaddle =
                    BetterHorsesCompat.createUpgradedSaddle();
            if (!upgradedSaddle.isEmpty()) {
                inventory.add(upgradedSaddle);
            }
        }

        switch (tier) {
            case STANDARD -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case FINE -> equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
            case RARE -> equipArmorSet(
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS
            );
            case ELITE -> equipArmorSet(
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS
            );
        }
    }

    private void initializeMonkLoadout() {
        equipArmorSet(
                Items.LEATHER_HELMET,
                Items.LEATHER_CHESTPLATE,
                Items.LEATHER_LEGGINGS,
                Items.LEATHER_BOOTS
        );
    }

    private void initializeDruidLoadout() {
        setMageSchool(MageSchool.NATURE);
        initializeMageLoadout();
    }

    private void initializeBardLoadout() {
        setMageSchool(MageSchool.HOLY);
        initializeClericLoadout();
    }

    private void initializeAlchemistLoadout() {
        equipArmorSet(
                Items.LEATHER_HELMET,
                Items.LEATHER_CHESTPLATE,
                Items.LEATHER_LEGGINGS,
                Items.LEATHER_BOOTS
        );
    }

    private void applyClassAdvancementLoadout() {
        if (!"arcane_archer".equals(getWildClassAdvancement())
                || !IronSpellsCompat.isLoaded()) {
            return;
        }

        if (entityData.get(DATA_MAGE_SCHOOL).isBlank()) {
            setMageSchool(MageSchool.randomSchool(getRandom()));
        }

        MageSchool school = getMageSchool();
        ItemStack book = IronSpellsCompat.createMageSpellBook(
                school.preferredSpellBookId(),
                school.spellIds(),
                getGearTier(),
                getRandom()
        );

        if (!book.isEmpty()) {
            book.getOrCreateTag().putString(
                    "CyberNpcMageSchool",
                    school.serializedName()
            );
            inventory.add(book);
        }
    }

    private void initializeClericLoadout() {
        if (!IronSpellsCompat.isLoaded()) {
            setWildClass("classless");
            setMageSchool(null);
            classLoadoutInitialized = false;
            return;
        }

        setMageSchool(MageSchool.HOLY);
        WildNpcGearTier tier = getGearTier();

        ItemStack book = IronSpellsCompat.createClericSpellBook(
                tier,
                getRandom()
        );

        if (!book.isEmpty()) {
            book.getOrCreateTag().putString(
                    "CyberNpcMageSchool",
                    MageSchool.HOLY.serializedName()
            );
            inventory.add(book);
        }

        equipIronSpellArmor(MageSchool.HOLY, tier);
    }

    private void initializeSpellbladeLoadout() {
        if (!IronSpellsCompat.isLoaded()) {
            setWildClass("classless");
            setMageSchool(null);
            classLoadoutInitialized = false;
            return;
        }

        MageSchool school = getMageSchool();
        WildNpcGearTier tier = getGearTier();

        ItemStack sword = switch (tier) {
            case STANDARD -> new ItemStack(Items.IRON_SWORD);
            case FINE -> new ItemStack(Items.IRON_SWORD);
            case RARE -> new ItemStack(Items.DIAMOND_SWORD);
            case ELITE -> new ItemStack(Items.NETHERITE_SWORD);
        };
        sword = maybeTinkersSword(sword);
        applyWeaponEnchantments(sword, tier);
        inventory.add(sword);

        ItemStack book = IronSpellsCompat.createMageSpellBook(
                school.preferredSpellBookId(),
                school.spellIds(),
                tier,
                getRandom()
        );

        if (!book.isEmpty()) {
            book.getOrCreateTag().putString(
                    "CyberNpcMageSchool",
                    school.serializedName()
            );
            inventory.add(book);
        }

        equipIronSpellArmor(school, tier);
    }

    private void initializeMageLoadout() {
        if (!IronSpellsCompat.isLoaded()) {
            setWildClass("classless");
            setMageSchool(null);
            classLoadoutInitialized = false;
            return;
        }

        MageSchool school = getMageSchool();
        WildNpcGearTier tier = getGearTier();

        ItemStack book = IronSpellsCompat.createMageSpellBook(
                school.preferredSpellBookId(),
                school.spellIds(),
                tier,
                getRandom()
        );

        if (!book.isEmpty()) {
            book.getOrCreateTag().putString(
                    "CyberNpcMageSchool",
                    school.serializedName()
            );
            inventory.add(book);
        }

        equipIronSpellArmor(school, tier);
    }

    private void equipIronSpellArmor(
            MageSchool school,
            WildNpcGearTier tier
    ) {
        String prefix = school.armorPrefix();
        ItemStack helmet = IronSpellsCompat.createItem(prefix + "_helmet");
        ItemStack chest = IronSpellsCompat.createItem(prefix + "_chestplate");
        ItemStack legs = IronSpellsCompat.createItem(prefix + "_leggings");
        ItemStack boots = IronSpellsCompat.createItem(prefix + "_boots");

        if (!helmet.isEmpty()
                && !chest.isEmpty()
                && !legs.isEmpty()
                && !boots.isEmpty()) {
            applyArmorEnchantments(helmet, tier);
            applyArmorEnchantments(chest, tier);
            applyArmorEnchantments(legs, tier);
            applyArmorEnchantments(boots, tier);
            setItemSlot(EquipmentSlot.HEAD, helmet);
            setItemSlot(EquipmentSlot.CHEST, chest);
            setItemSlot(EquipmentSlot.LEGS, legs);
            setItemSlot(EquipmentSlot.FEET, boots);
        } else {
            equipArmorSet(
                    Items.LEATHER_HELMET,
                    Items.LEATHER_CHESTPLATE,
                    Items.LEATHER_LEGGINGS,
                    Items.LEATHER_BOOTS
            );
        }
    }

    private void replaceLegacyChainmailArmor() {
        if (chainmailMigrationChecked || getNpcType() != NpcType.WILD) {
            return;
        }

        chainmailMigrationChecked = true;
        boolean knight = isWildClass("knight");

        replaceLegacyChainmailSlot(
                EquipmentSlot.HEAD,
                Items.CHAINMAIL_HELMET,
                knight ? Items.IRON_HELMET : Items.LEATHER_HELMET
        );
        replaceLegacyChainmailSlot(
                EquipmentSlot.CHEST,
                Items.CHAINMAIL_CHESTPLATE,
                knight ? Items.IRON_CHESTPLATE : Items.LEATHER_CHESTPLATE
        );
        replaceLegacyChainmailSlot(
                EquipmentSlot.LEGS,
                Items.CHAINMAIL_LEGGINGS,
                knight ? Items.IRON_LEGGINGS : Items.LEATHER_LEGGINGS
        );
        replaceLegacyChainmailSlot(
                EquipmentSlot.FEET,
                Items.CHAINMAIL_BOOTS,
                knight ? Items.IRON_BOOTS : Items.LEATHER_BOOTS
        );
    }

    private void replaceLegacyChainmailSlot(
            EquipmentSlot slot,
            net.minecraft.world.item.Item oldItem,
            net.minecraft.world.item.Item replacement
    ) {
        ItemStack current = getItemBySlot(slot);
        if (!current.is(oldItem)) {
            return;
        }

        ItemStack updated = new ItemStack(replacement);
        applyArmorEnchantments(updated, getGearTier());
        setItemSlot(slot, updated);
    }

    private void clearArmorSlots() {
        setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
    }

    private void equipArmorSet(
            net.minecraft.world.item.Item helmet,
            net.minecraft.world.item.Item chest,
            net.minecraft.world.item.Item legs,
            net.minecraft.world.item.Item boots
    ) {
        ItemStack helmetStack = new ItemStack(helmet);
        ItemStack chestStack = new ItemStack(chest);
        ItemStack legStack = new ItemStack(legs);
        ItemStack bootStack = new ItemStack(boots);

        applyArmorEnchantments(helmetStack, getGearTier());
        applyArmorEnchantments(chestStack, getGearTier());
        applyArmorEnchantments(legStack, getGearTier());
        applyArmorEnchantments(bootStack, getGearTier());

        setItemSlot(EquipmentSlot.HEAD, helmetStack);
        setItemSlot(EquipmentSlot.CHEST, chestStack);
        setItemSlot(EquipmentSlot.LEGS, legStack);
        setItemSlot(EquipmentSlot.FEET, bootStack);
    }

    private void applyArmorEnchantments(ItemStack stack, WildNpcGearTier tier) {
        int protection = switch (tier) {
            case FINE -> 1;
            case RARE -> 2;
            case ELITE -> 3;
            default -> 0;
        };

        if (protection > 0) {
            stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, protection);
            stack.enchant(
                    Enchantments.UNBREAKING,
                    tier == WildNpcGearTier.ELITE ? 2 : 1
            );
        }
    }

    private ItemStack maybeTinkersSword(ItemStack vanillaSword) {
        if (TinkersConstructCompat.isLoaded()
                && getRandom().nextFloat() < 0.30F) {
            ItemStack tinkers =
                    TinkersConstructCompat.createRandomSword(
                            getRandom()
                    );
            if (!tinkers.isEmpty()) {
                return tinkers;
            }
        }

        return vanillaSword;
    }

    private void applyWeaponEnchantments(ItemStack stack, WildNpcGearTier tier) {
        if (TinkersConstructCompat.isTinkersSword(stack)) {
            return;
        }

        int level = switch (tier) {
            case FINE -> 1;
            case RARE -> 2;
            case ELITE -> 3;
            default -> 0;
        };

        if (level <= 0 || stack.isEmpty()) {
            return;
        }

        if (stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem) {
            stack.enchant(Enchantments.SHARPNESS, level);
            stack.enchant(Enchantments.UNBREAKING, Math.max(1, level - 1));
        } else if (stack.is(Items.BOW)) {
            stack.enchant(Enchantments.POWER_ARROWS, level);
            stack.enchant(Enchantments.UNBREAKING, Math.max(1, level - 1));
        } else if (stack.is(Items.CROSSBOW)) {
            stack.enchant(Enchantments.QUICK_CHARGE, Math.min(3, level));
            stack.enchant(Enchantments.UNBREAKING, Math.max(1, level - 1));
        }
    }

    private boolean isMageSpellBook(ItemStack stack) {
        return !stack.isEmpty()
                && stack.hasTag()
                && stack.getTag().getBoolean("CyberNpcMageSpellbook");
    }

    public ItemStack getMageSpellBook() {
        return inventory.findFirst(this::isMageSpellBook);
    }

    private void applyClassAttributes() {
        double maxHealth;
        double movementSpeed;
        double armor;
        double attackDamage;

        switch (getWildClass()) {
            case "archer" -> {
                maxHealth = 20.0D;
                movementSpeed = 0.435D;
                armor = 0.0D;
                attackDamage = 2.0D;
            }
            case "knight" -> {
                maxHealth = 26.0D;
                movementSpeed = 0.395D;
                armor = 2.0D;
                attackDamage = 3.0D;
            }
            case "rogue" -> {
                maxHealth = 18.0D;
                movementSpeed = 0.455D;
                armor = 0.0D;
                attackDamage = 2.5D;
            }
            case "berserker" -> {
                maxHealth = 24.0D;
                movementSpeed = 0.440D;
                armor = 0.5D;
                attackDamage = 4.0D;
            }
            case "ranger" -> {
                maxHealth = 22.0D;
                movementSpeed = 0.440D;
                armor = 0.5D;
                attackDamage = 2.8D;
            }
            case "mage" -> {
                maxHealth = 22.0D;
                movementSpeed = 0.420D;
                armor = 0.0D;
                attackDamage = 2.0D;
            }
            case "cleric" -> {
                maxHealth = 22.0D;
                movementSpeed = 0.415D;
                armor = 1.0D;
                attackDamage = 2.0D;
            }
            case "spellblade" -> {
                maxHealth = 23.0D;
                movementSpeed = 0.430D;
                armor = 1.0D;
                attackDamage = 3.0D;
            }
            case "monk" -> {
                maxHealth = 20.0D;
                movementSpeed = 0.460D;
                armor = 0.0D;
                attackDamage = 4.5D;
            }
            case "druid" -> {
                maxHealth = 20.0D;
                movementSpeed = 0.425D;
                armor = 0.5D;
                attackDamage = 2.0D;
            }
            case "bard" -> {
                maxHealth = 20.0D;
                movementSpeed = 0.430D;
                armor = 0.5D;
                attackDamage = 2.5D;
            }
            case "alchemist" -> {
                maxHealth = 20.0D;
                movementSpeed = 0.425D;
                armor = 0.5D;
                attackDamage = 2.5D;
            }
            default -> {
                maxHealth = 20.0D;
                movementSpeed = 0.425D;
                armor = 0.0D;
                attackDamage = 2.0D;
            }
        }

        WildNpcGearTier tier = getGearTier();
        maxHealth += tier.healthBonus();
        movementSpeed += tier.speedBonus();
        attackDamage += tier.attackBonus();

        var healthAttribute = getAttribute(Attributes.MAX_HEALTH);
        var speedAttribute = getAttribute(Attributes.MOVEMENT_SPEED);
        var armorAttribute = getAttribute(Attributes.ARMOR);
        var attackAttribute = getAttribute(Attributes.ATTACK_DAMAGE);

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

        if (attackAttribute != null && attackAttribute.getBaseValue() != attackDamage) {
            attackAttribute.setBaseValue(attackDamage);
        }
    }

    private float getEmergencyHealthThreshold() {
        float base = getPersonality().emergencyHealth();

        if (isWildClass("knight")) {
            base -= 1.0F;
        } else if (isWildClass("berserker")) {
            base -= 1.5F;
        } else if (isWildClass("mage") || isWildClass("druid")) {
            base += 1.0F;
        } else if (isWildClass("cleric") || isWildClass("bard")) {
            base += 1.5F;
        }

        return Math.max(4.0F, base);
    }

    private float getHelpHealthFraction() {
        return getPersonality().helpHealthFraction();
    }

    private double getThreatRequiredRatio() {
        double ratio = getPersonality().threatRequiredRatio();

        if (isWildClass("knight")
                || isWildClass("berserker")) {
            ratio -= 0.08D;
        } else if (isWildClass("mage")
                || isWildClass("cleric")
                || isWildClass("druid")
                || isWildClass("bard")) {
            ratio += 0.05D;
        }

        return Math.max(0.65D, ratio);
    }

    public ItemStack getStoredSword() {
        return inventory.findFirst(this::isSwordStack);
    }

    public ItemStack getStoredMeleeWeapon() {
        return inventory.findFirst(this::isMeleeWeaponStack);
    }

    public ItemStack getStoredRangedWeapon() {
        return inventory.findFirst(this::isRangedWeaponStack);
    }

    private boolean isSwordStack(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(CyberNpcWeaponPool.WILD_NPC_SWORDS)
                || stack.getItem() instanceof SwordItem
                || TinkersConstructCompat.isTinkersSword(stack));
    }

    private boolean isMeleeWeaponStack(ItemStack stack) {
        return isSwordStack(stack)
                || (!stack.isEmpty() && stack.getItem() instanceof AxeItem);
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

    /**
     * Narrow public hook used by CyberRaces' Goblin Scavenger Sense AI.
     * CyberRaces finds this through reflection, keeping the mods optional and
     * independently buildable.
     */
    public boolean collectRacialScavengeItem(ItemEntity item) {
        if (level().isClientSide
                || item == null
                || !item.isAlive()
                || item.hasPickUpDelay()
                || getNpcType() != NpcType.WILD) {
            return false;
        }

        ItemStack stack = item.getItem();
        if (stack.isEmpty() || !inventory.canAdd(stack)) {
            return false;
        }

        int before = stack.getCount();
        ItemStack remaining = addToInventory(stack);

        if (remaining.isEmpty()) {
            item.discard();
        } else {
            item.setItem(remaining);
        }

        boolean collected = remaining.getCount() < before;
        if (collected) {
            showReaction(NpcReactionIcon.HAPPY, 45);
        }

        return collected;
    }

    private void equipMeleeWeapon() {
        ItemStack storedWeapon = getStoredMeleeWeapon();
        if (getNpcType() != NpcType.WILD || storedWeapon.isEmpty()) {
            return;
        }

        utilityItemActive = false;
        ItemStack held = getMainHandItem();
        if (!ItemStack.isSameItemSameTags(held, storedWeapon)) {
            stopUsingItem();
            setRangedState(RANGED_STATE_NONE);
            ItemStack copy = storedWeapon.copy();
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

    private void startMageCastingVisual(
            ItemStack spellBook,
            String spellId,
            int visualTicks,
            String castType
    ) {
        if (spellBook.isEmpty()) {
            return;
        }

        // Do not replace the rendered offhand stack every server tick. Reusing
        // the same stack reduces model-resolution churn for modded item models
        // and is friendlier to EMF/ETF-style render interception.
        ItemStack currentOffhand = getOffhandItem();
        if (!ItemStack.isSameItemSameTags(currentOffhand, spellBook)) {
            ItemStack visualBook = spellBook.copy();
            visualBook.setCount(1);
            setItemSlot(EquipmentSlot.OFFHAND, visualBook);
        }

        entityData.set(DATA_CASTING_SPELL, spellId == null ? "" : spellId);
        entityData.set(
                DATA_SPELL_CAST_MODE,
                "LONG".equals(castType)
                        ? SPELL_CAST_MODE_LONG
                        : SPELL_CAST_MODE_INSTANT
        );
        entityData.set(
                DATA_SPELL_CAST_TICKS,
                Math.max(entityData.get(DATA_SPELL_CAST_TICKS), Math.max(2, visualTicks))
        );
    }

    private void tickSpellCastingVisual() {
        int ticks = entityData.get(DATA_SPELL_CAST_TICKS);
        if (ticks <= 0) {
            return;
        }

        ticks--;
        entityData.set(DATA_SPELL_CAST_TICKS, ticks);

        if (ticks <= 0) {
            entityData.set(DATA_CASTING_SPELL, "");
            entityData.set(DATA_SPELL_CAST_MODE, SPELL_CAST_MODE_NONE);

            ItemStack offhand = getOffhandItem();
            if (isMageSpellBook(offhand)) {
                setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }
    }

    private void clearSpellCastingVisual() {
        entityData.set(DATA_SPELL_CAST_TICKS, 0);
        entityData.set(DATA_SPELL_CAST_MODE, SPELL_CAST_MODE_NONE);
        entityData.set(DATA_CASTING_SPELL, "");

        ItemStack offhand = getOffhandItem();
        if (isMageSpellBook(offhand)) {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private void cancelMageCast() {
        IronSpellsCompat.cancelCast(this);
        clearSpellCastingVisual();
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
        if (target instanceof Mob mob) {
            CombatConfidence confidence = evaluateCombatConfidence(mob, true);
            if (confidence.groupScore < FLEE_CONFIDENCE) {
                return;
            }
        }

        beginWildCombat(target, false, true);
    }

    void prepareForSleep() {
        if (getVehicle() instanceof AbstractHorse horse) {
            stopUsingHorse(horse);
        } else {
            clearHorseTarget();
        }

        corralBrain.interrupt();
        clearUtilityItem();
        cancelMageCast();
        stowWeapons();
        getNavigation().stop();
        setSprinting(false);
        setShiftKeyDown(false);
    }

    @Nullable
    BlockPos getClaimedBedPos() {
        return sleepBrain.getClaimedBed();
    }

    boolean shouldRangerReturnBeforeSleeping() {
        if (!isWildClass("ranger")
                || !(getVehicle() instanceof AbstractHorse)
                || rangerHomePos == null
                || blockPosition().distSqr(rangerHomePos)
                <= RANGER_SLEEP_RETURN_DISTANCE_SQR) {
            return false;
        }

        rangerReturningHomeForSleep = true;
        rangerPatrolTarget = rangerHomePos;
        return true;
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

        ensureAppearance();

        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            setNpcType(NpcType.WILD);
            ensureDefaultName();
        }

        if (getNpcType() == NpcType.WILD) {
            setHunger(MAX_HUNGER);
            ensureWildProfile();

            if (isWildClass("ranger")
                    && rangerHomePos == null) {
                rangerHomePos = blockPosition().immutable();
            }

            initializeStaggeredAiTimers();
            stowWeapons();

            if (spawnType == MobSpawnType.NATURAL
                    || spawnType == MobSpawnType.CHUNK_GENERATION) {
                /*
                 * CyberNpc owns the fact that this is a natural Wild spawn,
                 * but no longer owns world-specific level/advancement policy.
                 * CyberServer (when installed) consumes this marker and
                 * applies the server's progression rules. Without CyberServer,
                 * the NPC simply starts at the shared default Cyber Level.
                 */
                getPersistentData().putBoolean(
                    NATURAL_SPAWN_TAG,
                    true
                );

                // CyberRaces assigns a Wild NPC race just after the entity
                // joins the level. Delay natural friendship/party seeding so
                // race preference can participate in the initial social group.
                naturalSocialSeedPending = true;
                naturalSocialSeedWaitTicks = 40;
            }
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

        if (level().isClientSide) {
            return;
        }

        // Applies to Main, Quest, Wild and service NPCs. If navigation has
        // wedged against a fence, this temporarily owns movement so the
        // behaviour that selected the unreachable target cannot immediately
        // push the NPC straight back into the same fence.
        if (isAlive() && fenceAvoidanceBrain.tick()) {
            if (getNpcType() == NpcType.WILD) {
                finishAiTick();
            }
            return;
        }

        if (getNpcType() != NpcType.WILD) {
            if (isAlive()) {
                if (isSpecialServiceNpc()
                        && level() instanceof ServerLevel serverLevel) {
                    SpecialNpcSavedData.get(serverLevel)
                            .ensureRegistered(this);
                }
                buildingBrain.tick();
                serviceBrain.tick();
            } else {
                buildingBrain.release();
            }
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
        tickPendingNaturalSocialSeed();

        if (racialAiControlTicks > 0) {
            racialAiControlTicks--;
        }

        intentions.tick();

        if (isZombifying()) {
            clearHostileTargetsWhileZombifying();
        }

        if (combatHelpCooldown > 0) {
            combatHelpCooldown--;
        }

        if (chatReactionCooldown > 0) {
            chatReactionCooldown--;
        }

        if (horseRemountCooldown > 0) {
            horseRemountCooldown--;
        }

        // Claim discovery is useful but does not need to be a per-tick query.
        // Staggering it is important once many naturally spawned NPCs share a
        // settlement.
        if (backgroundClaimScanCooldown > 0) {
            backgroundClaimScanCooldown--;
        } else {
            backgroundClaimScanCooldown = BACKGROUND_CLAIM_SCAN_INTERVAL;
            corralBrain.tickClaimDiscovery();
        }

        tickMeleeSwingAnimation();
        tickSpellCastingVisual();
        vocalizations.tick();
        tickPlayerLikeSwimming();

        if (gapJumpCooldown > 0) {
            gapJumpCooldown--;
        } else {
            tickPlayerLikeGapJumping();
        }

        if (onClimbable() && horizontalCollision) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x, Math.max(movement.y, 0.20D), movement.z);
        }

        tickHunger();

        if (tickEnvironmentalSafety()) {
            finishAiTick();
            return;
        }

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

        // Active race effects stay owned by CyberRaces. CyberNpc only gives
        // that optional bridge a short movement window when a race such as
        // Dogfolk deliberately tracks a hidden combat target.
        if (tickCount % 4
                == Math.floorMod(getUUID().hashCode(), 4)
                && CyberRacesNpcCompat.tickCombatRacialAi(this)) {
            racialAiControlTicks = Math.max(racialAiControlTicks, 4);
        }

        if (racialAiControlTicks > 0 && getTarget() != null) {
            finishAiTick();
            return;
        }

        tickWildCombat();

        if (sleepBrain.tick()) {
            intentions.clear();
            finishAiTick();
            return;
        }

        if (environmentBrain.tick()) {
            finishAiTick();
            return;
        }

        if (tickHorseUse()) {
            finishAiTick();
            return;
        }

        if (!isBusyWithNeeds()
                && tickCount % 10
                == Math.floorMod(getUUID().hashCode(), 10)
                && CyberRacesNpcCompat.tickIdleRacialAi(this)) {
            racialAiControlTicks = Math.max(racialAiControlTicks, 12);
        }

        if (racialAiControlTicks > 0
                && getTarget() == null
                && !isBusyWithNeeds()) {
            finishAiTick();
            return;
        }

        tickSocialLife();
        tickHuntingAndFood();

        if (attentionBrain.tick()) {
            finishAiTick();
            return;
        }

        finishAiTick();
    }

    private boolean tickHorseUse() {
        if (!isWildClass("ranger")) {
            if (getVehicle() instanceof AbstractHorse horse) {
                stopUsingHorse(horse);
            } else {
                clearHorseTarget();
            }
            return false;
        }

        if (!BetterHorsesCompat.isLoaded()) {
            clearHorseTarget();
            return tickRangerFootPatrol();
        }

        if (isPassenger()) {
            if (getVehicle() instanceof AbstractHorse horse) {
                return tickRidingHorse(horse);
            }
            return false;
        }

        if (horseRemountCooldown > 0) {
            clearHorseTarget();
            return tickRangerFootPatrol();
        }

        if (isCombatActive()
                || fleeingThreat != null
                || isSleeping()
                || isZombifying()
                || socialConversationHoldTicks > 0
                || getHunger() <= HUNT_HUNGER_THRESHOLD
                || hasNonHorseUrgentNeed()) {
            clearHorseTarget();
            return false;
        }

        AbstractHorse target = findLoadedHorse(horseTargetId);

        if (target != null
                && !canUseHorse(target, true)
                && !canTameHorse(target, true)) {
            clearHorseTarget();
            target = null;
        }

        if (target == null) {
            if (horseSearchCooldown > 0) {
                horseSearchCooldown--;
                return tickRangerFootPatrol();
            }

            horseSearchCooldown = HORSE_SEARCH_INTERVAL;

            // Prefer the Ranger's existing horse before looking for a new
            // wild horse. Better Horses ownership persists independently of
            // CyberNpc's temporary coordination claim.
            target = level().getEntitiesOfClass(
                            AbstractHorse.class,
                            getBoundingBox().inflate(
                                    HORSE_SEARCH_RADIUS,
                                    6.0D,
                                    HORSE_SEARCH_RADIUS
                            ),
                            horse -> canUseHorse(horse, false)
                    ).stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .orElse(null);

            if (target == null && hasUpgradedSaddleForTaming()) {
                target = level().getEntitiesOfClass(
                                AbstractHorse.class,
                                getBoundingBox().inflate(
                                        HORSE_SEARCH_RADIUS,
                                        6.0D,
                                        HORSE_SEARCH_RADIUS
                                ),
                                horse -> canTameHorse(horse, false)
                        ).stream()
                        .min(Comparator.comparingDouble(this::distanceToSqr))
                        .orElse(null);
            }

            if (target == null) {
                return tickRangerFootPatrol();
            }

            claimHorse(target);
            horseTargetId = target.getUUID();
        }

        if (distanceToSqr(target) > HORSE_MOUNT_DISTANCE_SQR) {
            getNavigation().moveTo(target, 0.92D);
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            return true;
        }

        getNavigation().stop();

        if (!target.isTamed() && !tameHorseWithUpgradedSaddle(target)) {
            clearHorseTarget();
            return false;
        }

        if (!canUseHorse(target, true)
                || !startRiding(target, true)) {
            clearHorseTarget();
            return false;
        }

        if (rangerHomePos == null) {
            BlockPos claimedBed = getClaimedBedPos();
            rangerHomePos = claimedBed != null
                    ? claimedBed.immutable()
                    : blockPosition().immutable();
        }

        horseRideOrigin = rangerHomePos;
        horseRideTicks = HORSE_RIDE_MIN_TICKS
                + getRandom().nextInt(HORSE_RIDE_RANDOM_TICKS + 1);
        horseRepathCooldown = 0;
        rangerPatrolTarget = null;
        rangerPatrolPauseTicks = 0;

        showReaction(NpcReactionIcon.MOUNT, 60);
        return true;
    }

    private boolean hasUpgradedSaddleForTaming() {
        return !inventory.findFirst(
                BetterHorsesCompat::isUpgradedSaddle
        ).isEmpty();
    }

    private boolean tameHorseWithUpgradedSaddle(AbstractHorse horse) {
        if (!canTameHorse(horse, true)) {
            return false;
        }

        ItemStack saddle = inventory.takeOne(
                BetterHorsesCompat::isUpgradedSaddle
        );
        if (saddle.isEmpty()) {
            return false;
        }

        horse.setTamed(true);
        horse.setOwnerUUID(getUUID());

        boolean ownerSet = BetterHorsesCompat.setBetterHorsesOwner(
                horse,
                getUUID()
        );
        boolean saddleEquipped = ownerSet
                && BetterHorsesCompat.equipUpgradedSaddle(
                horse,
                saddle
        );

        if (!saddleEquipped) {
            BetterHorsesCompat.setBetterHorsesOwner(horse, null);
            horse.setOwnerUUID(null);
            horse.setTamed(false);
            inventory.add(saddle);
            return false;
        }

        horse.setHealth(horse.getMaxHealth());
        horse.setPersistenceRequired();

        level().playSound(
                null,
                horse.blockPosition(),
                SoundEvents.HORSE_SADDLE,
                SoundSource.NEUTRAL,
                0.5F,
                1.0F
        );

        showReaction(NpcReactionIcon.MOUNT, 70);
        return true;
    }

    private boolean canTameHorse(
            AbstractHorse horse,
            boolean alreadyClaimedByThisNpc
    ) {
        if (horse == null
                || !horse.isAlive()
                || horse.isBaby()
                || horse.isTamed()
                || horse.isVehicle()
                || BetterHorsesCompat.hasCartGear(horse)
                || BetterHorsesCompat.getBetterHorsesOwner(horse) != null
                || horse.getOwnerUUID() != null) {
            return false;
        }

        CompoundTag persistent = horse.getPersistentData();

        if (persistent.hasUUID("CyberNpcHorseClaim")) {
            UUID claim = persistent.getUUID("CyberNpcHorseClaim");

            if (!getUUID().equals(claim)) {
                boolean liveClaim = false;

                if (level() instanceof ServerLevel serverLevel) {
                    var claimant = serverLevel.getEntity(claim);
                    liveClaim = claimant instanceof CyberNpcEntity npc
                            && npc.isAlive();
                }

                if (liveClaim) {
                    return false;
                }

                persistent.remove("CyberNpcHorseClaim");
            }
        } else if (alreadyClaimedByThisNpc) {
            return false;
        }

        return true;
    }

    private boolean hasNonHorseUrgentNeed() {
        return !foodToEat.isEmpty()
                || cookingMode != COOK_MODE_NONE
                || dropSearchTicks > 0
                || cookingTarget != null
                || corralBrain.isBusy()
                || groundFoodTargetId != null
                || foodChestTarget != null
                || emergencyEating
                || (suspiciousNpc != null
                && infectionAvoidTicks > 0);
    }

    private boolean tickRidingHorse(AbstractHorse horse) {
        // The rider's torso should stay aligned with the mount. Head tracking
        // remains independent on the client, so the NPC can look around without
        // twisting its whole body through the saddle.
        setYBodyRot(horse.getYRot());

        boolean hardDismount = !horse.isAlive()
                || isCombatActive()
                || fleeingThreat != null
                || isSleeping()
                || isZombifying()
                || ownerReturnedForHorse(horse);

        if (hardDismount) {
            stopUsingHorse(horse);
            return false;
        }

        if (horseRideTicks > 0) {
            horseRideTicks--;
        }

        // Hunger and ordinary chores are real reasons to get off, but a Horse
        // Tamer does not abandon a ride seconds after mounting. The commitment
        // window prevents rapid mount/dismount loops while still allowing
        // combat, danger and sleep to interrupt immediately.
        boolean softDismount = getHunger() <= HUNT_HUNGER_THRESHOLD
                || hasNonHorseUrgentNeed();

        if (softDismount && horseRideTicks <= 0) {
            stopUsingHorse(horse);
            return false;
        }

        CyberNpcEntity partyLeader = getLoadedPartyLeader();
        if (partyLeader != null
                && distanceToSqr(partyLeader)
                > PARTY_FOLLOW_DISTANCE * PARTY_FOLLOW_DISTANCE) {
            boolean sprint = horse.distanceToSqr(partyLeader)
                    > HORSE_SPRINT_DISTANCE_SQR;
            horse.setSprinting(sprint);
            horse.getNavigation().moveTo(
                    partyLeader,
                    sprint
                            ? HORSE_SPRINT_NAV_SPEED
                            : HORSE_PARTY_NAV_SPEED
            );
            return true;
        }

        return tickRangerPatrol(horse);
    }

    private boolean tickRangerFootPatrol() {
        if (!(level() instanceof ServerLevel serverLevel)
                || isCombatActive()
                || fleeingThreat != null
                || isSleeping()
                || isZombifying()
                || socialConversationHoldTicks > 0
                || getHunger() <= HUNT_HUNGER_THRESHOLD
                || hasNonHorseUrgentNeed()) {
            return false;
        }

        BlockPos claimedBed = getClaimedBedPos();
        if (claimedBed != null) {
            rangerHomePos = claimedBed.immutable();
        } else if (rangerHomePos == null) {
            rangerHomePos = blockPosition().immutable();
        }

        if (rangerVillageCheckCooldown > 0) {
            rangerVillageCheckCooldown--;
        } else {
            rangerVillageCheckCooldown = RANGER_VILLAGE_CHECK_INTERVAL;
            rangerHomeIsVillage = rangerHomePos != null
                    && serverLevel.isVillage(rangerHomePos);

            if (!rangerHomeIsVillage
                    && serverLevel.isVillage(blockPosition())) {
                rangerHomePos = blockPosition().immutable();
                rangerHomeIsVillage = true;
                rangerPatrolTarget = null;
            }
        }

        if (rangerHomePos == null) {
            return false;
        }

        if (blockPosition().distSqr(rangerHomePos)
                > RANGER_HOME_RETURN_DISTANCE_SQR) {
            rangerPatrolTarget = rangerHomePos;
        }

        if (rangerPatrolPauseTicks > 0) {
            rangerPatrolPauseTicks--;
            getNavigation().stop();
            setSprinting(false);
            return true;
        }

        if (rangerPatrolTarget != null
                && horizontalDistanceSqr(
                blockPosition(),
                rangerPatrolTarget
        ) <= RANGER_PATROL_REACHED_SQR) {
            rangerPatrolTarget = null;
            rangerPatrolPauseTicks = RANGER_PATROL_PAUSE_MIN_TICKS
                    + getRandom().nextInt(
                    RANGER_PATROL_PAUSE_RANDOM_TICKS + 1
            );
            getNavigation().stop();
            setSprinting(false);
            return true;
        }

        if (rangerPatrolTarget == null) {
            chooseNextRangerPatrolPoint();
        }

        if (rangerPatrolTarget == null) {
            return false;
        }

        boolean started = getNavigation().moveTo(
                rangerPatrolTarget.getX() + 0.5D,
                rangerPatrolTarget.getY(),
                rangerPatrolTarget.getZ() + 0.5D,
                0.78D
        );

        if (!started) {
            rangerPatrolTarget = null;
            rangerPatrolAngle += 0.9D;
            return false;
        }

        setSprinting(false);
        return true;
    }

    private boolean tickRangerPatrol(AbstractHorse horse) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        BlockPos claimedBed = getClaimedBedPos();
        if (claimedBed != null) {
            rangerHomePos = claimedBed.immutable();
        } else if (rangerHomePos == null) {
            rangerHomePos = blockPosition().immutable();
        }

        if (rangerVillageCheckCooldown > 0) {
            rangerVillageCheckCooldown--;
        } else {
            rangerVillageCheckCooldown = RANGER_VILLAGE_CHECK_INTERVAL;
            rangerHomeIsVillage = rangerHomePos != null
                    && serverLevel.isVillage(rangerHomePos);

            // If the Ranger was generated just outside a village, adopt the
            // first village position it actually reaches as its local anchor.
            if (!rangerHomeIsVillage
                    && serverLevel.isVillage(horse.blockPosition())) {
                rangerHomePos = horse.blockPosition().immutable();
                rangerHomeIsVillage = true;
                rangerPatrolTarget = null;
            }
        }

        if (rangerHomePos == null) {
            return true;
        }

        if (rangerReturningHomeForSleep) {
            if (horse.blockPosition().distSqr(rangerHomePos)
                    <= RANGER_SLEEP_RETURN_DISTANCE_SQR) {
                rangerReturningHomeForSleep = false;
                rangerPatrolTarget = null;
                stopUsingHorse(horse);
                return false;
            }

            navigateRangerToward(horse, rangerHomePos, true);
            return true;
        }

        if (horse.blockPosition().distSqr(rangerHomePos)
                > RANGER_HOME_RETURN_DISTANCE_SQR) {
            rangerPatrolTarget = rangerHomePos;
            navigateRangerToward(horse, rangerHomePos, true);
            return true;
        }

        if (rangerPatrolPauseTicks > 0) {
            rangerPatrolPauseTicks--;
            horse.getNavigation().stop();
            horse.setSprinting(false);

            double lookAngle = rangerPatrolAngle
                    + (rangerPatrolPauseTicks % 40 < 20 ? 0.55D : -0.55D);
            getLookControl().setLookAt(
                    horse.getX() + Math.cos(lookAngle) * 10.0D,
                    horse.getEyeY(),
                    horse.getZ() + Math.sin(lookAngle) * 10.0D,
                    14.0F,
                    10.0F
            );
            return true;
        }

        if (rangerPatrolTarget != null
                && horizontalDistanceSqr(
                horse.blockPosition(),
                rangerPatrolTarget
        ) <= RANGER_PATROL_REACHED_SQR) {
            rangerPatrolTarget = null;
            rangerPatrolPauseTicks = RANGER_PATROL_PAUSE_MIN_TICKS
                    + getRandom().nextInt(
                    RANGER_PATROL_PAUSE_RANDOM_TICKS + 1
            );
            horse.getNavigation().stop();
            horse.setSprinting(false);
            return true;
        }

        if (rangerPatrolTarget == null) {
            chooseNextRangerPatrolPoint();
        }

        if (rangerPatrolTarget != null) {
            navigateRangerToward(horse, rangerPatrolTarget, false);
        }

        return true;
    }

    private void chooseNextRangerPatrolPoint() {
        if (rangerHomePos == null) {
            return;
        }

        if (Double.isNaN(rangerPatrolAngle)) {
            rangerPatrolAngle = getRandom().nextDouble()
                    * Math.PI * 2.0D;
        } else {
            // Walk the perimeter in broad arcs instead of choosing random
            // points all over the village interior.
            rangerPatrolAngle += 0.85D
                    + getRandom().nextDouble() * 0.70D;
        }

        int baseRadius = rangerHomeIsVillage
                ? RANGER_PATROL_MIN_RADIUS + 8
                : RANGER_PATROL_MIN_RADIUS;
        int radius = baseRadius
                + getRandom().nextInt(RANGER_PATROL_RANDOM_RADIUS + 1);

        int dx = Mth.floor(Math.cos(rangerPatrolAngle) * radius);
        int dz = Mth.floor(Math.sin(rangerPatrolAngle) * radius);

        rangerPatrolTarget = rangerHomePos.offset(dx, 0, dz).immutable();
    }

    private void navigateRangerToward(
            AbstractHorse horse,
            BlockPos destination,
            boolean sprint
    ) {
        Vec3 delta = new Vec3(
                destination.getX() + 0.5D - horse.getX(),
                0.0D,
                destination.getZ() + 0.5D - horse.getZ()
        );

        if (delta.lengthSqr() < 0.01D) {
            horse.getNavigation().stop();
            horse.setSprinting(false);
            return;
        }

        Vec3 direction = delta.normalize();
        double step = Math.min(
                RANGER_LOCAL_NAV_STEP,
                Math.sqrt(delta.lengthSqr())
        );

        Vec3 localTarget = horse.position().add(
                direction.x * step,
                0.0D,
                direction.z * step
        );

        horse.setSprinting(sprint);
        boolean pathStarted = horse.getNavigation().moveTo(
                localTarget.x,
                horse.getY(),
                localTarget.z,
                sprint
                        ? HORSE_SPRINT_NAV_SPEED
                        : HORSE_NORMAL_NAV_SPEED
        );

        if (!pathStarted && !sprint) {
            rangerPatrolTarget = null;
            rangerPatrolAngle += 0.9D
                    + getRandom().nextDouble() * 0.5D;
        }
    }

    private static double horizontalDistanceSqr(
            BlockPos a,
            BlockPos b
    ) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private void initializeStaggeredAiTimers() {
        int hash = getUUID().hashCode();

        backgroundClaimScanCooldown = Math.floorMod(
                hash ^ 0x4A37,
                BACKGROUND_CLAIM_SCAN_INTERVAL
        );
        socialTickCooldown = Math.floorMod(
                hash ^ 0x1B6D,
                SOCIAL_SCAN_INTERVAL
        );
        horseSearchCooldown = Math.floorMod(
                hash ^ 0x73C1,
                HORSE_SEARCH_INTERVAL
        );
        huntSearchCooldown = Math.floorMod(
                hash ^ 0x2F91,
                HUNT_SEARCH_INTERVAL
        );
        groundFoodSearchCooldown = Math.floorMod(
                hash ^ 0x55A9,
                Math.max(1, GROUND_FOOD_SEARCH_INTERVAL)
        );
        cookingSearchCooldown = Math.floorMod(
                hash ^ 0x0D4B,
                Math.max(1, COOK_STATION_SEARCH_INTERVAL)
        );
        hungerDecayTimer = Math.floorMod(
                hash ^ 0x6E23,
                HUNGER_DECAY_TICKS
        );
    }

    private void tickPlayerLikeSwimming() {
        if (isPassenger() || isSleeping()) {
            if (isSwimming()) {
                setSwimming(false);
            }
            return;
        }

        if (!isInWaterOrBubble()) {
            if (isSwimming()) {
                setSwimming(false);
            }
            return;
        }

        Path path = getNavigation().getPath();
        boolean movingToGoal =
                path != null && !path.isDone() && path.getTarget() != null;

        boolean underwater = isUnderWater();
        setSwimming(underwater && movingToGoal);

        Vec3 movement = getDeltaMovement();
        double upward = underwater ? 0.035D : 0.012D;

        if (underwater
                && getAirSupply() < getMaxAirSupply() / 2) {
            upward = 0.085D;
        }

        if (movingToGoal) {
            Vec3 target = Vec3.atCenterOf(path.getTarget());
            Vec3 horizontal = new Vec3(
                    target.x - getX(),
                    0.0D,
                    target.z - getZ()
            );

            if (horizontal.lengthSqr() > 0.01D) {
                horizontal = horizontal.normalize();
                double push = isSprinting() ? 0.045D : 0.030D;
                movement = movement.add(
                        horizontal.x * push,
                        upward,
                        horizontal.z * push
                );
            } else {
                movement = movement.add(0.0D, upward, 0.0D);
            }
        } else {
            movement = movement.add(0.0D, upward, 0.0D);
        }

        setDeltaMovement(movement);
    }

    private boolean canUseHorse(
            AbstractHorse horse,
            boolean alreadyClaimedByThisNpc
    ) {
        if (!isWildClass("ranger")
                || horse == null
                || !horse.isAlive()
                || horse.isBaby()
                || !horse.isTamed()
                || !horse.isSaddled()
                || !BetterHorsesCompat.hasUpgradedSaddle(horse)
                || horse.isVehicle()
                || BetterHorsesCompat.hasCartGear(horse)) {
            return false;
        }

        CompoundTag persistent = horse.getPersistentData();

        if (persistent.hasUUID("CyberNpcHorseClaim")) {
            UUID claim = persistent.getUUID("CyberNpcHorseClaim");

            if (!getUUID().equals(claim)) {
                boolean liveClaim = false;

                if (level() instanceof ServerLevel serverLevel) {
                    var claimant = serverLevel.getEntity(claim);
                    liveClaim = claimant instanceof CyberNpcEntity npc
                            && npc.isAlive();
                }

                if (liveClaim) {
                    return false;
                }

                persistent.remove("CyberNpcHorseClaim");
            }
        } else if (alreadyClaimedByThisNpc) {
            return false;
        }

        UUID owner = BetterHorsesCompat.getBetterHorsesOwner(horse);
        if (owner == null) {
            owner = horse.getOwnerUUID();
        }

        return getUUID().equals(owner);
    }

    private boolean ownerReturnedForHorse(AbstractHorse horse) {
        UUID owner = BetterHorsesCompat.getBetterHorsesOwner(horse);
        if (owner == null) {
            owner = horse.getOwnerUUID();
        }

        if (owner == null
                || owner.equals(getUUID())
                || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Player playerOwner = serverLevel.getPlayerByUUID(owner);
        return playerOwner != null
                && playerOwner.distanceToSqr(horse)
                <= HORSE_OWNER_RETURN_RADIUS_SQR;
    }

    private void claimHorse(AbstractHorse horse) {
        horse.getPersistentData().putUUID(
                "CyberNpcHorseClaim",
                getUUID()
        );
    }

    private void releaseHorseClaim(AbstractHorse horse) {
        CompoundTag persistent = horse.getPersistentData();

        if (persistent.hasUUID("CyberNpcHorseClaim")
                && getUUID().equals(
                persistent.getUUID("CyberNpcHorseClaim")
        )) {
            persistent.remove("CyberNpcHorseClaim");
        }
    }

    private void stopUsingHorse(AbstractHorse horse) {
        stopRiding();
        horse.setSprinting(false);
        horse.getNavigation().stop();
        releaseHorseClaim(horse);
        horseTargetId = null;
        horseRideOrigin = null;
        horseRideTicks = 0;
        horseRepathCooldown = 0;
        horseSearchCooldown = HORSE_SEARCH_INTERVAL;
        horseRemountCooldown = HORSE_REMOUNT_COOLDOWN_TICKS;
    }

    private void clearHorseTarget() {
        AbstractHorse horse = findLoadedHorse(horseTargetId);
        if (horse != null) {
            releaseHorseClaim(horse);
        }

        horseTargetId = null;
        horseRideOrigin = null;
    }

    @Nullable
    private AbstractHorse findLoadedHorse(@Nullable UUID id) {
        if (id == null
                || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var entity = serverLevel.getEntity(id);
        return entity instanceof AbstractHorse horse ? horse : null;
    }

    private void tickSocialLife() {
        regroupingWithParty = false;

        if (socialConversationCooldown > 0) {
            socialConversationCooldown--;
        }
        if (partyInviteCooldown > 0) {
            partyInviteCooldown--;
        }

        tickPendingPartyInvite();

        if (isCombatActive()
                || fleeingThreat != null
                || isSleeping()
                || isZombifying()) {
            clearSocialConversationHold();
            return;
        }

        boolean conversing = tickSocialConversationHold();

        if (!conversing) {
            if (socialTickCooldown > 0) {
                socialTickCooldown--;
            } else {
                socialTickCooldown = SOCIAL_SCAN_INTERVAL;
                updateNearbySocialBonds();
            }
        }

        if (!conversing && !isBusyWithNeeds()) {
            tickPartyCohesion();
            tickFriendshipBehaviour();
        }
    }

    private void updateNearbySocialBonds() {
        List<CyberNpcEntity> nearby = level().getEntitiesOfClass(
                        CyberNpcEntity.class,
                        getBoundingBox().inflate(
                                SOCIAL_RADIUS,
                                4.0D,
                                SOCIAL_RADIUS
                        ),
                        npc -> npc != this
                                && npc.isAlive()
                                && npc.getNpcType() == NpcType.WILD
                                && !npc.isSleeping()
                                && !npc.isZombifying()
                                && !npc.isCombatActive()
                                && npc.fleeingThreat == null
                ).stream()
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .limit(3)
                .toList();

        for (CyberNpcEntity other : nearby) {
            // One side owns a conversation attempt so the pair cannot trigger
            // two simultaneous exchanges from the same server tick.
            if (getUUID().compareTo(other.getUUID()) >= 0) {
                continue;
            }

            if (trySocialConversation(other)) {
                break;
            }
        }
    }

    private boolean trySocialConversation(CyberNpcEntity other) {
        if (other == null
                || socialConversationCooldown > 0
                || other.socialConversationCooldown > 0
                || pendingPartyInviteFrom != null
                || other.pendingPartyInviteFrom != null
                || hasActiveReaction()
                || other.hasActiveReaction()
                || getRandom().nextFloat() >= SOCIAL_CONVERSATION_CHANCE) {
            return false;
        }

        socialConversationCooldown = SOCIAL_CONVERSATION_COOLDOWN;
        other.socialConversationCooldown = SOCIAL_CONVERSATION_COOLDOWN;
        startSocialConversationHold(other, 52);

        boolean wereFriends = areMutualFriends(other);

        strengthenPeacefulBond(this, other);
        strengthenPeacefulBond(other, this);

        boolean becameFriends = promoteFriendshipIfReady(other);
        boolean sharedFood = false;

        if (areMutualFriends(other)) {
            sharedFood = tryShareFoodWithFriend(other);

            if (!becameFriends && !sharedFood && wereFriends
                    && getRandom().nextFloat() < 0.55F) {
                showReaction(NpcReactionIcon.GREETING, 48);
                other.showReaction(NpcReactionIcon.GREETING, 48);
            }

            considerPartyInvitation(other);
        }

        if (!becameFriends && !sharedFood && !wereFriends) {
            showConversationReactionPair(other);
        }

        return true;
    }

    private boolean promoteFriendshipIfReady(CyberNpcEntity other) {
        boolean thisReady = socialMemory.canBecomeFriends(other.getUUID())
                || socialMemory.isFriend(other.getUUID());
        boolean otherReady = other.socialMemory.canBecomeFriends(getUUID())
                || other.socialMemory.isFriend(getUUID());

        if (!thisReady || !otherReady) {
            return false;
        }

        boolean changed = socialMemory.markFriends(other.getUUID());
        changed |= other.socialMemory.markFriends(getUUID());

        if (changed) {
            showReaction(NpcReactionIcon.FRIENDLY, 70);
            other.showReaction(NpcReactionIcon.FRIENDLY, 70);
        }

        return changed;
    }

    private void showConversationReactionPair(CyberNpcEntity other) {
        int thisSupport = socialMemory.supportScore(other.getUUID());
        int otherSupport = other.socialMemory.supportScore(getUUID());
        int thisAffinity = CyberRacesNpcCompat.socialAffinity(this, other);
        int otherAffinity = CyberRacesNpcCompat.socialAffinity(other, this);
        boolean sameRace = CyberRacesNpcCompat.sameRace(this, other);

        NpcReactionIcon mine;
        NpcReactionIcon theirs;

        if (sameRace) {
            mine = CyberRacesNpcCompat.encounterReaction(this, other, 0);
            theirs = CyberRacesNpcCompat.encounterReaction(other, this, 0);
        } else if (thisAffinity >= 2 || otherAffinity >= 2) {
            mine = thisAffinity >= 2
                    ? CyberRacesNpcCompat.encounterReaction(this, other, 0)
                    : NpcReactionIcon.THINKING;
            theirs = otherAffinity >= 2
                    ? CyberRacesNpcCompat.encounterReaction(other, this, 0)
                    : NpcReactionIcon.THINKING;
        } else if (thisSupport >= 20 && otherSupport >= 20) {
            mine = getRandom().nextBoolean()
                    ? NpcReactionIcon.HAPPY
                    : NpcReactionIcon.FRIENDLY;
            theirs = getRandom().nextBoolean()
                    ? NpcReactionIcon.HAPPY
                    : NpcReactionIcon.FRIENDLY;
        } else if (thisAffinity < 0 || otherAffinity < 0
                || thisSupport <= -15 || otherSupport <= -15) {
            mine = thisAffinity < 0
                    ? CyberRacesNpcCompat.encounterReaction(this, other, 0)
                    : NpcReactionIcon.CONFUSED;
            theirs = otherAffinity < 0
                    ? CyberRacesNpcCompat.encounterReaction(other, this, 0)
                    : NpcReactionIcon.CONFUSED;
        } else {
            mine = getRandom().nextBoolean()
                    ? NpcReactionIcon.THINKING
                    : NpcReactionIcon.CONFUSED;
            theirs = getRandom().nextFloat() < 0.65F
                    ? NpcReactionIcon.HAPPY
                    : NpcReactionIcon.THINKING;
        }

        showReaction(mine, 46);
        other.showReaction(theirs, 46);
    }

    private static void strengthenPeacefulBond(
            CyberNpcEntity source,
            CyberNpcEntity target
    ) {
        int friendship = source.getPersonality()
                == WildNpcPersonality.SKITTISH ? 1 : 2;
        int trust = 2;
        int respect = 0;
        int affinity = CyberRacesNpcCompat.socialAffinity(source, target);

        if (source.getPersonality() == WildNpcPersonality.LOYAL
                || source.getPersonality()
                == WildNpcPersonality.PROTECTIVE) {
            friendship++;
            trust++;
        }

        if (source.getPersonality() == WildNpcPersonality.BRAVE
                || source.getPersonality() == WildNpcPersonality.TACTICAL
                || source.getPersonality() == WildNpcPersonality.STUBBORN) {
            respect++;
        }

        if (affinity >= 3) {
            friendship += 2;
            trust++;
        } else if (affinity >= 1) {
            friendship++;
        } else if (affinity < 0) {
            friendship = Math.max(1, friendship - 1);
            trust = Math.max(1, trust - 1);
        }

        int rivalryDelta = affinity < 0
                && source.getRandom().nextFloat() < 0.25F
                ? 1
                : -1;

        source.socialMemory.adjustRelationship(
                target.getUUID(),
                friendship,
                trust,
                respect,
                -1,
                rivalryDelta
        );
    }

    private boolean areMutualFriends(CyberNpcEntity other) {
        return other != null
                && socialMemory.isFriend(other.getUUID())
                && other.socialMemory.isFriend(getUUID());
    }

    private boolean tryShareFoodWithFriend(
            CyberNpcEntity other
    ) {
        if (other == null || !areMutualFriends(other)) {
            return false;
        }

        if (getHunger() > 12
                && other.getHunger()
                <= FRIEND_SHARE_HUNGER_THRESHOLD
                && hasReadyFood()) {
            return shareOneFoodTo(other);
        }

        if (other.getHunger() > 12
                && getHunger()
                <= FRIEND_SHARE_HUNGER_THRESHOLD
                && other.hasReadyFood()) {
            return other.shareOneFoodTo(this);
        }

        return false;
    }

    private boolean shareOneFoodTo(CyberNpcEntity friend) {
        ItemStack food = takeOneReadyFood();
        if (food.isEmpty()) {
            return false;
        }

        ItemStack remainder = friend.addToInventory(food);
        if (!remainder.isEmpty()) {
            addToInventory(remainder);
            return false;
        }

        showReaction(NpcReactionIcon.FOOD, 55);
        friend.showReaction(NpcReactionIcon.HAPPY, 55);

        socialMemory.adjustRelationship(
                friend.getUUID(),
                1,
                2,
                1,
                0,
                -1
        );
        friend.socialMemory.adjustRelationship(
                getUUID(),
                2,
                2,
                1,
                0,
                -1
        );

        return true;
    }

    private void tickFriendshipBehaviour() {
        if (socialMemory.hasParty()
                || isBusyWithNeeds()
                || tickCount % 40
                != Math.floorMod(getUUID().hashCode(), 40)) {
            return;
        }

        CyberNpcEntity friend = level().getEntitiesOfClass(
                        CyberNpcEntity.class,
                        getBoundingBox().inflate(
                                FRIEND_APPROACH_LIMIT,
                                5.0D,
                                FRIEND_APPROACH_LIMIT
                        ),
                        npc -> npc != this
                                && npc.isAlive()
                                && npc.getNpcType() == NpcType.WILD
                                && isFriendWith(npc)
                                && npc.isFriendWith(this)
                                && !npc.isBusyWithNeeds()
                                && !npc.isCombatActive()
                                && !npc.isSleeping()
                ).stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (friend == null) {
            return;
        }

        double distanceSqr = distanceToSqr(friend);
        double approach = FRIEND_APPROACH_DISTANCE
                * FRIEND_APPROACH_DISTANCE;

        if (distanceSqr > approach
                && distanceSqr
                <= FRIEND_APPROACH_LIMIT * FRIEND_APPROACH_LIMIT) {
            getNavigation().moveTo(friend, 0.72D);
        }
    }

    private void considerPartyInvitation(CyberNpcEntity other) {
        if (!areMutualFriends(other)
                || isSameParty(other)
                || partyInviteCooldown > 0
                || other.partyInviteCooldown > 0) {
            return;
        }

        CyberNpcEntity inviter = choosePartyInviter(this, other);
        if (inviter == null) {
            return;
        }

        CyberNpcEntity invitee = inviter == this ? other : this;

        if (!canInviteToParty(inviter, invitee)) {
            return;
        }

        float inviteChance = Mth.clamp(
                PARTY_INVITE_CHANCE
                        * CyberRacesNpcCompat.partyInviteMultiplier(
                        inviter,
                        invitee
                ),
                0.10F,
                0.82F
        );

        if (getRandom().nextFloat() >= inviteChance) {
            return;
        }

        inviter.partyInviteCooldown = PARTY_INVITE_COOLDOWN_TICKS;
        invitee.partyInviteCooldown = PARTY_INVITE_COOLDOWN_TICKS;
        invitee.pendingPartyInviteFrom = inviter.getUUID();
        invitee.pendingPartyInviteTicks = PARTY_INVITE_RESPONSE_TICKS;
        inviter.startSocialConversationHold(invitee, 72);

        inviter.showReaction(NpcReactionIcon.GROUP_INVITE, 60);
        invitee.showReaction(NpcReactionIcon.THINKING, 30);
    }

    @Nullable
    private static CyberNpcEntity choosePartyInviter(
            CyberNpcEntity first,
            CyberNpcEntity second
    ) {
        boolean firstParty = first.socialMemory.hasParty();
        boolean secondParty = second.socialMemory.hasParty();

        if (firstParty && secondParty) {
            return null;
        }

        if (firstParty) {
            return first.socialMemory.isPartyLeader(first.getUUID())
                    ? first
                    : null;
        }

        if (secondParty) {
            return second.socialMemory.isPartyLeader(second.getUUID())
                    ? second
                    : null;
        }

        return first.partyLeadershipScore()
                >= second.partyLeadershipScore()
                ? first
                : second;
    }

    private static boolean canInviteToParty(
            CyberNpcEntity inviter,
            CyberNpcEntity invitee
    ) {
        if (inviter == null
                || invitee == null
                || inviter == invitee
                || !inviter.areMutualFriends(invitee)
                || inviter.isSameParty(invitee)) {
            return false;
        }

        if (invitee.socialMemory.hasParty()) {
            return false;
        }

        if (!inviter.socialMemory.hasParty()) {
            return true;
        }

        return inviter.socialMemory.isPartyLeader(inviter.getUUID())
                && inviter.getLoadedPartySize() < PARTY_MAX_SIZE;
    }

    private void startSocialConversationHold(
            CyberNpcEntity other,
            int ticks
    ) {
        if (other == null || other == this || ticks <= 0) {
            return;
        }

        socialConversationPartnerId = other.getUUID();
        socialConversationHoldTicks = Math.max(
                socialConversationHoldTicks,
                ticks
        );

        other.socialConversationPartnerId = getUUID();
        other.socialConversationHoldTicks = Math.max(
                other.socialConversationHoldTicks,
                ticks
        );

        getNavigation().stop();
        other.getNavigation().stop();
        setSprinting(false);
        other.setSprinting(false);
    }

    private boolean tickSocialConversationHold() {
        if (socialConversationHoldTicks <= 0
                || socialConversationPartnerId == null) {
            clearSocialConversationHold();
            return false;
        }

        CyberNpcEntity partner = findLoadedWildNpc(
                socialConversationPartnerId
        );

        if (partner == null
                || distanceToSqr(partner)
                > SOCIAL_RADIUS * SOCIAL_RADIUS
                || partner.isCombatActive()
                || partner.fleeingThreat != null
                || partner.isZombifying()) {
            clearSocialConversationHold();
            return false;
        }

        socialConversationHoldTicks--;
        getNavigation().stop();
        setSprinting(false);
        setShiftKeyDown(false);
        getLookControl().setLookAt(partner, 30.0F, 30.0F);

        if (socialConversationHoldTicks <= 0) {
            clearSocialConversationHold();
        }

        return true;
    }

    private void clearSocialConversationHold() {
        socialConversationHoldTicks = 0;
        socialConversationPartnerId = null;
    }

    private void tickPendingPartyInvite() {
        if (pendingPartyInviteFrom == null) {
            return;
        }

        if (isCombatActive()
                || fleeingThreat != null
                || isZombifying()) {
            clearPendingPartyInvite();
            return;
        }

        if (pendingPartyInviteTicks > 0) {
            pendingPartyInviteTicks--;
            return;
        }

        CyberNpcEntity inviter = findLoadedWildNpc(
                pendingPartyInviteFrom
        );

        if (inviter == null || !canInviteToParty(inviter, this)) {
            showReaction(NpcReactionIcon.CONFUSED, 35);
            clearPendingPartyInvite();
            return;
        }

        acceptPartyInvite(inviter);
        clearPendingPartyInvite();
    }

    private void acceptPartyInvite(CyberNpcEntity inviter) {
        if (inviter.socialMemory.hasParty()) {
            setParty(
                    inviter.socialMemory.partyId(),
                    inviter.socialMemory.partyLeaderId()
            );
        } else {
            UUID partyId = UUID.randomUUID();
            CyberNpcEntity leader = inviter.partyLeadershipScore()
                    >= partyLeadershipScore()
                    ? inviter
                    : this;

            inviter.setParty(partyId, leader.getUUID());
            setParty(partyId, leader.getUUID());
        }

        reinforcePartyBond(inviter);
        showReaction(NpcReactionIcon.GROUP_ACCEPT, 65);
        inviter.showReaction(NpcReactionIcon.GROUP_ACCEPT, 65);
    }

    private void clearPendingPartyInvite() {
        pendingPartyInviteFrom = null;
        pendingPartyInviteTicks = 0;
    }

    @Nullable
    private CyberNpcEntity findLoadedWildNpc(UUID id) {
        if (id == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var entity = serverLevel.getEntity(id);
        return entity instanceof CyberNpcEntity npc
                && npc.isAlive()
                && npc.getNpcType() == NpcType.WILD
                ? npc
                : null;
    }

    private void reinforcePartyBond(CyberNpcEntity other) {
        socialMemory.markFriends(other.getUUID());
        other.socialMemory.markFriends(getUUID());

        socialMemory.adjustRelationship(
                other.getUUID(),
                4,
                5,
                2,
                -3,
                -3
        );
        other.socialMemory.adjustRelationship(
                getUUID(),
                4,
                5,
                2,
                -3,
                -3
        );
    }

    private void tickPendingNaturalSocialSeed() {
        if (!naturalSocialSeedPending) {
            return;
        }

        if (CyberRacesNpcCompat.isLoaded()
                && !CyberRacesNpcCompat.hasRace(this)
                && naturalSocialSeedWaitTicks > 0) {
            naturalSocialSeedWaitTicks--;
            return;
        }

        naturalSocialSeedPending = false;
        naturalSocialSeedWaitTicks = 0;
        seedNaturalSpawnSocialContext();
    }

    private void seedNaturalSpawnSocialContext() {
        if (!(level() instanceof ServerLevel serverLevel)
                || getRandom().nextFloat()
                >= NATURAL_FRIEND_SEED_CHANCE) {
            return;
        }

        List<CyberNpcEntity> nearby = serverLevel.getEntitiesOfClass(
                        CyberNpcEntity.class,
                        getBoundingBox().inflate(
                                NATURAL_SOCIAL_SEED_RADIUS,
                                6.0D,
                                NATURAL_SOCIAL_SEED_RADIUS
                        ),
                        npc -> npc != this
                                && npc.isAlive()
                                && npc.getNpcType() == NpcType.WILD
                                && !npc.isZombifying()
                ).stream()
                .sorted(
                        Comparator
                                .<CyberNpcEntity>comparingInt(
                                        npc -> CyberRacesNpcCompat
                                                .socialAffinity(this, npc)
                                )
                                .reversed()
                                .thenComparingDouble(this::distanceToSqr)
                )
                .limit(PARTY_MAX_SIZE)
                .toList();

        if (nearby.isEmpty()) {
            return;
        }

        CyberNpcEntity primary = nearby.get(0);
        seedPreExistingFriendship(primary);

        // Some natural NPCs arrive with more than one prior acquaintance.
        if (nearby.size() > 1 && getRandom().nextFloat() < 0.35F) {
            seedPreExistingFriendship(nearby.get(1));
        }

        float naturalPartyChance = Mth.clamp(
                NATURAL_PARTY_SEED_CHANCE
                        * CyberRacesNpcCompat.partyInviteMultiplier(
                        this,
                        primary
                ),
                0.20F,
                0.90F
        );

        if (getRandom().nextFloat() < naturalPartyChance) {
            seedPreExistingParty(primary);
        }
    }

    private void seedPreExistingFriendship(CyberNpcEntity other) {
        int affinity = CyberRacesNpcCompat.socialAffinity(this, other);
        int friendship = 36 + getRandom().nextInt(25)
                + Math.max(0, affinity * 2);
        int trust = 30 + getRandom().nextInt(26)
                + Math.max(0, affinity);
        int respect = 8 + getRandom().nextInt(28)
                + (affinity < 0 ? 2 : 0);

        if (affinity < 0) {
            friendship = Math.max(28, friendship - 4);
            trust = Math.max(22, trust - 3);
        }

        socialMemory.seedFriendship(
                other.getUUID(),
                friendship,
                trust,
                respect
        );
        other.socialMemory.seedFriendship(
                getUUID(),
                friendship,
                trust,
                respect
        );
    }

    private void seedPreExistingParty(CyberNpcEntity other) {
        if (!areMutualFriends(other) || isSameParty(other)) {
            return;
        }

        if (other.socialMemory.hasParty()) {
            if (other.getLoadedPartySize() < PARTY_MAX_SIZE) {
                setParty(
                        other.socialMemory.partyId(),
                        other.socialMemory.partyLeaderId()
                );
                reinforcePartyBond(other);
            }
            return;
        }

        if (socialMemory.hasParty()) {
            return;
        }

        UUID partyId = UUID.randomUUID();
        CyberNpcEntity leader = partyLeadershipScore()
                >= other.partyLeadershipScore()
                ? this
                : other;

        setParty(partyId, leader.getUUID());
        other.setParty(partyId, leader.getUUID());
        reinforcePartyBond(other);
    }

    private int partyLeadershipScore() {
        int score = getGearTier().ordinal();

        score += switch (getPersonality()) {
            case TACTICAL -> 5;
            case PROTECTIVE -> 4;
            case BRAVE -> 3;
            case LOYAL -> 2;
            case PATIENT -> 1;
            case SKITTISH -> -3;
            default -> 0;
        };

        score += switch (getWildClass()) {
            case "knight" -> 3;
            case "berserker" -> 2;
            case "cleric" -> 2;
            case "spellblade" -> 2;
            default -> 0;
        };

        return score;
    }

    private void setParty(@Nullable UUID partyId, @Nullable UUID leaderId) {
        if (partyId == null || leaderId == null) {
            socialMemory.clearParty();
            return;
        }

        socialMemory.setParty(partyId, leaderId);
    }

    private boolean isSameParty(CyberNpcEntity other) {
        return other != null
                && socialMemory.hasParty()
                && other.socialMemory.hasParty()
                && socialMemory.partyId().equals(other.socialMemory.partyId());
    }

    private int getLoadedPartySize() {
        if (!socialMemory.hasParty()) {
            return 1;
        }

        return level().getEntitiesOfClass(
                CyberNpcEntity.class,
                getBoundingBox().inflate(48.0D),
                npc -> npc != this
                        && npc.isAlive()
                        && isSameParty(npc)
        ).size() + 1;
    }

    @Nullable
    private CyberNpcEntity getLoadedPartyLeader() {
        if (!socialMemory.hasParty()
                || socialMemory.partyLeaderId() == null
                || socialMemory.partyLeaderId().equals(getUUID())
                || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var entity = serverLevel.getEntity(socialMemory.partyLeaderId());
        return entity instanceof CyberNpcEntity npc
                && npc.isAlive()
                && isSameParty(npc)
                ? npc
                : null;
    }

    private void tickPartyCohesion() {
        CyberNpcEntity leader = getLoadedPartyLeader();
        if (leader == null) {
            return;
        }

        double distanceSqr = distanceToSqr(leader);
        if (distanceSqr <= PARTY_FOLLOW_DISTANCE * PARTY_FOLLOW_DISTANCE
                || distanceSqr > PARTY_FOLLOW_LIMIT * PARTY_FOLLOW_LIMIT) {
            return;
        }

        regroupingWithParty = true;
        setShiftKeyDown(false);
        setSprinting(distanceSqr > 196.0D);
        getNavigation().moveTo(
                leader,
                distanceSqr > 196.0D ? 1.0D : 0.82D
        );
    }

    private void handlePartyDeath() {
        if (!socialMemory.hasParty()
                || !socialMemory.isPartyLeader(getUUID())) {
            return;
        }

        List<CyberNpcEntity> members = level().getEntitiesOfClass(
                CyberNpcEntity.class,
                getBoundingBox().inflate(48.0D),
                npc -> npc != this
                        && npc.isAlive()
                        && isSameParty(npc)
        );

        CyberNpcEntity replacement = members.stream()
                .max(Comparator.comparingInt(CyberNpcEntity::partyLeadershipScore))
                .orElse(null);

        if (replacement == null) {
            return;
        }

        UUID partyId = socialMemory.partyId();
        for (CyberNpcEntity member : members) {
            member.setParty(partyId, replacement.getUUID());
        }
    }

    private void finishAiTick() {
        updateSculkStealth();
        fixStuckSprintAnimation();
        updateDebugState();
    }

    private void updateSculkStealth() {
        boolean normalMovement = nearbyWarden == null
                && !isCombatActive()
                && fleeingThreat == null
                && !emergencyEating
                && !isSleeping()
                && !isZombifying()
                && !isSprinting();

        if (!normalMovement) {
            sculkStealthTicks = 0;
            if (sculkSneaking) {
                sculkSneaking = false;
                setShiftKeyDown(false);
            }
            return;
        }

        if (sculkScanCooldown > 0) {
            sculkScanCooldown--;
        } else {
            sculkScanCooldown = SCULK_SCAN_INTERVAL;
            if (findVisibleSculkSensor() != null) {
                sculkStealthTicks = SCULK_STEALTH_HOLD_TICKS;
            }
        }

        if (sculkStealthTicks > 0) {
            sculkStealthTicks--;
            sculkSneaking = true;
            setSprinting(false);
            setShiftKeyDown(true);
        } else if (sculkSneaking) {
            sculkSneaking = false;
            setShiftKeyDown(false);
        }
    }

    @Nullable
    private BlockPos findVisibleSculkSensor() {
        BlockPos origin = blockPosition();

        for (BlockPos candidate : BlockPos.withinManhattan(
                origin,
                SCULK_SCAN_RADIUS,
                6,
                SCULK_SCAN_RADIUS
        )) {
            var state = level().getBlockState(candidate);
            boolean normal = state.is(Blocks.SCULK_SENSOR);
            boolean calibrated = state.is(Blocks.CALIBRATED_SCULK_SENSOR);

            if (!normal && !calibrated) {
                continue;
            }

            double awarenessRange = calibrated ? 16.0D : 9.5D;
            if (distanceToSqr(Vec3.atCenterOf(candidate))
                    > awarenessRange * awarenessRange) {
                continue;
            }

            if (canSeeSculkBlock(candidate)) {
                return candidate.immutable();
            }
        }

        return null;
    }

    private boolean canSeeSculkBlock(BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos);
        BlockHitResult hit = level().clip(new ClipContext(
                getEyePosition(),
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        return hit.getType() == HitResult.Type.MISS
                || hit.getBlockPos().equals(pos);
    }

    @Override
    public boolean isSteppingCarefully() {
        return isShiftKeyDown() || super.isSteppingCarefully();
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

    private boolean tickEnvironmentalSafety() {
        if (isSleeping()) {
            return false;
        }

        BlockPos feet = blockPosition();
        BlockPos below = feet.below();

        if (isInLava()
                || isOnFire()
                || isImmediateHazard(level().getBlockState(feet).getBlock())
                || isImmediateHazard(level().getBlockState(below).getBlock())) {
            Vec3 safe = findNearbySafeEscapePosition(feet);
            if (safe != null) {
                cancelMageCast();
                setShiftKeyDown(false);
                setSprinting(true);
                getNavigation().moveTo(safe.x, safe.y, safe.z, 1.0D);
            }
            return true;
        }

        if (wardenScanCooldown > 0) {
            wardenScanCooldown--;
        } else {
            wardenScanCooldown = WARDEN_SCAN_INTERVAL;
            nearbyWarden = level().getEntitiesOfClass(
                            Warden.class,
                            getBoundingBox().inflate(
                                    WARDEN_QUIET_AVOID_RADIUS,
                                    10.0D,
                                    WARDEN_QUIET_AVOID_RADIUS
                            ),
                            Warden::isAlive
                    ).stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .orElse(null);
        }

        if (nearbyWarden == null
                || !nearbyWarden.isAlive()
                || distanceToSqr(nearbyWarden)
                > WARDEN_QUIET_AVOID_RADIUS * WARDEN_QUIET_AVOID_RADIUS) {
            nearbyWarden = null;
            return false;
        }

        double distance = distanceTo(nearbyWarden);
        boolean committedDanger = nearbyWarden.getTarget() == this
                || distance <= WARDEN_PANIC_RADIUS;

        if (committedDanger) {
            startFleeingFrom(nearbyWarden);
            tickFleeing();
            return true;
        }

        if (!isCombatActive() && !emergencyEating) {
            stopUsingItem();
            clearUtilityItem();
            cancelMageCast();
            setSprinting(false);
            setShiftKeyDown(true);

            Vec3 away = DefaultRandomPos.getPosAway(
                    this,
                    12,
                    5,
                    nearbyWarden.position()
            );

            if (away != null
                    && isSafeStandingPosition(BlockPos.containing(away))) {
                getNavigation().moveTo(
                        away.x,
                        away.y,
                        away.z,
                        QUIET_RETREAT_SPEED
                );
            }

            return true;
        }

        return false;
    }

    private boolean isImmediateHazard(net.minecraft.world.level.block.Block block) {
        return block == Blocks.CAMPFIRE
                || block == Blocks.SOUL_CAMPFIRE
                || block == Blocks.FIRE
                || block == Blocks.SOUL_FIRE
                || block == Blocks.MAGMA_BLOCK
                || block == Blocks.CACTUS
                || block == Blocks.SWEET_BERRY_BUSH
                || block == Blocks.WITHER_ROSE;
    }

    @Nullable
    private Vec3 findNearbySafeEscapePosition(BlockPos danger) {
        for (int attempt = 0; attempt < 10; attempt++) {
            Vec3 candidate = DefaultRandomPos.getPosAway(
                    this,
                    10,
                    5,
                    Vec3.atCenterOf(danger)
            );

            if (candidate != null
                    && isSafeStandingPosition(BlockPos.containing(candidate))) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isSafeStandingPosition(BlockPos feet) {
        if (!level().getFluidState(feet).isEmpty()
                || !level().getFluidState(feet.below()).isEmpty()) {
            return false;
        }

        var feetBlock = level().getBlockState(feet).getBlock();
        var belowBlock = level().getBlockState(feet.below()).getBlock();

        if (isImmediateHazard(feetBlock) || isImmediateHazard(belowBlock)) {
            return false;
        }

        return level().getBlockState(feet)
                        .getCollisionShape(level(), feet)
                        .isEmpty()
                && level().getBlockState(feet.above())
                        .getCollisionShape(level(), feet.above())
                        .isEmpty()
                && !level().getBlockState(feet.below())
                        .getCollisionShape(level(), feet.below())
                        .isEmpty();
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
            showReaction(NpcReactionIcon.CONFUSED, 45);
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

    private void tickPlayerLikeGapJumping() {
        if (!onGround()
                || isInWaterOrBubble()
                || isPassenger()
                || isSleeping()
                || isSpellCastingVisual()
                || getNavigation().isDone()) {
            return;
        }

        Path path = getNavigation().getPath();
        if (path == null || path.isDone() || path.getTarget() == null) {
            return;
        }

        Vec3 destination = Vec3.atCenterOf(path.getTarget());
        Vec3 direction = new Vec3(
                destination.x - getX(),
                0.0D,
                destination.z - getZ()
        );

        if (direction.lengthSqr() < 2.25D) {
            return;
        }

        direction = direction.normalize();
        int maxGap = isSprinting()
                ? SPRINT_GAP_JUMP_BLOCKS
                : WALK_GAP_JUMP_BLOCKS;

        GapJumpPlan plan = findJumpableGap(direction, maxGap);
        if (plan == null) {
            return;
        }

        boolean sprintJump = isSprinting();
        double horizontalSpeed = sprintJump ? 0.36D : 0.23D;
        Vec3 existing = getDeltaMovement();
        double existingHorizontal = Math.sqrt(
                existing.x * existing.x + existing.z * existing.z
        );
        horizontalSpeed = Math.max(horizontalSpeed, existingHorizontal);

        jumpFromGround();
        Vec3 jumped = getDeltaMovement();
        setDeltaMovement(
                plan.direction.x * horizontalSpeed,
                jumped.y,
                plan.direction.z * horizontalSpeed
        );

        setYRot((float) (Math.atan2(plan.direction.z, plan.direction.x)
                * (180.0D / Math.PI)) - 90.0F);
        setYHeadRot(getYRot());
        gapJumpCooldown = GAP_JUMP_COOLDOWN_TICKS;
    }

    @Nullable
    private GapJumpPlan findJumpableGap(Vec3 direction, int maxGapBlocks) {
        BlockPos origin = blockPosition();
        BlockPos previous = origin;
        boolean enteredGap = false;
        int gapBlocks = 0;

        for (double travel = 0.75D;
             travel <= maxGapBlocks + 1.75D;
             travel += 0.35D) {
            BlockPos sample = BlockPos.containing(
                    getX() + direction.x * travel,
                    getY() + 0.05D,
                    getZ() + direction.z * travel
            );

            if (sample.equals(previous)) {
                continue;
            }
            previous = sample;

            // Only jump level gaps. Height changes remain normal pathfinding's
            // job so this never becomes an unintended super-jump.
            if (sample.getY() != origin.getY()
                    || !isGapJumpBodyClear(sample)) {
                return null;
            }

            boolean supported = hasGapJumpSupport(sample);
            if (!enteredGap) {
                if (supported) {
                    return null;
                }
                enteredGap = true;
                gapBlocks = 1;
                continue;
            }

            if (!supported) {
                gapBlocks++;
                if (gapBlocks > maxGapBlocks) {
                    return null;
                }
                continue;
            }

            return gapBlocks >= 1
                    ? new GapJumpPlan(direction, sample.immutable(), gapBlocks)
                    : null;
        }

        return null;
    }

    private boolean isGapJumpBodyClear(BlockPos feet) {
        if (isTrapdoorBlock(feet)
                || isTrapdoorBlock(feet.above())
                || isTrapdoorBlock(feet.below())) {
            return false;
        }

        return level().getFluidState(feet).isEmpty()
                && level().getFluidState(feet.above()).isEmpty()
                && level().getBlockState(feet)
                        .getCollisionShape(level(), feet)
                        .isEmpty()
                && level().getBlockState(feet.above())
                        .getCollisionShape(level(), feet.above())
                        .isEmpty();
    }

    private boolean hasGapJumpSupport(BlockPos feet) {
        BlockPos support = feet.below();

        if (isTrapdoorBlock(feet)
                || isTrapdoorBlock(support)) {
            return false;
        }

        return level().getFluidState(support).isEmpty()
                && !level().getBlockState(support)
                        .getCollisionShape(level(), support)
                        .isEmpty();
    }

    private boolean isTrapdoorBlock(BlockPos pos) {
        return level().getBlockState(pos).getBlock()
                instanceof TrapDoorBlock;
    }

    private void tickHunger() {
        hungerDecayTimer++;

        if (hungerDecayTimer >= HUNGER_DECAY_TICKS) {
            hungerDecayTimer = 0;
            if (getHunger() > 0) {
                setHunger(getHunger() - 1);

                if (getHunger() == HUNT_HUNGER_THRESHOLD) {
                    showReaction(NpcReactionIcon.FOOD, 45);
                }
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

        if (target instanceof Player player
                && (player.isCreative() || player.isSpectator())) {
            calmWildNpc();
            return;
        }

        if (tickCount % COMBAT_TARGET_REFRESH_INTERVAL == 0) {
            LivingEntity closer = huntingTarget
                    ? findClosestHuntTarget(target)
                    : findClosestActiveThreat(target);

            if (closer != null && closer != target) {
                setTarget(closer);
                target = closer;
                outOfRangeTicks = 0;
            }
        }

        if (huntingTarget) {
            target = maybeSwitchHuntTarget(target);
        } else if (target instanceof Mob hostile && hostile instanceof Enemy) {
            CombatConfidence solo = evaluateCombatConfidence(hostile, false);
            CombatConfidence group = evaluateCombatConfidence(hostile, true);
            float healthFraction = getMaxHealth() <= 0.0F
                    ? 0.0F
                    : getHealth() / getMaxHealth();

            if (combatHelpCooldown <= 0
                    && group.helpers > 0
                    && (solo.groupScore < FIGHT_CONFIDENCE
                    || healthFraction <= getHelpHealthFraction())) {
                alertNearbyWildNpcs(target, MAX_COMBAT_HELPERS);
                combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;
            }

            boolean critical = healthFraction <= 0.18F;
            if (group.groupScore < FLEE_CONFIDENCE
                    || (critical && group.groupScore < HELP_CONFIDENCE)) {
                startFleeingFrom(hostile);
                return;
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
        if (isCombatActive() || socialConversationHoldTicks > 0) {
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

        if (prey instanceof Mob mob) {
            CombatConfidence confidence = evaluateCombatConfidence(mob, true);
            if (confidence.groupScore < HELP_CONFIDENCE) {
                return false;
            }
        }

        float confidenceHealth = getHealth() + 6.0F;
        float maximumPreyHealth = getMaxHealth() * 1.5F;

        return prey.getHealth() <= confidenceHealth
                && prey.getMaxHealth() <= maximumPreyHealth;
    }

    private LivingEntity maybeSwitchHuntTarget(
            LivingEntity current
    ) {
        if (huntTargetRecheckCooldown > 0) {
            huntTargetRecheckCooldown--;
            return current;
        }

        huntTargetRecheckCooldown =
                HUNT_TARGET_RECHECK_INTERVAL;

        LivingEntity closest = findClosestHuntTarget(current);
        if (closest != null && closest != current) {
            setTarget(closest);
            return closest;
        }

        return current;
    }

    @Nullable
    private LivingEntity findClosestHuntTarget(
            LivingEntity current
    ) {
        LivingEntity closest = level().getEntitiesOfClass(
                        LivingEntity.class,
                        getBoundingBox().inflate(HUNT_RADIUS),
                        this::canHuntPrey
                ).stream()
                .filter(candidate ->
                        distanceToSqr(candidate) <= 9.0D
                                || getSensing().hasLineOfSight(candidate)
                )
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (closest == null) {
            return current;
        }

        if (current == null
                || !current.isAlive()
                || !canHuntPrey(current)
                || !getSensing().hasLineOfSight(current)
                || distanceToSqr(closest)
                + COMBAT_TARGET_SWITCH_ADVANTAGE_SQR
                < distanceToSqr(current)) {
            return closest;
        }

        return current;
    }

    @Nullable
    private LivingEntity findClosestActiveThreat(
            LivingEntity current
    ) {
        LivingEntity closestHostile = level()
                .getEntitiesOfClass(
                        Mob.class,
                        getBoundingBox().inflate(
                                THREAT_SCAN_RADIUS,
                                10.0D,
                                THREAT_SCAN_RADIUS
                        ),
                        mob -> mob != this
                                && mob.isAlive()
                                && mob instanceof Enemy
                                && mob.getTarget() == this
                ).stream()
                .filter(mob ->
                        distanceToSqr(mob) <= 9.0D
                                || getSensing().hasLineOfSight(mob)
                )
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        if (closestHostile == null) {
            return current;
        }

        if (current == null
                || !current.isAlive()
                || !getSensing().hasLineOfSight(current)
                || distanceToSqr(closestHostile)
                + COMBAT_TARGET_SWITCH_ADVANTAGE_SQR
                < distanceToSqr(current)) {
            return closestHostile;
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
            if (fleeingThreat instanceof Mob mob
                    && tickCount % THREAT_SCAN_INTERVAL == 0
                    && mob.isAlive()) {
                CombatConfidence recovered =
                        evaluateCombatConfidence(mob, true);

                // Fleeing is a last resort. If the fight becomes clearly
                // manageable again (for example backup arrived or the mob was
                // badly hurt), re-engage instead of running forever.
                if (recovered.groupScore >= FIGHT_CONFIDENCE
                        && getHealth() > getMaxHealth() * 0.20F) {
                    fleeingThreat = null;
                    fleeSafeTicks = 0;
                    fleeRepathCooldown = 0;
                    beginWildCombat(mob, false, false);
                    return false;
                }
            }

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

        CombatConfidence solo = evaluateCombatConfidence(threat, false);
        CombatConfidence group = evaluateCombatConfidence(threat, true);
        float healthFraction = getMaxHealth() <= 0.0F
                ? 0.0F
                : getHealth() / getMaxHealth();
        boolean criticalHealth = healthFraction <= 0.18F;

        if (solo.groupScore >= FIGHT_CONFIDENCE && !criticalHealth) {
            if (!isCombatActive() || huntingTarget || getTarget() != threat) {
                beginWildCombat(threat, false, false);
            }
            return false;
        }

        // Uncertain NPCs look for nearby backup before considering escape.
        if (group.helpers > 0
                && solo.groupScore < FIGHT_CONFIDENCE
                && combatHelpCooldown <= 0) {
            int helpers = alertNearbyWildNpcs(threat, MAX_COMBAT_HELPERS);
            combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;

            if (helpers > 0
                    && group.groupScore >= HELP_CONFIDENCE
                    && !(criticalHealth && group.groupScore < FIGHT_CONFIDENCE)) {
                beginWildCombat(threat, false, false);
                return false;
            }
        }

        // Running is deliberately the final option. A merely unfavorable fight
        // is still taken; fleeing is reserved for genuinely bad odds or
        // critically low health without enough support.
        if (!criticalHealth && group.groupScore >= FLEE_CONFIDENCE) {
            if (!isCombatActive() || huntingTarget || getTarget() != threat) {
                beginWildCombat(threat, false, false);
            }
            return false;
        }

        startFleeingFrom(threat);

        if (hasReadyFood()
                && distanceToSqr(threat) >= EMERGENCY_EAT_SAFE_DISTANCE_SQR) {
            beginEmergencyEating();
            if (emergencyEating) {
                tickEating();
                return true;
            }
        }

        tickFleeing();
        return true;
    }

    private CombatConfidence evaluateCombatConfidence(
            Mob threat,
            boolean includeNearbyBackup
    ) {
        MobAggroClass aggroClass = classifyMobAggro(threat);
        double npcPower = estimateOwnCombatPower();
        double threatPower = estimateThreatPower(threat, aggroClass);

        int helpers = 0;
        double backupPower = 0.0D;

        if (includeNearbyBackup) {
            List<CyberNpcEntity> nearby = getPotentialCombatHelpers(threat);
            helpers = nearby.size();

            for (CyberNpcEntity helper : nearby) {
                int support = helper.socialMemory.supportScore(getUUID());
                double weight = isSameParty(helper)
                        ? 0.85D
                        : Mth.clamp(0.48D + support / 200.0D, 0.40D, 0.75D);

                backupPower += helper.estimateOwnCombatPower() * weight;
            }
        }

        double totalPower = npcPower + backupPower;
        double ratio = totalPower / Math.max(1.0D, threatPower);
        double score = 50.0D + (ratio - 1.0D) * 38.0D;

        float healthFraction = getMaxHealth() <= 0.0F
                ? 0.0F
                : getHealth() / getMaxHealth();
        score += (healthFraction - 0.50D) * 18.0D;

        score += getPersonality().confidenceModifier();

        if (isWildClass("knight")) {
            score += 5.0D;
        } else if (isWildClass("berserker")) {
            score += 8.0D;
        } else if (isWildClass("spellblade")) {
            score += 3.0D;
        } else if (wildClassUsesSpellBook()
                && IronSpellsCompat.hasActiveCast(this)) {
            score += 3.0D;
        }

        score = Mth.clamp(score, 0.0D, 100.0D);
        return new CombatConfidence(
                score,
                helpers,
                aggroClass,
                estimatePotentialDamage(),
                estimateMobAttackDamage(threat)
        );
    }

    private List<CyberNpcEntity> getPotentialCombatHelpers(LivingEntity threat) {
        Comparator<CyberNpcEntity> priority = Comparator
                .comparingInt((CyberNpcEntity npc) -> isSameParty(npc) ? 0 : 1)
                .thenComparing(
                        Comparator.comparingInt(
                                (CyberNpcEntity npc) ->
                                        npc.socialMemory.supportScore(getUUID())
                        ).reversed()
                )
                .thenComparingDouble(this::distanceToSqr);

        return level().getEntitiesOfClass(
                        CyberNpcEntity.class,
                        getBoundingBox().inflate(WILD_HELP_RADIUS),
                        npc -> npc != this
                                && npc.isAlive()
                                && npc.getNpcType() == NpcType.WILD
                                && !npc.isSleeping()
                                && npc.fleeingThreat == null
                                && npc.getHealth() >= Math.max(
                                        6.0F,
                                        npc.getMaxHealth() * 0.30F
                                )
                                && npc.getTarget() != threat
                                && npc.willHelpNpc(this, threat)
                ).stream()
                .sorted(priority)
                .limit(MAX_COMBAT_HELPERS)
                .toList();
    }

    private boolean willHelpNpc(
            CyberNpcEntity caller,
            LivingEntity threat
    ) {
        if (caller == null || caller == this) {
            return false;
        }

        if (isSameParty(caller) || areMutualFriends(caller)) {
            return true;
        }

        int support = socialMemory.supportScore(caller.getUUID());
        int threshold = switch (getPersonality()) {
            case PROTECTIVE, LOYAL, BRAVE -> 0;
            case RECKLESS -> 2;
            case TACTICAL, BALANCED -> 7;
            case AGGRESSIVE -> 8;
            case STUBBORN, PATIENT -> 10;
            case OPPORTUNISTIC -> 14;
            case CAUTIOUS -> 18;
            case SKITTISH -> 24;
        };

        if (threat instanceof Mob mob && mob.getTarget() == caller
                && (getPersonality() == WildNpcPersonality.PROTECTIVE
                || getPersonality() == WildNpcPersonality.LOYAL)) {
            threshold -= 6;
        }

        return support >= threshold;
    }

    private MobAggroClass classifyMobAggro(Mob mob) {
        if (mob instanceof Enemy) {
            return MobAggroClass.AGGRESSIVE;
        }
        if (mob instanceof NeutralMob) {
            return MobAggroClass.NEUTRAL;
        }
        return MobAggroClass.PASSIVE;
    }

    private double estimateThreatPower(
            Mob threat,
            MobAggroClass aggroClass
    ) {
        double attackDamage = estimateMobAttackDamage(threat);
        double power = Math.max(1.0D, threat.getHealth())
                + attackDamage * 3.0D;

        power *= switch (aggroClass) {
            case PASSIVE -> 0.65D;
            case NEUTRAL -> 0.90D;
            case AGGRESSIVE -> 1.15D;
        };

        if (threat.getTarget() == this) {
            power *= 1.10D;
        }

        if (threat.getType() == EntityType.WARDEN) {
            // Wardens are intentionally exceptional threats. Their enormous
            // health, melee damage and sonic attack should overwhelm normal
            // "backup makes me brave" behaviour.
            power *= 2.50D;
        }

        return power;
    }

    private double estimateMobAttackDamage(Mob threat) {
        var attackAttribute = threat.getAttribute(Attributes.ATTACK_DAMAGE);
        double attackDamage = attackAttribute == null
                ? 0.0D
                : attackAttribute.getValue();

        if (threat instanceof Creeper) {
            return Math.max(18.0D, attackDamage);
        }
        if (threat instanceof Enemy) {
            return Math.max(4.0D, attackDamage);
        }
        if (threat instanceof NeutralMob) {
            return Math.max(3.0D, attackDamage);
        }
        return Math.max(1.0D, attackDamage);
    }

    private double estimateOwnCombatPower() {
        return Math.max(1.0D, getHealth())
                + estimatePotentialDamage() * 3.0D;
    }

    private double estimatePotentialDamage() {
        double base = Math.max(2.0D, getAttributeValue(Attributes.ATTACK_DAMAGE));
        double tierBonus = getGearTier().attackBonus();

        return switch (getWildClass()) {
            case "knight" -> Math.max(base, 8.0D + tierBonus);
            case "rogue" -> Math.max(base, 7.0D + tierBonus);
            case "berserker" -> Math.max(base, 10.0D + tierBonus);
            case "archer" -> Math.max(base, 6.5D + tierBonus);
            case "mage", "cleric", "spellblade", "druid", "bard" -> Math.max(
                    base,
                    IronSpellsCompat.estimatePotentialDamage(
                            this,
                            getMageSpellBook()
                    )
            );
            default -> Math.max(
                    base,
                    (getStoredSword().isEmpty() ? 3.5D : 6.0D) + tierBonus
            );
        };
    }

    @Nullable
    private CyberNpcEntity findMostInjuredFriendly(double radius) {
        double threshold = Math.max(
                0.58D,
                Math.min(0.90D, getHelpHealthFraction())
        );

        Comparator<CyberNpcEntity> priority = Comparator
                .comparingInt((CyberNpcEntity npc) -> isSameParty(npc) ? 0 : 1)
                .thenComparingDouble(
                        npc -> npc.getHealth() / npc.getMaxHealth()
                )
                .thenComparing(
                        Comparator.comparingInt(
                                (CyberNpcEntity npc) ->
                                        socialMemory.supportScore(npc.getUUID())
                        ).reversed()
                );

        return level().getEntitiesOfClass(
                        CyberNpcEntity.class,
                        getBoundingBox().inflate(radius),
                        npc -> npc != this
                                && isFriendlyWildNpc(npc)
                                && npc.isAlive()
                                && npc.getMaxHealth() > 0.0F
                                && npc.getHealth() / npc.getMaxHealth() < threshold
                                && (isSameParty(npc)
                                || socialMemory.supportScore(npc.getUUID()) >= 0)
                ).stream()
                .sorted(priority)
                .findFirst()
                .orElse(null);
    }

    private void startFleeingFrom(LivingEntity threat) {
        if (getVehicle() instanceof AbstractHorse horse) {
            stopUsingHorse(horse);
        } else {
            clearHorseTarget();
        }

        sleepBrain.interrupt();
        corralBrain.interrupt();
        stopUsingItem();
        clearUtilityItem();
        cancelMageCast();
        setTarget(null);
        setCombatActive(false);
        huntingTarget = false;
        stowWeapons();
        fleeingThreat = threat;
        showReaction(NpcReactionIcon.SCARED, 55);
        fleeSafeTicks = 0;
        fleeRepathCooldown = 0;
        getNavigation().stop();
        setShiftKeyDown(false);
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

        fleeRepathCooldown = 4;

        BlockPos shelter = findNearbyShelter(fleeingThreat);
        if (shelter != null) {
            Vec3 shelterSpot = Vec3.atBottomCenterOf(shelter);
            if (distanceToSqr(shelterSpot) > 3.0D) {
                getNavigation().moveTo(shelterSpot.x, shelterSpot.y, shelterSpot.z, 1.0D);
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
                    navigateTowardPersistentClaim(bed, 1.0D);
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
            getNavigation().moveTo(away.x, away.y, away.z, 1.0D);
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
        if (level().isClientSide
                || Math.floorMod(
                tickCount + getUUID().hashCode(),
                DEBUG_SYNC_INTERVAL
        ) != 0) {
            return;
        }

        entityData.set(DATA_DEBUG_ACTIVITY, buildDebugActivity());
        entityData.set(DATA_DEBUG_REASON, buildDecisionReason());
        entityData.set(DATA_DEBUG_INTENTION, intentions.debugSummary());

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

        LivingEntity debugThreat = getTarget() != null
                ? getTarget()
                : fleeingThreat;

        if (debugThreat instanceof Mob mob) {
            CombatConfidence solo = evaluateCombatConfidence(mob, false);
            CombatConfidence group = evaluateCombatConfidence(mob, true);
            entityData.set(
                    DATA_DEBUG_CONFIDENCE,
                    String.format(
                            "%.0f%% group / %.0f%% solo | %s | backup:%d | dmg %.1f vs %.1f",
                            group.groupScore,
                            solo.groupScore,
                            group.aggroClass.displayName,
                            group.helpers,
                            group.npcDamage,
                            group.mobDamage
                    )
            );
        } else {
            entityData.set(DATA_DEBUG_CONFIDENCE, "none");
        }

        entityData.set(DATA_DEBUG_SPELLS, buildMageSpellDebug());
        entityData.set(DATA_DEBUG_PARTY, buildPartyDebug());
        entityData.set(
                DATA_DEBUG_RELATIONSHIPS,
                buildRelationshipDebug()
        );

        entityData.set(
                DATA_DEBUG_INVENTORY,
                inventory.debugSummary()
                        + (foodToEat.isEmpty()
                        ? ""
                        : " | using:" + stackDebug(foodToEat))
        );
    }

    private String buildPartyDebug() {
        if (!socialMemory.hasParty()) {
            return "none";
        }

        String partyId = socialMemory.partyId().toString();
        partyId = partyId.substring(0, Math.min(8, partyId.length()));

        String leaderName = shortUuid(socialMemory.partyLeaderId());
        if (level() instanceof ServerLevel serverLevel
                && socialMemory.partyLeaderId() != null) {
            var leader = serverLevel.getEntity(socialMemory.partyLeaderId());
            if (leader != null) {
                leaderName = leader.getName().getString();
            }
        }

        return (socialMemory.isPartyLeader(getUUID()) ? "Leader" : "Member")
                + " | party:" + partyId
                + " | leader:" + leaderName
                + " | loaded:" + getLoadedPartySize() + "/" + PARTY_MAX_SIZE;
    }

    private String buildRelationshipDebug() {
        List<String> lines = new ArrayList<>();

        if (level() instanceof ServerLevel serverLevel) {
            for (NpcSocialMemory.RelationshipEntry entry :
                    socialMemory.relationshipEntries().stream().limit(4).toList()) {
                NpcSocialMemory.RelationshipSnapshot relation =
                        entry.relationship();

                var entity = serverLevel.getEntity(entry.targetId());
                String name = entity == null
                        ? shortUuid(entry.targetId())
                        : entity.getName().getString();

                lines.add(
                        "NPC " + name
                                + (relation.friend() ? " [Friend]" : "")
                                + " | F:" + relation.friendship()
                                + " T:" + relation.trust()
                                + " R:" + relation.respect()
                                + " Fear:" + relation.fear()
                                + " Rival:" + relation.rivalry()
                );
            }

            for (NpcSocialMemory.PlayerReputationEntry entry :
                    socialMemory.playerReputationEntries().stream().limit(3).toList()) {
                var player = serverLevel.getServer()
                        .getPlayerList()
                        .getPlayer(entry.playerId());

                String name = player == null
                        ? shortUuid(entry.playerId())
                        : player.getGameProfile().getName();

                lines.add(
                        "Player " + name
                                + " | Rep:"
                                + (entry.reputation() >= 0 ? "+" : "")
                                + entry.reputation()
                );
            }
        }

        return lines.isEmpty() ? "none" : String.join("|", lines);
    }

    private static String shortUuid(@Nullable UUID id) {
        if (id == null) {
            return "none";
        }

        String value = id.toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    boolean canHoldPlayerLikeIntention() {
        return getNpcType() == NpcType.WILD
                && isAlive()
                && !isCombatActive()
                && getTarget() == null
                && fleeingThreat == null
                && suspiciousNpc == null
                && !isSleeping()
                && !isZombifying()
                && !isSpellCastingVisual()
                && getHunger() > HUNT_HUNGER_THRESHOLD
                && foodToEat.isEmpty()
                && cookingMode == COOK_MODE_NONE
                && !emergencyEating
                && !corralBrain.isBusy()
                && !sleepBrain.isBusy()
                && socialConversationHoldTicks <= 0
                && !regroupingWithParty
                && horseTargetId == null
                && !(getVehicle() instanceof AbstractHorse);
    }

    boolean canReactToEnvironment() {
        return canHoldPlayerLikeIntention()
                && !isPassenger()
                && !corralBrain.isBusy()
                && !sleepBrain.isBusy()
                && socialConversationHoldTicks <= 0;
    }

    boolean canStartPlayerLikeLifeActivity() {
        return canReactToEnvironment()
                && canWander()
                && getHunger() > 14
                && dropSearchTicks <= 0
                && groundFoodTargetId == null
                && foodChestTarget == null
                && !regroupingWithParty
                && horseTargetId == null;
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
        if (isSpellCastingVisual()) {
            return "Casting " + getCastingSpellId();
        }
        if (isSleeping()) {
            return "Sleeping";
        }
        if (getVehicle() instanceof AbstractHorse horse) {
            if (isWildClass("ranger")) {
                return rangerReturningHomeForSleep
                        ? "Ranger returning home"
                        : (rangerPatrolPauseTicks > 0
                        ? "Ranger watching perimeter"
                        : "Ranger patrolling perimeter");
            }

            return (horse.isSprinting()
                    ? "Sprinting on horse — "
                    : "Riding horse — ")
                    + horse.getName().getString();
        }
        if (horseTargetId != null) {
            return "Approaching horse";
        }
        if (isWildClass("ranger")
                && rangerPatrolPauseTicks > 0) {
            return "Ranger watching perimeter";
        }
        if (isWildClass("ranger")
                && rangerPatrolTarget != null) {
            return "Ranger patrolling perimeter";
        }
        if (sculkSneaking) {
            return "Sneaking near visible sculk sensor";
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
        if (socialConversationHoldTicks > 0) {
            return pendingPartyInviteFrom != null
                    ? "Considering party invite"
                    : "Socializing";
        }
        if (regroupingWithParty) {
            return "Regrouping with party";
        }
        if (environmentBrain.ownsCurrentIntention()) {
            return environmentBrain.getActivity();
        }
        if (attentionBrain.ownsCurrentIntention()) {
            return attentionBrain.getActivity();
        }
        if (!getNavigation().isDone()) {
            return "Travelling";
        }
        return canWander() ? "Wandering/idle" : "Idle";
    }

    private String buildDecisionReason() {
        if (isZombifying()) {
            return "Infection overrides normal routines until conversion resolves";
        }
        if (suspiciousNpc != null && infectionAvoidTicks > 0) {
            return "Avoiding a nearby NPC that may spread zombification";
        }
        if (fleeingThreat != null) {
            return "Threat confidence is too low to keep fighting safely";
        }
        if (isCombatActive()) {
            return huntingTarget
                    ? "Selected this prey because food is needed and the fight is considered safe"
                    : "Combat has priority because this NPC or an ally is under threat";
        }
        if (isSleeping()) {
            return "Nighttime rest is active and a claimed bed is available";
        }
        if (sleepBrain.isBusy()) {
            return "It is night and this NPC is committed to reaching its claimed bed";
        }
        if (getVehicle() instanceof AbstractHorse horse) {
            if (isWildClass("ranger")
                    && rangerReturningHomeForSleep) {
                return "Night is approaching, so the Ranger is riding back from the perimeter before sleeping";
            }

            if (isWildClass("ranger")) {
                return rangerPatrolPauseTicks > 0
                        ? "The Ranger reached a perimeter point and is watching the surrounding area before continuing"
                        : "The Ranger is checking the local village/home perimeter instead of wandering far away";
            }

            if (horse.isSprinting()) {
                return "Mounted destination is far enough away to make sprint travel worthwhile";
            }

            CyberNpcEntity leader = getLoadedPartyLeader();
            if (leader != null
                    && distanceToSqr(leader)
                    > PARTY_FOLLOW_DISTANCE * PARTY_FOLLOW_DISTANCE) {
                return "Using the owned horse to catch up with the party";
            }
            if (horseRideTicks > 0) {
                return "Ranger chose mounted travel and is still committed to that decision";
            }
            return "Ranger prefers to remain mounted while no higher-priority need exists";
        }
        if (horseTargetId != null) {
            AbstractHorse horse = findLoadedHorse(horseTargetId);
            if (horse != null && !horse.isTamed()) {
                return "Found an adult unowned horse and has an Upgraded Saddle available";
            }
            return "Returning to an owned horse for efficient travel";
        }
        if (horseRemountCooldown > 0
                && isWildClass("ranger")) {
            return "Recently dismounted for another need; delaying remount while continuing local Ranger duties on foot";
        }
        if (isWildClass("ranger")
                && rangerPatrolPauseTicks > 0) {
            return "The Ranger reached a perimeter point and is pausing to watch the surrounding area";
        }
        if (isWildClass("ranger")
                && rangerPatrolTarget != null) {
            return "The Ranger is checking the local home or village perimeter and stays within a bounded patrol area";
        }
        if (!foodToEat.isEmpty()) {
            return "Prepared food is available and hunger should be restored before lower-priority tasks";
        }
        if (cookingMode != COOK_MODE_NONE || !getRawFoodStack().isEmpty()) {
            return "Raw food is available, so preparing it is more useful than wandering";
        }
        if (foodChestTarget != null || groundFoodTargetId != null) {
            return "Nearby food can satisfy hunger without starting a new hunt";
        }
        if (corralBrain.isBusy()) {
            return "Livestock work is already in progress and remains the current commitment";
        }
        if (getHunger() <= HUNT_HUNGER_THRESHOLD) {
            return "Hunger has crossed the hunting threshold, so food becomes the main routine need";
        }
        if (socialConversationHoldTicks > 0) {
            return "No urgent survival need is active and a social interaction has already started";
        }
        if (regroupingWithParty) {
            return "Maintaining party cohesion has priority over wandering alone";
        }
        if (environmentBrain.ownsCurrentIntention()) {
            return environmentBrain.getReason();
        }
        if (attentionBrain.ownsCurrentIntention()) {
            return attentionBrain.getReason();
        }
        if (!getNavigation().isDone()) {
            return "Continuing the current route instead of selecting a new activity every tick";
        }
        return canWander()
                ? "No higher-priority need is active, so local exploration is reasonable"
                : "No higher-priority need is active and wandering is disabled";
    }

    private String buildMageSpellDebug() {
        if (!wildClassUsesSpellBook()) {
            return "No spellbook class";
        }

        ItemStack book = getMageSpellBook();
        if (book.isEmpty()) {
            return "No spellbook";
        }

        List<IronSpellsCompat.SpellEntry> spells =
                IronSpellsCompat.getBookSpells(book);
        if (spells.isEmpty()) {
            return "Spellbook empty";
        }

        return spells.stream()
                .map(entry -> {
                    String id = entry.spellId();
                    String path = id.contains(":")
                            ? id.substring(id.indexOf(':') + 1)
                            : id;
                    String name = humanizeSpellName(path);
                    int cooldown = IronSpellsCompat.getRemainingCooldownTicks(
                            this,
                            id
                    );
                    String state = cooldown <= 0
                            ? "ready"
                            : String.format("cd %.1fs", cooldown / 20.0D);

                    return name
                            + " L" + entry.level()
                            + " [" + IronSpellsCompat.getSpellRoleName(id) + "] "
                            + state;
                })
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private static String humanizeSpellName(String path) {
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
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
                || socialConversationHoldTicks > 0
                || horseTargetId != null
                || getVehicle() instanceof AbstractHorse
                || intentions.isActive()
                || (suspiciousNpc != null && infectionAvoidTicks > 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof CyberNpcEntity attacker
                && isFriendlyWildNpc(attacker)) {
            return false;
        }

        boolean damaged = super.hurt(source, amount);

        if (!damaged || level().isClientSide || getNpcType() != NpcType.WILD) {
            return damaged;
        }

        intentions.clear();
        sleepBrain.wakeUp();
        vocalizations.hurt(amount);

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

            int previousReputation =
                    socialMemory.getPlayerReputation(player.getUUID());
            socialMemory.adjustPlayerReputation(
                    player.getUUID(),
                    -Math.max(4, Mth.ceil(amount * 2.0F))
            );

            showReaction(
                    amount >= 5.0F || previousReputation < -25
                            ? NpcReactionIcon.ANGRY
                            : NpcReactionIcon.ANNOYED,
                    60
            );

            notifySocialWitnessesOfPlayerAttack(player, false);

            if (!isCombatActive() || huntingTarget) {
                int reputationModifier =
                        Math.max(0, -previousReputation / 5)
                                - Math.max(0, previousReputation / 12);
                int addedProvocation = Math.max(
                        4,
                        Mth.ceil(amount * 6.0F) + reputationModifier
                );
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

    private void notifySocialWitnessesOfPlayerAttack(
            Player player,
            boolean fatal
    ) {
        List<CyberNpcEntity> witnesses = level().getEntitiesOfClass(
                CyberNpcEntity.class,
                getBoundingBox().inflate(WILD_HELP_RADIUS),
                npc -> npc != this
                        && npc.isAlive()
                        && npc.getNpcType() == NpcType.WILD
                        && (isSameParty(npc)
                        || npc.isFriendWith(this)
                        || npc.socialMemory.supportScore(getUUID()) >= 8
                        || CyberRacesNpcCompat.strongSameRaceBond(
                                npc,
                                this
                        ))
        );

        for (CyberNpcEntity witness : witnesses) {
            boolean closeBond = isSameParty(witness)
                    || witness.isFriendWith(this);
            boolean racialKinship =
                    CyberRacesNpcCompat.strongSameRaceBond(
                            witness,
                            this
                    );

            int penalty = fatal
                    ? (closeBond ? -28 : racialKinship ? -10 : -18)
                    : (closeBond ? -8 : racialKinship ? -3 : -4);

            witness.socialMemory.adjustPlayerReputation(
                    player.getUUID(),
                    penalty
            );

            witness.showReaction(
                    fatal || witness.isSameParty(this)
                            ? NpcReactionIcon.ANGRY
                            : racialKinship
                            ? CyberRacesNpcCompat.encounterReaction(
                                    witness,
                                    this,
                                    0
                            )
                            : NpcReactionIcon.ANNOYED,
                    fatal ? 80 : 55
            );
        }
    }

    private boolean isFriendlyWildNpc(CyberNpcEntity other) {
        return other != null
                && other != this
                && getNpcType() == NpcType.WILD
                && other.getNpcType() == NpcType.WILD
                && getTarget() != other
                && other.getTarget() != this;
    }

    public boolean hasClearFriendlyFireLane(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        Vec3 start = getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();

        if (lengthSqr < 0.0001D) {
            return true;
        }

        AABB corridor = new AABB(start, end).inflate(1.35D);
        List<CyberNpcEntity> allies = level().getEntitiesOfClass(
                CyberNpcEntity.class,
                corridor,
                this::isFriendlyWildNpc
        );

        for (CyberNpcEntity ally : allies) {
            Vec3 center = ally.getBoundingBox().getCenter();
            double t = Mth.clamp(
                    center.subtract(start).dot(segment) / lengthSqr,
                    0.0D,
                    1.0D
            );

            if (t <= 0.05D || t >= 0.95D) {
                continue;
            }

            Vec3 closest = start.add(segment.scale(t));
            double safeRadius = ally.getBbWidth() * 0.5D + 0.65D;

            if (center.distanceToSqr(closest) <= safeRadius * safeRadius) {
                return false;
            }
        }

        return true;
    }

    private void beginWildCombat(LivingEntity target, boolean callForHelp, boolean isHunt) {
        if (isZombifying() && target instanceof Enemy) {
            return;
        }

        if (getVehicle() instanceof AbstractHorse horse) {
            stopUsingHorse(horse);
        } else {
            clearHorseTarget();
        }

        fleeingThreat = null;
        fleeSafeTicks = 0;
        fleeRepathCooldown = 0;
        sleepBrain.interrupt();
        corralBrain.interrupt();
        intentions.clear();
        clearUtilityItem();
        setTarget(target);
        setCombatActive(true);
        huntingTarget = isHunt;
        provocation = isHunt ? provocation : 100;
        outOfRangeTicks = 0;
        getNavigation().stop();

        if (!isHunt) {
            showReaction(NpcReactionIcon.COMBAT, 45);
        }

        if (callForHelp) {
            alertNearbyWildNpcs(target, MAX_COMBAT_HELPERS);
            combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;
        }
    }

    private int alertNearbyWildNpcs(LivingEntity target, int maxHelpers) {
        List<CyberNpcEntity> nearbyWildNpcs = getPotentialCombatHelpers(target)
                .stream()
                .limit(Math.max(0, maxHelpers))
                .toList();

        for (CyberNpcEntity npc : nearbyWildNpcs) {
            npc.ensureWildProfile();
            npc.huntingTarget = false;
            npc.dropSearchTicks = 0;
            npc.cookingTarget = null;
            npc.combatHelpCooldown = COMBAT_HELP_COOLDOWN_TICKS;

            npc.socialMemory.adjustRelationship(
                    getUUID(),
                    1,
                    2,
                    2,
                    -1,
                    -1
            );
            socialMemory.adjustRelationship(
                    npc.getUUID(),
                    1,
                    2,
                    2,
                    -1,
                    -1
            );

            npc.beginWildCombat(target, false, false);
        }

        return nearbyWildNpcs.size();
    }

    private void calmWildNpc() {
        cancelMageCast();
        setTarget(null);
        setCombatActive(false);
        huntingTarget = false;
        stowWeapons();
        getNavigation().stop();
        provocation = 0;
        outOfRangeTicks = 0;
    }

    private boolean fireAdaptiveArrow(
            LivingEntity target,
            float speed,
            float inaccuracy,
            double baseDamage
    ) {
        if (!hasClearFriendlyFireLane(target)) {
            return false;
        }

        Arrow arrow = new Arrow(level(), this);

        Vec3 targetVelocity = target.getDeltaMovement();
        double directDistance = position().distanceTo(target.position());
        double flightTicks = Mth.clamp(
                directDistance / Math.max(speed, 0.1F),
                1.0D,
                30.0D
        );

        Vec3 predictedTarget = target.position()
                .add(targetVelocity.scale(flightTicks * 0.85D));

        double dx = predictedTarget.x - getX();
        double dz = predictedTarget.z - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double targetY = predictedTarget.y + target.getBbHeight() * 0.45D;

        double gravityCompensation =
                0.5D * 0.05D * flightTicks * flightTicks;
        double dy = targetY - arrow.getY() + gravityCompensation;

        arrow.setBaseDamage(baseDamage);
        arrow.shoot(dx, dy, dz, speed, inaccuracy);
        level().addFreshEntity(arrow);
        return true;
    }

    public void completeZombification() {
        if (zombieConversionStarted
                || level().isClientSide
                || !isAlive()
                || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        zombieConversionStarted = true;

        if (getVehicle() instanceof AbstractHorse horse) {
            stopUsingHorse(horse);
        } else {
            clearHorseTarget();
        }
        corralBrain.interrupt();
        sleepBrain.wakeUp();
        intentions.clear();
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

        zombie.inheritFrom(this);

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

        zombie.inheritFrom(this);

        if (getCustomName() != null) {
            zombie.setCustomName(getCustomName().copy());
            zombie.setCustomNameVisible(isCustomNameVisible());
        }

        zombie.setPersistenceRequired();
        serverLevel.addFreshEntity(zombie);
    }

    private void dropOwnedStack(ItemStack stack) {
        dropOwnedStack(stack, true);
    }

    private void dropOwnedStack(ItemStack stack, boolean guaranteed) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack droppedStack = stack.copy();

        if (isMageSpellBook(droppedStack)) {
            droppedStack = IronSpellsCompat.createEmptyLootSpellBook(droppedStack);
            guaranteed = true;
        }

        if (!guaranteed && !shouldDropOwnedStack(droppedStack)) {
            return;
        }

        if (!guaranteed) {
            droppedStack.setCount(rollDroppedStackCount(droppedStack));
        }

        if (!droppedStack.isEmpty() && droppedStack.getCount() > 0) {
            spawnAtLocation(droppedStack);
        }
    }

    private boolean shouldDropOwnedStack(ItemStack stack) {
        double chance = switch (effectiveLootRarity(stack)) {
            case COMMON -> 0.68D;
            case UNCOMMON -> 0.48D;
            case RARE -> 0.28D;
            case EPIC -> 0.12D;
        };

        chance += switch (getGearTier()) {
            case STANDARD -> 0.00D;
            case FINE -> 0.08D;
            case RARE -> 0.18D;
            case ELITE -> 0.32D;
        };

        return getRandom().nextDouble() < Math.min(0.95D, chance);
    }

    private Rarity effectiveLootRarity(ItemStack stack) {
        Rarity rarity = stack.getRarity();

        if (!stack.isEnchanted()) {
            return rarity;
        }

        return switch (rarity) {
            case COMMON -> Rarity.UNCOMMON;
            case UNCOMMON -> Rarity.RARE;
            case RARE, EPIC -> Rarity.EPIC;
        };
    }

    private int rollDroppedStackCount(ItemStack stack) {
        if (stack.getCount() <= 1) {
            return 1;
        }

        double minFraction;
        double maxFraction;

        switch (getGearTier()) {
            case STANDARD -> {
                minFraction = 0.25D;
                maxFraction = 0.50D;
            }
            case FINE -> {
                minFraction = 0.35D;
                maxFraction = 0.62D;
            }
            case RARE -> {
                minFraction = 0.50D;
                maxFraction = 0.78D;
            }
            case ELITE -> {
                minFraction = 0.65D;
                maxFraction = 1.00D;
            }
            default -> {
                minFraction = 0.25D;
                maxFraction = 0.50D;
            }
        }

        double fraction = minFraction
                + getRandom().nextDouble() * (maxFraction - minFraction);

        return Mth.clamp(
                (int) Math.round(stack.getCount() * fraction),
                1,
                stack.getCount()
        );
    }

    @Override
    protected void dropEquipment() {
        if (suppressDeathLootForZombieConversion) {
            return;
        }

        List<ItemStack> carried = new ArrayList<>(inventory.removeAll());

        for (ItemStack stack : carried) {
            dropOwnedStack(stack, isMageSpellBook(stack));
        }

        dropOwnedStack(foodToEat, false);
        foodToEat = ItemStack.EMPTY;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = getItemBySlot(slot);
            if (!equipped.isEmpty()) {
                dropOwnedStack(equipped, false);
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public void releasePersistentClaims() {
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            CyberNpcWorldClaims.get(serverLevel).releaseOwner(getUUID());
        }

        serviceBrain.release();
        claimedCookingStation = null;
    }

    @Override
    public void die(DamageSource source) {
        boolean specialServiceNpc = isSpecialServiceNpc();

        boolean convertFromZombieKill = !specialServiceNpc
                && !level().isClientSide
                && source.getEntity() instanceof Zombie
                && getRandom().nextFloat() < ZOMBIE_KILL_CONVERSION_CHANCE;

        if (convertFromZombieKill || specialServiceNpc) {
            suppressDeathLootForZombieConversion = true;
        }

        if (!level().isClientSide) {
            if (getVehicle() instanceof AbstractHorse horse) {
                stopUsingHorse(horse);
            } else {
                clearHorseTarget();
            }

            corralBrain.interrupt();
            sleepBrain.wakeUp();
            intentions.clear();
            fleeingThreat = null;
            suspiciousNpc = null;
            emergencyEating = false;
            clearUtilityItem();
            cancelMageCast();
            stowWeapons();
            setSprinting(false);
            setShiftKeyDown(false);
            releasePersistentClaims();
        }

        if (!level().isClientSide) {
            if (specialServiceNpc
                    && level() instanceof ServerLevel serverLevel) {
                SpecialNpcSavedData.get(serverLevel)
                        .scheduleRespawn(this);
            } else {
                if (source.getEntity() instanceof Player player
                        && !player.isCreative()
                        && !player.isSpectator()) {
                    notifySocialWitnessesOfPlayerAttack(player, true);
                }
                handlePartyDeath();
            }
        }

        IronSpellsCompat.clearCasterState(this);
        super.die(source);

        if (convertFromZombieKill && !zombieConversionStarted) {
            zombieConversionStarted = true;
            spawnZombieNpcAfterDeath();
            discard();
        }

        suppressDeathLootForZombieConversion = false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldForTool = player.getItemInHand(hand);

        // Configuration/removal tools must win over the role's normal use
        // interaction. Otherwise Banker/Courier consume the right-click before
        // the item's interactLivingEntity hook ever gets a chance to run.
        if (hand == InteractionHand.MAIN_HAND
                && (heldForTool.is(ModItems.TOWN_REGISTER.get())
                || heldForTool.is(
                ModItems.SPECIAL_NPC_REMOVAL_STICK.get()
        ))) {
            return heldForTool.interactLivingEntity(
                    player,
                    this,
                    hand
            );
        }

        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            NpcServiceRole serviceRole =
                    NpcServiceRole.fromRole(getRole());

            if (serviceRole == NpcServiceRole.BANKER
                    && level() instanceof ServerLevel serverLevel) {
                ItemStack held = player.getItemInHand(hand);
                long deposit = CurrencyValue.valueOf(held);
                BankSavedData bank = BankSavedData.get(serverLevel);

                if (held.isEmpty() && player.isShiftKeyDown()) {
                    long balance = bank.getBalance(player.getUUID());

                    if (balance <= 0L) {
                        player.sendSystemMessage(Component.literal(
                                "Your bank balance is empty."
                        ));
                        return InteractionResult.CONSUME;
                    }

                    // Bound one physical payout to a normal inventory-sized
                    // amount so an absurd balance cannot create thousands of
                    // dropped item stacks in a single click.
                    long withdrawn = Math.min(
                            balance,
                            23_040_000L
                    );

                    if (!bank.withdraw(
                            player.getUUID(),
                            withdrawn
                    )) {
                        return InteractionResult.CONSUME;
                    }

                    int droppedStacks = 0;
                    for (ItemStack payout
                            : CurrencyValue.makePayout(withdrawn)) {
                        if (!player.getInventory().add(payout)) {
                            player.drop(payout, false);
                            droppedStacks++;
                        }
                    }

                    level().playSound(
                            null,
                            blockPosition(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.NEUTRAL,
                            0.55F,
                            0.92F
                    );

                    long remaining = bank.getBalance(player.getUUID());
                    player.sendSystemMessage(Component.literal(
                            "Withdrew "
                                    + CurrencyValue.format(withdrawn)
                                    + " credits as coins. Balance: "
                                    + CurrencyValue.format(remaining)
                                    + " credits."
                                    + (droppedStacks > 0
                                    ? " Some coins were dropped because your inventory was full."
                                    : "")
                    ));

                    return InteractionResult.CONSUME;
                }

                if (deposit > 0L) {
                    if (!player.getAbilities().instabuild) {
                        held.setCount(0);
                    }

                    long balance = bank.deposit(
                            player.getUUID(),
                            deposit
                    );

                    level().playSound(
                            null,
                            blockPosition(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.NEUTRAL,
                            0.55F,
                            1.15F
                    );

                    player.sendSystemMessage(Component.literal(
                            "Deposited "
                                    + CurrencyValue.format(deposit)
                                    + " credits. Balance: "
                                    + CurrencyValue.format(balance)
                                    + " credits."
                    ));

                    return InteractionResult.CONSUME;
                }

                long balance = bank.getBalance(player.getUUID());
                player.sendSystemMessage(Component.literal(
                        "Bank balance: "
                                + CurrencyValue.format(balance)
                                + " credits."
                ));
                player.sendSystemMessage(Component.literal(
                        "Hold coins and right-click to deposit. Sneak-right-click with an empty hand to withdraw your balance as coins."
                ));

                return InteractionResult.CONSUME;
            }

            if (serviceRole == NpcServiceRole.COURIER
                    || serviceRole == NpcServiceRole.GUARD) {
                player.sendSystemMessage(Component.literal(
                        serviceRole.displayName()
                                + " — " + serviceBrain.status()
                ));
                return InteractionResult.CONSUME;
            }

            String displayName = getCustomName() != null
                    ? getCustomName().getString()
                    : "Cyber NPC";

            if (getNpcType() == NpcType.WILD) {
                player.sendSystemMessage(Component.literal(
                        displayName + " — " + getWildClassDisplayName()
                                + " / " + getPersonalityDisplayName()
                                + " / " + getGearTierDisplayName()
                                + (wildClassHasMagicSchool()
                                ? " / " + getMageSchoolDisplayName()
                                : "")
                                + " — Hunger " + getHungerBar()
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                        displayName + " — " + getRole()
                ));
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

        if (specialNpcId != null) {
            tag.putUUID("CyberNpcSpecialId", specialNpcId);
        }

        ensureAppearance();
        tag.putString(
                "CyberNpcAppearanceGender",
                getAppearanceGender().serializedName()
        );
        tag.putInt("CyberNpcAppearanceSkinTone", getSkinToneIndex());
        tag.putInt("CyberNpcAppearanceEyes", getEyeStyleIndex());
        tag.putInt("CyberNpcAppearanceHair", getHairStyleIndex());

        if (getNpcType() == NpcType.WILD) {
            tag.putString("CyberNpcWildClass", getWildClass());
            if (!getWildClassAdvancement().isBlank()) {
                tag.putString(
                        "CyberNpcClassAdvancement",
                        getWildClassAdvancement()
                );
            }
            tag.putString("CyberNpcPersonality", getPersonality().serializedName());
            tag.putString("CyberNpcGearTier", getGearTier().serializedName());
            tag.putBoolean("CyberNpcClassLoadoutInitialized", classLoadoutInitialized);

            if (wildClassHasMagicSchool()) {
                tag.putString("CyberNpcMageSchool", getMageSchool().serializedName());
            }
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

        socialMemory.saveTo(tag);

        if (rangerHomePos != null) {
            tag.putLong(
                    "CyberNpcRangerHome",
                    rangerHomePos.asLong()
            );
        }

        corralBrain.addSaveData(tag);
        sleepBrain.addSaveData(tag);
        serviceBrain.addSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        specialNpcId = tag.hasUUID("CyberNpcSpecialId")
                ? tag.getUUID("CyberNpcSpecialId")
                : null;

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

        if (tag.contains("CyberNpcAppearanceGender")) {
            entityData.set(
                    DATA_APPEARANCE_GENDER,
                    NpcAppearance.Gender.fromSerializedName(
                            tag.getString("CyberNpcAppearanceGender")
                    ).serializedName()
            );
        }

        if (tag.contains("CyberNpcAppearanceSkinTone")) {
            entityData.set(
                    DATA_APPEARANCE_SKIN_TONE,
                    NpcAppearance.sanitizeSkinTone(
                            tag.getInt("CyberNpcAppearanceSkinTone")
                    )
            );
        }

        if (tag.contains("CyberNpcAppearanceEyes")) {
            entityData.set(
                    DATA_APPEARANCE_EYES,
                    NpcAppearance.sanitizeEyeStyle(
                            tag.getInt("CyberNpcAppearanceEyes")
                    )
            );
        }

        if (tag.contains("CyberNpcAppearanceHair")) {
            entityData.set(
                    DATA_APPEARANCE_HAIR,
                    NpcAppearance.sanitizeHairStyle(
                            tag.getInt("CyberNpcAppearanceHair")
                    )
            );
        }

        ensureAppearance();
        socialMemory.loadFrom(tag);

        rangerHomePos = tag.contains("CyberNpcRangerHome")
                ? BlockPos.of(tag.getLong("CyberNpcRangerHome"))
                : (tag.contains("CyberNpcExplorerHome")
                ? BlockPos.of(tag.getLong("CyberNpcExplorerHome"))
                : null);
        rangerPatrolTarget = null;
        rangerPatrolAngle = Double.NaN;
        rangerPatrolPauseTicks = 0;
        rangerVillageCheckCooldown = 0;
        rangerHomeIsVillage = false;
        rangerReturningHomeForSleep = false;

        if (getNpcType() == NpcType.WILD) {
            String loadedClassName = tag.contains("CyberNpcWildClass")
                    ? tag.getString("CyberNpcWildClass")
                    : "";
            boolean retiredBeastTamer =
                    "beast_tamer".equalsIgnoreCase(loadedClassName);

            String loadedClass = retiredBeastTamer
                    ? "classless"
                    : (loadedClassName.isBlank()
                    ? CyberClassesNpcCompat.randomClassId(getRandom())
                    : CyberClassesNpcCompat.normalize(loadedClassName));

            boolean unavailableLoadedClass =
                    !CyberClassesNpcCompat.isAvailable(loadedClass);

            setWildClass(
                    unavailableLoadedClass
                            ? "classless"
                            : loadedClass
            );

            if (tag.contains("CyberNpcClassAdvancement")) {
                setWildClassAdvancement(
                        tag.getString("CyberNpcClassAdvancement")
                );
            } else {
                wildClassAdvancement = "";
                ensureWildClassAdvancement();
            }

            if (tag.contains("CyberNpcPersonality")) {
                setPersonality(WildNpcPersonality.fromSerializedName(tag.getString("CyberNpcPersonality")));
            } else {
                setPersonality(WildNpcPersonality.randomPersonality(getRandom()));
            }

            if (tag.contains("CyberNpcGearTier")) {
                setGearTier(WildNpcGearTier.fromSerializedName(tag.getString("CyberNpcGearTier")));
            } else {
                setGearTier(WildNpcGearTier.randomTier(getRandom()));
            }

            if (wildClassHasMagicSchool()) {
                if (isWildClass("cleric") || isWildClass("bard")) {
                    setMageSchool(MageSchool.HOLY);
                } else if (isWildClass("druid")) {
                    setMageSchool(MageSchool.NATURE);
                } else if (tag.contains("CyberNpcMageSchool")) {
                    setMageSchool(MageSchool.fromSerializedName(tag.getString("CyberNpcMageSchool")));
                } else {
                    setMageSchool(MageSchool.randomSchool(getRandom()));
                }
            } else {
                setMageSchool(null);
            }

            classLoadoutInitialized = !retiredBeastTamer
                    && !unavailableLoadedClass
                    && tag.getBoolean("CyberNpcClassLoadoutInitialized");
        } else {
            entityData.set(DATA_WILD_CLASS, "");
            entityData.set(DATA_PERSONALITY, "");
            entityData.set(DATA_GEAR_TIER, "");
            entityData.set(DATA_MAGE_SCHOOL, "");
            classLoadoutInitialized = false;
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
        entityData.set(DATA_SPELL_CAST_TICKS, 0);
        entityData.set(DATA_SPELL_CAST_MODE, SPELL_CAST_MODE_NONE);
        entityData.set(DATA_CASTING_SPELL, "");
        if (isMageSpellBook(getOffhandItem())) {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
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
        socialTickCooldown = 0;
        socialConversationCooldown = 0;
        socialConversationHoldTicks = 0;
        socialConversationPartnerId = null;
        partyInviteCooldown = 0;
        pendingPartyInviteTicks = 0;
        pendingPartyInviteFrom = null;
        regroupingWithParty = false;
        chatReactionCooldown = 0;
        horseTargetId = null;
        horseRideOrigin = null;
        horseSearchCooldown = 0;
        horseRideTicks = 0;
        horseRemountCooldown = 0;
        horseRepathCooldown = 0;
        rangerPatrolTarget = null;
        rangerPatrolAngle = Double.NaN;
        rangerPatrolPauseTicks = 0;
        rangerVillageCheckCooldown = 0;
        rangerHomeIsVillage = false;
        rangerReturningHomeForSleep = false;
        backgroundClaimScanCooldown = 0;
        chainmailMigrationChecked = false;
        fleeingThreat = null;
        fleeSafeTicks = 0;
        fleeRepathCooldown = 0;
        corralBrain.readSaveData(tag);
        sleepBrain.readSaveData(tag);
        serviceBrain.readSaveData(tag);
        intentions.clear();
        initializeStaggeredAiTimers();

        if (getNpcType() == NpcType.WILD) {
            ensureWildProfile();
            stowWeapons();
        }

        ensureDefaultName();
    }

    private enum MobAggroClass {
        PASSIVE("Passive"),
        NEUTRAL("Neutral"),
        AGGRESSIVE("Aggressive");

        private final String displayName;

        MobAggroClass(String displayName) {
            this.displayName = displayName;
        }
    }

    private record CombatConfidence(
            double groupScore,
            int helpers,
            MobAggroClass aggroClass,
            double npcDamage,
            double mobDamage
    ) {
    }

    private record GapJumpPlan(
            Vec3 direction,
            BlockPos landing,
            int gapBlocks
    ) {
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
                    && !npc.buildingBrain.isBusy()
                    && npc.canWander()
                    && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.isCombatActive()
                    && !npc.isBusyWithNeeds()
                    && !npc.buildingBrain.isBusy()
                    && npc.canWander()
                    && super.canContinueToUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            for (int attempt = 0; attempt < 8; attempt++) {
                Vec3 candidate = super.getPosition();
                if (candidate == null) {
                    return null;
                }

                BlockPos candidatePos =
                        BlockPos.containing(candidate);

                if (npc.isBuildingDestinationAllowed(candidatePos)
                        && npc.fenceAvoidanceBrain
                        .isDestinationFenceSafe(candidatePos)) {
                    return candidate;
                }
            }

            return null;
        }

        @Override
        public void start() {
            super.start();

            // The destination can be outside while Minecraft still picks a
            // shorter route through somebody's private house. Validate every
            // path node so an open front/back door never becomes a shortcut.
            if (!npc.isCurrentWanderPathBuildingAllowed()) {
                npc.getNavigation().stop();
            }
        }
    }

    private static final class BuildingAwareOpenDoorGoal extends OpenDoorGoal {
        private final CyberNpcEntity npc;

        private BuildingAwareOpenDoorGoal(
                CyberNpcEntity npc,
                boolean closeDoor
        ) {
            super(npc, closeDoor);
            this.npc = npc;
        }

        @Override
        public boolean canUse() {
            return super.canUse()
                    && npc.canUseBuildingDoor(doorPos);
        }

        @Override
        public boolean canContinueToUse() {
            return npc.canUseBuildingDoor(doorPos)
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
        private int crossbowLostSightTicks;

        @Nullable
        private CyberNpcEntity clericSupportTarget;

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
            npc.cancelMageCast();
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
                case "knight", "rogue", "berserker", "monk", "alchemist" -> {
                    tickMelee(target, distanceSqr);
                    return;
                }
                case "archer" -> {
                    if (distanceSqr <= 16.0D && !npc.getStoredSword().isEmpty()) {
                        tickMelee(target, distanceSqr);
                    } else {
                        tickRanged(target, distanceSqr, lineOfSight);
                    }
                    return;
                }
                case "mage", "druid" -> {
                    tickMage(target, distanceSqr, lineOfSight);
                    return;
                }
                case "cleric", "bard" -> {
                    tickCleric(target, distanceSqr, lineOfSight);
                    return;
                }
                case "spellblade" -> {
                    tickSpellblade(target, distanceSqr, lineOfSight);
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
            npc.equipMeleeWeapon();

            String npcClass = npc.getWildClass();
            WildNpcPersonality personality = npc.getPersonality();

            int attackCooldown = switch (npcClass) {
                case "rogue" -> 9;
                case "knight" -> 13;
                case "berserker" -> 14;
                case "spellblade" -> 11;
                default -> MELEE_ATTACK_COOLDOWN;
            };

            int recoveryTicks = switch (npcClass) {
                case "rogue" -> 4;
                case "knight" -> 6;
                case "berserker" -> 5;
                case "spellblade" -> 5;
                default -> MELEE_RECOVERY_TICKS;
            };

            attackCooldown = Math.max(
                    6,
                    attackCooldown + personality.attackCooldownModifier()
            );
            recoveryTicks = Math.max(
                    2,
                    recoveryTicks + personality.recoveryModifier()
            );

            boolean berserkerRage = "berserker".equals(npcClass)
                    && npc.getHealth() <= npc.getMaxHealth() * 0.45F;

            if (berserkerRage) {
                attackCooldown = Math.max(6, attackCooldown - 3);
                recoveryTicks = Math.max(2, recoveryTicks - 2);
            }

            double attackDistanceSqr = switch (npcClass) {
                case "rogue" -> 8.41D;
                case "berserker" -> 10.24D;
                default -> MELEE_ATTACK_DISTANCE_SQR;
            };

            double tooCloseSqr = switch (npcClass) {
                case "knight" -> 4.0D;
                case "berserker" -> 3.24D;
                default -> MELEE_TOO_CLOSE_SQR;
            };

            double approachSpeed = switch (npcClass) {
                case "rogue" -> 1.20D;
                case "berserker" -> berserkerRage ? 1.28D : 1.16D;
                case "knight" -> 1.02D;
                case "spellblade" -> 1.10D;
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
                        && !"knight".equals(npcClass));
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

            double sideStrength = npc.isWildClass("rogue")
                    ? 1.25D
                    : (npc.isWildClass("berserker") ? 0.45D : 0.75D);
            sideStrength *= npc.getPersonality().strafeMultiplier();

            Vec3 side = new Vec3(-away.z, 0.0D, away.x)
                    .scale(sideStrength * meleeStrafeDirection);

            double radius;
            if (npc.isWildClass("knight")) {
                radius = retreat ? 3.0D : 2.55D;
            } else if (npc.isWildClass("rogue")) {
                radius = retreat ? 4.2D : 3.1D;
            } else if (npc.isWildClass("berserker")) {
                radius = retreat ? 2.7D : 2.3D;
            } else if (npc.isWildClass("spellblade")) {
                radius = retreat ? 3.8D : 3.0D;
            } else {
                radius = retreat ? 3.6D : 2.85D;
            }

            radius += npc.getPersonality().spacingOffset();

            Vec3 desired = target.position()
                    .add(away.scale(Math.max(2.2D, radius)))
                    .add(side);

            double speed = switch (npc.getWildClass()) {
                case "rogue" -> retreat ? 1.20D : 0.96D;
                case "berserker" -> retreat ? 1.12D : 0.98D;
                case "spellblade" -> retreat ? 1.12D : 0.90D;
                default -> retreat ? 1.08D : 0.82D;
            };

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
                    .scale(0.8D * npc.getPersonality().strafeMultiplier());

            radius += npc.getPersonality().spacingOffset() * 1.8D;

            Vec3 desired = target.position()
                    .add(away.scale(Math.max(6.0D, radius)))
                    .add(side);

            npc.getNavigation().moveTo(
                    desired.x,
                    npc.getY(),
                    desired.z,
                    npc.isWildClass("archer") ? 1.05D : 0.95D
            );
        }

        private void tickRanged(
                LivingEntity target,
                double distanceSqr,
                boolean lineOfSight
        ) {
            npc.equipRangedWeapon();
            ItemStack ranged = npc.getMainHandItem();

            if (distanceSqr > MAX_RANGED_DISTANCE_SQR) {
                resetRangedUse();
                npc.setSprinting(!npc.huntingTarget);
                npc.getNavigation().moveTo(
                        target,
                        npc.huntingTarget ? 0.62D : 1.12D
                );
                return;
            }

            if (!lineOfSight) {
                npc.setSprinting(!npc.huntingTarget);
                npc.getNavigation().moveTo(
                        target,
                        npc.huntingTarget ? 0.62D : 1.05D
                );

                boolean crossbowAlreadyActive = ranged.is(Items.CROSSBOW)
                        && (npc.getRangedState()
                        == RANGED_STATE_CROSSBOW_CHARGE
                        || npc.getRangedState()
                        == RANGED_STATE_CROSSBOW_HOLD);

                if (crossbowAlreadyActive) {
                    crossbowLostSightTicks++;

                    // Do not repeatedly cancel/restart a crossbow because LOS
                    // flickered for a few ticks. That restart was causing the
                    // vanilla loading sound sequence to retrigger repeatedly.
                    if (crossbowLostSightTicks <= 12) {
                        tickCrossbow(
                                target,
                                ranged,
                                distanceSqr,
                                false
                        );
                        return;
                    }
                }

                resetRangedUse();
                return;
            }

            crossbowLostSightTicks = 0;
            npc.setSprinting(false);

            boolean archer = npc.isWildClass("archer");
            double preferredStopSqr = archer
                    ? 144.0D
                    : RANGED_STOP_DISTANCE_SQR;
            double tooCloseSqr = archer ? 64.0D : 0.0D;

            if (archer && distanceSqr < tooCloseSqr) {
                moveForRangedSpacing(target, 11.0D);
            } else if (distanceSqr > preferredStopSqr) {
                npc.getNavigation().moveTo(
                        target,
                        npc.huntingTarget
                                ? 0.62D
                                : (archer ? 1.00D : 0.92D)
                );
            } else {
                npc.getNavigation().stop();
            }

            if (ranged.is(Items.CROSSBOW)) {
                tickCrossbow(
                        target,
                        ranged,
                        distanceSqr,
                        true
                );
            } else {
                tickBow(target, distanceSqr);
            }
        }

        private void tickCleric(
                LivingEntity enemy,
                double enemyDistanceSqr,
                boolean enemyLineOfSight
        ) {
            ItemStack spellBook = npc.getMageSpellBook();

            if (spellBook.isEmpty()
                    || !IronSpellsCompat.hasUsableCombatSpells(spellBook)) {
                npc.classLoadoutInitialized = false;
                npc.ensureWildProfile();
                spellBook = npc.getMageSpellBook();
            }

            if (spellBook.isEmpty()) {
                tickMelee(enemy, enemyDistanceSqr);
                return;
            }

            if (IronSpellsCompat.hasActiveCast(npc)) {
                if (IronSpellsCompat.isActiveSupportCast(npc)) {
                    if (clericSupportTarget != null
                            && clericSupportTarget.isAlive()) {
                        tickClericSupport(spellBook, clericSupportTarget);
                        return;
                    }

                    npc.cancelMageCast();
                    clericSupportTarget = null;
                } else {
                    tickMage(enemy, enemyDistanceSqr, enemyLineOfSight);
                    return;
                }
            }

            if (IronSpellsCompat.hasReadySupportSpell(npc, spellBook)) {
                clericSupportTarget = npc.findMostInjuredFriendly(24.0D);
                if (clericSupportTarget != null) {
                    tickClericSupport(spellBook, clericSupportTarget);
                    return;
                }
            }

            clericSupportTarget = null;
            tickMage(enemy, enemyDistanceSqr, enemyLineOfSight);
        }

        private void tickClericSupport(
                ItemStack spellBook,
                CyberNpcEntity ally
        ) {
            resetRangedUse();
            npc.clearUtilityItem();
            npc.stowWeapons();
            npc.setSprinting(false);
            npc.setShiftKeyDown(false);

            double distance = npc.distanceTo(ally);
            boolean lineOfSight = npc.getSensing().hasLineOfSight(ally);

            if (distance > 30.0D || !lineOfSight) {
                npc.getNavigation().moveTo(ally, 0.95D);
                return;
            }

            npc.getNavigation().stop();

            IronSpellsCompat.CastResult result =
                    IronSpellsCompat.tickSupportSpell(npc, ally, spellBook);

            if (!result.success()) {
                clericSupportTarget = null;
                return;
            }

            npc.startMageCastingVisual(
                    spellBook,
                    result.spellId(),
                    result.visualTicks(),
                    result.castType()
            );

            if (!result.casting()) {
                clericSupportTarget = null;
                mageCooldown = Math.max(
                        4,
                        8 + npc.getPersonality().attackCooldownModifier()
                );
            }
        }

        private void tickSpellblade(
                LivingEntity target,
                double distanceSqr,
                boolean lineOfSight
        ) {
            ItemStack spellBook = npc.getMageSpellBook();

            if (spellBook.isEmpty()
                    || !IronSpellsCompat.hasUsableCombatSpells(spellBook)) {
                npc.classLoadoutInitialized = false;
                npc.ensureWildProfile();
                spellBook = npc.getMageSpellBook();
            }

            if (spellBook.isEmpty()) {
                tickMelee(target, distanceSqr);
                return;
            }

            if (IronSpellsCompat.hasActiveCast(npc)) {
                tickMage(target, distanceSqr, lineOfSight);
                return;
            }

            IronSpellsCompat.CombatPlan plan =
                    IronSpellsCompat.getCombatPlan(npc, target, spellBook);

            double healthFraction = npc.getMaxHealth() <= 0.0F
                    ? 1.0D
                    : npc.getHealth() / npc.getMaxHealth();

            boolean defensiveMagic = plan.available()
                    && ("HEAL".equals(plan.role())
                    || "DEFENSE".equals(plan.role()))
                    && healthFraction < 0.65D;

            // Spellblades use magic to open/control at range, but once they are
            // inside sword distance they commit to melee unless defensive magic
            // has become urgent.
            if (!defensiveMagic
                    && (distanceSqr <= 36.0D || !plan.available())) {
                tickMelee(target, distanceSqr);
                return;
            }

            tickMage(target, distanceSqr, lineOfSight);
        }

        private void tickMage(LivingEntity target, double distanceSqr, boolean lineOfSight) {
            resetRangedUse();
            npc.clearUtilityItem();
            npc.stowWeapons();
            npc.setSprinting(false);
            npc.setShiftKeyDown(false);

            if (!IronSpellsCompat.isLoaded()) {
                npc.cancelMageCast();
                npc.setWildClass("classless");
                npc.setMageSchool(null);
                npc.classLoadoutInitialized = false;
                npc.ensureWildProfile();
                tickMelee(target, distanceSqr);
                return;
            }

            ItemStack spellBook = npc.getMageSpellBook();
            if (spellBook.isEmpty()
                    || !IronSpellsCompat.hasUsableCombatSpells(spellBook)) {
                // Repair old/invalid Mage books instead of letting an NPC keep
                // an empty spellbook forever.
                npc.classLoadoutInitialized = false;
                npc.ensureWildProfile();
                spellBook = npc.getMageSpellBook();

                if (spellBook.isEmpty()
                        || !IronSpellsCompat.hasUsableCombatSpells(spellBook)) {
                    mageCooldown = 40;
                    moveForRangedSpacing(target, 12.0D);
                    return;
                }
            }

            if (IronSpellsCompat.hasActiveCast(npc)) {
                npc.getNavigation().stop();
                IronSpellsCompat.CastResult result =
                        IronSpellsCompat.tickAttackSpell(npc, target, spellBook);

                if (!result.success()) {
                    npc.clearSpellCastingVisual();
                    mageCooldown = 8;
                    return;
                }

                npc.startMageCastingVisual(
                        spellBook,
                        result.spellId(),
                        result.visualTicks(),
                        result.castType()
                );

                if (!result.casting()) {
                    mageCooldown = Math.max(
                            3,
                            6 + npc.getPersonality().attackCooldownModifier()
                    );
                }
                return;
            }

            if (mageCooldown > 0) {
                mageCooldown--;
                return;
            }

            IronSpellsCompat.CombatPlan plan =
                    IronSpellsCompat.getCombatPlan(npc, target, spellBook);

            if (!plan.available()) {
                if (plan.coolingDown()) {
                    retreatMageWhileCooling(target);
                } else {
                    mageCooldown = 8;
                    npc.getNavigation().moveTo(target, 0.90D);
                }
                return;
            }

            double distance = Math.sqrt(distanceSqr);

            if (plan.requiresLineOfSight() && !lineOfSight) {
                npc.getNavigation().moveTo(target, 0.98D);
                return;
            }

            if (distance > plan.maxRange()) {
                // Close-range and mid-range spellbooks now actually close the
                // distance instead of hovering at the old fixed Mage radius.
                npc.getNavigation().moveTo(
                        target,
                        plan.preferredRange() <= 8.0D ? 1.02D : 0.92D
                );
                return;
            }

            if (distance < plan.minRange()) {
                // Long-range/projectile spells create their preferred gap before
                // committing to the cast.
                moveForRangedSpacing(
                        target,
                        Math.max(plan.preferredRange(), plan.minRange() + 2.0D)
                );
                return;
            }

            if (!plan.readyToCast()) {
                if (distance > plan.preferredRange()) {
                    npc.getNavigation().moveTo(target, 0.90D);
                } else {
                    moveForRangedSpacing(target, plan.preferredRange());
                }
                return;
            }

            if (("RANGED".equals(plan.role())
                    || "CONTROL".equals(plan.role()))
                    && !npc.hasClearFriendlyFireLane(target)) {
                moveForRangedSpacing(
                        target,
                        Math.max(8.0D, plan.preferredRange())
                );
                return;
            }

            npc.getNavigation().stop();
            npc.setSprinting(false);

            IronSpellsCompat.CastResult result =
                    IronSpellsCompat.tickAttackSpell(npc, target, spellBook);

            if (!result.success()) {
                mageCooldown = 8;
                moveForRangedSpacing(target, 12.0D);
                return;
            }

            npc.startMageCastingVisual(
                    spellBook,
                    result.spellId(),
                    result.visualTicks(),
                    result.castType()
            );

            if (result.casting()) {
                npc.getNavigation().stop();
                return;
            }

            mageCooldown = Math.max(
                    3,
                    6 + npc.getPersonality().attackCooldownModifier()
            );
        }

        private void retreatMageWhileCooling(LivingEntity target) {
            npc.clearSpellCastingVisual();
            npc.setShiftKeyDown(false);
            npc.setSprinting(true);

            Vec3 away = DefaultRandomPos.getPosAway(
                    npc,
                    14,
                    6,
                    target.position()
            );

            if (away != null) {
                npc.getNavigation().moveTo(
                        away.x,
                        away.y,
                        away.z,
                        1.18D
                );
            } else {
                moveForRangedSpacing(target, 16.0D);
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
            int desiredDrawTicks = Mth.clamp(
                    (int) Math.round(11.0D + distance * 0.35D),
                    12,
                    20
            );

            if (CyberRacesNpcCompat.isRace(npc, "elf")) {
                desiredDrawTicks = Math.max(
                        10,
                        Math.round(desiredDrawTicks * 0.85F)
                );
            }

            if (bowDrawTicks < desiredDrawTicks) {
                return;
            }

            float drawPower = Mth.clamp(bowDrawTicks / 20.0F, 0.65F, 1.0F);
            float speed = 1.65F + drawPower * 1.25F;
            float inaccuracy = Mth.clamp(5.0F - (float) distance * 0.08F, 1.5F, 4.5F);

            npc.stopUsingItem();
            npc.setRangedState(RANGED_STATE_NONE);

            if (!npc.fireAdaptiveArrow(
                    target,
                    speed,
                    inaccuracy,
                    3.0D + drawPower
            )) {
                bowDrawTicks = 0;
                rangedCooldown = 4;
                moveForRangedSpacing(target, 10.0D);
                return;
            }

            npc.level().playSound(
                    null,
                    npc.blockPosition(),
                    SoundEvents.ARROW_SHOOT,
                    SoundSource.NEUTRAL,
                    1.0F,
                    1.0F / (npc.getRandom().nextFloat() * 0.4F + 0.8F)
            );

            bowDrawTicks = 0;
            rangedCooldown = Math.max(
                    6,
                    (npc.isWildClass("archer") ? 10 : 12)
                            + npc.getPersonality().attackCooldownModifier()
            );
        }

        private void tickCrossbow(
                LivingEntity target,
                ItemStack crossbow,
                double distanceSqr,
                boolean lineOfSight
        ) {
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
                return;
            }

            if (state == RANGED_STATE_CROSSBOW_HOLD) {
                if (crossbowHoldTicks > 0) {
                    crossbowHoldTicks--;
                    return;
                }

                if (!lineOfSight) {
                    crossbowHoldTicks = 2;
                    return;
                }

                double distance = Math.sqrt(distanceSqr);
                float speed = 3.15F;
                float inaccuracy = Mth.clamp(3.0F - (float) distance * 0.04F, 0.8F, 2.5F);

                if (!npc.fireAdaptiveArrow(
                        target,
                        speed,
                        inaccuracy,
                        4.5D
                )) {
                    crossbowHoldTicks = 4;
                    moveForRangedSpacing(target, 11.0D);
                    return;
                }

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
                rangedCooldown = Math.max(
                        14,
                        (npc.isWildClass("archer") ? 24 : 28)
                                + npc.getPersonality().attackCooldownModifier() * 2
                );
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
            crossbowLostSightTicks = 0;
        }

        private void resetTimers() {
            resetRangedUse();
            meleeCooldown = 0;
            meleeRecoveryTicks = 0;
            meleeStrafeTicks = 0;
            meleeStrafeDirection = 1;
            rangedCooldown = 0;
            mageCooldown = 0;
            clericSupportTarget = null;
        }
    }
}
