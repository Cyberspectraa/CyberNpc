package com.cyberspectraa.cybernpc.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;

public final class ZombieCyberNpcEntity extends Zombie {
    private static final int RETARGET_INTERVAL_TICKS = 5;
    private static final double RETARGET_SWITCH_ADVANTAGE_SQR = 1.0D;

    private static final EntityDataAccessor<String> DATA_GENDER =
            SynchedEntityData.defineId(
                    ZombieCyberNpcEntity.class,
                    EntityDataSerializers.STRING
            );

    private static final EntityDataAccessor<Integer> DATA_SKIN_TONE =
            SynchedEntityData.defineId(
                    ZombieCyberNpcEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> DATA_EYE_STYLE =
            SynchedEntityData.defineId(
                    ZombieCyberNpcEntity.class,
                    EntityDataSerializers.INT
            );

    private static final EntityDataAccessor<Integer> DATA_HAIR_STYLE =
            SynchedEntityData.defineId(
                    ZombieCyberNpcEntity.class,
                    EntityDataSerializers.INT
            );

    private final WildNpcInventory preservedInventory =
            new WildNpcInventory();

    public ZombieCyberNpcEntity(
            EntityType<? extends Zombie> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(
                DATA_GENDER,
                NpcAppearance.Gender.MALE.serializedName()
        );
        entityData.define(DATA_SKIN_TONE, 0);
        entityData.define(DATA_EYE_STYLE, 0);
        entityData.define(DATA_HAIR_STYLE, 0);
    }

    /**
     * Converted CyberNpc zombies are always adults. Overriding this at the
     * source also migrates old saved CyberNpc zombies that happened to have
     * IsBaby=true when their NBT is read.
     */
    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        // Do not allow Zombie.finalizeSpawn to roll a baby/chicken-jockey
        // group for this custom converted zombie.
        SpawnGroupData result = super.finalizeSpawn(
                level,
                difficulty,
                spawnType,
                new Zombie.ZombieGroupData(false, false),
                dataTag
        );
        super.setBaby(false);
        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide || !isAlive()) {
            return;
        }

        if (isBaby()) {
            super.setBaby(false);
        }

