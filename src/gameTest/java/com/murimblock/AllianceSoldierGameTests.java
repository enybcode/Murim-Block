package com.murimblock;

import com.murimblock.item.MurimItems;
import com.murimblock.mob.AllianceSoldierEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Alliance's soldiers: each kind with the Alliance's weapon of his kind (Epic Fight takes the spear for a spear, the
 * jian for a sword), none despawns; they hunt bandits and leave players alone; the archer shoots; a soldier struck by a
 * player calls the others; they leave a few taels.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AllianceSoldierGameTests {
    private AllianceSoldierGameTests() { }

    private static AllianceSoldierEntity soldier(GameTestHelper helper, EntityType<AllianceSoldierEntity> type, BlockPos at) {
        AllianceSoldierEntity soldier = type.spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(soldier != null, "No soldier spawned: " + type);
        return soldier;
    }

    private static yesman.epicfight.world.capabilities.item.WeaponCategory category(ItemStack stack) {
        return EpicFightCapabilities.getItemStackCapability(stack).getWeaponCategory();
    }

    @GameTest(template = "foundation_arena", batch = "soldier_weapons")
    public static void eachSoldierHoldsHisWeapon(GameTestHelper helper) {
        var guard = soldier(helper, MurimEntities.ALLIANCE_SPEAR_GUARD.get(), new BlockPos(1, 1, 1));
        var disciple = soldier(helper, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), new BlockPos(3, 1, 1));
        var archer = soldier(helper, MurimEntities.ALLIANCE_ARCHER.get(), new BlockPos(1, 1, 3));
        var monk = soldier(helper, MurimEntities.ALLIANCE_WARRIOR_MONK.get(), new BlockPos(3, 1, 3));
        var sergeant = soldier(helper, MurimEntities.ALLIANCE_SERGEANT.get(), new BlockPos(2, 1, 5));
        try {
            helper.assertTrue(guard.getMainHandItem().is(MurimItems.ALLIANCE_SPEAR.get()) && category(guard.getMainHandItem()) == CapabilityItem.WeaponCategories.SPEAR,
                    "The spear guard has no spear: " + guard.getMainHandItem());
            helper.assertTrue(disciple.getMainHandItem().is(MurimItems.SNOW_PLUM_JIAN.get()) && category(disciple.getMainHandItem()) == CapabilityItem.WeaponCategories.SWORD,
                    "The disciple has no jian: " + disciple.getMainHandItem());
            helper.assertTrue(archer.getMainHandItem().is(Items.BOW), "The archer has no bow: " + archer.getMainHandItem());
            helper.assertTrue(monk.getMainHandItem().isEmpty() && monk.getOffhandItem().isEmpty(), "The monk holds something: " + monk.getMainHandItem());
            helper.assertTrue(sergeant.getMainHandItem().is(MurimItems.SNOW_PLUM_JIAN.get()) && sergeant.getOffhandItem().is(MurimItems.RATTAN_SHIELD.get()),
                    "The sergeant lacks his jian or shield: " + sergeant.getMainHandItem() + ", " + sergeant.getOffhandItem());
            for (AllianceSoldierEntity s : java.util.List.of(guard, disciple, archer, monk, sergeant)) {
                helper.assertTrue(EpicFightCapabilities.getEntityPatch(s, LivingEntityPatch.class) != null, "No Epic Fight patch on " + s.getName().getString());
                helper.assertTrue(!s.removeWhenFarAway(1.0E6) && s.isPersistenceRequired(), s.getName().getString() + " could despawn");
            }
            helper.assertTrue(monk.getMaxHealth() > disciple.getMaxHealth() && sergeant.getMaxHealth() > monk.getMaxHealth(), "Health order off");
        } finally {
            for (var s : java.util.List.of(guard, disciple, archer, monk, sergeant)) s.discard();
        }
        helper.succeed();
    }

    /** A disciple beside a player and a bandit: he goes for the bandit, never the player. */
    @GameTest(template = "foundation_arena", batch = "soldier_targets", timeoutTicks = 100)
    public static void soldiersHuntBanditsAndLeavePlayersAlone(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var disciple = soldier(helper, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), new BlockPos(2, 1, 1));
        var bandit = helper.spawn(MurimEntities.MOUNTAIN_BANDIT.get(), new BlockPos(2, 1, 4));
        bandit.setNoAi(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(disciple.getTarget() != player, "The disciple turned on the player");
            helper.assertTrue(disciple.getTarget() == bandit, "The disciple does not go for the bandit: " + disciple.getTarget() + " ai " + disciple.isEffectiveAi() + " ticks " + disciple.tickCount + " targets " + disciple.targetSelector.getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName() + (g.isRunning() ? "*" : "")).toList() + " goals " + disciple.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName() + (g.isRunning() ? "*" : "")).toList() + " sees " + disciple.getSensing().hasLineOfSight(bandit));
            disciple.discard();
            bandit.discard();
            cleanup(player);
        });
    }

    /** The archer, a zombie 4 blocks off: he draws and looses an arrow. */
    @GameTest(template = "foundation_arena", batch = "soldier_archer", timeoutTicks = 140)
    public static void theArcherShoots(GameTestHelper helper) {
        var archer = soldier(helper, MurimEntities.ALLIANCE_ARCHER.get(), new BlockPos(2, 1, 0));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        zombie.setNoAi(true);
        // Not invulnerable: an invulnerable mob is no one's enemy. Health enough to take a few arrows.
        zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
        zombie.setHealth(200);
        AABB around = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        helper.succeedWhen(() -> {
            var arrows = helper.getLevel().getEntitiesOfClass(Arrow.class, around, a -> a.getOwner() == archer);
            helper.assertTrue(!arrows.isEmpty(), "No arrow from the archer yet: target " + archer.getTarget() + ", drawing " + archer.isUsingItem() + " " + archer.getTicksUsingItem() + ", sees " + archer.getSensing().hasLineOfSight(zombie));
            helper.assertTrue(arrows.get(0).pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED, "His arrows can be picked up");
            arrows.forEach(a -> a.discard());
            archer.discard();
            zombie.discard();
        });
    }

    /** A player strikes a spear guard: he and the sergeant beside him turn on the player. */
    @GameTest(template = "foundation_arena", batch = "soldier_alert", timeoutTicks = 80)
    public static void aStruckSoldierCallsTheOthers(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var guard = soldier(helper, MurimEntities.ALLIANCE_SPEAR_GUARD.get(), new BlockPos(1, 1, 4));
        var sergeant = soldier(helper, MurimEntities.ALLIANCE_SERGEANT.get(), new BlockPos(3, 1, 4));
        helper.runAfterDelay(10, () -> guard.hurt(player.damageSources().playerAttack(player), 1.0F));
        helper.succeedWhen(() -> {
            helper.assertTrue(guard.getLastHurtByMob() == player, "Not struck yet");
            helper.assertTrue(guard.getTarget() == player, "The struck guard does not fight back");
            helper.assertTrue(sergeant.getTarget() == player, "The sergeant did not come to his help");
            guard.discard();
            sergeant.discard();
            cleanup(player);
        });
    }

    /** Killed by a player, a soldier leaves one to three taels. */
    @GameTest(template = "foundation_arena", batch = "soldier_loot")
    public static void soldiersLeaveTaels(GameTestHelper helper) {
        var monk = soldier(helper, MurimEntities.ALLIANCE_WARRIOR_MONK.get(), new BlockPos(2, 1, 2));
        ServerPlayer player = player(helper);
        try {
            var byPlayer = com.murimblock.testing.LootRolls.roll(helper.getLevel(), monk, player, 400);
            helper.assertTrue(byPlayer.onlyContains(MurimItems.SILVER_TAEL.get()) && byPlayer.rollsWith(MurimItems.SILVER_TAEL.get()) == 400,
                    "Not taels every time: " + byPlayer.rollsWith());
            helper.assertTrue(byPlayer.mostInOneRoll(MurimItems.SILVER_TAEL.get()) <= 3, "Too many taels at once");
        } finally {
            monk.discard();
            cleanup(player);
        }
        helper.succeed();
    }
}
