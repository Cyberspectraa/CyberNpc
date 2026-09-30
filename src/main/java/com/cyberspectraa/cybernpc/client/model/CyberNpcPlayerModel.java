package com.cyberspectraa.cybernpc.client.model;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public final class CyberNpcPlayerModel extends PlayerModel<CyberNpcEntity> {
    public CyberNpcPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(
            CyberNpcEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (entity.isSleeping()) {
            // A sleeping player keeps their head aligned with the body/bed rather
            // than continuing to track nearby entities.
            head.xRot = 0.0F;
            head.yRot = 0.0F;
            head.zRot = 0.0F;
            hat.copyFrom(head);
        }
    }
}
