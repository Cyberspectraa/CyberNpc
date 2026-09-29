package com.cyberspectraa.cybernpc.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class CyberNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_ROLE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> DATA_CAN_WANDER =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<String> DATA_NPC_TYPE =
            SynchedEntityData.defineId(CyberNpcEntity.class, EntityDataSerializers.STRING);

    public CyberNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
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

        return result;
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
            return npc.canWander() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return npc.canWander() && super.canContinueToUse();
        }
    }
}
