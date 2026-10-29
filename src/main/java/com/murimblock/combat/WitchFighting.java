package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Vector3f;

/**
 * How the witch fights as the Empoisonneuse des Cinq Venins ({@code docs/mobs/empoisonneuse.png}, storyboard
 * {@code docs/mobs/boards/witch.png}), timed on her clips ({@code skeletal/animations/witch}).
 * <ul>
 * <li>From 4 to 10 blocks, a vial every 3 s, thrown on tick 14 after it glows in her hand: venom (poison) when the
 * prey stands or comes, languor (slowness and weakness) when it runs; 15 ticks open after.
 * <li>Within 3 blocks, the scorpion needles (tick 8): three in a fan, 2 damage and poison each hit; parried, she is
 * staggered by the combat's parry; 14 ticks open after.
 * <li>Then, or when crowded, a gliding step back (every 6 s), 3 blocks.
 * <li>Her elixir is vanilla's drinking (below half her health and so on): her clip plays and she does nothing else.
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class WitchFighting extends Goal {
    /** A move: its clip, length (ticks) and the tick it acts. */
    public enum Move {
        THROW("throw", 30, 14),
        NEEDLES("needles", 22, 8),
        GLIDE("glide", 16, 1),
        DRINK("drink", 32, 0);

        final ResourceLocation clip;
        final int length, at;

        Move(String clip, int length, int at) {
            this.clip = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "witch/" + clip);
            this.length = length;
            this.at = at;
        }
    }

    private static final double THROW_FROM = 4, THROW_UNTIL = 10, NEEDLE_REACH = 3, GLIDE_FROM = 3;
    private static final int THROW_REST = 60, NEEDLE_REST = 40, GLIDE_REST = 120, REST = 8;
    private static final float NEEDLE_DAMAGE = 2;
    private static final double WALK = 1.0;
    private static final int VENOM = 0x5fd17a, LANGUOR = 0xb06ae0;

    /** Dev: told of each move a poisoner starts. */
    public static java.util.function.BiConsumer<Witch, Move> probe;

    private final Witch mob;
    private Move move;
    private int time, rest, throwRest = 20, needleRest, glideRest, path;
    private boolean languor, drinking;
    private double lastDistance;
    private Vec3 away;

    WitchFighting(Witch mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** The witch fights as the Empoisonneuse: her goal in place of vanilla's ranged attack. */
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.getEntity().getType() != EntityType.WITCH
                || !(event.getEntity() instanceof Witch witch)) {
            return;
        }
        if (witch.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof WitchFighting)) {
            witch.goalSelector.removeAllGoals(goal -> goal instanceof RangedAttackGoal);
            witch.goalSelector.addGoal(2, new WitchFighting(witch));
        }
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.canAttack(target);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() || move != null;
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        path = 0;
        lastDistance = mob.getTarget() == null ? 0 : mob.distanceTo(mob.getTarget());
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        if (move != null) {
            CombatServer.anim(mob, Animator.Priority.MIDDLE, null, false);
            move = null;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        throwRest--;
        needleRest--;
        glideRest--;
        // Her elixir: vanilla's drinking, drawn by her clip; she does nothing else meanwhile.
        if (mob.isDrinkingPotion()) {
            if (!drinking) {
                drinking = true;
                begin(Move.DRINK);
            }
            hold();
            return;
        }
        drinking = false;
        CombatState state = CombatServer.existing(mob);
        if (state != null && (state.stunTicks > 0 || state.hitStop > 0)) {
            // Her needles parried: staggered, nothing until it passes.
            if (move != null) {
                move = null;
            }
            hold();
            return;
        }
        if (move != null) {
            play(target);
            return;
        }
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30, 30);
        double distance = mob.distanceTo(target);
        // Running from her: the next vial is languor.
        languor = distance > lastDistance + 0.05;
        lastDistance = distance;
        if (--rest <= 0) {
            if (distance <= NEEDLE_REACH + target.getBbWidth() / 2 && needleRest <= 0) {
                begin(Move.NEEDLES);
                return;
            }
            if (distance < GLIDE_FROM && glideRest <= 0) {
                begin(Move.GLIDE);
                return;
            }
            if (distance >= THROW_FROM && distance <= THROW_UNTIL && throwRest <= 0
                    && mob.getSensing().hasLineOfSight(target)) {
                begin(Move.THROW);
                return;
            }
        }
        if (distance > THROW_UNTIL - 2) {
            if (--path <= 0) {
                mob.getNavigation().moveTo(target, WALK);
                path = 10;
            }
        } else {
            mob.getNavigation().stop();
        }
    }

    private void begin(Move next) {
        move = next;
        time = 0;
        mob.getNavigation().stop();
        CombatServer.anim(mob, Animator.Priority.MIDDLE, next.clip, false);
        switch (next) {
            case THROW -> throwRest = THROW_REST;
            case NEEDLES -> needleRest = NEEDLE_REST;
            case GLIDE -> {
                glideRest = GLIDE_REST;
                LivingEntity target = mob.getTarget();
                Vec3 back = target == null ? Vec3.directionFromRotation(0, mob.yBodyRot).reverse()
                        : mob.position().subtract(target.position()).multiply(1, 0, 1);
                away = back.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : back.normalize();
            }
            default -> {
            }
        }
        if (probe != null) {
            probe.accept(mob, next);
        }
    }

    private void play(LivingEntity target) {
        time++;
        if (move == Move.GLIDE) {
            // Gliding back 3 blocks over 10 ticks, facing her prey.
            if (time <= 10) {
                mob.setDeltaMovement(away.x * 0.32, mob.getDeltaMovement().y, away.z * 0.32);
            } else {
                hold();
            }
        } else {
            hold();
        }
        if (target != null && (time <= move.at || move == Move.GLIDE)) {
            face(target);
        }
        if (target != null && time == move.at) {
            switch (move) {
                case THROW -> vial(target);
                case NEEDLES -> needles(target);
                default -> {
                }
            }
        }
        if (time >= move.length) {
            move = null;
            rest = REST;
        }
    }

    /** A splash vial, aimed ahead of the prey as vanilla's witch aims: venom, or languor at one who runs. */
    private void vial(LivingEntity target) {
        Vec3 motion = target.getDeltaMovement();
        double dx = target.getX() + motion.x - mob.getX();
        double dy = target.getEyeY() - 1.1 - mob.getY();
        double dz = target.getZ() + motion.z - mob.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        List<MobEffectInstance> effects = languor
                ? List.of(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1),
                        new MobEffectInstance(MobEffects.WEAKNESS, 120, 0))
                : List.of(new MobEffectInstance(MobEffects.POISON, 100, 1));
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(),
                Optional.of(languor ? LANGUOR : VENOM), effects));
        ThrownPotion potion = new ThrownPotion(mob.level(), mob);
        potion.setItem(stack);
        potion.setXRot(potion.getXRot() + 20);
        potion.shoot(dx, dy + flat * 0.2, dz, 0.75f, 4);
        mob.level().addFreshEntity(potion);
        mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.WITCH_THROW, mob.getSoundSource(),
                1, 0.8f + mob.getRandom().nextFloat() * 0.4f);
    }

    /** Three needles in a fan ahead of her sleeve: whoever stands in it within reach is struck (guard and parry apply). */
    private void needles(LivingEntity target) {
        Vec3 facing = Vec3.directionFromRotation(0, mob.yBodyRot);
        Vec3 from = mob.position().add(0, 1.3, 0);
        if (mob.level() instanceof ServerLevel level) {
            DustParticleOptions green = new DustParticleOptions(new Vector3f(0.37f, 0.82f, 0.48f), 0.8f);
            for (float turn : new float[] {-0.25f, 0, 0.25f}) {
                Vec3 dir = facing.yRot(turn);
                for (int i = 1; i <= 8; i++) {
                    Vec3 at = from.add(dir.scale(i * 0.5));
                    level.sendParticles(green, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                }
            }
        }
        Vec3 toward = target.position().subtract(mob.position()).multiply(1, 0, 1);
        boolean inFan = toward.lengthSqr() < 1e-4 || toward.normalize().dot(facing) > 0.75;
        if (inFan && mob.distanceTo(target) <= NEEDLE_REACH + 0.8 + target.getBbWidth() / 2
                && target.hurt(mob.damageSources().mobAttack(mob), NEEDLE_DAMAGE)) {
            mob.setLastHurtMob(target);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), mob);
        }
    }

    private void face(LivingEntity target) {
        mob.getLookControl().setLookAt(target, 30, 30);
        double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
    }

    private void hold() {
        mob.getNavigation().stop();
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
    }
}
