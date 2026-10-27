package com.murimblock.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AnimationPlayerTest {
    private static final int ROOT = Biped.ARMATURE.get(Biped.ROOT).index();

    /** The root moves along X from 0 to 10 px over one second. */
    private static Animation slide(boolean loop) {
        Clip clip = new Clip("slide", List.of(new Clip.Channel(ROOT, Clip.Path.TRANSLATION, Clip.Interpolation.LINEAR,
                new float[] {0, 1}, new float[] {0, 0, 0, 10, 0, 0})));
        return new Animation("test/slide", clip, loop, Animation.DEFAULT_TRANSITION, JointMask.all(Biped.ARMATURE));
    }

    private static float x(AnimationPlayer player, float partialTick) {
        return player.pose(partialTick, Biped.ARMATURE.bindPose()).get(ROOT).translation().x();
    }

    @Test
    void partialTicksSampleBetweenTicks() {
        AnimationPlayer player = new AnimationPlayer();
        player.play(slide(false), 1, null);
        player.tick(1);
        player.tick(1);
        // Ticks at 0.05 s and 0.10 s: half way is 0.075 s, 0.75 px.
        assertEquals(1, x(player, 1), 1e-4f);
        assertEquals(0.75f, x(player, 0.5f), 1e-4f);
    }

    @Test
    void speedAndSlowMotionScaleTime() {
        AnimationPlayer player = new AnimationPlayer();
        player.play(slide(false), 2, null);
        player.tick(0.25f);
        assertEquals(0.025f, player.time(), 1e-6f);
    }

    @Test
    void aClipThatEndsCallsBackAndStops() {
        AnimationPlayer player = new AnimationPlayer();
        AtomicInteger ends = new AtomicInteger();
        player.play(slide(false), 1, ends::incrementAndGet);
        for (int i = 0; i < 19; i++) {
            player.tick(1);
        }
        assertTrue(player.playing());
        assertEquals(0, ends.get());
        player.tick(1);
        assertFalse(player.playing());
        assertEquals(1, ends.get());
    }

    @Test
    void aLoopWrapsWithoutJumping() {
        AnimationPlayer player = new AnimationPlayer();
        player.play(slide(true), 1, null);
        for (int i = 0; i < 21; i++) {
            player.tick(1);
        }
        assertTrue(player.playing());
        assertEquals(0.05f, player.time(), 1e-4f);
        assertEquals(0.5f, x(player, 1), 1e-3f);
        // Half way between the ticks at 1.00 s (0 again) and 1.05 s.
        assertEquals(0.25f, x(player, 0.5f), 1e-3f);
    }
}
