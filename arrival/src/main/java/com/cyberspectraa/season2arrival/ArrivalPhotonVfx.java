package com.cyberspectraa.season2arrival;

import com.lowdragmc.photon.client.fx.BlockEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.gameobject.emitter.beam.BeamEmitter;
import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.RendererSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Circle;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Cone;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Dot;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.IShape;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * The high-quality Arrival VFX. This class is only touched when Photon is loaded.
 *
 * The effect is intentionally built in code instead of relying on an external editor export:
 * the Arrival JAR owns the complete sequence and Photon supplies the renderer.
 */
public final class ArrivalPhotonVfx {
    private static final ResourceLocation LASER =
            new ResourceLocation("photon", "textures/particle/laser.png");
    private static final ResourceLocation RING =
            new ResourceLocation("photon", "textures/particle/ring.png");
    private static final ResourceLocation SMOKE =
            new ResourceLocation("photon", "textures/particle/smoke.png");

    private ArrivalPhotonVfx() {
    }

    public static void play(ArrivalVfxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        FX fx = buildEffect(packet);
        BlockPos origin = BlockPos.containing(packet.x(), packet.y(), packet.z());

        BlockEffect effect = new BlockEffect(fx, minecraft.level, origin);
        // BlockEffect anchors to the centre of the block. The arrival player's feet are at
        // the integer Y, so move the visual root down by half a block.
        effect.setOffset(new Vector3f(0.0F, -0.5F, 0.0F));
        effect.setAllowMulti(true);
        effect.setCheckState(false);
        effect.start();
    }

    private static FX buildEffect(ArrivalVfxPacket packet) {
        FX fx = new FX();

        // Floor ritual: two crisp, full-bright rings using Photon's own ring texture.
        fx.getMainFX().objects().add(createFloorRing(
                "ritual_outer", 0, 48, 2.85F, 0xC8FFD36A, 0.0F));
        fx.getMainFX().objects().add(createFloorRing(
                "ritual_inner", 7, 42, 1.95F, 0xD8FFF1B5, 45.0F));

        // Pre-summon energy being pulled upward around the arrival point.
        fx.getMainFX().objects().add(createRisingWisps());

        // A thin precursor beam announces the strike before the main column opens.
        float beamHeight = Math.max(32.0F, packet.maxBuildY() - (float) packet.y());
        fx.getMainFX().objects().add(createBeam(
                "precursor_beam", beamHeight, 8, 27, 0.16F, 0xB8FFF3C4));

        // Main world-height column: soft gold shell with a bright pale core.
        fx.getMainFX().objects().add(createBeam(
                "summon_beam_outer", beamHeight, 16, 61, 1.20F, 0x66FFD15A));
        fx.getMainFX().objects().add(createBeam(
                "summon_beam_core", beamHeight, 17, 60, 0.43F, 0xE8FFF5CE));

        // The arrival impact expands across the church floor, then sprays energy outward.
        fx.getMainFX().objects().add(createShockwave());
        fx.getMainFX().objects().add(createArrivalBurst());

        return fx;
    }

    private static ParticleEmitter createFloorRing(String name, int burstTime, int lifetime,
                                                   float size, int color, float rotation) {
        ParticleEmitter emitter = baseParticles(name, RING, color);
        emitter.config.setDuration(64);
        emitter.config.setLooping(false);
        emitter.config.setStartLifetime(NumberFunction.constant(lifetime));
        emitter.config.setStartSpeed(NumberFunction.constant(0.0F));
        emitter.config.setStartSize(new NumberFunction3(size, size, size));
        emitter.config.setStartRotation(new NumberFunction3(0.0F, 0.0F, rotation));
        emitter.config.shape.setShape(new Dot());
        emitter.config.shape.setPosition(new NumberFunction3(0.0F, 0.035F, 0.0F));
        emitter.config.renderer.setRenderMode(RendererSetting.Particle.Mode.Horizontal);
        addBurst(emitter, burstTime, 1, 1, 1);
        return emitter;
    }

    private static ParticleEmitter createRisingWisps() {
        ParticleEmitter emitter = baseParticles("rising_energy", SMOKE, 0xAAFFE09A);
        emitter.config.setDuration(55);
        emitter.config.setLooping(false);
        emitter.config.setStartLifetime(NumberFunction.constant(34));
        emitter.config.setStartSpeed(NumberFunction.constant(2.15F));
        emitter.config.setStartSize(new NumberFunction3(0.14F, 0.28F, 0.14F));

        Cone cone = new Cone();
        cone.setRadius(1.35F);
        cone.setRadiusThickness(0.72F);
        cone.setAngle(10.0F);
        emitter.config.shape.setShape(cone);
        emitter.config.shape.setPosition(new NumberFunction3(0.0F, 0.12F, 0.0F));

        // Four motes every three ticks gives motion without turning the church into particle fog.
        addBurst(emitter, 2, 4, 12, 3);
        return emitter;
    }

