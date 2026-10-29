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
 * the player sits down, breathes (one's own body with the breathing lane, others' on a slow loop), is lifted at a
 * stage breakthrough, and stands up. The clips are {@code skeletal/animations/meditation.gltf}: the seat on the lowest
 * layer, the breakthrough above it, a good breath as an additive layer over both.
 */
public final class LotusBody {
    private static final Animator.Priority SEAT = Animator.Priority.LOWEST;
    private static final Animator.Priority LIFTED = Animator.Priority.LOW;
    private static final Animator.Priority DEEP = Animator.Priority.MIDDLE;
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

    /** A good breath of one's own: a deeper inhale laid over the breath. */
    static void deepBreath() {
        Animation deep = clip("deep");
        Minecraft mc = Minecraft.getInstance();
        if (SkeletalPlayers.enabled() && deep != null && mc.player != null) {
            PlayerAnimators.play(mc.player, DEEP, deep, 1, false);
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || !SkeletalPlayers.enabled()) {
            return;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            boolean own = player == mc.player;
            float lung = own ? BreathingHud.lung() : -1;
            Animation seat = PlayerAnimators.playing(player, SEAT);
            if (ClientCultivation.isMeditating(player)) {
                // Seated once the sit is over: one's own body holds still and breathes with the lane while it plays;
                // otherwise (others, a full gauge) the slow breathing loop.
                Animation lotus = clip(lung >= 0 ? "lotus_still" : "lotus");
                boolean seated = seat != null && seat.id().contains("meditation/lotus");
                if (lotus != null && (seat == null || !ours(seat) || seated && seat != lotus)) {
                    PlayerAnimators.play(player, SEAT, lotus, 1, false);
                }
            }
            Animation breath = clip("breath");
            if (own && breath != null) {
                if (lung >= 0 && ClientCultivation.isMeditating(player)) {
                    PlayerAnimators.holdAdditive(player, breath, lung);
                } else {
                    PlayerAnimators.stopAdditive(player, breath.id());
                }
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
