package com.cyberspectraa.cybernpc.event;

import com.cyberspectraa.cybernpc.CyberNpc;
import com.cyberspectraa.cybernpc.entity.GuardAlarmSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BellBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = CyberNpc.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GuardBellEvents {
    @SubscribeEvent
    public static void onBellUse(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockState(event.getPos()).getBlock()
                instanceof BellBlock)) {
            return;
        }

        if (GuardAlarmSystem.ringBellAndAlert(
                level,
                event.getPos(),
                event.getEntity(),
                null
        )) {
            // We performed the server-side BellBlock ring directly, so cancel
            // vanilla's second server-side use to avoid a duplicate ring.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private GuardBellEvents() {
    }
}
