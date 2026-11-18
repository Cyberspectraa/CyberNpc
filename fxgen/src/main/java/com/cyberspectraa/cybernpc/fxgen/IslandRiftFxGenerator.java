package com.cyberspectraa.cybernpc.fxgen;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.lowdragmc.photon.integration.PhotonLDLibPlugin;
import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Dot;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Build-only Photon serializer. A short, subtle 2-block-high loop segment
 * repeats at the Rift while players are near it. No large explosion or rings.
 * Based on the proven CyberClasses Photon generator integration.
 */
public final class IslandRiftFxGenerator {
    private static final ResourceLocation SPARK =
            new ResourceLocation("photon", "textures/particle/ring.png");
    private static final ResourceLocation WISP =
            new ResourceLocation("photon", "textures/particle/smoke.png");

    private IslandRiftFxGenerator() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Output .fx path required");
        File output = new File(args[0]);
        if (output.getParentFile() != null && !output.getParentFile().exists()
                && !output.getParentFile().mkdirs()) {
            throw new IllegalStateException("Unable to create FX output directory");
        }

        SharedConstants.tryDetectVersion();
        var initialized = Bootstrap.class.getDeclaredField("isBootstrapped");
        initialized.setAccessible(true);
        initialized.setBoolean(null, true);
        TypedPayloadRegistries.init();
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new,
                PhotonLDLibPlugin.NUMBER_FUNCTION_ACCESSOR, 1000);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new,
                PhotonLDLibPlugin.NUMBER_FUNCTION3_ACCESSOR, 1000);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new,
                PhotonLDLibPlugin.SHAPE_ACCESSOR, 1000);
        TypedPayloadRegistries.postInit();

        FX fx = new FX();
        fx.getMainFX().objects().add(edgeSparks());
        fx.getMainFX().objects().add(voidMotes());
        fx.getMainFX().objects().add(realityWisps());

        CompoundTag wrapper = new CompoundTag();
        wrapper.put("fx", fx.serializeNBT());
        wrapper.putInt("_version", 1);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            NbtIo.writeCompressed(wrapper, stream);
        }
        System.out.println("Generated CyberNpc Photon Rift FX: " + output.length() + " bytes");
    }

    private static ParticleEmitter edgeSparks() {
        ParticleEmitter emitter = base("rift_edge_sparks", SPARK, 0xC46B9BFF);
        emitter.config.setStartLifetime(NumberFunction.constant(18));
        emitter.config.setStartSpeed(NumberFunction.constant(0.08F));
        emitter.config.setStartSize(new NumberFunction3(0.085F, 0.085F, 0.085F));
        Sphere shape = new Sphere();
        shape.setRadius(0.27F);
        shape.setRadiusThickness(0.9F);
        emitter.config.shape.setShape(shape);
        emitter.config.shape.setPosition(new NumberFunction3(0F, 1.05F, 0F));
        burst(emitter, 0, 5, 8, 12);
        return emitter;
    }

    private static ParticleEmitter voidMotes() {
        ParticleEmitter emitter = base("rift_void_motes", SPARK, 0xB8C26EFF);
        emitter.config.setStartLifetime(NumberFunction.constant(24));
        emitter.config.setStartSpeed(NumberFunction.constant(0.055F));
        emitter.config.setStartSize(new NumberFunction3(0.055F, 0.055F, 0.055F));
        Sphere shape = new Sphere();
        shape.setRadius(0.33F);
        shape.setRadiusThickness(0.75F);
        emitter.config.shape.setShape(shape);
        emitter.config.shape.setPosition(new NumberFunction3(0F, 0.90F, 0F));
        burst(emitter, 3, 3, 8, 12);
        return emitter;
    }

    private static ParticleEmitter realityWisps() {
        ParticleEmitter emitter = base("rift_distortion_fragments", WISP, 0x567F65DB);
        emitter.config.setStartLifetime(NumberFunction.constant(20));
        emitter.config.setStartSpeed(NumberFunction.constant(0.025F));
        emitter.config.setStartSize(new NumberFunction3(0.12F, 0.21F, 0.12F));
        emitter.config.shape.setShape(new Dot());
        emitter.config.shape.setPosition(new NumberFunction3(0F, 0.95F, 0F));
        burst(emitter, 9, 2, 7, 13);
        return emitter;
    }

    private static ParticleEmitter base(String name, ResourceLocation texture, int color) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName(name);
        emitter.config.setDuration(108);
        emitter.config.setLooping(false);
        emitter.config.setMaxParticles(96);
        emitter.config.setStartColor(NumberFunction.color(color));
        emitter.config.emission.setEmissionRate(NumberFunction.constant(0F));
        emitter.config.renderer.setBloomEffect(false);
        emitter.config.renderer.getCull().setEnable(false);
        emitter.config.lights.setEnable(true);
        emitter.config.material.setMaterial(new TextureMaterial(texture));
        emitter.config.material.setCull(false);
        emitter.config.material.setDepthMask(false);
        emitter.config.material.setDepthTest(true);
        emitter.config.material.getBlendMode().setEnableBlend(true);
        emitter.config.material.getBlendMode().setSrcColorFactor(SourceFactor.SRC_ALPHA);
        emitter.config.material.getBlendMode().setDstColorFactor(DestFactor.ONE);
        emitter.config.material.getBlendMode().setSrcAlphaFactor(SourceFactor.ONE);
        emitter.config.material.getBlendMode().setDstAlphaFactor(DestFactor.ONE);
        return emitter;
    }

    private static void burst(ParticleEmitter emitter, int at,
            int count, int cycles, int interval) {
        EmissionSetting.Burst burst = new EmissionSetting.Burst();
        burst.time = at;
        burst.setCount(NumberFunction.constant(count));
        burst.cycles = cycles;
        burst.interval = interval;
        burst.probability = 1F;
        emitter.config.emission.getBursts().add(burst);
    }
}
