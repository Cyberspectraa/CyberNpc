package com.cyberspectraa.cybernpc.network;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CyberNpcNetwork {
    private static final String PROTOCOL = "1";

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
