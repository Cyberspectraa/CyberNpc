package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.entity.NpcType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

public final class CyberNpcSpawnEggItem extends ForgeSpawnEggItem {
    private final NpcType npcType;
    private final String role;
    private final boolean canWander;

    public CyberNpcSpawnEggItem(
            Supplier<? extends EntityType<? extends Mob>> typeSupplier,
            int backgroundColor,
            int highlightColor,
            NpcType npcType,
            Item.Properties properties
    ) {
        this(
                typeSupplier,
                backgroundColor,
                highlightColor,
                npcType,
                "",
                true,
                properties
        );
    }

    public CyberNpcSpawnEggItem(
            Supplier<? extends EntityType<? extends Mob>> typeSupplier,
            int backgroundColor,
            int highlightColor,
            NpcType npcType,
            String role,
            boolean canWander,
            Item.Properties properties
    ) {
        super(typeSupplier, backgroundColor, highlightColor, properties);
        this.npcType = npcType;
        this.role = role == null ? "" : role.trim();
        this.canWander = canWander;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        applyNpcType(stack);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        applyNpcType(context.getItemInHand());
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        applyNpcType(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    private void applyNpcType(ItemStack stack) {
        CompoundTag entityTag = stack.getOrCreateTagElement("EntityTag");
        entityTag.putString("CyberNpcType", npcType.serializedName());

        if (!role.isEmpty()) {
            entityTag.putString("CyberNpcRole", role);
            entityTag.putBoolean("CyberNpcCanWander", canWander);
        }
    }
}
