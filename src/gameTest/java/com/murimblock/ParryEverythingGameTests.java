package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.inner.HeartDemonEntity;
import com.murimblock.integration.epicfight.ArmoredCrabEpicFight;
import com.murimblock.integration.epicfight.CrabManEpicFight;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import com.murimblock.mob.MurimEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Everything the mod added that strikes can be parried with Murim's guard, beyond what the Azure Cloud's own survey
 * (AzureParryGameTests), the bandits, the chief and the water ball already check: the Long Fist's techniques at a
 * player, the Heart Demon (sword and fists) and its shadow, Sensei Yun, the crab man's and the armoured crab's claws.
 * Each move is played three times at a player facing the attacker: unguarded (it must hurt: the case tests something),
 * into a perfect parry (nothing goes through; for a flurry, its first hit) and into a held guard (half at most). A table
 * of every case is written to the log ("Parry survey"). Near the world's origin, as Epic Fight's boxes want it.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ParryEverythingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CASE_TICKS = 40;
    private static final int WARM_UP = 70;
    /** Each survey has its own place (they run side by side): a row of them, 32 blocks apart, away from the Azure survey. */
    private static Vec3 origin(int slot) {
        return new Vec3(72.5 + slot * 32, 140, 72.5);
    }

    private ParryEverythingGameTests() { }

    private enum Guard { NONE, PERFECT, NORMAL }

    /** A move, and whether it strikes several times (a perfect parry then stops its first hit only). */
    private record Move(String name, Supplier<AnimationAccessor<? extends StaticAnimation>> animation, boolean flurry) {
        static Move one(String name, Supplier<AnimationAccessor<? extends StaticAnimation>> a) { return new Move(name, a, false); }
        static Move many(String name, Supplier<AnimationAccessor<? extends StaticAnimation>> a) { return new Move(name, a, true); }
    }

    /** Who strikes: made facing the defender, played by its patch, kept in place. */
    private record Attacker(LivingEntity entity, LivingEntityPatch<?> patch, double reach) { }

    // ---------------------------------------------------------------- who strikes

    /** A player of the Long Fist, bare-handed. */
    private static Attacker fist(ServerLevel level, ServerPlayer defender, GameTestHelper helper) {
        ServerPlayer p = player(helper);
        EpicFightCombatDefaults.enforce(p);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return new Attacker(p, EpicFightBridge.patch(p), 1.6);
    }

    /** The defender's Heart Demon (or shadow), its sword in hand or bare-handed. */
    private static BiFunction<ServerLevel, ServerPlayer, Attacker> demon(boolean shadow, boolean sword) {
        return (level, defender) -> {
            HeartDemonEntity demon = MurimEntities.HEART_DEMON.get().create(level);
            demon.setup(defender, shadow);
            demon.setItemSlot(EquipmentSlot.MAINHAND, sword ? new ItemStack(Items.IRON_SWORD) : ItemStack.EMPTY);
            // Bare fists reach no further than a jab: a step closer.
            return mob(level, demon, sword ? 1.6 : 1.0);
        };
    }

    private static Attacker sensei(ServerLevel level, ServerPlayer defender) {
        Mob sensei = MurimEntities.AZURE_SENSEI.get().create(level);
        sensei.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MurimItems.AZURE_CLOUD_SWORD.get()));
        return mob(level, sensei, 1.6);
    }

    private static Attacker crabMan(ServerLevel level, ServerPlayer defender) {
        return mob(level, MurimEntities.CRAB_MAN.get().create(level), 1.6);
    }

    private static Attacker armoredCrab(ServerLevel level, ServerPlayer defender) {
        return mob(level, MurimEntities.ARMORED_CRAB.get().create(level), 1.1);
    }

    private static Attacker mob(ServerLevel level, Mob mob, double reach) {
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return new Attacker(mob, EpicFightCapabilities.getEntityPatch(mob, LivingEntityPatch.class), reach);
    }

    // ---------------------------------------------------------------- the surveys

    @GameTest(template = "foundation_arena", batch = "parry_survey_long_fist", timeoutTicks = (3 * 12 + 2) * CASE_TICKS + 200)
    public static void everyLongFistTechniqueCanBeParried(GameTestHelper helper) {
        survey(helper, 0, "Long Fist", null, List.of(
                Move.one("tiger_step_punch", () -> MurimAnimations.TIGER_STEP_PUNCH),
                Move.one("tiger_leap", () -> MurimAnimations.TIGER_STEP_PUNCH_LONG),
                Move.one("tiger_step_dive", () -> MurimAnimations.TIGER_STEP_DIVE),
                Move.many("rapid_fists", () -> MurimAnimations.RAPID_FISTS),
                Move.many("hundred_fists", () -> MurimAnimations.RAPID_FISTS_STORM),
                Move.many("rapid_fists_air", () -> MurimAnimations.RAPID_FISTS_AIR),
                Move.one("whirlwind_kick", () -> MurimAnimations.WHIRLWIND_KICK),
                Move.one("tornado_kick", () -> MurimAnimations.WHIRLWIND_KICK_GREAT),
                Move.one("whirlwind_descent", () -> MurimAnimations.WHIRLWIND_DESCENT),
                Move.one("inch_force", () -> MurimAnimations.INCH_FORCE),
                Move.one("jab_1", () -> Animations.FIST_AUTO1),
                Move.one("jab_2", () -> Animations.FIST_AUTO2)));
    }

    @GameTest(template = "foundation_arena", batch = "parry_survey_heart_demon", timeoutTicks = (3 * 5 + 2) * CASE_TICKS + 200)
    public static void everyBlowOfTheHeartDemonsSwordCanBeParried(GameTestHelper helper) {
        survey(helper, 1, "Heart Demon (sword)", demon(false, true), List.of(
                Move.one("drifting_cut", () -> MurimAnimations.DRIFTING_CUT),
                Move.one("returning_cut", () -> MurimAnimations.RETURNING_CUT),
                Move.one("piercing_mist", () -> MurimAnimations.PIERCING_MIST),
                Move.one("falling_rain", () -> MurimAnimations.FALLING_RAIN),
                Move.one("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH)));
    }

    @GameTest(template = "foundation_arena", batch = "parry_survey_heart_demon_fist", timeoutTicks = (3 * 5 + 2) * CASE_TICKS + 200)
    public static void everyBlowOfTheHeartDemonsFistsCanBeParried(GameTestHelper helper) {
        survey(helper, 2, "Heart Demon (fists)", demon(false, false), List.of(
                Move.one("fist_1", () -> Animations.FIST_AUTO1),
                Move.one("fist_2", () -> Animations.FIST_AUTO2),
                Move.one("fist_3", () -> Animations.FIST_AUTO3),
                Move.many("rapid_fists", () -> MurimAnimations.RAPID_FISTS),
                Move.one("tiger_step_punch", () -> MurimAnimations.TIGER_STEP_PUNCH)));
    }

    @GameTest(template = "foundation_arena", batch = "parry_survey_shadow", timeoutTicks = (3 * 2 + 2) * CASE_TICKS + 200)
    public static void theShadowsBlowsCanBeParried(GameTestHelper helper) {
        survey(helper, 3, "Shadow", demon(true, true), List.of(
                Move.one("drifting_cut", () -> MurimAnimations.DRIFTING_CUT),
                Move.one("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH)));
    }

    @GameTest(template = "foundation_arena", batch = "parry_survey_sensei", timeoutTicks = (3 * 11 + 2) * CASE_TICKS + 200)
    public static void everyBlowOfSenseiYunCanBeParried(GameTestHelper helper) {
        survey(helper, 4, "Sensei Yun", ParryEverythingGameTests::sensei, List.of(
                Move.one("drifting_cut", () -> MurimAnimations.DRIFTING_CUT),
                Move.one("returning_cut", () -> MurimAnimations.RETURNING_CUT),
                Move.one("piercing_mist", () -> MurimAnimations.PIERCING_MIST),
                Move.one("rising_cloud", () -> MurimAnimations.RISING_CLOUD),
                Move.one("falling_rain", () -> MurimAnimations.FALLING_RAIN),
                Move.one("swallow_returns", () -> MurimAnimations.SWALLOW_RETURNS),
                Move.many("thousand_drizzle", () -> MurimAnimations.THOUSAND_DRIZZLE),
                Move.one("wind_step", () -> MurimAnimations.WIND_STEP),
                Move.one("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH),
                Move.one("heaven_splitting_crescent", () -> MurimAnimations.HEAVEN_SPLITTING_CRESCENT),
                Move.many("thousand_drizzle_storm", () -> MurimAnimations.THOUSAND_DRIZZLE_STORM)));
    }

    @GameTest(template = "foundation_arena", batch = "parry_survey_crab_man", timeoutTicks = (3 * 2 + 2) * CASE_TICKS + 200)
    public static void theCrabMansClawsCanBeParried(GameTestHelper helper) {
        survey(helper, 5, "Crab man", ParryEverythingGameTests::crabMan, List.of(
                Move.one("attack_claw", () -> CrabManEpicFight.ATTACK_CLAW),
                Move.one("claw_snap", () -> CrabManEpicFight.CLAW_SNAP)));
    }

    @GameTest(template = "foundation_arena", batch = "parry_survey_armored_crab", timeoutTicks = (3 * 2 + 2) * CASE_TICKS + 200)
    public static void theArmoredCrabsClawsCanBeParried(GameTestHelper helper) {
        survey(helper, 6, "Armoured crab", ParryEverythingGameTests::armoredCrab, List.of(
                Move.one("attack_right", () -> ArmoredCrabEpicFight.ATTACK_RIGHT),
                Move.one("attack_left", () -> ArmoredCrabEpicFight.ATTACK_LEFT)));
    }

    // ---------------------------------------------------------------- the harness

    /**
     * Plays every move three times (no guard, perfect parry, held guard) from the attacker made by {@code make} (a Long
     * Fist player when null) at a player 1.6 blocks in front of it, and checks what went through.
     */
    private static void survey(GameTestHelper helper, int slot, String who, BiFunction<ServerLevel, ServerPlayer, Attacker> make, List<Move> moves) {
        ServerLevel level = helper.getLevel();
        level.getServer().setPvpAllowed(true);
        Vec3 at = origin(slot);
        // Its chunk held (forced chunks tick their entities and Epic Fight's animations), released at the end.
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(at));
        level.setChunkForced(chunk.x, chunk.z, true);
        ServerPlayer defender = player(helper);
        EpicFightCombatDefaults.enforce(defender);
        defender.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        com.murimblock.combat.BurstGuard.clearForTests();
        Attacker attacker = make == null ? fist(level, defender, helper) : make.apply(level, defender);
        attacker.entity.moveTo(at.x, at.y, at.z, 0, 0);
        PlayerPatch<?> d = EpicFightBridge.patch(defender);
        LivingEntityPatch<?> a = attacker.patch;
        helper.assertTrue(a != null, who + " has no Epic Fight patch");
        Vec3 facing = at.add(0, 0, attacker.reach);
        float[][] damage = new float[moves.size()][Guard.values().length];
        // Two whole unguarded cases first, not counted: a fresh attacker's first blows may not land (a mob's bare hands
        // take two to be ready).
        List<int[]> cases = new ArrayList<>();
        cases.add(new int[]{0, Guard.NONE.ordinal(), 0});
        cases.add(new int[]{0, Guard.NONE.ordinal(), 0});
        for (int m = 0; m < moves.size(); m++) for (Guard g : Guard.values()) cases.add(new int[]{m, g.ordinal(), 1});
        int step = 0;
        for (int[] c : cases) {
            {
                Guard guard = Guard.values()[c[1]];
                boolean counted = c[2] == 1;
                Move move = moves.get(c[0]);
                int mi = c[0];
                int start = WARM_UP + step++ * CASE_TICKS;
                helper.runAfterDelay(start, () -> {
                    MartialCombat.clearForTests();
                    if (attacker.entity instanceof ServerPlayer p) MartialService.clearCooldownsForTests(p);
                    a.getAnimator().playAnimation(Animations.BIPED_IDLE, 0.0F);
                    d.getAnimator().playAnimation(Animations.BIPED_IDLE, 0.0F);
                    hold(attacker.entity, at, 0);
                    hold(defender, facing, 180);
                    defender.setHealth(defender.getMaxHealth());
                    defender.invulnerableTime = 0;
                    d.resetHolding();
                    if (guard != Guard.NONE) {
                        helper.assertTrue(d.startSkillHolding(MurimSkills.MURIM_GUARD.get()), "Could not raise the guard");
                        d.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, defender.tickCount - (guard == Guard.PERFECT ? 2 : 60));
                    }
                    a.playAnimationSynchronized(move.animation.get(), 0.0F);
                });
                for (int t = start + 1; t < start + CASE_TICKS - 1; t++) {
                    helper.runAfterDelay(t, () -> {
                        hold(attacker.entity, at, 0);
                        hold(defender, facing, 180);
                        if (guard == Guard.PERFECT) {
                            var data = d.getSkill(SkillSlots.GUARD).getDataManager();
                            if (data.getDataValue(EpicFightSkillDataKeys.LAST_ACTIVE) != 0) data.setData(EpicFightSkillDataKeys.LAST_ACTIVE, defender.tickCount - 2);
                        }
                        if (attacker.entity instanceof ServerPlayer p) p.doTick();
                        else if (!level.isPositionEntityTicking(attacker.entity.blockPosition())) attacker.entity.tick();
                        defender.doTick();
                        defender.invulnerableTime = 0;
                    });
                }
                helper.runAfterDelay(start + CASE_TICKS - 1, () -> {
                    if (counted) damage[mi][guard.ordinal()] = defender.getMaxHealth() - defender.getHealth();
                    d.resetHolding();
                });
            }
        }
        helper.runAfterDelay(WARM_UP + step * CASE_TICKS + 2, () -> {
            List<String> table = new ArrayList<>(), wrong = new ArrayList<>();
            table.add(String.format("%-26s %8s %8s %8s", who, "none", "perfect", "normal"));
            for (int m = 0; m < moves.size(); m++) {
                Move move = moves.get(m);
                float none = damage[m][0], perfect = damage[m][1], normal = damage[m][2];
                table.add(String.format("%-26s %8.2f %8.2f %8.2f", move.name, none, perfect, normal));
                if (none <= 0) wrong.add(move.name + ": no damage without guard (the case tests nothing)");
                if (!move.flurry && perfect > 0.01F) wrong.add(move.name + ": a perfect parry let " + perfect + " through");
                if (move.flurry && perfect >= normal - 0.01F) wrong.add(move.name + ": a perfect parry did not stop its first hit");
                if (normal <= 0.01F || normal > none * 0.6F + 0.05F) wrong.add(move.name + ": a held guard took " + normal + " of " + none);
            }
            LOGGER.info("Parry survey:\n{}", String.join("\n", table));
            MartialCombat.clearForTests();
            level.getServer().setPvpAllowed(false);
            if (attacker.entity instanceof ServerPlayer p) cleanup(p);
            else attacker.entity.discard();
            cleanup(defender);
            level.setChunkForced(chunk.x, chunk.z, false);
            helper.assertTrue(wrong.isEmpty(), who + ": " + String.join(" | ", wrong));
            helper.succeed();
        });
    }

    /** Puts a body at `at` facing `yaw`, still, on the ground (in every way Epic Fight reads the facing). */
    private static void hold(LivingEntity e, Vec3 at, float yaw) {
        e.setPos(at.x, at.y, at.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.setYRot(yaw);
        e.yRotO = yaw;
        e.setYHeadRot(yaw);
        e.yHeadRotO = yaw;
        e.setYBodyRot(yaw);
        e.yBodyRotO = yaw;
        e.setOnGround(true);
        if (e instanceof ServerPlayer p) {
            PlayerPatch<?> patch = EpicFightBridge.patch(p);
            patch.setYRot(yaw);
            patch.setYRotO(yaw);
            patch.setModelYRot(yaw, false);
        }
    }
}
