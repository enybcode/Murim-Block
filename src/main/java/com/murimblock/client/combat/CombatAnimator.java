package com.murimblock.client.combat;

import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.combat.FighterState;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The one place that plays combat animations, for our own player and the ones we see. Each player plays one action
 * at a time; a new one cuts the current one only when it ranks as high or higher (a blow taken over a dash over a
 * guard over a strike), with a short fade. Nothing plays while the cultivation layer (lotus, getting up) is busy.
 */
final class CombatAnimator {
    private static final int FADE_TICKS = 2;

    /** What each player plays now, and for how many ticks more. */
    private static final Map<Integer, Playing> PLAYING = new HashMap<>();

    private CombatAnimator() {
    }

    private static final class Playing {
        final FighterState action;
        int ticksLeft;
        int held;

        Playing(FighterState action, int ticks) {
            this.action = action;
            this.ticksLeft = ticks;
        }
    }

    /** Whether the lotus or the getting up is playing, which combat must not cover. */
    static boolean cultivationBusy(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAnimationLayer(player, ClientCultivation.LAYER)
                instanceof PlayerAnimationController controller && controller.isActive();
    }

    /** Whether {@code action} could start on this player now. */
    static boolean canPlay(AbstractClientPlayer player, FighterState action) {
        if (cultivationBusy(player)) {
            return false;
        }
        Playing current = PLAYING.get(player.getId());
        return current == null || action.priority() >= current.action.priority();
    }

    /** Plays {@code animation} as {@code action} for {@code ticks}, if it may cut what is playing. */
    static boolean play(AbstractClientPlayer player, FighterState action, ResourceLocation animation, int ticks) {
        if (!canPlay(player, action) || !(controller(player) instanceof PlayerAnimationController controller)) {
            return false;
        }
        controller.unpause();
        if (controller.isActive()) {
            controller.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(FADE_TICKS, EasingType.EASE_OUT_SINE),
                    animation);
        } else {
            controller.triggerAnimation(animation);
        }
        PLAYING.put(player.getId(), new Playing(action, ticks));
        return true;
    }

    /** Stops whatever combat animation this player plays. */
    static void stop(AbstractClientPlayer player) {
        PLAYING.remove(player.getId());
        if (controller(player) instanceof PlayerAnimationController controller) {
            controller.unpause();
            controller.stopTriggeredAnimation();
        }
    }

    /** Hit-stop: the animation stands still for a few ticks, and lasts as much longer. */
    static void hold(AbstractClientPlayer player, int ticks) {
        Playing current = PLAYING.get(player.getId());
        if (ticks <= 0 || current == null || !(controller(player) instanceof PlayerAnimationController controller)) {
            return;
        }
        controller.pause();
        current.held = Math.max(current.held, ticks);
    }

    static void tick(Minecraft mc) {
        for (Iterator<Map.Entry<Integer, Playing>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Playing> entry = it.next();
            Playing playing = entry.getValue();
            AbstractClientPlayer player = mc.level.getEntity(entry.getKey()) instanceof AbstractClientPlayer p ? p : null;
            if (player == null) {
                it.remove();
                continue;
            }
            if (playing.held > 0) {
                if (--playing.held == 0 && controller(player) instanceof PlayerAnimationController controller) {
                    controller.unpause();
                }
                continue;
            }
            if (--playing.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    static void clear() {
        PLAYING.clear();
    }

    private static Object controller(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAnimationLayer(player, ClientCombat.LAYER);
    }
}
