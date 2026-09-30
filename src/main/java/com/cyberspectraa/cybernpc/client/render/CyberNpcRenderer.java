package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.client.model.CyberNpcPlayerModel;
import com.cyberspectraa.cybernpc.client.render.layer.CyberNpcHeldItemLayer;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;

public final class CyberNpcRenderer extends MobRenderer<CyberNpcEntity, CyberNpcPlayerModel> {
    private static final ResourceLocation DEFAULT_STEVE_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public CyberNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new CyberNpcPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
        addLayer(new CyberNpcHeldItemLayer(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(
            CyberNpcEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        setPlayerModelProperties(entity);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void setPlayerModelProperties(CyberNpcEntity entity) {
        CyberNpcPlayerModel model = getModel();
        model.setAllVisible(true);
        model.crouching = entity.isShiftKeyDown();

        HumanoidModel.ArmPose mainPose = getArmPose(entity, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose offPose = getArmPose(entity, InteractionHand.OFF_HAND);

        if (mainPose.isTwoHanded()) {
            offPose = entity.getOffhandItem().isEmpty()
                    ? HumanoidModel.ArmPose.EMPTY
                    : HumanoidModel.ArmPose.ITEM;
        }

        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = mainPose;
            model.leftArmPose = offPose;
        } else {
            model.rightArmPose = offPose;
            model.leftArmPose = mainPose;
        }
    }

    private static HumanoidModel.ArmPose getArmPose(CyberNpcEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand);

        if (stack.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }

        if (hand == InteractionHand.MAIN_HAND
                && stack.is(Items.BOW)
                && entity.isAimingBow()) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }

        if (entity.getUsedItemHand() == hand && entity.getUseItemRemainingTicks() > 0) {
            UseAnim useAnim = stack.getUseAnimation();

            if (useAnim == UseAnim.BLOCK) {
                return HumanoidModel.ArmPose.BLOCK;
            }
            if (useAnim == UseAnim.BOW) {
                return HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
            if (useAnim == UseAnim.SPEAR) {
                return HumanoidModel.ArmPose.THROW_SPEAR;
            }
            if (useAnim == UseAnim.CROSSBOW) {
                return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            }
            if (useAnim == UseAnim.SPYGLASS) {
                return HumanoidModel.ArmPose.SPYGLASS;
            }
            if (useAnim == UseAnim.TOOT_HORN) {
                return HumanoidModel.ArmPose.TOOT_HORN;
            }
            if (useAnim == UseAnim.BRUSH) {
                return HumanoidModel.ArmPose.BRUSH;
            }
        }

        if (stack.is(Items.CROSSBOW)
                && (entity.isHoldingChargedCrossbow() || CrossbowItem.isCharged(stack))) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }

        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    protected void scale(
            CyberNpcEntity entity,
            PoseStack poseStack,
            float partialTick
    ) {
        poseStack.scale(0.9722F, 0.9722F, 0.9722F);
    }

    @Override
    public Vec3 getRenderOffset(CyberNpcEntity entity, float partialTick) {
        return entity.isShiftKeyDown()
                ? new Vec3(0.0D, -0.125D, 0.0D)
                : super.getRenderOffset(entity, partialTick);
    }

    @Override
    protected void setupRotations(
            CyberNpcEntity entity,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick
    ) {
        float swimAmount = entity.getSwimAmount(partialTick);

        if (entity.isFallFlying()) {
            super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);

            float flyingTicks = (float) entity.getFallFlyingTicks() + partialTick;
            float flightBlend = Mth.clamp(flyingTicks * flyingTicks / 100.0F, 0.0F, 1.0F);

            if (!entity.isAutoSpinAttack()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(
                        flightBlend * (-90.0F - entity.getXRot())
                ));
            }

            Vec3 view = entity.getViewVector(partialTick);
            Vec3 movement = entity.getDeltaMovement();
            double movementHorizontal = movement.horizontalDistanceSqr();
            double viewHorizontal = view.horizontalDistanceSqr();

            if (movementHorizontal > 0.0D && viewHorizontal > 0.0D) {
                double dot = (movement.x * view.x + movement.z * view.z)
                        / Math.sqrt(movementHorizontal * viewHorizontal);
                dot = Mth.clamp(dot, -1.0D, 1.0D);
                double cross = movement.x * view.z - movement.z * view.x;

                poseStack.mulPose(Axis.YP.rotation(
                        (float) (Math.signum(cross) * Math.acos(dot))
                ));
            }

            return;
        }

        if (swimAmount > 0.0F) {
            super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);

            float swimPitch = entity.isInWater()
                    ? -90.0F - entity.getXRot()
                    : -90.0F;

            poseStack.mulPose(Axis.XP.rotationDegrees(
                    Mth.lerp(swimAmount, 0.0F, swimPitch)
            ));

            if (entity.isVisuallySwimming()) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }

            return;
        }

        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(CyberNpcEntity entity) {
        return DEFAULT_STEVE_TEXTURE;
    }
}
