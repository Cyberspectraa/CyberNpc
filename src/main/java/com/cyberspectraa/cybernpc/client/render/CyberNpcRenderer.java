package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.client.model.CyberNpcPlayerModel;
import com.cyberspectraa.cybernpc.client.render.layer.CyberNpcHeldItemLayer;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcReactionIcon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class CyberNpcRenderer extends MobRenderer<CyberNpcEntity, CyberNpcPlayerModel> {
    private static final double REACTION_RENDER_DISTANCE_SQR = 32.0D * 32.0D;
    private static final ResourceLocation REACTION_WHITE_TEXTURE =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    private final CyberNpcPlayerModel wideModel;
    private final CyberNpcPlayerModel slimModel;
    private final EntityRenderDispatcher renderDispatcher;
    private final Font font;

    public CyberNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new CyberNpcPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = getModel();
        this.slimModel = new CyberNpcPlayerModel(
                context.bakeLayer(ModelLayers.PLAYER_SLIM),
                true
        );
        this.renderDispatcher = context.getEntityRenderDispatcher();
        this.font = context.getFont();
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
        this.model = entity.isSlimModel() ? slimModel : wideModel;
        setPlayerModelProperties(entity);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        renderReactionBubble(
                entity,
                partialTicks,
                poseStack,
                buffer,
                packedLight
        );
    }

    private void renderReactionBubble(
            CyberNpcEntity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        NpcReactionIcon reaction = entity.getReactionIcon();

        if (reaction == NpcReactionIcon.NONE
                || reaction.glyph().isBlank()
                || entity.isSleeping()
                || renderDispatcher.distanceToSqr(entity)
                > REACTION_RENDER_DISTANCE_SQR) {
            return;
        }

        String glyph = reaction.glyph();
        int glyphWidth = font.width(glyph);

        // Keep even the shortest symbols in a comfortably sized bubble while
        // allowing longer emoticons to expand naturally.
        float halfWidth = Math.max(
                12.0F,
                (glyphWidth + 16.0F) * 0.5F
        );

        float bob = Mth.sin(
                (entity.tickCount + partialTicks) * 0.12F
        ) * 0.035F;

        poseStack.pushPose();
        poseStack.translate(
                0.0D,
                entity.getBbHeight() + 1.02D + bob,
                0.0D
        );
        poseStack.mulPose(renderDispatcher.cameraOrientation());

        // Larger than the old 0.025 scale so reactions remain readable without
        // needing to stand directly beside the NPC.
        poseStack.scale(-0.032F, -0.032F, 0.032F);

        PoseStack.Pose bubblePose = poseStack.last();
        Matrix4f matrix = bubblePose.pose();
        VertexConsumer background = buffer.getBuffer(
                RenderType.entityTranslucent(REACTION_WHITE_TEXTURE)
        );

        int shadow = 0x80000000;
        int outline = 0xFF292725;
        int fill = 0xFFFDF7E6;
        int highlight = 0xFFFFFFFF;

        // Shadow first. Using the same stepped silhouette keeps the bubble from
        // looking like a flat GUI rectangle pasted into the world.
        drawPixelBubble(
                background,
                bubblePose,
                halfWidth,
                2.0F,
                2.0F,
                -0.04F,
                shadow,
                packedLight
        );

        // Dark outer silhouette.
        drawPixelBubble(
                background,
                bubblePose,
                halfWidth,
                0.0F,
                0.0F,
                0.0F,
                outline,
                packedLight
        );

        // Bright inner panel, inset by two pixels from the outline.
        drawBubbleRect(
                background,
                bubblePose,
                -halfWidth + 4.0F,
                -9.0F,
                halfWidth - 4.0F,
                9.0F,
                0.02F,
                fill,
                packedLight
        );
        drawBubbleRect(
                background,
                bubblePose,
                -halfWidth + 2.0F,
                -6.0F,
                halfWidth - 2.0F,
                6.0F,
                0.02F,
                fill,
                packedLight
        );

        // Small top highlight gives the otherwise pixel-flat panel a little
        // depth without making it look modern or glossy.
        drawBubbleRect(
                background,
                bubblePose,
                -halfWidth + 5.0F,
                -8.0F,
                halfWidth - 5.0F,
                -7.0F,
                0.03F,
                highlight,
                packedLight
        );

        // Fill the inside of the stepped speech tail.
        drawBubbleRect(
                background,
                bubblePose,
                -1.0F,
                8.0F,
                4.0F,
                12.0F,
                0.02F,
                fill,
                packedLight
        );
        drawBubbleRect(
                background,
                bubblePose,
                1.0F,
                11.0F,
                4.0F,
                14.0F,
                0.02F,
                fill,
                packedLight
        );

        int iconColor = reactionColor(reaction);
        float iconX = -glyphWidth / 2.0F;
        float iconY = -4.5F;

        // Pixel-style icon shadow for contrast on every biome/lighting setup.
        font.drawInBatch(
                glyph,
                iconX + 1.0F,
                iconY + 1.0F,
                0xB0000000,
                false,
                matrix,
                buffer,
                Font.DisplayMode.POLYGON_OFFSET,
                0,
                LightTexture.FULL_BRIGHT
        );
        font.drawInBatch(
                glyph,
                iconX,
                iconY,
                iconColor,
                false,
                matrix,
                buffer,
                Font.DisplayMode.POLYGON_OFFSET,
                0,
                LightTexture.FULL_BRIGHT
        );

        poseStack.popPose();
    }

    private static void drawPixelBubble(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float halfWidth,
            float offsetX,
            float offsetY,
            float z,
            int color,
            int packedLight
    ) {
        // Non-overlapping strips avoid the coplanar/z-fighting flicker that the
        // old overlapping rectangles could produce at certain camera angles.
        drawBubbleRect(
                consumer,
                pose,
                -halfWidth + 3.0F + offsetX,
                -11.0F + offsetY,
                halfWidth - 3.0F + offsetX,
                -8.0F + offsetY,
                z,
                color,
                packedLight
        );
        drawBubbleRect(
                consumer,
                pose,
                -halfWidth + offsetX,
                -8.0F + offsetY,
                halfWidth + offsetX,
                8.0F + offsetY,
                z,
                color,
                packedLight
        );
        drawBubbleRect(
                consumer,
                pose,
                -halfWidth + 3.0F + offsetX,
                8.0F + offsetY,
                halfWidth - 3.0F + offsetX,
                11.0F + offsetY,
                z,
                color,
                packedLight
        );

        // Two-step pixel tail, also kept non-overlapping.
        drawBubbleRect(
                consumer,
                pose,
                -2.0F + offsetX,
                11.0F + offsetY,
                5.0F + offsetX,
                13.0F + offsetY,
                z,
                color,
                packedLight
        );
        drawBubbleRect(
                consumer,
                pose,
                0.0F + offsetX,
                13.0F + offsetY,
                5.0F + offsetX,
                16.0F + offsetY,
                z,
                color,
                packedLight
        );
    }

    private static void drawBubbleRect(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float left,
            float top,
            float right,
            float bottom,
            float z,
            int color,
            int packedLight
    ) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;

        int light = LightTexture.FULL_BRIGHT;

        bubbleVertex(
                consumer, matrix, normal,
                left, top, z, 0.0F, 0.0F,
                red, green, blue, alpha, light
        );
        bubbleVertex(
                consumer, matrix, normal,
                left, bottom, z, 0.0F, 1.0F,
                red, green, blue, alpha, light
        );
        bubbleVertex(
                consumer, matrix, normal,
                right, bottom, z, 1.0F, 1.0F,
                red, green, blue, alpha, light
        );
        bubbleVertex(
                consumer, matrix, normal,
                right, top, z, 1.0F, 0.0F,
                red, green, blue, alpha, light
        );
    }

    private static void bubbleVertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha,
            int light
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    private static int reactionColor(NpcReactionIcon reaction) {
        return switch (reaction) {
            case HAPPY -> 0xFF2FAF4A;
            case FRIENDLY -> 0xFFF15B8A;
            case GROUP_INVITE -> 0xFF4F86E8;
            case GROUP_ACCEPT -> 0xFF32B55B;
            case ANNOYED -> 0xFFE08B18;
            case ANGRY -> 0xFFF04444;
            case SAD -> 0xFF4B93D1;
            case SCARED -> 0xFF8B6BE8;
            case SURPRISED -> 0xFFF0A126;
            case CONFUSED -> 0xFF557A92;
            case THINKING -> 0xFF5A5A5A;
            default -> 0xFF202020;
        };
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
        return CyberNpcSkinCache.getNpcTexture(entity);
    }
}
