package com.murimblock.client;

import com.murimblock.martial.DevouringDemon;
import com.murimblock.martial.KaguneState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The kagune's strikes on every client (docs/combat/styles/devouring-demon-art.md): the server says which strike and since
 * when (KaguneState), and the chains are pointed where the player looks, over their idle (KaguneModel.aim), with a whip's
 * wave running down them. Their timing is the server's: they reach the foe on the tick the blow lands (DevouringDemon).
 * <ul>
 *   <li><b>Lash</b>: the four tentacles rise behind his shoulders, coiled short, then lunge one after the other, right to
 *   left, each to its point of the arc in front of him, the upper two arching over his head, the lower two round his sides,
 *   never through him; each stretches as long as it needs to land on its point.</li>
 *   <li><b>Centipede Bite</b>: a centipede (left and right by turns) rears up like a cobra, coiled short in an S, then
 *   shoots at the foe: stretching to reach one farther than its length, arching over onto a nearer one.</li>
 * </ul>
 * On the blow each is held still a moment (the hit-stop: KaguneImpactFx). The body barely moves: the kagune strikes.
 * Directions are in the player's model space: forward -z, up +y, his right +x (checked in game: KaguneTour, MURIM_KAGUNE_FILM=art).
 */
public final class KaguneStrikes {
    private static final String[] TENTACLES = {"upper_right_0", "lower_right_0", "upper_left_0", "lower_left_0"};
    private static final float LASH_SECONDS = 0.75F, BITE_SECONDS = 0.85F, THORNS_SECONDS = 1.0F, BURROW_SECONDS = 1.1F;
    /** Impaling Thorns: where each tentacle drives into the ground in front of him (degrees to his right): the upper two,
     *  coming over his head, wide of his face; the lower two, coming round his sides, nearer the middle. */
    private static final float[] THORNS_YAWS = {50, 24, -50, -24};
    /** Lash: which turn each tentacle lunges (right to left), the point of the arc each goes for (degrees to his right), when
     *  the first lunges, the gap between them and how long a lunge takes (s): the four land round the blow's tick (0.3 s). */
    private static final int[] LASH_ORDER = {0, 1, 2, 3};
    private static final float[] LASH_YAWS = {52, 18, -18, -52};
    private static final float LASH_FIRST = 0.15F, LASH_GAP = 0.045F, LASH_LUNGE = 0.08F;
    /** How far (radians) a lunging tentacle bows out of the straight line, over his head and round his sides: enough to clear him. */
    private static final float LASH_BOW_OVER = 3.05F, LASH_BOW_SIDE = 2.45F;
    /** Impaling Thorns' dives go to the ground just ahead: a shallower bow, or they loop. */
    private static final float THORNS_BOW_OVER = 2.2F, THORNS_BOW_SIDE = 1.8F;
    /** A tentacle joint's length (blocks): 3.4 pixels. */
    private static final float TENTACLE_SEGMENT = 3.4F / 16;

    private static final boolean CHECKING = Boolean.getBoolean("murimblock.weaponTour");
    private static int inBody, biteInBody;

    private KaguneStrikes() { }

    /** Development tour: the most tentacle joints seen inside the player's body in a Lash so far (0: never through him). */
    public static int jointsSeenInBody() {
        return inBody;
    }

    /** Development tour: the same for Centipede Bite, on all fours. */
    public static int biteJointsSeenInBody() {
        return biteInBody;
    }

