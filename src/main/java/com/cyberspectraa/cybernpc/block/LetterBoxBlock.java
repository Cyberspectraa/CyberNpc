package com.cyberspectraa.cybernpc.block;

import com.cyberspectraa.cybernpc.mail.MailItemData;
import com.cyberspectraa.cybernpc.mail.MailRecord;
import com.cyberspectraa.cybernpc.mail.MailSavedData;
import com.cyberspectraa.cybernpc.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public final class LetterBoxBlock extends PostalBlockBase {
    private static final int CAPACITY = 9;

    public LetterBoxBlock(Properties properties) {
        super(Kind.LETTER_BOX, properties);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level instanceof ServerLevel serverLevel
                && placer instanceof Player player) {
            MailSavedData.get(serverLevel).registerLetterBox(
                    player.getUUID(),
                    serverLevel.dimension(),
                    pos
            );

            player.sendSystemMessage(Component.literal(
                    "This is now your active Letter Box."
            ));
        }
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        MailSavedData mail = MailSavedData.get(serverLevel);
        MailSavedData.PostalAddress active = mail.getLetterBox(
                player.getUUID()
        );

        if (active == null
                || !active.dimension().equals(serverLevel.dimension())
                || !active.pos().equals(pos)) {
            player.sendSystemMessage(Component.literal(
                    "This is not your active Letter Box."
            ));
            return InteractionResult.CONSUME;
        }

        List<MailRecord> waiting = mail.boxedMailFor(
                player.getUUID(),
                CAPACITY
        );

        if (waiting.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Your Letter Box is empty."
            ));
            return InteractionResult.CONSUME;
        }

        int collected = 0;

        for (MailRecord record : waiting) {
            ItemStack sealed = new ItemStack(
                    ModItems.SEALED_LETTER.get()
            );
            MailItemData.writeRecordId(sealed, record.id());

            if (!player.getInventory().add(sealed)) {
                break;
            }

            mail.markDelivered(record.id());
            collected++;
        }

        if (collected > 0) {
            level.playSound(
                    null,
                    pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS,
                    0.65F,
                    1.15F
            );
        }

        player.sendSystemMessage(Component.literal(
                "Collected " + collected + " letter"
                        + (collected == 1 ? "" : "s")
                        + "."
        ));

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean moving
    ) {
        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel serverLevel) {
            MailSavedData.get(serverLevel).unregisterLetterBoxAt(
                    serverLevel.dimension(),
                    pos
            );
        }

        super.onRemove(state, level, pos, newState, moving);
    }
}
