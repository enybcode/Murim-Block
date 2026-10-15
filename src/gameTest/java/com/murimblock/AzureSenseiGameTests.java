package com.murimblock;

import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.mob.AzureSenseiEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Sensei Yun: he comes with the Azure Cloud in hand (a sword for Epic Fight), a hard boss; stands and walks in his own
 * motions, stays about his home and never despawns, leaves a player alone until struck, hands each player the Azure
 * Cloud Sword Manual once, fights with the art itself, has a second phase and recovers, drops his sword and the manual.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AzureSenseiGameTests {
    private AzureSenseiGameTests() { }

    private static AzureSenseiEntity sensei(GameTestHelper helper, BlockPos at) {
        AzureSenseiEntity sensei = MurimEntities.AZURE_SENSEI.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(sensei != null, "No sensei spawned");
        return sensei;
    }

    @GameTest(template = "foundation_arena", batch = "sensei_sword")
    public static void theSenseiComesWithTheAzureCloudASwordForEpicFight(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 2));
        ItemStack held = sensei.getItemBySlot(EquipmentSlot.MAINHAND);
        helper.assertTrue(held.is(MurimItems.AZURE_CLOUD_SWORD.get()), "The sensei does not hold the Azure Cloud: " + held);
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(held);
        helper.assertTrue(capability.getWeaponCategory() == CapabilityItem.WeaponCategories.SWORD,
                "Epic Fight does not take the Azure Cloud for a sword: " + capability.getWeaponCategory());
        helper.assertTrue(EpicFightCapabilities.getEntityPatch(sensei, LivingEntityPatch.class) != null, "No Epic Fight patch on the sensei");
        helper.assertTrue(!sensei.removeWhenFarAway(1.0E6) && sensei.isPersistenceRequired(), "The sensei could despawn");
        helper.assertTrue(sensei.hasRestriction() && sensei.getRestrictCenter().equals(helper.absolutePos(new BlockPos(2, 1, 2))),
                "The sensei is not kept about where he was placed: " + sensei.getRestrictCenter());
        // His own 4 and the sword's 6 (held equipment counts from the next tick).
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(sensei.getAttributeValue(Attributes.ATTACK_DAMAGE) - 10) < 0.01,
                    "The sensei should hit for 10 with the Azure Cloud, not " + sensei.getAttributeValue(Attributes.ATTACK_DAMAGE));
            helper.assertTrue(sensei.getMaxHealth() >= 300, "The sensei is no hard boss: " + sensei.getMaxHealth() + " health");
            sensei.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "sensei_motions")
    public static void theSenseiStandsAndWalksInHisOwnMotions(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 2));
        helper.runAfterDelay(3, () -> {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(sensei, LivingEntityPatch.class);
            var idle = patch.getAnimator().getLivingAnimation(LivingMotions.IDLE, null);
            var walk = patch.getAnimator().getLivingAnimation(LivingMotions.WALK, null);
            helper.assertTrue(idle == MurimAnimations.SENSEI_IDLE, "With his sword, the sensei should stand in his own idle, not " + idle);
            helper.assertTrue(walk == MurimAnimations.SENSEI_WALK, "With his sword, the sensei should walk in his own stride, not " + walk);
            sensei.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "sensei_home")
    public static void theSenseiKeepsHisHomeInHisSave(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 2));
        BlockPos home = helper.absolutePos(new BlockPos(1, 1, 1));
        sensei.setHome(home);
        CompoundTag saved = new CompoundTag();
        sensei.save(saved);
        AzureSenseiEntity loaded = MurimEntities.AZURE_SENSEI.get().create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(loaded.hasRestriction() && loaded.getRestrictCenter().equals(home)
                        && loaded.getRestrictRadius() == AzureSenseiEntity.HOME_RADIUS,
                "The sensei's home was not kept: " + loaded.getRestrictCenter() + " within " + loaded.getRestrictRadius());
        sensei.discard();
        helper.succeed();
    }

    /** A player beside him is left alone; struck, he turns on whoever struck him. */
    @GameTest(template = "foundation_arena", batch = "sensei_neutral", timeoutTicks = 120)
    public static void theSenseiIsNeutralUntilStruck(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(sensei.getTarget() == null && !sensei.isAggressive(), "The sensei turned on a player who did nothing: " + sensei.getTarget());
            sensei.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(sensei.getTarget() == player, "Struck, the sensei should turn on the player, not " + sensei.getTarget());
                helper.assertTrue(sensei.isAggressive(), "Fighting, the sensei should show it (his face)");
                sensei.discard();
                cleanup(player);
                helper.succeed();
            });
        });
    }

    /** His boss bar shows to a player near him and follows his health. */
    @GameTest(template = "foundation_arena", batch = "sensei_boss_bar", timeoutTicks = 60)
    public static void theSenseiHasABossBar(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        sensei.startSeenByPlayer(player);
        helper.assertTrue(sensei.showsBossBarTo(player), "A player near the sensei does not see his boss bar");
        sensei.setHealth(sensei.getMaxHealth() / 2);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(sensei.bossBarProgress() - 0.5F) < 0.01F, "His boss bar does not follow his health: " + sensei.bossBarProgress());
            sensei.stopSeenByPlayer(player);
            helper.assertTrue(!sensei.showsBossBarTo(player), "A player who no longer sees him still has his boss bar");
            sensei.discard();
            cleanup(player);
            helper.succeed();
        });
    }

    /** Spoken to, he gives no manual: it must be won by defeating him. */
    @GameTest(template = "foundation_arena", batch = "sensei_manual")
    public static void theSenseiGivesNoManualWhenSpokenTo(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        MartialService.setForTests(player, MartialData.EMPTY);
        player.getInventory().clearContent();
        try {
            sensei.interact(player, InteractionHand.MAIN_HAND);
            sensei.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(count(player) == 0, "The sensei gave the manual without being defeated: " + count(player));
        } finally {
            sensei.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    /** Whether this is one of his moves: the art's strikes and techniques. */
    private static boolean senseiMove(Object a) {
        return java.util.List.of(MurimAnimations.DRIFTING_CUT, MurimAnimations.RETURNING_CUT, MurimAnimations.PIERCING_MIST,
                MurimAnimations.RISING_CLOUD, MurimAnimations.FALLING_RAIN, MurimAnimations.SWALLOW_RETURNS, MurimAnimations.THOUSAND_DRIZZLE,
                MurimAnimations.WIND_STEP, MurimAnimations.CLOUD_STEP_SLASH, MurimAnimations.HEAVEN_SPLITTING_CRESCENT, MurimAnimations.THOUSAND_DRIZZLE_STORM).contains(a);
    }

    private static java.util.List<yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<?>> senseiMoves() {
        return java.util.List.of(MurimAnimations.DRIFTING_CUT, MurimAnimations.RETURNING_CUT, MurimAnimations.PIERCING_MIST,
                MurimAnimations.RISING_CLOUD, MurimAnimations.FALLING_RAIN, MurimAnimations.SWALLOW_RETURNS, MurimAnimations.THOUSAND_DRIZZLE,
                MurimAnimations.WIND_STEP, MurimAnimations.CLOUD_STEP_SLASH, MurimAnimations.HEAVEN_SPLITTING_CRESCENT, MurimAnimations.THOUSAND_DRIZZLE_STORM);
    }

    /**
     * None of his moves takes his body under the ground he stands on: its root (the hips, 0.76 block up at rest) stays
     * above 0.3 block in every frame. A move made for the air (the Cloud Step Dive plunges 2.5 blocks) sank him into it.
     */
    @GameTest(template = "foundation_arena", batch = "sensei_ground")
    public static void noMoveOfTheSenseiSinksIntoTheGround(GameTestHelper helper) {
        for (var move : senseiMoves()) {
            var id = move.registryName();
            String file = "/assets/" + id.getNamespace() + "/animmodels/animations/" + id.getPath() + ".json";
            try (var in = AzureSenseiGameTests.class.getResourceAsStream(file)) {
                helper.assertTrue(in != null, "No animation file for " + id);
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject();
                for (var joint : json.getAsJsonArray("animation")) {
                    if (!joint.getAsJsonObject().get("name").getAsString().equals("Root")) continue;
                    for (var m : joint.getAsJsonObject().getAsJsonArray("transform")) {
                        double height = m.getAsJsonArray().get(11).getAsDouble();
                        helper.assertTrue(height > 0.3, id + " takes the sensei's body down to " + height + " (under the ground)");
                    }
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        helper.succeed();
    }

    private static int count(ServerPlayer player) {
        return player.getInventory().countItem(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
    }

    /** Below half his health he fights in earnest (quicker); whole again, he is calm. Left alone, he recovers. */
    @GameTest(template = "foundation_arena", batch = "sensei_phase", timeoutTicks = 100)
    public static void theSenseiHasASecondPhaseAndRecovers(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 2));
        double calm = sensei.getAttributeValue(Attributes.MOVEMENT_SPEED);
        sensei.setHealth(sensei.getMaxHealth() * 0.4F);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(sensei.secondPhase(), "Below half his health, the sensei is not in his second phase");
            helper.assertTrue(sensei.getAttributeValue(Attributes.MOVEMENT_SPEED) > calm * 1.4, "In his second phase he is not quicker");
            float before = sensei.getHealth();
            helper.runAfterDelay(41, () -> {
                helper.assertTrue(sensei.getHealth() >= before + 3.9F, "Left alone, he does not recover: " + before + " -> " + sensei.getHealth());
                sensei.setHealth(sensei.getMaxHealth());
                helper.runAfterDelay(3, () -> {
                    helper.assertTrue(!sensei.secondPhase(), "Whole again, the sensei is still in his second phase");
                    helper.assertTrue(Math.abs(sensei.getAttributeValue(Attributes.MOVEMENT_SPEED) - calm) < 1e-6, "Whole again, he is still quicker");
                    sensei.discard();
                    helper.succeed();
                });
            });
        });
    }

    /** Killed by a player, he drops the Azure Cloud one time in 200, always the manual, and 15 to 20 silver taels; otherwise, nothing. */
    @GameTest(template = "foundation_arena", batch = "sensei_loot")
    public static void theSenseiDropsHisSwordAndTheManual(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 2));
        ServerPlayer player = player(helper);
        try {
            // 20000 rolls: the shares stay close to theirs (the sword about 100 times).
            var byPlayer = com.murimblock.testing.LootRolls.roll(helper.getLevel(), sensei, player, 20000);
            double sword = byPlayer.rollsWith(MurimItems.AZURE_CLOUD_SWORD.get()) / 20000.0, manual = byPlayer.rollsWith(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get()) / 20000.0;
            helper.assertTrue(sword > 0.0025 && sword < 0.0075, "The sword should drop one time in 200, not " + sword);
            helper.assertTrue(manual == 1.0, "The manual should always drop, not " + manual);
            helper.assertTrue(byPlayer.mostInOneRoll(MurimItems.AZURE_CLOUD_SWORD.get()) == 1 && byPlayer.mostInOneRoll(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get()) == 1,
                    "More than one sword or manual at once");
            helper.assertTrue(byPlayer.onlyContains(MurimItems.AZURE_CLOUD_SWORD.get(), MurimItems.AZURE_CLOUD_SWORD_MANUAL.get(), MurimItems.SILVER_TAEL.get()), "Something else dropped: " + byPlayer.rollsWith());
            var byNothing = com.murimblock.testing.LootRolls.roll(helper.getLevel(), sensei, null, 20);
            helper.assertTrue(byNothing.rollsWith().isEmpty(), "Something dropped without a player: " + byNothing.rollsWith());
        } finally {
            sensei.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "sensei_attack", timeoutTicks = 200)
    public static void theSenseiFightsWithTheAzureCloudCuts(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        // A helmet: no burning in the sun.
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        boolean[] cut = {false};
        helper.onEachTick(() -> {
            sensei.setTarget(target);
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(sensei, LivingEntityPatch.class);
            AnimationPlayer main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
            if (main != null && main.getRealAnimation() != null && main.getRealAnimation().get() instanceof AttackAnimation) {
                var a = main.getRealAnimation();
                helper.assertTrue(senseiMove(a), "The sensei swung something else than the Azure Cloud Sword Art: " + a);
                cut[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(cut[0], "The sensei has not cut yet");
            helper.assertTrue(target.getHealth() < 200, "The sensei's cut has not landed yet");
            sensei.discard();
            target.discard();
        });
    }
}
