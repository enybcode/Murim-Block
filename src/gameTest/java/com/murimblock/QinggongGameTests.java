package com.murimblock;

import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.Qinggong;
import com.murimblock.qi.QiService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Qinggong (cultivation/Qinggong): the leap in the air, from Qi Guiding, paid in Qi, forgiving the fall, refused on the
 * ground, below the realm, without the Qi or seated; and each realm's higher jump and longer fall unhurt.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class QinggongGameTests {
    private static ServerPlayer inTheAir(GameTestHelper helper, CultivationRealm realm) {
        ServerPlayer player = player(helper);
        CultivationService.setCultivation(player, realm, CultivationStage.EARLY);
        QiService.setQiMax(player, 400);
        QiService.refillQi(player);
        player.setOnGround(false);
        player.fallDistance = 6;
        return player;
    }

    @GameTest(template = "foundation_arena", batch = "qinggong")
    public static void theLeapInTheAirIsPaidInQiAndForgivesTheFall(GameTestHelper helper) {
        ServerPlayer player = inTheAir(helper, CultivationRealm.QI_GUIDING);
        double qi = QiService.getQi(player);
        helper.assertTrue(Qinggong.leap(player), "A cultivator of Qi Guiding could not leap in the air");
        helper.assertTrue(QiService.getQi(player) == qi - Qinggong.LEAP_COST, "The leap did not cost " + Qinggong.LEAP_COST + " Qi");
        helper.assertTrue(player.fallDistance == 0, "The fall before the leap still counts: " + player.fallDistance);
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "qinggong")
    public static void noLeapOnTheGroundBelowTheRealmWithoutQiOrSeated(GameTestHelper helper) {
        ServerPlayer weak = inTheAir(helper, CultivationRealm.QI_SENSING);
        helper.assertTrue(!Qinggong.leap(weak), "Leapt at Qi Sensing");
        ServerPlayer grounded = inTheAir(helper, CultivationRealm.QI_GUIDING);
        grounded.setOnGround(true);
        helper.assertTrue(!Qinggong.leap(grounded), "Leapt from the ground");
        ServerPlayer empty = inTheAir(helper, CultivationRealm.QI_GUIDING);
        QiService.setQi(empty, Qinggong.LEAP_COST - 1);
        helper.assertTrue(!Qinggong.leap(empty) && QiService.getQi(empty) == Qinggong.LEAP_COST - 1, "Leapt without the Qi");
        ServerPlayer seated = inTheAir(helper, CultivationRealm.QI_GUIDING);
        seated.setOnGround(true);
        helper.assertTrue(MeditationService.start(seated), "Could not sit down");
        seated.setOnGround(false);
        helper.assertTrue(!Qinggong.leap(seated), "Leapt while meditating");
        MeditationService.forget(seated);
        for (ServerPlayer p : new ServerPlayer[]{weak, grounded, empty, seated}) cleanup(p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "qinggong")
    public static void eachRealmJumpsHigherAndFallsFurtherUnhurt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        CultivationService.setCultivation(player, CultivationRealm.QI_SENSING, CultivationStage.EARLY);
        double jump = player.getAttributeValue(Attributes.JUMP_STRENGTH), fall = player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.EARLY);
        double guidingJump = player.getAttributeValue(Attributes.JUMP_STRENGTH), guidingFall = player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        helper.assertTrue(Math.abs(guidingJump - jump - Qinggong.jump(CultivationRealm.QI_GUIDING)) < 1e-6, "Qi Guiding does not jump higher: " + jump + " -> " + guidingJump);
        helper.assertTrue(Math.abs(guidingFall - fall - 3) < 1e-6, "Qi Guiding does not fall 3 blocks further unhurt: " + fall + " -> " + guidingFall);
        CultivationService.setCultivation(player, CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationStage.EARLY);
        helper.assertTrue(player.getAttributeValue(Attributes.JUMP_STRENGTH) > guidingJump && player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) > guidingFall,
                "A higher realm does not jump higher or fall further");
        cleanup(player);
        helper.succeed();
    }
}
