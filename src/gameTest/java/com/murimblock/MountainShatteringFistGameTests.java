package com.murimblock;

import com.murimblock.combat.ShatteredGround;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.martial.MountainShatteringFist;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Mountain-Shattering Fist (docs/combat/styles/mountain-shattering-fist.md) on the server, through Epic Fight's real
 * animations and hits: the scroll (Qi Condensation), bare hands, and each technique: the punch that breaks the ground
 * ahead, the Earth Breaker all round, the secret Sky Breaker that throws its target up, and the ultimate's landing, a
 * crater that strikes everyone round it. The ground broken always comes back (ShatteredGround).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MountainShatteringFistGameTests {
    private MountainShatteringFistGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    /** A student of the art (scroll read at Qi Condensation), bare-handed, on stone ground, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        CultivationService.setCultivation(player, MartialArts.MOUNTAIN_FIST_REALM, CultivationStage.EARLY);
        new ItemStack(MurimItems.MOUNTAIN_SHATTERING_FIST_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // Stone a blow can break, under and round the student.
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 2);
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 1000);
        QiService.setQi(player, 1000);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        zombie.setHealth(200);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        ShatteredGround.restoreAllForTests(helper.getLevel());
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "mountain_fist_scroll")
    public static void theScrollTeachesFromQiCondensationAndNeedsBareHands(GameTestHelper helper) {
        ServerPlayer novice = player(helper);
        CultivationService.setCultivation(novice, CultivationRealm.QI_SENSING, CultivationStage.PEAK);
        new ItemStack(MurimItems.MOUNTAIN_SHATTERING_FIST_SCROLL.get()).use(novice.level(), novice, InteractionHand.MAIN_HAND);
        helper.assertFalse(MartialService.data(novice).knowsArt(MartialArts.MOUNTAIN_SHATTERING_FIST), "The scroll taught a reader below Qi Condensation");
        cleanup(novice);
        ServerPlayer player = adept(helper);
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.knowsArt(MartialArts.MOUNTAIN_SHATTERING_FIST), "The scroll did not teach the art");
        helper.assertTrue(data.mainArt().equals(Optional.of(MartialArts.MOUNTAIN_SHATTERING_FIST)), "The first art read is not the main style");
        helper.assertTrue(data.slot(0).equals(Optional.of(MartialArts.MOUNTAIN_CRUSHING_PUNCH.id())), "Mountain-Crushing Punch is not in slot 1");
        helper.assertTrue(data.passive().equals(Optional.of(MartialArts.EARTHSHAKER)), "Earthshaker is not the passive");
        helper.assertTrue(MountainShatteringFist.guardsBareHanded(player), "Bare hands do not raise the art's guard");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NO_WEAPON, "The punch went with a sword in hand");
        end(helper, player);
    }

    @GameTest(template = "spear_arena", batch = "mountain_fist_punch", timeoutTicks = 60)
    public static void thePunchStrikesTheConeAndBreaksTheGroundAhead(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie near = dummy(helper, player, 0, 2.0), cone = dummy(helper, player, 1.2, 3.6);
        int before = ShatteredGround.waiting(helper.getLevel());
        // A wall before the student, two blocks ahead.
        BlockPos wall = player.blockPosition().south(2);
        helper.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Mountain-Crushing Punch was not cast");
        tickFor(helper, player, 30);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(near.getHealth() < 200, "The punch did not strike the target ahead: " + near.getHealth());
            helper.assertTrue(cone.getHealth() < 200, "The cone did not strike the one beside: " + cone.getHealth());
            helper.assertTrue(helper.getLevel().getBlockState(wall).isAir(), "The wall ahead still stands: " + helper.getLevel().getBlockState(wall));
            helper.assertTrue(ShatteredGround.waiting(helper.getLevel()) > before, "Nothing waits to come back");
            helper.assertTrue(QiService.getQi(player) <= 1000 - MartialArts.MOUNTAIN_CRUSHING_PUNCH.qiCost() + 1, "No Qi spent");
            end(helper, player, near, cone);
        });
    }

    @GameTest(template = "spear_arena", batch = "mountain_fist_earth", timeoutTicks = 70)
    public static void theEarthBreakerStrikesAllRoundAndCraters(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        MartialService.unlock(player, MartialArts.EARTH_BREAKER);
        MartialService.assign(player, 1, MartialArts.EARTH_BREAKER.id());
        Zombie front = dummy(helper, player, 0, 2.5), back = dummy(helper, player, 0, -2.5), side = dummy(helper, player, 2.5, 0);
        BlockPos under = player.blockPosition().below().south();
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Earth Breaker was not cast");
        tickFor(helper, player, 45);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(front.getHealth() < 200 && back.getHealth() < 200 && side.getHealth() < 200,
                    "The shock missed someone: " + front.getHealth() + ", " + back.getHealth() + ", " + side.getHealth());
            helper.assertTrue(helper.getLevel().getBlockState(under).isAir(), "No crater before the student");
            ShatteredGround.restoreAllForTests(helper.getLevel());
            helper.assertTrue(helper.getLevel().getBlockState(under).is(Blocks.STONE), "The crater did not fill back");
            end(helper, player, front, back, side);
        });
    }

    @GameTest(template = "spear_arena", batch = "mountain_fist_sky", timeoutTicks = 60)
    public static void crouchingThePunchIsTheSecretSkyBreaker(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 1.6);
        player.setShiftKeyDown(true);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "The crouched punch was not cast");
        player.setShiftKeyDown(false);
        var playing = EpicFightBridge.patch(player).getAnimator().getPlayerFor(null).getRealAnimation();
        helper.assertTrue(playing == MurimAnimations.MSF_SKY_BREAKER, "Crouched, the punch is not the Sky Breaker: " + playing);
        helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.SKY_BREAKER.id()), "The secret was not revealed");
        double[] highest = {zombie.getY()};
        for (int t = 1; t <= 40; t++) helper.runAfterDelay(t, () -> highest[0] = Math.max(highest[0], zombie.getY()));
        tickFor(helper, player, 40);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(zombie.getHealth() < 200, "The uppercut missed: " + zombie.getHealth());
            // The arena's roof stops it some three blocks up; in the open it goes about eight (SKY_STEPS).
            helper.assertTrue(highest[0] > player.getY() + 2.5, "The target was not thrown up: at most " + (highest[0] - player.getY()));
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "spear_arena", batch = "mountain_fist_ultimate", timeoutTicks = 40)
    public static void theUltimatesLandingCratersAndStrikesAllRound(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        MartialService.unlock(player, MartialArts.HEAVEN_FALLING_MOUNTAIN);
        Zombie core = dummy(helper, player, 0, 1.5), edge = dummy(helper, player, 3.5, 3.5);
        MountainShatteringFist.landForTests(player, MartialArts.HEAVEN_FALLING_MOUNTAIN);
        helper.assertTrue(core.getHealth() < edge.getHealth() && edge.getHealth() < 200,
                "The heart should take more than the edge, and the edge something: " + core.getHealth() + ", " + edge.getHealth());
        helper.assertTrue(core.getRemainingFireTicks() > 0, "The meteor did not set the heart alight");
        helper.assertTrue(helper.getLevel().getBlockState(player.blockPosition().below()).isAir(), "No crater under the landing");
        end(helper, player, core, edge);
    }
}
