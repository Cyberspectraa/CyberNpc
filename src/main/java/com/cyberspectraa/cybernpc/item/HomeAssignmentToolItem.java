package com.cyberspectraa.cybernpc.item;

import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.town.FloorRoomScanner;
import com.cyberspectraa.cybernpc.town.TownHomeSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Dedicated room-selection item, independent from Town Register's workstation
 * and town-door modes. Holding it does not open any unconfigured NPC dialogue.
 */
public final class HomeAssignmentToolItem extends Item {
    private static final String DATA = "CyberNpcRoomTool";
    private static final String NPC = "Npc";
    private static final String DIMENSION = "Dimension";
    private static final String NAME = "Name";

    public HomeAssignmentToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (!(target instanceof CyberNpcEntity npc)) return InteractionResult.PASS;
        if (!player.level().isClientSide) {
            if (npc.getNpcType() != NpcType.MAIN || npc.isSpecialServiceNpc()) {
                tell(player, "Only ordinary Main NPCs use these rooms. Service NPCs keep their existing posts.");
            } else {
                CompoundTag tag = stack.getOrCreateTagElement(DATA);
                tag.putUUID(NPC, npc.getUUID());
                tag.putString(DIMENSION, npc.level().dimension().location().toString());
                tag.putString(NAME, npc.getName().getString());
                int count = TownHomeSavedData.get((ServerLevel) player.level()).roomCount(npc.getUUID());
                tell(player, "Selected " + npc.getName().getString() + " (" + count
                        + "/10 rooms). Right-click FLOOR tiles to add rooms; sneak-click a saved floor to remove.");
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /**
     * Invoked from the earliest block-click event, before interacting with
     * chests, bells or any other block. This does not alter the clicked block.
     */
    public static void handleFloorClick(ServerPlayer player, ItemStack stack,
                                         BlockPos clickedFloor, boolean upwardFace,
                                         boolean removing) {
        CompoundTag tag = stack.getTagElement(DATA);
        if (tag == null || !tag.hasUUID(NPC)) {
            tell(player, "First right-click an ordinary Main NPC with the Home Assignment Tool.");
            return;
        }
        if (!player.level().dimension().location().toString()
                .equals(tag.getString(DIMENSION))) {
            tell(player, "The selected NPC belongs to another dimension. Select an NPC here.");
            return;
        }
        if (!upwardFace) {
            tell(player, "Right-click the TOP of a floor block inside the room.");
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos inside = clickedFloor.above();
        if (removing) {
            tell(player, TownHomeSavedData.get(level).removeFloorRoom(
                    level, tag.getUUID(NPC), inside));
            return;
        }
        FloorRoomScanner.Result room = FloorRoomScanner.scan(level, inside);
        if (!room.success()) {
            tell(player, room.message());
            return;
        }
        String message = TownHomeSavedData.get(level).addFloorRoom(
                level, tag.getUUID(NPC), room);
        tell(player, tag.getString(NAME) + ": " + message);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CompoundTag tag = tool.getTagElement(DATA);
            if (player.isShiftKeyDown()) {
                if (tag != null) {
                    tag.remove(NPC);
                    tag.remove(DIMENSION);
                    tag.remove(NAME);
                }
                tell(player, "NPC selection cleared. Previously assigned rooms are unchanged.");
            } else if (tag != null && tag.hasUUID(NPC)) {
                UUID npc = tag.getUUID(NPC);
                tell(player, "Selected " + tag.getString(NAME) + ": "
                        + TownHomeSavedData.get((ServerLevel) level).roomCount(npc)
                        + "/10 rooms. Click floors to add; sneak-click floors to remove.");
            } else {
                tell(player, "Right-click an ordinary Main NPC to select it, then click room floors.");
            }
        }
        return InteractionResultHolder.sidedSuccess(tool, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click NPC: select").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click floor: add room (up to 10)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + floor: remove room").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + air: clear NPC selection").withStyle(ChatFormatting.GRAY));
    }

    private static void tell(Player player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }
}
