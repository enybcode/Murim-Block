package com.murimblock.combat;

import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.AttackData;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * One body's place in a fight, ticked the same way on the server (which decides) and on the client (which predicts
 * its own player, so a strike starts on the key press): the move it plays and how far in, the combo it is in, an
 * input waiting its turn (Epic Fight's 8-tick buffer), a stagger, the guard, and a hit stop that freezes the move.
 */
public final class CombatState {
    /** An input waits this many ticks for the move it would interrupt to let it in. */
    public static final int BUFFER = 8;
    /** After a combo strike ends, the next one continues the chain for this many ticks. */
    public static final int COMBO_GRACE = 8;
    /** A guard raised this recently (ticks) parries instead of blocking (Epic Fight: 8). */
    public static final int PARRY_WINDOW = 8;
    /** A new parry window needs the guard down at least this long (ticks): no parry by spamming the key. */
    public static final int PARRY_REST = 10;

    public enum Kind {
        ATTACK, DODGE, REACTION
    }

    /** What the player asks for. */
    public enum Input {
        ATTACK, DODGE
    }

    /** The move playing, or null. */
    public ResourceLocation clip;
    public Animation animation;
    public Kind kind;
    public float time, previousTime;
    /** The combo strike last played (index in the moveset's combo), and until when (game tick) the next continues. */
    public int combo = -1;
    public long comboUntil = Long.MIN_VALUE;
    /** A strike was made in the air since the body left the ground. */
    public boolean airStrike;
    /** An input waiting for its turn, its direction (for a dodge), and when it came (game tick). */
    public Input buffered;
    public int bufferedDirection;
    public long bufferedAt;
    /** The phase whose window is open and what it already hit (entity ids), so each body is hit once per phase. */
    public AttackData.Phase phase;
    public final Set<Integer> hit = new HashSet<>();
    /** A stagger: its kind and the ticks left. */
    public AttackData.Stun stun = AttackData.Stun.NONE;
    public int stunTicks;
    /** The guard: up or not, when it went up and down (game ticks), and how much each blocked blow costs more. */
    public boolean guarding;
    public long guardUp = Long.MIN_VALUE / 2, guardDown = Long.MIN_VALUE / 2;
    public float guardPenalty = 1;
    public long lastGuarded = Long.MIN_VALUE / 2;
    /** Ticks the move stays frozen after a blow lands (both bodies). */
    public int hitStop;
    /** Combat mode (players): strikes on the attack key instead of mining. */
    public boolean mode;

    /** Starts {@code animation} as a move of {@code kind}. */
    public void start(ResourceLocation id, Animation animation, Kind kind) {
        clip = id;
        this.animation = animation;
        this.kind = kind;
        time = previousTime = 0;
        phase = null;
        hit.clear();
    }

    /** Stops the move (it ended, or a blow broke it). */
    public void end() {
        clip = null;
        animation = null;
        kind = null;
        phase = null;
        hit.clear();
    }

    public boolean playing() {
        return animation != null;
    }

    /** One tick: the move goes on unless a hit stop holds it; a stagger wears off. Returns whether the move ended. */
    public boolean tick(long now) {
        if (stunTicks > 0 && --stunTicks == 0) {
            stun = AttackData.Stun.NONE;
        }
        if (hitStop > 0) {
            hitStop--;
            previousTime = time;
            return false;
        }
        if (animation == null) {
            return false;
        }
        previousTime = time;
        time += 0.05f;
        if (time >= animation.clip().duration() - 1e-4f) {
            if (kind == Kind.ATTACK) {
                comboUntil = now + COMBO_GRACE;
            }
            end();
            return true;
        }
        return false;
    }

    /** Whether a new move may start now: nothing plays, or a strike is past its recovery (a combo may go on). */
    public boolean free() {
        if (stunTicks > 0) {
            return false;
        }
        if (animation == null) {
            return true;
        }
        AttackData attack = animation.attack();
        return kind == Kind.ATTACK && attack != null && time >= attack.recovery();
    }

    /** Whether a dodge may start now: as {@link #free}, or during a strike's recovery (dodge cancels it). */
    public boolean canDodge() {
        return free();
    }

    /** Whether the body may not walk: a strike before its recovery, a dodge, a stagger. */
    public boolean movementLocked() {
        if (stunTicks > 0) {
            return true;
        }
        if (animation == null) {
            return false;
        }
        AttackData attack = animation.attack();
        return kind == Kind.DODGE || kind == Kind.REACTION || attack == null || time < attack.recovery();
    }

    /** Whether blows miss the body now (a dodge's window). */
    public boolean invulnerable() {
        return animation != null && animation.attack() != null && animation.attack().invulnerableAt(time);
    }

    /** Whether blows hurt without staggering now (a move's super armor). */
    public boolean superArmor() {
        return animation != null && animation.attack() != null && animation.attack().superArmorAt(time);
    }

    /** Whether a blow landing now would be parried: the guard went up within the window, after a rest. */
    public boolean parrying(long now) {
        return guarding && now - guardUp <= PARRY_WINDOW && guardUp - guardDown >= PARRY_REST;
    }

    /** The combo strike that comes next at {@code now}. */
    public int nextCombo(long now, int size) {
        // A strike started in the recovery of the last goes on with the chain, as one started just after it ends.
        boolean chained = combo >= 0 && (kind == Kind.ATTACK && animation != null || now <= comboUntil);
        return chained ? (combo + 1) % size : 0;
    }

    /** Raises or lowers the guard. */
    public void guard(boolean up, long now) {
        if (up && !guarding) {
            guardUp = now;
        } else if (!up && guarding) {
            guardDown = now;
        }
        guarding = up;
    }

    /** Keeps an input that could not start yet. */
    public void buffer(Input input, int direction, long now) {
        buffered = input;
        bufferedDirection = direction;
        bufferedAt = now;
    }

    /** The buffered input if it is still fresh, taken; else null. */
    public Input takeBuffered(long now) {
        Input input = buffered != null && now - bufferedAt <= BUFFER ? buffered : null;
        buffered = null;
        return input;
    }
}
