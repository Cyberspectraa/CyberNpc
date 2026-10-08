package com.cyberspectraa.cybernpc.client.render;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcAppearance;
import com.cyberspectraa.cybernpc.entity.ZombieCyberNpcEntity;
import com.cyberspectraa.cybernpc.service.NpcServiceRole;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CyberNpcSkinCache {
    private static final ResourceLocation APPEARANCE_PACK =
            new ResourceLocation(CyberNpc.MOD_ID, "appearance/lunarskins.b64");

    private static final ResourceLocation MASON =
            new ResourceLocation(
                    CyberNpc.MOD_ID,
                    "textures/entity/mason.png"
            );

    private static final ResourceLocation COURIER_POSTMAN =
            new ResourceLocation(
                    CyberNpc.MOD_ID,
                    "textures/entity/postman.png"
            );

    // The uploaded 64x64 skin is an outfit overlay, not a replacement face.
    private static final ResourceLocation SHOPKEEPER_OUTFIT =
            new ResourceLocation(
                    CyberNpc.MOD_ID,
                    "textures/entity/shopkeeper_outfit.png"
            );

    private static final ResourceLocation POPE_OUTFIT_B64 =
            new ResourceLocation(
                    CyberNpc.MOD_ID,
                    "appearance/pope.b64"
            );

    private static final ResourceLocation FALLBACK_STEVE =
            new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    private static final ResourceLocation FALLBACK_ALEX =
            new ResourceLocation("minecraft", "textures/entity/player/slim/alex.png");

    private static final ResourceLocation FALLBACK_ZOMBIE =
            new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static Map<String, byte[]> packedAssets;
    private static Boolean justExpressionsPresent;
    private static byte[] popeOutfitPng;

    private enum ZombieExpression {
        NORMAL,
        BLINK,
        FOCUSED
    }

    private CyberNpcSkinCache() {
    }

    public static ResourceLocation getNpcTexture(CyberNpcEntity entity) {
        if ("mason".equalsIgnoreCase(entity.getStoryNpcId())
                || (entity.getCustomName() != null
                && "Mason".equalsIgnoreCase(
                    entity.getCustomName().getString()
                ))) {
            return MASON;
        }

        if ("Courier".equalsIgnoreCase(entity.getRole())) {
            return COURIER_POSTMAN;
        }

        if (NpcServiceRole.fromRole(entity.getRole()) == NpcServiceRole.SHOPKEEPER) {
            return getRoleOutfitTexture(entity, true);
        }

        if ("Pope".equalsIgnoreCase(entity.getRole())
                || "Priest".equalsIgnoreCase(entity.getRole())
                || "Pontiff".equalsIgnoreCase(entity.getRole())) {
            return getRoleOutfitTexture(entity, false);
        }

        return getTexture(
                false,
                entity.getAppearanceGender(),
                entity.getSkinToneIndex(),
                entity.getEyeStyleIndex(),
                entity.getHairStyleIndex(),
                ZombieExpression.NORMAL
        );
    }

    private static ResourceLocation getRoleOutfitTexture(
            CyberNpcEntity entity,
            boolean shopkeeper
    ) {
        NpcAppearance.Gender gender =
                entity.getAppearanceGender();
        int tone = NpcAppearance.sanitizeSkinTone(
                entity.getSkinToneIndex()
        );
        int eyes = NpcAppearance.sanitizeEyeStyle(
                entity.getEyeStyleIndex()
        );
        int hair = NpcAppearance.sanitizeHairStyle(
                entity.getHairStyleIndex()
        );

        String key = (shopkeeper ? "shopkeeper_" : "pope_")
                + gender.serializedName()
                + "_" + tone
                + "_" + eyes
                + "_" + hair;

        ResourceLocation existing = CACHE.get(key);
        if (existing != null) {
            return existing;
        }

        try {
            // Start from the exact same generated living appearance used by
            // ordinary CyberNpc entities: skin tone, eyes and hair remain
            // unique to this Pope.
            NativeImage composed = loadImage(
                    "base/" + gender.serializedName() + "/"
                            + NpcAppearance.skinToneKey(tone)
                            + ".png"
            );

            try (NativeImage eyeLayer = loadImage(
                    "eyes/" + NpcAppearance.eyeStyleKey(eyes)
                            + ".png"
            );
                 NativeImage hairLayer = loadImage(
                         "hair/brown/"
                                 + NpcAppearance.hairStyleKey(hair)
                                 + ".png"
                 );
                 NativeImage outfitLayer = shopkeeper
                         ? loadResourceImage(SHOPKEEPER_OUTFIT)
                         : loadPopeOutfit()) {
                blend(composed, eyeLayer);
                blend(composed, hairLayer);

                // Role clothing overlays the existing randomized living NPC
                // skin, eyes and hair. Transparent regions keep the person
                // underneath, just as with the existing Pope outfit.
                blend(composed, outfitLayer);
            }

            clearUnusedTopLeftCorner(composed);

            ResourceLocation generated = new ResourceLocation(
                    CyberNpc.MOD_ID,
                    "generated/appearance/" + key
            );

            DynamicTexture texture = new DynamicTexture(composed);
            texture.upload();
            Minecraft.getInstance()
                    .getTextureManager()
                    .register(generated, texture);

            CACHE.put(key, generated);
            return generated;
        } catch (IOException | RuntimeException exception) {
            return getTexture(
                    false,
                    gender,
                    tone,
                    eyes,
                    hair,
                    ZombieExpression.NORMAL
            );
        }
    }

    private static NativeImage loadPopeOutfit()
            throws IOException {
        if (popeOutfitPng == null) {
            Resource resource = Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(POPE_OUTFIT_B64)
                    .orElseThrow(() -> new IOException(
                            "Missing CyberNpc Pope outfit "
                                    + POPE_OUTFIT_B64
                    ));

            byte[] encoded;
            try (InputStream input = resource.open()) {
                encoded = input.readAllBytes();
            }

            popeOutfitPng =
                    Base64.getMimeDecoder().decode(encoded);
        }

        try (ByteArrayInputStream input =
                     new ByteArrayInputStream(popeOutfitPng)) {
            return NativeImage.read(input);
        }
    }

    public static ResourceLocation getZombieTexture(ZombieCyberNpcEntity entity) {
        return getTexture(
                true,
                entity.getAppearanceGender(),
                entity.getSkinToneIndex(),
                entity.getEyeStyleIndex(),
                entity.getHairStyleIndex(),
                getZombieExpression(entity)
        );
    }

    private static ResourceLocation getTexture(
            boolean zombie,
            NpcAppearance.Gender gender,
            int skinTone,
            int eyeStyle,
            int hairStyle,
            ZombieExpression expression
    ) {
        int tone = NpcAppearance.sanitizeSkinTone(skinTone);
        int eyes = NpcAppearance.sanitizeEyeStyle(eyeStyle);
        int hair = NpcAppearance.sanitizeHairStyle(hairStyle);

        String key = (zombie ? "zombie_" : "living_")
                + gender.serializedName()
                + "_" + tone
                + "_" + eyes
                + "_" + hair
                + (zombie ? "_" + expression.name().toLowerCase() : "");

        ResourceLocation existing = CACHE.get(key);
        if (existing != null) {
            return existing;
        }

        try {
            NativeImage composed;
            if (zombie && gender == NpcAppearance.Gender.FEMALE) {
                composed = loadResourceImage(
                        new ResourceLocation(
                                CyberNpc.MOD_ID,
                                "textures/entity/appearance/zombie/female.png"
                        )
                );
            } else {
                composed = loadImage(
                        zombie
                                ? "zombie/" + gender.serializedName() + ".png"
                                : "base/" + gender.serializedName() + "/"
                                + NpcAppearance.skinToneKey(tone) + ".png"
                );
            }

            try (NativeImage eyeLayer = loadImage(
                    "eyes/" + NpcAppearance.eyeStyleKey(eyes) + ".png"
            );
                 NativeImage hairLayer = loadImage(
                         "hair/brown/" + NpcAppearance.hairStyleKey(hair) + ".png"
                 )) {
                blend(composed, eyeLayer);
                blend(composed, hairLayer);
            }

            if (zombie
                    && expression != ZombieExpression.NORMAL) {
                applyZombieExpression(
                        composed,
                        eyes,
                        expression
                );
            }

            // Starlight/Lunar exports may contain editor marker pixels in the
            // unused 8x8 corner at the top-left of a 64x64 skin. Minecraft
            // should never need that region for the player model, so sanitize
            // it before registering the generated texture. This also protects
            // future user-supplied appearance packs from the same artifact.
            clearUnusedTopLeftCorner(composed);

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

    private static ZombieExpression getZombieExpression(
            ZombieCyberNpcEntity entity
    ) {
        if (!hasJustExpressionsResources()) {
            return ZombieExpression.NORMAL;
        }

        int offset = Math.floorMod(entity.getId() * 31, 97);
        int phase = Math.floorMod(
                entity.tickCount + offset,
                97
        );

        if (phase < 4) {
            return ZombieExpression.BLINK;
        }

        if (entity.hurtTime > 0
                || entity.getTarget() != null) {
            return ZombieExpression.FOCUSED;
        }

        return ZombieExpression.NORMAL;
    }

    private static boolean hasJustExpressionsResources() {
        if (justExpressionsPresent != null) {
            return justExpressionsPresent;
        }

        var manager = Minecraft.getInstance()
                .getResourceManager();

        justExpressionsPresent =
                manager.getResource(
                        new ResourceLocation(
                                "minecraft",
                                "emf/cem/player_face.jpm"
                        )
                ).isPresent()
                        || manager.getResource(
                        new ResourceLocation(
                                "minecraft",
                                "optifine/cem/player_face.jpm"
                        )
                ).isPresent();

        return justExpressionsPresent;
    }

    private static void applyZombieExpression(
            NativeImage image,
            int eyeStyle,
            ZombieExpression expression
    ) {
        // The current Lunar eye layers place their two 2x2 eyes on the front
        // face at x 9-10 and x 13-14. Low/Middle/High only change the Y row.
        int eyeTop = switch (
                NpcAppearance.eyeStyleKey(eyeStyle)
        ) {
            case "low" -> 13;
            case "middle" -> 12;
            default -> 11;
        };

        if (eyeTop < 0 || eyeTop + 1 >= image.getHeight()) {
            return;
        }

        int leftSkin = image.getPixelRGBA(8, eyeTop);
        int rightSkin = image.getPixelRGBA(15, eyeTop);
        int leftLine = image.getPixelRGBA(10, eyeTop);
        int rightLine = image.getPixelRGBA(13, eyeTop);

        if (expression == ZombieExpression.FOCUSED) {
            // Half-close the top of the eye for a more focused/hostile look.
            image.setPixelRGBA(9, eyeTop, leftSkin);
            image.setPixelRGBA(10, eyeTop, leftSkin);
            image.setPixelRGBA(13, eyeTop, rightSkin);
            image.setPixelRGBA(14, eyeTop, rightSkin);
            return;
        }

        if (expression == ZombieExpression.BLINK) {
            for (int y = eyeTop; y <= eyeTop + 1; y++) {
                image.setPixelRGBA(9, y, leftSkin);
                image.setPixelRGBA(10, y, leftSkin);
                image.setPixelRGBA(13, y, rightSkin);
                image.setPixelRGBA(14, y, rightSkin);
            }

            // Reuse the eye's own dark pixel so the closed-eye line matches the
            // supplied skin rather than hardcoding a colour.
            image.setPixelRGBA(9, eyeTop + 1, leftLine);
            image.setPixelRGBA(10, eyeTop + 1, leftLine);
            image.setPixelRGBA(13, eyeTop + 1, rightLine);
            image.setPixelRGBA(14, eyeTop + 1, rightLine);
        }
    }

    private static void clearUnusedTopLeftCorner(NativeImage image) {
        int width = Math.min(8, image.getWidth());
        int height = Math.min(8, image.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixelRGBA(x, y, 0x00000000);
            }
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

    private static NativeImage loadResourceImage(
            ResourceLocation location
    ) throws IOException {
        Resource resource = Minecraft.getInstance()
                .getResourceManager()
                .getResource(location)
                .orElseThrow(() -> new IOException(
                        "Missing CyberNpc appearance asset: " + location
                ));

        try (InputStream input = resource.open()) {
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

        byte[] encoded;
        try (InputStream raw = resource.open()) {
            encoded = raw.readAllBytes();
        }

        byte[] archive = Base64.getMimeDecoder().decode(encoded);

        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(archive)
        )) {
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
