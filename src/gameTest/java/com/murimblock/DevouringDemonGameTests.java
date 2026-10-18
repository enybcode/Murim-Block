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

    /** A student of the art, bare-handed, combat on, facing south (+z), Lash in slot 0, Impaling Thorns in slot 1, Grapple in 2. */
    private static ServerPlayer student(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        MartialService.learn(player, MartialArts.art(MartialArts.DEVOURING_DEMON));
        MartialService.unlock(player, MartialArts.LASH);
        MartialService.assign(player, 0, MartialArts.LASH.id());
        MartialService.unlock(player, MartialArts.IMPALING_THORNS);
        MartialService.assign(player, 1, MartialArts.IMPALING_THORNS.id());
        MartialService.unlock(player, MartialArts.GRAPPLE);
        MartialService.assign(player, 2, MartialArts.GRAPPLE.id());
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

    /** Impaling Thorns: the foe he looks at, 7 blocks off, impaled where it stands (struck, thrown up, held); not the one aside. */
    @GameTest(template = "spear_arena", batch = "demon_thorns")
    public static void thornsImpaleTheFoeLookedAt(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie[] zombies = new Zombie[2];
        int cast = 3, hit = cast + DevouringDemon.THORNS_HIT_TICK + 2;
        ticking(helper, player, hit + 2);
        helper.runAfterDelay(cast, () -> {
            // Back to the arena's north side, so the far foe is still in it.
            player.teleportTo(player.getX(), player.getY(), player.getZ() - 3);
            zombies[0] = dummy(helper, player, 0, 7);
            zombies[1] = dummy(helper, player, 3, 2);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Impaling Thorns was not cast");
            helper.assertTrue(KaguneState.of(player).strike() == KaguneState.STRIKE_THORNS, "The kagune does not go into the ground");
        });
        helper.runAfterDelay(hit, () -> {
            Zombie far = zombies[0], aside = zombies[1];
            helper.assertTrue(far.getHealth() < 400, "The thorns missed the foe looked at");
            helper.assertTrue(far.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The impaled foe is not held");
            helper.assertTrue(aside.getHealth() == 400, "The thorns struck a foe he was not looking at");
            end(helper, player, far, aside);
        });
    }

    /** Nobody looked at: no thorns, and no Qi spent. */
    @GameTest(template = "spear_arena", batch = "demon_thorns_none")
    public static void noFoeNoThorns(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        ticking(helper, player, 4);
        helper.runAfterDelay(3, () -> {
            double qi = QiService.getQi(player);
            helper.assertTrue(MartialService.castSlot(player, 1) != MartialService.CastResult.CAST, "Impaling Thorns was cast at nobody");
            helper.assertTrue(QiService.getQi(player) == qi, "Qi was spent on nothing");
            end(helper, player);
        });
    }

    /** In the demon form the key burrows: the two nearest foes ahead struck from under the ground, not the farther one. */
    @GameTest(template = "spear_arena", batch = "demon_burrow", timeoutTicks = 200)
    public static void burrowStrikesTheTwoNearest(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        int cast = 4 + TRANSFORM_TICKS, hit = cast + DevouringDemon.BURROW_HIT_TICK + 2;
        ticking(helper, player, hit + 2);
        helper.runAfterDelay(2, () -> DevouringDemon.setFury(player, KaguneState.MAX_FURY));
        Zombie[] zombies = new Zombie[3];
        helper.runAfterDelay(cast, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.DEMON, "Not in the demon form: " + KaguneState.of(player).form());
            player.teleportTo(player.getX(), player.getY(), player.getZ() - 3);
            zombies[0] = dummy(helper, player, -2, 3);
            zombies[1] = dummy(helper, player, 2, 3);
            zombies[2] = dummy(helper, player, 0, 7);
            MartialService.clearCooldownsForTests(player);
            helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Burrow was not cast");
            helper.assertTrue(KaguneState.of(player).strike() == KaguneState.STRIKE_BURROW, "The centipedes do not burrow");
        });
        helper.runAfterDelay(hit, () -> {
            helper.assertTrue(zombies[0].getHealth() < 400 && zombies[1].getHealth() < 400, "Burrow missed the two nearest: "
                    + zombies[0].getHealth() + ", " + zombies[1].getHealth());
            helper.assertTrue(zombies[2].getHealth() == 400, "Burrow struck a third foe");
            end(helper, player, zombies);
        });
    }

    /** A zombie that stands (no will to walk) but can be moved, at (dx, dz) from the player. */
    private static Zombie standing(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = dummy(helper, player, dx, dz);
        zombie.setNoAi(false);
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
        return zombie;
    }

    /** Grapple at a foe 8 blocks off: hooked, struck, and dragged in close to him. */
    @GameTest(template = "spear_arena", batch = "demon_grapple")
    public static void grapplePullsTheFoeIn(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie[] zombie = new Zombie[1];
        int cast = 3, done = cast + DevouringDemon.GRAPPLE_HIT_TICK + DevouringDemon.GRAPPLE_PULL_TICKS;
        ticking(helper, player, done + 2);
        helper.runAfterDelay(cast, () -> {
            player.teleportTo(player.getX(), player.getY(), player.getZ() - 3);
            zombie[0] = standing(helper, player, 0, 8);
            helper.assertTrue(MartialService.castSlot(player, 2) == MartialService.CastResult.CAST, "Grapple was not cast");
        });
        helper.runAfterDelay(done, () -> {
            double d = zombie[0].distanceTo(player);
            helper.assertTrue(zombie[0].getHealth() < 400, "The hooked foe was not struck");
            helper.assertTrue(d < 4.5, "The foe was not dragged in: " + d + " blocks off");
            end(helper, player, zombie[0]);
        });
    }

    /** Grapple with no foe in sight, at a wall: the tentacle hooks into it and pulls him toward it. */
    @GameTest(template = "spear_arena", batch = "demon_grapple_wall")
    public static void grappleHooksTheWall(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        int cast = 3, pull = cast + DevouringDemon.GRAPPLE_HIT_TICK + 1;
        ticking(helper, player, pull + 2);
        helper.runAfterDelay(cast, () -> {
            BlockPos wall = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ() + 4);
            helper.getLevel().setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            helper.assertTrue(MartialService.castSlot(player, 2) == MartialService.CastResult.CAST, "Grapple did not hook the wall");
        });
        helper.runAfterDelay(pull, () -> {
            helper.assertTrue(player.getDeltaMovement().z > 0.3, "Not pulled toward the wall: " + player.getDeltaMovement());
            BlockPos wall = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ() + 4);
            helper.getLevel().setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            end(helper, player);
        });
    }

    /** Constrict, in the demon form: the foe held where it stands and crushed again and again, then thrown off. */
    @GameTest(template = "spear_arena", batch = "demon_constrict", timeoutTicks = 260)
    public static void constrictHoldsAndCrushes(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        int cast = 4 + TRANSFORM_TICKS, coiled = cast + DevouringDemon.CONSTRICT_HIT_TICK;
        int early = coiled + DevouringDemon.CONSTRICT_CRUSH_EVERY / 2 + 1, late = coiled + DevouringDemon.CONSTRICT_HOLD_TICKS - 1;
        ticking(helper, player, late + 4);
        helper.runAfterDelay(2, () -> DevouringDemon.setFury(player, KaguneState.MAX_FURY));
        Zombie[] zombie = new Zombie[1];
        float[] firstCrush = new float[1];
        double[] where = new double[3];
        helper.runAfterDelay(cast, () -> {
            helper.assertTrue(KaguneState.of(player).form() == KaguneState.Form.DEMON, "Not in the demon form: " + KaguneState.of(player).form());
            zombie[0] = standing(helper, player, 0, 4);
            MartialService.clearCooldownsForTests(player);
            helper.assertTrue(MartialService.castSlot(player, 2) == MartialService.CastResult.CAST, "Constrict was not cast");
        });
        helper.runAfterDelay(early, () -> {
            firstCrush[0] = zombie[0].getHealth();
            where[0] = zombie[0].getX(); where[1] = zombie[0].getY(); where[2] = zombie[0].getZ();
            helper.assertTrue(firstCrush[0] < 400, "The coils did not crush");
            helper.assertTrue(DevouringDemon.constricted(zombie[0]), "The foe is not held in the coils");
        });
        helper.runAfterDelay(late, () -> {
            helper.assertTrue(zombie[0].getHealth() < firstCrush[0], "Crushed only once: " + zombie[0].getHealth());
            helper.assertTrue(zombie[0].position().distanceTo(new net.minecraft.world.phys.Vec3(where[0], where[1], where[2])) < 0.5, "The held foe moved");
        });
        helper.runAfterDelay(late + 3, () -> {
            helper.assertTrue(!DevouringDemon.constricted(zombie[0]), "The coils never let go");
            end(helper, player, zombie[0]);
        });
    }
}
