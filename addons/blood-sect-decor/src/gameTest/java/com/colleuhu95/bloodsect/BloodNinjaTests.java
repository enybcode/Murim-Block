package com.colleuhu95.bloodsect;

import java.util.concurrent.atomic.AtomicBoolean;
import com.colleuhu95.bloodsect.ninja.BloodNinja;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(BloodSectDecor.ID)
@PrefixGameTestTemplate(false)
public final class BloodNinjaTests {
    @GameTest(template = "patrol", timeoutTicks = 50)
    public static void idleScanDoesNotDelayANewlyAcquiredTarget(GameTestHelper test) {
        var guard = ninja(test);guard.setNoAi(true);
        var victim = test.spawnWithNoFreeWill(EntityType.COW, 7, 2, 4);
        var melee = (MeleeAttackGoal)guard.goalSelector.getAvailableGoals().stream().map(w -> w.getGoal())
            .filter(goal -> goal instanceof MeleeAttackGoal).findFirst().orElseThrow();
        test.runAfterDelay(25, () -> {
            // NoAI skips travel, which normally establishes the floor-contact flag used by navigation.
            guard.setOnGround(true);
            test.assertTrue(guard.getNavigation().createPath(victim, 0) != null, "Reaction fixture has no navigable path");
            guard.setTarget(null);
            test.assertTrue(!melee.canUse(), "Idle guard should not enter combat");
            test.runAfterDelay(1, () -> {
                guard.setTarget(victim);
                test.assertTrue(melee.canUse(), "An empty target scan delayed combat with a newly acquired visible target");
                test.succeed();
            });
        });
    }
    /** Spawned, a cultist draws one of the five outfits and an armour (or none), and keeps them through a save. */
    @GameTest(template = "patrol", timeoutTicks = 20)
    public static void aCultistDrawsItsLookAndKeepsIt(GameTestHelper test) {
        var seen = new java.util.HashSet<String>();
        for (int i = 0; i < 40; i++) {
            var n = BloodSectDecor.BLOOD_NINJA.get().spawn(test.getLevel(), test.absolutePos(new BlockPos(4, 2, 4)), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG);
            test.assertTrue(n != null && n.getOutfit() >= 0 && n.getOutfit() < BloodNinja.OUTFITS && n.getArmour() >= 0 && n.getArmour() < BloodNinja.ARMOURS, "A look out of range");
            seen.add(n.getOutfit() + "/" + n.getArmour());
            var tag = new CompoundTag();
            n.addAdditionalSaveData(tag);
            var copy = BloodSectDecor.BLOOD_NINJA.get().create(test.getLevel());
            copy.readAdditionalSaveData(tag);
            test.assertTrue(copy.getOutfit() == n.getOutfit() && copy.getArmour() == n.getArmour(), "The look was not saved: " + n.getOutfit() + "/" + n.getArmour());
            n.discard();
            copy.discard();
        }
        test.assertTrue(seen.size() >= 8, "Forty cultists drew only " + seen.size() + " looks");
        test.succeed();
    }

    private static BloodNinja ninja(GameTestHelper test) {
        test.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        test.assertBlockPresent(Blocks.SMOOTH_SANDSTONE, 4, 1, 4);
        var ninja = test.spawn(BloodSectDecor.BLOOD_NINJA.get(), 4, 2, 4);
        ninja.setGuardPost(test.absolutePos(new BlockPos(4, 2, 4)));
        ninja.getRandom().setSeed(42);
        return ninja;
    }

