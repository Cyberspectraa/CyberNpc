package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.IntroControlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Stable 6.5-second arrival shot. The camera is positioned once at scene
 * start and remains locked to the actual arrival block, not the current
 * player rotation, render tick timing or camera orbit.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT)
public final class SummoningCinematicScreen extends Screen {
    private static final int DURATION_TICKS = 130;
    private static final int REVEAL_TICKS = 62;
    private static final int FADE_TICKS = 19;

    private final BlockPos arrival;
    private final float direction;
    @Nullable private Entity previousCamera;
    @Nullable private CameraType previousCameraType;
    @Nullable private ArmorStand camera;
    private int ticks;
    private boolean completionSent;
    private boolean closingFromServer;

    private SummoningCinematicScreen(BlockPos arrival, float direction) {
        super(Component.literal("Summoning"));
        this.arrival = arrival;
        this.direction = direction;
    }

    public static void open(BlockPos arrival, float direction) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen) return;
        client.setScreen(new SummoningCinematicScreen(arrival, direction));
    }

    public static void closeFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen scene) {
            scene.closingFromServer = true;
            client.setScreen(null);
        }
        // Do not close NpcDialogueScreen here; it belongs to a separate
        // server-authoritative conversation and may have opened already.
    }

    @Override
    protected void init() {
        super.init();
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        // Minecraft's third-person setting otherwise adds another camera
        // offset behind our already-positioned cinematic camera. In particular,
        // players using F5 appeared to drift away from the summoning point.
        if (previousCameraType == null) {
            previousCameraType = client.options.getCameraType();
        }
        client.options.setCameraType(CameraType.FIRST_PERSON);

        if (camera == null) {
            previousCamera = client.getCameraEntity();
            camera = new ArmorStand(EntityType.ARMOR_STAND, client.level);
            camera.setInvisible(true);
            positionCameraOnce();
        }
        client.setCameraEntity(camera);
    }

    /**
     * A fixed shot avoids per-frame position/rotation interpolation jitter.
     * The server teleports to the arrival block centre, so the camera uses
     * that exact centre rather than following client-side player movement.
     * Try nearby fixed viewpoints once if the preferred one hits a wall.
     */
    private void positionCameraOnce() {
        Minecraft client = Minecraft.getInstance();
        if (camera == null || client.level == null || client.player == null) return;

        double focusHeight = Math.max(0.32D,
                Math.min(1.35D, client.player.getBbHeight() * 0.55D));
        Vec3 focus = Vec3.atBottomCenterOf(arrival).add(0D, focusHeight, 0D);
        double[] angles = {140D, 100D, 180D, 60D, 220D};
        Vec3 bestPosition = null;
        double bestDistance = -1D;

        for (double angle : angles) {
            double radians = Math.toRadians(direction + angle);
            Vec3 desired = focus.add(
                    Math.sin(radians) * 4.8D,
                    1.9D,
                    Math.cos(radians) * 4.8D);
            Vec3 ray = desired.subtract(focus);
            BlockHitResult hit = client.level.clip(new ClipContext(
                    focus, desired, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, client.player));
            double allowed = ray.length();
            if (hit.getType() == HitResult.Type.BLOCK) {
                allowed = Math.max(0.65D,
                        Math.min(allowed, focus.distanceTo(hit.getLocation()) - 0.35D));
            }
            if (allowed > bestDistance) {
                bestDistance = allowed;
                bestPosition = focus.add(ray.normalize().scale(allowed));
            }
            if (hit.getType() != HitResult.Type.BLOCK) break;
        }

        Vec3 actual = bestPosition == null ? focus.add(0D, 1.9D, 4.8D) : bestPosition;
        double dx = focus.x - actual.x;
        double dy = focus.y - actual.y;
        double dz = focus.z - actual.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(
                Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        // Entity cameras render from their eyes, not their feet.
        double feetY = actual.y - camera.getEyeHeight();
        camera.setPos(actual.x, feetY, actual.z);
        camera.xo = actual.x;
        camera.yo = feetY;
        camera.zo = actual.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    /** Never render the player's hand inside the cinematic camera. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Minecraft.getInstance().screen instanceof SummoningCinematicScreen) {
            event.setCanceled(true);
        }
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
            graphics.fill(0, 0, width, height, (fade << 24));
        }
        // A subtle summoning flash coincides with the server-owned reveal;
        // this is a visual effect, not NPC speech or dialogue.
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
        camera = null;
    }

    @Override public boolean isPauseScreen() { return false; }
}
