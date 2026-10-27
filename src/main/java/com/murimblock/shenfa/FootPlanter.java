package com.murimblock.shenfa;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Keeps one body's feet where they land (the biped's two legs). When the clip puts a sole on the ground its place in
 * the world is kept, and the leg ({@link LegIk}) reaches back for it while the body moves on; when the clip lifts the
 * foot, or the leg would have to stretch straight, the foot lets go and eases back into the clip over {@link #FADE}
 * seconds. Only a foot going back under the body plants (a swinging foot skimming the ground does not); a foot
 * plants again only after the clip lifted it, so it never jumps from one spot to another.
 */
public final class FootPlanter {
    public static final Vector3fc SOLE = new Vector3f(0, -6, 0);
    /** The clip's sole this close to the ground (pixels) plants the foot; this far up lifts it. */
    public static final float DOWN = 0.8f, UP = 1.6f;
    /** A planted foot lets go when the leg would be this close to straight (pixels short of its full length). */
    private static final float STRETCH = 0.05f;
    public static final float FADE = 0.12f;
    private static final String[] THIGHS = {"Thigh_L", "Thigh_R"}, SHINS = {"Leg_L", "Leg_R"};

    private final Foot[] feet = {new Foot(), new Foot()};

    private static final class Foot {
        /** Where the sole stands in the world (blocks), or null when the foot is in the air. */
        Vector3f planted;
        /** The last target in model space, kept to ease out of it after letting go. */
        Vector3f target;
        boolean lifted = true;
        /** The clip's sole last frame (model space), to tell a foot pushing back from one swinging forward. */
        Vector3f lastSole;
        float weight;
    }

    /**
     * Plants the feet of {@code pose} (local transforms, changed in place).
     *
     * @param at       where the body stands in the world (blocks)
     * @param yaw      the body's turn about Y (radians), from model space to the world
     * @param toBlocks blocks per model pixel
     * @param seconds  time since the last call
     * @param stepping whether the body walks on the ground (otherwise every foot lets go)
     * @param heading  where the body goes, in model space (unit, level): a foot on the ground goes the other way
     */
    public void apply(Pose pose, Vector3fc at, float yaw, float toBlocks, float seconds, boolean stepping,
                      Vector3fc heading) {
        for (int side = 0; side < 2; side++) {
            Foot foot = feet[side];
            int thigh = Biped.ARMATURE.get(THIGHS[side]).index(), shin = Biped.ARMATURE.get(SHINS[side]).index();
            Vector3f sole = LegIk.sole(Biped.ARMATURE, pose, shin, SOLE);
            // A foot on the ground goes back under the body, a swinging foot comes forward (along the heading).
            float along = foot.lastSole == null ? 0 : new Vector3f(sole).sub(foot.lastSole).dot(heading);
            boolean forward = along > 1e-3f, back = along < -1e-3f;
            foot.lastSole = sole;
            if (!stepping || sole.y > UP || forward && (foot.planted == null || sole.y > DOWN)) {
                // A planted foot stays down while the clip only starts it forward on the ground (the toes), and lets go
                // once it is both off the ground and coming forward.
                foot.planted = null;
                foot.lifted = true;
            } else if (foot.planted == null && foot.lifted && sole.y < DOWN && back) {
                foot.planted = new Vector3f(sole).mul(toBlocks).rotateY(yaw).add(at);
                foot.lifted = false;
            }
            if (foot.planted != null) {
                Vector3f target = new Vector3f(foot.planted).sub(at).rotateY(-yaw).div(toBlocks);
                // A planted sole stands on the ground.
                target.y = 0;
                Vector3f hip = pose.toModelMatrices(Biped.ARMATURE)[thigh].getTranslation(new Vector3f());
                float length = pose.get(shin).translation().length() + SOLE.length();
                if (target.distance(hip) > length - STRETCH) {
                    // Out of reach: the body has gone past the foot; it lets go and waits for the next step.
                    foot.planted = null;
                } else {
                    foot.target = target;
                }
            }
            // A foot plants where it already is, so it holds at once; letting go eases back into the clip.
            foot.weight = foot.planted != null ? 1 : Math.max(0, foot.weight - seconds / FADE);
            if (foot.weight > 0 && foot.target != null) {
                float w = foot.weight * foot.weight * (3 - 2 * foot.weight);
                LegIk.reach(Biped.ARMATURE, pose, thigh, shin, SOLE, foot.target, w);
            } else if (sole.y < 0) {
                // A foot in the air never goes through the ground.
                LegIk.reach(Biped.ARMATURE, pose, thigh, shin, SOLE, new Vector3f(sole.x, 0, sole.z), 1);
            }
        }
        Biped.settleHelpers(pose);
    }
}
