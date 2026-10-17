package com.murimblock.mob.majin;

import com.murimblock.combat.ShatteredGround;
import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.SonOfTheDemonEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/**
 * The Majin Fist (Poing du Majin), the Son of the Demon's art (docs/combat/styles/majin-fist.md): Enzo gives its blows one
 * at a time, and each is one {@link Move} in {@link #MOVES}: its animation (ColossusAnimations), when he uses it (how
 * far his foe stands, how often, at what share of his life), and its {@link Beat beats}, what happens at a given time
 * into it (a blast, a leap, a wave of Qi). SonOfTheDemonPatch builds his fighting from the list; {@link #tick} runs the
 * beats of the move he is playing, once each. The tools below ({@link #blast}, {@link #strikeAhead}, {@link #crater},
 * {@link #launch}) are the blows' building blocks; every blow can be parried with the Murim guard.
 */
public final class MajinFist {
    /**
     * A blow of his art. {@code minRange}/{@code maxRange}: how far his foe stands (blocks); {@code cooldown}: ticks before
     * he uses it again; {@code weight}: how much he favours it among those he may use; {@code belowHealth}: only once his
     * life is at most this share of its whole (1: always); {@code beats}: its effects, in order.
     */
    public record Move(String id, Supplier<AnimationAccessor<? extends StaticAnimation>> animation, double minRange, double maxRange,
                       int cooldown, float weight, float belowHealth, List<Beat> beats) {
        public Move {
            beats = List.copyOf(beats);
        }
    }

    /** What happens {@code at} seconds into a move's animation. */
    public record Beat(float at, BiConsumer<SonOfTheDemonEntity, ServerLevel> effect) { }

    /**
     * His blows. For now only a stand-in, the heavy straight right (its own collider hits): the Majin Fist's blows join it,
     * then replace it, as Enzo gives them.
     */
    public static final List<Move> MOVES = List.of(
            new Move("straight_right", () -> ColossusAnimations.PUNCH, 0, 6, 30, 10, 1.0F, List.of()));

    /** A foe struck by one beat is not struck again by the next for this long (ticks), as the Warlord's blows. */
    private static final int BLOW_WINDOW = 12;

    private MajinFist() { }

    /** His fighting, built from {@link #MOVES}: each blow its own series, within its range, its share of life, its cooldown. */
    public static CombatBehaviors.Builder<HumanoidMobPatch<?>> behaviors() {
        CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = CombatBehaviors.builder();
        for (Move move : MOVES) {
            builder.newBehaviorSeries(CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder()
                    .weight(move.weight()).cooldown(move.cooldown()).canBeInterrupted(false).looping(false)
                    .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                            .animationBehavior(move.animation().get())
                            .withinDistance(move.minRange(), move.maxRange())
                            .custom(patch -> patch.getOriginal().getHealth() <= patch.getOriginal().getMaxHealth() * move.belowHealth())));
        }
        return builder;
    }

    /** The move whose animation this is, or null. */
    public static Move moveOf(Object animation) {
        for (Move move : MOVES) if (move.animation().get() == animation) return move;
        return null;
    }

    /** Runs the beats of the move he is playing, each once, at its time (a new play of a move starts its beats again). */
    public static void tick(SonOfTheDemonEntity him, ServerLevel level, State state) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(him, LivingEntityPatch.class);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        Object playing = main == null || main.isEnd() ? null : main.getRealAnimation();
        Move move = playing == null ? null : moveOf(playing);
        float elapsed = main == null ? 0 : main.getElapsedTime();
        if (move != state.move || elapsed < state.elapsed) { state.move = move; state.fired = 0; }
        state.elapsed = elapsed;
        if (move == null) return;
        while (state.fired < move.beats().size() && elapsed >= move.beats().get(state.fired).at()) {
            move.beats().get(state.fired++).effect().accept(him, level);
        }
    }

    /** Which move he is playing and how many of its beats have run: one per Son of the Demon. */
    public static final class State {
        Move move;
        int fired;
        float elapsed;
    }

    // ---------------------------------------------------------------- the blows' building blocks

    /** Whoever this beat may strike: alive, not him, not struck within the blow, not parrying it. */
    private static boolean strikable(SonOfTheDemonEntity him, LivingEntity victim) {
        if (victim == him || !victim.isAlive()) return false;
        boolean struck = victim.getLastHurtByMob() == him && victim.tickCount - victim.getLastHurtByMobTimestamp() <= BLOW_WINDOW;
        return !struck && !MurimGuardSkill.parriedWithin(victim, BLOW_WINDOW);
    }

    /**
     * A blast all round {@code at}: whoever stands within {@code radius} takes {@code share} of his attack (less the
     * farther), thrown back and {@code up}. Returns who was struck.
     */
    public static List<LivingEntity> blast(SonOfTheDemonEntity him, ServerLevel level, Vec3 at, double radius, float share, double up) {
        List<LivingEntity> struck = new ArrayList<>();
        float damage = (float) him.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * share;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius), e -> strikable(him, e))) {
            Vec3 away = victim.position().subtract(at).multiply(1, 0, 1);
            double d = away.length();
            if (d > radius) continue;
            double k = 1 - d / radius * 0.6;
            if (victim.hurt(him.damageSources().mobAttack(him), (float) (damage * k))) {
                Vec3 push = d < 1e-3 ? Vec3.directionFromRotation(0, him.getYRot()) : away.scale(1 / d);
                victim.push(push.x * 1.2 * k, up * k, push.z * 1.2 * k);
                victim.hurtMarked = true;
                struck.add(victim);
            }
        }
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, (int) (radius * 30), radius * 0.6, 0.1, radius * 0.6, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, Math.max(1, (int) radius), radius * 0.4, 0.1, radius * 0.4, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
        return struck;
    }

    /**
     * A strike along where he faces: whoever stands in a band {@code width} wide from his front to {@code reach} blocks
     * takes {@code share} of his attack and is thrown along it. Returns who was struck.
     */
    public static List<LivingEntity> strikeAhead(SonOfTheDemonEntity him, ServerLevel level, double reach, double width, float share, double push) {
        List<LivingEntity> struck = new ArrayList<>();
        Vec3 ahead = Vec3.directionFromRotation(0, him.getYRot()).multiply(1, 0, 1).normalize();
        Vec3 from = him.position();
        float damage = (float) him.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * share;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, him.getBoundingBox().inflate(reach), e -> strikable(him, e))) {
            Vec3 to = victim.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(ahead), across = Math.abs(to.x * ahead.z - to.z * ahead.x);
            if (along < 0 || along > reach || across > width / 2) continue;
            if (victim.hurt(him.damageSources().mobAttack(him), damage)) {
                victim.push(ahead.x * push, 0.4, ahead.z * push);
                victim.hurtMarked = true;
                struck.add(victim);
            }
        }
        return struck;
    }

    /** A crater under {@code at} (the ground breaks and comes back, as all broken ground: ShatteredGround). */
    public static void crater(ServerLevel level, Vec3 at, double radius, int depth) {
        ShatteredGround.crater(level, BlockPos.containing(at.x, at.y - 0.5, at.z), radius, depth);
    }

    /** Throws {@code victim} up into the air (Epic Fight's stun cancels a push: carried by hand, as the Sky Breaker's). */
    public static void launch(LivingEntity victim, double up) {
        victim.setDeltaMovement(victim.getDeltaMovement().x, up, victim.getDeltaMovement().z);
        victim.hurtMarked = true;
    }
}
