package com.murimblock;

import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.martial.SevenStarsItems;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.StarPeakBuilder;
import com.murimblock.world.structure.StarPeakPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Star-Plucking Peak: its shape (the summit on the world's highest block, sheer below it, broad at the foot) and the
 * Seven Stars Sword it gives a player at Qi Liquidation who meditates three minutes on it, once.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StarPeakGameTests {
    @GameTest(template = "foundation_arena", batch = "star_peak")
    public static void thePeakRisesToTheTopOfTheWorld(GameTestHelper helper) {
        var level = helper.getLevel();
        int top = level.getMaxBuildHeight() - 1;
        var peak = new StarPeakBuilder(level, 0, 0, 120, top, 42L);
        helper.assertTrue(peak.height(0, 0) == top && peak.height(StarPeakBuilder.PLATFORM, -StarPeakBuilder.PLATFORM) == top, "The summit is not on the world's highest block");
        helper.assertTrue(peak.height(StarPeakBuilder.PLATFORM + 1, 0) < top, "The platform runs on past its edge");
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI / 8;
            int near = peak.height((int) (Math.cos(a) * 12), (int) (Math.sin(a) * 12)), mid = peak.height((int) (Math.cos(a) * 50), (int) (Math.sin(a) * 50));
            helper.assertTrue(near > 250, "Twelve blocks from the summit it is only " + near + " high");
            helper.assertTrue(mid < near && mid > 120, "The slope does not fall away: " + near + " then " + mid);
            helper.assertTrue(peak.height((int) (Math.cos(a) * (StarPeakBuilder.RADIUS + 1)), (int) (Math.sin(a) * (StarPeakBuilder.RADIUS + 1))) == Integer.MIN_VALUE,
                    "The mountain runs past its radius");
        }
        helper.succeed();
    }

    /** On the summit, three minutes of meditation at Qi Liquidation give the manual and the florett, once only. */
    @GameTest(template = "foundation_arena", batch = "star_peak")
    public static void theStarsGiveTheSevenStarsSwordOnce(GameTestHelper helper) {
        var player = player(helper);
        var data = StarPeakPlacement.get(helper.getLevel().getServer().overworld());
        // The test's player stands in the overworld: its summit, the block under its feet.
        data.record(player.blockPosition().below());
        try {
            CultivationService.setCultivation(player, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
            helper.assertTrue(StarPeakPlacement.onSummit(player), "The player is not taken to stand on the summit");
            helper.assertTrue(!StarPeakPlacement.meditating(player, StarPeakPlacement.MEDITATION_TICKS - 20), "Given before three minutes");
            helper.assertTrue(StarPeakPlacement.meditating(player, StarPeakPlacement.MEDITATION_TICKS), "Not given after three minutes");
            helper.assertTrue(player.getInventory().contains(new net.minecraft.world.item.ItemStack(SevenStarsItems.MANUAL.get()))
                    && player.getInventory().contains(new net.minecraft.world.item.ItemStack(SevenStarsItems.FLORETT.get())), "The manual or the florett is missing");
            helper.assertTrue(!StarPeakPlacement.meditating(player, StarPeakPlacement.MEDITATION_TICKS + 200), "Given a second time");
        } finally {
            data.record(null);
        }
        cleanup(player);
        helper.succeed();
    }

    /** Below Qi Liquidation, or off the summit, the stars give nothing. */
    @GameTest(template = "foundation_arena", batch = "star_peak")
    public static void theStarsAnswerNoOneBelowQiLiquidationNorOffTheSummit(GameTestHelper helper) {
        var player = player(helper);
        var data = StarPeakPlacement.get(helper.getLevel().getServer().overworld());
        try {
            data.record(player.blockPosition().below());
            CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);
            helper.assertTrue(!StarPeakPlacement.meditating(player, StarPeakPlacement.MEDITATION_TICKS), "Given below Qi Liquidation");
            CultivationService.setCultivation(player, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
            data.record(player.blockPosition().offset(40, -1, 0));
            helper.assertTrue(!StarPeakPlacement.onSummit(player) && !StarPeakPlacement.meditating(player, StarPeakPlacement.MEDITATION_TICKS), "Given off the summit");
            helper.assertTrue(!StarPeakPlacement.given(player), "Marked as given");
        } finally {
            data.record(null);
        }
        cleanup(player);
        helper.succeed();
    }

    /** The summit's platform is white jade, rimmed in polished diorite. */
    @GameTest(template = "foundation_arena", batch = "star_peak")
    public static void theSummitIsAPlatformOfJade(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
        int top = at.getY() + 3;
        var peak = new StarPeakBuilder(level, at.getX(), at.getZ(), at.getY(), top, 7L);
        helper.assertTrue(peak.blockAt(at.getX(), top, at.getZ()).is(MurimBlocks.RAW_WHITE_JADE.get()), "No jade at the summit");
        helper.assertTrue(peak.blockAt(at.getX() + StarPeakBuilder.PLATFORM, top, at.getZ()).is(net.minecraft.world.level.block.Blocks.POLISHED_DIORITE), "No rim round it");
        helper.succeed();
    }
}
