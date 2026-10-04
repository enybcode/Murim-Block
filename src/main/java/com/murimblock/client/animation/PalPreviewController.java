package com.murimblock.client.animation;

import com.murimblock.combat.preview.CombatPreviewService;
import com.murimblock.combat.preview.PreviewDefinition;
import com.murimblock.combat.preview.PreviewPlayback;
import com.mojang.logging.LogUtils;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;

/** PAL is confined to this rendering backend; accepted server ages drive every sample. */
final class PalPreviewController extends PlayerAnimationController {
    private final PreviewPlayback playback = new PreviewPlayback();
    private final MirrorModifier mirror = new MirrorModifier();
    private long loadedGeneration = -1;
    private Animation loadedClip;
    private boolean warnedMissingClip;

    PalPreviewController(AbstractClientPlayer player) {
        super(player, (controller, state, setter) -> PlayState.STOP);
        setFirstPersonMode(FirstPersonMode.NONE);
        addModifierLast(mirror);
    }

    @Override
    public boolean isActive() {
        playback.observe(CombatPreviewService.getData(player), player.tickCount);
        return CombatPreviewService.canPreview(player) && playback.isActive(player.tickCount, 0)
                && PlayerAnimResources.hasAnimation(PreviewDefinition.ANIMATION);
    }

    @Override
    public void tick(AnimationData state) {
        // No independent PAL advancement: an off-screen player must keep the same timeline.
        if (!isActive()) {
            stopTriggeredAnimation();
            stop();
            loadedClip = null;
        }
        if (CombatPreviewService.getData(player).playing() && !PlayerAnimResources.hasAnimation(PreviewDefinition.ANIMATION)
                && !warnedMissingClip) {
            LogUtils.getLogger().warn("Missing Murimblock preview clip {}; no preview pose will be applied", PreviewDefinition.ANIMATION);
            warnedMissingClip = true;
        }
    }

    @Override
    public void setupAnim(AnimationData state) {
        if (!isActive()) return;
        Animation clip = PlayerAnimResources.getAnimation(PreviewDefinition.ANIMATION);
        float age = playback.age(player.tickCount, 0);
        if (loadedGeneration != playback.generation() || loadedClip != clip) {
            triggerAnimation(clip, age);
            loadedGeneration = playback.generation();
            loadedClip = clip;
            warnedMissingClip = false;
        }
        mirror.enabled = player.getMainArm() == HumanoidArm.LEFT;
        // Seeking does not trigger/restart the clip; PAL adds this frame's partial tick once.
        tick = 0;
        startAnimFrom = age;
        super.setupAnim(state);
    }
}
