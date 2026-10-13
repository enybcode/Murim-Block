package com.murimblock;

import com.murimblock.integration.epicfight.CrabManArmature;
import com.murimblock.integration.epicfight.CrabManEpicFight;
import com.murimblock.integration.epicfight.CrabManPatch;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.ArmoredCrabEntity;
import com.murimblock.mob.CrabManEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.qi.QiRewardManager;
import com.murimblock.qi.QiService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/** Server checks for the crab man; uses the foundation_arena template built by FoundationGameTests. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrabManGameTests {
    private CrabManGameTests() { }

    @GameTest(template = "foundation_arena", batch = "crab_man")
    public static void crabManHasItsArmatureAnimationsShellAndOnlyAnimatedAttacks(GameTestHelper helper) {
        var man = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(2, 1, 2));
        try {
            var patch = EpicFightCapabilities.getEntityPatch(man, CrabManPatch.class);
            helper.assertTrue(patch != null && patch.getArmature() instanceof CrabManArmature, "Crab man patch or armature missing");
            CrabManArmature armature = (CrabManArmature) patch.getArmature();
            for (Joint joint : List.of(armature.torso, armature.head, armature.claw, armature.jaw)) {
                helper.assertTrue(joint != Joint.EMPTY, "Crab man joint missing from crab_man.json");
            }
            for (var accessor : List.of(CrabManEpicFight.IDLE, CrabManEpicFight.WALK, CrabManEpicFight.ATTACK_CLAW,
                    CrabManEpicFight.CLAW_SNAP, CrabManEpicFight.HURT, CrabManEpicFight.DEATH)) {
                helper.assertTrue(accessor != null && accessor.get() != null, "Crab man animation not registered: " + accessor);
            }
            helper.assertTrue(man.getMaxHealth() == 20, "Crab man should have zombie health");
            helper.assertTrue(man.getAttributeValue(Attributes.ARMOR) >= 8 && man.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 2,
                    "Crab man shell armour missing");
            helper.assertTrue(man.getAttributeValue(Attributes.ATTACK_KNOCKBACK) > 0, "Crab man has no attack knockback");
            long animated = man.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof AnimatedAttackGoal).count();
            helper.assertTrue(animated == 1, "Expected one animated attack goal, got " + animated);
            helper.assertFalse(man.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof MeleeAttackGoal
                    && !(g.getGoal() instanceof TargetChasingGoal)), "Vanilla melee goal would double the claw attacks");
            helper.succeed();
        } finally { man.discard(); }
    }

    @GameTest(template = "foundation_arena", batch = "crab_man_escort")
    public static void crabManAlwaysArrivesWithTwoArmoredCrabs(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.SAND);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        var man = MurimEntities.CRAB_MAN.get().spawn(helper.getLevel(), at, MobSpawnType.SPAWN_EGG);
        helper.assertTrue(man != null, "Crab man did not spawn");
        List<ArmoredCrabEntity> crabs = helper.getLevel().getEntitiesOfClass(ArmoredCrabEntity.class, new AABB(at).inflate(4));
        try {
            helper.assertTrue(crabs.size() == 2, "Expected two escort crabs, found " + crabs.size() + " (difficulty " + helper.getLevel().getDifficulty()
                    + ", all crabs in level " + helper.getLevel().getEntities(MurimEntities.ARMORED_CRAB.get(), e -> true).size() + ", man at " + man.blockPosition() + ")");
            helper.succeed();
        } finally {
            crabs.forEach(LivingEntity::discard);
            man.discard();
        }
    }

    /** Natural spawn: near water only, any hour, and much rarer than the armored crab in the same biomes. */
    @GameTest(template = "foundation_arena", batch = "crab_man_spawn")
    public static void crabManSpawnsNaturallyNearWaterAndRarerThanTheCrab(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos spawn = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.SAND);
        // The search reaches past this arena: clear any water another test left within it, so only ours counts.
        int r = ArmoredCrabEntity.WATER_SEARCH_RADIUS;
        for (BlockPos p : BlockPos.betweenClosed(spawn.offset(-r, -2, -r), spawn.offset(r, 1, r))) {
            if (level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        var type = MurimEntities.CRAB_MAN.get();
        helper.assertFalse(CrabManEntity.checkCrabManSpawnRules(type, level, MobSpawnType.NATURAL, spawn, level.random), "Crab man may spawn away from water");
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.WATER);
        helper.assertTrue(CrabManEntity.checkCrabManSpawnRules(type, level, MobSpawnType.NATURAL, spawn, level.random), "Crab man cannot spawn next to water");
        var beach = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.BEACH).value();
        int crabWeight = 0, manWeight = 0, manMax = 0;
        for (var data : beach.getMobSettings().getMobs(MobCategory.MONSTER).unwrap()) {
            if (data.type == MurimEntities.ARMORED_CRAB.get()) crabWeight = data.getWeight().asInt();
            if (data.type == type) { manWeight = data.getWeight().asInt(); manMax = data.maxCount; }
        }
        helper.assertTrue(manWeight > 0 && manWeight < crabWeight, "Beach spawn weights: crab man " + manWeight + ", crab " + crabWeight);
        helper.assertTrue(manMax == 1, "Crab man should spawn alone (with its own escort), max group " + manMax);
        helper.succeed();
    }

    /** The real path: a player uses the spawn egg on a block (Enzo saw it arrive alone this way). */
    @GameTest(template = "foundation_arena", batch = "crab_man_egg")
    public static void crabManSpawnEggBringsTwoArmoredCrabs(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
        ServerPlayer player = player(helper);
        try {
            player.setGameMode(GameType.CREATIVE);
            var egg = new ItemStack(MurimItems.CRAB_MAN_SPAWN_EGG.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, egg);
            BlockPos floor = helper.absolutePos(new BlockPos(2, 0, 2));
            var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
            var result = egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            AABB area = new AABB(floor).inflate(5);
            var men = helper.getLevel().getEntitiesOfClass(CrabManEntity.class, area);
            var crabs = helper.getLevel().getEntitiesOfClass(ArmoredCrabEntity.class, area);
            try {
                helper.assertTrue(result.consumesAction() && men.size() == 1, "Egg did not spawn one crab man: " + result + ", " + men.size());
                helper.assertTrue(crabs.size() == 2, "Spawn egg brought " + crabs.size() + " crabs instead of 2");
                helper.succeed();
            } finally { crabs.forEach(LivingEntity::discard); men.forEach(LivingEntity::discard); }
        } finally { cleanup(player); }
    }

    /** Even with no room at all around it, it still arrives with its two crabs. */
    @GameTest(template = "foundation_arena", batch = "crab_man_escort_cramped")
    public static void crabManStillGetsTwoCrabsWhenCramped(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            if (x != 2 || z != 2) { helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE); helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE); }
        }
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        var man = MurimEntities.CRAB_MAN.get().spawn(helper.getLevel(), at, MobSpawnType.SPAWN_EGG);
        var crabs = helper.getLevel().getEntitiesOfClass(ArmoredCrabEntity.class, new AABB(at).inflate(4));
        try {
            helper.assertTrue(crabs.size() == 2, "Cramped crab man got " + crabs.size() + " crabs instead of 2");
            helper.succeed();
        } finally { crabs.forEach(LivingEntity::discard); if (man != null) man.discard(); }
    }

    @GameTest(template = "foundation_arena", batch = "crab_man", timeoutTicks = 60)
    public static void crabManDoesNotBurnInDaylightAndIsFasterInWater(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setDayTime(6000);
        var dry = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.WATER);
        var wet = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(3, 1, 3));
        dry.setNoAi(true);
        // Held in the water: with its AI it sometimes walked out before the check. The speed comes from aiStep, which
        // runs without AI too.
        wet.setNoAi(true);
        helper.runAfterDelay(40, () -> {
            try {
                helper.assertFalse(dry.isOnFire(), "Crab man burns in daylight");
                helper.assertTrue(wet.isInWater() && wet.hasEffect(MobEffects.MOVEMENT_SPEED)
                        && wet.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 0, "Crab man has no Speed I in water");
                helper.assertFalse(dry.hasEffect(MobEffects.MOVEMENT_SPEED), "Crab man keeps Speed out of water");
                helper.assertTrue(wet.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) > 0, "Speed would not apply while swimming");
                helper.succeed();
            } finally { dry.discard(); wet.discard(); }
        });
    }

    @GameTest(template = "foundation_arena", batch = "crab_man")
    public static void crabManGivesTenExperienceTenQiAndNoLoot(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var man = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(1, 1, 1));
        man.setNoAi(true);
        try {
            helper.assertTrue(QiRewardManager.rewardTarget(man.getType()).baseReward() == 10, "Crab man Qi reward is not 10");
            helper.assertTrue(man.getExperienceReward(helper.getLevel(), player) == 10, "Crab man experience is not 10");
            AtomicInteger deaths = new AtomicInteger();
            Consumer<LivingDropsEvent> observer = event -> {
                if (event.getEntity() != man) return;
                helper.assertTrue(event.getDrops().isEmpty(), "Crab man dropped loot");
                deaths.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(observer);
            try {
                man.hurt(player.damageSources().playerAttack(player), 1000);
                helper.assertTrue(!man.isAlive() && deaths.get() == 1, "Crab man death pipeline did not run");
            } finally { NeoForge.EVENT_BUS.unregister(observer); }
            helper.succeed();
        } finally { man.discard(); cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "crab_man_claw", timeoutTicks = 80)
    public static void crabManClawSwingHitsOnceAndPushesThePlayer(GameTestHelper helper) {
        clawHit(helper, CrabManEpicFight.ATTACK_CLAW, 1.4, 12, 50); // contact window 0.66-0.76 s
    }

    @GameTest(template = "foundation_arena", batch = "crab_man_jab", timeoutTicks = 80)
    public static void crabManJabHitsOnceAndThrowsThePlayerBack(GameTestHelper helper) {
        clawHit(helper, CrabManEpicFight.CLAW_SNAP, 1.7, 13, 50); // contact window 0.74-0.82 s
    }

    /**
     * Plays one clip on a frozen crab man facing a player and checks: no hit during the wind-up, exactly one hit,
     * and the player pushed away from it. Absolute coordinates: each attack test needs its own batch.
     */
    private static void clawHit(GameTestHelper helper, AnimationAccessor<AttackAnimation> attack, double distance, int beforeContactTick, int endTick) {
        ServerPlayer player = player(helper);
        var man = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(2, 1, 2));
        man.setNoAi(true);
        man.setNoGravity(true);
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        man.moveTo(0.5, 70, 0.5, 0, 0);
        man.yBodyRot = 0;
        man.yHeadRot = 0;
        player.moveTo(0.5, 70, 0.5 + distance, 180, 0);
        man.setTarget(player);
        player.connection.tick();
        var patch = EpicFightCapabilities.getEntityPatch(man, CrabManPatch.class);
        AtomicInteger contacts = new AtomicInteger();
        AtomicInteger firstHitTick = new AtomicInteger(-1);
        StringBuilder hitInfo = new StringBuilder();
        long startTick = helper.getTick();
        Consumer<LivingDamageEvent.Post> observer = event -> {
            if (event.getEntity() == player && event.getSource().getEntity() == man) {
                if (contacts.incrementAndGet() == 1) {
                    firstHitTick.set((int) (helper.getTick() - startTick));
                    var player0 = EpicFightCapabilities.getEntityPatch(man, CrabManPatch.class).getAnimator().getPlayerFor(null);
                    hitInfo.append("source ").append(event.getSource().getMsgId()).append(" anim ")
                            .append(player0 == null ? "?" : player0.getAnimation() + " t=" + player0.getElapsedTime());
                }
            }
        };
        NeoForge.EVENT_BUS.addListener(observer);
        float health = player.getHealth();
        // A server-side player is moved by its client, so knockback shows as the velocity it is given.
        double[] maxPush = {0};
        try {
            patch.playAnimationSynchronized(attack, 0);
            for (int tick = 1; tick <= endTick; tick++) helper.runAfterDelay(tick, () -> maxPush[0] = Math.max(maxPush[0], player.getDeltaMovement().z));
            // Recorded, not asserted here: failing mid-test would skip the cleanup and leave entities for the next test.
            AtomicInteger earlyHits = new AtomicInteger();
            helper.runAfterDelay(beforeContactTick, () -> earlyHits.set(contacts.get()));
            for (int tick = 1; tick <= endTick; tick++) helper.runAfterDelay(tick, player.connection::tick);
            helper.runAfterDelay(endTick, () -> {
                try {
                    helper.assertTrue(earlyHits.get() == 0, "Crab man claw hit during its wind-up, first hit at tick " + firstHitTick.get() + " (" + contacts.get() + " hits) " + hitInfo);
                    helper.assertTrue(contacts.get() == 1 && player.getHealth() < health,
                            "Crab man contacts=" + contacts.get() + ", health " + health + " -> " + player.getHealth());
                    helper.assertTrue(maxPush[0] > 0.1, "Player was not knocked back (velocity away " + maxPush[0] + ")");
                    helper.assertTrue(QiService.getQi(player) == 100, "Crab man attack changed player Qi");
                    helper.succeed();
                } finally { NeoForge.EVENT_BUS.unregister(observer); man.discard(); cleanup(player); }
            });
        } catch (RuntimeException exception) {
            NeoForge.EVENT_BUS.unregister(observer);
            man.discard();
            cleanup(player);
            throw exception;
        }
    }

    @GameTest(template = "foundation_arena", batch = "crab_man_mob", timeoutTicks = 80)
    public static void crabManJabKnocksBackOtherMobs(GameTestHelper helper) {
        var man = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(2, 1, 2));
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        man.setNoAi(true);
        man.setNoGravity(true);
        zombie.setNoAi(true);
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        man.moveTo(0.5, 70, 0.5, 0, 0);
        man.yBodyRot = 0;
        man.yHeadRot = 0;
        zombie.moveTo(0.5, 70, 2.2, 180, 0);
        man.setTarget(zombie);
        Vec3 start = zombie.position();
        float health = zombie.getHealth();
        EpicFightCapabilities.getEntityPatch(man, CrabManPatch.class).playAnimationSynchronized(CrabManEpicFight.CLAW_SNAP, 0);
        helper.runAfterDelay(50, () -> {
            try {
                helper.assertTrue(zombie.getHealth() < health, "Jab did not hit the zombie");
                helper.assertTrue(zombie.position().z - start.z > 0.3, "Zombie was not knocked back (moved " + (zombie.position().z - start.z) + ")");
                helper.succeed();
            } finally { man.discard(); zombie.discard(); }
        });
    }

    @GameTest(template = "foundation_arena", batch = "crab_man_ai", timeoutTicks = 140)
    public static void crabManAiAttacksANearbyPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var man = helper.spawn(MurimEntities.CRAB_MAN.get(), new BlockPos(2, 1, 2));
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        man.setNoGravity(true);
        man.moveTo(0.5, 70, 0.5, 0, 0);
        player.moveTo(0.5, 70, 2.0, 180, 0);
        man.setTarget(player);
        float health = player.getHealth();
        for (int tick = 1; tick <= 120; tick++) helper.runAfterDelay(tick, player.connection::tick);
        helper.runAfterDelay(120, () -> {
            try {
                helper.assertTrue(player.getHealth() < health, "Crab man AI never landed an attack");
                helper.succeed();
            } finally { man.discard(); cleanup(player); }
        });
    }
}
