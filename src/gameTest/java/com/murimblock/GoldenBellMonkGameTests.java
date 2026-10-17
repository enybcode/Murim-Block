package com.murimblock;

import com.murimblock.item.MurimItems;
import com.murimblock.martial.GoldenBellItems;
import com.murimblock.mob.GoldenBellMonkEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Golden Bell monk: he sells the Golden Bell manual for 40 taels from Minecraft's trading screen; he leaves a player
 * beside him alone; struck, he turns on whoever struck him and will not trade; below half his health he takes the golden
 * form (a synced flag) and takes 40 % less damage, then leaves it after 10 s.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoldenBellMonkGameTests {
    private GoldenBellMonkGameTests() { }

    private static GoldenBellMonkEntity monk(GameTestHelper helper, BlockPos at) {
        GoldenBellMonkEntity monk = MurimEntities.GOLDEN_BELL_MONK.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(monk != null, "No monk spawned");
        return monk;
    }

    /** He sells the manual for 40 silver taels; a right-click opens his trading screen and the manual can be bought. */
    @GameTest(template = "foundation_arena", batch = "goldenbellmonk_wares")
    public static void theMonkSellsTheManualForFortyTaels(GameTestHelper helper) {
        GoldenBellMonkEntity monk = monk(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        try {
            helper.assertTrue(monk.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(), "The monk holds something: he fights bare-handed");
            helper.assertTrue(EpicFightCapabilities.getEntityPatch(monk, LivingEntityPatch.class) != null, "No Epic Fight patch on the monk");
            helper.assertTrue(!monk.removeWhenFarAway(1.0E6) && monk.isPersistenceRequired(), "The monk could despawn");
            helper.assertTrue(Math.abs(monk.getMaxHealth() - GoldenBellMonkEntity.MAX_HEALTH) < 0.01F, "His health is " + monk.getMaxHealth());
            MerchantOffer manual = monk.getOffers().stream().filter(o -> o.getResult().is(GoldenBellItems.MANUAL.get())).findFirst().orElse(null);
            helper.assertTrue(manual != null, "He does not sell the Golden Bell manual");
            helper.assertTrue(manual.getBaseCostA().is(MurimItems.SILVER_TAEL.get()) && manual.getBaseCostA().getCount() == 40 && manual.getCostB().isEmpty(),
                    "The manual costs " + manual.getBaseCostA() + " + " + manual.getCostB());

            player.getInventory().clearContent();
            player.getInventory().setItem(20, new ItemStack(MurimItems.SILVER_TAEL.get(), 40));
            monk.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.containerMenu instanceof MerchantMenu, "No trading screen: " + player.containerMenu);
            MerchantMenu menu = (MerchantMenu) player.containerMenu;
            int index = -1;
            for (int i = 0; i < menu.getOffers().size(); i++) if (menu.getOffers().get(i).getResult().is(GoldenBellItems.MANUAL.get())) index = i;
            helper.assertTrue(index >= 0, "No manual on the trading screen");
            menu.setSelectionHint(index);
            menu.tryMoveItems(index);
            menu.quickMoveStack(player, 2);
            helper.assertTrue(player.getInventory().countItem(GoldenBellItems.MANUAL.get()) == 1, "The manual was not bought");
            helper.assertTrue(player.getInventory().countItem(MurimItems.SILVER_TAEL.get()) == 0, "The 40 taels were not paid");

            // A blow: the trade is off, and angry he will not trade again.
            monk.hurt(player.damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(monk.getTradingPlayer() == null && !(player.containerMenu instanceof MerchantMenu), "The trade stayed open after a blow");
            helper.assertTrue(monk.angry(), "Struck, the monk is not angry");
            monk.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!(player.containerMenu instanceof MerchantMenu), "Angry, the monk still trades");
        } finally {
            monk.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    /** A player beside him is left alone; struck, he turns on whoever struck him. */
    @GameTest(template = "foundation_arena", batch = "goldenbellmonk_neutral", timeoutTicks = 140)
    public static void theMonkIsNeutralUntilStruck(GameTestHelper helper) {
        GoldenBellMonkEntity monk = monk(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        float health = player.getHealth();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(monk.getTarget() == null && !monk.isAggressive(), "The monk turned on a player who did nothing: " + monk.getTarget());
            helper.assertTrue(player.getHealth() >= health, "The monk struck a player who did nothing");
            monk.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(monk.getTarget() == player, "Struck, the monk should turn on the player, not " + monk.getTarget());
                helper.assertTrue(monk.isAggressive(), "Fighting, the monk should show it (his face)");
                monk.discard();
                cleanup(player);
                helper.succeed();
            });
        });
    }

    /** Below half his health he goes golden: 40 % less damage than a monk who is not, no knockback, and after 10 s it ends. */
    @GameTest(template = "foundation_arena", batch = "goldenbellmonk_golden", timeoutTicks = GoldenBellMonkEntity.GOLDEN_TICKS + 80)
    public static void belowHalfHealthTheMonkGoesGolden(GameTestHelper helper) {
        GoldenBellMonkEntity golden = monk(helper, new BlockPos(1, 1, 1));
        GoldenBellMonkEntity plain = monk(helper, new BlockPos(5, 1, 5));
        var damage = helper.getLevel().damageSources().generic();
        helper.assertTrue(!golden.golden(), "A fresh monk is already golden");
        plain.hurt(damage, 1.0F);
        helper.assertTrue(!plain.golden(), "A monk above half his health went golden");
        golden.setHealth(golden.getMaxHealth() / 2 + 1.0F);
        golden.hurt(damage, 2.0F);
        helper.assertTrue(golden.golden(), "Below half his health, the monk did not go golden (health " + golden.getHealth() + ")");
        helper.assertTrue(golden.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "Golden, he can still be knocked back");

        // The same blow on both: the golden one loses 60 % of what the other loses.
        golden.invulnerableTime = 0;
        plain.invulnerableTime = 0;
        float g0 = golden.getHealth(), p0 = plain.getHealth();
        golden.hurt(damage, 10.0F);
        plain.hurt(damage, 10.0F);
        float lostGolden = g0 - golden.getHealth(), lostPlain = p0 - plain.getHealth();
        helper.assertTrue(lostPlain > 0 && Math.abs(lostGolden - lostPlain * (1.0F - GoldenBellMonkEntity.GOLDEN_REDUCTION)) < 0.05F,
                "Golden, he lost " + lostGolden + " where a plain monk lost " + lostPlain);

        helper.runAfterDelay(GoldenBellMonkEntity.GOLDEN_TICKS + 10, () -> {
            helper.assertTrue(!golden.golden(), "The golden form did not end after 10 s");
            golden.discard();
            plain.discard();
            helper.succeed();
        });
    }
}
