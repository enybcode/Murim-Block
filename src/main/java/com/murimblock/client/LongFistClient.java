package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import com.murimblock.martial.LongFist;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Client side of the Long Fist (docs/combat/styles/long-fist.md): the bare-hand guard. Epic Fight has no guard pose for
 * bare hands, so a player whose main style is the Long Fist shows the art's own (forearms up before the face) while
 * holding the guard; everything else is Epic Fight's and the server's.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class LongFistClient {
    private LongFistClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(e -> {
            if (e.getEntityPatch() instanceof PlayerPatch<?>) e.getAnimator().addLivingAnimation(Motions.FIST_GUARD, MurimAnimations.LONG_FIST_GUARD);
        }, "murimblock:long_fist_animations", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(e -> {
            var patch = e.getPlayerPatch();
            boolean guarding = e.getMotion() == LivingMotions.BLOCK || patch.getHoldingSkill() instanceof GuardSkill;
            if (guarding && (LongFist.guardsBareHanded(patch.getOriginal())
                    || com.murimblock.martial.MountainShatteringFist.guardsBareHanded(patch.getOriginal()))) e.setMotion(Motions.FIST_GUARD);
        }, "murimblock:long_fist_guard", -10);
    }
}
