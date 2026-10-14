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
 * Sensei Yun: he comes with the Azure Cloud in hand (a sword for Epic Fight, no drop), stands and walks in his own
 * motions, stays about his home and never despawns, leaves a player alone until struck, hands each player the Azure
 * Cloud Sword Manual once, and fights with the art's own cuts.
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
        // His own attack is nothing; the sword gives its 6 (held equipment counts from the next tick). It does not drop yet.
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(sensei.getAttributeValue(Attributes.ATTACK_DAMAGE) - 6) < 0.01,
                    "The sensei should hit for 6 with the Azure Cloud, not " + sensei.getAttributeValue(Attributes.ATTACK_DAMAGE));
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

    /** Spoken to, he hands the player the manual; a second time, nothing more. Not while he fights them. */
    @GameTest(template = "foundation_arena", batch = "sensei_manual")
    public static void theSenseiGivesTheManualOnce(GameTestHelper helper) {
        AzureSenseiEntity sensei = sensei(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        MartialService.setForTests(player, MartialData.EMPTY);
        try {
            sensei.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(count(player) == 1, "The sensei did not give the manual: " + count(player));
            helper.assertTrue(MartialService.data(player).manualsReceived().contains(MartialArts.AZURE_CLOUD_SWORD),
                    "The gift was not recorded");
            sensei.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(count(player) == 1, "The sensei gave a second manual: " + count(player));
            // While he fights them: nothing.
            MartialService.setForTests(player, MartialData.EMPTY);
            player.getInventory().clearContent();
            sensei.setAggressive(true);
            sensei.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(count(player) == 0, "The sensei gave the manual to someone he is fighting");
        } finally {
            sensei.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    private static int count(ServerPlayer player) {
        return player.getInventory().countItem(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
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
                helper.assertTrue(a == MurimAnimations.DRIFTING_CUT || a == MurimAnimations.RETURNING_CUT || a == MurimAnimations.FALLING_RAIN,
                        "The sensei swung something else than the Azure Cloud's cuts: " + a);
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
