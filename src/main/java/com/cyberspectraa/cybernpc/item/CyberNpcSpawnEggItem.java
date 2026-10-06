package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.entity.NpcType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
    private final String customName;
    private final String storyNpcId;

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
                "",
                "",
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
        this(
                typeSupplier,
                backgroundColor,
                highlightColor,
                npcType,
                role,
                canWander,
                "",
                "",
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
            String customName,
            String storyNpcId,
            Item.Properties properties
    ) {
        super(
                typeSupplier,
                backgroundColor,
                highlightColor,
                properties
        );
        this.npcType = npcType;
        this.role = role == null ? "" : role.trim();
        this.canWander = canWander;
        this.customName =
                customName == null ? "" : customName.trim();
        this.storyNpcId =
                storyNpcId == null
                    ? ""
                    : storyNpcId.trim().toLowerCase();
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        applyNpcData(stack);
        return stack;
    }

    @Override
    public InteractionResult useOn(
            UseOnContext context
    ) {
        applyNpcData(context.getItemInHand());
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        applyNpcData(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    private void applyNpcData(ItemStack stack) {
        CompoundTag entityTag =
                stack.getOrCreateTagElement("EntityTag");

        entityTag.putString(
                "CyberNpcType",
                npcType.serializedName()
        );

        if (!role.isEmpty()) {
            entityTag.putString(
                    "CyberNpcRole",
                    role
            );
        }

        entityTag.putBoolean(
                "CyberNpcCanWander",
                canWander
        );

        if (!customName.isEmpty()) {
            entityTag.putString(
                    "CustomName",
                    Component.Serializer.toJson(
                            Component.literal(customName)
                    )
            );
            entityTag.putBoolean(
                    "CustomNameVisible",
                    true
            );
            entityTag.putBoolean(
                    "PersistenceRequired",
                    true
            );
        }

        if (!storyNpcId.isEmpty()) {
            CompoundTag forgeData =
                    entityTag.contains(
                            "ForgeData",
                            Tag.TAG_COMPOUND
                    )
                    ? entityTag.getCompound("ForgeData")
                    : new CompoundTag();

            forgeData.putString(
                    "CyberQuestNpcId",
                    storyNpcId
            );
            forgeData.putString(
                    "CyberNpcStoryId",
                    storyNpcId
            );

            entityTag.put(
                    "ForgeData",
                    forgeData
            );
        }
    }
}
