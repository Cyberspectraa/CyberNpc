
package com.cyberspectraa.cybernpc.client.model;

import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Uses Minecraft's zombie animation model rather than PlayerModel.
 *
 * The root may come from either the normal zombie layer or CyberNpc's slim
 * humanoid layer. Both expose the standard humanoid part names ZombieModel
 * expects, while the slim layer keeps female/Alex-width arms.
 */
public final class ZombieCyberNpcModel
        extends ZombieModel<ZombieCyberNpcEntity> {
    public ZombieCyberNpcModel(ModelPart root) {
        super(root);
    }
}