    @GameTest(template = "patrol", timeoutTicks = 40)
    public static void ninjaDaoKeepsCombatBalance(GameTestHelper test) {
        var ninja = ninja(test);
        ninja.setNoAi(true);
        var victim = test.spawnWithNoFreeWill(EntityType.COW, 5, 2, 4);
        test.runAfterDelay(3, () -> {
            test.assertTrue(ninja.getMaxHealth() == 24, "Ninja must have 24 health");
            test.assertTrue(Math.abs(ninja.getAttributeValue(Attributes.MOVEMENT_SPEED) - .30) < .0001, "Wrong movement speed");
            test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ninja.getMainHandItem().getItem()).toString().equals("bloodsect:blood_dao")
                && ninja.getOffhandItem().isEmpty(), "Ninja must spawn holding the blood dao");
            float health = victim.getHealth();
            test.assertTrue(ninja.doHurtTarget(victim), "Dao hit failed");
            test.assertTrue(Math.abs(health - victim.getHealth() - 4) < .001, "Dao must preserve exactly 4 damage");
            test.succeed();
        });
    }

    @GameTest(template = "patrol", timeoutTicks = 40)
    public static void savedNinjaReceivesDaoAndKeepsItsPost(GameTestHelper test) {
        var old = ninja(test);
        old.setItemSlot(EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(BloodSectDecor.BLOOD_DAGGER.get()));
        old.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1);
        var tag = new CompoundTag();
        old.saveWithoutId(tag);
        var restored = BloodSectDecor.BLOOD_NINJA.get().create(test.getLevel());
        restored.load(tag);
        test.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(restored.getMainHandItem().getItem()).toString().equals("bloodsect:blood_dao")
            && restored.getOffhandItem().isEmpty(), "Saved ninja did not receive the blood dao");
        test.assertTrue(restored.getAttributeValue(Attributes.ATTACK_DAMAGE) == 4, "Saved ninja lost its combat damage during migration");
        test.assertTrue(restored.getGuardPost().equals(old.getGuardPost()), "Migration changed the guard post");
        test.succeed();
    }

    @GameTest(template = "patrol", timeoutTicks = 40)
    public static void ninjaPostSurvivesSaveReload(GameTestHelper test) {
        var ninja = ninja(test);
        var post = ninja.getGuardPost();
        ninja.moveTo(post.getX() + 15.5, post.getY(), post.getZ() + .5);
        var tag = new CompoundTag();
        ninja.saveWithoutId(tag);
        var restored = BloodSectDecor.BLOOD_NINJA.get().create(test.getLevel());
        restored.load(tag);
        test.assertTrue(restored.getGuardPost().equals(post), "Saved guard post was replaced by current position");
        test.succeed();
    }

    @GameTest(template = "patrol", timeoutTicks = 240)
    public static void ninjaTargetsSurvivalAndIgnoresCreative(GameTestHelper test) {
        var ninja = ninja(test);
        // Vanilla's deprecated helper hardcodes isCreative=true; use a real
        // server player so changing game modes exercises the actual target rules.
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "ninja-test-player"), false);
        var player = new ServerPlayer(test.getLevel().getServer(), test.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        test.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        var pos = test.absolutePos(new BlockPos(7, 2, 4));
        player.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        player.setGameMode(GameType.CREATIVE);
        test.startSequence().thenIdle(25).thenExecute(() -> {
            test.assertTrue(ninja.getTarget() == null, "Ninja targeted a creative player");
            player.setGameMode(GameType.SPECTATOR);
        }).thenIdle(25).thenExecute(() -> {
            test.assertTrue(ninja.getTarget() == null, "Ninja targeted a spectator");
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(20);
        }).thenWaitUntil(() -> {
            // Patrol chooses a path before the player becomes vulnerable.
            // Wait for the real hit within the bounded timeout, rather than
            // assuming every valid starting path reaches melee range at tick 100.
            test.assertTrue(ninja.getTarget() == player, "Ninja did not acquire the survival player");
            test.assertTrue(player.getHealth() < 20, "Ninja did not pursue and damage the player; ninja=" + ninja.position()
                + ", player=" + player.position() + ", distance=" + ninja.distanceTo(player)
                + ", pathDone=" + ninja.getNavigation().isDone() + ", health=" + player.getHealth());
            test.assertTrue(player.getLastHurtByMob() == ninja, "Player was damaged by something other than this ninja");
        }).thenExecute(() -> player.setGameMode(GameType.CREATIVE)).thenIdle(25).thenExecute(() -> {
            test.assertTrue(ninja.getTarget() == null, "Ninja kept a player who switched to creative");
            test.getLevel().getServer().getPlayerList().remove(player);
        }).thenSucceed();
    }

    @GameTest(template = "patrol", timeoutTicks = 540)
    public static void ninjaPatrolsAndReturnsToPost(GameTestHelper test) {
        var ninja = ninja(test);
        var post = ninja.getGuardPost();
        var moved = new AtomicBoolean();
        var returned = new AtomicBoolean();
        test.onEachTick(() -> {
            double distance = ninja.position().distanceToSqr(post.getBottomCenter());
            if (ninja.tickCount < 200 && distance > 1) moved.set(true);
            if (ninja.tickCount > 205 && distance < 9) returned.set(true);
        });
        test.runAfterDelay(200, () -> {
            test.assertTrue(moved.get(), "Ninja never patrolled");
            ninja.getNavigation().stop();
            ninja.moveTo(post.getX() + 16.5, post.getY(), post.getZ() + .5);
        });
        test.runAfterDelay(500, () -> {
            test.assertTrue(returned.get(), "Ninja failed to return near its guard post");
            test.succeed();
        });
    }

    @GameTest(template = "patrol", timeoutTicks = 100)
    public static void ninjaDropsTargetBeyondPostLimit(GameTestHelper test) {
        var ninja = ninja(test);
        var post = ninja.getGuardPost();
        var victim = test.spawnWithNoFreeWill(EntityType.COW, 37, 2, 4);
        ninja.moveTo(post.getX() + 32.5, post.getY(), post.getZ() + .5);
        ninja.setTarget(victim);
        test.runAfterDelay(5, () -> test.assertTrue(ninja.getTarget() == null, "Ninja pursued beyond its post limit"));
        test.runAfterDelay(80, () -> {
            test.assertTrue(ninja.position().distanceToSqr(post.getBottomCenter()) < 30 * 30, "Ninja did not begin returning");
            test.succeed();
        });
    }

    @GameTest(template = "patrol", timeoutTicks = 40)
    public static void ninjaPursuitContinuesOutsidePatrolRadius(GameTestHelper test) {
        var ninja = ninja(test);
        var post = ninja.getGuardPost();
        var victim = test.spawnWithNoFreeWill(EntityType.COW, 28, 2, 4);
        ninja.moveTo(post.getX() + 12.5, post.getY(), post.getZ() + .5);
        ninja.setTarget(victim);
        var melee = ninja.goalSelector.getAvailableGoals().stream().map(wrapped -> wrapped.getGoal())
            .filter(goal -> goal instanceof MeleeAttackGoal).findFirst().orElseThrow();
        test.assertTrue(melee.canContinueToUse(), "Pursuit stopped outside the 10-block patrol radius, within the 28-block combat zone");
        victim.moveTo(post.getX() + 29.5, post.getY(), post.getZ() + .5);
        test.assertTrue(!melee.canContinueToUse(), "Pursuit continued beyond the 28-block combat zone");
        test.succeed();
    }
}
