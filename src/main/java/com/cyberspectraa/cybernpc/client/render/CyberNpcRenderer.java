package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.client.compat.YdmWeaponMasterCompat;
import com.cyberspectraa.cybernpc.client.render.layer.CyberNpcCombatHeldItemLayer;
import com.cyberspectraa.cybernpc.client.render.layer.CyberNpcStowedWeaponLayer;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class CyberNpcRenderer extends MobRenderer<CyberNpcEntity, PlayerModel<CyberNpcEntity>> {
    private static final ResourceLocation DEFAULT_STEVE_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public CyberNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        addLayer(new CyberNpcCombatHeldItemLayer(this, context.getItemInHandRenderer()));

        if (YdmWeaponMasterCompat.isLoaded()) {
            addLayer(new CyberNpcStowedWeaponLayer(this, context.getItemRenderer()));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CyberNpcEntity entity) {
        return DEFAULT_STEVE_TEXTURE;
    }
}
