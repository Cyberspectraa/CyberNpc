package com.cyberspectraa.cybernpc.client;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.client.render.SpeechBubbleRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        value = Dist.CLIENT
)
public final class PlayerSpeechBubbleClient {
    private static final int MIN_DURATION_TICKS = 70;
    private static final int MAX_DURATION_TICKS = 180;
    private static final int MAX_MESSAGE_CHARACTERS = 256;

    private static final Map<UUID, SpeechState> ACTIVE =
            new HashMap<>();

    private PlayerSpeechBubbleClient() {
    }

    @SubscribeEvent
    public static void onPlayerChat(
            ClientChatReceivedEvent.Player event
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        PlayerChatMessage chat = event.getPlayerChatMessage();

        // Respect the server-provided chat filtering mask. A fully filtered
        // message returns null and therefore never appears over the player.
        Component filtered = chat.filterMask()
                .applyWithFormatting(chat.signedContent());

        if (filtered == null) {
            return;
        }

        String text = filtered.getString().trim();

        if (text.isBlank()) {
            return;
        }

        if (text.length() > MAX_MESSAGE_CHARACTERS) {
            text = text.substring(0, MAX_MESSAGE_CHARACTERS);
        }

        int duration = Mth.clamp(
                MIN_DURATION_TICKS + text.length() * 2,
                MIN_DURATION_TICKS,
                MAX_DURATION_TICKS
        );

        ACTIVE.put(
                event.getSender(),
                new SpeechState(
                        Component.literal(text),
                        minecraft.level.getGameTime() + duration
                )
        );
    }

    @SubscribeEvent
    public static void onRenderPlayer(
            RenderPlayerEvent.Post event
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Player player = event.getEntity();
        SpeechState state = ACTIVE.get(player.getUUID());

        if (state == null) {
            return;
        }

        long now = minecraft.level.getGameTime();

        if (now >= state.expiresAt()) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        if (player.isInvisible()) {
            return;
        }

        SpeechBubbleRenderUtil.renderPlayerTextBubble(
                player,
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                minecraft.font,
                minecraft.getEntityRenderDispatcher(),
                state.message()
        );
    }

    @SubscribeEvent
    public static void onLogout(
            ClientPlayerNetworkEvent.LoggingOut event
    ) {
        ACTIVE.clear();
    }

    private record SpeechState(
            Component message,
            long expiresAt
    ) {
    }
}
