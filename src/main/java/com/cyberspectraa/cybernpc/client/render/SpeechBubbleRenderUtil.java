package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.compat.PehkuiRenderCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class SpeechBubbleRenderUtil {
    public static final double RENDER_DISTANCE_SQR = 32.0D * 32.0D;

    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation(
                    "minecraft",
                    "textures/misc/white.png"
            );

    private static final int BORDER_COLOR = 0xFF2B2927;
    private static final int FILL_COLOR = 0xFFFFFFFF;
    private static final int PLAYER_TEXT_COLOR = 0xFF202020;

    private static final int MAX_TEXT_WIDTH = 104;
    private static final int MAX_TEXT_LINES = 4;
    private static final int LINE_HEIGHT = 9;

    private SpeechBubbleRenderUtil() {
    }

    public static void renderIconBubble(
            LivingEntity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            Font font,
            EntityRenderDispatcher dispatcher,
            String glyph,
            int iconColor
    ) {
        if (glyph == null
                || glyph.isBlank()
                || dispatcher.distanceToSqr(entity)
                > RENDER_DISTANCE_SQR) {
            return;
        }

        int glyphWidth = font.width(glyph);
        float halfWidth = Math.max(
                12.0F,
                (glyphWidth + 16.0F) * 0.5F
        );

        float bob = Mth.sin(
                (entity.tickCount + partialTicks) * 0.12F
        ) * 0.035F;

        poseStack.pushPose();

        // Pehkui scales the entire entity renderer before CyberNpc gets this
        // pose stack. Cancel only that outer model scale so the bubble keeps
        // a stable world-space size and can be positioned from the entity's
        // already-scaled bounding-box height. Without this, a 0.5x Fairy
        // scales the bubble offset a second time and pulls it into the head.
        cancelEntityRenderScale(
                entity,
                partialTicks,
                poseStack
        );

        poseStack.translate(
                0.0D,
                entity.getBbHeight() + 1.02D + bob,
                0.0D
        );
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-0.032F, -0.032F, 0.032F);

        PoseStack.Pose bubblePose = poseStack.last();
        Matrix4f matrix = bubblePose.pose();
        VertexConsumer background = buffer.getBuffer(
                RenderType.entityTranslucent(WHITE_TEXTURE)
        );

        drawFrameAndFill(
                background,
                bubblePose,
                halfWidth,
                -11.0F,
                11.0F
        );

        // Deliberately no text/icon shadow: the white panel provides all the
        // contrast needed and keeps the icon crisp.
        font.drawInBatch(
                glyph,
                -glyphWidth / 2.0F,
                -4.5F,
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

    public static void renderPlayerTextBubble(
            LivingEntity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            Font font,
            EntityRenderDispatcher dispatcher,
            Component message
    ) {
        if (message == null
                || message.getString().isBlank()
                || dispatcher.distanceToSqr(entity)
                > RENDER_DISTANCE_SQR) {
            return;
        }

        List<FormattedCharSequence> split =
                font.split(message, MAX_TEXT_WIDTH);

        if (split.isEmpty()) {
            return;
        }

        List<FormattedCharSequence> lines = new ArrayList<>(
                split.subList(
                        0,
                        Math.min(MAX_TEXT_LINES, split.size())
                )
        );

        if (split.size() > MAX_TEXT_LINES) {
            // Keep the bubble bounded even for unusually long chat messages.
            // The normal chat window still contains the complete message.
            lines.set(
                    MAX_TEXT_LINES - 1,
                    Component.literal("...").getVisualOrderText()
            );
        }

        int maxLineWidth = 0;
        for (FormattedCharSequence line : lines) {
            maxLineWidth = Math.max(
                    maxLineWidth,
                    font.width(line)
            );
        }

        float halfWidth = Math.max(
                18.0F,
                (maxLineWidth + 14.0F) * 0.5F
        );

        float contentHeight = lines.size() * LINE_HEIGHT;
        float bodyBottom = 9.0F;
        float bodyTop = Math.min(
                -11.0F,
                bodyBottom - contentHeight - 10.0F
        );

        float bob = Mth.sin(
                (entity.tickCount + partialTicks) * 0.10F
        ) * 0.025F;

        poseStack.pushPose();

        cancelEntityRenderScale(
                entity,
                partialTicks,
                poseStack
        );

        poseStack.translate(
                0.0D,
                entity.getBbHeight() + 1.10D + bob,
                0.0D
        );
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        PoseStack.Pose bubblePose = poseStack.last();
        Matrix4f matrix = bubblePose.pose();
        VertexConsumer background = buffer.getBuffer(
                RenderType.entityTranslucent(WHITE_TEXTURE)
        );

        drawFrameAndFill(
                background,
                bubblePose,
                halfWidth,
                bodyTop,
                bodyBottom
        );

        float y = bodyTop + 5.0F;
        for (FormattedCharSequence line : lines) {
            float x = -font.width(line) / 2.0F;

            // No text shadow for player speech either.
            font.drawInBatch(
                    line,
                    x,
                    y,
                    PLAYER_TEXT_COLOR,
                    false,
                    matrix,
                    buffer,
                    Font.DisplayMode.POLYGON_OFFSET,
                    0,
                    LightTexture.FULL_BRIGHT
            );

            y += LINE_HEIGHT;
        }

        poseStack.popPose();
    }

    private static void cancelEntityRenderScale(
            LivingEntity entity,
            float partialTicks,
            PoseStack poseStack
    ) {
        PehkuiRenderCompat.ModelScale modelScale =
                PehkuiRenderCompat.getModelScale(
                        entity,
                        partialTicks
                );

        poseStack.scale(
                modelScale.inverseWidth(),
                modelScale.inverseHeight(),
                modelScale.inverseWidth()
        );
    }

    private static void drawFrameAndFill(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float halfWidth,
            float top,
            float bottom
    ) {
        final float z = 0.0F;

        // Stepped top border.
        drawRect(
                consumer, pose,
                -halfWidth + 3.0F, top,
                halfWidth - 3.0F, top + 2.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                -halfWidth + 1.0F, top + 2.0F,
                -halfWidth + 3.0F, top + 4.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                halfWidth - 3.0F, top + 2.0F,
                halfWidth - 1.0F, top + 4.0F,
                z, BORDER_COLOR
        );

        // Side borders.
        drawRect(
                consumer, pose,
                -halfWidth, top + 4.0F,
                -halfWidth + 2.0F, bottom - 4.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                halfWidth - 2.0F, top + 4.0F,
                halfWidth, bottom - 4.0F,
                z, BORDER_COLOR
        );

        // Stepped bottom border.
        drawRect(
                consumer, pose,
                -halfWidth + 1.0F, bottom - 4.0F,
                -halfWidth + 3.0F, bottom - 2.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                halfWidth - 3.0F, bottom - 4.0F,
                halfWidth - 1.0F, bottom - 2.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                -halfWidth + 3.0F, bottom - 2.0F,
                halfWidth - 3.0F, bottom,
                z, BORDER_COLOR
        );

        // White fill occupies separate XY regions from the border.
        drawRect(
                consumer, pose,
                -halfWidth + 3.0F, top + 2.0F,
                halfWidth - 3.0F, top + 4.0F,
                z, FILL_COLOR
        );
        drawRect(
                consumer, pose,
                -halfWidth + 2.0F, top + 4.0F,
                halfWidth - 2.0F, bottom - 4.0F,
                z, FILL_COLOR
        );
        drawRect(
                consumer, pose,
                -halfWidth + 3.0F, bottom - 4.0F,
                halfWidth - 3.0F, bottom - 2.0F,
                z, FILL_COLOR
        );

        // Pixel speech tail beneath the body.
        drawRect(
                consumer, pose,
                -2.0F, bottom,
                0.0F, bottom + 3.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                0.0F, bottom + 2.0F,
                2.0F, bottom + 5.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                4.0F, bottom,
                6.0F, bottom + 3.0F,
                z, BORDER_COLOR
        );
        drawRect(
                consumer, pose,
                2.0F, bottom + 4.0F,
                4.0F, bottom + 6.0F,
                z, BORDER_COLOR
        );

        drawRect(
                consumer, pose,
                0.0F, bottom,
                4.0F, bottom + 2.0F,
                z, FILL_COLOR
        );
        drawRect(
                consumer, pose,
                2.0F, bottom + 2.0F,
                4.0F, bottom + 4.0F,
                z, FILL_COLOR
        );
    }

    private static void drawRect(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float left,
            float top,
            float right,
            float bottom,
            float z,
            int color
    ) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;

        vertex(
                consumer, matrix, normal,
                left, top, z, 0.0F, 0.0F,
                red, green, blue, alpha
        );
        vertex(
                consumer, matrix, normal,
                left, bottom, z, 0.0F, 1.0F,
                red, green, blue, alpha
        );
        vertex(
                consumer, matrix, normal,
                right, bottom, z, 1.0F, 1.0F,
                red, green, blue, alpha
        );
        vertex(
                consumer, matrix, normal,
                right, top, z, 1.0F, 0.0F,
                red, green, blue, alpha
        );
    }

    private static void vertex(
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
            int alpha
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
