package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.network.DialogueOpenPacket;
import com.cyberspectraa.cybernpc.network.DialogueChoicePacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CyberNpcNetwork {
    private static final String PROTOCOL = "2";

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
                        LetterSubmitPacket.class,
                        nextId++
                )
                .encoder(LetterSubmitPacket::encode)
                .decoder(LetterSubmitPacket::decode)
                .consumerMainThread(LetterSubmitPacket::handle)
                .add();
    }

    private CyberNpcNetwork() {
    }
}
