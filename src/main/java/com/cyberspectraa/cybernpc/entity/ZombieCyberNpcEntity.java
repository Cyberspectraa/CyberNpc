package com.cyberspectraa.cybernpc.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public final class ZombieCyberNpcEntity extends Zombie {
    public ZombieCyberNpcEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }
}
