package com.murimblock.shenfa;

import java.util.function.Consumer;

/**
 * Plays one animation: elapsed time, speed, looping, and a callback when a clip that does not loop ends (it then
 * holds its last frame until something else plays). Time moves on {@link #tick} (one game tick, 1/20 s);
 * {@link #pose} samples between two ticks with the partial tick, so motion is smooth at any frame rate. Each tick
 * hands the clip's events it passed (forward or backward, across a loop) to the listener, once each.
 */
public final class AnimationPlayer {
    public static final float TICK = 0.05f;
    // Ticks add up 0.05 s in floats: a clip of whole ticks must end on its last tick, not one later.
    private static final float EPSILON = 1e-4f;

    private Animation animation;
    private float time;
    private float previousTime;
    private float speed = 1;
    private boolean finished;
    /** Just started: an event at the very start fires on the first tick. */
    private boolean fresh;
    private Runnable onEnd;
    private Consumer<ClipEvent> listener = event -> {
    };

    /** Who hears the events the clip passes. */
    public void listener(Consumer<ClipEvent> listener) {
        this.listener = listener;
    }

    /** The animation playing or holding its last frame, or null. */
    public Animation animation() {
        return animation;
    }

    /** Whether an animation is on and has not reached its end. */
    public boolean playing() {
        return animation != null && !finished;
    }

    /** Starts {@code animation} from its beginning; {@code onEnd} (may be null) runs when a clip that does not loop ends. */
    public void play(Animation animation, float speed, Runnable onEnd) {
        this.animation = animation;
        this.speed = speed;
        this.onEnd = onEnd;
        finished = false;
        fresh = true;
        time = previousTime = 0;
    }

    public void stop() {
        animation = null;
        onEnd = null;
        finished = false;
    }

    public float speed() {
        return speed;
    }

    public void speed(float speed) {
        this.speed = speed;
    }

    /** Seconds into the clip at the last tick. */
    public float time() {
        return time;
    }

    /** Seconds into the clip at the tick before the last. */
    public float previousTime() {
        return previousTime;
    }

    /** Jumps to {@code seconds} into the clip, with no motion between ticks (to film a clip frame by frame); fires nothing. */
    public void seek(float seconds) {
        time = previousTime = seconds;
        fresh = false;
    }

    /** Moves one tick on, times {@code scale} (the global slow motion factor); a negative speed plays backward. */
    public void tick(float scale) {
        previousTime = time;
        if (animation == null || finished) {
            return;
        }
        Animation playing = animation;
        float from = time;
        float start = fresh ? from - 2 * EPSILON : from;
        fresh = false;
        time += TICK * speed * scale;
        float duration = animation.clip().duration();
        if (animation.loop()) {
            // Keep the previous time continuous so the frame between the two ticks does not jump.
            if (duration > 0 && time >= duration - EPSILON) {
                forward(playing, start, duration);
                time -= duration;
                previousTime -= duration;
                forward(playing, -2 * EPSILON, time);
            } else if (duration > 0 && time < 0) {
                backward(playing, 0, from);
                time += duration;
                previousTime += duration;
                backward(playing, time, duration);
            } else if (time >= from) {
                forward(playing, start, time);
            } else {
                backward(playing, time, from);
            }
        } else if (time < 0) {
            time = 0;
            backward(playing, 0, from);
        } else if (time >= duration - EPSILON) {
            time = duration;
            forward(playing, start, duration);
            finished = true;
            Runnable end = onEnd;
            onEnd = null;
            if (end != null) {
                end.run();
            }
        } else if (time >= from) {
            forward(playing, start, time);
        } else {
            backward(playing, time, from);
        }
    }

    /**
     * Fires the events in (from, to], both ends shifted by {@link #EPSILON}: a tick that ends a hair short of an event
     * still fires it, and the next tick does not fire it again.
     */
    private void forward(Animation playing, float from, float to) {
        for (ClipEvent event : playing.events()) {
            if (event.time() > from + EPSILON && event.time() <= to + EPSILON) {
                listener.accept(event);
            }
        }
    }

    /** Fires the events in [from, to), shifted as {@link #forward} does, latest first. */
    private void backward(Animation playing, float from, float to) {
        for (int i = playing.events().size() - 1; i >= 0; i--) {
            ClipEvent event = playing.events().get(i);
            if (event.time() >= from - EPSILON && event.time() < to - EPSILON) {
                listener.accept(event);
            }
        }
    }

    /** The clip between the last two ticks, written into {@code pose}; {@code pose} is left as it is when nothing plays. */
    public Pose pose(float partialTick, Pose pose) {
        if (animation == null) {
            return pose;
        }
        float at = previousTime + (time - previousTime) * partialTick;
        float duration = animation.clip().duration();
        if (animation.loop() && duration > 0) {
            at = ((at % duration) + duration) % duration;
        }
        return animation.clip().sample(at, pose);
    }
}
