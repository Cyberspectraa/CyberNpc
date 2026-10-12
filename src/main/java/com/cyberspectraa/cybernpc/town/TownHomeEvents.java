package com.cyberspectraa.cybernpc.town;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.CyberNpcEntity;
import com.cyberspectraa.cybernpc.entity.NpcType;
import com.cyberspectraa.cybernpc.item.TownRegisterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One-click Town Register controls, implemented in the block interaction hook
 * because vanilla bells and doors consume their own right-click interactions
 * before Item#useOn. This must run before the guard's ordinary bell handler.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TownHomeEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRegister(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack stack = event.getEntity().getMainHandItem();
        if (!(stack.getItem() instanceof TownRegisterItem)) return;
        var state = event.getLevel().getBlockState(event.getPos());
        boolean bell = state.getBlock() instanceof BellBlock;
        boolean door = state.getBlock() instanceof DoorBlock;
        if (!bell && !door) return;

        // Consume both sides, but run all registry writes only on the server.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        TownHomeSavedData data = TownHomeSavedData.get(level);
        String response;
        if (bell) {
            response = data.useBell(level, event.getPos(), event.getEntity().isShiftKeyDown());
        } else {
            BlockPos lower = HouseInteriorScanner.lowerDoor(level, event.getPos());
            CyberNpcEntity selected = selectedMainNpc(level, stack);
            response = data.useDoor(level, lower, selected, event.getEntity().isShiftKeyDown());
        }
        event.getEntity().displayClientMessage(Component.literal(response), false);
    }

    @Nullable
    private static CyberNpcEntity selectedMainNpc(ServerLevel level, ItemStack register) {
        var tag = register.getTagElement("CyberNpcTownRegister");
        if (tag == null || !tag.hasUUID("SelectedEntity")) return null;
        Entity selected = level.getEntity(tag.getUUID("SelectedEntity"));
        return selected instanceof CyberNpcEntity npc
                && npc.getNpcType() == NpcType.MAIN
                && !npc.isSpecialServiceNpc() ? npc : null;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 1200L != 200L) return;
        TownHomeSavedData.get(level).populateOne(level);
    }

    @SubscribeEvent
    public static void onNpcDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof CyberNpcEntity npc
                && npc.getNpcType() == NpcType.MAIN
                && npc.level() instanceof ServerLevel level) {
            TownHomeSavedData.get(level).onNpcDeath(npc);
        }
    }

    private TownHomeEvents() {}
}