    private static ParticleEmitter createShockwave() {
        ParticleEmitter emitter = baseParticles("floor_shockwave", RING, 0xD8FFD56A);
        emitter.config.setDuration(58);
        emitter.config.setLooping(false);
        emitter.config.setStartLifetime(NumberFunction.constant(18));
        emitter.config.setStartSpeed(NumberFunction.constant(5.2F));
        emitter.config.setStartSize(new NumberFunction3(0.11F, 0.11F, 0.11F));

        Circle circle = new Circle();
        circle.setRadius(0.42F);
        circle.setRadiusThickness(0.0F);
        emitter.config.shape.setShape(circle);
        emitter.config.shape.setPosition(new NumberFunction3(0.0F, 0.10F, 0.0F));

        addBurst(emitter, 27, 72, 1, 1);
        return emitter;
    }

    private static ParticleEmitter createArrivalBurst() {
        ParticleEmitter emitter = baseParticles("arrival_sparks", RING, 0xEFFFF4C8);
        emitter.config.setDuration(62);
        emitter.config.setLooping(false);
        emitter.config.setStartLifetime(NumberFunction.constant(27));
        emitter.config.setStartSpeed(NumberFunction.constant(3.0F));
        emitter.config.setStartSize(new NumberFunction3(0.10F, 0.10F, 0.10F));

        Sphere sphere = new Sphere();
        sphere.setRadius(0.38F);
        sphere.setRadiusThickness(1.0F);
        emitter.config.shape.setShape(sphere);
        emitter.config.shape.setPosition(new NumberFunction3(0.0F, 1.0F, 0.0F));

        addBurst(emitter, 28, 54, 1, 1);
        return emitter;
    }

    private static BeamEmitter createBeam(String name, float height, int startDelay,
                                          int duration, float width, int color) {
        BeamEmitter emitter = new BeamEmitter();
        emitter.setName(name);
        emitter.getConfig().setLooping(false);
        emitter.getConfig().setDuration(duration);
        emitter.getConfig().setStartDelay(startDelay);
        emitter.getConfig().getEnd().set(0.0F, height, 0.0F);
        emitter.getConfig().setWidth(NumberFunction.constant(width));
        emitter.getConfig().setColor(NumberFunction.color(color));
        emitter.getConfig().getRenderer().setBloomEffect(false);
        emitter.getConfig().getRenderer().getCull().setEnable(false);
        emitter.getConfig().getLights().setEnable(true);

        configureMaterial(emitter.getConfig().getMaterial(), LASER, true);
        return emitter;
    }

    private static ParticleEmitter baseParticles(String name, ResourceLocation texture, int color) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName(name);
        emitter.config.setLooping(false);
        emitter.config.setMaxParticles(256);
        emitter.config.setStartColor(NumberFunction.color(color));
        emitter.config.emission.setEmissionRate(NumberFunction.constant(0.0F));
        emitter.config.renderer.setBloomEffect(false);
        emitter.config.renderer.getCull().setEnable(false);
        emitter.config.lights.setEnable(true);
        configureMaterial(emitter.config.material, texture, false);
        return emitter;
    }

    private static void configureMaterial(
            com.lowdragmc.photon.client.gameobject.emitter.data.MaterialSetting material,
            ResourceLocation texture,
            boolean drawThroughBlocks
    ) {
        material.setMaterial(new TextureMaterial(texture));
        material.setCull(false);
        material.setDepthMask(false);
        material.setDepthTest(!drawThroughBlocks);

        // Additive blending makes pale gold layers build into a convincing magical glow
        // without using Photon's bloom post-process.
        material.getBlendMode().setEnableBlend(true);
        material.getBlendMode().setSrcColorFactor(SourceFactor.SRC_ALPHA);
        material.getBlendMode().setDstColorFactor(DestFactor.ONE);
        material.getBlendMode().setSrcAlphaFactor(SourceFactor.ONE);
        material.getBlendMode().setDstAlphaFactor(DestFactor.ONE);
    }

    private static void addBurst(ParticleEmitter emitter, int time, int count, int cycles, int interval) {
        EmissionSetting.Burst burst = new EmissionSetting.Burst();
        burst.time = time;
        burst.setCount(NumberFunction.constant(count));
        burst.cycles = cycles;
        burst.interval = interval;
        burst.probability = 1.0F;
        emitter.config.emission.getBursts().add(burst);
    }
}
