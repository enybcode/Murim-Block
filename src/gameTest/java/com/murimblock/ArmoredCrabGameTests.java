package com.murimblock;

import com.murimblock.integration.epicfight.ArmoredCrabArmature;
import com.murimblock.integration.epicfight.ArmoredCrabEpicFight;
import com.murimblock.integration.epicfight.ArmoredCrabPatch;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.ArmoredCrabEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.qi.QiRewardManager;
import com.murimblock.qi.QiService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/** Server checks for the armored crab; uses the foundation_arena template built by FoundationGameTests. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArmoredCrabGameTests {
    private ArmoredCrabGameTests() { }

    @GameTest(template = "foundation_arena", batch = "crab")
    public static void armoredCrabHasItsOwnArmatureAnimationsAndOnlyAnimatedAttacks(GameTestHelper helper) {
        var crab = helper.spawn(MurimEntities.ARMORED_CRAB.get(), new BlockPos(2, 1, 2));
        try {
            var patch = EpicFightCapabilities.getEntityPatch(crab, ArmoredCrabPatch.class);
            helper.assertTrue(patch != null, "Crab has no Epic Fight patch");
            helper.assertTrue(patch.getArmature() instanceof ArmoredCrabArmature, "Crab armature is " + patch.getArmature());
            ArmoredCrabArmature armature = (ArmoredCrabArmature) patch.getArmature();
            for (Joint joint : List.of(armature.body, armature.clawRightPalm, armature.clawLeftPalm)) {
                helper.assertTrue(joint != Joint.EMPTY, "Crab joint missing from armored_crab.json");
            }
            for (var accessor : List.of(ArmoredCrabEpicFight.IDLE, ArmoredCrabEpicFight.WALK, ArmoredCrabEpicFight.ATTACK_RIGHT,
                    ArmoredCrabEpicFight.ATTACK_LEFT, ArmoredCrabEpicFight.HURT, ArmoredCrabEpicFight.DEATH)) {
                helper.assertTrue(accessor != null && accessor.get() != null, "Crab animation not registered: " + accessor);
            }
            for (var attribute : List.of(EpicFightAttributes.WEIGHT, EpicFightAttributes.IMPACT,
                    EpicFightAttributes.MAX_STRIKES, EpicFightAttributes.STUN_ARMOR)) {
                helper.assertTrue(crab.getAttribute(attribute) != null, "Crab engine attribute missing: " + attribute);
            }
            long animated = crab.goalSelector.getAvailableGoals().stream()
                    .filter(goal -> goal.getGoal() instanceof AnimatedAttackGoal).count();
            helper.assertTrue(animated == 1, "Expected exactly one animated attack goal, got " + animated);
            helper.assertFalse(crab.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof MeleeAttackGoal
                    && !(goal.getGoal() instanceof TargetChasingGoal)), "Vanilla melee goal would double the claw attacks");
            helper.assertTrue(crab.getBbWidth() < 1.4F && crab.getBbHeight() < 0.9F, "Crab is not smaller than a spider");
            helper.assertTrue(crab.getMaxHealth() == 10, "Crab health changed: " + crab.getMaxHealth());
            helper.succeed();
        } finally { crab.discard(); }
    }

    @GameTest(template = "foundation_arena", batch = "crab")
    public static void armoredCrabGivesChickenExperienceThreeQiAndNoLoot(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var crab = helper.spawn(MurimEntities.ARMORED_CRAB.get(), new BlockPos(1, 1, 1));
        crab.setNoAi(true);
        try {
            helper.assertTrue(QiRewardManager.rewardTarget(crab.getType()).baseReward() == 3, "Crab Qi reward is not 3");
            for (int i = 0; i < 40; i++) {
                int xp = crab.getExperienceReward(helper.getLevel(), player);
                helper.assertTrue(xp >= 1 && xp <= 3, "Crab experience outside chicken range: " + xp);
            }
            AtomicInteger deaths = new AtomicInteger();
            Consumer<LivingDropsEvent> observer = event -> {
                if (event.getEntity() != crab) return;
                helper.assertTrue(event.getDrops().isEmpty(), "Crab dropped loot");
                deaths.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(observer);
            try {
                crab.hurt(player.damageSources().playerAttack(player), 1000);
                helper.assertTrue(!crab.isAlive() && deaths.get() == 1, "Crab death pipeline did not run");
            } finally { NeoForge.EVENT_BUS.unregister(observer); }
            helper.succeed();
        } finally { crab.discard(); cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "crab")
    public static void armoredCrabSpawnsOnlyNearWaterButAtAnyHour(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(2, 0, 2));
        BlockPos spawn = ground.above();
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.SAND);
        var type = MurimEntities.ARMORED_CRAB.get();
        helper.assertFalse(ArmoredCrabEntity.checkCrabSpawnRules(type, level, MobSpawnType.NATURAL, spawn, level.random),
                "Crab may spawn away from water");
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.WATER);
        helper.assertTrue(ArmoredCrabEntity.checkCrabSpawnRules(type, level, MobSpawnType.NATURAL, spawn, level.random),
                "Crab cannot spawn on sand next to water");
        // Monsters normally refuse bright light; the crab must still pass the per-mob rule at noon.
        level.setDayTime(6000);
        var crab = type.create(level);
        crab.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
        helper.assertTrue(crab.checkSpawnRules(level, MobSpawnType.NATURAL), "Crab refuses to spawn in daylight");
        helper.assertTrue(crab.getMaxSpawnClusterSize() >= 5, "Crab groups capped below five");
        crab.discard();
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "crab")
    public static void murimblockCreativeTabOffersTheCrabSpawnEgg(GameTestHelper helper) {
        var egg = MurimItems.ARMORED_CRAB_SPAWN_EGG.get();
        helper.assertTrue(egg.getType(new ItemStack(egg)) == MurimEntities.ARMORED_CRAB.get(),
                "Spawn egg does not create an armored crab");
        var tab = MurimItems.MURIMBLOCK_TAB.get();
        var level = helper.getLevel();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                level.enabledFeatures(), false, level.registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(egg)), "Murimblock tab lacks the crab egg");
        helper.assertTrue(tab.getIconItem().is(egg), "Murimblock tab icon is not the crab egg");
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "crab_claw_right", timeoutTicks = 60)
    public static void armoredCrabRightClawHitsOnceInsideItsContactWindow(GameTestHelper helper) {
        clawContact(helper, ArmoredCrabEpicFight.ATTACK_RIGHT, 9, 34);
    }

    @GameTest(template = "foundation_arena", batch = "crab_claw_left", timeoutTicks = 60)
    public static void armoredCrabLeftClawHitsOnceInsideItsContactWindow(GameTestHelper helper) {
        clawContact(helper, ArmoredCrabEpicFight.ATTACK_LEFT, 6, 30);
    }

    /**
     * Plays one clip on a frozen crab facing a player 1.5 blocks straight ahead and counts damage contacts.
     * Uses absolute coordinates, so each claw test needs its own batch: a claw strikes one target only.
     */
    private static void clawContact(GameTestHelper helper, AnimationAccessor<AttackAnimation> attack, int beforeContactTick, int endTick) {
        ServerPlayer player = player(helper);
        var crab = helper.spawn(MurimEntities.ARMORED_CRAB.get(), new BlockPos(2, 1, 2));
        crab.setNoAi(true);
        crab.setNoGravity(true);
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        crab.moveTo(0.5, 70, 0.5, 0, 0);
        crab.yBodyRot = 0;
        crab.yHeadRot = 0;
        // 1.5 blocks ahead: the claws must reach a player who is not glued to the crab.
        player.moveTo(0.5, 70, 2.0, 180, 0);
        crab.setTarget(player);
        player.connection.tick();
        var patch = EpicFightCapabilities.getEntityPatch(crab, ArmoredCrabPatch.class);
        AtomicInteger contacts = new AtomicInteger();
        Consumer<LivingDamageEvent.Post> observer = event -> {
            if (event.getEntity() == player && event.getSource().getEntity() == crab) contacts.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(observer);
        float health = player.getHealth();
        try {
            patch.playAnimationSynchronized(attack, 0);
            helper.runAfterDelay(beforeContactTick, () -> helper.assertTrue(contacts.get() == 0 && player.getHealth() == health,
                    "Crab claw dealt damage during its wind-up"));
            for (int tick = 1; tick <= endTick; tick++) helper.runAfterDelay(tick, player.connection::tick);
            helper.runAfterDelay(endTick, () -> {
                try {
                    helper.assertTrue(contacts.get() == 1 && player.getHealth() < health,
                            "Crab claw contacts=" + contacts.get() + ", health " + health + " -> " + player.getHealth());
                    helper.assertTrue(QiService.getQi(player) == 100, "Crab attack changed player Qi");
                    helper.succeed();
                } finally { NeoForge.EVENT_BUS.unregister(observer); crab.discard(); cleanup(player); }
            });
        } catch (RuntimeException exception) {
            NeoForge.EVENT_BUS.unregister(observer);
            crab.discard();
            cleanup(player);
            throw exception;
        }
    }

    @GameTest(template = "foundation_arena", batch = "crab_ai", timeoutTicks = 120)
    public static void armoredCrabAiAttacksANearbyPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var crab = helper.spawn(MurimEntities.ARMORED_CRAB.get(), new BlockPos(2, 1, 2));
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        crab.setNoGravity(true);
        crab.moveTo(0.5, 70, 0.5, 0, 0);
        player.moveTo(0.5, 70, 1.5, 180, 0);
        crab.setTarget(player);
        float health = player.getHealth();
        for (int tick = 1; tick <= 100; tick++) helper.runAfterDelay(tick, player.connection::tick);
        helper.runAfterDelay(100, () -> {
            try {
                helper.assertTrue(player.getHealth() < health, "Crab AI never landed a claw attack");
                helper.succeed();
            } finally { crab.discard(); cleanup(player); }
        });
    }
}