        if (tickCount % RETARGET_INTERVAL_TICKS == 0) {
            retargetClosestValidEntity();
        }
    }

    private void retargetClosestValidEntity() {
        double followRange = Math.max(
                24.0D,
                getAttributeValue(Attributes.FOLLOW_RANGE)
        );

        AABB search = getBoundingBox().inflate(
                followRange,
                Math.min(16.0D, followRange),
                followRange
        );

        LivingEntity closest = level().getEntitiesOfClass(
                        LivingEntity.class,
                        search,
                        this::isValidRetargetCandidate
                ).stream()
                .filter(candidate ->
                        distanceToSqr(candidate) <= 9.0D
                                || getSensing().hasLineOfSight(candidate)
                )
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);

        LivingEntity current = getTarget();

        if (closest == null) {
            if (current != null
                    && (!current.isAlive()
                    || distanceToSqr(current) > followRange * followRange)) {
                setTarget(null);
            }
            return;
        }

        if (current == null
                || !current.isAlive()
                || !isValidRetargetCandidate(current)
                || !getSensing().hasLineOfSight(current)
                || distanceToSqr(closest)
                + RETARGET_SWITCH_ADVANTAGE_SQR
                < distanceToSqr(current)) {
            setTarget(closest);
        }
    }

    private boolean isValidRetargetCandidate(LivingEntity candidate) {
        if (candidate == this
                || !candidate.isAlive()
                || !canAttack(candidate)) {
            return false;
        }

        if (candidate instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        if (candidate instanceof CyberNpcEntity npc) {
            return !npc.isZombifying();
        }

        if (candidate instanceof AbstractVillager
                || candidate instanceof IronGolem) {
            return true;
        }

        // Preserve retaliation against an unusual mob that has actively
        // attacked this zombie, but never create zombie-on-zombie infighting.
        return candidate instanceof Mob mob
                && !(mob instanceof Zombie)
                && mob.getTarget() == this;
    }

    public NpcAppearance.Gender getAppearanceGender() {
        return NpcAppearance.Gender.fromSerializedName(
                entityData.get(DATA_GENDER)
        );
    }

    public boolean isSlimModel() {
        return getAppearanceGender().slim();
    }

    public int getSkinToneIndex() {
        return NpcAppearance.sanitizeSkinTone(
                entityData.get(DATA_SKIN_TONE)
        );
    }

    public int getEyeStyleIndex() {
        return NpcAppearance.sanitizeEyeStyle(
                entityData.get(DATA_EYE_STYLE)
        );
    }

    public int getHairStyleIndex() {
        return NpcAppearance.sanitizeHairStyle(
                entityData.get(DATA_HAIR_STYLE)
        );
    }

    public void inheritFrom(CyberNpcEntity source) {
        super.setBaby(false);

        entityData.set(
                DATA_GENDER,
                source.getAppearanceGender().serializedName()
        );
        entityData.set(DATA_SKIN_TONE, source.getSkinToneIndex());
        entityData.set(DATA_EYE_STYLE, source.getEyeStyleIndex());
        entityData.set(DATA_HAIR_STYLE, source.getHairStyleIndex());

        preservedInventory.load(
                source.saveInventoryForZombieConversion()
        );

        ItemStack activeFood =
                source.copyActiveFoodForZombieConversion();
        if (!activeFood.isEmpty()) {
            preservedInventory.add(activeFood);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack sourceStack = source.getItemBySlot(slot);
            setItemSlot(slot, sourceStack.copy());

            if (!sourceStack.isEmpty()) {
                setDropChance(slot, 2.0F);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(
                "CyberNpcAppearanceGender",
                getAppearanceGender().serializedName()
        );
        tag.putInt(
                "CyberNpcAppearanceSkinTone",
                getSkinToneIndex()
        );
        tag.putInt(
                "CyberNpcAppearanceEyes",
                getEyeStyleIndex()
        );
        tag.putInt(
                "CyberNpcAppearanceHair",
                getHairStyleIndex()
        );
        tag.put(
                "CyberNpcPreservedInventory",
                preservedInventory.save()
        );
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        super.setBaby(false);

        if (tag.contains("CyberNpcAppearanceGender")) {
            entityData.set(
                    DATA_GENDER,
                    NpcAppearance.Gender.fromSerializedName(
                            tag.getString(
                                    "CyberNpcAppearanceGender"
                            )
                    ).serializedName()
            );
        }

        if (tag.contains("CyberNpcAppearanceSkinTone")) {
            entityData.set(
                    DATA_SKIN_TONE,
                    NpcAppearance.sanitizeSkinTone(
                            tag.getInt(
                                    "CyberNpcAppearanceSkinTone"
                            )
                    )
            );
        }

        if (tag.contains("CyberNpcAppearanceEyes")) {
            entityData.set(
                    DATA_EYE_STYLE,
                    NpcAppearance.sanitizeEyeStyle(
                            tag.getInt(
                                    "CyberNpcAppearanceEyes"
                            )
                    )
            );
        }

        if (tag.contains("CyberNpcAppearanceHair")) {
            entityData.set(
                    DATA_HAIR_STYLE,
                    NpcAppearance.sanitizeHairStyle(
                            tag.getInt(
                                    "CyberNpcAppearanceHair"
                            )
                    )
            );
        }

        if (tag.contains("CyberNpcPreservedInventory")) {
            preservedInventory.load(
                    tag.getList(
                            "CyberNpcPreservedInventory",
                            net.minecraft.nbt.Tag.TAG_COMPOUND
                    )
            );
        }
    }

    @Override
    protected void dropCustomDeathLoot(
            DamageSource source,
            int looting,
            boolean recentlyHit
    ) {
        super.dropCustomDeathLoot(
                source,
                looting,
                recentlyHit
        );

        for (ItemStack stack : preservedInventory.removeAll()) {
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        }
    }
}
