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
import com.murimblock.martial.RavenArt;
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
        RavenHalls.clearForTests();
        RavenHalls.useStandInForTests(null, 0);
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "raven_home", timeoutTicks = 40)
    public static void theCrouchingClosedRoomTakesHomeToOnesOwnHallAndBack(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        double x = player.getX(), z = player.getZ();
        // No fan needed to go home.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        CastResult home = MartialService.castSlot(player, 4);
        helper.assertTrue(home == CastResult.CAST, "Could not go home: " + home);
        ServerLevel halls = standIn(helper);
        helper.assertTrue(player.level() == halls, "Not in the halls");
        player.hasChangedDimension();
        BlockPos hall = RavenHalls.hallOf(player);
        helper.assertTrue(Math.abs(player.getZ() - hall.getZ()) < 10 && Math.abs(player.getX() - hall.getX()) < 10, "Not in one's own hall");
        helper.assertFalse(halls.getBlockState(player.blockPosition().below()).isAir(), "No floor under the feet in the hall");
        int doors = 0;
        for (BlockPos p : BlockPos.betweenClosed(hall.offset(-12, 0, -12), hall.offset(12, 6, 12)))
            if (halls.getBlockState(p).is(RavenBlocks.RAVEN_DOOR.get())) doors++;
        helper.assertTrue(doors >= 4 * 9, "The hall should have four doors of shadow, found " + doors + " blocks");
        // Going home from home does nothing; crouching again leads back where one came in from.
        MartialService.clearCooldownsForTests(player);
        CastResult back = MartialService.castSlot(player, 4);
        helper.assertTrue(back == CastResult.CAST, "Could not go back: " + back);
        player.hasChangedDimension();
        helper.assertTrue(player.level() == helper.getLevel(), "Not back in the world");
        helper.assertTrue(Math.abs(player.getX() - x) < 0.01 && Math.abs(player.getZ() - z) < 0.01, "Not back where one left");
        player.setShiftKeyDown(false);
        end(helper, player);
    }

    @GameTest(template = "spear_arena", batch = "raven_gate", timeoutTicks = 120)
    public static void theRavenGateSwallowsTheTargetThenDropsItFromTenBlocks(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 2.5);
        UUID id = zombie.getUUID();
        double y = zombie.getY();
        CastResult cast = MartialService.castSlot(player, 0);
        helper.assertTrue(cast == CastResult.CAST, "The gate did not open: " + cast);
        helper.assertTrue(helper.getLevel().getEntity(id) == null, "The target is still in the world");
        helper.runAfterDelay(RavenArt.GATE_TICKS + 2, () -> {
            Entity back = helper.getLevel().getEntity(id);
            helper.assertTrue(back != null, "The target did not come back");
            helper.assertTrue(back.getY() > y + 7, "The target was not dropped from above: " + (back.getY() - y));
            end(helper, player, back);
        });
    }

    @GameTest(template = "spear_arena", batch = "raven_duel", timeoutTicks = 60)
    public static void theClosedRoomShutsBothInARoomUntilOneFalls(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 2.5);
        UUID id = zombie.getUUID();
        double x = player.getX(), z = player.getZ();
        CastResult cast = MartialService.castSlot(player, 4);
        helper.assertTrue(cast == CastResult.CAST, "No duel: " + cast);
        player.hasChangedDimension();
        ServerLevel halls = standIn(helper);
        helper.assertTrue(player.level() == halls && helper.getLevel().getEntity(id) == null, "Both should be in the room");
        helper.assertTrue(RavenHalls.dueling(player), "No duel under way");
        MartialService.clearCooldownsForTests(player);
        helper.assertFalse(MartialService.castSlot(player, 0) == CastResult.CAST, "The Raven Gate opened inside a duel");
        // The foe stands in the room, before the player, then falls.
        tickFor(helper, player, 10);
        helper.runAfterDelay(6, () -> {
            Entity foe = RavenHalls.duelFoe(player);
            helper.assertTrue(foe != null && foe.level() == halls && foe.distanceTo(player) < 10, "The foe is not in the room with the player");
            ((LivingEntity) foe).kill();
        });
        helper.runAfterDelay(10, () -> {
            player.hasChangedDimension();
            helper.assertFalse(RavenHalls.dueling(player), "The duel did not end with the fall");
            helper.assertTrue(player.level() == helper.getLevel(), "The player did not come back");
            helper.assertTrue(Math.abs(player.getX() - x) < 0.01 && Math.abs(player.getZ() - z) < 0.01, "Not back in place");
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

    @GameTest(template = "spear_arena", batch = "raven_swallow", timeoutTicks = 100)
    public static void theSwallowingGateTakesArrowsAndSendsThemBack(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie shooter = dummy(helper, player, 0, 4);
        CastResult cast = MartialService.castSlot(player, 3);
        helper.assertTrue(cast == CastResult.CAST, "The gate did not open: " + cast);
        Arrow arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), null);
        arrow.setPos(player.getX(), player.getY() + 1, player.getZ() + 2);
        arrow.setDeltaMovement(0, 0, -0.01);
        arrow.setNoGravity(true);
        helper.getLevel().addFreshEntity(arrow);
        tickFor(helper, player, 70);
        helper.runAfterDelay(3, () -> helper.assertFalse(arrow.isAlive(), "The gate did not swallow the arrow"));
        helper.runAfterDelay(66, () -> {
            boolean returned = !helper.getLevel().getEntitiesOfClass(Arrow.class, new AABB(player.blockPosition()).inflate(12),
                    a -> a.getOwner() == player).isEmpty() || shooter.getHealth() < 300;
            helper.assertTrue(returned, "The arrow was not sent back");
            helper.getLevel().getEntitiesOfClass(Arrow.class, new AABB(player.blockPosition()).inflate(12)).forEach(Entity::discard);
            end(helper, player, shooter);
        });
    }
}
