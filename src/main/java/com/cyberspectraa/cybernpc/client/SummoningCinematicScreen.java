package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.IntroControlPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * The entire cinematic uses ONE static, world-space camera target:
 * the exact centre of the configured spawn block, at its saved Y.
 *
 * Minecraft can replace the active camera during dimension teleport or
 * recalculate yaw/pitch at world render time. Keep the camera entity attached,
 * lock its previous/current transforms, and override render-time angles.
 *
 * No moving/orbiting camera, per-frame raycasting, dynamic player tracking
 * or interpolation. Scene duration, reveal, and server-owned progression
 * remain identical to previous versions.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT)
public final class SummoningCinematicScreen extends Screen {
    private static final int DURATION_TICKS = 130;
    private static final int REVEAL_TICKS = 62;
    private static final int FADE_TICKS = 19;

    private static final double SHOT_DISTANCE = 5.2D;
    private static final double SHOT_ELEVATION = 2.25D;
    private static final double[] VIEW_ANGLES = {155D, 205D, 110D, 250D, 70D, 290D};

    private final BlockPos arrival;
    private final float direction;
    private final Vec3 exactSpawn;

    @Nullable private Entity previousCamera;
    @Nullable private CameraType previousCameraType;
    @Nullable private Boolean previousBobView;
    @Nullable private ArmorStand camera;
    @Nullable private Vec3 cameraEye;

    private float shotYaw;
    private float shotPitch;
    private int ticks;
    private boolean completionSent;
    private boolean closingFromServer;

    private SummoningCinematicScreen(BlockPos arrival, float direction) {
        super(Component.literal("Summoning"));
        this.arrival = arrival.immutable();
        this.direction = direction;
        // Matches the server teleport exactly: X + 0.5, Y, Z + 0.5.
        this.exactSpawn = Vec3.atBottomCenterOf(arrival);
    }

