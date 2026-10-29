package com.murimblock.shenfa.client;

import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.Joint;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.Pose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Moine du Vide's rosary and blindfold ({@code docs/mobs/boards/moine.png}), which its clips do not key: calm, the
 * rosary floats round his neck and turns; from the tick he tears it off (tick 10 of {@code moine/anger}), the blindfold
 * and the cord are gone and the eleven beads orbit his body, drawn in tight before he steps through the Void, gathered
 * before his palm and flung for the volley, burst outward, and dropped when he dies. Each bead is a joint of its own
 * ({@code Bead_0} to {@code Bead_11} under {@code Rosary}), moved here in the Rosary's space, in model pixels.
 */
final class MoineRosary {
    private static final int BEADS = 12;
    private static final int TEAR = 10;
    /** The Rosary joint's height in the model (the beads' rest places are relative to it). */
    private static final float ROSARY_Y = 36;

    private MoineRosary() {
    }

    static void apply(Mob mob, Animator animator, Armature armature, Pose pose, float partialTick) {
        Joint rosary = armature.find("Rosary");
        if (rosary == null) {
            return;
        }
        float now = mob.tickCount + partialTick;
        Animation move = animator.playing(Animator.Priority.MIDDLE);
        String clip = move == null ? "" : move.id().substring(move.id().indexOf('/') + 1);
        float t = move == null ? -1 : animator.time(Animator.Priority.MIDDLE) * 20;
        boolean dying = mob.deathTime > 0;
        boolean angry = dying || mob.isAggressive() || mob instanceof EnderMan enderman && enderman.isCreepy();
        boolean tearing = clip.equals("anger") && t < TEAR;
        if (!angry || tearing) {
            // Calm: the rosary turns slowly round his neck, the blindfold on.
            pose.set(rosary.index(), JointTransform.of(rosary.bind().translation(),
                    new Quaternionf().rotationY(now * 0.035f), rosary.bind().scale()));
            return;
        }
        hide(armature, pose, "Blindfold");
        hide(armature, pose, "Cord");
        pose.set(rosary.index(), JointTransform.of(rosary.bind().translation(), new Quaternionf(),
                rosary.bind().scale()));
        for (int i = 0; i < BEADS; i++) {
            Joint bead = armature.find("Bead_" + i);
            if (bead == null) {
                continue;
            }
            Vector3f at = orbit(i, now, 13, 11);
            if (dying) {
                // The beads stop and fall to the ground (the model's feet are 36 pixels below the Rosary).
                float fall = Mth.clamp(mob.deathTime / 10f, 0, 1);
                at = new Vector3f(at.x, Mth.lerp(fall * fall, at.y, -ROSARY_Y + 0.5f), at.z);
            } else if (clip.equals("anger")) {
                at = lerp(new Vector3f(bead.bind().translation()), at, (t - TEAR) / 10);
            } else if (clip.equals("step")) {
                Vector3f tight = orbit(i, now * 2, 5, 4);
                at = t < 9 ? lerp(at, tight, t / 6) : lerp(tight, at, (t - 9) / 11);
            } else if (clip.equals("burst")) {
                Vector3f tight = orbit(i, now * 2.5f, 5, 4);
                float wide = t < 10 ? 0 : t < 14 ? (t - 10) / 4 : 1 - (t - 14) / 16;
                at = t < 10 ? lerp(at, tight, t / 6) : orbit(i, now, Mth.lerp(wide, 13, 42), Mth.lerp(wide, 11, 36));
            } else if (clip.equals("volley")) {
                at = volley(i, now, t, at);
            } else if (clip.equals("stagger")) {
                // Dazed, the beads drift low and slow.
                at = orbit(i, now * 0.4f, 14, 12).add(0, -5, 0);
            }
            pose.set(bead.index(), JointTransform.of(at, new Quaternionf(), bead.bind().scale()));
        }
    }

    /** Bead {@code i} on a ring round his waist, {@code rx} by {@code rz} pixels, tilted and bobbing. */
    private static Vector3f orbit(int i, float now, float rx, float rz) {
        double a = i * Math.PI * 2 / BEADS + now * 0.07;
        return new Vector3f((float) (rx * Math.cos(a)), (float) (-7 + 3 * Math.sin(a) + 1.2 * Math.sin(3 * a + now * 0.1)),
                (float) (rz * Math.sin(a)));
    }

    /**
     * The volley: gathered in a ring before his right palm (to tick 13), flung in a line ahead (tick 15 on, about 10
     * pixels a tick), back to the orbit from tick 26.
     */
    private static Vector3f volley(int i, float now, float t, Vector3f orbit) {
        Vector3f palm = new Vector3f(-4, -6, 14);
        double a = i * Math.PI * 2 / BEADS + now * 0.2;
        Vector3f gathered = new Vector3f(palm).add((float) (3.5 * Math.cos(a)), (float) (3.5 * Math.sin(a)), 0);
        if (t < 13) {
            return lerp(orbit, gathered, t / 6);
        }
        if (t < 15) {
            return gathered;
        }
        float flown = (t - 15) * 10 - i * 2.5f;
        Vector3f flung = new Vector3f(palm).add((float) Math.sin(i * 1.7) * 1.5f, (float) Math.cos(i * 2.3) * 1.5f,
                Math.max(0, flown));
        return t < 26 ? flung : lerp(flung, orbit, (t - 26) / 10);
    }

    private static Vector3f lerp(Vector3f from, Vector3f to, float k) {
        float s = Mth.clamp(k, 0, 1);
        s = s * s * (3 - 2 * s);
        return new Vector3f(from).lerp(to, s);
    }

    private static void hide(Armature armature, Pose pose, String name) {
        Joint joint = armature.find(name);
        if (joint != null) {
            JointTransform current = pose.get(joint.index());
            pose.set(joint.index(), JointTransform.of(current.translation(), current.rotation(), new Vector3f(0)));
        }
    }
}
