package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Choso plush with Minecraft's actual player-model geometry instead
 * of baking the doll through an OBJ.  The proportions are a chibi half-scale
 * player body with an enlarged head and the original 64x64 skin layout.
 */
public final class ChosoPlushItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CyberNpc.MOD_ID, "textures/item/choso_plush.png");

    private static final float BODY_SCALE = 0.5F;
    private static final float HEAD_SCALE = 5.5F / 8.0F;
    private static final float HAT_SCALE = 6.0F / 9.0F;

    private static final float LEG_PITCH = -79.0F * Mth.DEG_TO_RAD;
    private static final float LEG_YAW = 22.5F * Mth.DEG_TO_RAD;
    private static final float ARM_PITCH = -39.75778F * Mth.DEG_TO_RAD;
    private static final float ARM_YAW = 4.81281F * Mth.DEG_TO_RAD;
    private static final float ARM_ROLL = 5.7589F * Mth.DEG_TO_RAD;

    private final PlayerModel<LivingEntity> model;

    public ChosoPlushItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
        this.model = new PlayerModel<>(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER),
                false
        );
        this.model.setAllVisible(true);
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();

        // Keep the model centred in normal item-model coordinates.  Minecraft
        // entity models use +Y downward, so Y is flipped exactly once here.
        poseStack.translate(0.5F, 0.55F, 0.5F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        poseModel();

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                packedOverlay,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        poseStack.popPose();
    }

    private void poseModel() {
        resetPart(model.head);
        resetPart(model.hat);
        resetPart(model.body);
        resetPart(model.jacket);
        resetPart(model.rightArm);
        resetPart(model.rightSleeve);
        resetPart(model.leftArm);
        resetPart(model.leftSleeve);
        resetPart(model.rightLeg);
        resetPart(model.rightPants);
        resetPart(model.leftLeg);
        resetPart(model.leftPants);

        // Head: the source doll has a 5.5-unit base head and a 6-unit outer
        // head layer.  Scaling vanilla player parts preserves every normal
        // Minecraft skin UV while matching those proportions exactly.
        setScale(model.head, HEAD_SCALE);
        model.hat.copyFrom(model.head);
        setScale(model.hat, HAT_SCALE);

        setScale(model.body, BODY_SCALE);
        model.jacket.copyFrom(model.body);
        setScale(model.jacket, BODY_SCALE);

        // Wide player arms scaled to half size are 2x6x2, matching the doll.
        model.rightArm.setPos(-2.5F, 1.0F, 0.21443F);
        model.rightArm.xRot = ARM_PITCH;
        model.rightArm.yRot = ARM_YAW;
        model.rightArm.zRot = ARM_ROLL;
        setScale(model.rightArm, BODY_SCALE);
        model.rightSleeve.copyFrom(model.rightArm);
        setScale(model.rightSleeve, BODY_SCALE);

        model.leftArm.setPos(2.5F, 1.0F, 0.21443F);
        model.leftArm.xRot = ARM_PITCH;
        model.leftArm.yRot = -ARM_YAW;
        model.leftArm.zRot = -ARM_ROLL;
        setScale(model.leftArm, BODY_SCALE);
        model.leftSleeve.copyFrom(model.leftArm);
        setScale(model.leftSleeve, BODY_SCALE);

        // Seated legs, using the source doll's pose rather than a standing
        // player pose.
        model.rightLeg.setPos(-1.0F, 6.0F, 0.50F);
        model.rightLeg.xRot = LEG_PITCH;
        model.rightLeg.yRot = LEG_YAW;
        setScale(model.rightLeg, BODY_SCALE);
        model.rightPants.copyFrom(model.rightLeg);
        setScale(model.rightPants, BODY_SCALE);

        model.leftLeg.setPos(1.0F, 6.0F, 0.45431F);
        model.leftLeg.xRot = LEG_PITCH;
        model.leftLeg.yRot = -LEG_YAW;
        setScale(model.leftLeg, BODY_SCALE);
        model.leftPants.copyFrom(model.leftLeg);
        setScale(model.leftPants, BODY_SCALE);
    }

    private static void resetPart(ModelPart part) {
        part.setPos(0.0F, 0.0F, 0.0F);
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
        part.xScale = 1.0F;
        part.yScale = 1.0F;
        part.zScale = 1.0F;
        part.visible = true;
    }

    private static void setScale(ModelPart part, float scale) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }
}
