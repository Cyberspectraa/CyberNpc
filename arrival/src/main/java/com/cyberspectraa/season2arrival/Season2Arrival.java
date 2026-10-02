package com.cyberspectraa.season2arrival;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(Season2Arrival.MOD_ID)
public final class Season2Arrival {
    public static final String MOD_ID = "season2arrival";

    public Season2Arrival() {
        MinecraftForge.EVENT_BUS.register(new ArrivalEvents());
    }
}
