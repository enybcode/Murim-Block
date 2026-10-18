package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.DemonArtAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import com.murimblock.martial.KaguneState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Devouring Demon Art's beast gait (docs/combat/styles/devouring-demon-art.md; Enzo, 18/10/2026: "start working on
 * being on four legs"): in the demon form, once the transformation is over, the player stands, walks and runs on all
 * fours (DemonArtAnimations), in place of Epic Fight's own motions, with nothing layered over them.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class DemonBeastClient {
    /** Walking speed (of the walk animation's own measure) above which the beast is moving. */
    private static final float MOVING = 0.02F;

    private DemonBeastClient() { }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(e -> {
            if (!(e.getEntityPatch() instanceof PlayerPatch<?>)) return;
            e.getAnimator().addLivingAnimation(Motions.DEMON_BEAST, DemonArtAnimations.BEAST_IDLE);
            e.getAnimator().addLivingAnimation(Motions.DEMON_BEAST_WALK, DemonArtAnimations.BEAST_WALK);
            e.getAnimator().addLivingAnimation(Motions.DEMON_BEAST_RUN, DemonArtAnimations.BEAST_RUN);
        }, "murimblock:demon_beast_animations", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(e -> {
            Motions motion = motion(e.getPlayerPatch().getOriginal());
            if (motion != null) e.setMotion(motion);
        }, "murimblock:demon_beast_motion", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(e -> {
            Motions motion = motion(e.getPlayerPatch().getOriginal());
            if (motion != null) e.setMotion(motion);
        }, "murimblock:demon_beast_composite", 0);
    }

    /** The beast's motion for a player in the demon form (once the transformation is over), or null. */
    @Nullable public static Motions motion(Player player) {
        if (!inBeastForm(player)) return null;
        if (player.walkAnimation.speed() <= MOVING) return Motions.DEMON_BEAST;
        return player.isSprinting() ? Motions.DEMON_BEAST_RUN : Motions.DEMON_BEAST_WALK;
    }

    /** Whether the player is in the demon form now, past the transformation. */
    public static boolean inBeastForm(Player player) {
        var state = KaguneState.of(player);
        if (state.form() == KaguneState.Form.DEMON) return true;
        if (state.form() != KaguneState.Form.TRANSFORM) return false;
        return (player.level().getGameTime() - state.since()) / 20F >= KaguneModel.get().length("transform");
    }
}
