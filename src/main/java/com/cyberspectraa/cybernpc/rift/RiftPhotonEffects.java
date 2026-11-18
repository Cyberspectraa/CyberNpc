package com.cyberspectraa.cybernpc.rift;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Forge-server-side Photon bridge, using Photon 1.x's built-in server
 * command/network. Photon is a required mod for CyberNpc.
 */
public final class RiftPhotonEffects {
    public static final String EFFECT_ID = "cybernpc:island_rift";
    private static final Logger LOGGER = LoggerFactory.getLogger(RiftPhotonEffects.class);
    private static boolean failedBefore;

    private RiftPhotonEffects() {}

    public static void play(ServerLevel level, BlockPos lowerPos) {
        // The block effect anchors to its center; -0.5 Y anchors the base
        // of the 2-block tall visual exactly at the lower block's floor.
        String command = String.format(Locale.ROOT,
                "photon fx %s block %d %d %d 0 -0.5 0 0 0 0 1 1 1 0 false true false",
                EFFECT_ID, lowerPos.getX(), lowerPos.getY(), lowerPos.getZ());

        CommandSourceStack source = level.getServer().createCommandSourceStack()
                .withLevel(level)
                .withPosition(new Vec3(lowerPos.getX() + 0.5D,
                        lowerPos.getY() + 0.5D, lowerPos.getZ() + 0.5D))
                .withPermission(4)
                .withSuppressedOutput();

        try {
            boolean played = level.getServer().getCommands()
                    .performPrefixedCommand(source, command) > 0;
            if (!played && !failedBefore) {
                failedBefore = true;
                LOGGER.warn("Island Rift Photon effect was not started. Check asset {}.",
                        EFFECT_ID);
            }
        } catch (RuntimeException error) {
            if (!failedBefore) {
                failedBefore = true;
                LOGGER.warn("Could not start Island Rift Photon effect {}.", EFFECT_ID, error);
            }
        }
    }
}
