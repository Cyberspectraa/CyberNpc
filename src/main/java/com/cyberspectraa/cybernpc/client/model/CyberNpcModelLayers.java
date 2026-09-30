package com.cyberspectraa.cybernpc.client.model;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public final class CyberNpcModelLayers {
    public static final ModelLayerLocation ZOMBIE_CYBER_NPC_SLIM =
            new ModelLayerLocation(
                    new ResourceLocation(
                            CyberNpc.MOD_ID,
                            "zombie_cyber_npc"
                    ),
                    "slim"
            );

    public static LayerDefinition createSlimZombieLayer() {
        // Player slim geometry gives the converted female zombie its 3-pixel
        // Alex-width arms. The renderer still wraps this geometry in
        // ZombieModel, so player animation packs do not own the animation.
        return LayerDefinition.create(
                PlayerModel.createMesh(CubeDeformation.NONE, true),
                64,
                64
        );
    }

    private CyberNpcModelLayers() {
    }
}
