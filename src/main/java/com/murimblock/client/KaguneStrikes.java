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
 *   <li><b>Lash</b>: the four tentacles coil back short behind his right shoulder, then shoot out, reaching half as long
 *   again, and sweep the arc in front of him to his left, whipping; then they draw back in.</li>
 *   <li><b>Centipede Bite</b>: a centipede (left and right by turns) rears up like a cobra, coiled short in an S, then
 *   shoots at the foe: stretching to reach one farther than its length, arching over onto a nearer one.</li>
 * </ul>
 * On the blow each is held still a moment (the hit-stop: KaguneImpactFx). The body barely moves: the kagune strikes.
 * Directions are in the player's model space: forward -z, up +y, his right +x (checked in game: KaguneTour, MURIM_KAGUNE_FILM=art).
 */
final class KaguneStrikes {
    private static final String[] TENTACLES = {"upper_right_0", "lower_right_0", "upper_left_0", "lower_left_0"};
    private static final float LASH_SECONDS = 0.75F, BITE_SECONDS = 0.85F;

    private KaguneStrikes() { }

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
            float hit = DevouringDemon.LASH_HIT_TICK / 20F;
            for (int i = 0; i < TENTACLES.length; i++) {
                float local = t - i * 0.025F;
                float weight = envelope(local, 0.08F, 0.44F, LASH_SECONDS - 0.025F * i);
                // Coiled back behind his right shoulder, short; then they shoot out, reaching long, and sweep the arc
                // in front of him to his left; then they draw back in.
                float sweep = local < 0.14F ? Mth.lerp(ease(local / 0.14F), 70, 115)
                        : Mth.lerp(ease(Mth.clamp((local - 0.14F) / 0.3F, 0, 1)), 115, -70);
                float stretch = local < 0.14F ? Mth.lerp(ease(local / 0.14F), 1, 0.75F)
                        : local < hit + 0.05F ? Mth.lerp(ease((local - 0.14F) / (hit + 0.05F - 0.14F)), 0.75F, 1.45F)
                        : Mth.lerp(ease(Mth.clamp((local - hit - 0.05F) / 0.25F, 0, 1)), 1.45F, 1);
                boolean lower = TENTACLES[i].startsWith("lower");
                Vector3f dir = direction(yaw + sweep, pitch * 0.4F + (lower ? 14 : -4));
                // The whip's crack runs down them as they reach out.
                float curl = 20 * Mth.clamp(1 - Math.abs(local - hit) / 0.2F, 0, 1);
                model.aim(model.bone(TENTACLES[i]), weight, dir, up, curl, local * 28, 0, stretch, from, animation, time, bend[0], bend[1], base);
            }
        } else if (state.strike() == KaguneState.STRIKE_BITE && t < BITE_SECONDS && model.bone("kakuja") == from) {
            String root = (state.strikeSince() & 1) == 0 ? "centipede_right_0" : "centipede_left_0";
            float weight = envelope(t, 0.12F, 0.55F, BITE_SECONDS);
            Vector3f dir;
            float curl, arc = 0, stretch;
            float strike = DevouringDemon.BITE_HIT_TICK / 20F;
            if (t < strike - 0.1F) {
                // Reared up like a cobra, coiled short in an S, its head over his.
                dir = direction(yaw, -55);
                curl = 26;
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
                Vector3f reared = direction(yaw, -55), aimed = direction(aimYaw, aimPitch - (float) Math.toDegrees(arch / 2) * k);
                dir = reared.lerp(aimed, ease(k)).normalize();
                curl = Mth.lerp(k, 26, 2);
                arc = joints > 1 ? (float) Math.toDegrees(arch) / (joints - 1) * k : 0;
                stretch = Mth.lerp(ease(k), 0.7F, reach);
            }
            model.aim(model.bone(root), weight, dir, up, curl, t * 30, arc, stretch, from, animation, time, bend[0], bend[1], base);
        }
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
