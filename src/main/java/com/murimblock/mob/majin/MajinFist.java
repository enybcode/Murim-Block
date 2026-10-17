package com.murimblock.mob.majin;

import com.murimblock.combat.ShatteredGround;
import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.SonOfTheDemonEntity;
import com.murimblock.network.SunFlashPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

/**
 * The Majin Fist, the Pride of the Sun (docs/combat/styles/majin-fist.md): the Son of the Demon's art, inspired by
 * Escanor (Enzo, 17/10/2026: "a high-level martial art of 6 skills; use Photon; something with style, for an epic
 * fight"). His blows are {@link Move moves} in {@link #MOVES}: an animation (ColossusAnimations), when he uses it (how far
 * his foe stands, how often, at what share of his life), and its {@link Beat beats}, what happens at a time into it.
 * SonOfTheDemonPatch builds his fighting from the list; {@link #tick} runs the beats of the move he is playing (once
 * each), the things that go on in time ({@link State#later}), his heat, and The One:
 * <ol>
 *   <li><b>Sunfire Fist</b>: the straight right; at its impact a burst of flame before him.</li>
 *   <li><b>Pride Flare</b>: the palm drawn back, glowing, thrust out: a wave of solar flame running along the ground.</li>
 *   <li><b>Cruel Sun</b>: a small sun grown over his raised palm, hurled in an arc: a great burst, a crater.</li>
 *   <li><b>Noon Judgment</b> (from three quarters of his life): pointing at the sky; golden circles under each foe and
 *   about him; then pillars of sunlight strike them.</li>
 *   <li><b>Midday Meteor</b> (from three fifths): a leap into the sky onto his foe, a crater, a ring of fire running out
 *   along the ground (to be jumped).</li>
 *   <li><b>The One</b> (once, at a third of his life): he rises, a great sun born behind him, the sky turned gold;
 *   stronger and quicker, his heat doubled, for 25 s; then the great sun brought down on the middle of the arena.</li>
 * </ol>
 * His heat (<b>Sunshine</b>) burns whoever stands near him; and his strength grows as the fight goes on, like the sun
 * towards noon. Every blow can be parried with the Murim guard; a foe is struck once a blow.
 */
public final class MajinFist {
    /** A blow of his art (see the class): range in blocks, cooldown in ticks, the share of his life it needs, its beats. */
    public record Move(String id, Supplier<AnimationAccessor<? extends StaticAnimation>> animation, double minRange, double maxRange,
                       int cooldown, float weight, float belowHealth, List<Beat> beats) {
        public Move {
            beats = List.copyOf(beats);
        }
    }

    /** What happens {@code at} seconds into a move's animation. */
    public record Beat(float at, BiConsumer<SonOfTheDemonEntity, ServerLevel> effect) { }

