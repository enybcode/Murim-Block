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
import yesman.epicfight.gameasset.Animations;

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

    /** A student of the spear (manual read), the Alliance spear in hand, on the ground, combat on, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        ItemStack manual = new ItemStack(CelestialPeakItems.MANUAL.get());
        manual.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.ALLIANCE_SPEAR.get()));
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 0));
        // In the middle of the arena, room all round for the sweep.
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 100);
        QiService.setQi(player, 100);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
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

    @GameTest(template = "foundation_arena", batch = "spear_manual")
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

    @GameTest(template = "foundation_arena", batch = "spear_dragon", timeoutTicks = 60)
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

    @GameTest(template = "foundation_arena", batch = "spear_plum", timeoutTicks = 60)
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

    @GameTest(template = "foundation_arena", batch = "spear_sweep", timeoutTicks = 60)
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

    @GameTest(template = "foundation_arena", batch = "spear_reach", timeoutTicks = 140)
    public static void aThrustAtThePointStrikesHarderThanUpClose(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie atPoint = dummy(helper, player, 0, 3.6);
        helper.assertTrue(CelestialPeakSpear.atReach(player, atPoint), "3.6 blocks out is not at the point");
        float[] dealt = new float[2];
        Zombie[] upClose = new Zombie[1];
        // Each thrust with the swing fully charged (a blow too soon after the last is weaker in Minecraft).
        tickFor(helper, player, 120);
        helper.runAfterDelay(30, () -> EpicFightBridge.patch(player).playAnimationSynchronized(Animations.SPEAR_TWOHAND_AUTO1, 0.0F));
        helper.runAfterDelay(60, () -> {
            dealt[0] = 200 - atPoint.getHealth();
            atPoint.discard();
            upClose[0] = dummy(helper, player, 0, 1.4);
            helper.assertTrue(!CelestialPeakSpear.atReach(player, upClose[0]), "1.4 blocks out is at the point");
            EpicFightBridge.patch(player).playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE, 0.0F);
        });
        helper.runAfterDelay(90, () -> EpicFightBridge.patch(player).playAnimationSynchronized(Animations.SPEAR_TWOHAND_AUTO1, 0.0F));
        helper.runAfterDelay(120, () -> {
            dealt[1] = 200 - upClose[0].getHealth();
            helper.assertTrue(dealt[0] > 0 && dealt[1] > 0, "A thrust missed: at the point " + dealt[0] + ", up close " + dealt[1]);
            helper.assertTrue(dealt[0] > dealt[1] * 1.1, "Long Reach did not strike harder at the point: " + dealt[0] + " against " + dealt[1]);
            end(helper, player, upClose[0]);
        });
    }
}
