package com.murimblock;

import com.murimblock.item.MurimItems;
import com.murimblock.mob.AllianceMerchantEntity;
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
 * The Alliance Merchant: a neutral man with empty hands who never despawns; every trade of his is in silver taels; a
 * right-click opens the trading screen and a trade goes through; struck, he ends the trade; he drops a few taels.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AllianceMerchantGameTests {
    private AllianceMerchantGameTests() { }

    private static AllianceMerchantEntity merchant(GameTestHelper helper, BlockPos at) {
        AllianceMerchantEntity merchant = MurimEntities.ALLIANCE_MERCHANT.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(merchant != null, "No merchant spawned");
        return merchant;
    }

    /** The Murim's money, any of its four kinds (copper cash, strings of cash, taels, gold yuanbao). */
    private static boolean tael(ItemStack stack) {
        return stack.is(MurimItems.SILVER_TAEL.get()) || stack.is(MurimItems.COPPER_CASH.get()) || stack.is(MurimItems.CASH_STRING.get())
                || stack.is(MurimItems.GOLD_YUANBAO.get());
    }

    @GameTest(template = "foundation_arena", batch = "merchant_wares")
    public static void theMerchantTradesOnlyInTaels(GameTestHelper helper) {
        AllianceMerchantEntity merchant = merchant(helper, new BlockPos(2, 1, 2));
        try {
            helper.assertTrue(merchant.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(), "The merchant holds something");
            helper.assertTrue(EpicFightCapabilities.getEntityPatch(merchant, LivingEntityPatch.class) != null, "No Epic Fight patch on the merchant");
            helper.assertTrue(!merchant.removeWhenFarAway(1.0E6) && merchant.isPersistenceRequired(), "The merchant could despawn");
            var offers = merchant.getOffers();
            helper.assertTrue(offers.size() >= 10, "Too few wares: " + offers.size());
            boolean spear = false, dao = false, bishou = false, flyingKnife = false;
            for (MerchantOffer offer : offers) {
                // Either he takes taels for a ware, or he gives taels for a spoil; never anything for nothing.
                helper.assertTrue(tael(offer.getBaseCostA()) != tael(offer.getResult()), "A trade not in taels: " + offer.getBaseCostA() + " -> " + offer.getResult());
                // A second price, if any, is taels too (the locator pendants cost two stacks), but for the sect's token: gold yuanbao.
                boolean token = offer.getResult().is(MurimItems.SECT_TOKEN.get());
                helper.assertTrue((offer.getCostB().isEmpty() || tael(offer.getCostB()) || token && offer.getCostB().is(MurimItems.GOLD_YUANBAO.get())) && offer.getBaseCostA().getCount() > 0 && offer.getMaxUses() > 0, "A strange trade: " + offer.getBaseCostA());
                if (offer.getResult().is(MurimItems.ALLIANCE_SPEAR.get())) spear = offer.getBaseCostA().getCount() == 24;
                if (offer.getBaseCostA().is(MurimItems.BANDIT_DAO.get())) dao = offer.getResult().getCount() == 3;
                if (offer.getResult().is(MurimItems.BISHOU.get())) bishou = offer.getBaseCostA().getCount() == 16;
                if (offer.getResult().is(MurimItems.FLYING_KNIFE.get())) flyingKnife = offer.getBaseCostA().getCount() == 12;
            }
            helper.assertTrue(spear, "No Azure Peak Spear for 24 taels");
            helper.assertTrue(bishou && flyingKnife, "No Bishou for 16 taels or Flying Knife for 12");
            for (var pendant : MurimItems.pendants()) {
                var sold = offers.stream().filter(o -> o.getResult().is(pendant.get())).findFirst();
                helper.assertTrue(sold.isPresent(), "He does not sell the " + pendant.getId());
                int price = sold.get().getBaseCostA().getCount() + sold.get().getCostB().getCount();
                helper.assertTrue(price == AllianceMerchantEntity.PENDANT_PRICE && sold.get().getMaxUses() == 1, "The " + pendant.getId() + " costs " + price);
            }
            helper.assertTrue(dao, "He does not buy a bandit dao for 3 taels");
        } finally {
            merchant.discard();
        }
        helper.succeed();
    }

    /** A right-click opens his trading screen; a player with 24 taels buys the spear; a blow ends the trade. */
    @GameTest(template = "foundation_arena", batch = "merchant_trade")
    public static void aRightClickTradesAndABlowEndsIt(GameTestHelper helper) {
        AllianceMerchantEntity merchant = merchant(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        try {
            player.getInventory().clearContent();
            // In the bag, not in hand (the empty hand is the first hotbar slot).
            player.getInventory().setItem(20, new ItemStack(MurimItems.SILVER_TAEL.get(), 24));
            merchant.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.containerMenu instanceof MerchantMenu, "No trading screen: " + player.containerMenu);
            helper.assertTrue(merchant.getTradingPlayer() == player, "The merchant is not trading with the player");
            MerchantMenu menu = (MerchantMenu) player.containerMenu;
            int spear = -1;
            for (int i = 0; i < menu.getOffers().size(); i++) if (menu.getOffers().get(i).getResult().is(MurimItems.ALLIANCE_SPEAR.get())) spear = i;
            helper.assertTrue(spear >= 0, "No spear on the trading screen");
            menu.setSelectionHint(spear);
            menu.tryMoveItems(spear);
            String slots = "pay " + menu.getSlot(0).getItem() + ", result " + menu.getSlot(2).getItem();
            menu.quickMoveStack(player, 2);
            helper.assertTrue(player.getInventory().countItem(MurimItems.ALLIANCE_SPEAR.get()) == 1, "The spear was not bought (" + slots + ", taels left " + player.getInventory().countItem(MurimItems.SILVER_TAEL.get()) + ")");
            helper.assertTrue(player.getInventory().countItem(MurimItems.SILVER_TAEL.get()) == 0, "The taels were not paid");
            // A blow: the trade is off and the screen closes.
            merchant.hurt(player.damageSources().playerAttack(player), 1.0F);
            helper.assertTrue(merchant.getTradingPlayer() == null, "The merchant still trades after a blow");
            helper.assertTrue(!(player.containerMenu instanceof MerchantMenu), "The trading screen stayed open after a blow");
        } finally {
            merchant.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    /** Killed by a player he leaves one to three taels (no Looting); to no player, nothing. */
    @GameTest(template = "foundation_arena", batch = "merchant_loot")
    public static void theMerchantLeavesAFewTaels(GameTestHelper helper) {
        AllianceMerchantEntity merchant = merchant(helper, new BlockPos(2, 1, 2));
        ServerPlayer player = player(helper);
        try {
            var byPlayer = com.murimblock.testing.LootRolls.roll(helper.getLevel(), merchant, player, 400);
            helper.assertTrue(byPlayer.onlyContains(MurimItems.SILVER_TAEL.get()) && byPlayer.rollsWith(MurimItems.SILVER_TAEL.get()) == 400,
                    "Not taels every time: " + byPlayer.rollsWith());
            helper.assertTrue(byPlayer.mostInOneRoll(MurimItems.SILVER_TAEL.get()) <= 3, "Too many taels at once");
            var byNothing = com.murimblock.testing.LootRolls.roll(helper.getLevel(), merchant, null, 20);
            helper.assertTrue(byNothing.rollsWith().isEmpty(), "Taels without a player: " + byNothing.rollsWith());
        } finally {
            merchant.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    /** Bandits leave taels to a player, up to two (no Looting), and none to no one. */
    @GameTest(template = "foundation_arena", batch = "merchant_loot")
    public static void banditsLeaveTaels(GameTestHelper helper) {
        var bandit = helper.spawn(MurimEntities.MOUNTAIN_BANDIT.get(), new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        try {
            var byPlayer = com.murimblock.testing.LootRolls.roll(helper.getLevel(), bandit, player, 2000);
            int with = byPlayer.rollsWith(MurimItems.SILVER_TAEL.get());
            // Uniform 0 to 2, rounded: about two rolls in three give some.
            helper.assertTrue(with > 1000 && with < 1700 && byPlayer.mostInOneRoll(MurimItems.SILVER_TAEL.get()) == 2, "Bandit taels off: " + with + " in 2000");
            var byNothing = com.murimblock.testing.LootRolls.roll(helper.getLevel(), bandit, null, 200);
            helper.assertTrue(byNothing.rollsWith(MurimItems.SILVER_TAEL.get()) == 0, "Bandit taels to no player");
        } finally {
            bandit.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    /** With 128 taels in the bag, a player buys a locator pendant from the trading screen, paying both stacks. */
    @GameTest(template = "foundation_arena", batch = "merchant_pendant")
    public static void aPendantCostsTwoStacksOfTaels(GameTestHelper helper) {
        AllianceMerchantEntity merchant = merchant(helper, new BlockPos(2, 1, 4));
        ServerPlayer player = player(helper);
        try {
            player.getInventory().clearContent();
            player.getInventory().setItem(20, new ItemStack(MurimItems.SILVER_TAEL.get(), 64));
            player.getInventory().setItem(21, new ItemStack(MurimItems.SILVER_TAEL.get(), 64));
            merchant.interact(player, InteractionHand.MAIN_HAND);
            MerchantMenu menu = (MerchantMenu) player.containerMenu;
            int index = -1;
            for (int i = 0; i < menu.getOffers().size(); i++) if (menu.getOffers().get(i).getResult().is(MurimItems.BANDIT_DEN_PENDANT.get())) index = i;
            helper.assertTrue(index >= 0, "No bandit den pendant on the trading screen");
            menu.setSelectionHint(index);
            menu.tryMoveItems(index);
            menu.quickMoveStack(player, 2);
            helper.assertTrue(player.getInventory().countItem(MurimItems.BANDIT_DEN_PENDANT.get()) == 1, "The pendant was not bought");
            helper.assertTrue(player.getInventory().countItem(MurimItems.SILVER_TAEL.get()) == 0, "Not all 128 taels were paid");
        } finally {
            merchant.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    /** Where none of its structure stands within reach, a pendant says so and its needle points nowhere. */
    @GameTest(template = "foundation_arena", batch = "merchant_pendant", timeoutTicks = 400)
    public static void aPendantWithNothingToFindSaysSo(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            for (var pendant : MurimItems.pendants()) {
                helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                        .containsKey(pendant.get().structure()), "No structure " + pendant.get().structure().location());
            }
            ItemStack stack = new ItemStack(MurimItems.ALLIANCE_ARENA_PENDANT.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            // The test world is flat and builds no structure: nothing to find, nothing thrown.
            stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            var tracker = stack.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
            helper.assertTrue(tracker != null && tracker.target().isEmpty(), "The needle points somewhere: " + tracker);
            helper.assertTrue(player.getCooldowns().isOnCooldown(MurimItems.ALLIANCE_ARENA_PENDANT.get()), "No cooldown after a search");
        } finally {
            cleanup(player);
        }
        helper.succeed();
    }
}
