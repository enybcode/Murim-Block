package com.murimblock.shenfa;

import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * What a clip does in a fight, from the sidecar's {@code "attack"} (after Epic Fight's attack phases): its strikes
 * as phases with their hit windows and colliders, and the windows in which the body cannot be hurt (a dodge) or
 * cannot be staggered (super armor).
 *
 * <pre>
 * "attack": {
 *     "phases": [{"windup": 0.05, "active": [0.1, 0.2], "recovery": 0.3, "hand": "left",
 *                 "colliders": [{"joint": "Hand_L", "center": [0, -5, 0], "size": [5, 5, 5]}],
 *                 "damage": 1.0, "impact": 1.0, "stun": "short", "knockback": 0.3, "kind": "blunt"}],
 *     "invulnerable": [[0.05, 0.3]], "super_armor": [[0.1, 0.2]]}
 * </pre>
 *
 * @param invulnerable windows (seconds, from and to) in which melee blows and projectiles miss the body
 * @param superArmor   windows in which a blow hurts but does not stagger
 */
public record AttackData(List<Phase> phases, List<float[]> invulnerable, List<float[]> superArmor) {
    public AttackData {
        phases = List.copyOf(phases);
        invulnerable = List.copyOf(invulnerable);
        superArmor = List.copyOf(superArmor);
    }

    /** How a blow staggers what it hits (Epic Fight's stun types, ours added). */
    public enum Stun {
        NONE, SHORT, LONG, KNOCKDOWN, LAUNCH
    }

    /**
     * One strike of a clip.
     *
     * @param windup        when the strike starts (seconds); before it the clip only prepares
     * @param activeFrom    the hit window: colliders hit between these times
     * @param recovery      from here the next attack of a combo may start; movement is locked until then
     * @param hand          which hand strikes: {@code left}, {@code right}, {@code both} or {@code none} (a kick)
     * @param damage        times the attacker's attack damage
     * @param impact        how hard the blow staggers (against the target's stun armor) and what a guard pays
     * @param armorNegation the share of the target's armor ignored (0 to 1)
     * @param maxTargets    how many bodies one phase can hit
     * @param knockback     blocks the target is pushed along the blow
     * @param kind          {@code blunt} or {@code cut}: the hit's sparks and sounds
     */
    public record Phase(float windup, float activeFrom, float activeTo, float recovery, String hand,
                        List<Collider> colliders, float damage, float impact, float armorNegation, int maxTargets,
                        Stun stun, float knockback, String kind) {
        public Phase {
            colliders = List.copyOf(colliders);
        }

        public boolean active(float time) {
            return time >= activeFrom && time <= activeTo;
        }
    }

    /**
     * A box that hits, in a joint's space (model pixels), or in the body's when {@code joint} is null (in front of the
     * body).
     *
     * @param size full width, height and depth (pixels)
     */
    public record Collider(String joint, Vector3fc center, Vector3fc size) {
        public Collider {
            center = new Vector3f(center);
            size = new Vector3f(size);
        }
    }

    public boolean invulnerableAt(float time) {
        return within(invulnerable, time);
    }

    public boolean superArmorAt(float time) {
        return within(superArmor, time);
    }

    /** When the last phase lets the next attack start (seconds), or 0 with no phase. */
    public float recovery() {
        return phases.isEmpty() ? 0 : phases.get(phases.size() - 1).recovery();
    }

    private static boolean within(List<float[]> windows, float time) {
        for (float[] window : windows) {
            if (time >= window[0] && time <= window[1]) {
                return true;
            }
        }
        return false;
    }
}
