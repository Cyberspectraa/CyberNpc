package com.cyberspectraa.cybernpc.client.model;

import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Converted CyberNpc zombies always use Minecraft's real zombie model path so
 * EMF/Fresh Animations can see the same vanilla zombie model entry point.
 *
 * Female zombies only narrow the vanilla arm parts after normal zombie
 * animation setup. This keeps the Alex-like silhouette without switching the
 * entity back onto a PlayerModel animation path.
 */
public final class ZombieCyberNpcModel
        extends ZombieModel<ZombieCyberNpcEntity> {
    private final boolean slimArms;

    public ZombieCyberNpcModel(
            ModelPart root,
            boolean slimArms
    ) {
        super(root);
        this.slimArms = slimArms;
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

        float armScale = slimArms ? 0.75F : 1.0F;
        leftArm.xScale = armScale;
        rightArm.xScale = armScale;
    }
}
