package com.murimblock;

import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.AzureCloudSword;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialProgress;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.martial.QiBladeEntity;
import com.murimblock.martial.SectLibrary;
import com.murimblock.qi.QiService;
import com.murimblock.world.structure.SanctuaryPlacement;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Azure Cloud Sword Art complete, on the server, through Epic Fight's real animations and hits: the passive's
 * strikes (the sword's M1 chain replaced, Qi from the strikes that land), its parry (Qi, riposte at half cost, the guard
 * broken), each technique's variants (Swallow Returns behind the attacker, Thousand Drizzle's thrusts and wave, the
 * crescent's lift and its air slam, the dive), the bounded combo and its quest, mastery and the free second dash, the
 * quests through play, and the art's finish. Real server players on mock connections, ticked by hand.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AzureCloudSwordGameTests {
    private AzureCloudSwordGameTests() { }

    /** A student of the art (manual read), iron sword in hand, on the ground, combat mode on, full Qi, facing south (+Z). */
    private static ServerPlayer adept(GameTestHelper helper, double z) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        ItemStack manual = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
        manual.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 0));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + z, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        return player;
    }

    /** A sturdy zombie standing still `ahead` blocks in front of the player, facing them. */
    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double ahead) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        // A helmet: no burning in the sun (it would count as hits).
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(200);
        zombie.moveTo(player.getX(), player.getY(), player.getZ() + ahead, 180, 0);
        zombie.setYHeadRot(180);
        zombie.yBodyRot = 180;
        return zombie;
    }

    private static PlayerPatch<?> patch(ServerPlayer player) {
        return EpicFightBridge.patch(player);
    }

    /** Ends whatever the player is playing (back to idle, free to act), as the end of an animation would. */
    private static void stopAnimation(ServerPlayer player) {
        PlayerPatch<?> patch = patch(player);
        patch.getAnimator().playAnimation(Animations.BIPED_IDLE, 0.0F);
        player.doTick();
        patch.updateEntityState();
    }

    /** The animation the player's Epic Fight animator is playing (past any transition). */
    private static boolean playing(ServerPlayer player, AnimationAccessor<? extends StaticAnimation> animation) {
        AnimationPlayer main = patch(player).getAnimator().getPlayerFor(null);
        return main != null && main.getRealAnimation() != null && main.getRealAnimation().get() == animation.get();
    }

    /** Ticks the player by hand (the server-side animator, the art's timed actions) for `ticks` ticks from now. */
    private static void tickFor(GameTestHelper helper, ServerPlayer player, int from, int ticks) {
        for (int t = from; t < from + ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    // ---------------------------------------------------------------- Flowing Cloud Stance

    @GameTest(template = "foundation_arena", batch = "azure_stance_chain")
    public static void theStanceGivesASwordItsStrikes(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        PlayerPatch<?> patch = patch(player);
        List<?> chain = patch.getPrimaryItemCapability().getAutoAttackMotion(patch);
        helper.assertTrue(chain.equals(List.of(MurimAnimations.DRIFTING_CUT, MurimAnimations.RETURNING_CUT, MurimAnimations.PIERCING_MIST,
                MurimAnimations.FALLING_RAIN, MurimAnimations.WIND_STEP, MurimAnimations.FALLING_RAIN_AIR)), "Not the stance's default chain: " + chain);
        MartialService.setStrike(player, 3, MartialArts.RISING_CLOUD.id());
        chain = patch.getPrimaryItemCapability().getAutoAttackMotion(patch);
        helper.assertTrue(chain.get(3) == MurimAnimations.RISING_CLOUD, "The fourth M1 did not become Rising Cloud");
        MartialService.setStrike(player, 3, MartialArts.WIND_STEP.id());
        helper.assertTrue(patch.getPrimaryItemCapability().getAutoAttackMotion(patch).get(3) == MurimAnimations.RISING_CLOUD,
                "An opener was accepted in the fourth M1 slot");
        // Not a sword: the item's own chain. Not the main style any more: the sword's own chain.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        player.doTick();
        helper.assertTrue(!patch.getPrimaryItemCapability().getAutoAttackMotion(patch).contains(MurimAnimations.DRIFTING_CUT), "An axe got the sword stance");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.doTick();
        MartialService.forgetArt(player, MartialArts.art(MartialArts.AZURE_CLOUD_SWORD));
        helper.assertTrue(!patch.getPrimaryItemCapability().getAutoAttackMotion(patch).contains(MurimAnimations.DRIFTING_CUT), "The stance stayed after forgetting the art");
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "azure_stance_qi", timeoutTicks = 60)
    public static void aStrikeThatLandsGivesBackQi(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 1.5);
        QiService.setQi(player, 50);
        patch(player).playAnimationSynchronized(MurimAnimations.DRIFTING_CUT, 0.0F);
        tickFor(helper, player, 1, 14);
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(zombie.getHealth() < 200, "Drifting Cut did not hit the zombie in front");
            helper.assertTrue(QiService.getQi(player) == 51, "A strike that landed should give back 1 Qi, Qi is " + QiService.getQi(player));
            helper.assertTrue(MartialService.data(player).progress(MartialArts.STANCE_HITS) == 1, "The strike did not count for the passive's mastery");
            end(helper, player, zombie);
        });
    }

    /** Raises the Murim guard as if `ticksAgo` ticks before the hit, then the zombie hits: a perfect parry. */
    private static void perfectParry(GameTestHelper helper, ServerPlayer player, Zombie zombie) {
        PlayerPatch<?> patch = patch(player);
        helper.assertTrue(patch.getSkill(SkillSlots.GUARD).getSkill() == MurimSkills.MURIM_GUARD.get(), "The Murim guard is not in the guard slot");
        helper.assertTrue(patch.startSkillHolding(MurimSkills.MURIM_GUARD.get()), "Could not raise the guard");
        patch.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, player.tickCount - 3);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(zombie), 6.0F);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "The parry was not perfect");
    }

    @GameTest(template = "foundation_arena", batch = "azure_parry_riposte", timeoutTicks = 60)
    public static void aPerfectParryGivesQiAndARiposteAtHalfCost(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 1.5);
        QiService.setQi(player, 50);
        perfectParry(helper, player, zombie);
        helper.assertTrue(QiService.getQi(player) == 50 + MartialCombat.PARRY_QI, "A perfect parry should give back 6 Qi: " + QiService.getQi(player));
        helper.assertTrue(MartialCombat.riposte(player) != null, "No riposte window after a perfect parry");
        helper.assertTrue(MartialService.data(player).progress(MartialArts.PATIENT_BLADE_PARRIES) == 1, "The quest did not count the parry");
        patch(player).resetHolding();
        // Past Swallow Returns' quarter second, still within the riposte's 1.5 s: Cloud Step's riposte, at half cost.
        tickFor(helper, player, 1, MartialCombat.SWALLOW_TICKS + 2);
        helper.runAfterDelay(MartialCombat.SWALLOW_TICKS + 4, () -> {
            CastResult result = MartialService.castSlot(player, 0);
            helper.assertTrue(result == CastResult.CAST, "The riposte was refused: " + result);
            helper.assertTrue(QiService.getQi(player) == 56 - MartialArts.CLOUD_STEP_SLASH.qiCost() / 2, "A riposte costs half: Qi " + QiService.getQi(player));
            helper.assertTrue(playing(player, MurimAnimations.CLOUD_STEP_RIPOSTE), "Not Cloud Step's riposte (the short double cut)");
            helper.assertTrue(MartialCombat.riposte(player) == null, "The riposte was not used up");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "azure_swallow", timeoutTicks = 40)
    public static void swallowReturnsAppearsBehindTheAttackerAndIsDiscovered(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 1.6);
        perfectParry(helper, player, zombie);
        CastResult result = MartialService.castSlot(player, 0);
        helper.assertTrue(result == CastResult.CAST, "Cloud Step right after the parry was refused: " + result);
        helper.assertTrue(player.getZ() > zombie.getZ() + 0.5, "The player did not appear behind the zombie: " + player.getZ() + " vs " + zombie.getZ());
        helper.assertTrue(playing(player, MurimAnimations.SWALLOW_RETURNS), "Swallow Returns' cut is not playing");
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.hasTechnique(MartialArts.SWALLOW_RETURNS.id()), "The secret was not revealed");
        helper.assertTrue(!data.bar().contains(java.util.Optional.of(MartialArts.SWALLOW_RETURNS.id())), "The secret went on the bar");
        float health = zombie.getHealth();
        tickFor(helper, player, 1, 12);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(zombie.getHealth() < health, "Swallow Returns' cut missed the attacker");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "azure_guard_break")
    public static void aBrokenGuardParriesNothing(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 1.5);
        PlayerPatch<?> patch = patch(player);
        patch.startSkillHolding(MurimSkills.MURIM_GUARD.get());
        patch.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, player.tickCount - 3);
        // What the last thrust of a riposte's Thousand Drizzle does to its target.
        MartialCombat.clearForTests();
        MartialCombat.breakGuard(player);
        helper.assertTrue(MartialCombat.guardBroken(player), "The guard is not broken");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(zombie), 6.0F);
        float taken = player.getMaxHealth() - player.getHealth();
        helper.assertTrue(Math.abs(taken - 6.0F) < 0.01F, "A broken guard still parried: took " + taken);
        end(helper, player, zombie);
    }

    // ---------------------------------------------------------------- Thousand Drizzle

    private static void unlock(ServerPlayer player, MartialArts.Technique technique, int slot) {
        MartialService.unlock(player, technique);
        MartialService.assign(player, slot, technique.id());
    }

    @GameTest(template = "foundation_arena", batch = "azure_drizzle", timeoutTicks = 80)
    public static void thousandDrizzleStrikesSevenTimes(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 1.8);
        unlock(player, MartialArts.THOUSAND_DRIZZLE, 1);
        int[] hits = {0};
        float[] last = {zombie.getHealth()};
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Thousand Drizzle was refused");
        helper.assertTrue(playing(player, MurimAnimations.THOUSAND_DRIZZLE), "Not the close-range flurry");
        for (int t = 1; t <= 34; t++) helper.runAfterDelay(t, () -> {
            player.doTick();
            zombie.invulnerableTime = 0;
            if (zombie.getHealth() < last[0]) hits[0]++;
            last[0] = zombie.getHealth();
        });
        helper.runAfterDelay(38, () -> {
            helper.assertTrue(hits[0] >= 5, "The flurry should strike many times, struck " + hits[0]);
            helper.assertTrue(MartialService.data(player).mastery(MartialArts.THOUSAND_DRIZZLE.id()) == 1, "Its mastery did not go up once");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "azure_drizzle_edges", timeoutTicks = 20)
    public static void thousandDrizzleThrustsWithinThreeBlocksAndSendsItsWaveOtherwise(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        unlock(player, MartialArts.THOUSAND_DRIZZLE, 1);
        // Nobody in front: the wave, straight ahead.
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Thousand Drizzle was refused");
        helper.assertTrue(playing(player, MurimAnimations.DRIZZLE_WAVE), "With nobody in front, not the wave");
        // An enemy 2.9 blocks ahead, within the thrusts' reach: the flurry.
        stopAnimation(player);
        MartialService.clearCooldownsForTests(player);
        Zombie zombie = dummy(helper, player, 2.9);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Thousand Drizzle was refused");
        helper.assertTrue(playing(player, MurimAnimations.THOUSAND_DRIZZLE), "An enemy 2.9 blocks ahead did not get the flurry");
        // 3.4 blocks ahead, out of reach: the wave.
        stopAnimation(player);
        MartialService.clearCooldownsForTests(player);
        zombie.moveTo(player.getX(), player.getY(), player.getZ() + 3.4, 180, 0);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Thousand Drizzle was refused");
        helper.assertTrue(playing(player, MurimAnimations.DRIZZLE_WAVE), "An enemy 3.4 blocks ahead did not get the wave");
        end(helper, player, zombie);
    }

    @GameTest(template = "foundation_arena", batch = "azure_drizzle_wave", timeoutTicks = 60)
    public static void atMidDistanceThousandDrizzleSendsAWave(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.2);
        Zombie zombie = dummy(helper, player, 4.4);
        unlock(player, MartialArts.THOUSAND_DRIZZLE, 1);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Thousand Drizzle was refused");
        helper.assertTrue(playing(player, MurimAnimations.DRIZZLE_WAVE), "Not the wave's thrust at mid-distance");
        var level = player.serverLevel();
        boolean[] sent = {false};
        for (int t = 1; t <= 20; t++) helper.runAfterDelay(t, () -> {
            player.doTick();
            for (QiBladeEntity blade : level.getEntitiesOfClass(QiBladeEntity.class, new AABB(player.blockPosition()).inflate(12))) {
                sent[0] = true;
                // Where the test server does not tick entities (it depends on where the arena lands), move it on here.
                if (!level.isPositionEntityTicking(blade.blockPosition())) blade.tick();
            }
        });
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(sent[0], "No wave left the blade");
            helper.assertTrue(zombie.getHealth() < 200, "The wave did not reach the zombie 4.4 blocks away");
            end(helper, player, zombie);
        });
    }

    // ---------------------------------------------------------------- Heaven-Splitting Crescent

    @GameTest(template = "foundation_arena", batch = "azure_crescent", timeoutTicks = 50)
    public static void theCrescentCarriesTheTargetThreeBlocksUpWithThePlayer(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 1.5);
        unlock(player, MartialArts.HEAVEN_SPLITTING_CRESCENT, 2);
        // A floor and real gravity: the mock player then moves under Minecraft's own physics, as a client would.
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        player.setNoGravity(false);
        double ground = player.getY();
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST, "The crescent was refused");
        helper.assertTrue(playing(player, MurimAnimations.HEAVEN_SPLITTING_CRESCENT), "Not the ground crescent");
        double[] vy = {Double.NaN}, peak = {ground}, zombiePeak = {zombie.getY()}, gap = {0}, ahead = {0};
        for (int t = 1; t <= 22; t++) helper.runAfterDelay(t, () -> {
            if (Double.isNaN(vy[0]) && player.getDeltaMovement().y > 0.5) vy[0] = player.getDeltaMovement().y;
            player.doTick();
            if (player.getY() > peak[0]) {
                peak[0] = player.getY();
                gap[0] = Math.abs(zombie.getY() - player.getY());
                ahead[0] = zombie.position().subtract(player.position()).horizontalDistance();
            }
            zombiePeak[0] = Math.max(zombiePeak[0], zombie.getY());
        });
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(zombie.getHealth() < 200, "The crescent missed");
            helper.assertTrue(!Double.isNaN(vy[0]), "The player was not sent up");
            double rise = peak[0] - ground;
            helper.assertTrue(rise > 2.8 && rise < 3.2, String.format("The leap should take the player 3 blocks up, took %.2f", rise));
            helper.assertTrue(zombiePeak[0] - ground > 2.6, String.format("The target was not carried up (%.2f blocks)", zombiePeak[0] - ground));
            helper.assertTrue(gap[0] < 0.4 && ahead[0] > 0.8 && ahead[0] < 1.7,
                    String.format("At the top the target should be in front of the player, at their height (height gap %.2f, %.2f ahead)", gap[0], ahead[0]));
            helper.assertTrue(!MartialCombat.isCarriedUp(zombie), "The target was never let go");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "azure_air")
    public static void inTheAirTheArtPlungesAndSlams(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        // High in the arena's open air, nothing under the feet.
        player.moveTo(player.getX(), player.getY() + 2.5, player.getZ(), 0, 0);
        player.setOnGround(false);
        unlock(player, MartialArts.HEAVEN_SPLITTING_CRESCENT, 2);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST, "The descent was refused");
        helper.assertTrue(playing(player, MurimAnimations.CLOUD_SPLITTING_DESCENT), "Not Cloud-Splitting Descent in the air");
        helper.assertTrue(player.getDeltaMovement().y <= -1.5, "The descent did not throw the player down: " + player.getDeltaMovement().y);
        patch(player).playAnimationSynchronized(MurimAnimations.FINISH_GENERIC, 0.0F);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.HINDERED, "A technique cut through another animation");
        stopAnimation(player);
        MartialService.clearCooldownsForTests(player);
        CastResult dive = MartialService.castSlot(player, 0);
        helper.assertTrue(dive == CastResult.CAST && playing(player, MurimAnimations.CLOUD_STEP_DIVE), "Cloud Step in the air is not the dive: " + dive);
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "azure_cloud_step_aim", timeoutTicks = 200)
    public static void cloudStepIsHeldToAimAndGoesWhenLetGo(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        double qi = QiService.getQi(player);
        helper.assertTrue(MartialService.aim(player, 0), "The draw stance was refused");
        helper.assertTrue(playing(player, MurimAnimations.CLOUD_STEP_AIM), "Not in the draw stance");
        helper.assertTrue(QiService.getQi(player) == qi && MartialService.cooldownLeft(player, MartialArts.CLOUD_STEP_SLASH.id()) == 0,
                "The stance alone should cost nothing");
        // Held past a loop of the stance (1.2 s), then let go.
        tickFor(helper, player, 1, 40);
        helper.runAfterDelay(10, () -> {
            // Past the blend into it: the stance holds the feet and leaves the aim free.
            var state = patch(player).getEntityState();
            helper.assertTrue(state.movementLocked() && !state.turningLocked(), "The stance should hold the feet and leave the aim free: " + state);
        });
        helper.runAfterDelay(42, () -> {
            helper.assertTrue(playing(player, MurimAnimations.CLOUD_STEP_AIM), "The stance did not hold");
            CastResult result = MartialService.castSlot(player, 0);
            helper.assertTrue(result == CastResult.CAST && playing(player, MurimAnimations.CLOUD_STEP_SLASH), "Letting go did not dash: " + result);
            helper.assertTrue(QiService.getQi(player) == qi - MartialArts.CLOUD_STEP_SLASH.qiCost(), "The dash did not take its Qi");
            stopAnimation(player);
            helper.assertTrue(!MartialService.aim(player, 0), "The stance was taken on cooldown");
            // In the air: no stance (the dive goes at once, on the release).
            MartialService.clearCooldownsForTests(player);
            player.moveTo(player.getX(), player.getY() + 2.5, player.getZ(), 0, 0);
            player.setOnGround(false);
            helper.assertTrue(!MartialService.aim(player, 0), "A stance in the air");
            player.moveTo(player.getX(), player.getY() - 2.5, player.getZ(), 0, 0);
            player.setOnGround(true);
            // A stance whose release never comes is dropped.
            helper.assertTrue(MartialService.aim(player, 0), "The draw stance was refused the second time");
        });
        tickFor(helper, player, 43, 150);
        helper.runAfterDelay(194, () -> {
            helper.assertTrue(!playing(player, MurimAnimations.CLOUD_STEP_AIM), "A stance held forever");
            end(helper, player);
        });
    }

    // ---------------------------------------------------------------- combos, quests, mastery

    @GameTest(template = "foundation_arena", batch = "azure_combo", timeoutTicks = 120)
    public static void aComboLosesDamageAndEndsOnAKnockdownForTheQuest(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 1.5);
        // The first quest done, the bell rung: the second quest counts complete combos.
        MartialService.unlock(player, MartialArts.THOUSAND_DRIZZLE);
        MartialProgress.count(player, MartialArts.SEA_OF_CLOUDS_BELL, 1);
        List<AnimationAccessor<? extends StaticAnimation>> chain = List.of(MurimAnimations.DRIFTING_CUT, MurimAnimations.RETURNING_CUT,
                MurimAnimations.PIERCING_MIST, MurimAnimations.FALLING_RAIN);
        float[] drops = new float[chain.size()];
        float[] before = {zombie.getHealth()};
        // The sword fully charged before each strike (Epic Fight scales a hit by the attack's charge, like Minecraft),
        // so the drop measures the combo alone: each strike plays 14 ticks after the last.
        tickFor(helper, player, 1, 16);
        for (int i = 0; i < chain.size(); i++) {
            int strike = i, at = 17 + i * 14;
            helper.runAfterDelay(at, () -> {
                before[0] = zombie.getHealth();
                zombie.invulnerableTime = 0;
                patch(player).playAnimationSynchronized(chain.get(strike), 0.0F);
            });
            tickFor(helper, player, at + 1, 12);
            helper.runAfterDelay(at + 13, () -> drops[strike] = before[0] - zombie.getHealth());
        }
        helper.runAfterDelay(17 + chain.size() * 14 + 2, () -> {
            for (int i = 0; i < drops.length; i++) helper.assertTrue(drops[i] > 0, "Strike " + (i + 1) + " missed");
            // Same strength (Drifting then Returning Cut, both x1.0): the second keeps 92 %.
            helper.assertTrue(Math.abs(drops[1] / drops[0] - MartialCombat.comboFactor(1)) < 0.03F,
                    "The second hit of the combo should keep 92 % of the first: " + java.util.Arrays.toString(drops));
            helper.assertTrue(MartialService.data(player).progress(MartialArts.SEA_OF_CLOUDS_COMBOS) == 1,
                    "Three hits then Falling Rain should count a complete combo: " + MartialService.data(player).progress(MartialArts.SEA_OF_CLOUDS_COMBOS));
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "azure_quests", timeoutTicks = 60)
    public static void theQuestsUnlockTheirTechniquesThroughPlay(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 2.0);
        // The Patient Blade: ten perfect parries against an enemy...
        for (int i = 0; i < 10; i++) MartialCombat.onPerfectParry(player, zombie);
        MartialCombat.clearForTests();
        helper.assertTrue(MartialService.data(player).progress(MartialArts.PATIENT_BLADE_PARRIES) == 10, "Parries not counted");
        // ...then enemies cut by Cloud Step Slash: one for real, the rest counted.
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Cloud Step was refused");
        tickFor(helper, player, 1, 20);
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(MartialService.data(player).progress(MartialArts.PATIENT_BLADE_DASH_HITS) == 1, "The dash's hit did not count");
            MartialProgress.count(player, MartialArts.PATIENT_BLADE_DASH_HITS, 4);
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.hasTechnique(MartialArts.THOUSAND_DRIZZLE.id()), "The Patient Blade did not teach Thousand Drizzle");
            helper.assertTrue(data.slot(1).equals(java.util.Optional.of(MartialArts.THOUSAND_DRIZZLE.id())), "Thousand Drizzle not placed on the bar");
            // Above the Sea of Clouds: the sanctuary's bell.
            SanctuaryPlacement sanctuary = SanctuaryPlacement.get(player.server.overworld());
            BlockPos bell = helper.absolutePos(new BlockPos(4, 1, 4));
            sanctuary.record(bell.offset(-10, -40, -10), bell.offset(-10, -40, -10), bell.getY() - 30);
            try {
                helper.getLevel().setBlockAndUpdate(bell, Blocks.BELL.defaultBlockState());
                var hit = new BlockHitResult(Vec3.atCenterOf(bell), Direction.NORTH, bell, false);
                SectLibrary.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, bell, hit));
                helper.assertTrue(MartialService.data(player).progress(MartialArts.SEA_OF_CLOUDS_BELL) == 1, "Ringing the sanctuary's bell did not count");
                MartialProgress.count(player, MartialArts.SEA_OF_CLOUDS_COMBOS, 3);
                helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.HEAVEN_SPLITTING_CRESCENT.id()), "The second quest did not teach the crescent");
            } finally {
                sanctuary.clear();
                helper.getLevel().setBlockAndUpdate(bell, Blocks.AIR.defaultBlockState());
            }
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "azure_mastery", timeoutTicks = 60)
    public static void masteryEvolvesCloudStepIntoAFreeSecondDash(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 2.0);
        MartialService.setMastery(player, MartialArts.FLOWING_CLOUD_STANCE.id(), 60);
        MartialService.setMastery(player, MartialArts.CLOUD_STEP_SLASH.id(), 59);
        MartialArts.Art art = MartialArts.art(MartialArts.AZURE_CLOUD_SWORD);
        helper.assertTrue(!MartialService.data(player).evolved(art, MartialArts.CLOUD_STEP_SLASH.id()), "Evolved too early");
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Cloud Step was refused");
        tickFor(helper, player, 1, 16);
        helper.runAfterDelay(18, () -> {
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.mastery(MartialArts.CLOUD_STEP_SLASH.id()) == 60, "A dash that cut an enemy should raise its mastery: " + data.mastery(MartialArts.CLOUD_STEP_SLASH.id()));
            helper.assertTrue(data.evolved(art, MartialArts.CLOUD_STEP_SLASH.id()), "Cloud Step did not evolve at style mastery 30");
            // The evolution counts from the next dash: a dash, then a second one free.
            stopAnimation(player);
            MartialService.clearCooldownsForTests(player);
            player.setOnGround(true);
            double qi = QiService.getQi(player);
            helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "The first dash was refused");
            stopAnimation(player);
            helper.assertTrue(MartialService.cooldownLeft(player, MartialArts.CLOUD_STEP_SLASH.id()) > 0, "No cooldown after the first dash");
            CastResult second = MartialService.castSlot(player, 0);
            helper.assertTrue(second == CastResult.CAST, "Thousand-Li Cloud Step's second dash was refused: " + second);
            helper.assertTrue(QiService.getQi(player) == qi - MartialArts.CLOUD_STEP_SLASH.qiCost(), "The second dash was not free");
            stopAnimation(player);
            helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.COOLDOWN, "A third dash went through");
            end(helper, player, zombie);
        });
    }

    // ---------------------------------------------------------------- the finish

    @GameTest(template = "foundation_arena", batch = "azure_finish", timeoutTicks = 80)
    public static void theArtsFinishIsLastRainOverTheClouds(GameTestHelper helper) {
        helper.getLevel().getServer().setPvpAllowed(true);
        ServerPlayer executor = adept(helper, 0.5);
        ServerPlayer victim = player(helper);
        victim.moveTo(executor.getX(), executor.getY(), executor.getZ() + 1.5, 180, 0);
        KnockdownService.knockDownFor(victim, 600);
        helper.assertTrue(KnockdownService.execute(executor, victim), "The finish was refused");
        helper.assertTrue(playing(executor, MurimAnimations.FINISH_AZURE), "Not the art's finish");
        helper.runAfterDelay(AzureCloudSword.FINISH_HIT_TICK - 4, () ->
                helper.assertTrue(victim.isAlive(), "The blow landed before the leap (at the generic finish's time)"));
        helper.runAfterDelay(AzureCloudSword.FINISH_HIT_TICK + 4, () -> {
            try {
                helper.assertTrue(victim.isDeadOrDying(), "Last Rain Over the Clouds did not end the downed player");
            } finally {
                KnockdownService.clearForTests();
                helper.getLevel().getServer().setPvpAllowed(false);
                cleanup(victim);
            }
            end(helper, executor);
        });
    }
}
