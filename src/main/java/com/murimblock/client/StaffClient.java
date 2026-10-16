package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Client side of the Traveller's Staff Art: the Spinning Rampart held. Its start and end are action animations; while it
 * is held, a player in {@link #SPINNING} (told by the server, StaffSpinPayload) shows the looping spin as a living motion
 * of the whole body (as the meditation). The local player cannot jump while it spins (the server slows the walk).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class StaffClient {
    /** The entity ids of the players holding the Spinning Rampart. */
    public static final Set<Integer> SPINNING = ConcurrentHashMap.newKeySet();

    private StaffClient() {
    }

    /** The server says a player started or stopped spinning. */
    public static void spin(int entityId, boolean on) {
        if (on) SPINNING.add(entityId);
        else SPINNING.remove(entityId);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (SPINNING.contains(event.getEntity().getId())) event.getInput().jumping = false;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SPINNING.clear();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(e -> {
            if (e.getEntityPatch() instanceof PlayerPatch<?>) e.getAnimator().addLivingAnimation(Motions.STAFF_RAMPART, MurimAnimations.STAFF_RAMPART_LOOP);
        }, "murimblock:staff_animations", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(e -> {
            if (SPINNING.contains(e.getPlayerPatch().getOriginal().getId())) e.setMotion(Motions.STAFF_RAMPART);
        }, "murimblock:staff_rampart_motion", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(e -> {
            if (SPINNING.contains(e.getPlayerPatch().getOriginal().getId())) e.setMotion(Motions.STAFF_RAMPART);
        }, "murimblock:staff_rampart_composite", 0);
    }
}
