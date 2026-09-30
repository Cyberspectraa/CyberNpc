package com.cyberspectraa.cybernpc.client.render.layer;

import com.cyberspectraa.cybernpc.client.model.CyberNpcPlayerModel;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;

public final class CyberNpcHeldItemLayer
        extends ItemInHandLayer<CyberNpcEntity, CyberNpcPlayerModel> {

    public CyberNpcHeldItemLayer(
            RenderLayerParent<CyberNpcEntity, CyberNpcPlayerModel> parent,
            ItemInHandRenderer itemInHandRenderer
    ) {
        super(parent, itemInHandRenderer);
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
        if (entity.getMainHandItem().isEmpty() && entity.getOffhandItem().isEmpty()) {
            return;
        }

        super.render(
                poseStack,
                buffer,
                packedLight,
                entity,
                limbSwing,
                limbSwingAmount,
                partialTick,
                ageInTicks,
                netHeadYaw,
                headPitch
        );
    }
}
