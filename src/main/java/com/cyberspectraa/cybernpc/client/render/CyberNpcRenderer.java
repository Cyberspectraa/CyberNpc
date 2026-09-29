package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.client.render.layer.CyberNpcCombatHeldItemLayer;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public final class CyberNpcRenderer extends MobRenderer<CyberNpcEntity, PlayerModel<CyberNpcEntity>> {
    private static final ResourceLocation DEFAULT_STEVE_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public CyberNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new CyberNpcCombatHeldItemLayer(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(
            CyberNpcEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        PlayerModel<CyberNpcEntity> model = getModel();
        model.crouching = entity.isShiftKeyDown();
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;

        if (entity.getMainHandItem().is(Items.BOW) && entity.isAimingBow()) {
            model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        } else if (entity.getMainHandItem().is(Items.CROSSBOW) && entity.isChargingCrossbow()) {
            model.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        } else if (entity.getMainHandItem().is(Items.CROSSBOW) && entity.isHoldingChargedCrossbow()) {
            model.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        } else if (!entity.getMainHandItem().isEmpty()) {
            model.rightArmPose = HumanoidModel.ArmPose.ITEM;
        } else {
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CyberNpcEntity entity) {
        return DEFAULT_STEVE_TEXTURE;
    }
}
