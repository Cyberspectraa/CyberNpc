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
        applyStowTransform(poseStack, entity.getStowStyle());

        /*
         * FIXED keeps the flat face of handheld items parallel to the body.
         * The previous THIRD_PERSON_RIGHT_HAND transform added its own hand
         * rotation before our stow rotation, which turned swords vertical and
         * axes edge-on through the torso.
         */
        itemRenderer.renderStatic(
                entity,
                weapon,
                ItemDisplayContext.FIXED,
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

    private void applyStowTransform(PoseStack poseStack, int style) {
        PlayerModel<CyberNpcEntity> model = getParentModel();

        switch (style) {
            // Opposite diagonal across the body.
            case 1 -> {
                attach(model.body, poseStack);
                transform(poseStack, 0.35D, 0.35D, 0.00D, 90.0F, 1.70F);
            }

            // Right hip / upper leg.
            case 2 -> {
                attach(model.rightLeg, poseStack);
                transform(poseStack, 0.01D, -0.20D, 0.12D, -10.0F, 1.70F);
            }

            // Left hip / upper leg.
            case 3 -> {
                attach(model.leftLeg, poseStack);
                transform(poseStack, 0.32D, -0.20D, 0.12D, 10.0F, 1.70F);
            }

            // Right lower angled leg position.
            case 4 -> {
                attach(model.rightLeg, poseStack);
                transform(poseStack, 0.01D, -0.24D, 0.00D, -30.0F, 1.70F);
            }

            // Left lower angled leg position.
            case 5 -> {
                attach(model.leftLeg, poseStack);
                transform(poseStack, 0.31D, -0.24D, 0.00D, 30.0F, 1.70F);
            }

            // Main diagonal body/back position.
            default -> {
                attach(model.body, poseStack);
                transform(poseStack, 0.23D, 0.14D, 0.18D, 0.0F, 1.36F);
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
            float zRot,
            float scale
    ) {
        poseStack.translate(x, y, z);

        // Keep the item face flat against the model; only rotate within that plane.
        poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        poseStack.scale(scale, scale, scale);
    }
}
