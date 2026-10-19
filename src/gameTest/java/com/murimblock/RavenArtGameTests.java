package com.murimblock;

import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.martial.RavenArt;
import com.murimblock.martial.RavenGateEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.qi.QiService;
import com.murimblock.world.raven.RavenBlocks;
import com.murimblock.world.raven.RavenHalls;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Raven Messenger Art (docs/combat/styles/raven-art.md): home to one's own hall and back with the crouching Closed
 * Room, the Raven Gate's fall, the Closed Room's duel, the bosses no door takes, the rifts of Between Worlds and the
 * Swallowing Gate. The GameTest server has no datapack dimension: the End, far out, stands in for the Raven Halls.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RavenArtGameTests {
    private RavenArtGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
    private static final int STAND_IN_Z = 3_100_000;
    private static final MartialArts.Technique[] ALL = {MartialArts.RAVEN_GATE, MartialArts.HALLS_STEP, MartialArts.EXCHANGE,
            MartialArts.SWALLOWING_GATE, MartialArts.CLOSED_ROOM, MartialArts.HALLS_UNFOLD};

    private static ServerLevel standIn(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(Level.END);
    }

    /** A student of the art, every technique on the bar (slots 0 to 5), an iron fan in hand, on stone, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper) {
        RavenHalls.useStandInForTests(standIn(helper), STAND_IN_Z);
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        CultivationService.setCultivation(player, MartialArts.RAVEN_REALM, CultivationStage.EARLY);
        MartialData data = MartialService.data(player).learn(MartialArts.art(MartialArts.RAVEN_ART));
        for (int i = 0; i < ALL.length; i++) data = data.assign(i, ALL[i].id());
        MartialService.setForTests(player, data);
        MartialService.setMainArt(player, MartialArts.RAVEN_ART);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.IRON_FAN.get()));
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 2);
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 2000);
        QiService.setQi(player, 2000);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
        zombie.setHealth(300);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Entity... others) {
        for (Entity e : others) if (e != null) e.discard();
        // Only this test's own state: the batches run side by side, and the stand-in halls stay for the other raven tests.
        if (RavenHalls.dueling(player)) RavenHalls.endDuel(player);
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.forget(player);
        cleanup(player);
        helper.succeed();
    }

    /** Ticks after a cast when its door has opened (the gesture played out, a little margin). */
    private static int after(float seconds) {
        return Math.round(seconds * 20) + 3;
    }

    @GameTest(template = "spear_arena", batch = "raven_home", timeoutTicks = 80)
    public static void theCrouchingClosedRoomTakesHomeToOnesOwnHallAndBack(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        double x = player.getX(), z = player.getZ();
        // No fan needed to go home.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        CastResult home = MartialService.castSlot(player, 4);
        helper.assertTrue(home == CastResult.CAST, "Could not go home: " + home);
        ServerLevel halls = standIn(helper);
        int there = after(MurimAnimations.RAVEN_ROOM_CLAP);
        tickFor(helper, player, 60);
        helper.runAfterDelay(there, () -> {
            player.hasChangedDimension();
            helper.assertTrue(player.level() == halls, "Not in the halls");
            BlockPos hall = RavenHalls.hallOf(player);
            helper.assertTrue(Math.abs(player.getZ() - hall.getZ()) < 10 && Math.abs(player.getX() - hall.getX()) < 10, "Not in one's own hall");
            helper.assertFalse(halls.getBlockState(player.blockPosition().below()).isAir(), "No floor under the feet in the hall");
            int doors = 0;
            for (BlockPos p : BlockPos.betweenClosed(hall.offset(-12, 0, -12), hall.offset(12, 6, 12)))
                if (halls.getBlockState(p).is(RavenBlocks.RAVEN_DOOR.get())) doors++;
            helper.assertTrue(doors >= 4 * 9, "The hall should have four doors of shadow, found " + doors + " blocks");
        });
        // Crouching again, once the clap is over, leads back where one came in from.
        helper.runAfterDelay(there + 16, () -> {
            MartialService.clearCooldownsForTests(player);
            CastResult back = MartialService.castSlot(player, 4);
            helper.assertTrue(back == CastResult.CAST, "Could not go back: " + back);
        });
        helper.runAfterDelay(there + 16 + there, () -> {
            player.hasChangedDimension();
            helper.assertTrue(player.level() == helper.getLevel(), "Not back in the world");
            helper.assertTrue(Math.abs(player.getX() - x) < 0.01 && Math.abs(player.getZ() - z) < 0.01, "Not back where one left");
            player.setShiftKeyDown(false);
            end(helper, player);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_gate", timeoutTicks = 140)
    public static void theRavenGateSwallowsTheTargetThenDropsItFromTenBlocks(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 2.5);
        UUID id = zombie.getUUID();
        double y = zombie.getY();
        CastResult cast = MartialService.castSlot(player, 0);
        helper.assertTrue(cast == CastResult.CAST, "The gate did not open: " + cast);
        int in = after(MurimAnimations.RAVEN_GATE_OPEN) + 4;
        tickFor(helper, player, in + 2);
        helper.runAfterDelay(in, () -> {
            helper.assertTrue(helper.getLevel().getEntity(id) == null, "The target is still in the world");
            helper.assertFalse(helper.getLevel().getEntitiesOfClass(RavenGateEntity.class, new AABB(zombie.blockPosition()).inflate(4)).isEmpty(),
                    "No door opened under the target");
        });
        helper.runAfterDelay(in + RavenArt.GATE_TICKS + 2, () -> {
            Entity back = helper.getLevel().getEntity(id);
            helper.assertTrue(back != null, "The target did not come back");
            helper.assertTrue(back.getY() > y + 7, "The target was not dropped from above: " + (back.getY() - y));
            end(helper, player, back);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_duel", timeoutTicks = 300)
    public static void theClosedRoomShutsBothInARoomUntilOneFalls(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 2.5);
        UUID id = zombie.getUUID();
        double x = player.getX(), z = player.getZ();
        CastResult cast = MartialService.castSlot(player, 4);
        helper.assertTrue(cast == CastResult.CAST, "No duel: " + cast);
        ServerLevel halls = standIn(helper);
        int in = after(MurimAnimations.RAVEN_ROOM_CLAP);
        tickFor(helper, player, in + 90);
        // Under load the clap may land late: wait (up to 70 ticks) until both are in the room.
        int[] at = {-1};
        for (int t = in; t <= in + 70; t++) {
            int now = t;
            helper.runAfterDelay(t, () -> {
                if (at[0] >= 0) return;
                player.hasChangedDimension();
                if (player.level() != halls || !RavenHalls.dueling(player)) {
                    helper.assertTrue(now < in + 70, "Both should be in the room");
                    // Another test running beside this one may clear every scheduled move (MartialCombat.clearForTests): cast again once.
                    if ((now - in) % 12 == 10 && player.level() != halls) {
                        MartialService.clearCooldownsForTests(player);
                        MartialService.castSlot(player, 4);
                    }
                    return;
                }
                at[0] = now;
                helper.assertTrue(helper.getLevel().getEntity(id) == null, "The foe stayed in the world");
                MartialService.clearCooldownsForTests(player);
                helper.assertFalse(MartialService.castSlot(player, 0) == CastResult.CAST, "The Raven Gate opened inside a duel");
                // The foe stands in the room, before the player, then falls.
                Entity foe = RavenHalls.duelFoe(player);
                helper.assertTrue(foe != null && foe.level() == halls && foe.distanceTo(player) < 10, "The foe is not in the room with the player");
                ((LivingEntity) foe).kill();
                helper.runAfterDelay(now + 4, () -> {
                    player.hasChangedDimension();
                    helper.assertFalse(RavenHalls.dueling(player), "The duel did not end with the fall");
                    helper.assertTrue(player.level() == helper.getLevel(), "The player did not come back");
                    helper.assertTrue(Math.abs(player.getX() - x) < 0.01 && Math.abs(player.getZ() - z) < 0.01, "Not back in place");
                    end(helper, player);
                });
            });
        }
    }

    @GameTest(template = "spear_arena", batch = "raven_step", timeoutTicks = 40)
    public static void hallsStepComesOutWhereOneLooks(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        double z = player.getZ();
        player.setXRot(25);
        CastResult cast = MartialService.castSlot(player, 1);
        helper.assertTrue(cast == CastResult.CAST, "No step: " + cast);
        int out = after(MurimAnimations.RAVEN_STEP_THROUGH);
        tickFor(helper, player, out + 2);
        helper.runAfterDelay(out, () -> {
            helper.assertTrue(player.getZ() > z + 2, "The player did not step through (moved " + (player.getZ() - z) + ")");
            end(helper, player);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_exchange", timeoutTicks = 40)
    public static void exchangeSwapsPlacesWithTheTarget(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 3.5);
        double pz = player.getZ(), zz = zombie.getZ();
        CastResult cast = MartialService.castSlot(player, 2);
        helper.assertTrue(cast == CastResult.CAST, "No exchange: " + cast);
        int swap = after(MurimAnimations.RAVEN_EXCHANGE_SWAP);
        tickFor(helper, player, swap + 2);
        helper.runAfterDelay(swap, () -> {
            helper.assertTrue(Math.abs(player.getZ() - zz) < 0.2 && Math.abs(zombie.getZ() - pz) < 0.2,
                    "Places not changed: player " + (player.getZ() - zz) + ", target " + (zombie.getZ() - pz));
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_unfold", timeoutTicks = 80)
    public static void theHallsUnfoldScatterEveryEnemyNear(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie a = dummy(helper, player, -2, 3), b = dummy(helper, player, 2, -3);
        CastResult cast = MartialService.castSlot(player, 5);
        helper.assertTrue(cast == CastResult.CAST, "No ultimate: " + cast);
        int call = after(MurimAnimations.RAVEN_UNFOLD_CALL);
        tickFor(helper, player, call + 2);
        helper.runAfterDelay(call, () -> {
            helper.assertTrue(helper.getLevel().getEntity(a.getUUID()) == null && helper.getLevel().getEntity(b.getUUID()) == null,
                    "Not every enemy was taken into the halls");
            end(helper, player);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_boss", timeoutTicks = 20)
    public static void noDoorTakesABoss(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        LivingEntity warlord = MurimEntities.BANDIT_WARLORD.get().create(helper.getLevel());
        warlord.moveTo(player.getX(), player.getY(), player.getZ() + 3, 180, 0);
        helper.getLevel().addFreshEntity(warlord);
        ((net.minecraft.world.entity.Mob) warlord).setNoAi(true);
        helper.assertFalse(RavenArt.canSwallow(player, warlord), "A boss can be swallowed");
        CastResult cast = MartialService.castSlot(player, 0);
        helper.assertFalse(cast == CastResult.CAST, "The Raven Gate took a boss");
        helper.assertTrue(helper.getLevel().getEntity(warlord.getUUID()) != null, "The boss left the world");
        end(helper, player, warlord);
    }

    @GameTest(template = "spear_arena", batch = "raven_rifts", timeoutTicks = 20)
    public static void threeRiftsTipTheNextStruckBehindThePlayer(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 2);
        for (int i = 1; i <= 3; i++) {
            RavenArt.onHit(player, zombie, true);
            helper.assertTrue(RavenArt.rifts(zombie) == i, "Rifts should be " + i + ", are " + RavenArt.rifts(zombie));
        }
        RavenArt.onHit(player, zombie, true);
        tickFor(helper, player, 3);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(zombie.getZ() < player.getZ(), "The target was not set behind (z " + (zombie.getZ() - player.getZ()) + ")");
            helper.assertTrue(RavenArt.rifts(zombie) == 0, "The rifts did not close");
            end(helper, player, zombie);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_swallow", timeoutTicks = 120)
    public static void theSwallowingGateTakesArrowsAndSendsThemBack(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie shooter = dummy(helper, player, 0, 4);
        CastResult cast = MartialService.castSlot(player, 3);
        helper.assertTrue(cast == CastResult.CAST, "The gate did not open: " + cast);
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        int open = after(MurimAnimations.RAVEN_SWALLOW_OPEN);
        helper.runAfterDelay(open, () -> {
            arrow.setPos(player.getX(), player.getY() + 1.5, player.getZ() + 2);
            arrow.setDeltaMovement(0, 0, -0.01);
            arrow.setNoGravity(true);
            helper.getLevel().addFreshEntity(arrow);
        });
        tickFor(helper, player, open + 70);
        helper.runAfterDelay(open + 3, () -> helper.assertFalse(arrow.isAlive(), "The gate did not swallow the arrow"));
        helper.runAfterDelay(open + RavenArt.SWALLOW_TICKS + 4, () -> {
            boolean returned = !helper.getLevel().getEntitiesOfClass(Arrow.class, new AABB(player.blockPosition()).inflate(12),
                    a -> a.getOwner() == player).isEmpty() || shooter.getHealth() < 300;
            helper.assertTrue(returned, "The arrow was not sent back");
            helper.getLevel().getEntitiesOfClass(Arrow.class, new AABB(player.blockPosition()).inflate(12)).forEach(Entity::discard);
            end(helper, player, shooter);
        });
    }

    // ---------------------------------------------------------------- the familiar and the Crow Master

    private static java.util.List<com.murimblock.martial.RavenFamiliarEntity> ravens(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(com.murimblock.martial.RavenFamiliarEntity.class, player.getBoundingBox().inflate(30),
                r -> r.ownerId().filter(player.getUUID()::equals).isPresent());
    }

    @GameTest(template = "spear_arena", batch = "raven_familiar", timeoutTicks = 80)
    public static void theRavenKeepsByItsStudentAndLeavesWhenTheArtIsLaidDown(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        tickFor(helper, player, 60);
        helper.runAfterDelay(24, () -> {
            var ravens = ravens(helper, player);
            helper.assertTrue(ravens.size() == 1, "One raven should keep by the student, found " + ravens.size());
            helper.assertTrue(ravens.getFirst().distanceTo(player) < 4, "The raven is not by its student");
            MartialService.setForTests(player, MartialService.data(player).learn(MartialArts.art(MartialArts.LONG_FIST)).withMainArt(MartialArts.LONG_FIST));
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(ravens(helper, player).isEmpty(), "The raven stayed once the art was laid down");
            end(helper, player);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_familiar_step", timeoutTicks = 80)
    public static void hallsStepSendsTheRavenAheadFirst(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        tickFor(helper, player, 60);
        helper.runAfterDelay(24, () -> {
            player.setXRot(25);
            CastResult cast = MartialService.castSlot(player, 1);
            helper.assertTrue(cast == CastResult.CAST, "No step: " + cast);
        });
        helper.runAfterDelay(24 + after(MurimAnimations.RAVEN_STEP_THROUGH), () -> {
            var ravens = ravens(helper, player);
            helper.assertTrue(!ravens.isEmpty() && ravens.getFirst().distanceTo(player) < 3, "The raven was not where the student came out");
            end(helper, player);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_master", timeoutTicks = 40)
    public static void theCrowMasterSellsTheScrollAndMeetsBlowsWithFeathers(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        com.murimblock.mob.CrowMasterEntity master = com.murimblock.mob.MurimEntities.CROW_MASTER.get().create(helper.getLevel());
        master.moveTo(player.getX(), player.getY(), player.getZ() + 3, 180, 0);
        master.setNoAi(true);
        helper.getLevel().addFreshEntity(master);
        var scroll = master.getOffers().stream().filter(o -> o.getResult().is(MurimItems.RAVEN_ART_SCROLL.get())).findFirst();
        helper.assertTrue(scroll.isPresent() && scroll.get().getCostA().getCount() == com.murimblock.mob.CrowMasterEntity.SCROLL_PRICE,
                "The Crow Master does not sell the scroll for its price");
        float health = master.getHealth();
        master.hurt(helper.getLevel().damageSources().playerAttack(player), 10);
        helper.assertTrue(master.getHealth() == health, "A blow hurt the Crow Master");
        helper.assertTrue(master.sulking(), "Struck, the Crow Master should not trade for a while");
        helper.assertTrue(master.getZ() < player.getZ(), "The Crow Master did not step out behind whoever struck him");
        // The Golden Dragon sells a guide to his tower.
        helper.assertTrue(com.murimblock.mob.GoldenDragonShop.wares().stream().anyMatch(w -> w.item().get().is(MurimItems.RAVEN_ART_GUIDE.get()) && w.forSale()),
                "The Golden Dragon does not sell the Raven Messenger Art's guide");
        end(helper, player, master);
    }
}
