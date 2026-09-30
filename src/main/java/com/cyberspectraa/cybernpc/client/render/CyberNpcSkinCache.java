package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcAppearance;
import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CyberNpcSkinCache {
    private static final ResourceLocation APPEARANCE_PACK =
            new ResourceLocation(CyberNpc.MOD_ID, "appearance/lunarskins.zip");

    private static final ResourceLocation FALLBACK_STEVE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    private static final ResourceLocation FALLBACK_ALEX =
            new ResourceLocation("minecraft", "textures/entity/player/slim/alex.png");

    private static final ResourceLocation FALLBACK_ZOMBIE =
            new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static Map<String, byte[]> packedAssets;

    private CyberNpcSkinCache() {
    }

    public static ResourceLocation getNpcTexture(CyberNpcEntity entity) {
        return getTexture(
                false,
                entity.getAppearanceGender(),
                entity.getSkinToneIndex(),
                entity.getEyeStyleIndex(),
                entity.getHairStyleIndex()
        );
    }

    public static ResourceLocation getZombieTexture(ZombieCyberNpcEntity entity) {
        return getTexture(
                true,
                entity.getAppearanceGender(),
                entity.getSkinToneIndex(),
                entity.getEyeStyleIndex(),
                entity.getHairStyleIndex()
        );
    }

    private static ResourceLocation getTexture(
            boolean zombie,
            NpcAppearance.Gender gender,
            int skinTone,
            int eyeStyle,
            int hairStyle
    ) {
        int tone = NpcAppearance.sanitizeSkinTone(skinTone);
        int eyes = NpcAppearance.sanitizeEyeStyle(eyeStyle);
        int hair = NpcAppearance.sanitizeHairStyle(hairStyle);

        String key = (zombie ? "zombie_" : "living_")
                + gender.serializedName()
                + "_" + tone
                + "_" + eyes
                + "_" + hair;

        ResourceLocation existing = CACHE.get(key);
        if (existing != null) {
            return existing;
        }

        try {
            NativeImage composed = loadImage(
                    zombie
                            ? "zombie/" + gender.serializedName() + ".png"
                            : "base/" + gender.serializedName() + "/"
                            + NpcAppearance.skinToneKey(tone) + ".png"
            );

            try (NativeImage eyeLayer = loadImage(
                    "eyes/" + NpcAppearance.eyeStyleKey(eyes) + ".png"
            );
                 NativeImage hairLayer = loadImage(
                         "hair/brown/" + NpcAppearance.hairStyleKey(hair) + ".png"
                 )) {
                blend(composed, eyeLayer);
                blend(composed, hairLayer);
            }

            ResourceLocation generated = new ResourceLocation(
                    CyberNpc.MOD_ID,
                    "generated/appearance/" + key
            );

            DynamicTexture texture = new DynamicTexture(composed);
            texture.upload();
            Minecraft.getInstance().getTextureManager().register(generated, texture);
            CACHE.put(key, generated);
            return generated;
        } catch (IOException | RuntimeException exception) {
            return fallback(zombie, gender);
        }
    }

    private static void blend(NativeImage base, NativeImage overlay) {
        int width = Math.min(base.getWidth(), overlay.getWidth());
        int height = Math.min(base.getHeight(), overlay.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                base.blendPixel(x, y, overlay.getPixelRGBA(x, y));
            }
        }
    }

    private static NativeImage loadImage(String path) throws IOException {
        byte[] bytes = assets().get(path);
        if (bytes == null) {
            throw new IOException("Missing CyberNpc appearance asset: " + path);
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            return NativeImage.read(input);
        }
    }

    private static synchronized Map<String, byte[]> assets() throws IOException {
        if (packedAssets != null) {
            return packedAssets;
        }

        Resource resource = Minecraft.getInstance()
                .getResourceManager()
                .getResource(APPEARANCE_PACK)
                .orElseThrow(() -> new IOException(
                        "Missing CyberNpc appearance pack " + APPEARANCE_PACK
                ));

        Map<String, byte[]> loaded = new HashMap<>();

        try (InputStream raw = resource.open();
             ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    loaded.put(entry.getName(), zip.readAllBytes());
                }
                zip.closeEntry();
            }
        }

        packedAssets = loaded;
        return packedAssets;
    }

    private static ResourceLocation fallback(
            boolean zombie,
            NpcAppearance.Gender gender
    ) {
        if (zombie) {
            return FALLBACK_ZOMBIE;
        }

        return gender.slim() ? FALLBACK_ALEX : FALLBACK_STEVE;
    }
}
