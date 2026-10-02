package com.cyberspectraa.season2arrival;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Season2Arrival.MOD_ID)
public final class Season2Arrival {
    public static final String MOD_ID = "season2arrival";
    public static final Logger LOGGER = LoggerFactory.getLogger("Season2Arrival");

    public Season2Arrival() {
        ArrivalNetwork.register();
        MinecraftForge.EVENT_BUS.register(new ArrivalEvents());
    }
}
