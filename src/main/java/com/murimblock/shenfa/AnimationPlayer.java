package com.murimblock.shenfa;

/**
 * Plays one animation: elapsed time, speed, looping, and a callback when a clip that does not loop ends (it then
 * holds its last frame until something else plays). Time moves on {@link #tick} (one game tick, 1/20 s);
 * {@link #pose} samples between two ticks with the partial tick, so motion is smooth at any frame rate.
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
    private Runnable onEnd;

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

    /** Jumps to {@code seconds} into the clip, with no motion between ticks (to film a clip frame by frame). */
    public void seek(float seconds) {
        time = previousTime = seconds;
    }

    /** Moves one tick on, times {@code scale} (the global slow motion factor); a negative speed plays backward. */
    public void tick(float scale) {
        previousTime = time;
        if (animation == null || finished) {
            return;
        }
        time += TICK * speed * scale;
        float duration = animation.clip().duration();
        if (animation.loop()) {
            // Keep the previous time continuous so the frame between the two ticks does not jump.
            if (duration > 0 && time >= duration - EPSILON) {
                time -= duration;
                previousTime -= duration;
            } else if (duration > 0 && time < 0) {
                time += duration;
                previousTime += duration;
            }
        } else if (time < 0) {
            time = 0;
        } else if (time >= duration - EPSILON) {
            time = duration;
            finished = true;
            Runnable end = onEnd;
            onEnd = null;
            if (end != null) {
                end.run();
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
