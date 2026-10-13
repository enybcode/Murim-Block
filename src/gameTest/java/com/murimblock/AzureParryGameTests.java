package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.QiBladeEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Every attack of the Azure Cloud Sword Art against Murim's guard, player against player: each move (the M1, the
 * techniques and their variants, the blades of Qi) is played three times at a player facing the attacker, with no guard,
 * with a perfect parry (the guard raised just before the hit, once: one perfect parry per raise) and with a normal
 * parry (the guard held long before). A perfect parry stops the first hit entirely; a normal parry lets half through; and
 * what a hit does besides its damage (a lift, the crescent's carry, a broken guard) does not happen to a guard that
 * took it. A table of every case is written to the log ("Azure parry"). Near the world's origin, like the hit box survey.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AzureParryGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CASE_TICKS = 40;
    private static final int WARM_UP = 70;
    /** Above any hurt or guard recoil (a hop of 0.4 at most): only a launcher sends a body up faster. */
    private static final double LIFT = 0.42;
    private static final Vec3 ORIGIN = new Vec3(24.5, 140, 24.5);

    private AzureParryGameTests() { }

    private enum Guard { NONE, PERFECT, NORMAL }

    /** An attack: a move played by the attacker, or a blade of Qi sent at the defender. */
    private record Move(String name, Supplier<AnimationAccessor<? extends StaticAnimation>> animation, byte blade) {
        static Move strike(String name, Supplier<AnimationAccessor<? extends StaticAnimation>> a) { return new Move(name, a, (byte) -1); }
        static Move blade(String name, byte kind) { return new Move(name, null, kind); }
    }

    private static List<Move> moves() {
        return List.of(
                Move.strike("drifting_cut", () -> MurimAnimations.DRIFTING_CUT),
                Move.strike("returning_cut", () -> MurimAnimations.RETURNING_CUT),
                Move.strike("piercing_mist", () -> MurimAnimations.PIERCING_MIST),
                Move.strike("rising_cloud", () -> MurimAnimations.RISING_CLOUD),
                Move.strike("falling_rain", () -> MurimAnimations.FALLING_RAIN),
                Move.strike("falling_rain_air", () -> MurimAnimations.FALLING_RAIN_AIR),
                Move.strike("wind_step", () -> MurimAnimations.WIND_STEP),
                Move.strike("cloud_step_slash", () -> MurimAnimations.CLOUD_STEP_SLASH),
                Move.strike("cloud_step_riposte", () -> MurimAnimations.CLOUD_STEP_RIPOSTE),
                Move.strike("cloud_step_dive", () -> MurimAnimations.CLOUD_STEP_DIVE),
                Move.strike("swallow_returns", () -> MurimAnimations.SWALLOW_RETURNS),
                Move.strike("thousand_drizzle", () -> MurimAnimations.THOUSAND_DRIZZLE),
                Move.strike("thousand_drizzle_storm", () -> MurimAnimations.THOUSAND_DRIZZLE_STORM),
                Move.strike("heaven_splitting_crescent", () -> MurimAnimations.HEAVEN_SPLITTING_CRESCENT),
                Move.strike("cloud_splitting_descent", () -> MurimAnimations.CLOUD_SPLITTING_DESCENT),
                Move.blade("qi_wave", QiBladeEntity.WAVE),
                Move.blade("qi_needle", QiBladeEntity.NEEDLE),
                Move.blade("qi_crescent", QiBladeEntity.CRESCENT));
    }

    /** What a case did to the defender. */
    private static final class Result {
        float damage;
        boolean lifted, carried, guardBroken;
    }

    @GameTest(template = "foundation_arena", batch = "azure_parry_all", timeoutTicks = 3 * 20 * CASE_TICKS + 100)
    public static void everyAttackOfTheArtCanBeParried(GameTestHelper helper) {
        helper.getLevel().getServer().setPvpAllowed(true);
        ServerPlayer attacker = player(helper), defender = player(helper);
        EpicFightCombatDefaults.enforce(attacker);
        EpicFightCombatDefaults.enforce(defender);
        defender.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        helper.getLevel().getChunk(1, 1);
        PlayerPatch<?> a = EpicFightBridge.patch(attacker), d = EpicFightBridge.patch(defender);
        Vec3 at = ORIGIN, facing = ORIGIN.add(0, 0, 1.6);
        List<Move> moves = moves();
        Result[][] results = new Result[moves.size()][Guard.values().length];
        // A swing at nobody first: a fresh attacker's very first one does not land (the first case would read nothing).
        helper.runAfterDelay(WARM_UP / 2, () -> {
            hold(attacker, at, 180);
            a.playAnimationSynchronized(MurimAnimations.DRIFTING_CUT, 0.0F);
        });
        for (int t = WARM_UP / 2 + 1; t < WARM_UP - 2; t++) helper.runAfterDelay(t, attacker::doTick);
        int step = 0;
        for (int m = 0; m < moves.size(); m++) {
            for (Guard guard : Guard.values()) {
                Move move = moves.get(m);
                Result r = results[m][guard.ordinal()] = new Result();
                // After a warm-up: a player who just joined cannot be hurt for 60 ticks.
                int start = WARM_UP + step++ * CASE_TICKS;
                QiBladeEntity[] blade = new QiBladeEntity[1];
                helper.runAfterDelay(start, () -> {
                    MartialCombat.clearForTests();
                    MartialService.clearCooldownsForTests(attacker);
                    a.getAnimator().playAnimation(Animations.BIPED_IDLE, 0.0F);
                    d.getAnimator().playAnimation(Animations.BIPED_IDLE, 0.0F);
                    hold(attacker, at, 0);
                    hold(defender, facing, 180);
                    defender.setHealth(defender.getMaxHealth());
                    defender.invulnerableTime = 0;
                    d.resetHolding();
                    if (guard != Guard.NONE) {
                        helper.assertTrue(d.getSkill(SkillSlots.GUARD).getSkill() == MurimSkills.MURIM_GUARD.get(), "The Murim guard is not in the guard slot");
                        helper.assertTrue(d.startSkillHolding(MurimSkills.MURIM_GUARD.get()), "Could not raise the guard");
                        // Normal: raised long before. Perfect: raised just before the hit (kept so until a perfect parry uses it).
                        d.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, defender.tickCount - (guard == Guard.PERFECT ? 2 : 60));
                    }
                    if (move.animation != null) a.playAnimationSynchronized(move.animation.get(), 0.0F);
                    else blade[0] = QiBladeEntity.send(attacker, move.blade, MartialArts.THOUSAND_DRIZZLE.id());
                });
                for (int t = start + 1; t < start + CASE_TICKS - 1; t++) {
                    int k = t - start;
                    helper.runAfterDelay(t, () -> {
                    hold(attacker, at, 0);
                    // The defender stays put (a lift or a carry is seen in its speed or in the carry itself).
                    if (defender.getDeltaMovement().y > LIFT) {
                        r.lifted = true;
                    }
                    if (MartialCombat.isCarriedUp(defender)) r.carried = true;
                    if (MartialCombat.guardBroken(defender)) r.guardBroken = true;
                    hold(defender, facing, 180);
                    if (guard == Guard.PERFECT) {
                        var data = d.getSkill(SkillSlots.GUARD).getDataManager();
                        // Until a perfect parry has used the raise (it sets the time to 0), the raise stays just now.
                        if (data.getDataValue(EpicFightSkillDataKeys.LAST_ACTIVE) != 0) data.setData(EpicFightSkillDataKeys.LAST_ACTIVE, defender.tickCount - 2);
                    }
                    attacker.doTick();
                    if (defender.getDeltaMovement().y > LIFT) r.lifted = true;
                    defender.doTick();
                    if (blade[0] != null && !blade[0].isRemoved() && !helper.getLevel().isPositionEntityTicking(blade[0].blockPosition())) blade[0].tick();
                    defender.invulnerableTime = 0;
                    });
                }
                helper.runAfterDelay(start + CASE_TICKS - 1, () -> {
                    // What the case took off (the hits may land in the server's own tick of the players, between ours).
                    r.damage = defender.getMaxHealth() - defender.getHealth();
                    d.resetHolding();
                    if (blade[0] != null) blade[0].discard();
                    for (QiBladeEntity b : helper.getLevel().getEntitiesOfClass(QiBladeEntity.class, new AABB(BlockPosOf(at)).inflate(20))) b.discard();
                });
            }
        }
        helper.runAfterDelay(WARM_UP + step * CASE_TICKS + 2, () -> {
            List<String> table = new ArrayList<>(), wrong = new ArrayList<>();
            table.add(String.format("%-26s %8s %8s %8s   effects with guard", "move", "none", "perfect", "normal"));
            for (int m = 0; m < moves.size(); m++) {
                Result none = results[m][0], perfect = results[m][1], normal = results[m][2];
                String effects = (perfect.lifted || normal.lifted ? " lifted" : "") + (perfect.carried || normal.carried ? " carried" : "")
                        + (perfect.guardBroken || normal.guardBroken ? " guard-broken" : "");
                table.add(String.format("%-26s %8.2f %8.2f %8.2f  %s", moves.get(m).name, none.damage, perfect.damage, normal.damage, effects));
                String n = moves.get(m).name;
                if (none.damage <= 0) wrong.add(n + ": no damage without guard (the case is not testing anything)");
                // A multi-hit move: the perfect parry stops the first hit; the next ones meet a held guard (normal).
                boolean multi = n.startsWith("thousand_drizzle") || n.equals("cloud_step_riposte");
                if (!multi && perfect.damage > 0.01F) wrong.add(n + ": a perfect parry let " + perfect.damage + " through");
                if (multi && perfect.damage >= normal.damage - 0.01F) wrong.add(n + ": a perfect parry did not stop its first hit");
                if (normal.damage <= 0.01F || normal.damage > none.damage * 0.6F) wrong.add(n + ": a normal parry took " + normal.damage + " of " + none.damage);
                if (!effects.isEmpty()) wrong.add(n + ": with the guard up," + effects);
                // And without a guard they do happen (else the check above proves nothing).
                if (n.equals("rising_cloud") && !none.lifted) wrong.add(n + ": did not lift an unguarded body");
                if (n.equals("heaven_splitting_crescent") && !none.carried) wrong.add(n + ": did not carry an unguarded body");
            }
            LOGGER.info("Azure parry:\n{}", String.join("\n", table));
            MartialCombat.clearForTests();
            helper.getLevel().getServer().setPvpAllowed(false);
            cleanup(attacker);
            cleanup(defender);
            helper.assertTrue(wrong.isEmpty(), String.join(" | ", wrong));
            helper.succeed();
        });
    }

    private static net.minecraft.core.BlockPos BlockPosOf(Vec3 v) {
        return net.minecraft.core.BlockPos.containing(v);
    }

    /** Puts a player at `at` facing `yaw`, still, on the ground (in every way Epic Fight reads the facing). */
    private static void hold(ServerPlayer p, Vec3 at, float yaw) {
        p.setPos(at.x, at.y, at.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.setYHeadRot(yaw);
        p.yHeadRotO = yaw;
        p.setYBodyRot(yaw);
        p.yBodyRotO = yaw;
        p.setOnGround(true);
        PlayerPatch<?> patch = EpicFightBridge.patch(p);
        patch.setYRot(yaw);
        patch.setYRotO(yaw);
        patch.setModelYRot(yaw, false);
    }
}
