package com.cyberspectraa.cybernpc.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only preferences. Grunts never affect server entities or gameplay. */
public final class NpcVoiceConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue VOLUME;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("dialogue_voice");
        ENABLED = builder.comment("Play short NPC voice grunts as dialogue letters appear.")
                .define("enabled", true);
        VOLUME = builder.comment("Volume of dialogue voice grunts. 0 disables sound.")
                .defineInRange("volume", 0.30D, 0D, 1D);
        builder.pop();
        SPEC = builder.build();
    }

    private NpcVoiceConfig() { }
}