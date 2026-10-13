package com.cyberspectraa.cybernpc.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** All settings are client-side only and do not change gameplay values. */
public final class PlayerHudConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA;
    public static final ForgeConfigSpec.BooleanValue CHARACTER_PREVIEW;
    public static final ForgeConfigSpec.IntValue X_OFFSET;
    public static final ForgeConfigSpec.IntValue Y_OFFSET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("rpg_hud");
        ENABLED = builder.comment("Show the top-left CyberNpc player HUD.")
                .define("enabled", true);
        REPLACE_VANILLA = builder.comment(
                "Hide vanilla health, hunger and armour, plus Iron's native mana bar",
                "while the new CyberNpc HUD is displayed. Leave the hotbar and XP intact.")
                .define("replace_vanilla_bars", true);
        CHARACTER_PREVIEW = builder.comment(
                "Render your real Minecraft character, skin and gear in the HUD.")
                .define("show_character_preview", true);
        X_OFFSET = builder.comment("Horizontal HUD offset from the top-left screen corner.")
                .defineInRange("x_offset", 8, 0, 1000);
        Y_OFFSET = builder.comment("Vertical HUD offset from the top-left screen corner.")
                .defineInRange("y_offset", 8, 0, 1000);
        builder.pop();
        SPEC = builder.build();
    }
    private PlayerHudConfig() {}
}
