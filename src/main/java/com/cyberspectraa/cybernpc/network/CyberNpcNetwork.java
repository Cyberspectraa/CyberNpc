package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.network.DialogueOpenPacket;
import com.cyberspectraa.cybernpc.network.DialogueChoicePacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CyberNpcNetwork {
    private static final String PROTOCOL = "9";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.ChannelBuilder.named(
                            new ResourceLocation(
                                    CyberNpc.MOD_ID,
                                    "main"
                            )
                    )
                    .networkProtocolVersion(() -> PROTOCOL)
                    .clientAcceptedVersions(PROTOCOL::equals)
                    .serverAcceptedVersions(PROTOCOL::equals)
                    .simpleChannel();

    private static int nextId;

    public static void register() {
        CHANNEL.messageBuilder(
                DialogueOpenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueOpenPacket::encode)
                .decoder(DialogueOpenPacket::decode)
                .consumerMainThread(DialogueOpenPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                DialogueChoicePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueChoicePacket::encode)
                .decoder(DialogueChoicePacket::decode)
                .consumerMainThread(DialogueChoicePacket::handle)
                .add();

        CHANNEL.messageBuilder(
                DialogueClosePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueClosePacket::encode)
                .decoder(DialogueClosePacket::decode)
                .consumerMainThread(DialogueClosePacket::handle)
                .add();

        CHANNEL.messageBuilder(
                IntroScenePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(IntroScenePacket::encode)
                .decoder(IntroScenePacket::decode)
                .consumerMainThread(IntroScenePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                IntroQueuePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(IntroQueuePacket::encode)
                .decoder(IntroQueuePacket::decode)
                .consumerMainThread(IntroQueuePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                IntroControlPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(IntroControlPacket::encode)
                .decoder(IntroControlPacket::decode)
                .consumerMainThread(IntroControlPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        LetterSubmitPacket.class,
                        nextId++
                )
                .encoder(LetterSubmitPacket::encode)
                .decoder(LetterSubmitPacket::decode)
                .consumerMainThread(LetterSubmitPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                WorkplaceGroupTogglePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WorkplaceGroupTogglePacket::encode)
                .decoder(WorkplaceGroupTogglePacket::decode)
                .consumerMainThread(WorkplaceGroupTogglePacket::handle)
                .add();

        CHANNEL.messageBuilder(
                WorkplaceRemovePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WorkplaceRemovePacket::encode)
                .decoder(WorkplaceRemovePacket::decode)
                .consumerMainThread(WorkplaceRemovePacket::handle)
                .add();

        // Append new IDs only; do not renumber existing intro/dialogue packets.
        CHANNEL.messageBuilder(
                RiftPromptPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RiftPromptPacket::encode)
                .decoder(RiftPromptPacket::decode)
                .consumerMainThread(RiftPromptPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                RiftChoicePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RiftChoicePacket::encode)
                .decoder(RiftChoicePacket::decode)
                .consumerMainThread(RiftChoicePacket::handle)
                .add();
        CHANNEL.messageBuilder(
                RiftCreditsPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RiftCreditsPacket::encode)
                .decoder(RiftCreditsPacket::decode)
                .consumerMainThread(RiftCreditsPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                RiftAbortPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RiftAbortPacket::encode)
                .decoder(RiftAbortPacket::decode)
                .consumerMainThread(RiftAbortPacket::handle)
                .add();
        CHANNEL.messageBuilder(
                RiftDecisionFeedbackPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RiftDecisionFeedbackPacket::encode)
                .decoder(RiftDecisionFeedbackPacket::decode)
                .consumerMainThread(RiftDecisionFeedbackPacket::handle)
                .add();
    }

    private CyberNpcNetwork() {
    }
}
