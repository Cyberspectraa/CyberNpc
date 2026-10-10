package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.intro.CyberIntroService;
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
 * Two-part intro: a quiet pack title on black followed by the original
 * summoning effects with a small, deterministic cinematic camera arc.
 *
 * The focus never moves: it is the saved arrival location used by the server
 * teleport (X + 0.5, Y, Z + 0.5). The camera curve depends only on elapsed
 * time, not previous frames, mouse movement, race scale or player location.
 * Forge viewport angle locking remains in place to avoid renderer drift.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT)
public final class SummoningCinematicScreen extends Screen {
    private static final int TITLE_TICKS = CyberIntroService.TITLE_TICKS;
    private static final int SUMMON_TICKS = CyberIntroService.SUMMON_TICKS;
    private static final int DURATION_TICKS = CyberIntroService.SCENE_TICKS;
    private static final int REVEAL_TICKS = CyberIntroService.REVEAL_TICKS;
    private static final int FADE_TICKS = 19;

    private static final String PACK_TITLE = "CYBERSPECTRA";
    private static final String PACK_SUBTITLE = "SEASON II";
    private static final double SHOT_DISTANCE = 5.2D;
    private static final double SHOT_ELEVATION = 2.25D;
    private static final double ARC_DEGREES = 12.0D;
    private static final double PUSH_IN = 0.95D;
    private static final double LOWER_BY = 0.22D;
    private static final double[] VIEW_ANGLES = {155D, 205D, 110D, 250D, 70D, 290D};

    private final float direction;
    private final Vec3 exactSpawn;

    @Nullable private Entity previousCamera;
    @Nullable private CameraType previousCameraType;
    @Nullable private Boolean previousBobView;
    @Nullable private ArmorStand camera;
    @Nullable private Vec3 cameraEye;

    // Chosen once per client level. If the arc crosses walls, use the
    // already-proven fixed camera instead of clipping mid-cutscene.
    private boolean allowArc;
    private double baseAngle;
    @Nullable private Vec3 staticShot;

    private float shotYaw;
    private float shotPitch;
    private int ticks;
    private boolean completionSent;
    private boolean closingFromServer;

    private SummoningCinematicScreen(BlockPos arrival, float direction) {
        super(Component.literal("Summoning"));
        this.direction = direction;
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
    }

    @Override
    protected void init() {
        super.init();
        ensureCameraLocked(0D);
    }

    /**
     * RenderTick.START runs before the world view is drawn. Position the
     * camera from absolute scene time for frame-rate-independent motion.
     * Setting both old and current entity transforms avoids double lerp.
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen scene) {
            scene.ensureCameraLocked(scene.ticks
                    + Math.max(0F, Math.min(1F, event.renderTickTime)));
        }
    }

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

    private void ensureCameraLocked(double sceneTime) {
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

        if (camera == null || camera.level() != client.level) {
            camera = new ArmorStand(EntityType.ARMOR_STAND, client.level);
            camera.setInvisible(true);
            chooseCameraPath(client);
        }

        Vec3 eye = cameraPosition(sceneTime);
        cameraEye = eye;
        double feetY = eye.y - camera.getEyeHeight();
        camera.setPos(eye.x, feetY, eye.z);
        camera.xo = eye.x;
        camera.yo = feetY;
        camera.zo = eye.z;

        double dx = exactSpawn.x - eye.x;
        double dy = exactSpawn.y - eye.y;
        double dz = exactSpawn.z - eye.z;
        shotYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        shotPitch = (float) -Math.toDegrees(
                Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        camera.setYRot(shotYaw);
        camera.setXRot(shotPitch);
        camera.yRotO = shotYaw;
        camera.xRotO = shotPitch;

        if (client.getCameraEntity() != camera) {
            client.setCameraEntity(camera);
        }
    }

    /**
     * Preview beginning, middle and end of each candidate camera path.
     * No level raycasts occur in the animation loop itself.
     */
    private void chooseCameraPath(Minecraft client) {
        allowArc = false;
        staticShot = chooseStaticShot(client);
        for (double offset : VIEW_ANGLES) {
            double candidate = direction + offset;
            if (isClear(client, arcPosition(candidate, 0D))
                    && isClear(client, arcPosition(candidate, 0.5D))
                    && isClear(client, arcPosition(candidate, 1D))) {
                baseAngle = candidate;
                allowArc = true;
                return;
            }
        }
    }