    /** Points the chains for the strike under way, if any (the model is cleared of the last player's first). */
    static void apply(KaguneModel model, Player player, KaguneState state, int from, String animation, float time,
                      float[] bend, Matrix4f base, float partial) {
        model.clearAims();
        if (state.strike() == KaguneState.STRIKE_NONE) return;
        float t = held(player, state, (player.level().getGameTime() - state.strikeSince() + partial) / 20F);
        if (t < 0) return;
        // Where he looks, from his body: his head's turn and its pitch.
        float yaw = Mth.wrapDegrees(Mth.lerp(partial, player.yHeadRotO, player.yHeadRot) - Mth.lerp(partial, player.yBodyRotO, player.yBodyRot));
        float pitch = player.getViewXRot(partial);
        Vector3f up = new Vector3f(0, 1, 0);
        if (state.strike() == KaguneState.STRIKE_LASH && t < LASH_SECONDS && model.bone("kagune") == from) {
            // Enzo, 18/10/2026: the kagune stretches out and strikes, never through him. The four rise behind his
            // shoulders, coiled short like scorpions' tails, then lunge one after the other, right to left, each to its
            // own point of the arc in front of him: the upper two arch high over his shoulders and head, the lower two
            // swing wide round his sides, and come down on it; they hold, then draw back in.
            Vector3f root = base.transformPosition(new Vector3f(0, 14, 2));
            for (int i = 0; i < TENTACLES.length; i++) {
                int order = LASH_ORDER[i];
                int bone = model.bone(TENTACLES[i]), joints = model.chainLength(bone);
                // Back the way it came: drawn in to its raised place first, then down to its idle.
                float weight = envelope(t, 0.1F, 0.6F, LASH_SECONDS);
                float side = TENTACLES[i].contains("right") ? 1 : -1;
                boolean lower = TENTACLES[i].startsWith("lower");
                // Raised behind him, leaning back (not over his head), the lower two out behind his hips.
                Vector3f raised = lower ? direction(side * 125, -30) : direction(side * 160, -58);
                float start = LASH_FIRST + order * LASH_GAP;
                float k = ease(Mth.clamp((t - start) / LASH_LUNGE, 0, 1)) * (1 - ease(Mth.clamp((t - 0.45F) / 0.15F, 0, 1)));
                Vector3f target = direction(yaw + LASH_YAWS[order], 0).mul((float) DevouringDemon.LASH_REACH - 0.6F).add(0, lower ? 0.7F : 1.0F, 0);
                bowTo(model, bone, raised, root, target, lower ? new Vector3f(side, 0, 0) : new Vector3f(0, 1, 0), lower ? LASH_BOW_SIDE : LASH_BOW_OVER,
                        k, t, lower ? 18 : 10, weight, TENTACLE_SEGMENT, from, animation, time, bend, base);
                // The development tour checks that no tentacle goes through him (past its first joints, on his back).
                if (CHECKING) inBody = Math.max(inBody, model.jointsInside(from, bone, animation, time, bend[0], bend[1], base,
                        -0.38F, 0, -0.16F, 0.38F, 1.95F, 0.16F, 3));
            }
        } else if (state.strike() == KaguneState.STRIKE_THORNS && t < THORNS_SECONDS && model.bone("kagune") == from) {
            // The four rise behind him, then drive into the ground in front of him, round him (over his head, round his
            // sides), one after the other; they stay in it while the thorns burst out under the foe (KaguneEruptions),
            // then come back out the way they went.
            Vector3f root = base.transformPosition(new Vector3f(0, 14, 2));
            for (int i = 0; i < TENTACLES.length; i++) {
                int bone = model.bone(TENTACLES[i]);
                float side = TENTACLES[i].contains("right") ? 1 : -1;
                boolean lower = TENTACLES[i].startsWith("lower");
                Vector3f raised = lower ? direction(side * 125, -30) : direction(side * 160, -58);
                float start = 0.12F + LASH_ORDER[i] * 0.03F;
                float k = ease(Mth.clamp((t - start) / 0.1F, 0, 1)) * (1 - ease(Mth.clamp((t - 0.72F) / 0.13F, 0, 1)));
                float weight = envelope(t, 0.1F, 0.85F, THORNS_SECONDS);
                // Into the ground two blocks and a half in front of him, fanned out.
                Vector3f target = direction(yaw + THORNS_YAWS[i], 0).mul(2.5F).add(0, -0.6F, 0);
                bowTo(model, bone, raised, root, target, lower ? new Vector3f(side, 0, 0) : new Vector3f(0, 1, 0), lower ? THORNS_BOW_SIDE : THORNS_BOW_OVER,
                        k, t, lower ? 18 : 10, weight, TENTACLE_SEGMENT, from, animation, time, bend, base);
                if (k > 0.05F) model.dive(bone);
                if (CHECKING) inBody = Math.max(inBody, model.jointsInside(from, bone, animation, time, bend[0], bend[1], base,
                        -0.38F, 0, -0.16F, 0.38F, 1.95F, 0.16F, 3));
            }
        } else if (state.strike() == KaguneState.STRIKE_BURROW && t < BURROW_SECONDS && model.bone("kakuja") == from) {
            // Both centipedes rear up over his back, then arch over his head and dive into the ground ahead; they stay in
            // it while they burst out under the foes (KaguneEruptions), then come back out.
            Vector3f root = base.transformPosition(new Vector3f(0, 12, 2));
            for (String name : new String[]{"centipede_right_0", "centipede_left_0"}) {
                int bone = model.bone(name);
                float side = name.contains("right") ? 1 : -1;
                float k = ease(Mth.clamp((t - 0.2F - (side < 0 ? 0.05F : 0)) / 0.14F, 0, 1)) * (1 - ease(Mth.clamp((t - 0.85F) / 0.15F, 0, 1)));
                float weight = envelope(t, 0.12F, 0.95F, BURROW_SECONDS);
                Vector3f target = direction(yaw + side * 22, 0).mul(2.4F).add(0, -1.2F, 0);
                bowTo(model, bone, direction(side * 15, -74), root, target, new Vector3f(0, 1, 0), LASH_BOW_OVER,
                        k, t, 16, weight, SEGMENT, from, animation, time, bend, base);
                if (k > 0.05F) model.dive(bone);
                if (CHECKING) biteInBody = Math.max(biteInBody, model.jointsInside(from, bone, animation, time, bend[0], bend[1], base,
                        -0.42F, 0, -1.0F, 0.42F, 0.82F, 0.45F, 3));
            }
        } else if (state.strike() == KaguneState.STRIKE_BITE && t < BITE_SECONDS && model.bone("kakuja") == from) {
            String root = (state.strikeSince() & 1) == 0 ? "centipede_right_0" : "centipede_left_0";
            float weight = envelope(t, 0.12F, 0.55F, BITE_SECONDS);
            Vector3f dir;
            float curl, arc = 0, stretch;
            float strike = DevouringDemon.BITE_HIT_TICK / 20F;
            if (t < strike - 0.1F) {
                // Reared up like a cobra, coiled short in an S, high over his back (not over his head and neck).
                dir = direction(yaw, -74);
                curl = 16;
                stretch = Mth.lerp(ease(t / (strike - 0.1F)), 1, 0.7F);
            } else {
                // It shoots out at the foe: farther than its length, it stretches to reach it; nearer, it arches over
                // and comes down on it, its head on the foe (the arc's chord is the distance).
                float k = Mth.clamp((t - (strike - 0.1F)) / 0.1F, 0, 1);
                float aimYaw = yaw, aimPitch = pitch, reach = 1;
                int joints = model.chainLength(model.bone(root));
                float length = joints * SEGMENT, arch = 0;
                var target = biteTarget(player, partial);
                if (target != null) {
                    Vec3 to = target.getBoundingBox().getCenter().subtract(player.getPosition(partial).add(0, 0.6, 0));
                    aimYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-to.x, to.z)) - Mth.lerp(partial, player.yBodyRotO, player.yBodyRot));
                    aimPitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z)));
                    float share = (float) to.length() / length;
                    if (share > 1) reach = Math.min(1.35F, share); else arch = arcFor(share);
                } else reach = 1.2F;
                Vector3f reared = direction(yaw, -74), aimed = direction(aimYaw, aimPitch - (float) Math.toDegrees(arch / 2) * k);
                dir = reared.lerp(aimed, ease(k)).normalize();
                curl = Mth.lerp(k, 16, 2);
                arc = joints > 1 ? (float) Math.toDegrees(arch) / (joints - 1) * k : 0;
                stretch = Mth.lerp(ease(k), 0.7F, reach);
            }
            model.aim(model.bone(root), weight, dir, up, curl, t * 30, arc, stretch, from, animation, time, bend[0], bend[1], base);
            // On all fours his body lies level, from his head (ahead) to his hips.
            if (CHECKING) biteInBody = Math.max(biteInBody, model.jointsInside(from, model.bone(root), animation, time, bend[0], bend[1], base,
                    -0.42F, 0, -1.0F, 0.42F, 0.82F, 0.45F, 3));
        }
    }

    /**
     * A chain lunging from {@code raised} (by {@code k}, 0 to 1) to land on {@code target} (the base's space) along a bow
     * that goes out on the side of {@code away} (over his head, round his side) by {@code bow} radians: never straight
     * through him; stretched as long as it needs to get there. Coiled short (an S of {@code coiledCurl}) while raised.
     */
    private static void bowTo(KaguneModel model, int bone, Vector3f raised, Vector3f root, Vector3f target, Vector3f away, float bow,
                              float k, float t, float coiledCurl, float weight, float segment,
                              int from, String animation, float time, float[] bend, Matrix4f base) {
        int joints = model.chainLength(bone);
        Vector3f chord = new Vector3f(target).sub(root);
        float distance = chord.length();
        chord.normalize();
        away = new Vector3f(away);
        away.sub(new Vector3f(chord).mul(away.dot(chord))).normalize();
        float half = bow / 2;
        Vector3f lunge = new Vector3f(chord).mul(Mth.cos(half)).add(new Vector3f(away).mul(Mth.sin(half))).normalize();
        // Long enough for the bowed chain to land on its point.
        float reach = Mth.clamp(1.08F * distance / (Mth.sin(half) / half) / Math.max(1, joints * segment), 0.9F, 2.0F);
        Vector3f dir = new Vector3f(raised).lerp(lunge, k).normalize();
        // The bow bends toward the chain's "top" in these mirrored frames: the side away from him, turned over.
        Vector3f top = new Vector3f(0, 1, 0).lerp(new Vector3f(away).negate(), k).normalize();
        float coil = ease(Mth.clamp(t / 0.15F, 0, 1));
        float stretch = Mth.lerp(k, Mth.lerp(coil, 1, 0.78F), reach);
        float curl = Mth.lerp(k, coiledCurl * coil, 2);
        float arc = joints > 1 ? (float) Math.toDegrees(bow) / (joints - 1) * k : 0;
        model.aim(bone, weight, dir, top, curl, t * 24, arc, stretch, from, animation, time, bend[0], bend[1], base);
    }

    /** The strike's time, held still a moment on the blow it landed (the hit-stop, KaguneImpactFx). */
    private static float held(Player player, KaguneState state, float t) {
        Long landed = KaguneImpactFx.landed(player.getId());
        if (landed == null || landed < state.strikeSince()) return t;
        float hit = (landed - state.strikeSince()) / 20F, stop = KaguneImpactFx.HIT_STOP_TICKS / 20F;
        if (t <= hit) return t;
        return t < hit + stop ? hit : t - stop;
    }

    /** A centipede joint's length (blocks): 4.5 pixels (scripts/concepts/kagune-preview.cjs). */
    private static final float SEGMENT = 4.5F / 16;

    /** The foe a bite goes for, as the server picks it (DevouringDemon.biteTarget): the nearest he looks at, within reach. */
    @javax.annotation.Nullable
    static LivingEntity biteTarget(Player player, float partial) {
        Vec3 eye = player.getEyePosition(partial), look = player.getViewVector(partial);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(DevouringDemon.BITE_REACH),
                e -> e != player && e.isAlive() && (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof Player))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double d = to.length();
            if (d > DevouringDemon.BITE_REACH + e.getBbWidth() / 2 || d < 1e-3) continue;
            if (Math.toDegrees(Math.acos(Mth.clamp(to.normalize().dot(look), -1, 1))) > DevouringDemon.BITE_CONE + Math.toDegrees(Math.atan2(e.getBbWidth(), d))) continue;
            if (d < bestDistance) { bestDistance = d; best = e; }
        }
        return best;
    }

    /** The angle (radians) an arc must turn through for its chord to be this share of its length (0 when straight). */
    static float arcFor(float chordShare) {
        float r = Mth.clamp(chordShare, 0.3F, 1);
        if (r >= 0.999F) return 0;
        float lo = 0, hi = 5.5F;
        for (int i = 0; i < 30; i++) {
            float mid = (lo + hi) / 2, half = mid / 2;
            if (Mth.sin(half) / half > r) lo = mid; else hi = mid;
        }
        return (lo + hi) / 2;
    }

    /** A direction from his facing: yaw to his right (degrees), pitch down (degrees). */
    static Vector3f direction(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f((float) (Math.sin(y) * Math.cos(p)), (float) -Math.sin(p), (float) (-Math.cos(y) * Math.cos(p)));
    }

    /** 0 to 1 by `in`, held to `hold`, back to 0 at `end`. */
    private static float envelope(float t, float in, float hold, float end) {
        if (t <= 0 || t >= end) return 0;
        if (t < in) return ease(t / in);
        if (t < hold) return 1;
        return 1 - ease((t - hold) / (end - hold));
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }
}
