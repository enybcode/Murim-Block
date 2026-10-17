package com.murimblock;

import com.murimblock.integration.epicfight.TigerArmature;
import com.murimblock.integration.epicfight.TigerEpicFight;
import com.murimblock.integration.epicfight.TigerPatch;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.TigerEntity;
import com.murimblock.testing.LootRolls;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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

/** The tiger (docs/mobs/tiger.md): a wolf's statistics doubled, its Epic Fight attacks, its drops, where it lives. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TigerGameTests {
    private TigerGameTests() { }

    @GameTest(template = "foundation_arena", batch = "tiger")
    public static void theTigerIsAWolfTwiceOverWithItsOwnSkeletonAndAttacks(GameTestHelper helper) {
        var tiger = helper.spawn(MurimEntities.TIGER.get(), new BlockPos(2, 1, 2));
        try {
            // A wolf: 8 health, 4 attack, 0.3 speed, 16 blocks of pursuit; the tiger doubles all but the speed.
            helper.assertTrue(tiger.getMaxHealth() == 16, "Tiger health " + tiger.getMaxHealth());
            helper.assertTrue(tiger.getAttributeValue(Attributes.ATTACK_DAMAGE) == 8, "Tiger attack " + tiger.getAttributeValue(Attributes.ATTACK_DAMAGE));
            helper.assertTrue(tiger.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.3, "Tiger speed " + tiger.getAttributeValue(Attributes.MOVEMENT_SPEED));
            helper.assertTrue(tiger.getAttributeValue(Attributes.FOLLOW_RANGE) == 32, "Tiger pursuit " + tiger.getAttributeValue(Attributes.FOLLOW_RANGE));
            helper.assertTrue(tiger.targetSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof NearestAttackableTargetGoal<?>),
                    "The tiger does not hunt players");
            var patch = EpicFightCapabilities.getEntityPatch(tiger, TigerPatch.class);
            helper.assertTrue(patch != null, "Tiger has no Epic Fight patch");
            helper.assertTrue(patch.getArmature() instanceof TigerArmature, "Tiger armature is " + patch.getArmature());
            TigerArmature armature = (TigerArmature) patch.getArmature();
            for (Joint joint : List.of(armature.body, armature.head, armature.frontFootRight, armature.frontFootLeft))
                helper.assertTrue(joint != Joint.EMPTY, "Tiger joint missing from tiger.json");
            for (var accessor : List.of(TigerEpicFight.IDLE, TigerEpicFight.WALK, TigerEpicFight.SWIPE, TigerEpicFight.BITE,
                    TigerEpicFight.DOUBLE_SWIPE, TigerEpicFight.POUNCE_ATTACK, TigerEpicFight.MAUL, TigerEpicFight.HURT, TigerEpicFight.DEATH))
                helper.assertTrue(accessor != null && accessor.get() != null, "Tiger animation not registered: " + accessor);
            long animated = tiger.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof AnimatedAttackGoal).count();
            helper.assertTrue(animated == 1, "Expected one animated attack goal, got " + animated);
            helper.assertFalse(tiger.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof MeleeAttackGoal
                    && !(g.getGoal() instanceof TargetChasingGoal)), "A vanilla melee goal would double the tiger's attacks");
            helper.succeed();
        } finally { tiger.discard(); }
    }

    @GameTest(template = "foundation_arena", batch = "tiger")
    public static void theTigerLeavesItsPeltAndItsClawsOneTimeInFiveEach(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var tiger = helper.spawn(MurimEntities.TIGER.get(), new BlockPos(1, 1, 1));
        tiger.setNoAi(true);
        try {
            var rolls = LootRolls.roll(helper.getLevel(), tiger, player, 4000);
            var pelt = MurimItems.TIGER_PELT.get();
            var claw = MurimItems.TIGER_CLAW.get();
            helper.assertTrue(rolls.onlyContains(pelt, claw), "Tiger loot holds something else: " + rolls.rollsWith());
            // About 800 in 4000 each; outside 650 to 950 would take odds of under one in a hundred thousand.
            helper.assertTrue(rolls.rollsWith(pelt) >= 650 && rolls.rollsWith(pelt) <= 950, "Tiger pelt " + rolls.rollsWith(pelt) + " in 4000");
            helper.assertTrue(rolls.rollsWith(claw) >= 650 && rolls.rollsWith(claw) <= 950, "Tiger claw " + rolls.rollsWith(claw) + " in 4000");
            helper.assertTrue(rolls.mostInOneRoll(pelt) == 1 && rolls.mostInOneRoll(claw) == 1, "Tiger gave two of a kind");
            helper.succeed();
        } finally { tiger.discard(); cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "tiger")
    public static void theTigerLivesOnlyInTheCentreOfTheMap(GameTestHelper helper) {
        helper.assertTrue(TigerEntity.inItsLands(0, 0) && TigerEntity.inItsLands(2_500, -1_500), "No tiger in the centre");
        helper.assertFalse(TigerEntity.inItsLands(0, -15_000), "A tiger in the north");
        helper.assertFalse(TigerEntity.inItsLands(15_000, 0), "A tiger in the east");
        helper.assertFalse(TigerEntity.inItsLands(0, 15_000) || TigerEntity.inItsLands(-15_000, 0), "A tiger in the south or the west");
        helper.assertFalse(TigerEntity.inItsLands(30_000, 0), "A tiger beyond the Murim bubble");
        var level = helper.getLevel();
        var tiger = helper.spawn(MurimEntities.TIGER.get(), new BlockPos(2, 1, 2));
        helper.assertTrue(TigerEntity.checkTigerSpawnRules(MurimEntities.TIGER.get(), level, MobSpawnType.SPAWNER, tiger.blockPosition(), level.random),
                "A spawner cannot make a tiger");
        helper.assertTrue(tiger.getMaxSpawnClusterSize() == 2, "Tigers come in more than pairs");
        tiger.discard();
        var tab = MurimItems.MURIMBLOCK_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false, level.registryAccess()));
        for (var item : List.of(MurimItems.TIGER_SPAWN_EGG.get(), MurimItems.TIGER_PELT.get(), MurimItems.TIGER_CLAW.get()))
            helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(item)), "Murimblock tab lacks " + item);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "tiger_swipe", timeoutTicks = 60)
    public static void theSwipeHitsOnceInItsWindow(GameTestHelper helper) { contact(helper, TigerEpicFight.SWIPE, 1.9, 3, 20, 1); }

    @GameTest(template = "foundation_arena", batch = "tiger_bite", timeoutTicks = 60)
    public static void theBiteHitsOnceInItsWindow(GameTestHelper helper) { contact(helper, TigerEpicFight.BITE, 2.0, 5, 22, 1); }

    @GameTest(template = "foundation_arena", batch = "tiger_double", timeoutTicks = 60)
    public static void theDoubleSwipeHitsTwice(GameTestHelper helper) { contact(helper, TigerEpicFight.DOUBLE_SWIPE, 1.9, 3, 30, 2); }

    @GameTest(template = "foundation_arena", batch = "tiger_maul", timeoutTicks = 80)
    public static void theMaulHitsOnceAfterItsLongWindUp(GameTestHelper helper) { contact(helper, TigerEpicFight.MAUL, 1.9, 14, 40, 1); }

    /**
     * Plays one clip on a frozen tiger facing a player `ahead` blocks straight in front, held in place, and counts the
     * blows: none before `beforeContactTick`, `expected` by `endTick`.
     */
    private static void contact(GameTestHelper helper, AnimationAccessor<AttackAnimation> attack, double ahead, int beforeContactTick, int endTick, int expected) {
        ServerPlayer player = player(helper);
        var tiger = helper.spawn(MurimEntities.TIGER.get(), new BlockPos(2, 1, 2));
        tiger.setNoAi(true);
        tiger.setNoGravity(true);
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        tiger.moveTo(0.5, 70, 0.5, 0, 0);
        tiger.yBodyRot = 0;
        tiger.yHeadRot = 0;
        Runnable hold = () -> { player.moveTo(0.5, 70, 0.5 + ahead, 180, 0); player.invulnerableTime = 0; player.setHealth(player.getMaxHealth()); };
        hold.run();
        tiger.setTarget(player);
        player.connection.tick();
        var patch = EpicFightCapabilities.getEntityPatch(tiger, TigerPatch.class);
        AtomicInteger contacts = new AtomicInteger();
        Consumer<LivingDamageEvent.Post> observer = event -> {
            if (event.getEntity() == player && event.getSource().getEntity() == tiger) contacts.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(observer);
        try {
            patch.playAnimationSynchronized(attack, 0);
            helper.runAfterDelay(beforeContactTick, () -> helper.assertTrue(contacts.get() == 0, "The tiger struck during its wind-up"));
            for (int tick = 1; tick <= endTick; tick++) helper.runAfterDelay(tick, () -> { hold.run(); player.connection.tick(); });
            helper.runAfterDelay(endTick, () -> {
                try {
                    helper.assertTrue(contacts.get() == expected, attack + ": " + contacts.get() + " blows, expected " + expected);
                    helper.succeed();
                } finally { NeoForge.EVENT_BUS.unregister(observer); tiger.discard(); cleanup(player); }
            });
        } catch (RuntimeException exception) {
            NeoForge.EVENT_BUS.unregister(observer);
            tiger.discard();
            cleanup(player);
            throw exception;
        }
    }

    @GameTest(template = "foundation_arena", batch = "tiger_pounce")
    public static void thePounceThrowsTheTigerAtItsPrey(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var tiger = helper.spawn(MurimEntities.TIGER.get(), new BlockPos(2, 1, 2));
        try {
            tiger.moveTo(0.5, 70, 0.5, 0, 0);
            player.moveTo(0.5, 70, 6.5, 180, 0);
            tiger.leapAt(player);
            var v = tiger.getDeltaMovement();
            helper.assertTrue(v.z > 0.9 && Math.abs(v.x) < 0.05 && v.y > 0.3, "The pounce threw the tiger " + v);
            helper.succeed();
        } finally { tiger.discard(); cleanup(player); }
    }

    @GameTest(template = "foundation_arena", batch = "tiger_ai", timeoutTicks = 160)
    public static void theTigerHuntsAPlayerDown(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var tiger = helper.spawn(MurimEntities.TIGER.get(), new BlockPos(2, 1, 2));
        helper.getLevel().getChunk(0, 0);
        helper.getLevel().setChunkForced(0, 0, true);
        tiger.setNoGravity(true);
        tiger.moveTo(0.5, 70, 0.5, 0, 0);
        player.moveTo(0.5, 70, 2.2, 180, 0);
        tiger.setTarget(player);
        float health = player.getHealth();
        for (int tick = 1; tick <= 150; tick++) helper.runAfterDelay(tick, player.connection::tick);
        helper.runAfterDelay(150, () -> {
            try {
                helper.assertTrue(player.getHealth() < health, "The tiger never landed a blow");
                helper.succeed();
            } finally { tiger.discard(); cleanup(player); }
        });
    }
}
