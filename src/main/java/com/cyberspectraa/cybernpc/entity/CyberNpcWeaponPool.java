package com.cyberspectraa.cybernpc.entity;

import com.cyberspectraa.cybernpc.CyberNpc;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CyberNpcWeaponPool {
    public static final TagKey<Item> WILD_NPC_SWORDS =
            TagKey.create(Registries.ITEM, new ResourceLocation(CyberNpc.MOD_ID, "wild_npc_swords"));

    public static final TagKey<Item> WILD_NPC_RANGED_WEAPONS =
            TagKey.create(Registries.ITEM, new ResourceLocation(CyberNpc.MOD_ID, "wild_npc_ranged_weapons"));

    public static ItemStack randomWildSword(RandomSource random) {
        return randomFromTag(WILD_NPC_SWORDS, random, Items.WOODEN_SWORD);
    }

    public static ItemStack randomWildRangedWeapon(RandomSource random) {
        return randomFromTag(WILD_NPC_RANGED_WEAPONS, random, Items.BOW);
    }

    private static ItemStack randomFromTag(
            TagKey<Item> tagKey,
            RandomSource random,
            Item fallback
    ) {
        return BuiltInRegistries.ITEM
                .getTag(tagKey)
                .flatMap(tag -> tag.getRandomElement(random))
                .map(holder -> new ItemStack(holder.value()))
                .orElseGet(() -> new ItemStack(fallback));
    }

    private CyberNpcWeaponPool() {
    }
}
