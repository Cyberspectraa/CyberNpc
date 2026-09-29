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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.List;

public class CyberNpcEntity extends PathfinderMob {
    private static final double WILD_HELP_RADIUS = 20.0D;
    private static final double WILD_DISENGAGE_DISTANCE = 40.0D;
    private static final int WILD_DISENGAGE_TICKS = 100;
    private static final int WILD_MIN_AGGRESSION = 10;
    private static final int WILD_MAX_AGGRESSION = 80;

    private static final EntityDataAccessor<String> DATA_ROLE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_CAN_WANDER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> DATA_NPC_TYPE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_COMBAT_ACTIVE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private int aggressionLevel = -1;
    private int provocation;
    private int outOfRangeTicks;
    private ItemStack storedWeapon = ItemStack.EMPTY;

    public CyberNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
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

    private void ensureWildCombatProfile() {
        if (getNpcType() != NpcType.WILD) {
            return;
        }

        if (aggressionLevel < 0) {
            aggressionLevel = WILD_MIN_AGGRESSION
                    + getRandom().nextInt(WILD_MAX_AGGRESSION - WILD_MIN_AGGRESSION + 1);
        }

        // Migrates v0.3.0 Wild NPCs that may already have their passive weapon equipped.
        if (!isCombatActive() && !getMainHandItem().isEmpty()) {
            storeCurrentWeapon();
        }

        if (storedWeapon.isEmpty() && getMainHandItem().isEmpty()) {
            storedWeapon = CyberNpcWeaponPool.randomWildWeapon(getRandom());
        }
    }

    private void drawStoredWeapon() {
        if (getNpcType() != NpcType.WILD || !getMainHandItem().isEmpty()) {
            return;
        }

        if (storedWeapon.isEmpty()) {
            storedWeapon = CyberNpcWeaponPool.randomWildWeapon(getRandom());
        }

        setItemSlot(EquipmentSlot.MAINHAND, storedWeapon.copy());
        storedWeapon = ItemStack.EMPTY;
    }

    private void storeCurrentWeapon() {
        ItemStack equipped = getMainHandItem();

        if (!equipped.isEmpty()) {
            storedWeapon = equipped.copy();
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    /**
     * Shared weapon visibility hook for Wild NPC activities.
     * Combat uses this now; a future hunting system can use the same draw/stow behavior.
     */
    public void setWeaponDrawn(boolean drawn) {
        if (getNpcType() != NpcType.WILD) {
            return;
        }

        if (drawn) {
            drawStoredWeapon();
        } else {
            storeCurrentWeapon();
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
            ensureWildCombatProfile();
            setWeaponDrawn(false);
        }

        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide || getNpcType() != NpcType.WILD) {
            return;
        }

        ensureWildCombatProfile();

        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && !isCombatActive()) {
            setCombatActive(true);
            setWeaponDrawn(true);
        }

        tickWildCombat();
    }

    private void tickWildCombat() {
        if (!isCombatActive()) {
            if (provocation > 0 && tickCount % 100 == 0) {
                provocation = Math.max(0, provocation - 10);
            }

            setWeaponDrawn(false);
            return;
        }

        LivingEntity target = getTarget();

        if (target == null || !target.isAlive()) {
            calmWildNpc();
            return;
        }

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            calmWildNpc();
            return;
        }

        setWeaponDrawn(true);

        if (distanceToSqr(target) > WILD_DISENGAGE_DISTANCE * WILD_DISENGAGE_DISTANCE) {
            outOfRangeTicks++;

            if (outOfRangeTicks >= WILD_DISENGAGE_TICKS) {
                calmWildNpc();
            }
        } else {
            outOfRangeTicks = 0;
        }
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
            ensureWildCombatProfile();

            if (!isCombatActive()) {
                int addedProvocation = Math.max(8, Mth.ceil(amount * 6.0F));
                provocation = Mth.clamp(provocation + addedProvocation, 0, 100);

                int hostilityThreshold = 100 - aggressionLevel;

                if (provocation >= hostilityThreshold) {
                    beginWildCombat(player, true);
                }
            }
        }

        return damaged;
    }

    private void beginWildCombat(LivingEntity target, boolean callForHelp) {
        setTarget(target);
        setCombatActive(true);
        setWeaponDrawn(true);
        provocation = 100;
        outOfRangeTicks = 0;

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
            npc.ensureWildCombatProfile();
            npc.beginWildCombat(target, false);
        }
    }

    private void calmWildNpc() {
        setTarget(null);
        setCombatActive(false);
        setWeaponDrawn(false);
        getNavigation().stop();
        provocation = 0;
        outOfRangeTicks = 0;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            String displayName = getCustomName() != null ? getCustomName().getString() : "Cyber NPC";
            player.sendSystemMessage(Component.literal(displayName + " — " + getRole()));
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

        if (!storedWeapon.isEmpty()) {
            tag.put("CyberNpcStoredWeapon", storedWeapon.save(new CompoundTag()));
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

        storedWeapon = tag.contains("CyberNpcStoredWeapon")
                ? ItemStack.of(tag.getCompound("CyberNpcStoredWeapon"))
                : ItemStack.EMPTY;

        setCombatActive(false);
        provocation = 0;
        outOfRangeTicks = 0;

        if (getNpcType() == NpcType.WILD) {
            ensureWildCombatProfile();
            setWeaponDrawn(false);
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
            return !npc.isCombatActive() && npc.canWander() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !npc.isCombatActive() && npc.canWander() && super.canContinueToUse();
        }
    }
}
