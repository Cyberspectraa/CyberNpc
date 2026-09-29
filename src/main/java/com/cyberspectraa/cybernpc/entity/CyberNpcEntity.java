package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class CyberNpcEntity extends PathfinderMob {
    private static final double WILD_HELP_RADIUS = 20.0D;
    private static final double WILD_DISENGAGE_DISTANCE = 40.0D;
    private static final int WILD_DISENGAGE_TICKS = 100;
    private static final int WILD_MIN_AGGRESSION = 10;
    private static final int WILD_MAX_AGGRESSION = 80;

    private static final int MAX_HUNGER = 20;
    private static final int HUNT_HUNGER_THRESHOLD = 12;
    private static final int STOP_EATING_HUNGER = 18;
    private static final int HUNGER_DECAY_TICKS = 1200;
    private static final int STARVATION_DAMAGE_TICKS = 80;
    private static final int HUNT_SEARCH_INTERVAL = 100;
    private static final int DROP_SEARCH_TIME = 120;
    private static final int COOK_TIME_TICKS = 100;
    private static final int COOK_STATION_SEARCH_INTERVAL = 200;
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
    private int cookingTicks;
    private int cookingSearchCooldown;

    public CyberNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
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

        if (!isCombatActive() && !getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    public ItemStack getStoredSword() {
        return storedSword;
    }

    public ItemStack getStoredRangedWeapon() {
        return storedRangedWeapon;
    }

    private void equipSword() {
        if (getNpcType() == NpcType.WILD && !storedSword.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, storedSword.copy());
        }
    }

    private void equipRangedWeapon() {
        if (getNpcType() == NpcType.WILD && !storedRangedWeapon.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, storedRangedWeapon.copy());
        }
    }

    private void stowWeapons() {
        if (getNpcType() == NpcType.WILD) {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
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

        if (getHunger() > HUNT_HUNGER_THRESHOLD && carriedRawFood.isEmpty()) {
            return;
        }

        if (!carriedRawFood.isEmpty()) {
            tickCooking();
            return;
        }

        if (dropSearchTicks > 0) {
            tickFoodPickup();
            return;
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

        getNavigation().moveTo(food, 1.0D);

        if (distanceToSqr(food) <= 2.25D) {
            ItemStack found = food.getItem();

            if (carriedRawFood.isEmpty()) {
                carriedRawFood = found.copy();
                food.discard();
                dropSearchTicks = 0;
                lastHuntKillPos = null;
                cookingTarget = null;
                cookingSearchCooldown = 0;
                getNavigation().stop();
            } else if (ItemStack.isSameItemSameTags(carriedRawFood, found)) {
                carriedRawFood.grow(found.getCount());
                food.discard();
                dropSearchTicks = 0;
                lastHuntKillPos = null;
                cookingTarget = null;
                cookingSearchCooldown = 0;
                getNavigation().stop();
            }
        }
    }

    private void tickCooking() {
        if (getHunger() >= STOP_EATING_HUNGER) {
            cookingTarget = null;
            cookingTicks = 0;
            getNavigation().stop();
            return;
        }

        if (cookingTarget == null || !isCookingStation(cookingTarget)) {
            if (cookingSearchCooldown > 0) {
                cookingSearchCooldown--;
                return;
            }

            cookingSearchCooldown = COOK_STATION_SEARCH_INTERVAL;
            cookingTarget = findCookingStation();
            cookingTicks = 0;

            if (cookingTarget == null) {
                return;
            }
        }

        Vec3 cookingSpot = Vec3.atCenterOf(cookingTarget).add(0.0D, 1.0D, 0.0D);
        double distance = distanceToSqr(cookingSpot);

        if (distance > 4.0D) {
            getNavigation().moveTo(cookingSpot.x, cookingSpot.y, cookingSpot.z, 0.9D);
            cookingTicks = 0;
            return;
        }

        getNavigation().stop();
        cookingTicks++;

        if (cookingTicks < COOK_TIME_TICKS) {
            return;
        }

        cookingTicks = 0;

        ItemStack cooked = CyberNpcHuntingData.cookOne(carriedRawFood);
        if (cooked.isEmpty()) {
            carriedRawFood = ItemStack.EMPTY;
            cookingTarget = null;
            return;
        }

        carriedRawFood.shrink(1);
        if (carriedRawFood.isEmpty()) {
            carriedRawFood = ItemStack.EMPTY;
        }

        setHunger(getHunger() + CyberNpcHuntingData.hungerRestored(cooked));

        if (getHunger() >= STOP_EATING_HUNGER || carriedRawFood.isEmpty()) {
            cookingTarget = null;
            getNavigation().stop();
        }
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
            if (!isCookingStation(pos)) {
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

    private boolean isCookingStation(BlockPos pos) {
        var state = level().getBlockState(pos);
        return state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.SMOKER);
    }

    private boolean isBusyWithNeeds() {
        return getHunger() <= HUNT_HUNGER_THRESHOLD
                || !carriedRawFood.isEmpty()
                || dropSearchTicks > 0
                || cookingTarget != null;
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
        setTarget(target);
        setCombatActive(true);
        huntingTarget = isHunt;
        provocation = isHunt ? provocation : 100;
        outOfRangeTicks = 0;
        cookingTarget = null;
        cookingTicks = 0;
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

    private void fireRangedWeapon(LivingEntity target) {
        boolean crossbow = storedRangedWeapon.is(Items.CROSSBOW);

        Arrow arrow = new Arrow(level(), this);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getY(0.4D) - arrow.getY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(
                dx,
                dy + horizontal * 0.18D,
                dz,
                crossbow ? 2.2F : 1.6F,
                crossbow ? 4.0F : 7.0F
        );

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

        // Migrate the old single stored weapon only if it was actually a sword.
        if (storedSword.isEmpty() && tag.contains("CyberNpcStoredWeapon")) {
            ItemStack oldWeapon = ItemStack.of(tag.getCompound("CyberNpcStoredWeapon"));
            if (oldWeapon.getItem() instanceof SwordItem) {
                storedSword = oldWeapon;
            }
        }

        setCombatActive(false);
        huntingTarget = false;
        provocation = 0;
        outOfRangeTicks = 0;
        huntSearchCooldown = 0;
        dropSearchTicks = 0;
        lastHuntKillPos = null;
        cookingTarget = null;
        cookingTicks = 0;
        cookingSearchCooldown = 0;

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
        private static final double MELEE_DISTANCE_SQR = 16.0D;
        private static final double MAX_RANGED_DISTANCE_SQR = 225.0D;

        private final CyberNpcEntity npc;
        private int meleeCooldown;
        private int rangedCooldown;

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
        public void stop() {
            npc.getNavigation().stop();
            npc.stowWeapons();
            meleeCooldown = 0;
            rangedCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = npc.getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }

            npc.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (meleeCooldown > 0) {
                meleeCooldown--;
            }
            if (rangedCooldown > 0) {
                rangedCooldown--;
            }

            double distanceSqr = npc.distanceToSqr(target);

            if (distanceSqr > MELEE_DISTANCE_SQR && !npc.storedRangedWeapon.isEmpty()) {
                npc.equipRangedWeapon();

                if (distanceSqr > 100.0D || !npc.getSensing().hasLineOfSight(target)) {
                    npc.getNavigation().moveTo(target, 1.0D);
                } else {
                    npc.getNavigation().stop();
                }

                if (distanceSqr <= MAX_RANGED_DISTANCE_SQR
                        && npc.getSensing().hasLineOfSight(target)
                        && rangedCooldown <= 0) {
                    npc.fireRangedWeapon(target);
                    rangedCooldown = npc.storedRangedWeapon.is(Items.CROSSBOW) ? 50 : 35;
                }

                return;
            }

            npc.equipSword();
            npc.getNavigation().moveTo(target, 1.15D);

            if (distanceSqr <= MELEE_DISTANCE_SQR && meleeCooldown <= 0) {
                npc.doHurtTarget(target);
                meleeCooldown = 20;
            }
        }
    }
}
