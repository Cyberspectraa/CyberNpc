package com.cyberspectraa.cybernpc.compat;

import com.cyberspectraa.cybernpc.entity.WildNpcGearTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

public final class TinkersConstructCompat {
    public static final String MOD_ID = "tconstruct";
    private static final float TINKERS_SWORD_CHANCE = 0.38F;

    private static final Set<String> SUPPORTED_WEAPONS = Set.of(
            "dagger",
            "sword",
            "cleaver"
    );

    private static boolean reflectionResolved;
    private static Method getToolDefinitionMethod;
    private static Method ensureInitializedMethod;
    private static Class<?> modifiableClass;

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ItemStack maybeCreateWildSword(
            RandomSource random,
            WildNpcGearTier tier,
            ItemStack vanillaFallback
    ) {
        ItemStack fallback = vanillaFallback == null
                ? new ItemStack(Items.IRON_SWORD)
                : vanillaFallback;

        if (!isLoaded()
                || random == null
                || random.nextFloat() >= TINKERS_SWORD_CHANCE) {
            return fallback;
        }

        String toolId = pickToolId(random, tier);
        Item item = BuiltInRegistries.ITEM
                .getOptional(new ResourceLocation(MOD_ID, toolId))
                .orElse(Items.AIR);

        if (item == Items.AIR) {
            return fallback;
        }

        ItemStack stack = new ItemStack(item);
        if (!initializeTool(stack, item)) {
            return fallback;
        }

        return stack;
    }

    public static boolean isSupportedSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(stack.getItem());

        return MOD_ID.equals(id.getNamespace())
                && SUPPORTED_WEAPONS.contains(id.getPath());
    }

    private static String pickToolId(
            RandomSource random,
            WildNpcGearTier tier
    ) {
        float roll = random.nextFloat();

        return switch (tier) {
            case STANDARD -> roll < 0.22F
                    ? "dagger"
                    : "sword";
            case FINE -> roll < 0.15F
                    ? "dagger"
                    : (roll > 0.92F ? "cleaver" : "sword");
            case RARE -> roll > 0.78F
                    ? "cleaver"
                    : "sword";
            case ELITE -> roll > 0.58F
                    ? "cleaver"
                    : "sword";
        };
    }

    private static boolean initializeTool(
            ItemStack stack,
            Item item
    ) {
        try {
            resolveReflection();

            if (modifiableClass == null
                    || getToolDefinitionMethod == null
                    || ensureInitializedMethod == null
                    || !modifiableClass.isInstance(item)) {
                return false;
            }

            Object definition =
                    getToolDefinitionMethod.invoke(item);

            ensureInitializedMethod.invoke(
                    null,
                    stack,
                    definition
            );

            // A valid Tinkers tool receives its material/stat data in NBT.
            // If datapack tool data was not ready yet, keep the vanilla
            // fallback rather than giving an NPC a zero-stat blank tool.
            return stack.hasTag()
                    && stack.getTag() != null
                    && !stack.getTag().isEmpty();
        } catch (ReflectiveOperationException
                 | LinkageError
                 | RuntimeException ignored) {
            return false;
        }
    }

    private static synchronized void resolveReflection()
            throws ReflectiveOperationException {
        if (reflectionResolved) {
            return;
        }

        reflectionResolved = true;

        modifiableClass = Class.forName(
                "slimeknights.tconstruct.library.tools.item.IModifiable"
        );
        getToolDefinitionMethod =
                modifiableClass.getMethod("getToolDefinition");

        Class<?> toolStackClass = Class.forName(
                "slimeknights.tconstruct.library.tools.nbt.ToolStack"
        );

        for (Method method : toolStackClass.getMethods()) {
            if (!"ensureInitialized".equals(method.getName())
                    || method.getParameterCount() != 2
                    || !Modifier.isStatic(method.getModifiers())
                    || method.getParameterTypes()[0]
                    != ItemStack.class) {
                continue;
            }

            ensureInitializedMethod = method;
            break;
        }
    }

    private TinkersConstructCompat() {
    }
}
