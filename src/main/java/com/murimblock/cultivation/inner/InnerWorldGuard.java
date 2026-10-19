package com.murimblock.cultivation.inner;

import com.murimblock.Murimblock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The inner world is not mined (Enzo, 19/10/2026): no block of the island breaks, in any game mode, nor even starts to
 * crack under the cultivator's hand (on both sides, so that no cracking shows).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class InnerWorldGuard {
    private InnerWorldGuard() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level && InnerWorldService.isInnerWorld(level)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (InnerWorldService.isInnerWorld(event.getLevel())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (InnerWorldService.isInnerWorld(event.getEntity().level())) event.setCanceled(true);
    }
}