    public static void open(BlockPos arrival, float direction) {
        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof SummoningCinematicScreen)) {
            client.setScreen(new SummoningCinematicScreen(arrival, direction));
        }
    }

    public static void closeFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen scene) {
            scene.closingFromServer = true;
            client.setScreen(null);
        }
        // Intro dialogue belongs to the server and must not be closed here.
    }

    @Override
    protected void init() {
        super.init();
        ensureCameraLocked();
    }

    /**
     * Forge fires this before the world camera is set up on every frame.
     * Reattach after a client-side respawn/teleport or another mod's camera
     * changes; never shift the target to follow that camera/player.
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen scene) {
            scene.ensureCameraLocked();
        }
    }

    /**
     * Minecraft/other mods can alter the rendered orientation even when an
     * entity's yaw and pitch are static. Correct the VIEWPORT angles at the
     * final available Forge event, using the same fixed spawn target.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen scene
                && scene.camera != null
                && client.getCameraEntity() == scene.camera) {
            event.setYaw(scene.shotYaw);
            event.setPitch(scene.shotPitch);
            event.setRoll(0.0F);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Minecraft.getInstance().screen instanceof SummoningCinematicScreen) {
            event.setCanceled(true);
        }
    }

    private void ensureCameraLocked() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        if (previousCameraType == null) {
            previousCameraType = client.options.getCameraType();
            previousBobView = client.options.bobView().get();
            previousCamera = client.getCameraEntity();
        }
        if (client.options.getCameraType() != CameraType.FIRST_PERSON) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (client.options.bobView().get()) {
            client.options.bobView().set(false);
        }

        // A dimension transition discards the previous client level. Never
        // reuse a phantom entity tied to that old level.
        if (camera == null || camera.level() != client.level) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, client.level);
            camera.setInvisible(true);
            cameraEye = chooseShot(client);
            updateShotAngles();
        }

        // Reassert identical old/current positions: zero position blending
        // at all partial ticks, even with high FPS or unstable server TPS.
        Vec3 eye = cameraEye;
        if (eye == null) return;
        double feetY = eye.y - camera.getEyeHeight();
        camera.setPos(eye.x, feetY, eye.z);
        camera.xo = eye.x;
        camera.yo = feetY;
        camera.zo = eye.z;
        camera.setYRot(shotYaw);
        camera.setXRot(shotPitch);
        camera.yRotO = shotYaw;
        camera.xRotO = shotPitch;

        if (client.getCameraEntity() != camera) {
            client.setCameraEntity(camera);
        }
    }

    /**
     * Pick a visible viewpoint ONCE. The look-at position never changes.
     * The selection does not depend on player yaw, player movement, scale,
     * client render ticks, or particles.
     */
    private Vec3 chooseShot(Minecraft client) {
        Vec3 best = exactSpawn.add(0D, SHOT_ELEVATION, SHOT_DISTANCE);
        double bestClearance = -1D;

        for (double offset : VIEW_ANGLES) {
            double angle = Math.toRadians(direction + offset);
            Vec3 desired = exactSpawn.add(
                    Math.sin(angle) * SHOT_DISTANCE,
                    SHOT_ELEVATION,
                    Math.cos(angle) * SHOT_DISTANCE);
            Vec3 ray = desired.subtract(exactSpawn);
            BlockHitResult hit = client.level.clip(new ClipContext(
                    exactSpawn, desired, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, client.player));

            if (hit.getType() == HitResult.Type.MISS) {
                return desired;
            }

            double clear = Math.max(0.6D,
                    Math.min(ray.length(),
                            exactSpawn.distanceTo(hit.getLocation()) - 0.4D));
            if (clear > bestClearance) {
                bestClearance = clear;
                best = exactSpawn.add(ray.normalize().scale(clear));
            }
        }
        return best;
    }

    private void updateShotAngles() {
        if (cameraEye == null) return;
        // Aim at the exact saved block-centre position, not an NPC, live
        // player bounding box, particle cloud, or gradually changing focus.
        double dx = exactSpawn.x - cameraEye.x;
        double dy = exactSpawn.y - cameraEye.y;
        double dz = exactSpawn.z - cameraEye.z;
        shotYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        shotPitch = (float) -Math.toDegrees(
                Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    @Override
    public void tick() {
        if (closingFromServer) return;
        ticks++;
        if (ticks >= DURATION_TICKS && !completionSent) {
            completionSent = true;
            CyberNpcNetwork.CHANNEL.sendToServer(new IntroControlPacket(false));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        double t = Math.max(0D, Math.min(DURATION_TICKS, ticks + delta));
        int fade = t < FADE_TICKS
                ? (int) Math.round(255D * (1D - t / FADE_TICKS))
                : t > DURATION_TICKS - FADE_TICKS
                ? (int) Math.round(200D * (t - (DURATION_TICKS - FADE_TICKS)) / FADE_TICKS)
                : 0;
        if (fade > 0) {
            graphics.fill(0, 0, width, height, fade << 24);
        }

        double offset = Math.abs(t - REVEAL_TICKS);
        int flash = offset < 7D ? (int) (110D * (1D - offset / 7D)) : 0;
        if (flash > 0) {
            graphics.fill(0, 0, width, height, (flash << 24) | 0x00E8D8F6);
        }

        graphics.fill(0, 0, width, 10, 0xCE090811);
        graphics.fill(0, height - 10, width, height, 0xCE090811);
        String hint = "ESC - Skip intro";
        graphics.drawString(font, hint, width - font.width(hint) - 12, 17,
                0xFFD8C8AC, false);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (!closingFromServer) {
            closingFromServer = true;
            CyberNpcNetwork.CHANNEL.sendToServer(new IntroControlPacket(true));
        }
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void removed() {
        Minecraft client = Minecraft.getInstance();
        if (camera != null && client.getCameraEntity() == camera) {
            client.setCameraEntity(client.player != null
                    ? client.player : previousCamera);
        }
        if (previousCameraType != null) {
            client.options.setCameraType(previousCameraType);
            previousCameraType = null;
        }
        if (previousBobView != null) {
            client.options.bobView().set(previousBobView);
            previousBobView = null;
        }
        camera = null;
        cameraEye = null;
        previousCamera = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
