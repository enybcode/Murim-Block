package com.murimblock;

import com.murimblock.integration.epicfight.CrabManEpicFight;
import com.murimblock.integration.epicfight.CrabManPatch;
import com.murimblock.mob.CrabManEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.WaterBallEntity;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The crab man's water ball volley. Absolute coordinates around (0.5, 70, 0.5), above the void: each test has its own
 * batch so they never share that spot.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WaterBallGameTests {
    private WaterBallGameTests() { }

    /** A frozen crab man at the shared spot, facing +Z. */
    private static CrabManEntity frozenCrabMan(GameTestHelper helper) {
        var man = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(2, 1, 2));
        man.setNoAi(true);
        man.setNoGravity(true);
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        man.moveTo(0.5, 70, 0.5, 0, 0);
        man.yBodyRot = 0;
        man.yHeadRot = 0;
        return man;
    }

    private static WaterBallEntity ball(CrabManEntity man, float scale, int slot) {
        WaterBallEntity ball = WaterBallEntity.charging(man, scale, slot);
        man.level().addFreshEntity(ball);
        return ball;
    }

    @GameTest(template = "foundation_arena", batch = "water_ball")
    public static void waterBallAimSolvesTheSlowArrowPath(GameTestHelper helper) {
        // Simulated with the projectile's own drag and gravity, the aimed shot must pass within 0.2 block of the target.
        for (double[] c : new double[][]{{6, 0}, {10, 1.5}, {16, -1}}) {
            Vec3 from = Vec3.ZERO, to = new Vec3(0, c[1], c[0]);
            Vec3 v = WaterBallEntity.aim(from, to, WaterBallEntity.SPEED);
            helper.assertTrue(Math.abs(Math.hypot(v.x, v.z) - WaterBallEntity.SPEED) < 1.0E-3, "Horizontal speed is not 0.8 block/tick");
            Vec3 p = from;
            double best = Double.MAX_VALUE;
            for (int tick = 0; tick < 60; tick++) {
                Vec3 next = p.add(v);
                for (int k = 0; k <= 10; k++) best = Math.min(best, p.lerp(next, k / 10.0).distanceTo(to));
                p = next;
                v = v.scale(0.99).subtract(0, WaterBallEntity.GRAVITY, 0);
            }
            helper.assertTrue(best < 0.2, "Shot at " + c[0] + " blocks misses by " + best);
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "water_ball_entity", timeoutTicks = 80)
    public static void bigWaterBallSlowsDamagesAndPushesItsTarget(GameTestHelper helper) {
        CrabManEntity man = frozenCrabMan(helper);
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.moveTo(0.5, 70, 9.5, 180, 0);
        float health = zombie.getHealth();
        WaterBallEntity ball = ball(man, 1.0F, 0);
        ball.launch(zombie, WaterBallEntity.SPEED, 0, 0);
        long start = helper.getTick();
        AtomicInteger hitTick = new AtomicInteger(-1);
        // A NoAI mob does not move by itself: the push shows as the velocity it is given.
        double[] maxPush = {0};
        for (int tick = 1; tick <= 40; tick++) helper.runAfterDelay(tick, () -> {
            if (hitTick.get() < 0 && ball.state() == WaterBallEntity.SPLASH_ENTITY) hitTick.set((int) (helper.getTick() - start));
            maxPush[0] = Math.max(maxPush[0], zombie.getDeltaMovement().z);
        });
        helper.runAfterDelay(45, () -> {
            try {
                // About 9 blocks at 0.8 block/tick with drag: 12 ticks. An arrow would take 3 to 6.
                helper.assertTrue(hitTick.get() >= 9 && hitTick.get() <= 20, "Ball reached the zombie at tick " + hitTick.get());
                helper.assertTrue(zombie.getHealth() < health, "Big ball did no damage");
                helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Big ball did not slow the zombie");
                helper.assertTrue(maxPush[0] > 0.2, "Zombie was not pushed back (velocity " + maxPush[0] + ")");
                helper.succeed();
            } finally { man.discard(); zombie.discard(); ball.discard(); }
        });
    }

    @GameTest(template = "foundation_arena", batch = "water_ball_ground", timeoutTicks = 80)
    public static void waterBallBurstsOnTheFloorThenDisappears(GameTestHelper helper) {
        for (int x = -2; x <= 3; x++) for (int z = 0; z <= 16; z++) helper.getLevel().setBlockAndUpdate(new BlockPos(x, 69, z), Blocks.STONE.defaultBlockState());
        CrabManEntity man = frozenCrabMan(helper);
        WaterBallEntity ball = ball(man, 1.0F, 0);
        ball.launch(null, WaterBallEntity.SPEED, 0, 0);
        byte[] seen = {-1};
        double[] burstY = {0};
        for (int tick = 1; tick <= 40; tick++) helper.runAfterDelay(tick, () -> {
            if (seen[0] < 0 && ball.state() >= WaterBallEntity.SPLASH_GROUND) { seen[0] = ball.state(); burstY[0] = ball.getY(); }
        });
        helper.runAfterDelay(70, () -> {
            try {
                helper.assertTrue(seen[0] == WaterBallEntity.SPLASH_GROUND, "Expected a floor burst, got state " + seen[0]);
                // Ball centre one radius above the floor top (y 70): hitbox bottom = centre - half height.
                double centre = burstY[0] + ball.getBbHeight() / 2;
                helper.assertTrue(Math.abs(centre - (70 + WaterBallEntity.RADIUS)) < 0.05, "Burst centre at y " + centre);
                helper.assertFalse(ball.isAlive(), "Ball still there after its burst");
                helper.succeed();
            } finally {
                man.discard(); ball.discard();
                for (int x = -2; x <= 3; x++) for (int z = 0; z <= 16; z++) helper.getLevel().setBlockAndUpdate(new BlockPos(x, 69, z), Blocks.AIR.defaultBlockState());
            }
        });
    }

    @GameTest(template = "foundation_arena", batch = "water_ball_wall", timeoutTicks = 80)
    public static void waterBallSplatsOnAWallAndIgnoresTheCrabs(GameTestHelper helper) {
        for (int x = -2; x <= 3; x++) for (int y = 68; y <= 74; y++) helper.getLevel().setBlockAndUpdate(new BlockPos(x, y, 5), Blocks.STONE.defaultBlockState());
        CrabManEntity man = frozenCrabMan(helper);
        var crab = helper.spawn(MurimEntities.ARMORED_CRAB.get(), new BlockPos(2, 1, 2));
        crab.setNoAi(true);
        crab.setNoGravity(true);
        crab.moveTo(0.05, 71.0, 2.5, 0, 0);
        float crabHealth = crab.getHealth();
        WaterBallEntity ball = ball(man, 1.0F, 0);
        ball.launch(null, WaterBallEntity.SPEED, 0, 0);
        byte[] seen = {-1};
        float[] yaw = {0};
        for (int tick = 1; tick <= 30; tick++) helper.runAfterDelay(tick, () -> {
            if (seen[0] < 0 && ball.state() >= WaterBallEntity.SPLASH_GROUND) { seen[0] = ball.state(); yaw[0] = ball.splashYaw(); }
        });
        helper.runAfterDelay(35, () -> {
            try {
                helper.assertTrue(seen[0] == WaterBallEntity.SPLASH_WALL, "Expected a wall burst, got state " + seen[0]);
                // Thrown toward +Z: the burst faces the wall, its -Z turned toward +Z.
                helper.assertTrue(Math.abs(Math.abs(yaw[0]) - 180) < 1, "Wall burst turned " + yaw[0]);
                helper.assertTrue(crab.getHealth() == crabHealth, "The ball hurt the crab man's own crab");
                helper.succeed();
            } finally {
                man.discard(); crab.discard(); ball.discard();
                for (int x = -2; x <= 3; x++) for (int y = 68; y <= 74; y++) helper.getLevel().setBlockAndUpdate(new BlockPos(x, y, 5), Blocks.AIR.defaultBlockState());
            }
        });
    }

    @GameTest(template = "foundation_arena", batch = "water_ball_volley", timeoutTicks = 100)
    public static void crabManCastsABigBallAndFourSmallOnesThatFollow(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        CrabManEntity man = frozenCrabMan(helper);
        player.moveTo(0.5, 70, 10.5, 180, 0);
        man.setTarget(player);
        float health = player.getHealth();
        EpicFightCapabilities.getEntityPatch(man, CrabManPatch.class).playAnimationSynchronized(CrabManEpicFight.WATER_BALL, 0);
        int[] charging = {0};
        AtomicInteger flying = new AtomicInteger();
        // 0.3 s: the volley forms; 0.82 s: it leaves.
        helper.runAfterDelay(12, () -> charging[0] = balls(man).size());
        helper.runAfterDelay(22, () -> flying.set((int) balls(man).stream().filter(b -> b.state() == WaterBallEntity.FLYING).count()));
        for (int tick = 1; tick <= 70; tick++) helper.runAfterDelay(tick, player.connection::tick);
        helper.runAfterDelay(70, () -> {
            try {
                helper.assertTrue(charging[0] == 5, "Expected 5 balls forming in the claw, got " + charging[0]);
                helper.assertTrue(flying.get() >= 1, "No ball left the claw after the release");
                helper.assertTrue(player.getHealth() < health, "Volley did not hurt the player");
                helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Volley did not slow the player");
                helper.succeed();
            } finally {
                balls(man).forEach(WaterBallEntity::discard);
                man.discard(); cleanup(player);
            }
        });
    }

    @GameTest(template = "foundation_arena", batch = "water_ball_ai", timeoutTicks = 160)
    public static void crabManThrowsWaterAtAPlayerOutOfClawReach(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var man = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(2, 1, 2));
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        man.setNoGravity(true);
        man.moveTo(0.5, 70, 0.5, 0, 0);
        player.moveTo(0.5, 70, 9.5, 180, 0);
        man.setTarget(player);
        boolean[] sawBall = {false};
        for (int tick = 1; tick <= 140; tick++) helper.runAfterDelay(tick, () -> {
            player.connection.tick();
            if (!balls(man).isEmpty()) sawBall[0] = true;
        });
        helper.runAfterDelay(140, () -> {
            try {
                helper.assertTrue(sawBall[0], "Crab man never cast its water ball at 9 blocks");
                helper.succeed();
            } finally {
                balls(man).forEach(WaterBallEntity::discard);
                man.discard(); cleanup(player);
            }
        });
    }

    private static List<WaterBallEntity> balls(LivingEntity owner) {
        return owner.level().getEntitiesOfClass(WaterBallEntity.class, new AABB(owner.blockPosition()).inflate(24), b -> b.getOwner() == owner);
    }
}