    private Vec3 chooseStaticShot(Minecraft client) {
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
            if (hit.getType() == HitResult.Type.MISS) return desired;

            double clear = Math.max(0.6D, Math.min(ray.length(),
                    exactSpawn.distanceTo(hit.getLocation()) - 0.4D));
            if (clear > bestClearance) {
                bestClearance = clear;
                best = exactSpawn.add(ray.normalize().scale(clear));
            }
        }
        return best;
    }

    private boolean isClear(Minecraft client, Vec3 eye) {
        return client.level.clip(new ClipContext(
                exactSpawn, eye, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, client.player))
                .getType() == HitResult.Type.MISS;
    }

    private Vec3 arcPosition(double angle, double eased) {
        double radians = Math.toRadians(angle + ARC_DEGREES * (eased - 0.5D));
        double radius = SHOT_DISTANCE + PUSH_IN * (1D - eased);
        double height = SHOT_ELEVATION + LOWER_BY * (1D - eased);
        return exactSpawn.add(
                Math.sin(radians) * radius, height,
                Math.cos(radians) * radius);
    }

    private Vec3 cameraPosition(double sceneTime) {
        if (!allowArc) {
            return staticShot == null
                    ? exactSpawn.add(0D, SHOT_ELEVATION, SHOT_DISTANCE)
                    : staticShot;
        }

        double u = Math.max(0D, Math.min(1D,
                (sceneTime - TITLE_TICKS) / SUMMON_TICKS));
        // Cubic smoothstep: zero velocity at beginning and end; no sudden
        // first/last frame motion or accumulated coordinate drift.
        double eased = u * u * (3D - 2D * u);
        return arcPosition(baseAngle, eased);
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
        if (t < TITLE_TICKS) {
            renderTitle(graphics, t);
        } else {
            renderSummoningOverlay(graphics, t - TITLE_TICKS);
        }

        String hint = "ESC - Skip intro";
        graphics.drawString(font, hint, width - font.width(hint) - 12, 17,
                0xFFD8C8AC, false);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderTitle(GuiGraphics graphics, double titleTime) {
        // A deliberately clean pack title, not a replacement for the pack's
        // existing summoning art or animations.
        graphics.fill(0, 0, width, height, 0xFF101016);
        double opacity = Math.min(1D,
                Math.min(titleTime / 12D, (TITLE_TICKS - titleTime) / 13D));
        int alpha = (int) Math.round(Math.max(0D, opacity) * 255D);
        int titleColor = (alpha << 24) | 0x00F5E6CD;
        int subColor = (alpha << 24) | 0x00A999BB;

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F - 11.0F, 0.0F);
        float scale = Math.min(2.2F,
                Math.max(1F, (width - 44F) / Math.max(1F, font.width(PACK_TITLE))));
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, PACK_TITLE, 0, -9, titleColor);
        graphics.pose().popPose();

        graphics.drawCenteredString(font, PACK_SUBTITLE,
                width / 2, height / 2 + 24, subColor);
        int lineWidth = Math.min(106, width / 2 - 20);
        if (lineWidth > 0) {
            graphics.fill(width / 2 - lineWidth, height / 2 + 15,
                    width / 2 + lineWidth, height / 2 + 16,
                    (alpha << 24) | 0x007E6A93);
        }
    }

    private void renderSummoningOverlay(GuiGraphics graphics, double elapsed) {
        int fade = elapsed < FADE_TICKS
                ? (int) Math.round(255D * (1D - elapsed / FADE_TICKS))
                : elapsed > SUMMON_TICKS - FADE_TICKS
                ? (int) Math.round(200D
                    * (elapsed - (SUMMON_TICKS - FADE_TICKS)) / FADE_TICKS)
                : 0;
        if (fade > 0) {
            graphics.fill(0, 0, width, height, fade << 24);
        }

        double offset = Math.abs(elapsed - (REVEAL_TICKS - TITLE_TICKS));
        int flash = offset < 7D ? (int) (110D * (1D - offset / 7D)) : 0;
        if (flash > 0) {
            graphics.fill(0, 0, width, height, (flash << 24) | 0x00E8D8F6);
        }

        graphics.fill(0, 0, width, 10, 0xCE090811);
        graphics.fill(0, height - 10, width, height, 0xCE090811);
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
        staticShot = null;
        previousCamera = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
