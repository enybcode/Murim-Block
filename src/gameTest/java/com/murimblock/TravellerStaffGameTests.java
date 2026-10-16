package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.CelestialPeakSpear;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.martial.TravellerStaff;
import com.murimblock.martial.TravellerStaffItems;
import com.murimblock.qi.QiService;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.gameasset.Animations;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Traveller's Staff Art (docs/combat/styles/traveller-staff.md) on the server, through Epic Fight's real animations
 * and hits: the scroll teaches all three techniques; the Vertical Strike and the Flurry of Points strike ahead; Third
 * Resonance doubles a chain's third strike; the Spinning Rampart stops what comes from the front only, draws its Qi, lasts
 * 3 s at most, and its cooldown runs from its end.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TravellerStaffGameTests {
    private TravellerStaffGameTests() { }

    /** The middle of spear_arena (9 by 9 blocks): room ahead and behind. */
    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    /** A traveller (scroll read), the staff in hand, on the ground, combat on, full Qi, facing south. */
    private static ServerPlayer traveller(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        new ItemStack(TravellerStaffItems.SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.TRAVELLER_STAFF.get()));
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 100);
        QiService.setQi(player, 100);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        // A helmet: no burning in the sun.
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(200);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, dz > 0 ? 180 : 0, 0);
        return zombie;
    }

    /**
     * Runs {@code then} once the player's level finds every zombie where it was moved (a zombie spawned on the player and
     * moved is found there only after its next tick: before, a blow passes through it).
     */
    private static void whenReachable(GameTestHelper helper, ServerPlayer player, Runnable then, Zombie... zombies) {
        helper.startSequence().thenWaitUntil(() -> {
            for (Zombie z : zombies) helper.assertTrue(player.level().getEntities(player, z.getBoundingBox()).contains(z), "A zombie is not reachable yet");
        }).thenExecute(then);
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        TravellerStaff.stopForTests(player);
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "staff_scroll")
    public static void theScrollTeachesAllThreeTechniquesWhichNeedTheStaff(GameTestHelper helper) {
        ServerPlayer player = traveller(helper);
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.knowsArt(MartialArts.TRAVELLER_STAFF), "The scroll did not teach the art");
        helper.assertTrue(data.mainArt().equals(Optional.of(MartialArts.TRAVELLER_STAFF)), "The first art read is not the main style");
        helper.assertTrue(data.slot(0).equals(Optional.of(MartialArts.VERTICAL_STRIKE.id())), "The Vertical Strike is not in slot 1");
        helper.assertTrue(data.slot(1).equals(Optional.of(MartialArts.POINT_FLURRY.id())), "The Flurry of Points is not in slot 2");
        helper.assertTrue(data.slot(2).equals(Optional.of(MartialArts.SPINNING_RAMPART.id())), "The Spinning Rampart is not in slot 3");
        helper.assertTrue(data.passive().equals(Optional.of(MartialArts.THIRD_RESONANCE)), "Third Resonance is not the passive");
        helper.assertTrue(TravellerStaff.holdsStaff(player) && !CelestialPeakSpear.holdsSpear(player), "The staff counts as a spear for the spear's art");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NO_WEAPON, "The Vertical Strike went with a sword");
        helper.assertTrue(QiService.getQi(player) == 100, "A refused cast spent Qi");
        end(helper, player);
    }

    @GameTest(template = "spear_arena", batch = "staff_strike", timeoutTicks = 100)
    public static void theVerticalStrikeLandsAheadNotBehind(GameTestHelper helper) {
        ServerPlayer player = traveller(helper);
        Zombie ahead = dummy(helper, player, 0, 2.0), behind = dummy(helper, player, 0, -2.0);
        whenReachable(helper, player, () -> {
            helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "The Vertical Strike was not cast");
            tickFor(helper, player, 30);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(ahead.getHealth() < 200, "The strike missed the enemy ahead");
                helper.assertTrue(behind.getHealth() == 200, "The strike hit behind: ahead " + ahead.getHealth() + ", behind " + behind.getHealth() + " (" + behind.getLastDamageSource() + ")");
                helper.assertTrue(QiService.getQi(player) <= 100 - MartialArts.VERTICAL_STRIKE.qiCost() + 1, "No Qi spent");
                helper.assertTrue(MartialService.cooldownLeft(player, MartialArts.VERTICAL_STRIKE.id()) > 0, "No cooldown");
                end(helper, player, ahead, behind);
            });
        }, ahead, behind);
    }

    @GameTest(template = "spear_arena", batch = "staff_flurry", timeoutTicks = 100)
    public static void theFlurryOfPointsStrikesAgainAndAgain(GameTestHelper helper) {
        ServerPlayer player = traveller(helper);
        Zombie zombie = dummy(helper, player, 0, 2.4);
        whenReachable(helper, player, () -> {
            helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "The Flurry of Points was not cast");
            float[] early = new float[1];
            tickFor(helper, player, 30);
            helper.runAfterDelay(7, () -> early[0] = zombie.getHealth());
            helper.runAfterDelay(25, () -> {
                helper.assertTrue(early[0] < 200 && zombie.getHealth() < early[0], "The thrusts did not strike more than once: " + early[0] + " then " + zombie.getHealth());
                end(helper, player, zombie);
            });
        }, zombie);
    }

    /** Three strikes 25 ticks apart (each fully charged, within the chain's 1.5 s): the third deals about twice the first. */
    @GameTest(template = "spear_arena", batch = "staff_resonance", timeoutTicks = 160)
    public static void theThirdStrikeOfAChainStrikesTwiceAsHard(GameTestHelper helper) {
        ServerPlayer player = traveller(helper);
        Zombie zombie = dummy(helper, player, 0, 1.6);
        float[] health = {200, 0, 0, 0};
        whenReachable(helper, player, () -> {
            tickFor(helper, player, 100);
            for (int k = 0; k < 3; k++) {
                int n = k;
                helper.runAfterDelay(20 + 25 * k, () -> EpicFightBridge.patch(player).playAnimationSynchronized(Animations.SPEAR_TWOHAND_AUTO1, 0.0F));
                helper.runAfterDelay(43 + 25 * k, () -> health[n + 1] = zombie.getHealth());
            }
            helper.runAfterDelay(100, () -> {
                float first = health[0] - health[1], third = health[2] - health[3];
                helper.assertTrue(first > 0 && third > 0, "A strike missed: " + first + ", then the third " + third);
                // Twice, less the combo's share for its two hits before (x0.84): about 1.7 times the first.
                helper.assertTrue(third > first * 1.4, "The third strike was not doubled: " + third + " against " + first);
                end(helper, player, zombie);
            });
        }, zombie);
    }

    @GameTest(template = "spear_arena", batch = "staff_rampart", timeoutTicks = 120)
    public static void theRampartStopsTheFrontNotTheBackAndLastsThreeSeconds(GameTestHelper helper) {
        ServerPlayer player = traveller(helper);
        Zombie front = dummy(helper, player, 0, 1.5), back = dummy(helper, player, 0, -1.5);
        helper.assertTrue(MartialService.aim(player, 2) && TravellerStaff.spinning(player), "The rampart did not start");
        helper.assertTrue(QiService.getQi(player) <= 100 - MartialArts.SPINNING_RAMPART.qiCost() + 0.01, "Its 10 Qi were not paid");
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.HINDERED, "Another technique went while it spins");
        tickFor(helper, player, 80);
        float[] at = new float[2];
        helper.runAfterDelay(10, () -> {
            float before = player.getHealth();
            player.hurt(player.damageSources().mobAttack(front), 6);
            helper.assertTrue(player.getHealth() == before, "A blow from the front went through");
        });
        helper.runAfterDelay(12, () -> {
            float before = player.getHealth();
            player.hurt(player.damageSources().mobAttack(back), 6);
            helper.assertTrue(player.getHealth() < before, "A blow from behind was stopped");
            player.setHealth(player.getMaxHealth());
        });
        // 5 Qi a second, a second in: about 15 spent in all.
        helper.runAfterDelay(21, () -> at[0] = (float) QiService.getQi(player));
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(at[0] < 100 - MartialArts.SPINNING_RAMPART.qiCost() - 3 && at[0] > 100 - MartialArts.SPINNING_RAMPART.qiCost() - 7,
                    "The rampart did not draw 5 Qi a second: " + at[0]);
            helper.assertTrue(!TravellerStaff.spinning(player), "The rampart lasted past 3 seconds");
            int left = MartialService.cooldownLeft(player, MartialArts.SPINNING_RAMPART.id());
            helper.assertTrue(left > 200 && left <= MartialArts.SPINNING_RAMPART.cooldownTicks(), "Its cooldown does not run from its end: " + left);
            end(helper, player, front, back);
        });
    }

    @GameTest(template = "spear_arena", batch = "staff_release", timeoutTicks = 40)
    public static void lettingGoStopsTheRampart(GameTestHelper helper) {
        ServerPlayer player = traveller(helper);
        helper.assertTrue(MartialService.aim(player, 2), "The rampart did not start");
        tickFor(helper, player, 20);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST && !TravellerStaff.spinning(player), "Letting go did not stop it");
            helper.assertTrue(MartialService.cooldownLeft(player, MartialArts.SPINNING_RAMPART.id()) > 230, "No cooldown after it");
            end(helper, player);
        });
    }
}
