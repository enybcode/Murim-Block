package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.martial.GoldenBell;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Golden Bell (docs/combat/styles/golden-bell.md), bare hands: the golden form kept at a cost of Qi, lessening every
 * blow and ringing when it ends; Iron Rebound sending a blow back; Rooted Mountain, still and healing; Bell Toll found
 * after twenty blows taken in the form; the quests.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoldenBellGameTests {
    private GoldenBellGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    /** A student of the art, bare-handed, combat on, facing south, the form in slot 0, the others in 1 and 2. */
    private static ServerPlayer student(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        MartialService.learn(player, MartialArts.art(MartialArts.GOLDEN_BELL));
        give(player, MartialArts.GOLDEN_BELL_FORM, 0);
        give(player, MartialArts.IRON_REBOUND, 1);
        give(player, MartialArts.ROOTED_MOUNTAIN, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 1000);
        QiService.setQi(player, 1000);
        return player;
    }

    private static void give(ServerPlayer player, MartialArts.Technique technique, int slot) {
        MartialService.unlock(player, technique);
        MartialService.assign(player, slot, technique.id());
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(400);
        zombie.moveTo(player.getX(), player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    /** A blow of `amount` from the zombie, whatever the player's last one. */
    private static void struck(ServerPlayer player, Zombie by, float amount) {
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(by), amount);
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        GoldenBell.stopForTests(player);
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "bell_hands")
    public static void theArtIsBareHandsOnly(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.NO_WEAPON, "The form was taken with a sword");
        helper.assertTrue(!GoldenBell.inForm(player), "In the form with a sword");
        end(helper, player);
    }

    /** In the form a blow is 40% lighter and nothing pushes; pressing again ends it, the bell striking all near. */
    @GameTest(template = "spear_arena", batch = "bell_form")
    public static void theFormLessensBlowsAndRingsWhenLeft(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie zombie = dummy(helper, player, 1.6);
        helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "The form was not taken");
        helper.assertTrue(GoldenBell.inForm(player), "Not in the form");
        helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1, "Something can still push the form");
        player.setHealth(20);
        struck(player, zombie, 10);
        float golden = 20 - player.getHealth();
        helper.assertTrue(GoldenBell.blowsInForm(player) == 1, "Blows counted: " + GoldenBell.blowsInForm(player));
        helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "The form was not left");
        helper.assertTrue(!GoldenBell.inForm(player), "Still in the form");
        helper.assertTrue(zombie.getHealth() < 400, "The bell did not strike the zombie at its end");
        helper.assertTrue(MartialService.cooldownLeft(player, MartialArts.GOLDEN_BELL_FORM.id()) > 0, "No cooldown once left");
        helper.assertTrue(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1, "The form's steadiness stayed");
        // The same blow out of the form (the realm's own shield is in both).
        player.setHealth(20);
        struck(player, zombie, 10);
        float bare = 20 - player.getHealth();
        helper.assertTrue(Math.abs(golden - bare * (1 - GoldenBell.FORM_REDUCTION)) < 0.05, "A blow took " + golden + " in the form, " + bare + " without");
        end(helper, player, zombie);
    }

    /** The form costs 4 Qi a second, and ends when the Qi runs out. */
    @GameTest(template = "spear_arena", batch = "bell_drain", timeoutTicks = 100)
    public static void theFormDrainsQiAndEndsWithout(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        MartialService.castSlot(player, 0);
        double after = QiService.getQi(player);
        tickFor(helper, player, 40);
        helper.runAfterDelay(21, () -> {
            double spent = after - QiService.getQi(player);
            helper.assertTrue(Math.abs(spent - GoldenBell.FORM_QI_PER_SECOND) < 0.5, "One second cost " + spent + " Qi");
            QiService.setQi(player, 0.1);
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(!GoldenBell.inForm(player), "The form went on without Qi");
            end(helper, player);
        });
    }

    /** Iron Rebound sends a melee blow back whole: the player untouched, the striker hurt by as much. */
    @GameTest(template = "spear_arena", batch = "bell_rebound")
    public static void ironReboundSendsTheBlowBack(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        Zombie zombie = dummy(helper, player, 1.6);
        player.setHealth(20);
        helper.assertTrue(MartialService.castSlot(player, 1) == MartialService.CastResult.CAST, "Iron Rebound was not cast");
        struck(player, zombie, 6);
        helper.assertTrue(player.getHealth() == 20, "The player was hurt: " + player.getHealth());
        float dealt = 400 - zombie.getHealth();
        helper.assertTrue(dealt >= 5, "The blow sent back did " + dealt);
        struck(player, zombie, 6);
        helper.assertTrue(player.getHealth() < 20, "A second blow was sent back too");
        end(helper, player, zombie);
    }

    /** Missed (nothing struck in its second), Iron Rebound is ready again sooner. */
    @GameTest(template = "spear_arena", batch = "bell_rebound_missed", timeoutTicks = 60)
    public static void aMissedReboundIsReadySooner(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        MartialService.castSlot(player, 1);
        helper.assertTrue(MartialService.cooldownLeft(player, MartialArts.IRON_REBOUND.id()) > GoldenBell.REBOUND_MISSED_COOLDOWN, "The full cooldown did not start");
        tickFor(helper, player, 25);
        helper.runAfterDelay(25, () -> {
            int left = MartialService.cooldownLeft(player, MartialArts.IRON_REBOUND.id());
            helper.assertTrue(left > 0 && left <= GoldenBell.REBOUND_MISSED_COOLDOWN, "Missed, its cooldown is " + left);
            end(helper, player);
        });
    }

    /** Rooted Mountain: the body cannot walk, heals each second, and is free again after four. */
    @GameTest(template = "spear_arena", batch = "bell_rooted", timeoutTicks = 140)
    public static void rootedMountainHoldsAndHeals(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        player.setHealth(10);
        helper.assertTrue(MartialService.castSlot(player, 2) == MartialService.CastResult.CAST, "Rooted Mountain was not cast");
        helper.assertTrue(GoldenBell.rooted(player), "Not rooted");
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) < 1e-6, "The rooted body can walk");
        tickFor(helper, player, 100);
        helper.runAfterDelay(45, () -> helper.assertTrue(player.getHealth() >= 11.5, "Two seconds healed to " + player.getHealth()));
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(!GoldenBell.rooted(player), "Still rooted after four seconds");
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "The body still cannot walk");
            helper.assertTrue(MartialService.cooldownLeft(player, MartialArts.ROOTED_MOUNTAIN.id()) > 0, "No cooldown once it ended");
            end(helper, player);
        });
    }

    /** Twenty blows taken in the form find Bell Toll; fifty finish Take the Blow, which teaches Iron Rebound. */
    @GameTest(template = "spear_arena", batch = "bell_toll")
    public static void blowsInTheFormFindBellTollAndFinishTheQuest(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        MartialService.learn(player, MartialArts.art(MartialArts.GOLDEN_BELL));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 1000);
        QiService.setQi(player, 1000);
        Zombie zombie = dummy(helper, player, 1.6);
        helper.assertTrue(!MartialService.data(player).hasTechnique(MartialArts.IRON_REBOUND.id()), "Iron Rebound known before its quest");
        helper.assertTrue(MartialService.castSlot(player, 0) == MartialService.CastResult.CAST, "The form was not taken");
        for (int i = 1; i <= 50; i++) {
            player.setHealth(20);
            struck(player, zombie, 1);
            if (i == GoldenBell.TOLL_HITS - 1)
                helper.assertTrue(!MartialService.data(player).hasTechnique(MartialArts.BELL_TOLL.id()), "Bell Toll found before twenty blows");
            if (i == GoldenBell.TOLL_HITS)
                helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.BELL_TOLL.id()), "Twenty blows did not find Bell Toll");
        }
        helper.assertTrue(MartialService.data(player).hasTechnique(MartialArts.IRON_REBOUND.id()), "Fifty blows did not finish Take the Blow");
        end(helper, player, zombie);
    }

    /** Bell Toll: a perfect parry in the form rings the bell at once; the form goes on. */
    @GameTest(template = "spear_arena", batch = "bell_toll_parry")
    public static void bellTollRingsOnAPerfectParry(GameTestHelper helper) {
        ServerPlayer player = student(helper);
        MartialService.unlock(player, MartialArts.BELL_TOLL);
        Zombie zombie = dummy(helper, player, 1.6);
        MartialService.castSlot(player, 0);
        MartialCombat.onPerfectParry(player, zombie);
        helper.assertTrue(zombie.getHealth() < 400, "The perfect parry did not ring the bell");
        helper.assertTrue(GoldenBell.inForm(player), "The form ended with the toll");
        end(helper, player, zombie);
    }
}
