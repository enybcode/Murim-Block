package com.murimblock.combat;

import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.ShenfaLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Which move an input starts, the same on the server and on the predicting client: the next strike of the combo,
 * the dash strike out of a sprint, the strike in the air (once per jump), the technique if the Qi is there, or a
 * dodge the way the keys point. An input that comes while a move cannot be broken waits in the buffer.
 */
public final class CombatMoves {
    /** The Qi a dodge costs (D6: everything special costs Qi). */
    public static final float DODGE_QI = 6;

    private CombatMoves() {
    }

    /** A move to start. */
    public record Choice(ResourceLocation clip, Animation animation, CombatState.Kind kind, float qi, int combo) {
    }

    /**
     * What {@code input} starts now, or null (it is then buffered when the body is busy).
     *
     * @param direction for a dodge: 0 forward, 1 back, 2 left, 3 right
     * @param qi        the Qi the body holds
     */
    public static Choice decide(CombatState state, CombatState.Input input, int direction, LivingEntity body, long now,
                                double qi) {
        if (input == CombatState.Input.DODGE ? !state.canDodge() : !state.free()) {
            if (state.stunTicks == 0) {
                state.buffer(input, direction, now);
            }
            return null;
        }
        Moveset moves = Moveset.of(body);
        return switch (input) {
            case DODGE -> qi < DODGE_QI ? null
                    : choice(Moveset.DODGES.get(Math.floorMod(direction, 4)), CombatState.Kind.DODGE, DODGE_QI, -1);
            case TECHNIQUE -> qi < moves.techniqueQi() ? null
                    : choice(moves.technique(), CombatState.Kind.TECHNIQUE, moves.techniqueQi(), -1);
            case ATTACK -> {
                if (!body.onGround() && !body.isInWater()) {
                    yield state.airStrike ? null : choice(moves.air(), CombatState.Kind.ATTACK, 0, -1);
                }
                if (body.isSprinting()) {
                    yield choice(moves.dash(), CombatState.Kind.ATTACK, 0, -1);
                }
                int next = state.nextCombo(now, moves.combo().size());
                yield choice(moves.combo().get(next), CombatState.Kind.ATTACK, 0, next);
            }
        };
    }

    private static Choice choice(ResourceLocation clip, CombatState.Kind kind, float qi, int combo) {
        Animation animation = ShenfaLibrary.get(clip);
        return animation == null ? null : new Choice(clip, animation, kind, qi, combo);
    }

    /** Starts {@code choice} on {@code state} (after the Qi is paid). */
    public static void begin(CombatState state, Choice choice, LivingEntity body) {
        state.start(choice.clip(), choice.animation(), choice.kind());
        state.buffered = null;
        if (choice.kind() == CombatState.Kind.ATTACK) {
            state.combo = choice.combo();
            if (!body.onGround()) {
                state.airStrike = true;
            }
        }
    }
}
