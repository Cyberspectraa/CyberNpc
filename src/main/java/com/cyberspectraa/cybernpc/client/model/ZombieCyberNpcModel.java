package com.cyberspectraa.cybernpc.client.model;

import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public final class ZombieCyberNpcModel extends PlayerModel<ZombieCyberNpcEntity> {
    public ZombieCyberNpcModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(
            ZombieCyberNpcEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        super.setupAnim(
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                netHeadYaw,
                headPitch
        );

        AnimationUtils.animateZombieArms(
                leftArm,
                rightArm,
                entity.isAggressive(),
                attackTime,
                ageInTicks
        );

        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }
}
