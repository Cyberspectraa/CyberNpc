package com.cyberspectraa.cybernpc.client.model;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

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
            return;
        }

        if (entity.isSpellCastingVisual()) {
            float lookPitch = headPitch * Mth.DEG_TO_RAD;
            int mode = entity.getSpellCastingVisualMode();

            if (mode == CyberNpcEntity.SPELL_CAST_MODE_LONG) {
                // Long Iron's casts hold both arms forward for the actual cast
                // duration, matching the sustained mob-casting silhouette.
                float pulse = Mth.sin(ageInTicks * 0.22F) * 0.035F;
                float castPitch = -1.42F + lookPitch * 0.30F + pulse;

                rightArm.xRot = castPitch;
                leftArm.xRot = castPitch;
                rightArm.yRot = -0.30F;
                leftArm.yRot = 0.30F;
                rightArm.zRot = 0.12F;
                leftArm.zRot = -0.12F;
            } else {
                // Instant spells use a shorter forward thrust while the offhand
                // keeps the real Iron's spellbook readable.
                float thrust = Mth.sin(Math.min(1.0F, entity.getAttackAnim(0.0F) + 0.55F) * Mth.PI);
                rightArm.xRot = -1.50F + lookPitch * 0.25F - thrust * 0.12F;
                rightArm.yRot = -0.10F;
                rightArm.zRot = 0.02F;

                leftArm.xRot = -0.78F + lookPitch * 0.12F;
                leftArm.yRot = 0.42F;
                leftArm.zRot = -0.16F;
            }

            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
        }
    }
}
