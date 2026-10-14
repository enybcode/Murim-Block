package com.murimblock;

import com.murimblock.AzureHitboxGameTests.Case;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimSkills;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.LongFist;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialProgress;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.qi.QiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import static com.murimblock.AzureHitboxGameTests.hit;
import static com.murimblock.AzureHitboxGameTests.miss;
import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Long Fist (docs/combat/styles/long-fist.md) on the server, through Epic Fight's real animations and hits: the
 * scroll, bare hands, Steady Breath (Qi from Epic Fight's own punches and from a perfect parry, the bare-hand guard only
 * for its main style), each technique and its air variant, the secret Inch Force after a parry, the quests through play,
 * the evolutions, the finish Final Palm, and where every move's box hits. Real server players on mock connections.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LongFistGameTests {
    private LongFistGameTests() { }

    /** A student of the Long Fist (scroll read), bare-handed, on the ground, combat mode on, full Qi, facing south (+Z). */
    private static ServerPlayer adept(GameTestHelper helper, double z) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        ItemStack scroll = new ItemStack(MurimItems.LONG_FIST_SCROLL.get());
        scroll.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 0));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + z, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        return player;
    }

    /** A sturdy zombie standing still, `dx` to the side and `dz` in front of the player, facing them. */
    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        // A helmet: no burning in the sun (it would count as hits).
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(200);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
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

    private static boolean playing(ServerPlayer player, AnimationAccessor<? extends StaticAnimation> animation) {
        AnimationPlayer main = patch(player).getAnimator().getPlayerFor(null);
        return main != null && main.getRealAnimation() != null && main.getRealAnimation().get() == animation.get();
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int from, int ticks) {
        for (int t = from; t < from + ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void unlock(ServerPlayer player, MartialArts.Technique technique, int slot) {
        MartialService.unlock(player, technique);
        MartialService.assign(player, slot, technique.id());
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    /** Raises the Murim guard as if 3 ticks before the hit, then the zombie hits: a perfect parry. */
    private static void perfectParry(GameTestHelper helper, ServerPlayer player, Zombie zombie) {
        PlayerPatch<?> patch = patch(player);
        helper.assertTrue(patch.startSkillHolding(MurimSkills.MURIM_GUARD.get()), "Could not raise the guard bare-handed");
        patch.getSkill(SkillSlots.GUARD).getDataManager().setData(EpicFightSkillDataKeys.LAST_ACTIVE, player.tickCount - 3);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(zombie), 6.0F);
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "The parry was not perfect");
    }

    // ---------------------------------------------------------------- the scroll, bare hands

    @GameTest(template = "foundation_arena", batch = "long_fist_scroll")
    public static void theScrollTeachesTheArtWhoseTechniquesNeedBareHands(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.knowsArt(MartialArts.LONG_FIST), "The scroll did not teach the Long Fist");
        helper.assertTrue(data.mainArt().equals(Optional.of(MartialArts.LONG_FIST)), "The first art read is not the main style");
        helper.assertTrue(data.hasTechnique(MartialArts.TIGER_STEP_PUNCH.id()) && !data.hasTechnique(MartialArts.RAPID_FISTS.id()), "Not Tiger Step alone");
        helper.assertTrue(data.slot(0).equals(Optional.of(MartialArts.TIGER_STEP_PUNCH.id())), "Tiger Step Punch is not in slot 1");
        // A sword in hand: refused, nothing spent.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NO_WEAPON, "Tiger Step went with a sword in hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        double qi = QiService.getQi(player);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Tiger Step was refused bare-handed");
        helper.assertTrue(playing(player, MurimAnimations.TIGER_STEP_PUNCH), "Not the bound");
        helper.assertTrue(QiService.getQi(player) == qi - MartialArts.TIGER_STEP_PUNCH.qiCost(), "Tiger Step did not take its Qi");
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_guard")
    public static void theBareHandGuardBelongsToTheLongFistAsMainStyle(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        PlayerPatch<?> patch = patch(player);
        // What Epic Fight asks before raising the guard on the key: a guard motion for what is in hand.
        var guard = patch.getSkill(SkillSlots.GUARD);
        helper.assertTrue(LongFist.guardsBareHanded(player), "The Long Fist's student does not guard bare-handed");
        helper.assertTrue(MurimSkills.MURIM_GUARD.get().canExecute(guard), "No guard bare-handed with the Long Fist");
        // Another main style: bare hands guard nothing, as before.
        ItemStack manual = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
        manual.use(player.level(), player, InteractionHand.MAIN_HAND);
        MartialService.setMainArt(player, MartialArts.AZURE_CLOUD_SWORD);
        helper.assertTrue(!LongFist.guardsBareHanded(player), "Still the Long Fist's guard with the sword as main style");
        helper.assertTrue(!MurimSkills.MURIM_GUARD.get().canExecute(guard), "A guard bare-handed without the Long Fist");
        // Its techniques stay on the bar, bare-handed, whatever the main style.
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Tiger Step refused with another main style");
        end(helper, player);
    }

    // ---------------------------------------------------------------- Steady Breath

    @GameTest(template = "foundation_arena", batch = "long_fist_punch", timeoutTicks = 60)
    public static void epicFightsOwnPunchesGiveBackQiAndCountForFirstSteps(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 0, 0.9);
        QiService.setQi(player, 50);
        patch(player).playAnimationSynchronized(Animations.FIST_AUTO1, 0.0F);
        tickFor(helper, player, 1, 14);
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(zombie.getHealth() < 200, "Epic Fight's first punch did not hit the zombie in front");
            helper.assertTrue(QiService.getQi(player) == 51, "A punch that landed should give back 1 Qi, Qi is " + QiService.getQi(player));
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.progress(MartialArts.FIRST_STEPS_PUNCHES) == 1, "The punch did not count for First Steps");
            helper.assertTrue(data.progress(MartialArts.STANCE_HITS) == 1, "The punch did not count for the passive's mastery");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_inch_force", timeoutTicks = 60)
    public static void rightAfterAPerfectParryTigerStepAtTheChestIsInchForce(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 0, 1.0);
        QiService.setQi(player, 50);
        perfectParry(helper, player, zombie);
        helper.assertTrue(QiService.getQi(player) == 50 + LongFist.PARRY_QI, "A perfect parry should give back 5 Qi: " + QiService.getQi(player));
        // Still in the guard: Inch Force cuts its recoil short, as a riposte does.
        CastResult result = MartialService.castSlot(player, 0);
        helper.assertTrue(result == CastResult.CAST, "Tiger Step right after the parry was refused: " + result);
        helper.assertTrue(playing(player, MurimAnimations.INCH_FORCE), "Not Inch Force at the chest after a parry");
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.hasTechnique(MartialArts.INCH_FORCE.id()), "The secret was not revealed");
        helper.assertTrue(!data.bar().contains(Optional.of(MartialArts.INCH_FORCE.id())), "The secret went on the bar");
        // Free to move now (a mob without AI is not moved by any push): it must fly away from the fist.
        zombie.setNoAi(false);
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200, 9));
        double z0 = zombie.getZ();
        tickFor(helper, player, 1, 16);
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(zombie.getHealth() < 200, "Inch Force missed the attacker");
            helper.assertTrue(zombie.getZ() > z0 + 2.0, "Inch Force did not send the body flying: moved " + (zombie.getZ() - z0));
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_no_inch_force")
    public static void withoutAParryTigerStepBoundsAsUsual(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 0, 1.0);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Tiger Step was refused");
        helper.assertTrue(playing(player, MurimAnimations.TIGER_STEP_PUNCH), "Inch Force without a parry");
        helper.assertTrue(!MartialService.data(player).hasTechnique(MartialArts.INCH_FORCE.id()), "The secret revealed without a parry");
        end(helper, player, zombie);
    }

    // ---------------------------------------------------------------- techniques

    @GameTest(template = "foundation_arena", batch = "long_fist_tiger_step", timeoutTicks = 40)
    public static void tigerStepPunchStrikesAtTheEndOfItsBound(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 0, 2.8);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Tiger Step was refused");
        tickFor(helper, player, 1, 16);
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(zombie.getHealth() < 200, "The bound's punch missed a zombie 2.8 blocks ahead");
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.mastery(MartialArts.TIGER_STEP_PUNCH.id()) == 1, "Its mastery did not go up once");
            // First Steps: the punches first; the bound's hit waits for them.
            helper.assertTrue(data.progress(MartialArts.FIRST_STEPS_TIGER_HITS) == 0, "Tiger hits counted before the punches");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_rapid_fists", timeoutTicks = 60)
    public static void rapidFistsLandsEveryPunchAndCountsAFullFlurry(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 0, 1.5);
        unlock(player, MartialArts.RAPID_FISTS, 1);
        int[] hits = {0};
        float[] last = {zombie.getHealth()};
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Rapid Fists was refused");
        helper.assertTrue(playing(player, MurimAnimations.RAPID_FISTS), "Not the flurry");
        for (int t = 1; t <= 30; t++) helper.runAfterDelay(t, () -> {
            player.doTick();
            zombie.invulnerableTime = 0;
            if (zombie.getHealth() < last[0]) hits[0]++;
            last[0] = zombie.getHealth();
        });
        helper.runAfterDelay(32, () -> {
            helper.assertTrue(hits[0] >= MurimAnimations.RAPID_FISTS_PUNCHES + 1, "The flurry should land 6 times, landed " + hits[0]);
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.progress(MartialArts.UNBROKEN_FLURRY_FULL) == 1, "A full flurry did not count for Unbroken Flurry");
            helper.assertTrue(data.mastery(MartialArts.RAPID_FISTS.id()) == 1, "Its mastery did not go up once");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_whirlwind", timeoutTicks = 40)
    public static void whirlwindKickStrikesAllRound(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 3.5);
        Zombie front = dummy(helper, player, 0, 1.5), back = dummy(helper, player, 0, -1.5), side = dummy(helper, player, 1.8, 0);
        unlock(player, MartialArts.WHIRLWIND_KICK, 2);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST, "Whirlwind Kick was refused");
        helper.assertTrue(playing(player, MurimAnimations.WHIRLWIND_KICK), "Not the whirlwind");
        tickFor(helper, player, 1, 20);
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(front.getHealth() < 200 && back.getHealth() < 200 && side.getHealth() < 200,
                    "The whirlwind missed someone: front " + front.getHealth() + ", back " + back.getHealth() + ", side " + side.getHealth());
            end(helper, player, front, back, side);
        });
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_air")
    public static void inTheAirEachTechniqueTakesItsAirVariant(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        unlock(player, MartialArts.RAPID_FISTS, 1);
        unlock(player, MartialArts.WHIRLWIND_KICK, 2);
        player.moveTo(player.getX(), player.getY() + 2.5, player.getZ(), 0, 0);
        player.setOnGround(false);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST && playing(player, MurimAnimations.TIGER_STEP_DIVE), "Tiger Step in the air is not the dive");
        stopAnimation(player);
        player.setOnGround(false);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST && playing(player, MurimAnimations.RAPID_FISTS_AIR), "Rapid Fists in the air is not the hammer fist");
        stopAnimation(player);
        player.setOnGround(false);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST && playing(player, MurimAnimations.WHIRLWIND_DESCENT), "Whirlwind Kick in the air is not the descent");
        helper.assertTrue(player.getDeltaMovement().y <= -1.0, "The descent did not throw the player down: " + player.getDeltaMovement().y);
        end(helper, player);
    }

    // ---------------------------------------------------------------- quests, evolutions, finish

    @GameTest(template = "foundation_arena", batch = "long_fist_quests", timeoutTicks = 40)
    public static void theQuestsUnlockRapidFistsThenWhirlwindKickThroughPlay(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        Zombie zombie = dummy(helper, player, 0, 2.5);
        MartialProgress.count(player, MartialArts.FIRST_STEPS_PUNCHES, 30);
        // One bound for real, the rest counted.
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Tiger Step was refused");
        tickFor(helper, player, 1, 16);
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(MartialService.data(player).progress(MartialArts.FIRST_STEPS_TIGER_HITS) == 1, "The bound's hit did not count");
            MartialProgress.count(player, MartialArts.FIRST_STEPS_TIGER_HITS, 4);
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.hasTechnique(MartialArts.RAPID_FISTS.id()), "First Steps did not teach Rapid Fists");
            helper.assertTrue(data.slot(1).equals(Optional.of(MartialArts.RAPID_FISTS.id())), "Rapid Fists not placed on the bar");
            MartialProgress.count(player, MartialArts.UNBROKEN_FLURRY_FULL, 10);
            data = MartialService.data(player);
            helper.assertTrue(data.hasTechnique(MartialArts.WHIRLWIND_KICK.id()), "Unbroken Flurry did not teach Whirlwind Kick");
            helper.assertTrue(data.slot(2).equals(Optional.of(MartialArts.WHIRLWIND_KICK.id())), "Whirlwind Kick not placed on the bar");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_evolutions")
    public static void theEvolutionsChangeWhatEachTechniquePlays(GameTestHelper helper) {
        ServerPlayer player = adept(helper, 0.5);
        unlock(player, MartialArts.RAPID_FISTS, 1);
        unlock(player, MartialArts.WHIRLWIND_KICK, 2);
        for (var id : List.of(MartialArts.STEADY_BREATH.id(), MartialArts.TIGER_STEP_PUNCH.id(), MartialArts.RAPID_FISTS.id(), MartialArts.WHIRLWIND_KICK.id()))
            MartialService.setMastery(player, id, 100);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST && playing(player, MurimAnimations.TIGER_STEP_PUNCH_LONG), "Not Tiger Leap's longer bound");
        stopAnimation(player);
        player.setOnGround(true);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST && playing(player, MurimAnimations.RAPID_FISTS_STORM), "Not Hundred Fists");
        stopAnimation(player);
        player.setOnGround(true);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST && playing(player, MurimAnimations.WHIRLWIND_KICK_GREAT), "Not Tornado Kick");
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_finish", timeoutTicks = 80)
    public static void theArtsFinishIsFinalPalm(GameTestHelper helper) {
        helper.getLevel().getServer().setPvpAllowed(true);
        ServerPlayer executor = adept(helper, 0.5);
        ServerPlayer victim = player(helper);
        victim.moveTo(executor.getX(), executor.getY(), executor.getZ() + 1.5, 180, 0);
        KnockdownService.knockDownFor(victim, 600);
        helper.assertTrue(KnockdownService.execute(executor, victim), "The finish was refused");
        helper.assertTrue(playing(executor, MurimAnimations.FINISH_LONG_FIST), "Not the art's finish");
        helper.runAfterDelay(LongFist.FINISH_HIT_TICK - 4, () ->
                helper.assertTrue(victim.isAlive(), "The palm landed before the breath (at the generic finish's time)"));
        helper.runAfterDelay(LongFist.FINISH_HIT_TICK + 4, () -> {
            try {
                helper.assertTrue(victim.isDeadOrDying(), "Final Palm did not end the downed player");
            } finally {
                KnockdownService.clearForTests();
                helper.getLevel().getServer().setPvpAllowed(false);
                cleanup(victim);
            }
            end(helper, executor);
        });
    }

    // ---------------------------------------------------------------- hit boxes

    /**
     * Where each move must hit and must not (MurimAnimations; a zombie reaches 0.3 beyond a box). The whirlwind's square
     * turns with the body, so its corners sweep a little beyond 2.5 blocks: its misses are well out.
     */
    private static List<Case> cases() {
        List<Case> c = new ArrayList<>();
        c.add(hit("tiger_step_punch", () -> MurimAnimations.TIGER_STEP_PUNCH, 0, 1.0));
        c.add(hit("tiger_step_punch", () -> MurimAnimations.TIGER_STEP_PUNCH, 0, 3.0));
        c.add(miss("tiger_step_punch", () -> MurimAnimations.TIGER_STEP_PUNCH, 0, 4.2));
        c.add(miss("tiger_step_punch", () -> MurimAnimations.TIGER_STEP_PUNCH, 1.5, 1.5));
        c.add(miss("tiger_step_punch", () -> MurimAnimations.TIGER_STEP_PUNCH, 0, -0.8));
        c.add(hit("tiger_step_punch_long", () -> MurimAnimations.TIGER_STEP_PUNCH_LONG, 0, 4.8));
        c.add(miss("tiger_step_punch_long", () -> MurimAnimations.TIGER_STEP_PUNCH_LONG, 0, 6.2));
        c.add(hit("tiger_step_dive", () -> MurimAnimations.TIGER_STEP_DIVE, 0, 2.0));
        c.add(miss("tiger_step_dive", () -> MurimAnimations.TIGER_STEP_DIVE, 0, -0.9));
        c.add(miss("tiger_step_dive", () -> MurimAnimations.TIGER_STEP_DIVE, 1.7, 1.5));
        c.add(hit("rapid_fists", () -> MurimAnimations.RAPID_FISTS, 0, 1.5));
        c.add(miss("rapid_fists", () -> MurimAnimations.RAPID_FISTS, 0, 3.0));
        c.add(miss("rapid_fists", () -> MurimAnimations.RAPID_FISTS, 1.3, 1.2));
        c.add(miss("rapid_fists", () -> MurimAnimations.RAPID_FISTS, 0, -0.8));
        c.add(hit("rapid_fists_storm", () -> MurimAnimations.RAPID_FISTS_STORM, 0, 1.5));
        c.add(hit("rapid_fists_air", () -> MurimAnimations.RAPID_FISTS_AIR, 0, 1.5));
        c.add(miss("rapid_fists_air", () -> MurimAnimations.RAPID_FISTS_AIR, 0, 3.0));
        c.add(hit("whirlwind_kick", () -> MurimAnimations.WHIRLWIND_KICK, 0, 2.0));
        c.add(hit("whirlwind_kick", () -> MurimAnimations.WHIRLWIND_KICK, 2.0, 0));
        c.add(hit("whirlwind_kick", () -> MurimAnimations.WHIRLWIND_KICK, 0, -2.0));
        c.add(miss("whirlwind_kick", () -> MurimAnimations.WHIRLWIND_KICK, 0, 4.2));
        c.add(hit("whirlwind_kick_great", () -> MurimAnimations.WHIRLWIND_KICK_GREAT, 3.0, 0));
        c.add(miss("whirlwind_kick_great", () -> MurimAnimations.WHIRLWIND_KICK_GREAT, 0, 5.6));
        c.add(hit("whirlwind_descent", () -> MurimAnimations.WHIRLWIND_DESCENT, 2.0, 0));
        c.add(hit("whirlwind_descent", () -> MurimAnimations.WHIRLWIND_DESCENT, 0, -2.0));
        c.add(miss("whirlwind_descent", () -> MurimAnimations.WHIRLWIND_DESCENT, 0, 4.2));
        c.add(hit("inch_force", () -> MurimAnimations.INCH_FORCE, 0, 1.0));
        c.add(miss("inch_force", () -> MurimAnimations.INCH_FORCE, 0, 2.4));
        c.add(miss("inch_force", () -> MurimAnimations.INCH_FORCE, 1.3, 0.8));
        return c;
    }

    @GameTest(template = "foundation_arena", batch = "long_fist_hitboxes", timeoutTicks = 40 * AzureHitboxGameTests.CASE_TICKS)
    public static void everyMoveOfTheLongFistHitsWhereItIsDrawn(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AzureHitboxGameTests.survey(helper, "Long Fist", player, cases());
    }
}
