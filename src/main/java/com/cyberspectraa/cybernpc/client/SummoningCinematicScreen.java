package com.cyberspectraa.cybernpc.client;

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
import net.minecraft.world.phys.Vec3;

/**
 * Client-only smooth orbit / approach sequence. The server owns the reveal
 * and the Pope's arrival; this transient camera never exists on the server.
 */
public final class SummoningCinematicScreen extends Screen {
    private static final int DURATION_TICKS = 150;
    private final BlockPos arrival;
    private final float direction;
    private Entity oldCamera;
    private ArmorStand camera;
    private int ticks;
    private boolean finishing;

    private SummoningCinematicScreen(BlockPos arrival, float direction) {
        super(Component.literal("The Summoning"));
        this.arrival = arrival;
        this.direction = direction;
    }

    public static void open(BlockPos arrival, float direction) {
        Minecraft.getInstance().setScreen(new SummoningCinematicScreen(arrival, direction));
    }

    public static void closeFromServer() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof SummoningCinematicScreen screen) {
            screen.finishing = true;
            client.setScreen(null);
        } else if (client.screen instanceof NpcDialogueScreen) {
            client.setScreen(null);
        }
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        oldCamera = mc.getCameraEntity();
        camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        camera.setInvisible(true);
        updateCamera();
        mc.setCameraEntity(camera);
    }

    private void updateCamera() {
        if (camera == null) return;
        double t = Math.min(1D, ticks / (double) DURATION_TICKS);
        double sweep = Math.toRadians(direction + 205D - t * 125D);
        double radius = 6.4D - 2.4D * t;
        Vec3 center = Vec3.atBottomCenterOf(arrival).add(0, 1.15D, 0);
        double x = center.x + Math.sin(sweep) * radius;
        double z = center.z + Math.cos(sweep) * radius;
        double y = center.y + 2.8D - t * 1.6D;
        camera.setPos(x, y, z);
        double dx = center.x - x, dz = center.z - z, dy = center.y - y;
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float)-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)));
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
        camera.xo = x; camera.yo = y; camera.zo = z;
    }

    @Override public void tick() {
        if (finishing) return;
        ticks++;
        updateCamera();
        if (ticks >= DURATION_TICKS) {
            finishing = true;
            CyberNpcNetwork.CHANNEL.sendToServer(new IntroControlPacket(false));
            // Keep the world visible while waiting for the server-owned transition.
        }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int fade = ticks < 18 ? 255 - ticks * 14 :
                ticks > 127 ? Math.min(230, (ticks - 127) * 9) : 0;
        if (fade > 0) g.fill(0, 0, width, height, (fade << 24));
        g.fill(0, 0, width, 12, 0xCC080808);
        g.fill(0, height - 12, width, height, 0xCC080808);
        String caption = ticks < 65 ? "The summoning begins..." :
                ticks < 125 ? "A new adventurer has arrived." : "A new story awaits.";
        g.drawString(font, caption, (width - font.width(caption)) / 2,
                height - 32, 0xFFF8E5B6, false);
        g.drawString(font, "Press ESC to skip", width - 110, 17, 0xFFD8C8AC, false);
        super.render(g, mouseX, mouseY, delta);
    }

    @Override public void onClose() {
        if (!finishing) {
            finishing = true;
            CyberNpcNetwork.CHANNEL.sendToServer(new IntroControlPacket(true));
        }
        Minecraft.getInstance().setScreen(null);
    }

    @Override public void removed() {
        Minecraft mc = Minecraft.getInstance();
        if (camera != null && mc.getCameraEntity() == camera)
            mc.setCameraEntity(mc.player != null ? mc.player : oldCamera);
        camera = null;
    }

    @Override public boolean isPauseScreen() { return false; }
}
