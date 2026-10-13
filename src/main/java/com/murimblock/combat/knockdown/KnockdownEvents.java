package com.murimblock.combat.knockdown;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Game events of the knock-down: lethal PvP damage, what a downed or carrying player cannot do, carries and finishes. */
public final class KnockdownEvents {
    private KnockdownEvents() {
    }

    /** A player killed by another player falls K.O. instead (after totems, like any death). */
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KnockdownService.knocksDown(player, event.getSource())) {
            event.setCanceled(true);
            KnockdownService.knockDown(player);
        }
    }

    /** A downed player only takes the finish, or damage nothing can stop (the void, /kill). */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && KnockdownService.isDowned(player)
                && !event.getSource().is(KnockdownService.EXECUTION) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    /** A blow on someone performing a finish breaks it. */
    public static void onDamageTaken(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && event.getNewDamage() > 0) KnockdownService.interrupt(player);
    }

    public static void onAttack(AttackEntityEvent event) {
        if (KnockdownService.isHindered(event.getEntity())) event.setCanceled(true);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (KnockdownService.isHindered(event.getEntity())) event.setCanceled(true);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (KnockdownService.isHindered(event.getEntity())) event.setCanceled(true);
    }

    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (KnockdownService.isHindered(event.getEntity())) event.setCanceled(true);
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (KnockdownService.isHindered(event.getEntity())) event.setCanceled(true);
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (KnockdownService.isHindered(event.getEntity())) event.setCanceled(true);
    }

    /**
     * A carried player stays on their carrier until put down; a downed player mounts nothing by themself. Server only: a
     * client must follow whatever riding the server sends (refusing it there left bodies stuck on their carrier).
     */
    public static void onMount(EntityMountEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntityMounting() instanceof Player player)) return;
        if (event.isDismounting() && !KnockdownService.mayDismount(player)) event.setCanceled(true);
        if (event.isMounting() && KnockdownService.isDowned(player) && !(event.getEntityBeingMounted() instanceof Player)) event.setCanceled(true);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) KnockdownService.tick(player);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        KnockdownService.tickExecutions(event.getServer());
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) KnockdownService.forget(player);
    }

    /** A carrier who dies drops whoever they carried; a player who dies is no longer part of any finish. */
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) KnockdownService.forget(player);
    }
}
