package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.mob.CrabManEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.WaterBallEntity;
import net.minecraft.world.effect.MobEffects;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Murim's guard: a perfect parry (guard raised within 0.5 s of the hit) cancels any attack (a hit, an arrow, a water
 * ball, even a skill made to pierce guards) and throws a melee attacker off balance; a normal parry lets half the
 * damage through (and none of a water ball's slowness) but not guard-piercing skills; nothing is parried from behind.
 * Real damage through Epic Fight's pipeline, on a player holding the guard with a sword in combat mode.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParryGameTests {
    private static final float HIT = 6.0F;

    private ParryGameTests() { }

    /** A player facing south (+Z) with a sword, the Murim guard in its slot, and a zombie 1.5 blocks away. */
    private record Scene(ServerPlayer player, PlayerPatch<?> patch, Zombie zombie) { }

    private static Scene scene(GameTestHelper helper, boolean zombieInFront) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        zombie.setNoAi(true);
        zombie.moveTo(player.getX(), player.getY(), player.getZ() + (zombieInFront ? 1.5 : -1.5));
        return new Scene(player, EpicFightBridge.patch(player), zombie);
    }

    /** Raises the guard, as if the key went down {@code ticksAgo} ticks before the hit. */
    private static void raiseGuard(GameTestHelper helper, Scene s, int ticksAgo) {
        helper.assertTrue(s.patch.getSkill(SkillSlots.GUARD).getSkill() == MurimSkills.MURIM_GUARD.get(), "The Murim guard is not in the guard slot");
        helper.assertTrue(s.patch.startSkillHolding(MurimSkills.MURIM_GUARD.get()), "Could not raise the guard");
        s.patch.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, s.player.tickCount - ticksAgo);
    }

    private static Arrow arrowFrom(Scene s) {
        Arrow arrow = new Arrow(s.player.level(), s.zombie.getX(), s.zombie.getEyeY(), s.zombie.getZ(), new ItemStack(Items.ARROW), null);
        arrow.setOwner(s.zombie);
        return arrow;
    }

    private static void end(GameTestHelper helper, Scene s) {
        s.patch.resetHolding();
        s.zombie.discard();
        cleanup(s.player);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "parry_perfect")
    public static void aPerfectParryCancelsTheHitAndStaggersTheAttacker(GameTestHelper helper) {
        Scene s = scene(helper, true);
        raiseGuard(helper, s, 4);
        s.player.hurt(s.player.damageSources().mobAttack(s.zombie), HIT);
        helper.assertTrue(s.player.getHealth() == s.player.getMaxHealth(), "A perfect parry let damage through: " + s.player.getHealth());
        LivingEntityPatch<?> zombiePatch = EpicFightCapabilities.getEntityPatch(s.zombie, LivingEntityPatch.class);
        helper.assertTrue(zombiePatch != null && zombiePatch.getEntityState().hurt(), "The attacker was not thrown off balance");
        end(helper, s);
    }

    @GameTest(template = "foundation_arena", batch = "parry_normal")
    public static void aNormalParryHalvesTheDamage(GameTestHelper helper) {
        Scene s = scene(helper, true);
        raiseGuard(helper, s, MurimGuardSkill.PERFECT_WINDOW_TICKS + 30);
        s.player.hurt(s.player.damageSources().mobAttack(s.zombie), HIT);
        float taken = s.player.getMaxHealth() - s.player.getHealth();
        helper.assertTrue(Math.abs(taken - HIT * MurimGuardSkill.NORMAL_PARRY_DAMAGE) < 0.01F, "A normal parry should take half the hit, took " + taken);
        end(helper, s);
    }

    @GameTest(template = "foundation_arena", batch = "parry_arrow_perfect")
    public static void aPerfectParryStopsAnArrow(GameTestHelper helper) {
        Scene s = scene(helper, true);
        raiseGuard(helper, s, 3);
        Arrow arrow = arrowFrom(s);
        DamageSource source = s.player.damageSources().arrow(arrow, s.zombie);
        s.player.hurt(source, HIT);
        helper.assertTrue(s.player.getHealth() == s.player.getMaxHealth(), "A perfect parry let an arrow through: " + s.player.getHealth());
        arrow.discard();
        end(helper, s);
    }

    @GameTest(template = "foundation_arena", batch = "parry_arrow_normal")
    public static void aNormalParryHalvesAnArrow(GameTestHelper helper) {
        Scene s = scene(helper, true);
        raiseGuard(helper, s, MurimGuardSkill.PERFECT_WINDOW_TICKS + 30);
        Arrow arrow = arrowFrom(s);
        s.player.hurt(s.player.damageSources().arrow(arrow, s.zombie), HIT);
        float taken = s.player.getMaxHealth() - s.player.getHealth();
        helper.assertTrue(Math.abs(taken - HIT * MurimGuardSkill.NORMAL_PARRY_DAMAGE) < 0.01F, "A normal parry should take half an arrow, took " + taken);
        arrow.discard();
        end(helper, s);
    }

    @GameTest(template = "foundation_arena", batch = "parry_behind")
    public static void nothingIsParriedFromBehindOrWithoutGuard(GameTestHelper helper) {
        Scene s = scene(helper, false);
        raiseGuard(helper, s, 2);
        s.player.hurt(s.player.damageSources().mobAttack(s.zombie), HIT);
        float taken = s.player.getMaxHealth() - s.player.getHealth();
        helper.assertTrue(Math.abs(taken - HIT) < 0.01F, "A hit from behind was parried: took " + taken);
        s.patch.resetHolding();
        s.player.setHealth(s.player.getMaxHealth());
        s.player.invulnerableTime = 0;
        s.zombie.moveTo(s.player.getX(), s.player.getY(), s.player.getZ() + 1.5);
        s.player.hurt(s.player.damageSources().mobAttack(s.zombie), HIT);
        taken = s.player.getMaxHealth() - s.player.getHealth();
        helper.assertTrue(Math.abs(taken - HIT) < 0.01F, "A hit was parried without the guard raised: took " + taken);
        end(helper, s);
    }

    /** A crab man 5 blocks in front of the player throws a real big water ball at them. */
    private static WaterBallEntity throwWaterBall(Scene s) {
        CrabManEntity man = MurimEntities.CRAB_MAN.get().create(s.player.level());
        man.moveTo(s.player.getX(), s.player.getY(), s.player.getZ() + 5, 180, 0);
        man.setNoAi(true);
        man.setNoGravity(true);
        s.player.level().addFreshEntity(man);
        WaterBallEntity ball = WaterBallEntity.charging(man, 1.0F, 0);
        s.player.level().addFreshEntity(ball);
        ball.launch(s.player, WaterBallEntity.SPEED, 0, 0);
        return ball;
    }

    private static void waterBall(GameTestHelper helper, int guardRaisedTicksAgo, float expectedDamage, boolean slowed, String what) {
        Scene s = scene(helper, true);
        // No zombie in the ball's way, and the open air above the arena: its walls would stop the ball.
        s.zombie.discard();
        s.player.moveTo(s.player.getX(), s.player.getY() + 6, s.player.getZ(), 0, 0);
        var level = s.player.serverLevel();
        WaterBallEntity[] ball = new WaterBallEntity[1];
        boolean[] hit = {false};
        helper.runAfterDelay(2, () -> {
            raiseGuard(helper, s, guardRaisedTicksAgo);
            ball[0] = throwWaterBall(s);
        });
        for (int t = 3; t <= 28; t++) helper.runAfterDelay(t, () -> {
            // Where the test server does not tick entities (it depends on where the arena lands), the ball is moved on here.
            if (ball[0] != null && !ball[0].isRemoved() && !level.isPositionEntityTicking(ball[0].blockPosition())) ball[0].tick();
            if (ball[0] != null && !ball[0].isRemoved() && ball[0].state() == WaterBallEntity.SPLASH_ENTITY) hit[0] = true;
        });
        helper.runAfterDelay(32, () -> {
            try {
                float taken = s.player.getMaxHealth() - s.player.getHealth();
                helper.assertTrue(hit[0], what + ": the ball never reached the player");
                helper.assertTrue(Math.abs(taken - expectedDamage) < 0.01F, what + ": expected " + expectedDamage + " damage, took " + taken);
                helper.assertTrue(s.player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) == slowed, what + (slowed ? ": not slowed" : ": slowed anyway"));
            } finally {
                if (ball[0] != null) {
                    if (ball[0].getOwner() != null) ball[0].getOwner().discard();
                    ball[0].discard();
                }
            }
            end(helper, s);
        });
    }

    @GameTest(template = "foundation_arena", batch = "parry_water_ball_perfect", timeoutTicks = 80)
    public static void aPerfectParryStopsAWaterBall(GameTestHelper helper) {
        // Raised as the ball leaves the crab man's claw: it lands well within the 0.5 s window.
        waterBall(helper, 0, 0.0F, false, "perfect parry");
    }

    @GameTest(template = "foundation_arena", batch = "parry_water_ball_normal", timeoutTicks = 80)
    public static void aNormalParryHalvesAWaterBallWithoutTheSlowness(GameTestHelper helper) {
        waterBall(helper, MurimGuardSkill.PERFECT_WINDOW_TICKS + 30, 4.0F * MurimGuardSkill.NORMAL_PARRY_DAMAGE, false, "normal parry");
    }

    @GameTest(template = "foundation_arena", batch = "parry_skill")
    public static void aSkillThatPiercesGuardsStillHasItsPerfectParryFrame(GameTestHelper helper) {
        Scene s = scene(helper, true);
        // Perfect parry: stopped, like any attack.
        raiseGuard(helper, s, 3);
        s.player.hurt(EpicFightDamageSources.mobAttack(s.zombie).addRuntimeTag(EpicFightDamageTypeTags.GUARD_PUNCTURE), HIT);
        helper.assertTrue(s.player.getHealth() == s.player.getMaxHealth(), "A perfect parry let a guard-piercing skill through: " + s.player.getHealth());
        // Normal parry: it pierces the guard, all its damage goes through.
        s.patch.resetHolding();
        s.player.invulnerableTime = 0;
        raiseGuard(helper, s, MurimGuardSkill.PERFECT_WINDOW_TICKS + 30);
        s.player.hurt(EpicFightDamageSources.mobAttack(s.zombie).addRuntimeTag(EpicFightDamageTypeTags.GUARD_PUNCTURE), HIT);
        float taken = s.player.getMaxHealth() - s.player.getHealth();
        helper.assertTrue(Math.abs(taken - HIT) < 0.01F, "A guard-piercing skill should go through a normal parry, took " + taken);
        end(helper, s);
    }
}
