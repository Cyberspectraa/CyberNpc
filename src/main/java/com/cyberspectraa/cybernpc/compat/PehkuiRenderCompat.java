package com.cyberspectraa.cybernpc.compat;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class PehkuiRenderCompat {
    private static final String MOD_ID = "pehkui";

    private static boolean reflectionAttempted;
    private static Method getModelWidthScale;
    private static Method getModelHeightScale;

    private PehkuiRenderCompat() {
    }

    public static ModelScale getModelScale(
            Entity entity,
            float partialTick
    ) {
        if (entity == null
                || !ModList.get().isLoaded(MOD_ID)
                || !initializeReflection()) {
            return ModelScale.IDENTITY;
        }

        try {
            float width = ((Number) getModelWidthScale.invoke(
                    null,
                    entity,
                    partialTick
            )).floatValue();

            float height = ((Number) getModelHeightScale.invoke(
                    null,
                    entity,
                    partialTick
            )).floatValue();

            return new ModelScale(
                    sanitize(width),
                    sanitize(height)
            );
        } catch (ReflectiveOperationException
                 | RuntimeException exception) {
            return ModelScale.IDENTITY;
        }
    }

    private static boolean initializeReflection() {
        if (reflectionAttempted) {
            return getModelWidthScale != null
                    && getModelHeightScale != null;
        }

        reflectionAttempted = true;

        try {
            Class<?> scaleUtils = Class.forName(
                    "virtuoel.pehkui.util.ScaleUtils"
            );

            getModelWidthScale = scaleUtils.getMethod(
                    "getModelWidthScale",
                    Entity.class,
                    float.class
            );

            getModelHeightScale = scaleUtils.getMethod(
                    "getModelHeightScale",
                    Entity.class,
                    float.class
            );

            return true;
        } catch (ReflectiveOperationException
                 | LinkageError
                 | RuntimeException exception) {
            getModelWidthScale = null;
            getModelHeightScale = null;
            return false;
        }
    }

    private static float sanitize(float scale) {
        if (!Float.isFinite(scale)
                || Math.abs(scale) < 0.0001F) {
            return 1.0F;
        }

        return Math.abs(scale);
    }

    public record ModelScale(
            float width,
            float height
    ) {
        private static final ModelScale IDENTITY =
                new ModelScale(1.0F, 1.0F);

        public float inverseWidth() {
            return 1.0F / width;
        }

        public float inverseHeight() {
            return 1.0F / height;
        }
    }
}
