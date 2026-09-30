package com.cyberspectraa.cybernpc.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ZombificationEffect extends MobEffect {
    public static final int DURATION_TICKS = 20 * 120;

    public ZombificationEffect() {
        super(MobEffectCategory.HARMFUL, 0x4F7F42);
    }
}
