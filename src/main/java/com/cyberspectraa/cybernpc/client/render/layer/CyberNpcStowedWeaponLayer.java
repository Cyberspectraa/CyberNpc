package com.cyberspectraa.cybernpc.client.render.layer;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class CyberNpcStowedWeaponLayer
        extends RenderLayer<CyberNpcEntity, PlayerModel<CyberNpcEntity>> {

    private final ItemRenderer itemRenderer;

    public CyberNpcStowedWeaponLayer(
            RenderLayerParent<CyberNpcEntity, PlayerModel<CyberNpcEntity>> parent,
            ItemRenderer itemRenderer
    ) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CyberNpcEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (entity.isCombatActive()) {
            return;
        }

        ItemStack weapon = entity.getStoredWeapon();
        if (weapon.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        applyYdmStyleTransform(poseStack, entity.getStowStyle());

        itemRenderer.renderStatic(
                entity,
                weapon,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false,
                poseStack,
                buffer,
                entity.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                entity.getId()
        );

        poseStack.popPose();
    }

    private void applyYdmStyleTransform(PoseStack poseStack, int style) {
        PlayerModel<CyberNpcEntity> model = getParentModel();

        switch (style) {
            // YDM default hotbar slot 2: body
            case 1 -> {
                attach(model.body, poseStack);
                transform(poseStack, 0.35D, 0.35D, 0.00D, 0.0F, 0.0F, 90.0F, 1.00F);
            }

            // YDM default hotbar slot 3: right leg
            case 2 -> {
                attach(model.rightLeg, poseStack);
                transform(poseStack, 0.01D, -0.20D, 0.12D, 0.0F, -90.0F, 0.0F, 1.00F);
            }

            // YDM default hotbar slot 4: left leg
            case 3 -> {
                attach(model.leftLeg, poseStack);
                transform(poseStack, 0.32D, -0.20D, 0.12D, 0.0F, -90.0F, 0.0F, 1.00F);
            }

            // YDM default hotbar slot 5: right leg, angled
            case 4 -> {
                attach(model.rightLeg, poseStack);
                transform(poseStack, 0.01D, -0.24D, 0.00D, 0.0F, -90.0F, -30.0F, 1.00F);
            }

            // YDM default hotbar slot 6: left leg, angled
            case 5 -> {
                attach(model.leftLeg, poseStack);
                transform(poseStack, 0.31D, -0.24D, 0.00D, 0.0F, -90.0F, -30.0F, 1.00F);
            }

            // YDM default hotbar slot 1: body/back.
            // Its configured scale is -20, which corresponds to 80% size.
            default -> {
                attach(model.body, poseStack);
                transform(poseStack, 0.23D, 0.14D, 0.18D, 92.0F, -142.0F, -96.0F, 0.80F);
            }
        }
    }

    private static void attach(ModelPart part, PoseStack poseStack) {
        part.translateAndRotate(poseStack);
    }

    private static void transform(
            PoseStack poseStack,
            double x,
            double y,
            double z,
            float xRot,
            float yRot,
            float zRot,
            float scale
    ) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        poseStack.scale(scale, scale, scale);
    }
}
