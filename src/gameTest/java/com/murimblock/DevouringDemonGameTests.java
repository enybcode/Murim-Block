package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.martial.DevouringDemon;
import com.murimblock.martial.KaguneState;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
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
 * The Devouring Demon Art (docs/combat/styles/devouring-demon-art.md), bare hands: the kagune out in combat, the Fury fed
 * by blows taken, full the transformation into the demon form on all fours, spent back to the tentacles; Lash sweeping
 * the arc ahead, and on its key in the demon form Centipede Bite, which goes for the foe he looks at and devours.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DevouringDemonGameTests {
    private DevouringDemonGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
    private static final int TRANSFORM_TICKS = (int) Math.ceil(KaguneState.TRANSFORM_SECONDS * 20) + 2;

    /** A student of the art, bare-handed, combat on, facing south (+z), Lash in slot 0. */
    private static ServerPlayer student(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        MartialService.learn(player, MartialArts.art(MartialArts.DEVOURING_DEMON));
        MartialService.unlock(player, MartialArts.LASH);
        MartialService.assign(player, 0, MartialArts.LASH.id());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setYHeadRot(0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 5000);
        QiService.setQi(player, 5000);
        return player;
    }

    /** A still zombie of 400 health at (dx, dz) from the player. */
    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(400);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    /** The player ticked every server tick from now on, for so many ticks (a mock player is not ticked by the server). */
    private static void ticking(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        DevouringDemon.stopForTests(player);
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    /** In combat mode the tentacles come out; a blow taken feeds the Fury; put away, the Fury is lost. */
    @GameTest(template = "spear_arena", batch = "demon_fury")
    public static void theKaguneComesOutAndBlowsFeedTheFury(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie zombie = dummy(helper, player, 0, 1.6);
        ticking(helper, player, 4);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.HUNGER, "The tentacles did not come out: " + KaguneState.of(player).form());
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().mobAttack(zombie), 2);
            helper.assertTrue(DevouringDemon.fury(player) == DevouringDemon.HURT_FURY, "Fury after a blow taken: " + DevouringDemon.fury(player));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.NONE && DevouringDemon.fury(player) == 0,
                    "A sword in hand did not put the kagune away: " + KaguneState.of(player));
            end(helper, player, zombie);
        });
    }

    /** Full Fury: the transformation by itself, then the demon form on all fours (lower and faster); spent, himself again. */
    @GameTest(template = "spear_arena", batch = "demon_form", timeoutTicks = 200)
    public static void fullFuryTransformsAndSpentFuryReturns(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        int full = 3, demon = full + TRANSFORM_TICKS, spent = demon + 25;
        ticking(helper, player, spent + 4);
        helper.runAfterDelay(2, () -> DevouringDemon.setFury(player, KaguneState.MAX_FURY));
        helper.runAfterDelay(full + 1, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.TRANSFORM, "Full Fury did not transform: " + KaguneState.of(player).form());
            helper.assertTrue(MartialService.castSlot(player, 0) != MartialService.CastResult.CAST, "A technique was cast in the transformation");
        });
        helper.runAfterDelay(demon, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.DEMON, "Not in the demon form after the transformation: " + KaguneState.of(player).form());
            helper.assertTrue(Math.abs(player.getBbHeight() - KaguneState.BEAST_SIZE.height()) < 0.01F, "Not on all fours: " + player.getBbHeight() + " high");
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.3, "Not faster on all fours");
            // The Fury drains in the form; at 0 he is himself again, spent.
            DevouringDemon.setFury(player, DevouringDemon.DEMON_DRAIN_PER_SECOND);
        });
        helper.runAfterDelay(spent, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.HUNGER, "Spent Fury did not end the demon form: " + KaguneState.of(player).form());
            helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Not spent after the demon form");
        });
        helper.runAfterDelay(spent + 3, () -> {
            helper.assertTrue(player.getBbHeight() > 1.7F, "Still on all fours: " + player.getBbHeight());
            end(helper, player);
        });
    }

    /** Lash: the foes in the arc ahead are struck, not the one behind. */
    @GameTest(template = "spear_arena", batch = "demon_lash")
    public static void lashStrikesTheArcAhead(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie ahead = dummy(helper, player, 0, 3), side = dummy(helper, player, 3 * Math.sin(Math.toRadians(-60)), 3 * Math.cos(Math.toRadians(60))),
                behind = dummy(helper, player, 0, -3);
        int cast = 3, hit = cast + DevouringDemon.LASH_HIT_TICK + 2;
        ticking(helper, player, hit + 2);
        helper.runAfterDelay(cast, () -> helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "Lash was not cast"));
        helper.runAfterDelay(hit, () -> {
            helper.assertTrue(ahead.getHealth() < 400 && side.getHealth() < 400, "Lash missed the arc: " + ahead.getHealth() + ", " + side.getHealth());
            helper.assertTrue(behind.getHealth() == 400, "Lash struck behind him");
            helper.assertTrue(DevouringDemon.fury(player) > 0, "Lash's hits fed no Fury");
            end(helper, player, ahead, side, behind);
        });
    }

    /** In the demon form Lash's key bites: the foe he looks at, 6 blocks off, struck, slowed; he devours some of it. */
    @GameTest(template = "spear_arena", batch = "demon_bite", timeoutTicks = 200)
    public static void inTheDemonFormLashBites(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        int cast = 4 + TRANSFORM_TICKS, hit = cast + DevouringDemon.BITE_HIT_TICK + 2;
        ticking(helper, player, hit + 2);
        helper.runAfterDelay(2, () -> DevouringDemon.setFury(player, KaguneState.MAX_FURY));
        Zombie[] zombies = new Zombie[2];
        helper.runAfterDelay(cast, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.DEMON, "Not in the demon form: " + KaguneState.of(player).form());
            // Back to the arena's north side, so the far foe is still in it.
            player.teleportTo(player.getX(), player.getY(), player.getZ() - 3);
            zombies[0] = dummy(helper, player, 0, 6);
            zombies[1] = dummy(helper, player, 3, 1);
            player.setHealth(10);
            MartialService.clearCooldownsForTests(player);
            helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "Centipede Bite was not cast");
            helper.assertTrue(KaguneState.of(player).strike() == KaguneState.STRIKE_BITE, "The kagune does not bite: " + KaguneState.of(player).strike());
        });
        helper.runAfterDelay(hit, () -> {
            Zombie far = zombies[0], aside = zombies[1];
            helper.assertTrue(far.getHealth() < 400, "The bite missed the foe 6 blocks ahead");
            helper.assertTrue(aside.getHealth() == 400, "The bite went for a foe he was not looking at");
            helper.assertTrue(far.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The bitten foe is not slowed");
            helper.assertTrue(player.getHealth() > 10, "He devoured nothing: " + player.getHealth());
            end(helper, player, far, aside);
        });
    }
}
