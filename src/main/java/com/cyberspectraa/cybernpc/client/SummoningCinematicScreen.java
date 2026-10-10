package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.IntroControlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Smooth 6.5-second camera orbit. RenderTickEvent.START positions the
 * temporary client-only camera BEFORE world rendering, so movement
 * interpolates every rendered frame instead of stepping at 20 TPS.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, value = Dist.CLIENT)
public final class SummoningCinematicScreen extends Screen {
    private static final int DURATION_TICKS = 130;
    private static final int REVEAL_TICKS = 62;
    private static final int FADE_TICKS = 19;

    private final BlockPos arrival;
    private final float direction;
    @Nullable private Entity previousCamera;
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
        previousCamera = client.getCameraEntity();
        camera = new ArmorStand(EntityType.ARMOR_STAND, client.level);
        camera.setInvisible(true);
        positionCamera(0D);
        client.setCameraEntity(camera);
    }

    /**
     * Called before the 3D world is drawn. The camera's old/current positions
     * are kept equal to avoid a second layer of Minecraft tick interpolation.
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen scene) {
            scene.positionCamera(scene.ticks + Math.min(1F, event.renderTickTime));
        }
    }

    private void positionCamera(double timeTicks) {
        Minecraft client = Minecraft.getInstance();
        if (camera == null || client.level == null || client.player == null) return;

        double u = Math.min(1D, Math.max(0D, timeTicks / DURATION_TICKS));
        // Cubic smoothstep gives zero acceleration jumps at the ends.
        double eased = u * u * (3D - 2D * u);
        double radians = Math.toRadians(direction + 140D - 115D * eased);
        double radius = 4.6D - 1.9D * eased;
        Vec3 focus = Vec3.atBottomCenterOf(arrival).add(0D, 1.05D, 0D);

        double x = focus.x + Math.sin(radians) * radius;
        double z = focus.z + Math.cos(radians) * radius;
        double y = focus.y + 2.15D - 0.85D * eased;
        Vec3 desired = new Vec3(x, y, z);

        // The scene needs no extra admin camera markers. Clip the camera
        // closer when an indoor summoning room has walls in the orbit path.
        BlockHitResult hit = client.level.clip(new ClipContext(
                focus, desired, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                client.player));
        Vec3 actual = desired;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 ray = desired.subtract(focus);
            double available = Math.max(0.4D, focus.distanceTo(hit.getLocation()) - 0.30D);
            actual = focus.add(ray.normalize().scale(
                    Math.min(available, ray.length())));
        }

        double dx = focus.x - actual.x;
        double dz = focus.z - actual.z;
        double dy = focus.y - actual.y;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        camera.setPos(actual.x, actual.y, actual.z);
        camera.xo = actual.x;
        camera.yo = actual.y;
        camera.zo = actual.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
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
        camera = null;
    }

    @Override public boolean isPauseScreen() { return false; }
}
