package com.cyberspectraa.cybernpc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class LetterWritingClient {
    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(
                new LetterWritingScreen(hand)
        );
    }

    private LetterWritingClient() {
    }
}
