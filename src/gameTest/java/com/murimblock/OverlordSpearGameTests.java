package com.murimblock;

import com.murimblock.combat.ShatteredGround;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialProgress;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.martial.OverlordQiEntity;
import com.murimblock.martial.OverlordSpear;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.gameasset.Animations;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Overlord Spear (docs/combat/styles/overlord-spear.md, Enzo's design of 18/10/2026): its scroll comes at the
 * Celestial Peak Spear's mastery 50 and teaches from Qi Condensation; the battering ram at five charges; each technique
 * striking where it should (the beam and its secret, the sweep, the leap's landing, the charge into a wall, the
 * rampage, the giant spear).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OverlordSpearGameTests {
    private OverlordSpearGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
    private static final MartialArts.Technique[] ALL = {MartialArts.TYRANTS_THRUST, MartialArts.EARTH_SPLITTING_SWEEP, MartialArts.FALLING_MOUNTAIN,
            MartialArts.OVERLORDS_CHARGE, MartialArts.HUNDRED_BATTLES_RAMPAGE, MartialArts.CONQUEROR_DESCENDS};

    /** A student of the art, every technique on the bar (slots 0 to 5), the Spear of Ruin in hand, on stone, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        CultivationService.setCultivation(player, MartialArts.OVERLORD_REALM, CultivationStage.EARLY);
        new ItemStack(MurimItems.OVERLORD_SPEAR_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        MartialData data = MartialService.data(player);
        for (int i = 0; i < ALL.length; i++) data = data.assign(i, ALL[i].id());
        MartialService.setForTests(player, data);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.SPEAR_OF_RUIN.get()));
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 2);
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 2000);
        QiService.setQi(player, 2000);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
        zombie.setHealth(300);
        // A helmet: no burning in the sun (it would count as hits).
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        helper.getLevel().getEntitiesOfClass(OverlordQiEntity.class, new AABB(player.blockPosition()).inflate(120)).forEach(e -> e.discard());
        ShatteredGround.restoreAllForTests(helper.getLevel());
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "overlord_scroll")
    public static void theScrollComesAtTheCelestialSpearsMasteryFiftyAndTeachesFromQiCondensation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.getInventory().clearContent();
        new ItemStack(com.murimblock.martial.CelestialPeakItems.MANUAL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        MartialArts.Art celestial = MartialArts.art(MartialArts.CELESTIAL_PEAK_SPEAR);
        MartialData learnt = MartialService.data(player).unlock(MartialArts.PLUM_BLOSSOM_THRUSTS).unlock(MartialArts.THOUSAND_SOLDIER_SWEEP);
        MartialService.setForTests(player, learnt);
        // The style's mastery is the mean of the passive's and the techniques': all at 49, no scroll yet.
        for (var id : java.util.List.of(MartialArts.LONG_REACH.id(), MartialArts.PIERCING_DRAGON.id(), MartialArts.PLUM_BLOSSOM_THRUSTS.id(), MartialArts.THOUSAND_SOLDIER_SWEEP.id()))
            MartialProgress.addMastery(player, celestial, id, 49);
        helper.assertTrue(player.getInventory().countItem(MurimItems.OVERLORD_SPEAR_SCROLL.get()) == 0, "The scroll came before mastery 50");
        MartialProgress.addMastery(player, celestial, MartialArts.LONG_REACH.id(), 4);
        int style = MartialService.data(player).styleMastery(celestial);
        helper.assertTrue(player.getInventory().countItem(MurimItems.OVERLORD_SPEAR_SCROLL.get()) == 1, "No scroll at mastery " + style);
        MartialProgress.addMastery(player, celestial, MartialArts.PIERCING_DRAGON.id(), 40);
        helper.assertTrue(player.getInventory().countItem(MurimItems.OVERLORD_SPEAR_SCROLL.get()) == 1, "The scroll came twice");
        // Below Qi Condensation, a closed book; from it, the art and all its techniques, not the secret.
        CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.PEAK);
        new ItemStack(MurimItems.OVERLORD_SPEAR_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(MartialService.data(player).knowsArt(MartialArts.OVERLORD_SPEAR), "The scroll taught below Qi Condensation");
        CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
        new ItemStack(MurimItems.OVERLORD_SPEAR_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.knowsArt(MartialArts.OVERLORD_SPEAR), "The scroll did not teach the art");
        for (MartialArts.Technique t : ALL) helper.assertTrue(data.hasTechnique(t.id()), "Not taught: " + t.id());
        helper.assertFalse(data.hasTechnique(MartialArts.BREAKING_THE_CAULDRONS.id()), "The secret was taught");
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "overlord_ram", timeoutTicks = 60)
    public static void fiveChargesMakeTheNextStrikeABatteringRam(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        MartialService.setMainArt(player, MartialArts.OVERLORD_SPEAR);
        Zombie zombie = dummy(helper, player, 0, 1.6);
        OverlordSpear.setChargesForTests(player, OverlordSpear.DOMINATION_MAX);
        EpicFightBridge.patch(player).playAnimationSynchronized(Animations.SPEAR_TWOHAND_AUTO1, 0.0F);
        tickFor(helper, player, 18);
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(zombie.getHealth() < 300, "The ram missed");
            helper.assertTrue(MartialCombat.guardBroken(zombie), "The ram did not break the guard");
            helper.assertTrue(OverlordSpear.charges(player) == 0, "The charges were not spent: " + OverlordSpear.charges(player));
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "spear_arena", batch = "overlord_thrust", timeoutTicks = 80)
    public static void tyrantsThrustBeamsThroughTheLineAndIsBreakingTheCauldronsWhenNearDeath(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie near = dummy(helper, player, 0, 2.5), far = dummy(helper, player, 0, 4.6), side = dummy(helper, player, 1.4, 3.5);
        helper.assertTrue(MartialService.aim(player, 0), "Tyrant's Thrust did not start charging");
        helper.runAfterDelay(MartialArts.TYRANTS_THRUST_MAX_CHARGE, () -> helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Not let go"));
        tickFor(helper, player, MartialArts.TYRANTS_THRUST_MAX_CHARGE + 8);
        helper.runAfterDelay(MartialArts.TYRANTS_THRUST_MAX_CHARGE + 8, () -> {
            helper.assertTrue(near.getHealth() < 300 && far.getHealth() < 300, "The beam did not go through the line: " + near.getHealth() + ", " + far.getHealth());
            helper.assertTrue(side.getHealth() == 300, "The beam struck to the side");
            // Near death: Breaking the Cauldrons, three blocks wide, the side struck too, the secret found.
            MartialService.clearCooldownsForTests(player);
            player.setHealth(player.getMaxHealth() * 0.15F);
            EpicFightBridge.patch(player).playAnimationSynchronized(Animations.BIPED_IDLE, 0.0F);
            helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "The secret was not cast");
        });
        tickFor(helper, player, MartialArts.TYRANTS_THRUST_MAX_CHARGE + 20);
        helper.runAfterDelay(MartialArts.TYRANTS_THRUST_MAX_CHARGE + 20, () -> {
            helper.assertTrue(side.getHealth() < 300, "Breaking the Cauldrons was not three blocks wide");
            helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.BREAKING_THE_CAULDRONS.id()), "The secret was not discovered");
            end(helper, player, near, far, side);
        });
    }

    @GameTest(template = "spear_arena", batch = "overlord_sweep", timeoutTicks = 60)
    public static void earthSplittingSweepStrikesAllRoundAndThrowsThemUp(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie front = dummy(helper, player, 0, 2.5), back = dummy(helper, player, 0, -2.5);
        double y0 = front.getY();
        double[] top = {y0};
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "The sweep was not cast");
        tickFor(helper, player, 24);
        for (int t = 8; t < 24; t++) helper.runAfterDelay(t, () -> top[0] = Math.max(top[0], front.getY()));
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(front.getHealth() < 300 && back.getHealth() < 300, "The sweep missed someone: " + front.getHealth() + ", " + back.getHealth());
            helper.assertTrue(top[0] > y0 + 0.5, "The sweep did not throw them up: " + (top[0] - y0));
            end(helper, player, front, back);
        });
    }

    @GameTest(template = "spear_arena", batch = "overlord_mountain")
    public static void fallingMountainLandsAndStrikesAllRound(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie close = dummy(helper, player, 1.5, 0), edge = dummy(helper, player, -4, 0), out = dummy(helper, player, 0, -7.5);
        OverlordSpear.landForTests(player);
        helper.assertTrue(close.getHealth() < edge.getHealth() && edge.getHealth() < 300, "The landing did not strike the centre harder: " + close.getHealth() + ", " + edge.getHealth());
        helper.assertTrue(out.getHealth() == 300, "The landing struck beyond its crater");
        end(helper, player, close, edge, out);
    }

    @GameTest(template = "spear_arena", batch = "overlord_charge", timeoutTicks = 60)
    public static void overlordsChargeCarriesThemOnThePointAndCrushesThemAgainstAWall(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        // A wall three blocks ahead.
        for (int x = 2; x <= 6; x++) for (int y = 1; y <= 3; y++) helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, 8)), Blocks.STONE.defaultBlockState(), 2);
        Zombie zombie = dummy(helper, player, 0, 1.6);
        helper.assertTrue(MartialService.castSlot(player, 3) == CastResult.CAST, "The charge was not cast");
        tickFor(helper, player, 26);
        helper.runAfterDelay(28, () -> {
            helper.assertTrue(zombie.getHealth() < 300 - 4, "The charge did not crush the zombie: " + zombie.getHealth());
            helper.assertTrue(zombie.getZ() > player.getZ(), "The zombie was not carried before the point");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "spear_arena", batch = "overlord_rampage", timeoutTicks = 80)
    public static void hundredBattlesRampageStrikesManyTimesAndThrowsAtTheEnd(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 2.4);
        float[] last = {300};
        int[] hits = {0};
        helper.assertTrue(MartialService.castSlot(player, 4) == CastResult.CAST, "The rampage was not cast");
        tickFor(helper, player, 50);
        for (int t = 1; t <= 50; t++) helper.runAfterDelay(t, () -> {
            if (zombie.getHealth() < last[0]) hits[0]++;
            last[0] = zombie.getHealth();
        });
        helper.runAfterDelay(52, () -> {
            helper.assertTrue(hits[0] >= 8, "Too few thrusts landed: " + hits[0]);
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "spear_arena", batch = "overlord_ultimate", timeoutTicks = 80)
    public static void conquerorDescendsDropsAGiantSpearThatBlastsAllRound(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        player.setXRot(60);
        Zombie zombie = dummy(helper, player, 0, 3), out = dummy(helper, player, 0, -30);
        helper.assertTrue(MartialService.castSlot(player, 5) == CastResult.CAST, "The ultimate was not cast");
        helper.assertTrue(OverlordSpear.calling(player), "The ultimate did not start");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(OverlordQiEntity.class, new AABB(player.blockPosition()).inflate(100),
                q -> q.kind() == OverlordQiEntity.SPEAR).isEmpty(), "No giant spear in the sky");
        OverlordSpear.impactForTests(player);
        helper.assertTrue(zombie.getHealth() < 300, "The giant spear's blast missed the zombie beside it");
        helper.assertTrue(out.getHealth() == 300, "The blast reached 30 blocks away");
        end(helper, player, zombie, out);
    }

    @GameTest(template = "spear_arena", batch = "overlord_weapon")
    public static void theArtNeedsASpearAndHasItsOwnFinish(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        MartialService.setMainArt(player, MartialArts.OVERLORD_SPEAR);
        helper.assertTrue(OverlordSpear.finishes(player), "No Overlord's Verdict with the spear as main style");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.NO_WEAPON, "The sweep went with a sword in hand");
        helper.assertFalse(OverlordSpear.finishes(player), "Overlord's Verdict with a sword in hand");
        helper.assertTrue(MartialService.data(player).mainArt().equals(Optional.of(MartialArts.OVERLORD_SPEAR)), "Not the main style");
        end(helper, player);
    }
}
