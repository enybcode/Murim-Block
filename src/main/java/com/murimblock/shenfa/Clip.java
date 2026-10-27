package com.murimblock.shenfa;

import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An animation: per joint, keyframed channels of translation, rotation and scale, sampled at any time. Values are the
 * joint's whole transform relative to its parent (as glTF stores them), not offsets from the rest; joints without a
 * channel keep the pose they had.
 */
public final class Clip {
    private final String name;
    private final float duration;
    private final List<Channel> channels;

    public Clip(String name, List<Channel> channels) {
        this.name = name;
        this.channels = List.copyOf(channels);
        this.duration = (float) channels.stream().mapToDouble(Channel::end).max().orElse(0);
    }

    public String name() {
        return name;
    }

    /** Seconds, the last keyframe's time. */
    public float duration() {
        return duration;
    }

    public List<Channel> channels() {
        return channels;
    }

    /** Writes the clip at {@code time} seconds (clamped to the clip) into {@code pose}. */
    public Pose sample(float time, Pose pose) {
        float[] values = new float[4];
        for (Channel channel : channels) {
            JointTransform current = pose.get(channel.joint());
            channel.sample(time, values);
            pose.set(channel.joint(), switch (channel.path()) {
                case TRANSLATION -> JointTransform.of(new Vector3f(values[0], values[1], values[2]),
                        current.rotation(), current.scale());
                case ROTATION -> JointTransform.of(current.translation(),
                        new Quaternionf(values[0], values[1], values[2], values[3]), current.scale());
                case SCALE -> JointTransform.of(current.translation(), current.rotation(),
                        new Vector3f(values[0], values[1], values[2]));
            });
        }
        return pose;
    }

    /** The value of one joint's channel at {@code time}, or null when the clip has no such channel. */
    public float[] value(int joint, Path path, float time) {
        for (Channel channel : channels) {
            if (channel.joint() == joint && channel.path() == path) {
                float[] values = new float[4];
                channel.sample(time, values);
                return values;
            }
        }
        return null;
    }

    public enum Path {
        TRANSLATION(3), ROTATION(4), SCALE(3);

        final int size;

        Path(int size) {
            this.size = size;
        }
    }

    public enum Interpolation {
        LINEAR, STEP, CUBICSPLINE
    }

    /**
     * One joint's keyframes for one path. For {@code CUBICSPLINE}, each keyframe holds in-tangent, value and
     * out-tangent, as in glTF; rotations are quaternions (x, y, z, w).
     */
    public record Channel(int joint, Path path, Interpolation interpolation, float[] times, float[] values) {
        public Channel {
            int perKey = path.size * (interpolation == Interpolation.CUBICSPLINE ? 3 : 1);
            if (times.length == 0 || values.length != times.length * perKey) {
                throw new IllegalArgumentException("channel of joint " + joint + ": " + times.length + " keyframes but "
                        + values.length + " values");
            }
            for (int i = 1; i < times.length; i++) {
                if (times[i] < times[i - 1]) {
                    throw new IllegalArgumentException("channel of joint " + joint + ": keyframe times go backwards");
                }
            }
        }

        float end() {
            return times[times.length - 1];
        }

        /** The value at {@code time} into {@code out} (3 or 4 floats). */
        void sample(float time, float[] out) {
            int last = times.length - 1;
            if (time <= times[0] || last == 0) {
                value(0, out);
                return;
            }
            if (time >= times[last]) {
                value(last, out);
                return;
            }
            int k = 0;
            while (times[k + 1] <= time) {
                k++;
            }
            float span = times[k + 1] - times[k];
            float t = span > 0 ? (time - times[k]) / span : 0;
            switch (interpolation) {
                case STEP -> value(k, out);
                case LINEAR -> {
                    float[] next = new float[4];
                    value(k, out);
                    value(k + 1, next);
                    if (path == Path.ROTATION) {
                        Quaternionf a = new Quaternionf(out[0], out[1], out[2], out[3]);
                        a.slerp(new Quaternionf(next[0], next[1], next[2], next[3]), t);
                        store(a, out);
                    } else {
                        for (int i = 0; i < path.size; i++) {
                            out[i] += (next[i] - out[i]) * t;
                        }
                    }
                }
                case CUBICSPLINE -> {
                    // Hermite spline: glTF's tangents are per second, so they are scaled by the span.
                    float t2 = t * t, t3 = t2 * t;
                    float h00 = 2 * t3 - 3 * t2 + 1, h10 = t3 - 2 * t2 + t, h01 = -2 * t3 + 3 * t2, h11 = t3 - t2;
                    int n = path.size, a = k * 3 * n, b = (k + 1) * 3 * n;
                    for (int i = 0; i < n; i++) {
                        out[i] = h00 * values[a + n + i] + h10 * span * values[a + 2 * n + i]
                                + h01 * values[b + n + i] + h11 * span * values[b + i];
                    }
                    if (path == Path.ROTATION) {
                        store(new Quaternionf(out[0], out[1], out[2], out[3]).normalize(), out);
                    }
                }
            }
        }

        private void value(int key, float[] out) {
            int n = path.size;
            int at = interpolation == Interpolation.CUBICSPLINE ? key * 3 * n + n : key * n;
            System.arraycopy(values, at, out, 0, n);
        }

        private static void store(Quaternionf q, float[] out) {
            out[0] = q.x;
            out[1] = q.y;
            out[2] = q.z;
            out[3] = q.w;
        }
    }
}
