package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /** {@link #slide} with events at 0, 0.15, 0.5 and 1 s. */
    private static Animation slideWithEvents(boolean loop) {
        Animation slide = slide(loop);
        List<ClipEvent> events = List.of(event(0), event(0.15f), event(0.5f), event(1));
        return new Animation(slide.id(), slide.clip(), loop, slide.transition(), slide.mask(), Map.of(), false, 0,
                events, null, null);
    }

    private static ClipEvent event(float time) {
        return new ClipEvent(time, "sound", ClipEvent.Side.CLIENT, new JsonObject());
    }

    private static List<Float> run(AnimationPlayer player, int ticks) {
        List<Float> fired = new ArrayList<>();
        player.listener(event -> fired.add(event.time()));
        for (int i = 0; i < ticks; i++) {
            player.tick(1);
        }
        return fired;
    }

    @Test
    void eventsFireOnceEachInOrder() {
        AnimationPlayer player = new AnimationPlayer();
        player.play(slideWithEvents(false), 1, null);
        assertEquals(List.of(0f), run(player, 1));
        assertEquals(List.of(0.15f), run(player, 3));
        assertEquals(List.of(0.5f, 1f), run(player, 30));
    }

    @Test
    void eventsFireAcrossALoopAndBackward() {
        AnimationPlayer player = new AnimationPlayer();
        player.play(slideWithEvents(true), 1, null);
        // Two rounds: the end and the start of the loop are both passed each round.
        assertEquals(List.of(0f, 0.15f, 0.5f, 1f, 0f, 0.15f, 0.5f, 1f, 0f), run(player, 40));
        player.play(slideWithEvents(true), -1, null);
        player.seek(0.6f);
        // Going back past the start fires the start once (the end is the same moment of the loop).
        assertEquals(List.of(0.5f, 0.15f, 0f), run(player, 13));
    }

    @Test
    void slowMotionStillFiresEachOnce() {
        AnimationPlayer player = new AnimationPlayer();
        player.play(slideWithEvents(false), 0.1f, null);
        assertEquals(List.of(0f, 0.15f, 0.5f, 1f), run(player, 200));
    }
}
