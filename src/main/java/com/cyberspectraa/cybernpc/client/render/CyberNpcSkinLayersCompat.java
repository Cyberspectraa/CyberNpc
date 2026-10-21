package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.client.model.CyberNpcPlayerModel;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-only, optional Skin Layers 3D integration for the actual player-shaped
 * CyberNpc model. Use its public mesh API and injected ModelPart renderer instead
 * of copying its voxeliser or pretending that an NPC is a client player.
 *
 * All Skin Layers types are looked up reflectively: loading CyberNpc on a
 * dedicated server or without Skin Layers 3D never loads its client classes.
 */
public final class CyberNpcSkinLayersCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "skinlayers3d";
    private static final int MAX_CACHED_SKINS = 96;
    private static final String[] ENABLE_FLAGS = {
            "enableHat", "enableJacket", "enableLeftSleeve",
            "enableRightSleeve", "enableLeftPants", "enableRightPants"
    };

    private static final Map<SkinKey, Object[]> MESH_CACHE =
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<SkinKey, Object[]> eldest) {
                    return size() > MAX_CACHED_SKINS;
                }
            };

    private static boolean checked;
    private static boolean broken;
    private static Bridge bridge;

    private CyberNpcSkinLayersCompat() {
    }

    public static void prepare(CyberNpcEntity npc, CyberNpcPlayerModel model, ResourceLocation texture) {
        Bridge api = api();
        if (api == null || texture == null || npc.isInvisible()) {
            return;
        }

        try {
            Object config = api.configField.get(null);
            if (config == null) {
                return;
            }
            int maxDistance = Math.max(0, api.distanceField.getInt(config));
            if (npc.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
                    > (double) maxDistance * maxDistance) {
                // Leave the original flat vanilla layers enabled beyond the mod's LOD range.
                return;
            }

            SkinKey key = new SkinKey(texture, npc.isSlimModel());
            Object[] meshes;
            synchronized (MESH_CACHE) {
                meshes = MESH_CACHE.get(key);
            }
            if (meshes == null) {
                meshes = createMeshes(api, texture, npc.isSlimModel());
                if (meshes == null) {
                    return; // Texture not ready (or unsupported); keep vanilla rendering.
                }
                synchronized (MESH_CACHE) {
                    MESH_CACHE.put(key, meshes);
                }
            }

            for (int i = 0; i < meshes.length; i++) {
                if (api.enabledFields[i].getBoolean(config)) {
                    api.injectMethod.invoke(parts(model)[i], meshes[i],
                            i == 2 ? api.slimOrWideLeft(npc.isSlimModel())
                                    : i == 3 ? api.slimOrWideRight(npc.isSlimModel())
                                    : api.offsets[i]);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            disable(exception);
            clearModel(model);
        }
    }

    /**
     * The wide and slim model instances are shared between NPCs. Always remove
     * injected meshes after rendering so the next NPC cannot inherit a skin.
     */
    public static void clearModel(CyberNpcPlayerModel model) {
        Bridge api = bridge;
        if (api == null) {
            return;
        }
        try {
            for (var part : parts(model)) {
                api.injectMethod.invoke(part, null, null);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            disable(exception);
        }
    }

    /** Invoked on client resource reload so meshes never hold stale texture data. */
    public static void clearCache() {
        synchronized (MESH_CACHE) {
            MESH_CACHE.clear();
        }
    }

    private static net.minecraft.client.model.geom.ModelPart[] parts(CyberNpcPlayerModel model) {
        return new net.minecraft.client.model.geom.ModelPart[] {
                model.hat, model.jacket, model.leftSleeve,
                model.rightSleeve, model.leftPants, model.rightPants
        };
    }

    private static Object[] createMeshes(Bridge api, ResourceLocation texture, boolean slim)
            throws ReflectiveOperationException {
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture registered = mc.getTextureManager().getTexture(texture);

        // Most CyberNpc appearances are DynamicTextures assembled from base
        // skin/eye/hair/clothing layers, not textures from a resource pack.
        if (registered instanceof DynamicTexture dynamic) {
            NativeImage image = dynamic.getPixels();
            if (image != null && image.getWidth() == 64 && image.getHeight() == 64) {
                return createMeshes(api, image, slim);
            }
            return null;
        }

        Optional<Resource> resource = mc.getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            return null;
        }
        try (InputStream stream = resource.get().open();
             NativeImage image = NativeImage.read(stream)) {
            if (image.getWidth() != 64 || image.getHeight() != 64) {
                return null;
            }
            return createMeshes(api, image, slim);
        } catch (java.io.IOException exception) {
            // Missing/broken optional textures must not disrupt NPC rendering.
            return null;
        }
    }

    private static Object[] createMeshes(Bridge api, NativeImage image, boolean slim)
            throws ReflectiveOperationException {
        int armWidth = slim ? 3 : 4;
        // UV locations match the standard 64x64 second-skin layer layout.
        return new Object[] {
                api.mesh(image, 8, 8, 8, 32, 0, false, 0.6F),
                api.mesh(image, 8, 12, 4, 16, 32, true, 0.0F),
                api.mesh(image, armWidth, 12, 4, 48, 48, true, -2.0F),
                api.mesh(image, armWidth, 12, 4, 40, 32, true, -2.0F),
                api.mesh(image, 4, 12, 4, 0, 48, true, 0.0F),
                api.mesh(image, 4, 12, 4, 0, 32, true, 0.0F)
        };
    }

    private static Bridge api() {
        if (broken) {
            return null;
        }
        if (!checked) {
            checked = true;
            if (!ModList.get().isLoaded(MOD_ID)) {
                return null;
            }
            try {
                bridge = new Bridge();
                LOGGER.info("CyberNpc: enabled optional Skin Layers 3D support");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                disable(exception);
            }
        }
        return bridge;
    }

    private static void disable(Throwable error) {
        if (!broken) {
            LOGGER.warn("CyberNpc: Skin Layers 3D hook unavailable; falling back to 2D skins", error);
        }
        broken = true;
        clearCache();
    }

    private record SkinKey(ResourceLocation texture, boolean slim) {
    }

    private static final class Bridge {
        private final Object helper;
        private final Method meshMethod;
        private final Method injectMethod;
        private final Field configField;
        private final Field distanceField;
        private final Field[] enabledFields = new Field[ENABLE_FLAGS.length];
        private final Object[] offsets = new Object[ENABLE_FLAGS.length];
        private final Object leftArmSlim;
        private final Object rightArmSlim;

        private Bridge() throws ReflectiveOperationException {
            Class<?> api = Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI");
            Class<?> mesh = Class.forName("dev.tr7zw.skinlayers.api.Mesh");
            Class<?> helperType = Class.forName("dev.tr7zw.skinlayers.api.MeshHelper");
            Class<?> offsetsType = Class.forName("dev.tr7zw.skinlayers.api.OffsetProvider");
            Class<?> injected = Class.forName("dev.tr7zw.skinlayers.accessor.ModelPartInjector");

            helper = api.getMethod("getMeshHelper").invoke(null);
            meshMethod = helperType.getMethod("create3DMesh", NativeImage.class,
                    int.class, int.class, int.class, int.class, int.class,
                    boolean.class, float.class);
            injectMethod = injected.getMethod("setInjectedMesh", mesh, offsetsType);

            String[] offsetNames = {
                    "HEAD", "BODY", "LEFT_ARM", "RIGHT_ARM", "LEFT_LEG", "RIGHT_LEG"
            };
            for (int i = 0; i < offsetNames.length; i++) {
                offsets[i] = offsetsType.getField(offsetNames[i]).get(null);
            }
            leftArmSlim = offsetsType.getField("LEFT_ARM_SLIM").get(null);
            rightArmSlim = offsetsType.getField("RIGHT_ARM_SLIM").get(null);

            Class<?> base = Class.forName("dev.tr7zw.skinlayers.versionless.ModBase");
            configField = base.getField("config");
            Class<?> config = configField.getType();
            distanceField = config.getField("renderDistanceLOD");
            for (int i = 0; i < ENABLE_FLAGS.length; i++) {
                enabledFields[i] = config.getField(ENABLE_FLAGS[i]);
            }
        }

        private Object slimOrWideLeft(boolean slim) {
            return slim ? leftArmSlim : offsets[2];
        }

        private Object slimOrWideRight(boolean slim) {
            return slim ? rightArmSlim : offsets[3];
        }

        private Object mesh(NativeImage image, int width, int height, int depth,
                int u, int v, boolean topPivot, float rotationOffset)
                throws ReflectiveOperationException {
            return meshMethod.invoke(helper, image, width, height, depth, u, v, topPivot, rotationOffset);
        }
    }
}
