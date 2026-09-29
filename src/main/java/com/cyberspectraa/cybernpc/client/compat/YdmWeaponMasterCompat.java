package com.cyberspectraa.cybernpc.client.compat;

import net.minecraftforge.fml.ModList;

public final class YdmWeaponMasterCompat {
    public static final String MOD_ID = "weaponmaster_ydm";

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private YdmWeaponMasterCompat() {
    }
}
