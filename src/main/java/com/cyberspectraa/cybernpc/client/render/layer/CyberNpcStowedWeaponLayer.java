package com.cyberspectraa.cybernpc.client.render.layer;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
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

        ItemStack weapon = entity.getMainHandItem();
        if (weapon.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // YDM's Weapon Master uses body/leg attachment points for stowed hotbar items.
        // CyberNpc mirrors the body-mounted presentation for its custom mob renderer.
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.28D, 0.16D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);

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
}
