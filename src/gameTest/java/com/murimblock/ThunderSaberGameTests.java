package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.ThunderSaber;
import com.murimblock.martial.ThunderSaberItems;
import com.murimblock.martial.ThunderSaberMoves;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Purple Heaven Thunder Saber (docs/combat/styles/purple-heaven-thunder-saber.md). Its passive, Rolling Drums: a
 * greatsword only; strikes beat the drums, a parry twice, three to a drum, three drums; left alone they fade; a technique
 * beats a full drum for its thunder version. Then each technique, the secret, the ultimate and the finish.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThunderSaberGameTests {
    private ThunderSaberGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    /** A student of the art (its main style), the Purple Heaven Zhanmadao in hand, combat on, facing south. */
    private static ServerPlayer student(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        MartialService.learn(player, MartialArts.art(MartialArts.THUNDER_SABER));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThunderSaberItems.ZHANMADAO.get()));
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 1000);
        QiService.setQi(player, 1000);
        return player;
    }

    /** At the near edge of streak_arena (16 by 16), facing south across it. */
    private static ServerPlayer atTheEdge(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        BlockPos p = helper.absolutePos(new BlockPos(8, 1, 1));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(400);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void give(ServerPlayer player, MartialArts.Technique technique, int slot) {
        MartialService.unlock(player, technique);
        MartialService.assign(player, slot, technique.id());
    }

    private static void whenReachable(GameTestHelper helper, ServerPlayer player, Runnable then, Zombie... zombies) {
        helper.startSequence().thenWaitUntil(() -> {
            for (Zombie z : zombies) helper.assertTrue(player.level().getEntities(player, z.getBoundingBox()).contains(z), "Not reachable yet");
        }).thenExecute(then);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    // ---------------------------------------------------------------- the passive

    @GameTest(template = "spear_arena", batch = "thunder_weapon")
    public static void theArtIsTheGreatswordsOnly(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        helper.assertTrue(MartialService.data(player).mainArt().equals(Optional.of(MartialArts.THUNDER_SABER)), "The art is not the main style");
        helper.assertTrue(ThunderSaber.holdsSaber(player) && ThunderSaber.active(player), "The Purple Heaven Zhanmadao does not count as a greatsword");
        helper.assertTrue(ThunderSaberMoves.finishes(player), "Its finish is not the art's");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(!ThunderSaber.holdsSaber(player), "A sword counts as a greatsword");
        helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.NO_WEAPON, "Thunder Descends went with a sword");
        end(helper, player);
    }

    /** Three beats a drum, three drums at most; a perfect parry beats twice. */
    @GameTest(template = "spear_arena", batch = "thunder_drums")
    public static void strikesBeatTheDrumsAndAParryTwice(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie z = dummy(helper, player, 0, 1.6);
        for (int i = 1; i <= 4; i++) ThunderSaber.strikeForTests(player, z, false);
        helper.assertTrue(ThunderSaber.beats(player) == 4 && ThunderSaber.drums(player) == 1, "Four strikes: " + ThunderSaber.beats(player) + " beats");
        MartialCombat.onPerfectParry(player, z);
        helper.assertTrue(ThunderSaber.beats(player) == 6, "A perfect parry beat " + (ThunderSaber.beats(player) - 4) + " times");
        for (int i = 0; i < 10; i++) ThunderSaber.strikeForTests(player, z, false);
        helper.assertTrue(ThunderSaber.beats(player) == ThunderSaber.MAX_BEATS && ThunderSaber.drums(player) == 3, "The drums went past three: " + ThunderSaber.beats(player));
        end(helper, player, z);
    }

    /** The chain's last strike with a drum full lets go a discharge: a zombie beside the target is struck and electrified. */
    @GameTest(template = "spear_arena", batch = "thunder_discharge")
    public static void theLastStrikeOfTheChainDischarges(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie target = dummy(helper, player, 0, 1.6), beside = dummy(helper, player, 1.2, 1.8);
        whenReachable(helper, player, () -> {
            ThunderSaber.strikeForTests(player, target, true);
            helper.assertTrue(beside.getHealth() == 400, "A discharge without a full drum");
            ThunderSaber.setBeats(player, 3);
            ThunderSaber.strikeForTests(player, target, true);
            helper.assertTrue(beside.getHealth() < 400 && ThunderSaber.electrified(beside), "The discharge did not strike the zombie beside");
            end(helper, player, target, beside);
        }, target, beside);
    }

    /** Left alone 8 s, the beats fade, one every 2 s. */
    @GameTest(template = "spear_arena", batch = "thunder_fade", timeoutTicks = 240)
    public static void beatsLeftAloneFade(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie z = dummy(helper, player, 0, 1.6);
        for (int i = 0; i < 3; i++) ThunderSaber.strikeForTests(player, z, false);
        tickFor(helper, player, 210);
        helper.runAfterDelay(150, () -> helper.assertTrue(ThunderSaber.beats(player) == 3, "Beats faded before 8 s"));
        helper.runAfterDelay(205, () -> {
            helper.assertTrue(ThunderSaber.beats(player) == 1, "After 10 s: " + ThunderSaber.beats(player) + " beats, not 1");
            end(helper, player, z);
        });
    }

    // ---------------------------------------------------------------- the techniques

    /** Thunder Descends leaps to the zombie and strikes it; with a drum full, its bolt electrifies it and the drum is spent. */
    @GameTest(template = "streak_arena", batch = "thunder_descends", timeoutTicks = 80)
    public static void thunderDescendsLeapsAndStrikes(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        Zombie z = dummy(helper, player, 0, 5.5);
        double z0 = player.getZ();
        whenReachable(helper, player, () -> {
            ThunderSaber.setBeats(player, 6);
            tickFor(helper, player, 40);
            helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "Thunder Descends was not cast");
            helper.assertTrue(ThunderSaber.drums(player) == 1, "Its drum was not spent: " + ThunderSaber.drums(player));
            helper.assertTrue(ThunderSaber.unshaken(player), "The player can be staggered in the leap");
            helper.runAfterDelay(ThunderSaberMoves.LEAP_START + ThunderSaberMoves.LEAP_TICKS + 2, () -> {
                helper.assertTrue(player.getZ() - z0 > 3, "The leap went " + (player.getZ() - z0) + " blocks");
                helper.assertTrue(z.getHealth() < 400 && ThunderSaber.electrified(z), "The zombie was not struck by the thunder: " + z.getHealth());
                end(helper, player, z);
            });
        }, z);
    }

    /** Rolling Thunder strikes the zombies on both sides; with a drum, its arcs electrify them. */
    @GameTest(template = "streak_arena", batch = "thunder_rolling", timeoutTicks = 80)
    public static void rollingThunderStrikesAllRound(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        give(player, MartialArts.ROLLING_THUNDER, 1);
        Zombie left = dummy(helper, player, -2, 2), right = dummy(helper, player, 2, 2);
        whenReachable(helper, player, () -> {
            ThunderSaber.setBeats(player, 3);
            tickFor(helper, player, 40);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Rolling Thunder was not cast");
            helper.runAfterDelay(ThunderSaberMoves.ROLL_TURNS * ThunderSaberMoves.TURN_TICKS + 2, () -> {
                helper.assertTrue(left.getHealth() < 400 && right.getHealth() < 400, "A zombie beside was not struck: " + left.getHealth() + ", " + right.getHealth());
                helper.assertTrue(ThunderSaber.electrified(left) || ThunderSaber.electrified(right), "The arcs electrified nobody");
                end(helper, player, left, right);
            });
        }, left, right);
    }

    /** Heaven's Drum: a blow from the front is stopped and beats a drum; beaten, it strikes the zombie. */
    @GameTest(template = "spear_arena", batch = "thunder_drum", timeoutTicks = 60)
    public static void heavensDrumStopsABlowAndStrikesWhenBeaten(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie z = dummy(helper, player, 0, 1.6);
        whenReachable(helper, player, () -> {
            helper.assertTrue(ThunderSaberMoves.startDrumForTests(player), "Heaven's Drum did not start");
            float health = player.getHealth();
            player.hurt(player.damageSources().mobAttack(z), 6);
            helper.assertTrue(player.getHealth() == health, "The blow went through the drum");
            helper.assertTrue(ThunderSaber.beats(player) == 1, "The blow stopped did not beat the drum: " + ThunderSaber.beats(player));
            ThunderSaberMoves.endDrumForTests(player);
            helper.assertTrue(!ThunderSaberMoves.drumming(player) && z.getHealth() < 400, "The discharge did not strike: " + z.getHealth());
            end(helper, player, z);
        }, z);
    }

    /** Severing Flash, held then let go: its wave cuts both zombies on its line; one off it is spared. */
    @GameTest(template = "streak_arena", batch = "thunder_flash", timeoutTicks = 100)
    public static void severingFlashCutsAlongItsLine(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        give(player, MartialArts.SEVERING_FLASH, 1);
        Zombie near = dummy(helper, player, 0, 3), far = dummy(helper, player, 0, 7), aside = dummy(helper, player, 5, 5);
        whenReachable(helper, player, () -> {
            tickFor(helper, player, 70);
            helper.assertTrue(MartialService.aim(player, 1), "Severing Flash did not start charging");
            helper.runAfterDelay(20, () -> helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Severing Flash was not let go"));
            helper.runAfterDelay(20 + ThunderSaberMoves.FLASH_CUT_TICK + 8, () -> {
                helper.assertTrue(near.getHealth() < 400 && far.getHealth() < 400, "A zombie on the line was not cut: " + near.getHealth() + ", " + far.getHealth());
                helper.assertTrue(aside.getHealth() == 400, "A zombie off the line was cut");
                helper.assertTrue(Math.abs(QiService.getQi(player) - (1000 - MartialArts.SEVERING_FLASH.qiCost())) < 1, "Its Qi was not spent once");
                end(helper, player, near, far, aside);
            });
        }, near, far, aside);
    }

    /** Nine Heavens' Wrath: the bolts and the column fall on the zombie where the player looks; it spends all the drums. */
    @GameTest(template = "streak_arena", batch = "thunder_wrath", timeoutTicks = 140)
    public static void nineHeavensWrathFallsWhereThePlayerLooks(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        player.setXRot(35);
        give(player, MartialArts.NINE_HEAVENS_WRATH, 1);
        Vec3 at = ThunderSaberMoves.aim(player);
        Zombie z = dummy(helper, player, 0, 3);
        z.moveTo(at.x, player.getY(), at.z, 180, 0);
        whenReachable(helper, player, () -> {
            ThunderSaber.setBeats(player, 9);
            tickFor(helper, player, 110);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Nine Heavens' Wrath was not cast");
            helper.assertTrue(ThunderSaber.beats(player) == 0, "Its drums were not all spent: " + ThunderSaber.beats(player));
            float[] mid = new float[1];
            int bolts = ThunderSaberMoves.WRATH_BOLTS + 3, gap = Math.max(3, ThunderSaberMoves.WRATH_SPAN / bolts);
            int slam = ThunderSaberMoves.WRATH_FIRST + bolts * gap + 10;
            helper.runAfterDelay(slam - 2, () -> mid[0] = z.getHealth());
            helper.runAfterDelay(slam + 3, () -> {
                helper.assertTrue(mid[0] < 400 && ThunderSaber.electrified(z), "No bolt struck the zombie in the circle");
                helper.assertTrue(z.getHealth() < mid[0], "The column did not fall on it");
                end(helper, player, z);
            });
        }, z);
    }

    /** Heaven Answers: found at the thirtieth perfect parry; then a strike on the one parried fills a drum and electrifies it. */
    @GameTest(template = "spear_arena", batch = "thunder_answers")
    public static void heavenAnswersIsFoundAndFillsADrum(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie z = dummy(helper, player, 0, 1.6);
        for (int i = 0; i < 29; i++) MartialCombat.onPerfectParry(player, z);
        helper.assertTrue(!MartialService.data(player).hasTechnique(MartialArts.HEAVEN_ANSWERS.id()), "The secret was found before thirty parries");
        MartialCombat.onPerfectParry(player, z);
        helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.HEAVEN_ANSWERS.id()), "The secret was not found at thirty parries");
        ThunderSaber.setBeats(player, 0);
        MartialCombat.onPerfectParry(player, z);
        ThunderSaber.setBeats(player, 0);
        ThunderSaber.strikeForTests(player, z, false);
        helper.assertTrue(ThunderSaber.beats(player) == ThunderSaber.BEATS_PER_DRUM + 1, "The riposte did not fill a drum: " + ThunderSaber.beats(player));
        helper.assertTrue(ThunderSaber.electrified(z), "The riposte's bolt did not fall");
        end(helper, player, z);
    }

    /** The First Drum: all three drums filled twenty times opens Rolling Thunder. */
    @GameTest(template = "spear_arena", batch = "thunder_quest")
    public static void twentyFullDrumsUnlockRollingThunder(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie z = dummy(helper, player, 0, 1.6);
        for (int i = 0; i < 20; i++) {
            ThunderSaber.setBeats(player, 6);
            for (int k = 0; k < 3; k++) ThunderSaber.strikeForTests(player, z, false);
        }
        helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.ROLLING_THUNDER.id()), "Twenty full drums did not unlock Rolling Thunder");
        end(helper, player, z);
    }

    /** Thunder Sovereign's Descent: the drums full and every strike calling a bolt; at its end, a colossal bolt. */
    @GameTest(template = "spear_arena", batch = "thunder_sovereign", timeoutTicks = 240)
    public static void theDescentKeepsTheDrumsFullAndEndsInABolt(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        give(player, MartialArts.THUNDER_SOVEREIGN, 1);
        Zombie z = dummy(helper, player, 0, 2);
        whenReachable(helper, player, () -> {
            tickFor(helper, player, 200);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "The Descent was not cast");
            float[] before = new float[1];
            helper.runAfterDelay(14, () -> {
                helper.assertTrue(ThunderSaber.sovereign(player) && ThunderSaber.drums(player) == 3, "The drums are not full in the Descent");
                before[0] = z.getHealth();
                ThunderSaber.strikeForTests(player, z, false);
                helper.assertTrue(z.getHealth() < before[0], "A strike in the Descent called no bolt");
                before[0] = z.getHealth();
            });
            helper.runAfterDelay(10 + ThunderSaberMoves.SOVEREIGN_TICKS + 4, () -> {
                helper.assertTrue(!ThunderSaber.sovereign(player), "The Descent did not end");
                helper.assertTrue(z.getHealth() < before[0], "The colossal bolt did not fall");
                end(helper, player, z);
            });
        }, z);
    }
}
