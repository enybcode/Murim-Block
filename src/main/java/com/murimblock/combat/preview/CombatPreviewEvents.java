package com.murimblock.combat.preview;

import com.murimblock.Murimblock;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class CombatPreviewEvents {
    private CombatPreviewEvents() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void attack(AttackEntityEvent event) { cancel(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void useItem(LivingEntityUseItemEvent.Start event) { cancel(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void use(PlayerInteractEvent.RightClickItem event) { cancel(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) { cancel(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void attackBlock(PlayerInteractEvent.LeftClickBlock event) { cancel(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void interact(PlayerInteractEvent.EntityInteract event) { cancel(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) { cancel(event.getEntity()); }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) { cancel(event.getEntity()); }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { cancel(event.getEntity()); }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        cancel(event.getOriginal());
        cancel(event.getEntity());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { cancel(event.getEntity()); }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer player) CombatPreviewService.refreshTracking(player);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) { CombatPreviewService.tick(event.getServer()); }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CombatPreviewService.clearAll(); }

    private static void cancel(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof ServerPlayer player) CombatPreviewService.cancel(player);
    }
}
