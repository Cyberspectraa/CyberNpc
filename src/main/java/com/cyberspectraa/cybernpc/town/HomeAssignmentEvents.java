package com.cyberspectraa.cybernpc.town;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.item.HomeAssignmentToolItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Intercepts floor use before vanilla block UIs swallow the tool interaction.
 * The authoritative scan and persistent assignment happen only server-side.
 */
@Mod.EventBusSubscriber(modid = CyberNpc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HomeAssignmentEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFloorClick(PlayerInteractEvent.RightClickBlock event) {
        if (!TownSystems.housingAndJobsEnabled()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity().getMainHandItem().getItem()
                    instanceof HomeAssignmentToolItem)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            HomeAssignmentToolItem.handleFloorClick(
                    player, player.getMainHandItem(), event.getPos(),
                    event.getFace() == net.minecraft.core.Direction.UP,
                    player.isShiftKeyDown());
        }
    }

    private HomeAssignmentEvents() {}
}
