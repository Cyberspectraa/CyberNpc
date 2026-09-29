package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class CyberNpcRenderer extends MobRenderer<CyberNpcEntity, PlayerModel<CyberNpcEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CyberNpc.MOD_ID, "textures/entity/cyber_npc.png");

    public CyberNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CyberNpcEntity entity) {
        return TEXTURE;
    }
}