    // ---------------------------------------------------------------- the numbers
    /** Sunshine: how near his heat burns (blocks), how much a second, doubled in The One. */
    public static final double HEAT = 5, HEAT_ONE = 8;
    public static final float HEAT_DAMAGE = 1, HEAT_DAMAGE_ONE = 2;
    /** His strength towards noon: a twentieth more every 30 s of the fight, up to three tenths. */
    public static final int NOON_STEP = 600;
    public static final double NOON_GAIN = 0.04, NOON_MAX = 0.2;
    /** The One: at what share of his life; how long he rises (untouchable); how long it lasts; how the great sun falls. */
    public static final float ONE_AT = 0.35F;
    public static final int ONE_RISE = 60, ONE_LENGTH = 560, ONE_FALL = 80;
    public static final double ONE_REACH = 18;
    private static final net.minecraft.resources.ResourceLocation NOON = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("murimblock", "majin_noon"),
            ONE_SPEED = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("murimblock", "the_one_speed"),
            ONE_STRENGTH = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("murimblock", "the_one_strength");
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.8F, 0.25F), 2.0F);
    /** A foe struck by one beat is not struck again by the next for this long (ticks). */
    private static final int BLOW_WINDOW = 12;

    /** His blows. */
    public static final List<Move> MOVES = List.of(
            new Move("sunfire_fist", () -> ColossusAnimations.PUNCH, 0, 6, 24, 10, 1.0F,
                    List.of(new Beat(0.48F, MajinFist::sunfireFist))),
            new Move("pride_flare", () -> ColossusAnimations.FLARE, 5, 18, 180, 7, 1.0F,
                    List.of(new Beat(0.3F, MajinFist::flareGlow), new Beat(0.7F, MajinFist::prideFlare))),
            new Move("cruel_sun", () -> ColossusAnimations.CONJURE, 9, 34, 280, 6, 1.0F,
                    List.of(new Beat(0.35F, MajinFist::conjureSun), new Beat(1.6F, MajinFist::hurlSun))),
            new Move("noon_judgment", () -> ColossusAnimations.JUDGMENT, 0, 40, 400, 6, 0.75F,
                    List.of(new Beat(0.6F, MajinFist::markJudgment), new Beat(1.6F, MajinFist::strikeJudgment))),
            new Move("midday_meteor", () -> ColossusAnimations.LEAP, 8, 24, 320, 6, 0.6F,
                    List.of(new Beat(0.7F, MajinFist::meteorLeap))));

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
                            .custom(patch -> patch.getOriginal() instanceof SonOfTheDemonEntity s && fights(s)
                                    && s.getHealth() <= s.getMaxHealth() * move.belowHealth())));
        }
        return builder;
    }

    /** Whether he may strike now: not while he rises into The One, nor while its great sun falls, nor in mid-leap. */
    public static boolean fights(SonOfTheDemonEntity him) {
        int t = him.theOneTicks();
        return (t == 0 || t >= ONE_RISE && t < ONE_LENGTH) && !him.art().leaping();
    }

    /** The move whose animation this is, or null. */
    public static Move moveOf(Object animation) {
        for (Move move : MOVES) if (move.animation().get() == animation) return move;
        return null;
    }

    /** Which move he is playing and how many of its beats have run, what goes on in time, The One: one per Son of the Demon. */
    public static final class State {
        Move move;
        int fired;
        float elapsed;
        private final List<Task> tasks = new ArrayList<>();
        int fighting;
        /** His leap (Midday Meteor): from, to, how long, how far along (-1: on the ground). */
        Vec3 leapFrom, leapTo;
        int leapTicks, leapAt = -1;
        CruelSunEntity sun, greatSun;
        /** The Noon Judgment's circles, between their marking and their striking. */
        List<Vec3> judgment;
        private record Task(int at, Consumer<ServerLevel> effect) { }
        private int now;

        /** Something that happens {@code ticks} from now. */
        public void later(int ticks, Consumer<ServerLevel> effect) {
            tasks.add(new Task(now + Math.max(1, ticks), effect));
        }

        public boolean leaping() {
            return leapAt >= 0;
        }

        /** Lands him at once (tours, tests): no leap left. */
        public void land() {
            leapAt = -1;
        }
    }

    /**
     * His art each tick: the beats of the move he plays, each once; what goes on in time; his leap; his heat; his strength
     * towards noon; The One when his life falls to a third.
     */
    public static void tick(SonOfTheDemonEntity him, ServerLevel level, State state) {
        state.now++;
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(him, LivingEntityPatch.class);
        AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
        Object playing = main == null || main.isEnd() ? null : main.getRealAnimation();
        Move move = playing == null ? null : moveOf(playing);
        float elapsed = main == null ? 0 : main.getElapsedTime();
        if (move != state.move || elapsed < state.elapsed) { state.move = move; state.fired = 0; }
        state.elapsed = elapsed;
        if (move != null) while (state.fired < move.beats().size() && elapsed >= move.beats().get(state.fired).at()) {
            move.beats().get(state.fired++).effect().accept(him, level);
        }
        for (Iterator<State.Task> it = new ArrayList<>(state.tasks).iterator(); it.hasNext(); ) {
            State.Task task = it.next();
            if (task.at() <= state.now) { state.tasks.remove(task); task.effect().accept(level); }
        }
        if (state.leaping()) leapTick(him, level, state);
        sunshine(him, level);
        noon(him, state);
        theOne(him, level, state);
    }

    // ---------------------------------------------------------------- the blows' building blocks

    /** Whoever this beat may strike: alive, not him, not struck within the blow, not parrying it. */
    private static boolean strikable(SonOfTheDemonEntity him, LivingEntity victim) {
        if (victim == him || !victim.isAlive() || victim instanceof SonOfTheDemonEntity) return false;
        boolean struck = victim.getLastHurtByMob() == him && victim.tickCount - victim.getLastHurtByMobTimestamp() <= BLOW_WINDOW;
        return !struck && !MurimGuardSkill.parriedWithin(victim, BLOW_WINDOW);
    }

    private static float attack(SonOfTheDemonEntity him) {
        return (float) him.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /**
     * A blast all round {@code at}: whoever stands within {@code radius} takes {@code share} of his attack (less the
     * farther), thrown back and {@code up}. Returns who was struck.
     */
    public static List<LivingEntity> blast(SonOfTheDemonEntity him, ServerLevel level, Vec3 at, double radius, float share, double up) {
        List<LivingEntity> struck = new ArrayList<>();
        float damage = attack(him) * share;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius), e -> strikable(him, e))) {
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
     * A strike along where he faces: whoever stands in a band {@code width} wide from {@code from} to {@code to} blocks
     * before him takes {@code share} of his attack and is thrown along it. Returns who was struck.
     */
    public static List<LivingEntity> strikeAhead(SonOfTheDemonEntity him, ServerLevel level, Vec3 origin, Vec3 ahead, double from, double to, double width, float share, double push) {
        List<LivingEntity> struck = new ArrayList<>();
        float damage = attack(him) * share;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(to + width), e -> strikable(him, e))) {
            Vec3 rel = victim.position().subtract(origin).multiply(1, 0, 1);
            double along = rel.dot(ahead), across = Math.abs(rel.x * ahead.z - rel.z * ahead.x);
            if (along < from || along > to || across > width / 2 || Math.abs(victim.getY() - origin.y) > 4) continue;
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

    /** Throws {@code victim} up into the air (Epic Fight's stun cancels a push: set by hand, as the Sky Breaker's). */
    public static void launch(LivingEntity victim, double up) {
        victim.setDeltaMovement(victim.getDeltaMovement().x, up, victim.getDeltaMovement().z);
        victim.hurtMarked = true;
    }

    /** A flash of sunlight at {@code at} for whoever is near (a Photon light, on their screens). */
    public static void flash(ServerLevel level, Vec3 at, float range, float intensity, int ticks) {
        SunFlashPayload payload = new SunFlashPayload(at.x, at.y, at.z, range, intensity, ticks);
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(at) < 160 * 160) PacketDistributor.sendToPlayer(p, payload);
    }

    /** A ball of sunlight at {@code at}, swelling from {@code from} to {@code to} blocks across in {@code ticks} and fading out. */
    public static void glow(ServerLevel level, Vec3 at, float from, float to, int ticks) {
        CruelSunEntity ball = MurimEntities.CRUEL_SUN.get().create(level);
        if (ball == null) return;
        ball.setPos(at.x, at.y, at.z);
        ball.setSize(from);
        ball.burst(to, ticks);
        level.addFreshEntity(ball);
    }

    private static void burn(List<LivingEntity> struck, int ticks) {
        for (LivingEntity e : struck) e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), ticks));
    }

    private static Vec3 ahead(SonOfTheDemonEntity him) {
        return Vec3.directionFromRotation(0, him.getYRot()).multiply(1, 0, 1).normalize();
    }

    /** Where his foe stands, or before him if he has none. */
    private static Vec3 foe(SonOfTheDemonEntity him, double orAhead) {
        LivingEntity t = him.getTarget();
        return t != null && t.isAlive() ? t.position() : him.position().add(ahead(him).scale(orAhead));
    }

    // ---------------------------------------------------------------- 1. Sunfire Fist

    private static void sunfireFist(SonOfTheDemonEntity him, ServerLevel level) {
        Vec3 at = him.position().add(ahead(him).scale(4.8));
        burn(blast(him, level, at, 2.6, 0.35F, 0.35), 60);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.5, at.z, 60, 1.0, 1.0, 1.0, 0.12);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 1, at.z, 8, 0.6, 0.4, 0.6, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5F, 0.5F);
        glow(level, at.add(0, 2.2, 0), 1.5F, 7F, 10);
        flash(level, at.add(0, 2, 0), 9, 3, 6);
    }

    // ---------------------------------------------------------------- 2. Pride Flare

    private static Vec3 palm(SonOfTheDemonEntity him) {
        return him.position().add(ahead(him).scale(3.6)).add(0, 5.6, 0);
    }

    private static void flareGlow(SonOfTheDemonEntity him, ServerLevel level) {
        Vec3 p = palm(him).subtract(ahead(him).scale(4.2));
        level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 40, 0.4, 0.4, 0.4, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 12, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, p.x, p.y, p.z, SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 3, 0.5F);
        glow(level, p, 0.5F, 3F, 8);
        flash(level, p, 8, 3, 9);
    }

    /** The flare's wave: twelve steps along the ground, a step and a half a tick, four blocks wide. */
    private static void prideFlare(SonOfTheDemonEntity him, ServerLevel level) {
        Vec3 origin = him.position(), ahead = ahead(him);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 3, 0.4F);
        flash(level, palm(him), 14, 5, 8);
        State state = him.art();
        for (int i = 0; i < 12; i++) {
            final double d = 3 + 1.5 * i;
            state.later(i + 1, l -> {
                if (!him.isAlive()) return;
                burn(strikeAhead(him, l, origin, ahead, d - 1.6, d, 4.5, 0.5F, 0.8), 80);
                Vec3 at = origin.add(ahead.scale(d));
                l.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.8, at.z, 80, 1.8, 1.0, 1.8, 0.09);
                // Balls of sunlight bursting all along it.
                if ((int) (d * 2) % 3 == 0) glow(l, at.add(0, 1.2, 0), 1.2F, 5F, 10);
                l.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 3, 1.2, 0.1, 1.2, 0);
                l.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y + 1.5, at.z, 20, 1.6, 1.2, 1.6, 0.02);
                if (((int) d) % 3 == 0) l.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.7F);
            });
        }
    }

    // ---------------------------------------------------------------- 3. Cruel Sun

    private static void conjureSun(SonOfTheDemonEntity him, ServerLevel level) {
        CruelSunEntity sun = MurimEntities.CRUEL_SUN.get().create(level);
        if (sun == null) return;
        sun.setSize(0.3F);
        sun.hold(him, 11.5, 0, 4.0F, 24);
        level.addFreshEntity(sun);
        him.art().sun = sun;
        level.playSound(null, him.getX(), him.getY() + 9, him.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3, 0.6F);
        level.playSound(null, him.getX(), him.getY() + 9, him.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2, 0.4F);
    }

    private static void hurlSun(SonOfTheDemonEntity him, ServerLevel level) {
        CruelSunEntity sun = him.art().sun;
        him.art().sun = null;
        if (sun == null || !sun.isAlive()) return;
        Vec3 target = foe(him, 16);
        sun.hurl(target, 8, 22, 5.5, 1.3F);
        level.playSound(null, sun.getX(), sun.getY(), sun.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 3, 0.6F);
    }

    /** A sun bursting where it falls: the blast, the crater, fire, its light. */
    public static void sunBurst(SonOfTheDemonEntity him, ServerLevel level, Vec3 at, double radius, float share, float size) {
        // The ball of fire, swelling and fading.
        CruelSunEntity ball = MurimEntities.CRUEL_SUN.get().create(level);
        if (ball != null) {
            ball.setPos(at.x, at.y + 1, at.z);
            ball.setSize(size);
            ball.burst((float) radius * 3.2F, size > 5 ? 24 : 16);
            level.addFreshEntity(ball);
        }
        burn(blast(him, level, at, radius, share, 0.9), 100);
        crater(level, at, Math.max(2.5, radius * 0.7), size > 5 ? 3 : 2);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 1, at.z, size > 5 ? 4 : 1, radius * 0.3, 0.2, radius * 0.3, 0);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, (int) (radius * 40), radius * 0.5, 1.0, radius * 0.5, 0.25);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 1, at.z, (int) (radius * 4), radius * 0.4, 0.5, radius * 0.4, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 2, at.z, (int) (radius * 10), radius * 0.5, 1.5, radius * 0.5, 0.05);
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            level.sendParticles(ParticleTypes.FLAME, at.x + Math.cos(a) * radius * 0.6, at.y + 0.3, at.z + Math.sin(a) * radius * 0.6, 0, Math.cos(a), 0.05, Math.sin(a), 0.6);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6, 0.4F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 4, 0.5F);
        flash(level, at.add(0, 2, 0), (float) radius * 4, size > 5 ? 10 : 6, size > 5 ? 20 : 12);
    }

    // ---------------------------------------------------------------- 4. Noon Judgment

    /** The circles: one under each foe near, three more about him. */
    private static void markJudgment(SonOfTheDemonEntity him, ServerLevel level) {
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (p.isAlive() && !p.isSpectator() && p.distanceTo(him) < 48) spots.add(p.position());
        for (int i = 0; i < 3; i++) {
            double a = level.random.nextDouble() * Math.PI * 2, d = 6 + level.random.nextDouble() * 14;
            spots.add(him.position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
        }
        State state = him.art();
        state.judgment = spots;
        for (int k = 0; k < 20; k += 2) {
            final int when = k;
            state.later(k + 1, l -> {
                for (Vec3 s : spots) {
                    double r = 3.2;
                    for (int i = 0; i < 24; i++) {
                        double a = Math.PI * 2 * i / 24 + when * 0.1;
                        l.sendParticles(GOLD, s.x + Math.cos(a) * r, s.y + 0.15, s.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    }
                    l.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 12 - when * 0.5, s.z, 2, 0.2, 2, 0.2, 0);
                }
            });
        }
        level.playSound(null, him.getX(), him.getY() + 8, him.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4, 0.5F);
    }

    private static void strikeJudgment(SonOfTheDemonEntity him, ServerLevel level) {
        List<Vec3> spots = him.art().judgment;
        if (spots == null) return;
        him.art().judgment = null;
        for (Vec3 s : spots) {
            burn(blast(him, level, s, 3.2, 0.8F, 0.5), 80);
            for (int y = 0; y < 30; y += 2) level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + y, s.z, 4, 0.4, 0.6, 0.4, 0.01);
            // The pillar itself: balls of sunlight stacked up into the sky, a beam, fading.
            for (int y = 0; y <= 26; y += 2) glow(level, s.add(0, y + 0.5, 0), 2.5F, 3.6F + (y == 0 ? 5 : 0), 14);
            level.sendParticles(ParticleTypes.FLASH, s.x, s.y + 1, s.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.FLAME, s.x, s.y + 0.5, s.z, 50, 1.4, 0.3, 1.4, 0.1);
            level.playSound(null, s.x, s.y, s.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3, 0.8F);
            flash(level, s.add(0, 3, 0), 12, 6, 10);
        }
        level.playSound(null, him.getX(), him.getY(), him.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 4, 0.7F);
    }

    // ---------------------------------------------------------------- 5. Midday Meteor

    private static void meteorLeap(SonOfTheDemonEntity him, ServerLevel level) {
        State state = him.art();
        state.leapFrom = him.position();
        state.leapTo = foe(him, 12);
        state.leapTicks = 24;
        state.leapAt = 0;
        him.setNoGravity(true);
        level.playSound(null, him.getX(), him.getY(), him.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3, 0.6F);
        BlockState under = level.getBlockState(him.blockPosition().below());
        if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), him.getX(), him.getY() + 0.2, him.getZ(), 120, 2, 0.2, 2, 0.3);
    }

    private static void leapTick(SonOfTheDemonEntity him, ServerLevel level, State state) {
        state.leapAt++;
        double k = state.leapAt / (double) state.leapTicks;
        Vec3 at = state.leapFrom.lerp(state.leapTo, k).add(0, 16 * 4 * k * (1 - k), 0);
        him.setPos(at.x, at.y, at.z);
        him.setDeltaMovement(Vec3.ZERO);
        him.fallDistance = 0;
        if (state.leapAt == state.leapTicks / 2) play(him, ColossusAnimations.DIVE);
        // Where he will land: a circle of fire, closing.
        if (state.leapAt % 2 == 0) {
            Vec3 to = state.leapTo;
            double r = 6 * (1 - k) + 1.5;
            for (int i = 0; i < 20; i++) {
                double a = Math.PI * 2 * i / 20;
                level.sendParticles(ParticleTypes.FLAME, to.x + Math.cos(a) * r, to.y + 0.2, to.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 3, at.z, 10, 1, 2, 1, 0.05);
        if (state.leapAt < state.leapTicks) return;
        state.leapAt = -1;
        him.setNoGravity(false);
        play(him, ColossusAnimations.LAND);
        Vec3 to = state.leapTo;
        burn(blast(him, level, to, 4, 0.9F, 0.8), 80);
        crater(level, to, 3.5, 2);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, to.x, to.y + 0.5, to.z, 2, 1, 0.2, 1, 0);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 6, 0.45F);
        flash(level, to.add(0, 2, 0), 18, 7, 14);
        // The ring of fire running out along the ground: only who stands on the ground as it passes is struck.
        for (int i = 0; i < 10; i++) {
            final double r = 3 + i * 1.2;
            state.later(i + 1, l -> {
                if (!him.isAlive()) return;
                for (LivingEntity v : l.getEntitiesOfClass(LivingEntity.class, new AABB(to, to).inflate(r + 1, 3, r + 1), e -> strikable(him, e) && e.onGround())) {
                    double d = Math.sqrt(v.distanceToSqr(to.x, v.getY(), to.z));
                    if (d < r - 1.2 || d > r + 0.6) continue;
                    if (v.hurt(him.damageSources().mobAttack(him), attack(him) * 0.6F)) {
                        Vec3 away = v.position().subtract(to).multiply(1, 0, 1).normalize();
                        v.push(away.x * 0.9, 0.6, away.z * 0.9);
                        v.hurtMarked = true;
                        v.setRemainingFireTicks(60);
                    }
                }
                for (int n = 0; n < 36; n++) {
                    double a = Math.PI * 2 * n / 36;
                    l.sendParticles(ParticleTypes.FLAME, to.x + Math.cos(a) * r, to.y + 0.3, to.z + Math.sin(a) * r, 2, 0.1, 0.3, 0.1, 0.02);
                }
                int balls = (int) Math.max(6, r * 1.5);
                for (int n = 0; n < balls; n++) {
                    double a = Math.PI * 2 * n / balls;
                    glow(l, to.add(Math.cos(a) * r, 0.8, Math.sin(a) * r), 1F, 2.6F, 8);
                }
            });
        }
    }

    private static void play(LivingEntity e, AnimationAccessor<? extends StaticAnimation> a) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(e, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(a, 0.0F);
    }

    // ---------------------------------------------------------------- Sunshine, noon

    /** His heat: whoever stands near burns, every second. */
    private static void sunshine(SonOfTheDemonEntity him, ServerLevel level) {
        boolean one = him.theOneTicks() > 0;
        double reach = one ? HEAT_ONE : HEAT;
        if (him.tickCount % 4 == 0)
            level.sendParticles(ParticleTypes.SMALL_FLAME, him.getX(), him.getY() + 1, him.getZ(), one ? 12 : 4, reach * 0.5, 0.6, reach * 0.5, 0.01);
        if (him.tickCount % 20 != 0) return;
        for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, him.getBoundingBox().inflate(reach), e -> e != him && e.isAlive() && !(e instanceof SonOfTheDemonEntity))) {
            if (v.distanceTo(him) > reach + 1.5) continue;
            v.hurt(him.damageSources().inFire(), one ? HEAT_DAMAGE_ONE : HEAT_DAMAGE);
            v.setRemainingFireTicks(Math.max(v.getRemainingFireTicks(), 40));
        }
    }

    /** His strength growing as the fight goes on, like the sun towards noon. */
    private static void noon(SonOfTheDemonEntity him, State state) {
        state.fighting++;
        if (state.fighting % NOON_STEP != 0) return;
        double gain = Math.min(NOON_MAX, NOON_GAIN * (state.fighting / NOON_STEP));
        var attack = him.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;
        attack.removeModifier(NOON);
        attack.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(NOON, gain,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    // ---------------------------------------------------------------- 6. The One

    /** The One: begun once, at a third of his life; its rise, its time, the great sun brought down at its end. */
    private static void theOne(SonOfTheDemonEntity him, ServerLevel level, State state) {
        int t = him.theOneTicks();
        if (t == 0) {
            if (him.theOneUsed() || him.getHealth() > him.getMaxHealth() * ONE_AT || state.leaping()) return;
            him.beginTheOne();
            play(him, ColossusAnimations.THE_ONE);
            CruelSunEntity sun = MurimEntities.CRUEL_SUN.get().create(level);
            if (sun != null) {
                sun.setSize(0.5F);
                sun.hold(him, 26, 3, 15F, ONE_RISE);
                level.addFreshEntity(sun);
                state.greatSun = sun;
            }
            level.playSound(null, him.getX(), him.getY(), him.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 8, 0.6F);
            level.playSound(null, him.getX(), him.getY(), him.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 6, 0.5F);
            level.playSound(null, him.getX(), him.getY(), him.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8, 0.5F);
            flash(level, him.position().add(0, 12, 0), 60, 12, 30);
            return;
        }
        t = him.advanceTheOne();
        if (t == ONE_RISE) {
            empower(him, true);
            level.sendParticles(ParticleTypes.FLASH, him.getX(), him.getY() + 6, him.getZ(), 3, 0, 0, 0, 0);
        }
        if (t > ONE_RISE && t < ONE_LENGTH && t % 6 == 0)
            level.sendParticles(GOLD, him.getX(), him.getY() + 4, him.getZ(), 20, 2.5, 3, 2.5, 0.05);
        if (t == ONE_LENGTH) {
            // The great sun brought down on the middle of the arena (or on him, with none): flee to its rim, or parry.
            play(him, ColossusAnimations.CONJURE);
            CruelSunEntity sun = state.greatSun;
            BlockPos arena = him.arena();
            Vec3 middle = arena != null ? Vec3.atBottomCenterOf(arena) : him.position();
            if (sun != null && sun.isAlive()) {
                sun.setSize(17F);
                sun.hurl(middle, 6, ONE_FALL, ONE_REACH, 1.6F);
            }
            level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 10, 0.4F);
            for (int k = 0; k < ONE_FALL; k += 4) {
                final int when = k;
                state.later(k + 1, l -> {
                    double r = ONE_REACH;
                    for (int i = 0; i < 64; i++) {
                        double a = Math.PI * 2 * i / 64 + when * 0.05;
                        l.sendParticles(GOLD, middle.x + Math.cos(a) * r, middle.y + 0.2, middle.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    }
                    if (when % 20 == 0) l.playSound(null, middle.x, middle.y, middle.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 8, 0.5F);
                });
            }
        }
        if (t >= ONE_LENGTH + ONE_FALL + 10) {
            empower(him, false);
            him.endTheOne();
            state.greatSun = null;
        }
    }

    /** The One's strength: a third quicker, a quarter stronger. */
    private static void empower(SonOfTheDemonEntity him, boolean on) {
        var speed = him.getAttribute(Attributes.MOVEMENT_SPEED);
        var attack = him.getAttribute(Attributes.ATTACK_DAMAGE);
        if (speed != null) {
            speed.removeModifier(ONE_SPEED);
            if (on) speed.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ONE_SPEED, 0.3,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (attack != null) {
            attack.removeModifier(ONE_STRENGTH);
            if (on) attack.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ONE_STRENGTH, 0.2,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
