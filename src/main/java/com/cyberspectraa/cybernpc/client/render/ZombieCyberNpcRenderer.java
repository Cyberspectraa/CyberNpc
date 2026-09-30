package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.client.model.ZombieCyberNpcModel;
import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class ZombieCyberNpcRenderer
        extends MobRenderer<ZombieCyberNpcEntity, ZombieCyberNpcModel> {
    private final ZombieCyberNpcModel wideModel;
    private final ZombieCyberNpcModel slimModel;

    public ZombieCyberNpcRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new ZombieCyberNpcModel(
                        context.bakeLayer(ModelLayers.ZOMBIE),
                        false
                ),
                0.5F
        );

        this.wideModel = getModel();

        // Important: female zombies intentionally bake the SAME vanilla zombie
        // layer. EMF/Fresh Animations therefore see the normal zombie model
        // path instead of CyberNpc's former custom/player-derived slim layer.
        this.slimModel = new ZombieCyberNpcModel(
                context.bakeLayer(ModelLayers.ZOMBIE),
                true
        );

        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(
                        context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)
                ),
                new HumanoidModel<>(
                        context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)
                ),
                context.getModelManager()
        ));

        addLayer(new ItemInHandLayer<>(
                this,
                context.getItemInHandRenderer()
        ));
    }

    @Override
    public void render(
            ZombieCyberNpcEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        this.model = entity.isSlimModel() ? slimModel : wideModel;

        super.render(
                entity,
                entityYaw,
                partialTicks,
                poseStack,
                buffer,
                packedLight
        );
    }

    @Override
    protected void scale(
            ZombieCyberNpcEntity entity,
            PoseStack poseStack,
            float partialTick
    ) {
        poseStack.scale(0.9722F, 0.9722F, 0.9722F);
    }

    @Override
    public ResourceLocation getTextureLocation(
            ZombieCyberNpcEntity entity
    ) {
        return CyberNpcSkinCache.getZombieTexture(entity);
    }
}
