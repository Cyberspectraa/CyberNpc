package com.cyberspectraa.cybernpc.voice;

import com.cyberspectraa.cybernpc.network.CyberNpcNetwork;
import com.cyberspectraa.cybernpc.network.VoiceCaptionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Public optional CyberServer -> CyberNpc bridge.
 * Called only from the Minecraft server thread; no direct CyberServer
 * dependency, no raw audio and no persistent transcripts.
 */
public final class CyberNpcVoiceBubbleApi {
    private static final double DISPLAY_DISTANCE_SQR = 32.0D * 32.0D;

    public static void broadcast(ServerPlayer speaker, String caption, boolean complete) {
        broadcast(speaker, caption, complete, 32.0D);
    }

    public static void broadcast(ServerPlayer speaker, String caption,
                                 boolean complete, double voiceRange) {
        if (speaker == null || !speaker.isAlive() || speaker.isSpectator()
                || caption == null || caption.isBlank()) return;

        String clean = caption.replaceAll("[\\p{Cntrl}]", " ").trim();
        if (clean.length() > 180) clean = clean.substring(0, 180);
        if (clean.isBlank()) return;
        double permittedRadius = Math.min(32.0D, Math.max(0.0D, voiceRange));
        if (permittedRadius <= 0.0D) return;
        UUID speakerId = speaker.getUUID();
        VoiceCaptionPacket packet = new VoiceCaptionPacket(speakerId, clean, complete);

        // Proximity-only: never broadcast captions globally or across
        // dimensions. No voice-group/audio routing is handled here.
        for (ServerPlayer viewer : speaker.serverLevel().players()) {
            if (viewer.isSpectator()
                    || viewer.distanceToSqr(speaker) > permittedRadius * permittedRadius) continue;
            CyberNpcNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
        }
    }

    private CyberNpcVoiceBubbleApi() {}
}
