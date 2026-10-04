package com.cyberspectraa.cybernpc.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.List;

public final class TinkersConstructCompat {
    public static final String MOD_ID = "tconstruct";

    private static final List<String> SWORD_TOOLS = List.of(
            "sword",
            "cleaver",
            "dagger"
    );

    private static boolean reflectionAttempted;
    private static Method buildRandomTool;
    private static Class<?> modifiableClass;

    private TinkersConstructCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Creates a genuine Tinkers tool with material data instead of returning
     * the raw registry item, which would be an unbuilt/invalid tool.
     */
    public static ItemStack createRandomSword(RandomSource random) {
        if (!isLoaded() || random == null || !initializeReflection()) {
            return ItemStack.EMPTY;
        }

        for (int attempt = 0; attempt < SWORD_TOOLS.size(); attempt++) {
            String path = SWORD_TOOLS.get(
                    random.nextInt(SWORD_TOOLS.size())
            );

            Item item = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation(MOD_ID, path)
            );

            if (item == null || !modifiableClass.isInstance(item)) {
                continue;
            }

            try {
                Object result = buildRandomTool.invoke(
                        null,
                        item,
                        random
                );

                if (result instanceof ItemStack stack
                        && !stack.isEmpty()) {
                    stack.getOrCreateTag().putBoolean(
                            "CyberNpcTinkersWeapon",
                            true
                    );
                    return stack;
                }
            } catch (ReflectiveOperationException
                     | RuntimeException ignored) {
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean initializeReflection() {
        if (reflectionAttempted) {
            return buildRandomTool != null
                    && modifiableClass != null;
        }

        reflectionAttempted = true;

        try {
            modifiableClass = Class.forName(
                    "slimeknights.tconstruct.library.tools.item.IModifiable"
            );

            Class<?> buildHandler = Class.forName(
                    "slimeknights.tconstruct.library.tools.helper.ToolBuildHandler"
            );

            buildRandomTool = buildHandler.getMethod(
                    "buildItemRandomMaterials",
                    modifiableClass,
                    RandomSource.class
            );

            return true;
        } catch (ReflectiveOperationException
                 | LinkageError
                 | RuntimeException ignored) {
            buildRandomTool = null;
            modifiableClass = null;
            return false;
        }
    }
}
