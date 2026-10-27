package com.murimblock.shenfa.client;

import com.murimblock.shenfa.FootPlanter;
import com.murimblock.shenfa.LivingMotion;
import com.murimblock.shenfa.Pose;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Keeps a walking player's feet where they land ({@link FootPlanter}, one per player). Minecraft moves a walking body
 * at 4.3 blocks a second, more than a clip's stride covers at a natural pace, so a clip alone lets the planted foot
 * slide.
 */
final class FootPlanting {
    private static final Set<LivingMotion> STEPPING = EnumSet.of(LivingMotion.WALK, LivingMotion.RUN,
            LivingMotion.SNEAK_WALK);

    private static final Map<AbstractClientPlayer, State> STATES = new WeakHashMap<>();

    private FootPlanting() {
    }

    private static final class State {
        final FootPlanter planter = new FootPlanter();
        float lastTime = Float.NaN;
    }

    /**
     * Plants the feet of {@code pose} (changed in place).
     *
     * @param bodyYaw the body's turn as drawn (degrees)
     * @param scale   the player's scale (attribute)
     */
    static void apply(AbstractClientPlayer player, Pose pose, float partialTick, float bodyYaw, float scale) {
        State state = STATES.computeIfAbsent(player, key -> new State());
        float now = player.level().getGameTime() + partialTick;
        float seconds = Float.isNaN(state.lastTime) ? 0 : Math.max(0, Math.min(0.25f, (now - state.lastTime) / 20));
        state.lastTime = now;
        boolean stepping = SkeletalPlayers.animated() && player.onGround() && !player.isPassenger()
                && !player.isVisuallySwimming() && !player.isFallFlying() && !player.isSleeping()
                && STEPPING.contains(PlayerAnimators.motion(player));
        Vector3f at = new Vector3f((float) Mth.lerp(partialTick, player.xo, player.getX()),
                (float) Mth.lerp(partialTick, player.yo, player.getY()),
                (float) Mth.lerp(partialTick, player.zo, player.getZ()));
        // Where the body goes, in model space (the model faces +Z; the world is the model turned by the body's yaw).
        Vector3f heading = new Vector3f((float) (player.getX() - player.xo), 0, (float) (player.getZ() - player.zo))
                .rotateY((float) Math.toRadians(bodyYaw));
        heading = heading.lengthSquared() < 1e-8f ? new Vector3f(0, 0, 1) : heading.normalize();
        state.planter.apply(pose, at, (float) Math.toRadians(-bodyYaw), SkeletalPlayerRenderer.PLAYER_SCALE * scale / 16,
                seconds, stepping, heading);
    }
}
