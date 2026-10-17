package com.murimblock;

import com.murimblock.combat.ShatteredGround;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MajinFistArt;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.SonOfTheDemonEntity;
import com.murimblock.mob.majin.CruelSunEntity;
import com.murimblock.mob.majin.MajinGuard;
import com.murimblock.qi.QiService;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Majin Fist in a player's hands (docs/combat/styles/majin-fist.md): the scroll (Qi Liquidation), bare hands, each
 * technique striking where it should (the fist's burst, the flare's wave, the Cruel Sun's burst, the Noon Judgment's
 * pillars, the Midday Meteor's landing), The One's strength; the Son of the Demon drops the scroll and never takes more
 * than a third of a player's health in one blow.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MajinFistArtGameTests {
    private MajinFistArtGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);

    /** A student of the art, every technique on the bar (slots 0 to 5), bare-handed, on stone, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        CultivationService.setCultivation(player, MartialArts.MAJIN_REALM, CultivationStage.EARLY);
        new ItemStack(MurimItems.MAJIN_FIST_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        MartialData data = MartialService.data(player);
        MartialArts.Technique[] all = {MartialArts.SUNFIRE_FIST, MartialArts.PRIDE_FLARE, MartialArts.CRUEL_SUN, MartialArts.NOON_JUDGMENT,
                MartialArts.MIDDAY_METEOR, MartialArts.THE_ONE};
        for (int i = 0; i < all.length; i++) data = data.unlock(all[i]).assign(i, all[i].id());
        MartialService.setForTests(player, data);
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 2);
        BlockPos p = helper.absolutePos(CENTRE);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 2000);
        QiService.setQi(player, 2000);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
        zombie.setHealth(300);
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        helper.getLevel().getEntitiesOfClass(CruelSunEntity.class, new AABB(player.blockPosition()).inflate(40)).forEach(e -> e.discard());
        ShatteredGround.restoreAllForTests(helper.getLevel());
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "majin_fist_scroll")
    public static void theScrollTeachesFromQiLiquidationAndNeedsBareHands(GameTestHelper helper) {
        ServerPlayer novice = player(helper);
        CultivationService.setCultivation(novice, CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);
        new ItemStack(MurimItems.MAJIN_FIST_SCROLL.get()).use(novice.level(), novice, InteractionHand.MAIN_HAND);
        helper.assertFalse(MartialService.data(novice).knowsArt(MartialArts.MAJIN_FIST), "The scroll taught a reader below Qi Liquidation");
        cleanup(novice);
        ServerPlayer player = adept(helper);
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.knowsArt(MartialArts.MAJIN_FIST), "The scroll did not teach the art");
        helper.assertTrue(data.mainArt().equals(Optional.of(MartialArts.MAJIN_FIST)), "The first art read is not the main style");
        helper.assertTrue(data.passive().equals(Optional.of(MartialArts.PRIDE_OF_THE_SUN)), "Pride of the Sun is not the passive");
        helper.assertTrue(MajinFistArt.guardsBareHanded(player), "Bare hands do not raise the art's guard");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NO_WEAPON, "Sunfire Fist went with a sword in hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        end(helper, player);
    }

    @GameTest(template = "spear_arena", batch = "majin_fist_sunfire", timeoutTicks = 60)
    public static void sunfireFistBurstsAndBurns(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie z = dummy(helper, player, 0, 2.6);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Sunfire Fist was not cast");
        tickFor(helper, player, 20);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(z.getHealth() < 300, "Sunfire Fist's burst did not strike the target ahead");
            helper.assertTrue(z.getRemainingFireTicks() > 0, "Sunfire Fist did not set the target alight");
            end(helper, player, z);
        });
    }

    @GameTest(template = "spear_arena", batch = "majin_fist_flare", timeoutTicks = 60)
    public static void prideFlareRunsAlongTheGround(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie far = dummy(helper, player, 0, 8);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Pride Flare was not cast");
        tickFor(helper, player, 34);
        helper.runAfterDelay(34, () -> {
            helper.assertTrue(far.getHealth() < 300, "The flare's wave did not reach 8 blocks");
            end(helper, player, far);
        });
    }

    @GameTest(template = "spear_arena", batch = "majin_fist_sun", timeoutTicks = 100)
    public static void theCruelSunIsHurledAndBursts(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        player.setXRot(30);
        Zombie z = dummy(helper, player, 0, 3.5);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST, "Cruel Sun was not cast");
        tickFor(helper, player, 70);
        helper.runAfterDelay(20, () -> helper.assertTrue(
                !helper.getLevel().getEntitiesOfClass(CruelSunEntity.class, new AABB(player.blockPosition()).inflate(8)).isEmpty(), "No sun over the palm"));
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(z.getHealth() < 300, "The Cruel Sun's burst did not strike the target where it fell");
            end(helper, player, z);
        });
    }

    @GameTest(template = "spear_arena", batch = "majin_fist_judgment", timeoutTicks = 60)
    public static void noonJudgmentStrikesItsTargets(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie a = dummy(helper, player, 3, 3), b = dummy(helper, player, -3, 2);
        helper.assertTrue(MartialService.castSlot(player, 3) == CastResult.CAST, "Noon Judgment was not cast");
        tickFor(helper, player, 40);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(a.getHealth() < 300 && b.getHealth() < 300, "The pillars did not strike both targets");
            end(helper, player, a, b);
        });
    }

    @GameTest(template = "spear_arena", batch = "majin_fist_meteor", timeoutTicks = 60)
    public static void middayMeteorLandsWithABlast(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        // Looking down at the ground some three blocks ahead: the leap comes down there, by the target.
        player.setXRot(30);
        Zombie z = dummy(helper, player, 0.6, 3.2);
        helper.assertTrue(MartialService.castSlot(player, 4) == CastResult.CAST, "Midday Meteor was not cast");
        tickFor(helper, player, 16);
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(MajinFistArt.leaping(player), "Midday Meteor did not leap");
            MajinFistArt.landForTests(player);
            helper.assertFalse(MajinFistArt.leaping(player), "The leap did not land");
            helper.assertTrue(z.getHealth() < 300, "The landing did not strike the target near it");
            end(helper, player, z);
        });
    }

    @GameTest(template = "spear_arena", batch = "majin_fist_one", timeoutTicks = 40)
    public static void theOneGivesTheSunsStrength(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(MartialService.castSlot(player, 5) == CastResult.CAST, "The One was not cast");
        tickFor(helper, player, 5);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(MajinFistArt.inTheOne(player), "The One did not begin");
            helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > attack, "The One did not make the player stronger");
            end(helper, player);
        });
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon")
    public static void theSonOfTheDemonDropsTheMajinFist(GameTestHelper helper) {
        SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), MobSpawnType.COMMAND);
        helper.assertTrue(s != null, "No Son of the Demon spawned");
        s.setNoAi(true);
        ServerPlayer player = player(helper);
        s.hurt(helper.getLevel().damageSources().playerAttack(player), 100_000);
        helper.runAfterDelay(2, () -> {
            boolean dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(s.blockPosition()).inflate(6))
                    .stream().anyMatch(e -> e.getItem().is(MurimItems.MAJIN_FIST_SCROLL.get()));
            helper.assertTrue(dropped, "The Son of the Demon did not drop the Majin Fist scroll");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(s.blockPosition()).inflate(6)).forEach(e -> e.discard());
            cleanup(player);
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon")
    public static void theSonOfTheDemonNeverOneShots(GameTestHelper helper) {
        helper.assertTrue(MajinGuard.allowed(28, 0, 100) <= 28 * MajinGuard.HIT_SHARE + 1e-3, "A blow was not held to a third of the health");
        helper.assertTrue(MajinGuard.allowed(28, 28 * MajinGuard.WINDOW_SHARE, 10) == 0, "Blows past the window's share were not stopped");
        SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), MobSpawnType.COMMAND);
        helper.assertTrue(s != null, "No Son of the Demon spawned");
        s.setNoAi(true);
        ServerPlayer player = player(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        float before = player.getHealth();
        player.hurt(helper.getLevel().damageSources().mobAttack(s), 1000);
        helper.assertTrue(player.isAlive(), "A single blow of his killed a player at full health");
        helper.assertTrue(before - player.getHealth() <= player.getMaxHealth() * MajinGuard.HIT_SHARE + 0.01, "A blow took more than a third of the health: " + (before - player.getHealth()));
        s.discard();
        cleanup(player);
        helper.succeed();
    }
}
