package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.client.PlayerAnimators;
import com.murimblock.shenfa.client.SkeletalClips;
import com.murimblock.shenfa.client.SkeletalPlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The lotus meditation on Shenfa (storyboards {@code docs/mobs/boards/meditation.png} and {@code meditation_fp.png}):
 * the player sits down, breathes on a slow loop, is lifted at a stage breakthrough, and stands up. The clips are
 * {@code skeletal/animations/meditation.gltf}: the seat on the lowest layer, the breakthrough above it.
 */
public final class LotusBody {
    private static final Animator.Priority SEAT = Animator.Priority.LOWEST;
    private static final Animator.Priority LIFTED = Animator.Priority.LOW;
    /** Getting up after a hit plays this much faster. */
    private static final float HURT_SPEED = 1.5f;

    private LotusBody() {
    }

    private static Animation clip(String name) {
        return SkeletalClips.get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "meditation/" + name));
    }

    private static boolean ours(Animation animation) {
        return animation != null && animation.id().contains("meditation/");
    }

    /** The server sat {@code player} down or got it up. */
    static void seat(AbstractClientPlayer player, boolean meditating) {
        if (!SkeletalPlayers.enabled()) {
            return;
        }
        Animation sit = clip("sit"), stand = clip("stand");
        if (meditating && sit != null) {
            PlayerAnimators.play(player, SEAT, sit, 1, false);
        } else if (!meditating && stand != null && ours(PlayerAnimators.playing(player, SEAT))) {
            PlayerAnimators.stop(player, LIFTED);
            PlayerAnimators.play(player, SEAT, stand, player.hurtTime > 0 ? HURT_SPEED : 1, false);
        }
    }

    /** A stage breakthrough begins: the Qi lifts the seated body (72 ticks, with StageBreakthrough's spiral). */
    static void breakthrough(AbstractClientPlayer player) {
        Animation lift = clip("breakthrough");
        if (SkeletalPlayers.enabled() && lift != null) {
            PlayerAnimators.play(player, LIFTED, lift, 1, false);
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || !SkeletalPlayers.enabled()) {
            return;
        }
        Animation lotus = clip("lotus");
        if (lotus == null) {
            return;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            // Seated once the sit is over: the slow breathing loop.
            Animation seat = PlayerAnimators.playing(player, SEAT);
            if (ClientCultivation.isMeditating(player) && (seat == null || !ours(seat))) {
                PlayerAnimators.play(player, SEAT, lotus, 1, false);
            }
        }
    }

    /** Whether a meditation clip holds the body: seated, sitting down, getting up or lifted. */
    private static boolean held(AbstractClientPlayer player) {
        return ours(PlayerAnimators.playing(player, SEAT)) || ours(PlayerAnimators.playing(player, LIFTED));
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            PlayerAnimators.bodyHeld = PlayerAnimators.bodyHeld.or(LotusBody::held);
        }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Pre event) {
            tick();
        }
    }
}
