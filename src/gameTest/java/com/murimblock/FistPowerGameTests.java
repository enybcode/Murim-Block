package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.FistPower;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.gameasset.Animations;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * A fist art as main style strengthens the bare hands' plain blows by its power (FistPower, Enzo, 17/10/2026): the
 * same punch of Epic Fight, thrown with the Long Fist as main style and then with a sword art as main style, lands
 * harder the first time, by the Long Fist's +2 and x1.15.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FistPowerGameTests {
    private FistPowerGameTests() { }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.setHealth(200);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + 0.9, 180, 0);
        zombie.setYHeadRot(180);
        zombie.yBodyRot = 180;
        return zombie;
    }

    private static void punch(GameTestHelper helper, ServerPlayer player, int from) {
        helper.runAfterDelay(from, () -> EpicFightBridge.patch(player).playAnimationSynchronized(Animations.FIST_AUTO1, 0.0F));
        for (int t = from + 1; t < from + 15; t++) helper.runAfterDelay(t, player::doTick);
    }

    @GameTest(template = "foundation_arena", batch = "fist_power", timeoutTicks = 120)
    public static void aFistArtAsMainStyleStrengthensTheBareHandsPunch(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        new ItemStack(MurimItems.LONG_FIST_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 0));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.setMainArt(player, MartialArts.LONG_FIST);
        helper.assertTrue(FistPower.boost(player).isPresent(), "No boost bare-handed with the Long Fist as main style");
        Zombie first = dummy(helper, player, 0);
        punch(helper, player, 1);
        float[] boosted = new float[1];
        helper.runAfterDelay(20, () -> {
            boosted[0] = 200 - first.getHealth();
            first.discard();
            EpicFightBridge.patch(player).playAnimationSynchronized(Animations.BIPED_IDLE, 0.0F);
            MartialService.setMainArt(player, MartialArts.AZURE_CLOUD_SWORD);
            helper.assertTrue(FistPower.boost(player).isEmpty(), "A boost with a sword art as main style");
        });
        Zombie[] second = new Zombie[1];
        helper.runAfterDelay(22, () -> second[0] = dummy(helper, player, 0));
        punch(helper, player, 24);
        helper.runAfterDelay(44, () -> {
            float plain = 200 - second[0].getHealth();
            FistPower.Boost b = FistPower.BOOSTS.get(MartialArts.LONG_FIST);
            com.mojang.logging.LogUtils.getLogger().info("Fist power: a punch deals {} with the Long Fist as main style, {} without", boosted[0], plain);
            helper.assertTrue(plain > 0 && boosted[0] > 0, "A punch missed: boosted " + boosted[0] + ", plain " + plain);
            helper.assertTrue(boosted[0] >= plain + b.extra() * 0.9F, "The Long Fist's punch is not stronger: " + boosted[0] + " against " + plain);
            // A weapon in hand gets nothing.
            MartialService.setMainArt(player, MartialArts.LONG_FIST);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            helper.assertTrue(FistPower.boost(player).isEmpty(), "A boost with a sword in hand");
            second[0].discard();
            EpicFightBridge.patch(player).resetHolding();
            MartialCombat.clearForTests();
            cleanup(player);
            helper.succeed();
        });
    }
}
