package com.murimblock;

import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.qi.QiService;
import com.murimblock.cultivation.MeditationService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Knock-down in PvP: K.O. instead of death, what a downed player can and cannot do, carry and put down, the finish and
 * its interruption. Real server players on mock connections, ticked by hand ({@code doTick} runs the full player tick and its events).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KnockdownGameTests {
    private KnockdownGameTests() { }

    /** Two players face to face, 1.5 blocks apart. The test server has PvP off: the knock-down is about PvP. */
    private static ServerPlayer[] pair(GameTestHelper helper) {
        helper.getLevel().getServer().setPvpAllowed(true);
        ServerPlayer a = player(helper), b = player(helper);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 2));
        a.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);       // looks south (+Z)
        b.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.0, 180, 0);     // looks north
        return new ServerPlayer[]{a, b};
    }

    private static void end(GameTestHelper helper, ServerPlayer... players) {
        KnockdownService.clearForTests();
        helper.getLevel().getServer().setPvpAllowed(false);
        for (ServerPlayer p : players) cleanup(p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_pvp")
    public static void aLethalHitFromAPlayerKnocksDownInsteadOfKilling(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper);
        ServerPlayer attacker = p[0], victim = p[1];
        QiService.setQiMax(victim, 1_000);
        // Worn down first: no single blow (or combo) takes a player from full health (BurstGuard).
        victim.setHealth(2);
        victim.hurt(victim.damageSources().playerAttack(attacker), 1000);
        victim.doTick();
        victim.doTick(); // the pose follows one tick after the forced pose
        helper.assertTrue(victim.isAlive() && !victim.isDeadOrDying(), "The victim died instead of falling K.O.");
        helper.assertTrue(KnockdownService.isDowned(victim), "The victim is not knocked down");
        helper.assertTrue(victim.getHealth() == 1.0F, "Downed health should be 1, got " + victim.getHealth());
        helper.assertTrue(victim.getForcedPose() == Pose.SWIMMING && victim.getPose() == Pose.SWIMMING, "A downed player must lie in the crawl pose");
        double speed = victim.getAttributeValue(Attributes.MOVEMENT_SPEED), base = victim.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(speed < base * 0.5, "Crawling should be very slow: " + speed + " of " + base);
        helper.assertTrue(victim.getAttributeValue(Attributes.JUMP_STRENGTH) == 0, "A downed player cannot jump");
        helper.assertTrue(QiService.getQiMax(victim) == 1_000, "A knock-down is not a death: no Qi lost, got " + QiService.getQiMax(victim));
        end(helper, attacker, victim);
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_mob")
    public static void aMobStillKills(GameTestHelper helper) {
        ServerPlayer victim = player(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 1, 4));
        QiService.setQiMax(victim, 500);
        victim.hurt(victim.damageSources().mobAttack(zombie), 1000);
        helper.assertTrue(victim.isDeadOrDying() && !KnockdownService.isDowned(victim), "PvE deaths are not knock-downs");
        helper.assertTrue(Math.abs(QiService.getQiMax(victim) - 450) < 1e-6, "Any death costs 10% of the maximum Qi: " + QiService.getQiMax(victim));
        zombie.discard();
        end(helper, victim);
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_limits")
    public static void aDownedPlayerIsSparedButCannotFightAndTheVoidStillKills(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper);
        ServerPlayer attacker = p[0], victim = p[1];
        KnockdownService.knockDownFor(victim, 200);
        victim.hurt(victim.damageSources().playerAttack(attacker), 6);
        helper.assertTrue(victim.getHealth() == 1.0F && victim.isAlive(), "A downed player took more damage: " + victim.getHealth());
        float before = attacker.getHealth();
        victim.attack(attacker);
        helper.assertTrue(attacker.getHealth() == before, "A downed player hit someone");
        helper.assertTrue(!MeditationService.canStart(victim), "A downed player can meditate");
        victim.hurt(victim.damageSources().fellOutOfWorld(), 1000);
        helper.assertTrue(victim.isDeadOrDying(), "The void must still kill a downed player");
        end(helper, attacker, victim);
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_recover")
    public static void aDownedPlayerGetsUpWhenTheTimeIsOver(GameTestHelper helper) {
        ServerPlayer victim = player(helper);
        KnockdownService.knockDownFor(victim, 3);
        for (int i = 0; i < 4; i++) victim.doTick();
        helper.assertTrue(!KnockdownService.isDowned(victim), "Still down after the timer");
        helper.assertTrue(victim.getHealth() >= victim.getMaxHealth() * KnockdownService.RECOVER_HEALTH - 0.01F, "Got up with too little health: " + victim.getHealth());
        victim.doTick();
        helper.assertTrue(victim.getForcedPose() == null && victim.getAttributeValue(Attributes.JUMP_STRENGTH) > 0, "Still crawling after getting up");
        end(helper, victim);
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_carry")
    public static void aDownedPlayerCanBeCarriedAndPutDownButNotEscape(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper);
        ServerPlayer carrier = p[0], victim = p[1];
        helper.assertTrue(!KnockdownService.carry(carrier, victim), "Carried a player who is not down");
        KnockdownService.knockDownFor(victim, 200);
        helper.assertTrue(KnockdownService.carry(carrier, victim), "Could not carry a downed player within reach");
        helper.assertTrue(victim.getVehicle() == carrier && KnockdownService.isCarrying(carrier) && KnockdownService.isCarried(victim), "Not riding the carrier");
        helper.assertTrue(KnockdownService.isHindered(carrier), "A carrier must not fight");
        victim.stopRiding();
        helper.assertTrue(victim.getVehicle() == carrier, "A carried player got off by themself");
        int left = KnockdownService.data(victim).ticksLeft();
        for (int i = 0; i < 5; i++) victim.doTick();
        helper.assertTrue(KnockdownService.data(victim).ticksLeft() == left, "The knock-down ran down while carried");
        helper.assertTrue(KnockdownService.release(carrier), "Could not put the player down");
        helper.assertTrue(victim.getVehicle() == null && KnockdownService.isDowned(victim), "Put down but still riding, or no longer down");
        helper.assertTrue(victim.distanceTo(carrier) < 2.0, "Put down too far away: " + victim.distanceTo(carrier));
        end(helper, carrier, victim);
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_reach")
    public static void actionsNeedReach(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper);
        ServerPlayer actor = p[0], victim = p[1];
        KnockdownService.knockDownFor(victim, 200);
        victim.teleportTo(victim.getX(), victim.getY(), victim.getZ() + 5);
        helper.assertTrue(!KnockdownService.carry(actor, victim) && !KnockdownService.execute(actor, victim), "Acted on a player 6 blocks away");
        end(helper, actor, victim);
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_finish", timeoutTicks = 100)
    public static void theFinishKillsTheDownedPlayer(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper);
        ServerPlayer executor = p[0], victim = p[1];
        QiService.setQiMax(victim, 1_000);
        victim.getInventory().clearContent();
        victim.getInventory().add(new ItemStack(Items.DIAMOND, 7));
        KnockdownService.knockDownFor(victim, 200);
        helper.assertTrue(KnockdownService.execute(executor, victim), "Could not start the finish");
        helper.assertTrue(KnockdownService.isExecuting(executor) && KnockdownService.isBeingExecuted(victim), "Finish not registered");
        helper.runAfterDelay(KnockdownService.FINISH_HIT_TICK - 4, () ->
                helper.assertTrue(victim.isAlive(), "The finish landed too early"));
        helper.runAfterDelay(KnockdownService.FINISH_HIT_TICK + 3, () -> {
            helper.assertTrue(victim.isDeadOrDying(), "The finish did not kill");
            helper.assertTrue(victim.getKillCredit() == executor, "The kill is not credited to the executor: " + victim.getKillCredit());
            // A finish is a normal death: the inventory falls to the ground and the maximum Qi shrinks for good.
            helper.assertTrue(victim.getInventory().countItem(Items.DIAMOND) == 0, "The executed player kept their inventory");
            int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(4))
                    .stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(dropped == 7, "The inventory was not dropped: " + dropped + " diamonds on the ground");
            helper.assertTrue(Math.abs(QiService.getQiMax(victim) - 900) < 1e-6, "An execution costs 10% of the maximum Qi: " + QiService.getQiMax(victim));
            end(helper, executor, victim);
        });
    }

    @GameTest(template = "foundation_arena", batch = "knockdown_interrupt", timeoutTicks = 100)
    public static void aHitOnTheExecutorBreaksTheFinish(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper);
        ServerPlayer executor = p[0], victim = p[1];
        ServerPlayer rescuer = player(helper);
        KnockdownService.knockDownFor(victim, 200);
        KnockdownService.execute(executor, victim);
        helper.runAfterDelay(5, () -> executor.hurt(executor.damageSources().playerAttack(rescuer), 2));
        helper.runAfterDelay(KnockdownService.FINISH_HIT_TICK + 6, () -> {
            helper.assertTrue(victim.isAlive() && KnockdownService.isDowned(victim), "The interrupted finish still killed");
            helper.assertTrue(!KnockdownService.isExecuting(executor), "The finish is still running");
            end(helper, executor, victim, rescuer);
        });
    }
}
