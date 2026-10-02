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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

public final class DropBoxBlock extends Block {
    private static final int CAPACITY = 64;

    public DropBoxBlock(Properties properties) {
        super(properties);
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

        if (level instanceof ServerLevel serverLevel) {
            MailSavedData.get(serverLevel).registerDropBox(
                    serverLevel.dimension(),
                    pos
            );
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
        mail.registerDropBox(serverLevel.dimension(), pos);

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModItems.ADDRESSED_LETTER.get())) {
            player.sendSystemMessage(Component.literal(
                    "Drop Box: " + mail.pendingCountAt(
                            serverLevel.dimension(),
                            pos
                    ) + "/" + CAPACITY + " letters waiting for collection."
            ));
            return InteractionResult.CONSUME;
        }

        MailItemData.Draft draft = MailItemData.readDraft(held);
        if (draft == null) {
            player.sendSystemMessage(Component.literal(
                    "This addressed letter is missing its postal information."
            ));
            return InteractionResult.CONSUME;
        }

        if (mail.pendingCountAt(serverLevel.dimension(), pos) >= CAPACITY) {
            player.sendSystemMessage(Component.literal(
                    "This Drop Box is full."
            ));
            return InteractionResult.CONSUME;
        }

        long recordId = mail.post(
                draft,
                serverLevel.dimension(),
                pos
        );

        if (recordId < 0L) {
            player.sendSystemMessage(Component.literal(
                    "The post office could not register this letter."
            ));
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        level.playSound(
                null,
                pos,
                SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS,
                0.65F,
                0.85F
        );

        player.sendSystemMessage(Component.literal(
                "Letter posted for " + draft.recipientName() + "."
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
            MailSavedData mail = MailSavedData.get(serverLevel);
            mail.unregisterDropBox(serverLevel.dimension(), pos);

            List<MailRecord> recovered = mail.removePendingAt(
                    serverLevel.dimension(),
                    pos
            );

            for (MailRecord record : recovered) {
                ItemStack letter = new ItemStack(
                        ModItems.ADDRESSED_LETTER.get()
                );
                MailItemData.writeDraft(
                        letter,
                        record.senderId(),
                        record.senderName(),
                        record.recipientId(),
                        record.recipientName(),
                        record.message()
                );
                popResource(level, pos, letter);
            }
        }

        super.onRemove(state, level, pos, newState, moving);
    }
}
