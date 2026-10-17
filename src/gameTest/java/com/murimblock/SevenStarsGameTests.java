package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.SevenStarsMoves;
import com.murimblock.martial.SevenStarsSword;
import com.murimblock.qi.QiService;
import java.util.Optional;
import net.dawn_day.gameasset.animation.DawnDayAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Seven Stars Sword (docs/combat/styles/seven-stars-sword.md), made one piece at a time. Its passive, Star Marks: the
 * florett only; a real florett thrust of Dawn Day puts a star; seven make a constellation the next blow bursts, harder;
 * a perfect parry puts two; being hurt loses two; left alone, they go out one by one.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SevenStarsGameTests {
    private SevenStarsGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    private static ItemStack florett() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("epicfight_dd", "iron_florett")));
    }

    /** A student of the art (its main style), a florett in hand, combat on, facing south. */
    private static ServerPlayer student(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        MartialService.learn(player, MartialArts.art(MartialArts.SEVEN_STARS_SWORD));
        player.setItemInHand(InteractionHand.MAIN_HAND, florett());
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 1000);
        QiService.setQi(player, 1000);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(400);
        zombie.moveTo(player.getX(), player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "stars_florett")
    public static void theArtIsTheFlorettsOnly(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        helper.assertTrue(MartialService.data(player).mainArt().equals(Optional.of(MartialArts.SEVEN_STARS_SWORD)), "The art is not the main style");
        helper.assertTrue(SevenStarsSword.holdsFlorett(player) && SevenStarsSword.active(player), "A florett does not count as one");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(!SevenStarsSword.holdsFlorett(player), "A sword counts as a florett");
        helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.NO_WEAPON, "Shooting Star went with a sword");
        end(helper, player);
    }

    /** Seven stars, then the next blow bursts them, harder, and they fall back to none. */
    @GameTest(template = "spear_arena", batch = "stars_burst")
    public static void sevenStarsBurstOnTheNextBlow(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie zombie = dummy(helper, player, 1.6);
        for (int i = 1; i <= 7; i++) {
            SevenStarsSword.strikeForTests(player, zombie);
            helper.assertTrue(SevenStarsSword.stars(player, zombie) == i, "After " + i + " blows: " + SevenStarsSword.stars(player, zombie) + " stars");
        }
        SevenStarsSword.strikeForTests(player, zombie);
        helper.assertTrue(SevenStarsSword.stars(player, zombie) == 0, "The constellation did not burst: " + SevenStarsSword.stars(player, zombie));
        end(helper, player, zombie);
    }

    /** A perfect parry puts two; being hurt loses two on the last target. */
    @GameTest(template = "spear_arena", batch = "stars_parry")
    public static void aParryGivesTwoAndAWoundTakesTwo(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie zombie = dummy(helper, player, 1.6);
        MartialCombat.onPerfectParry(player, zombie);
        helper.assertTrue(SevenStarsSword.stars(player, zombie) == 2, "A perfect parry put " + SevenStarsSword.stars(player, zombie));
        SevenStarsSword.strikeForTests(player, zombie);
        SevenStarsSword.strikeForTests(player, zombie);
        player.hurt(player.damageSources().mobAttack(zombie), 2);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(SevenStarsSword.stars(player, zombie) == 2, "A wound left " + SevenStarsSword.stars(player, zombie) + " of 4");
            end(helper, player, zombie);
        });
    }

    /** Left alone 4 s, the stars go out one a second: three are gone at 6 s. */
    @GameTest(template = "spear_arena", batch = "stars_fade", timeoutTicks = 200)
    public static void starsLeftAloneGoOut(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie zombie = dummy(helper, player, 1.6);
        for (int i = 0; i < 3; i++) SevenStarsSword.strikeForTests(player, zombie);
        helper.runAfterDelay(70, () -> helper.assertTrue(SevenStarsSword.stars(player, zombie) == 3, "Stars went out before 4 s"));
        helper.runAfterDelay(95, () -> helper.assertTrue(SevenStarsSword.stars(player, zombie) == 2, "After 4.75 s: " + SevenStarsSword.stars(player, zombie) + ", not 2"));
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(SevenStarsSword.stars(player, zombie) == 0, "After 6.5 s some still shine");
            end(helper, player, zombie);
        });
    }

    /**
     * Dawn Day's real florett thrust, in the stance: its hit puts a star; the eighth, on a full constellation, strikes
     * harder than the first.
     */
    @GameTest(template = "spear_arena", batch = "stars_thrust", timeoutTicks = 200)
    public static void aRealFlorettThrustPutsAStarAndTheBurstStrikesHarder(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie zombie = dummy(helper, player, 1.4);
        float[] dealt = new float[2];
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(player.level().getEntities(player, zombie.getBoundingBox()).contains(zombie), "Not reachable yet"))
                .thenExecute(() -> {
                    tickFor(helper, player, 160);
                    helper.runAfterDelay(25, () -> EpicFightBridge.patch(player).playAnimationSynchronized(DawnDayAnimations.FLORETT_AUTO1, 0.0F));
                    helper.runAfterDelay(55, () -> {
                        dealt[0] = 400 - zombie.getHealth();
                        helper.assertTrue(dealt[0] > 0, "The thrust missed");
                        helper.assertTrue(SevenStarsSword.stars(player, zombie) == 1, "The thrust put " + SevenStarsSword.stars(player, zombie) + " stars");
                        for (int i = 0; i < 6; i++) SevenStarsSword.strikeForTests(player, zombie);
                        zombie.setHealth(400);
                        zombie.invulnerableTime = 0;
                    });
                    helper.runAfterDelay(90, () -> EpicFightBridge.patch(player).playAnimationSynchronized(DawnDayAnimations.FLORETT_AUTO1, 0.0F));
                    helper.runAfterDelay(120, () -> {
                        dealt[1] = 400 - zombie.getHealth();
                        helper.assertTrue(SevenStarsSword.stars(player, zombie) == 0, "The full constellation did not burst");
                        helper.assertTrue(dealt[1] > dealt[0] * 1.3, "The burst did not strike harder: " + dealt[1] + " against " + dealt[0]);
                        end(helper, player, zombie);
                    });
                });
    }

    // ---------------------------------------------------------------- Shooting Star

    /** At the near edge of streak_arena (16 by 16), facing south across it. */
    private static ServerPlayer atTheEdge(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        BlockPos p = helper.absolutePos(new BlockPos(8, 1, 1));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        return player;
    }

    /** Shooting Star crosses 8 blocks; both zombies on its line are cut and marked; it costs its Qi. */
    @GameTest(template = "streak_arena", batch = "stars_streak", timeoutTicks = 80)
    public static void shootingStarCrossesEightBlocksCuttingAndMarking(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        Zombie near = dummy(helper, player, 2.5), far = dummy(helper, player, 5.5);
        double z0 = player.getZ();
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(player.level().getEntities(player, far.getBoundingBox()).contains(far), "Not reachable yet"))
                .thenExecute(() -> {
                    tickFor(helper, player, 20);
                    helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "Shooting Star was not cast");
                    helper.assertTrue(Math.abs(QiService.getQi(player) - (1000 - MartialArts.SHOOTING_STAR.qiCost())) < 1, "Its Qi was not spent");
                    helper.runAfterDelay(12, () -> {
                        double moved = player.getZ() - z0;
                        helper.assertTrue(Math.abs(moved - SevenStarsSword.STREAK) < 0.3, "The streak went " + moved + " blocks, not 8");
                        helper.assertTrue(near.getHealth() < 400 && far.getHealth() < 400, "A zombie on the line was not cut: " + near.getHealth() + ", " + far.getHealth());
                        helper.assertTrue(SevenStarsSword.stars(player, near) == 1 && SevenStarsSword.stars(player, far) == 1, "The cut zombies were not marked");
                        end(helper, player, near, far);
                    });
                });
    }

    /** A zombie already at seven bursts as the streak passes; one off the line is not touched. */
    @GameTest(template = "streak_arena", batch = "stars_streak_burst", timeoutTicks = 80)
    public static void theStreakBurstsAFullConstellationAndSparesWhatIsOffTheLine(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        Zombie full = dummy(helper, player, 4.0), aside = dummy(helper, player, 4.0);
        aside.moveTo(player.getX() + 3.5, player.getY(), player.getZ() + 4.0, 180, 0);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(player.level().getEntities(player, aside.getBoundingBox()).contains(aside), "Not reachable yet"))
                .thenExecute(() -> {
                    for (int i = 0; i < 7; i++) SevenStarsSword.strikeForTests(player, full);
                    full.setHealth(400);
                    tickFor(helper, player, 20);
                    helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "Shooting Star was not cast");
                    helper.runAfterDelay(12, () -> {
                        helper.assertTrue(SevenStarsSword.stars(player, full) == 0 && full.getHealth() < 400, "The full constellation did not burst");
                        helper.assertTrue(aside.getHealth() == 400 && SevenStarsSword.stars(player, aside) == 0, "A zombie off the line was cut");
                        end(helper, player, full, aside);
                    });
                });
    }

    /** A wall ahead ends the streak against it. */
    @GameTest(template = "streak_arena", batch = "stars_streak_wall", timeoutTicks = 60)
    public static void aWallEndsTheStreak(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        for (int x = 6; x <= 10; x++) for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, 5), net.minecraft.world.level.block.Blocks.STONE);
        double z0 = player.getZ();
        tickFor(helper, player, 20);
        helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "Shooting Star was not cast");
        helper.runAfterDelay(12, () -> {
            double moved = player.getZ() - z0;
            helper.assertTrue(moved > 2.5 && moved < 3.8, "The streak went " + moved + " blocks through a wall 4 ahead");
            end(helper, player);
        });
    }

    // ---------------------------------------------------------------- the other techniques, the secret, the ultimate

    /** Unlocks a technique and puts it in a bar slot. */
    private static void give(ServerPlayer player, MartialArts.Technique technique, int slot) {
        MartialService.unlock(player, technique);
        MartialService.assign(player, slot, technique.id());
    }

    private static void whenReachable(GameTestHelper helper, ServerPlayer player, Runnable then, Zombie... zombies) {
        helper.startSequence().thenWaitUntil(() -> {
            for (Zombie z : zombies) helper.assertTrue(player.level().getEntities(player, z.getBoundingBox()).contains(z), "Not reachable yet");
        }).thenExecute(then);
    }

    /** Dipper Steps: three bounds round the zombie, a thrust and a star at each, and the player ends behind it. */
    @GameTest(template = "streak_arena", batch = "stars_dipper", timeoutTicks = 100)
    public static void dipperStepsBoundRoundAndEndBehind(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        give(player, MartialArts.DIPPER_STEPS, 1);
        helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.NO_TARGET, "Dipper Steps went with nobody in reach");
        Zombie z = dummy(helper, player, 3.0);
        whenReachable(helper, player, () -> {
            tickFor(helper, player, 40);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Dipper Steps was not cast");
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(SevenStarsSword.stars(player, z) == 3 && z.getHealth() < 400, "Three thrusts did not land: " + SevenStarsSword.stars(player, z));
                helper.assertTrue(player.getZ() > z.getZ() + 1, "The player did not end behind the zombie: " + player.getZ() + " against " + z.getZ());
                end(helper, player, z);
            });
        }, z);
    }

    /** Orbiting Stars: a blow is stopped and struck back, marking its author; the stars left fly to it at the end. */
    @GameTest(template = "spear_arena", batch = "stars_orbit", timeoutTicks = 60)
    public static void orbitingStarsStopABlowAndStrikeBack(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        give(player, MartialArts.ORBITING_STARS, 1);
        Zombie z = dummy(helper, player, 1.6);
        helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Orbiting Stars was not cast");
        float health = player.getHealth();
        player.hurt(player.damageSources().mobAttack(z), 6);
        helper.assertTrue(player.getHealth() == health, "The blow went through the orbiting stars");
        helper.assertTrue(SevenStarsMoves.orbiting(player) == 6 && z.getHealth() < 400 && SevenStarsSword.stars(player, z) == 1,
                "The blow was not struck back: " + SevenStarsMoves.orbiting(player) + " stars round, " + SevenStarsSword.stars(player, z) + " on it");
        SevenStarsMoves.releaseForTests(player);
        helper.assertTrue(SevenStarsSword.stars(player, z) == 7 && SevenStarsMoves.orbiting(player) == 0, "The six left did not fly to it: " + SevenStarsSword.stars(player, z));
        end(helper, player, z);
    }

    /** Constellation Link: every marked zombie's stars burst together, harder for more stars; nobody marked, not cast. */
    @GameTest(template = "streak_arena", batch = "stars_link", timeoutTicks = 60)
    public static void constellationLinkBurstsThemAll(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        give(player, MartialArts.CONSTELLATION_LINK, 1);
        Zombie few = dummy(helper, player, 3), many = dummy(helper, player, 6);
        few.moveTo(player.getX() - 3, player.getY(), player.getZ() + 3, 180, 0);
        helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.NO_TARGET, "Constellation Link went with nobody marked");
        for (int i = 0; i < 2; i++) SevenStarsSword.strikeForTests(player, few);
        for (int i = 0; i < 6; i++) SevenStarsSword.strikeForTests(player, many);
        tickFor(helper, player, 30);
        helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Constellation Link was not cast");
        helper.runAfterDelay(SevenStarsMoves.LINK_AT + 3, () -> {
            helper.assertTrue(SevenStarsSword.stars(player, few) == 0 && SevenStarsSword.stars(player, many) == 0, "The stars did not burst");
            float a = 400 - few.getHealth(), b = 400 - many.getHealth();
            helper.assertTrue(a > 0 && b > a * 2, "Six stars did not strike harder than two: " + b + " against " + a);
            end(helper, player, few, many);
        });
    }

    /** Falling Star Rain: the stars fall where the player looks, onto the marked zombie there. */
    @GameTest(template = "streak_arena", batch = "stars_rain", timeoutTicks = 120)
    public static void fallingStarRainFallsOnTheMarked(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        player.setXRot(35);
        give(player, MartialArts.FALLING_STAR_RAIN, 1);
        net.minecraft.world.phys.Vec3 at = SevenStarsMoves.aim(player);
        Zombie z = dummy(helper, player, 3);
        z.moveTo(at.x, player.getY(), at.z, 180, 0);
        SevenStarsSword.strikeForTests(player, z);
        whenReachable(helper, player, () -> {
            tickFor(helper, player, 90);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Falling Star Rain was not cast");
            helper.runAfterDelay(SevenStarsMoves.RAIN_FIRST + 7 * SevenStarsMoves.RAIN_GAP + SevenStarsMoves.RAIN_FALL + 4, () -> {
                helper.assertTrue(z.getHealth() < 400, "No falling star struck the marked zombie");
                end(helper, player, z);
            });
        }, z);
    }

    /** Seven Stars Sword Array: seven blows, then all together; the stars it carried are burst. */
    @GameTest(template = "streak_arena", batch = "stars_array", timeoutTicks = 120)
    public static void theArrayStrikesSevenTimesThenAllTogether(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        give(player, MartialArts.SEVEN_STARS_ARRAY, 1);
        Zombie z = dummy(helper, player, 4);
        whenReachable(helper, player, () -> {
            tickFor(helper, player, 80);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "The array was not cast");
            float[] mid = new float[1];
            helper.runAfterDelay(SevenStarsMoves.ARRAY_FINAL - 2, () -> mid[0] = z.getHealth());
            helper.runAfterDelay(SevenStarsMoves.ARRAY_FINAL + 3, () -> {
                helper.assertTrue(mid[0] < 400, "The seven swords did not strike");
                helper.assertTrue(z.getHealth() < mid[0], "The last blow, all together, did not land");
                helper.assertTrue(SevenStarsSword.stars(player, z) == 0, "Its stars were not burst");
                end(helper, player, z);
            });
        }, z);
    }

    /** Polaris Riposte: found at the thirtieth perfect parry; then a strike on the one parried completes its constellation. */
    @GameTest(template = "spear_arena", batch = "stars_polaris")
    public static void polarisRiposteIsFoundAndCompletesTheConstellation(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie z = dummy(helper, player, 1.6);
        for (int i = 0; i < 29; i++) MartialCombat.onPerfectParry(player, z);
        helper.assertTrue(!MartialService.data(player).hasTechnique(MartialArts.POLARIS_RIPOSTE.id()), "The secret was found before thirty parries");
        MartialCombat.onPerfectParry(player, z);
        helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.POLARIS_RIPOSTE.id()), "The secret was not found at thirty parries");
        SevenStarsSword.setStars(player, z, 0);
        MartialCombat.onPerfectParry(player, z);
        SevenStarsSword.setStars(player, z, 0);
        SevenStarsSword.strikeForTests(player, z);
        helper.assertTrue(SevenStarsSword.stars(player, z) == 7, "The riposte did not complete the constellation: " + SevenStarsSword.stars(player, z));
        end(helper, player, z);
    }

    /** Ten full constellations burst open Dipper Steps (the first quest). */
    @GameTest(template = "spear_arena", batch = "stars_quest")
    public static void tenBurstsUnlockDipperSteps(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie z = dummy(helper, player, 1.6);
        for (int i = 0; i < 80; i++) SevenStarsSword.strikeForTests(player, z);
        helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.DIPPER_STEPS.id()), "Ten bursts did not unlock Dipper Steps");
        end(helper, player, z);
    }

    /** The art's own florett is a florett, and its first blow on a target puts two stars. */
    @GameTest(template = "spear_arena", batch = "stars_own_florett")
    public static void theArtsFlorettPutsTwoStarsFirst(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(com.murimblock.martial.SevenStarsItems.FLORETT.get()));
        helper.assertTrue(SevenStarsSword.holdsFlorett(player), "The Seven Stars Florett does not count as a florett");
        Zombie z = dummy(helper, player, 1.6);
        SevenStarsSword.strikeForTests(player, z);
        helper.assertTrue(SevenStarsSword.stars(player, z) == 2, "Its first blow put " + SevenStarsSword.stars(player, z));
        SevenStarsSword.strikeForTests(player, z);
        helper.assertTrue(SevenStarsSword.stars(player, z) == 3, "Its second blow put more than one");
        helper.assertTrue(com.murimblock.martial.SevenStarsMoves.finishes(player), "Its finish is not the art's");
        end(helper, player, z);
    }
}
