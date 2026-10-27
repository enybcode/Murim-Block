package com.murimblock.shenfa;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Keeps one body's feet on the ground where they stand (the biped's two legs, through {@link LegIk}).
 * <ul>
 * <li>The ground: each foot stands on the ground under it (a stair, a slab), not on the body's level; the hips sink
 * (up to {@link #MOST_SINK} pixels) so the lower foot reaches down, a foot's height changes at most at
 * {@link #FOOT_SPEED} (a swinging foot looks ahead for a step up), and a foot does not stay in the ground.</li>
 * <li>Stepping ({@link Mode#STEP}, walking or running): when the clip puts a sole down while it goes back under the
 * body, the sole keeps its place in the world while the body moves on; it lets go when the clip lifts it (off the
 * ground and coming forward) or the leg would be straight, easing back into the clip over {@link #FADE} seconds. Only
 * a foot going back plants, and a foot plants again only after a lift, so it never jumps.</li>
 * <li>Standing ({@link Mode#STAND}): both feet stay where they stand while the body turns; when one has drifted
 * {@link #DRIFT} pixels from where the clip wants it, or the body turned {@link #TWIST} from it, it takes a short
 * step there (lifted on an arc), one foot at a time.</li>
 * </ul>
 */
public final class FootPlanter {
    public static final Vector3fc SOLE = new Vector3f(0, -6, 0);
    /** The clip's sole this close to the ground (pixels) plants the foot; this far up lifts it. */
    public static final float DOWN = 0.8f, UP = 1.6f;
    /** A planted foot lets go when the leg would be this close to straight (pixels short of its full length). */
    private static final float STRETCH = 0.05f;
    public static final float FADE = 0.12f;
    /** How far the hips sink at most (pixels) for a foot on lower ground, and how fast (pixels a second). */
    public static final float MOST_SINK = 9, SINK_SPEED = 50;
    public static final float MOST_SINK_STANDING = 4;
    /** Standing: a foot steps again past this drift (pixels) or this turn of the body (radians). */
    public static final float DRIFT = 2.5f, TWIST = (float) Math.toRadians(35);
    /** A standing step: how long it takes (seconds) and how high the foot lifts (pixels). */
    public static final float STEP_TIME = 0.2f, STEP_LIFT = 2;
    /** How fast the drawn body catches up with a step up or down (pixels a second); a bigger jump is a teleport. */
    public static final float LAG_SPEED = 60, MOST_LAG = 10;
    /** The hips keep at least this height (pixels) above the highest foot. */
    public static final float HIP_ABOVE_FEET = 6;
    /** How fast a foot's height may change (pixels a second), and how far ahead a swinging foot looks for a step up. */
    public static final float FOOT_SPEED = 90, LOOK_AHEAD = 16;
    private static final String[] THIGHS = {"Thigh_L", "Thigh_R"}, SHINS = {"Leg_L", "Leg_R"};

    /** What the feet do. */
    public enum Mode {
        /** In the air, swimming, riding...: the clip alone (planted feet ease out). */
        OFF,
        /** Standing still: feet stay put while the body turns, stepping when they must. */
        STAND,
        /** Walking or running: feet plant as the clip puts them down. */
        STEP
    }

    /** The world's ground. */
    public interface Ground {
        /** The ground's height (blocks) under {@code x, z}, searched around {@code near}; NaN when there is none. */
        float height(float x, float z, float near);
    }

    private final Foot[] feet = {new Foot(), new Foot()};

    /** For checks: the hips' sink and lag, and each foot (left, right): planted, stepping or free, weight, target. */
    public String debug() {
        StringBuilder text = new StringBuilder(String.format("sink %.1f lag %.1f", sink, lag));
        for (Foot foot : feet) {
            text.append(String.format(" | %s w%.2f y%.2f", foot.planted != null ? "P" : foot.stepTime >= 0 ? "S" : "-",
                    foot.weight, foot.drawnY));
        }
        return text.toString();
    }
    /** How far the hips are sunk (pixels, negative down). */
    private float sink;
    /**
     * How far the body is drawn from where it is (pixels): Minecraft lifts a body up a stair or slab at once and drops
     * it down one in a few ticks; drawn, it follows over a moment. Feet planted in the world do not move with it.
     */
    private float lag;
    private float lastY = Float.NaN;

    private static final class Foot {
        /** Where the sole stands in the world (blocks), or null when the foot is in the air. */
        Vector3f planted;
        /** The body's turn when the foot was planted (standing). */
        float plantedYaw;
        /** The last target in the world (blocks), kept to ease out of it after letting go. */
        Vector3f target;
        /** Where the target was in model space when the foot let go: the ease out follows the body there. */
        Vector3f released;
        boolean lifted = true;
        /** The clip's sole last frame (model space), to tell a foot pushing back from one swinging forward. */
        Vector3f lastSole;
        float weight;
        /** A standing step under way: where it started (world) and its time (seconds), or a negative time. */
        Vector3f stepFrom;
        float stepTime = -1;
        /** The height the sole is drawn at in the world (blocks), moved at most {@link #FOOT_SPEED}; NaN at first. */
        float drawnY = Float.NaN;
    }

    /**
     * Places the feet of {@code pose} (local transforms, changed in place).
     *
     * @param at       where the body stands in the world (blocks)
     * @param yaw      the body's turn about Y (radians), from model space to the world
     * @param toBlocks blocks per model pixel
     * @param seconds  time since the last call
     * @param heading  where the body goes, in model space (unit, level): a foot on the ground goes the other way
     * @param ground   the world's ground, or null for a flat ground at the body's level
     */
    public void apply(Pose pose, Vector3fc at, float yaw, float toBlocks, float seconds, Mode mode,
                      Vector3fc heading, Ground ground) {
        int[] thighs = new int[2], shins = new int[2];
        Vector3f[] clip = new Vector3f[2];
        float[] floor = new float[2], over = new float[2];
        for (int side = 0; side < 2; side++) {
            thighs[side] = Biped.ARMATURE.get(THIGHS[side]).index();
            shins[side] = Biped.ARMATURE.get(SHINS[side]).index();
            clip[side] = LegIk.sole(Biped.ARMATURE, pose, shins[side], SOLE);
            floor[side] = mode == Mode.OFF ? 0 : floor(clip[side], at, yaw, toBlocks, ground);
            // The ground a free foot crosses: up onto what is just ahead of it while stepping.
            over[side] = floor[side];
            if (mode == Mode.STEP) {
                for (float distance = 4; distance <= LOOK_AHEAD; distance += 4) {
                    Vector3f ahead = new Vector3f(heading).mul(distance).add(clip[side]);
                    over[side] = Math.max(over[side], floor(ahead, at, yaw, toBlocks, ground));
                }
            }
        }

        float bodyRise = Float.isNaN(lastY) ? 0 : (at.y() - lastY) / toBlocks;
        lastY = at.y();
        // A bigger jump is a teleport; off the ground the drawn body only catches up.
        lag = Math.abs(lag - bodyRise) > MOST_LAG ? 0 : mode == Mode.OFF ? lag : lag - bodyRise;
        float catchUp = LAG_SPEED * seconds;
        lag = lag > 0 ? Math.max(0, lag - catchUp) : Math.min(0, lag + catchUp);

        // The hips sink so the lower foot (planted, or about to stand) reaches its ground.
        float wanted = 0;
        if (mode != Mode.OFF) {
            for (int side = 0; side < 2; side++) {
                Foot foot = feet[side];
                if (foot.planted != null) {
                    wanted = Math.min(wanted, (foot.planted.y - at.y()) / toBlocks);
                } else if (clip[side].y < UP) {
                    wanted = Math.min(wanted, floor[side]);
                }
            }
        }
        // Standing, the hips sink less: on a ledge the lower foot hangs a little rather than the body squatting.
        wanted = Math.max(wanted, mode == Mode.STAND ? -MOST_SINK_STANDING : -MOST_SINK);
        float step = SINK_SPEED * seconds;
        sink = sink < wanted ? Math.min(wanted, sink + step) : Math.max(wanted, sink - step);
        // The hips stay well above the highest foot (planted, or the ground a foot stands on): a body drawn low just after
        // a step up would otherwise leave a foot above its hip, and the leg would fold over.
        float highest = -Float.MAX_VALUE;
        for (int side = 0; side < 2; side++) {
            Foot foot = feet[side];
            if (foot.planted != null) {
                highest = Math.max(highest, (foot.planted.y - at.y()) / toBlocks);
            } else if (mode != Mode.OFF) {
                highest = Math.max(highest, floor[side]);
            }
        }
        if (highest > -Float.MAX_VALUE) {
            Matrix4f[] model = pose.toModelMatrices(Biped.ARMATURE);
            float hip = Math.min(model[thighs[0]].m31(), model[thighs[1]].m31());
            float lowest = highest + HIP_ABOVE_FEET - hip;
            if (sink + lag < lowest) {
                lag = lowest - sink;
            }
        }
        if (sink + lag != 0) {
            int root = Biped.ARMATURE.get(Biped.ROOT).index();
            JointTransform hips = pose.get(root);
            pose.set(root, JointTransform.of(new Vector3f(hips.translation()).add(0, sink + lag, 0), hips.rotation(),
                    hips.scale()));
        }

        for (int side = 0; side < 2; side++) {
            Foot foot = feet[side];
            int thigh = thighs[side], shin = shins[side];
            Vector3f sole = LegIk.sole(Biped.ARMATURE, pose, shin, SOLE);
            float height = clip[side].y;
            float along = foot.lastSole == null ? 0 : new Vector3f(clip[side]).sub(foot.lastSole).dot(heading);
            foot.lastSole = clip[side];
            boolean forward = along > 1e-3f, back = along < -1e-3f;
            Vector3f onFloor = new Vector3f(sole.x, floor[side], sole.z);

            switch (mode) {
                case OFF -> {
                    foot.planted = null;
                    foot.stepTime = -1;
                    foot.lifted = true;
                }
                case STEP -> {
                    foot.stepTime = -1;
                    if (height > UP || forward && (foot.planted == null || height > DOWN)) {
                        // A planted foot stays down while the clip only starts it forward on the ground (the toes),
                        // and lets go once it is both off the ground and coming forward.
                        foot.planted = null;
                        foot.lifted = true;
                    } else if (foot.planted == null && foot.lifted && height < DOWN && back) {
                        Vector3f spot = reachable(pose, thigh, shin, onFloor);
                        if (spot != null) {
                            foot.planted = world(spot, at, yaw, toBlocks);
                            foot.lifted = false;
                            // A foot plants where it is and holds at once; pulled in from where the clip put it, it moves
                            // there instead of jumping.
                            foot.weight = spot.distance(onFloor) > 0.2f ? 0 : 1;
                        }
                    }
                }
                case STAND -> {
                    Foot other = feet[1 - side];
                    if (foot.stepTime >= 0) {
                        foot.stepTime += seconds;
                        if (foot.stepTime >= STEP_TIME) {
                            foot.stepTime = -1;
                            foot.planted = world(onFloor, at, yaw, toBlocks);
                            foot.plantedYaw = yaw;
                            foot.weight = 1;
                        }
                    } else if (foot.planted == null) {
                        if (height < DOWN) {
                            foot.planted = world(onFloor, at, yaw, toBlocks);
                            foot.plantedYaw = yaw;
                            foot.weight = 1;
                            foot.weight = 1;
                        }
                    } else if (other.stepTime < 0) {
                        Vector3f now = model(foot.planted, at, yaw, toBlocks);
                        float drift = (float) Math.hypot(now.x - sole.x, now.z - sole.z);
                        float twist = Math.abs(wrap(yaw - foot.plantedYaw));
                        if (drift > DRIFT || twist > TWIST) {
                            foot.stepFrom = foot.planted;
                            foot.planted = null;
                            foot.stepTime = 0;
                        }
                    }
                }
            }

            // A free foot follows the clip over the ground it crosses: up onto what is just ahead of it, down onto
            // what is under it.
            Vector3f free = new Vector3f(sole);
            if (mode != Mode.OFF) {
                // ...but never above its own hip (a foot over the hip would fold the leg over).
                float hip = pose.toModelMatrices(Biped.ARMATURE)[thigh].m31();
                free.y = Math.min(free.y + over[side], Math.max(sole.y, hip - HIP_ABOVE_FEET));
            }

            Vector3f target = null;
            if (foot.stepTime >= 0) {
                // A standing step: from where the foot stood to where the clip wants it, lifted on an arc.
                float t = Math.min(1, foot.stepTime / STEP_TIME);
                float eased = t * t * (3 - 2 * t);
                target = model(foot.stepFrom, at, yaw, toBlocks).lerp(onFloor, eased);
                target.y += (float) Math.sin(Math.PI * t) * STEP_LIFT;
            } else if (foot.planted != null) {
                target = model(foot.planted, at, yaw, toBlocks);
                Vector3f hip = pose.toModelMatrices(Biped.ARMATURE)[thigh].getTranslation(new Vector3f());
                float length = pose.get(shin).translation().length() + SOLE.length();
                if (mode == Mode.STEP && target.distance(hip) > length - STRETCH) {
                    // Out of reach: the body has gone past the foot; it lets go and waits for the next step.
                    foot.planted = null;
                    target = null;
                }
            }
            if (target != null) {
                foot.target = world(target, at, yaw, toBlocks);
                foot.released = null;
            } else if (foot.released == null && foot.target != null) {
                foot.released = model(foot.target, at, yaw, toBlocks);
            }
            // Holding eases in (a foot pulled in moves to its spot) and letting go eases back into the clip.
            foot.weight = target != null ? Math.min(1, foot.weight + seconds / FADE)
                    : Math.max(0, foot.weight - seconds / FADE);
            Vector3f goal = free;
            if (foot.weight > 0 && foot.target != null) {
                float w = foot.weight * foot.weight * (3 - 2 * foot.weight);
                // Letting go: the spot moves on with the body (a foot left behind would whip forward), its height stays
                // where it was in the world (a body lifted up a stair does not lift it).
                Vector3f held = model(foot.target, at, yaw, toBlocks);
                if (target == null && foot.released != null) {
                    held.x = foot.released.x;
                    held.z = foot.released.z;
                }
                goal = new Vector3f(free).lerp(held, w);
            }
            // Heights change at a limited speed, so a foot never jumps up or down a step.
            // (In the world: a body lifted up a stair at once does not lift its feet with it.)
            float goalY = at.y() + goal.y * toBlocks;
            // Off the ground a foot simply follows its clip (a body falling a block a tick would leave it behind).
            if (mode == Mode.OFF || Float.isNaN(foot.drawnY)
                    || Math.abs(goalY - foot.drawnY) > MOST_LAG * toBlocks * 2) {
                foot.drawnY = goalY;
            } else {
                float rise = FOOT_SPEED * toBlocks * seconds;
                foot.drawnY += Math.max(-rise, Math.min(rise, goalY - foot.drawnY));
            }
            goal.y = (foot.drawnY - at.y()) / toBlocks;
            // A foot does not stay in the ground under where it ends up: it comes up out of it, fast but not in one jump.
            float under = mode == Mode.OFF ? 0 : floor(goal, at, yaw, toBlocks, ground);
            if (goal.y < under) {
                goal.y = Math.min(under, goal.y + 2 * FOOT_SPEED * seconds);
                foot.drawnY = at.y() + goal.y * toBlocks;
            }
            if (goal.distanceSquared(sole) > 1e-6f) {
                LegIk.reach(Biped.ARMATURE, pose, thigh, shin, SOLE, goal, 1);
            }
        }
        Biped.settleHelpers(pose);
    }

    /**
     * Where on the ground at {@code spot}'s height the leg can reach, nearest to {@code spot} (pulled in toward the
     * hip when too far, as on a stair down); null when that ground is out of reach below.
     */
    private static Vector3f reachable(Pose pose, int thigh, int shin, Vector3f spot) {
        Vector3f hip = pose.toModelMatrices(Biped.ARMATURE)[thigh].getTranslation(new Vector3f());
        float length = pose.get(shin).translation().length() + SOLE.length() - 2 * STRETCH;
        float drop = hip.y - spot.y;
        if (drop >= length) {
            return null;
        }
        float most = (float) Math.sqrt(length * length - drop * drop);
        float dx = spot.x - hip.x, dz = spot.z - hip.z, flat = (float) Math.hypot(dx, dz);
        if (flat <= most) {
            return new Vector3f(spot);
        }
        return new Vector3f(hip.x + dx * most / flat, spot.y, hip.z + dz * most / flat);
    }

    /** The ground's height (model pixels) under a sole, 0 (the body's level) when unknown. */
    private static float floor(Vector3f sole, Vector3fc at, float yaw, float toBlocks, Ground ground) {
        if (ground == null) {
            return 0;
        }
        Vector3f where = world(sole, at, yaw, toBlocks);
        float height = ground.height(where.x, where.z, at.y());
        return Float.isNaN(height) ? 0 : (height - at.y()) / toBlocks;
    }

    private static Vector3f world(Vector3f model, Vector3fc at, float yaw, float toBlocks) {
        return new Vector3f(model).mul(toBlocks).rotateY(yaw).add(at);
    }

    private static Vector3f model(Vector3f world, Vector3fc at, float yaw, float toBlocks) {
        return new Vector3f(world).sub(at).rotateY(-yaw).div(toBlocks);
    }

    private static float wrap(float radians) {
        float turn = (float) (2 * Math.PI);
        return (float) (radians - turn * Math.floor((radians + Math.PI) / turn));
    }
}
