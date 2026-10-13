package com.cyberspectraa.cybernpc.town;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Explicit opt-in vanilla workstation recognition. A non-workstation block
 * can never accidentally become a job just because it was right-clicked.
 */
public final class TownJobTypes {
    @Nullable
    public static String forBlock(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null || !"minecraft".equals(id.getNamespace())) return null;
        return switch (id.getPath()) {
            case "smithing_table", "blast_furnace", "grindstone",
                    "anvil", "chipped_anvil", "damaged_anvil" -> "Blacksmith";
            case "lectern" -> "Librarian";
            case "composter" -> "Farmer";
            case "smoker" -> "Cook";
            case "brewing_stand" -> "Alchemist";
            case "barrel" -> "Merchant";
            case "fletching_table" -> "Fletcher";
            case "cartography_table" -> "Cartographer";
            case "stonecutter" -> "Mason";
            case "loom" -> "Tailor";
            case "crafting_table" -> "Artisan";
            case "furnace" -> "Smelter";
            default -> null;
        };
    }
    private TownJobTypes() {}
}
