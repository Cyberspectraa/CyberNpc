package com.cyberspectraa.season2arrival;

import net.minecraftforge.fml.ModList;

/**
 * Client-only dispatch class. Photon types are deliberately isolated in ArrivalPhotonVfx
 * so the base Arrival mod can still load if Photon is ever removed.
 */
public final class ArrivalClientVfx {
    private ArrivalClientVfx() {
    }

    public static void handle(ArrivalVfxPacket packet) {
        if (ModList.get().isLoaded("photon")) {
            try {
                ArrivalPhotonVfx.play(packet);
                return;
            } catch (Throwable throwable) {
                Season2Arrival.LOGGER.warn("Photon arrival VFX failed; using lightweight fallback.", throwable);
            }
        }

        ArrivalVanillaClientVfx.play(packet);
    }
}
