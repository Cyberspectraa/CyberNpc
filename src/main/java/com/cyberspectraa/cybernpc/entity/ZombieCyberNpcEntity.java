package com.cyberspectraa.cybernpc.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ZombieCyberNpcEntity extends Zombie {
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

    private final WildNpcInventory preservedInventory = new WildNpcInventory();

    public ZombieCyberNpcEntity(
            EntityType<? extends Zombie> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_GENDER, NpcAppearance.Gender.MALE.serializedName());
        entityData.define(DATA_SKIN_TONE, 0);
        entityData.define(DATA_EYE_STYLE, 0);
        entityData.define(DATA_HAIR_STYLE, 0);
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
        return NpcAppearance.sanitizeSkinTone(entityData.get(DATA_SKIN_TONE));
    }

    public int getEyeStyleIndex() {
        return NpcAppearance.sanitizeEyeStyle(entityData.get(DATA_EYE_STYLE));
    }

    public int getHairStyleIndex() {
        return NpcAppearance.sanitizeHairStyle(entityData.get(DATA_HAIR_STYLE));
    }

    public void inheritFrom(CyberNpcEntity source) {
        entityData.set(
                DATA_GENDER,
                source.getAppearanceGender().serializedName()
        );
        entityData.set(DATA_SKIN_TONE, source.getSkinToneIndex());
        entityData.set(DATA_EYE_STYLE, source.getEyeStyleIndex());
        entityData.set(DATA_HAIR_STYLE, source.getHairStyleIndex());

        preservedInventory.load(source.saveInventoryForZombieConversion());

        ItemStack activeFood = source.copyActiveFoodForZombieConversion();
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
        tag.putInt("CyberNpcAppearanceSkinTone", getSkinToneIndex());
        tag.putInt("CyberNpcAppearanceEyes", getEyeStyleIndex());
        tag.putInt("CyberNpcAppearanceHair", getHairStyleIndex());
        tag.put("CyberNpcPreservedInventory", preservedInventory.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("CyberNpcAppearanceGender")) {
            entityData.set(
                    DATA_GENDER,
                    NpcAppearance.Gender.fromSerializedName(
                            tag.getString("CyberNpcAppearanceGender")
                    ).serializedName()
            );
        }

        if (tag.contains("CyberNpcAppearanceSkinTone")) {
            entityData.set(
                    DATA_SKIN_TONE,
                    NpcAppearance.sanitizeSkinTone(
                            tag.getInt("CyberNpcAppearanceSkinTone")
                    )
            );
        }

        if (tag.contains("CyberNpcAppearanceEyes")) {
            entityData.set(
                    DATA_EYE_STYLE,
                    NpcAppearance.sanitizeEyeStyle(
                            tag.getInt("CyberNpcAppearanceEyes")
                    )
            );
        }

        if (tag.contains("CyberNpcAppearanceHair")) {
            entityData.set(
                    DATA_HAIR_STYLE,
                    NpcAppearance.sanitizeHairStyle(
                            tag.getInt("CyberNpcAppearanceHair")
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
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        for (ItemStack stack : preservedInventory.removeAll()) {
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        }
    }
}
