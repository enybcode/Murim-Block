package com.murimblock;

import com.murimblock.cultivation.MeditationOrbs;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.QiMoteEntity;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.qi.QiService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The motes of Qi round a meditation (MeditationOrbs): they form round the seated body, come when their cultivator's gaze
 * rests on them (and only theirs, only while seated), fill the reserve and then condense; a golden one is worth more;
 * they fade when the meditation ends.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MeditationOrbsGameTests {
    private static ServerPlayer seated(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        com.murimblock.combat.CombatService.setCombatMode(player, false);
        player.setOnGround(true);
        helper.assertTrue(MeditationService.start(player), "Could not sit down to meditate");
        return player;
    }

    private static void end(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            MeditationService.forget(p);
            cleanup(p);
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "meditation_orbs")
    public static void aMoteFillsTheReserveThenCondensesAndAGoldenOneIsWorthMore(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        QiService.setQi(player, 50);
        QiMoteEntity mote = MeditationOrbs.spawn(player, false);
        MeditationOrbs.absorbed(player, mote);
        helper.assertTrue(Math.abs(QiService.getQi(player) - 54) < 1e-6, "A mote should fill 4 % of the reserve, now " + QiService.getQi(player));
        QiService.refillQi(player);
        double before = QiService.getQiMax(player);
        MeditationOrbs.absorbed(player, mote);
        double plain = QiService.getQiMax(player) - before;
        helper.assertTrue(plain > 0, "With a full reserve, a mote did not condense");
        QiService.refillQi(player);
        before = QiService.getQiMax(player);
        QiMoteEntity golden = MeditationOrbs.spawn(player, true);
        MeditationOrbs.absorbed(player, golden);
        double gold = QiService.getQiMax(player) - before;
        helper.assertTrue(gold > plain * 3, "A golden mote is not worth more: " + gold + " against " + plain);
        mote.discard();
        golden.discard();
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "meditation_orbs")
    public static void onlyItsCultivatorDrawsAMoteInAndOnlyWhileSeated(GameTestHelper helper) {
        ServerPlayer player = seated(helper), other = seated(helper);
        QiMoteEntity mote = MeditationOrbs.spawn(player, false);
        helper.assertTrue(!MeditationOrbs.draw(other, mote.getId()), "Another player drew it in");
        QiMoteEntity mission = MeditationOrbs.spawn(player, false);
        mission.setKind(QiMoteEntity.MISSION);
        helper.assertTrue(!MeditationOrbs.draw(player, mission.getId()), "The inner world's mote was drawn in");
        mission.moveTo(player.getX() + 30, player.getY(), player.getZ());
        mission.setKind(QiMoteEntity.MEDITATION);
        helper.assertTrue(!MeditationOrbs.draw(player, mission.getId()), "A far mote was drawn in");
        // Standing: a mote of their own does not come (one sitting down again is busy a moment with the animation).
        ServerPlayer standing = player(helper);
        QiMoteEntity theirs = MeditationOrbs.spawn(standing, false);
        helper.assertTrue(!MeditationOrbs.draw(standing, theirs.getId()), "Drawn in standing up");
        theirs.discard();
        helper.assertTrue(MeditationOrbs.draw(player, mote.getId()), "Its cultivator could not draw it in");
        helper.assertTrue(mote.pulled(), "Drawn in but not coming");
        mote.discard();
        mission.discard();
        end(helper, player, other, standing);
    }

    /** Drawn in, it flies to the chest and is absorbed; left alone, it fades when the meditation ends. */
    @GameTest(template = "foundation_arena", batch = "meditation_orbs", timeoutTicks = 120)
    public static void aDrawnMoteFliesInAndTheOthersFadeWhenTheMeditationEnds(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        QiService.setQi(player, 50);
        QiMoteEntity drawn = MeditationOrbs.spawn(player, false), left = MeditationOrbs.spawn(player, false);
        helper.assertTrue(MeditationOrbs.draw(player, drawn.getId()), "Not drawn in");
        // Waited for, not timed: a fresh entity may begin ticking a few ticks late.
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(drawn.isRemoved() && QiService.getQi(player) > 50, "The drawn mote did not fly in"))
                .thenExecute(() -> {
                    helper.assertTrue(!left.isRemoved(), "The other faded while the meditation lasts");
                    MeditationService.stop(player, false);
                })
                .thenWaitUntil(() -> helper.assertTrue(left.isRemoved(), "A mote stayed after the meditation"))
                .thenExecute(() -> {
                    MeditationService.forget(player);
                    cleanup(player);
                })
                .thenSucceed();
    }

    /** Seated three seconds, the first mote forms; then one every 3.5 s. */
    @GameTest(template = "foundation_arena", batch = "meditation_orbs")
    public static void motesFormRoundTheSeatedBody(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        int before = MeditationOrbs.motes(player).size();
        MeditationOrbs.tick(player, MeditationOrbs.FIRST_AFTER - 1);
        MeditationOrbs.tick(player, MeditationOrbs.FIRST_AFTER);
        helper.runAfterDelay(5, () -> {
            var motes = MeditationOrbs.motes(player);
            helper.assertTrue(motes.size() == before + 1, "Not one mote after three seconds: " + motes.size());
            var m = motes.get(0);
            double d = Math.hypot(m.getX() - player.getX(), m.getZ() - player.getZ());
            helper.assertTrue(d >= 1.5 && d <= 4.5, "The mote is " + d + " blocks out");
            motes.forEach(QiMoteEntity::discard);
            end(helper, player);
        });
    }
}
