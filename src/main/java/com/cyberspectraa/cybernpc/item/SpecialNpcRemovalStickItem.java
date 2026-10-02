package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.service.SpecialNpcSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SpecialNpcRemovalStickItem extends Item {
    public SpecialNpcRemovalStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!(target instanceof CyberNpcEntity npc)
                || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        if (!npc.isSpecialServiceNpc()
                || npc.getSpecialNpcId() == null) {
            player.displayClientMessage(
                    Component.literal(
                            "This NPC is not a registered special NPC."
                    ),
                    true
            );
            return InteractionResult.CONSUME;
        }

        String name = npc.getCustomName() == null
                ? npc.getRole()
                : npc.getCustomName().getString();

        npc.releasePersistentClaims();

        boolean removed = SpecialNpcSavedData.get(level)
                .removePermanent(npc.getSpecialNpcId());

        if (removed) {
            player.displayClientMessage(
                    Component.literal(
                            "Permanently removed " + name
                                    + ". It will not respawn."
                    ),
                    true
            );
            npc.discard();
        } else {
            player.displayClientMessage(
                    Component.literal(
                            "Could not find that special NPC record."
                    ),
                    true
            );
        }

        return InteractionResult.CONSUME;
    }
}
