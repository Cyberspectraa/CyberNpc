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
    public static final TagKey<Item> WILD_NPC_WEAPONS =
            TagKey.create(Registries.ITEM, new ResourceLocation(CyberNpc.MOD_ID, "wild_npc_weapons"));

    public static ItemStack randomWildWeapon(RandomSource random) {
        return BuiltInRegistries.ITEM
                .getRandomElementOf(WILD_NPC_WEAPONS, random)
                .map(holder -> new ItemStack(holder.value()))
                .orElseGet(() -> new ItemStack(Items.WOODEN_SWORD));
    }

    private CyberNpcWeaponPool() {
    }
}
