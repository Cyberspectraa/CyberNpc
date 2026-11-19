package com.cyberspectraa.cybernpc.client;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Local player dialogue voices. Race, appearance and NPC gender are not
 * reliable evidence of how a player wants their character to sound.
 * This client setting never changes dialogue choices or server state.
 */
public final class DialogueVoiceConfig {
    public enum Profile { LOW, HIGH, OFF }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<Profile> PLAYER_VOICE;
    public static final ForgeConfigSpec.DoubleValue PLAYER_VOLUME;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("dialogue");
        PLAYER_VOICE = builder.comment(
                "The local player's short dialogue voice blips when replying.",
                "LOW uses the generic lower-voiced sample, HIGH the higher-voiced sample,",
                "and OFF silences only player replies. Independent of player race.")
                .defineEnum("player_voice", Profile.LOW);
        PLAYER_VOLUME = builder.comment(
                "Volume of local player reply blips (NPC voice volume is unchanged).")
                .defineInRange("player_voice_volume", 0.45D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private DialogueVoiceConfig() {}
}
