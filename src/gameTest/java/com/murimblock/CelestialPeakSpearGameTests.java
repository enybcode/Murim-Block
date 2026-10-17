package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.CelestialPeakItems;
import com.murimblock.martial.CelestialPeakSpear;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.qi.QiService;
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

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Celestial Peak Spear (docs/combat/styles/celestial-peak-spear.md) on the server, through Epic Fight's real
 * animations and hits: the manual, a spear in hand, Long Reach (a hit at the point harder than one up close), and each
 * technique: Piercing Dragon through all in its line, Plum Blossom Thrusts' many hits, the sweep all round.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CelestialPeakSpearGameTests {
    private CelestialPeakSpearGameTests() { }

    /**
     * The middle of spear_arena (9 by 9 blocks): every target placed from there stays inside the test, whose chunks alone
     * are force-loaded. A zombie moved past the template into a chunk not loaded is hidden from the lookup, never hit.
     */
    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    /** A student of the spear in the middle of spear_arena: room all round for the sweep, and four blocks ahead. */
    private static ServerPlayer adept(GameTestHelper helper) {
        return adept(helper, CENTRE);
    }

    /** A student of the spear (manual read), the Alliance spear in hand, on the ground, combat on, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        ItemStack manual = new ItemStack(CelestialPeakItems.MANUAL.get());
        manual.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.ALLIANCE_SPEAR.get()));
        BlockPos p = helper.absolutePos(at);
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
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(200);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
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

    @GameTest(template = "spear_arena", batch = "spear_manual")
    public static void theManualTeachesTheArtWhoseTechniquesNeedASpear(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.knowsArt(MartialArts.CELESTIAL_PEAK_SPEAR), "The manual did not teach the spear");
        helper.assertTrue(data.mainArt().equals(Optional.of(MartialArts.CELESTIAL_PEAK_SPEAR)), "The first art read is not the main style");
        helper.assertTrue(data.slot(0).equals(Optional.of(MartialArts.PIERCING_DRAGON.id())), "Piercing Dragon is not in slot 1");
        helper.assertTrue(data.passive().equals(Optional.of(MartialArts.LONG_REACH)), "Long Reach is not the passive");
        helper.assertTrue(CelestialPeakSpear.holdsSpear(player), "The Alliance spear is not a spear");
        // A sword in hand: refused, nothing spent.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NO_WEAPON, "Piercing Dragon went with a sword");
        helper.assertTrue(QiService.getQi(player) == 100, "A refused cast spent Qi");
        end(helper, player);
    }

    @GameTest(template = "spear_arena", batch = "spear_dragon", timeoutTicks = 60)
    public static void piercingDragonLungesThroughEveryoneInItsLine(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie near = dummy(helper, player, 0, 2.0), far = dummy(helper, player, 0, 3.6);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Piercing Dragon was not cast");
        tickFor(helper, player, 30);
        helper.runAfterDelay(32, () -> {
            helper.assertTrue(near.getHealth() < 200 && far.getHealth() < 200, "The lunge did not go through both: " + near.getHealth() + ", " + far.getHealth());
            // How far it carries is the client's (a player moves himself): the spear tour measures it, about 4 blocks.
            helper.assertTrue(QiService.getQi(player) <= 100 - MartialArts.PIERCING_DRAGON.qiCost() + 1, "No Qi spent");
            end(helper, player, near, far);
        });
    }

    @GameTest(template = "spear_arena", batch = "spear_plum", timeoutTicks = 60)
    public static void plumBlossomThrustsStrikeAgainAndAgain(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        MartialService.unlock(player, MartialArts.PLUM_BLOSSOM_THRUSTS);
        MartialService.assign(player, 1, MartialArts.PLUM_BLOSSOM_THRUSTS.id());
        Zombie zombie = dummy(helper, player, 0, 2.6);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Plum Blossom Thrusts was not cast");
        float[] after = new float[2];
        tickFor(helper, player, 40);
        helper.runAfterDelay(6, () -> after[0] = zombie.getHealth());
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(zombie.getHealth() < after[0] && after[0] < 200, "The thrusts did not strike more than once: " + after[0] + " then " + zombie.getHealth());
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "spear_arena", batch = "spear_sweep", timeoutTicks = 60)
    public static void theSweepKnocksDownEveryoneAllRound(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        MartialService.unlock(player, MartialArts.THOUSAND_SOLDIER_SWEEP);
        MartialService.assign(player, 2, MartialArts.THOUSAND_SOLDIER_SWEEP.id());
        Zombie front = dummy(helper, player, 0, 2.5), back = dummy(helper, player, 0, -2.5), side = dummy(helper, player, 2.5, 0);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST, "The sweep was not cast");
        tickFor(helper, player, 30);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(front.getHealth() < 200 && back.getHealth() < 200 && side.getHealth() < 200,
                    "The sweep missed someone: " + front.getHealth() + ", " + back.getHealth() + ", " + side.getHealth());
            end(helper, player, front, back, side);
        });
    }

    @GameTest(template = "spear_arena", batch = "spear_reach", timeoutTicks = 60)
    public static void aThrustAtThePointStrikesHarderThanUpClose(GameTestHelper helper) {
        // One Piercing Dragon through a zombie up close and one at the point: the same hit, Long Reach the only difference.
        // Its path really runs 4.6 blocks ahead on the server. Epic Fight's M1 thrust ends 2.3 blocks ahead of the body
        // (the client carries the body forward with it), so it never lands 3 blocks out on its own.
        ServerPlayer player = adept(helper);
        Zombie upClose = dummy(helper, player, 0, 2.0), atPoint = dummy(helper, player, 0, 3.6);
        helper.assertTrue(CelestialPeakSpear.atReach(player, atPoint), "3.6 blocks out is not at the point");
        helper.assertTrue(!CelestialPeakSpear.atReach(player, upClose), "2.0 blocks out is at the point");
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Piercing Dragon was not cast");
        tickFor(helper, player, 30);
        helper.runAfterDelay(32, () -> {
            float near = 200 - upClose.getHealth(), far = 200 - atPoint.getHealth();
            helper.assertTrue(near > 0 && far > 0, "The lunge missed: up close " + near + ", at the point " + far);
            helper.assertTrue(far > near * 1.1, "Long Reach did not strike harder at the point: " + far + " against " + near);
            end(helper, player, upClose, atPoint);
        });
    }
}
