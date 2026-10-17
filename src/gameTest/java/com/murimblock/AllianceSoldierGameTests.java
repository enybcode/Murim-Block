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
 * jian for a sword), none despawns; neutral toward peaceful people, a soldier struck by a player calls
 * the others and they stay angry for a while; the archer shoots back; they leave a few taels.
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
                helper.assertTrue(s.isPersistenceRequired(), s.getName().getString() + " placed by hand could despawn");
            }
            helper.assertTrue(monk.getMaxHealth() > disciple.getMaxHealth() && sergeant.getMaxHealth() > monk.getMaxHealth(), "Health order off");
        } finally {
            for (var s : java.util.List.of(guard, disciple, archer, monk, sergeant)) s.discard();
        }
        helper.succeed();
    }

    /** Peaceful people and ordinary animals remain safe; bandits and tigers have separate defence tests. */
    @GameTest(template = "foundation_arena", batch = "soldier_targets", timeoutTicks = 100)
    public static void soldiersDoNotAttackPeacefulPeopleFirst(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var disciple = soldier(helper, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), new BlockPos(2, 1, 1));
        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 4));
        villager.setNoAi(true);
        var horse = helper.spawn(EntityType.HORSE, new BlockPos(4, 1, 2));
        horse.setNoAi(true);
        var cow = helper.spawn(EntityType.COW, new BlockPos(0, 1, 2));
        cow.setNoAi(true);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(disciple.getTarget() == null && !disciple.isAngry(), "The disciple attacked first: " + disciple.getTarget());
            disciple.discard();
            villager.discard();
            horse.discard();
            cow.discard();
            cleanup(player);
            helper.succeed();
        });
    }

    /** The archer, struck by a zombie 4 blocks off: he draws and looses an arrow at it. */
    @GameTest(template = "foundation_arena", batch = "soldier_archer", timeoutTicks = 140)
    public static void theArcherShoots(GameTestHelper helper) {
        var archer = soldier(helper, MurimEntities.ALLIANCE_ARCHER.get(), new BlockPos(2, 1, 0));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        zombie.setNoAi(true);
        // Not invulnerable: an invulnerable mob is no one's enemy. Health enough to take a few arrows.
        zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
        zombie.setHealth(200);
        helper.runAfterDelay(10, () -> archer.hurt(helper.getLevel().damageSources().mobAttack(zombie), 0.5F));
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
            helper.assertTrue(guard.isAngryAt(player) && guard.getRemainingPersistentAngerTime() > 0 && guard.getRemainingPersistentAngerTime() <= 39 * 20,
                    "The guard's anger is not the piglins': " + guard.getRemainingPersistentAngerTime());
            guard.discard();
            sergeant.discard();
            cleanup(player);
        });
    }

    /** A patrol: the sergeant leads two spear guards, a disciple and an archer; out walking, none is kept for good. */
    @GameTest(template = "foundation_arena", batch = "soldier_patrol", timeoutTicks = 100)
    public static void aPatrolWalksTogether(GameTestHelper helper) {
        // A player beside them: out walking, they would otherwise despawn at once (the nearest player is far off).
        ServerPlayer player = player(helper);
        var patrol = com.murimblock.mob.AllianceWanderers.spawnPatrol(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)));
        helper.assertTrue(patrol.size() == 5, "A patrol of " + patrol.size());
        var leader = patrol.get(0);
        helper.assertTrue(leader.kind() == AllianceSoldierEntity.Kind.SERGEANT && leader.isPatrolLeader(), "The sergeant does not lead");
        java.util.Map<AllianceSoldierEntity.Kind, Integer> kinds = new java.util.EnumMap<>(AllianceSoldierEntity.Kind.class);
        for (var s : patrol) {
            kinds.merge(s.kind(), 1, Integer::sum);
            helper.assertTrue(!s.isPersistenceRequired() && !s.hasRestriction(), s.getName().getString() + " on patrol is kept at a post");
            if (s != leader) helper.assertTrue(leader.getUUID().equals(s.patrolLeaderId()), s.getName().getString() + " does not follow the sergeant");
        }
        helper.assertTrue(kinds.get(AllianceSoldierEntity.Kind.SPEAR_GUARD) == 2 && kinds.get(AllianceSoldierEntity.Kind.SWORD_DISCIPLE) == 1
                && kinds.get(AllianceSoldierEntity.Kind.ARCHER) == 1, "Wrong patrol: " + kinds);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(leader.patrolTarget() != null, "The leader has chosen nowhere to go");
            // The leader gone, the patrol goes on under one of the others.
            leader.discard();
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(patrol.stream().filter(s -> s != leader).anyMatch(AllianceSoldierEntity::isPatrolLeader), "The patrol has no leader left");
                patrol.forEach(s -> s.discard());
                cleanup(player);
                helper.succeed();
            });
        });
    }

    /** Monks walk the mountains, patrols the Alliance's lands but not the Cult's desert. */
    @GameTest(template = "foundation_arena", batch = "soldier_biomes")
    public static void wanderersKeepToTheirBiomes(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        java.util.function.Function<String, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> biome = name ->
                biomes.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name)));
        for (String mountain : new String[] { "cloud_sea_peaks", "jade_karst_forest", "tianshan_snowfields", "kunlun_glaciers" })
            helper.assertTrue(biome.apply(mountain).is(com.murimblock.mob.AllianceWanderers.MONK_BIOMES), "No monks in " + mountain);
        helper.assertTrue(!biome.apply("jiangnan_waterlands").is(com.murimblock.mob.AllianceWanderers.MONK_BIOMES), "Monks in the waterlands");
        helper.assertTrue(biome.apply("cloud_sea_peaks").is(com.murimblock.mob.AllianceWanderers.PATROL_BIOMES), "No patrols on the sacred peaks");
        helper.assertTrue(!biome.apply("hexi_frontier_desert").is(com.murimblock.mob.AllianceWanderers.PATROL_BIOMES), "Patrols in the Cult's desert");
        ServerPlayer player = player(helper);
        var monks = com.murimblock.mob.AllianceWanderers.spawnMonks(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), 2);
        helper.assertTrue(monks.size() == 2 && monks.stream().allMatch(m -> m.kind() == AllianceSoldierEntity.Kind.WARRIOR_MONK && !m.isPersistenceRequired() && !m.onPatrol()),
                "The wandering monks are wrong");
        monks.forEach(m -> m.discard());
        cleanup(player);
        helper.succeed();
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
