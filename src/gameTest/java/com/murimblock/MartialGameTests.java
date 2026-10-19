package com.murimblock;

import com.murimblock.combat.CombatService;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.martial.SectLibrary;
import com.murimblock.qi.QiService;
import com.murimblock.world.structure.SanctuaryBuilder;
import com.murimblock.world.structure.SanctuaryPlacement;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Azure Cloud Sword Art's first slice: Sensei Yun hands each player one manual (the sanctuary's lectern no longer does), reading it teaches the
 * art and puts Cloud Step Slash on the combat bar, the cast checks (combat mode, Qi, sword, cooldown), and a real Cloud
 * Step Slash cutting a zombie through Epic Fight.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MartialGameTests {
    private MartialGameTests() { }

    @GameTest(template = "foundation_arena", batch = "martial_library")
    public static void theLibraryLecternNoLongerGivesTheManual(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        SanctuaryPlacement data = SanctuaryPlacement.get(player.server.overworld());
        // A sanctuary recorded around the arena, so its library lectern falls on a block of the arena.
        BlockPos lectern = helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos centre = new BlockPos(lectern.getX() - 13, lectern.getY() - 2 - 70, lectern.getZ() - 1);
        data.record(centre, centre, lectern.getY() - 2);
        try {
            helper.assertTrue(SanctuaryBuilder.libraryLectern(centre, lectern.getY() - 2).equals(lectern), "Library lectern position mismatch");
            helper.getLevel().setBlockAndUpdate(lectern, Blocks.LECTERN.defaultBlockState());
            player.getInventory().clearContent();
            var hit = new BlockHitResult(Vec3.atCenterOf(lectern), Direction.UP, lectern, false);
            // Sensei Yun is the manual's only source: the sanctuary's lectern is an ordinary lectern.
            var click = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, lectern, hit);
            SectLibrary.onRightClickBlock(click);
            helper.assertTrue(!click.isCanceled(), "The library lectern still answers");
            helper.assertTrue(player.getInventory().countItem(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get()) == 0, "The library still gives the manual");
            helper.succeed();
        } finally {
            data.clear();
            cleanup(player);
        }
    }

    @GameTest(template = "foundation_arena", batch = "martial_learn")
    public static void readingTheManualTeachesTheArtAndFillsTheBar(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            ItemStack manual = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, manual);
            manual.use(player.level(), player, InteractionHand.MAIN_HAND);
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.knowsArt(MartialArts.AZURE_CLOUD_SWORD), "The art was not learned");
            helper.assertTrue(data.hasTechnique(MartialArts.CLOUD_STEP_SLASH.id()), "Cloud Step Slash was not unlocked");
            helper.assertTrue(!data.hasTechnique(MartialArts.THOUSAND_DRIZZLE.id()), "Thousand Drizzle came with the book");
            helper.assertTrue(data.slot(0).equals(Optional.of(MartialArts.CLOUD_STEP_SLASH.id())), "Cloud Step Slash is not in slot 1");
            // One manual, one reading.
            helper.assertTrue(manual.isEmpty(), "Reading did not use up the manual");
            // Another copy, read by someone who knows the art already: kept (to give away), and nothing changes.
            ItemStack another = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, another);
            another.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(!another.isEmpty(), "A manual read by someone who knew the art was used up");
            helper.assertTrue(MartialService.data(player).equals(data), "Reading again changed something");
            helper.succeed();
        } finally {
            cleanup(player);
        }
    }

    @GameTest(template = "foundation_arena", batch = "martial_commands")
    public static void theMartialCommandTeachesForgetsAndSetsMastery(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        var source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        java.util.function.Consumer<String> run = command -> player.server.getCommands().performPrefixedCommand(source, command);
        try {
            run.accept("martial unlock all");
            MartialData data = MartialService.data(player);
            helper.assertTrue(data.knowsArt(MartialArts.AZURE_CLOUD_SWORD), "unlock all: the art is not known");
            helper.assertTrue(data.hasTechnique(MartialArts.HEAVEN_SPLITTING_CRESCENT.id()), "unlock all: a technique is missing");
            helper.assertTrue(data.slot(2).equals(Optional.of(MartialArts.HEAVEN_SPLITTING_CRESCENT.id())), "unlock all: not placed on the bar");
            run.accept("martial mastery set cloud_step_slash 40");
            run.accept("martial mastery add cloud_step_slash 75");
            helper.assertTrue(MartialService.data(player).mastery(MartialArts.CLOUD_STEP_SLASH.id()) == 100, "mastery is not capped at 100");
            run.accept("martial bar set 10 murimblock:cloud_step_slash");
            data = MartialService.data(player);
            helper.assertTrue(data.slot(9).equals(Optional.of(MartialArts.CLOUD_STEP_SLASH.id())) && data.slot(0).isEmpty(), "bar set did not move the technique to slot 10");
            run.accept("martial bar clear 10");
            helper.assertTrue(MartialService.data(player).slot(9).isEmpty(), "bar clear did not empty slot 10");
            run.accept("martial forget thousand_drizzle");
            helper.assertTrue(!MartialService.data(player).hasTechnique(MartialArts.THOUSAND_DRIZZLE.id()), "forget a technique");
            run.accept("martial forget azure_cloud_sword");
            helper.assertTrue(!MartialService.data(player).knowsArt(MartialArts.AZURE_CLOUD_SWORD), "forget an art");
            run.accept("martial forget long_fist");
            run.accept("martial forget sanguine_art");
            run.accept("martial forget celestial_peak_spear");
            run.accept("martial forget traveller_staff");
            run.accept("martial forget mountain_shattering_fist");
            run.accept("martial forget majin_fist");
            run.accept("martial forget seven_stars_sword");
            run.accept("martial forget thunder_saber");
            run.accept("martial forget golden_bell");
            run.accept("martial forget overlord_spear");
            run.accept("martial forget devouring_demon");
            run.accept("martial forget crescent_moon_halberd");
            helper.assertTrue(MartialService.data(player).equals(MartialData.EMPTY), "forgetting the art left something: " + MartialService.data(player));
            run.accept("martial learn azure_cloud_sword");
            data = MartialService.data(player);
            helper.assertTrue(data.hasTechnique(MartialArts.CLOUD_STEP_SLASH.id()) && !data.hasTechnique(MartialArts.THOUSAND_DRIZZLE.id()), "learn is not like reading the manual");
            run.accept("martial cooldown reset");
            run.accept("martial info");
            helper.succeed();
        } finally {
            cleanup(player);
        }
    }

    /** A player who knows the art, sword in hand, combat mode on, full Qi. */
    private static ServerPlayer adept(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack manual = new ItemStack(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get());
        manual.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        // A mock player is never told it stands on the ground: without this, the techniques take their air variants.
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        return player;
    }

    @GameTest(template = "foundation_arena", batch = "martial_checks")
    public static void aTechniqueNeedsCombatModeQiASwordAndItsCooldown(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        try {
            helper.assertTrue(MartialService.castSlot(player, 5) == CastResult.NO_TECHNIQUE, "An empty slot cast something");
            CombatService.setCombatMode(player, false);
            helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NOT_IN_COMBAT, "Cast outside combat mode");
            CombatService.setCombatMode(player, true);
            QiService.setQi(player, 5);
            helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NO_QI, "Cast without enough Qi");
            QiService.setQi(player, 100);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.NO_WEAPON, "A sword art without a sword");
            helper.assertTrue(QiService.getQi(player) == 100, "A refused cast cost Qi");
            helper.succeed();
        } finally {
            cleanup(player);
        }
    }

    @GameTest(template = "foundation_arena", batch = "martial_power")
    public static void aTechniqueStrikesAsItsWeaponAndItsQiAllow(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        try {
            // An iron sword (6), 100 Qi Max: the technique as authored; 400 Qi Max: +24 %. (A tick puts the sword's attack on.)
            player.doTick();
            helper.assertTrue(Math.abs(com.murimblock.martial.TechniquePower.factor(player, MartialArts.AZURE_CLOUD_SWORD) - 1.0) < 1e-6, "A beginner's sword technique was scaled");
            QiService.setQiMax(player, 400);
            helper.assertTrue(Math.abs(com.murimblock.martial.TechniquePower.damage(player, MartialArts.AZURE_CLOUD_SWORD, 1.0) - 6 * 1.24) < 1e-4,
                    "400 Qi Max with an iron sword: 6 x 1.24, got " + com.murimblock.martial.TechniquePower.damage(player, MartialArts.AZURE_CLOUD_SWORD, 1.0));
            // Bare hands for the Long Fist: the hand (1) and its training (3).
            QiService.setQiMax(player, 100);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.doTick();
            helper.assertTrue(Math.abs(com.murimblock.martial.TechniquePower.damage(player, MartialArts.LONG_FIST, 1.0) - 4) < 1e-4,
                    "A beginner's trained fist should strike as 4, got " + com.murimblock.martial.TechniquePower.damage(player, MartialArts.LONG_FIST, 1.0));
            helper.succeed();
        } finally {
            cleanup(player);
        }
    }

    @GameTest(template = "foundation_arena", batch = "martial_burst", timeoutTicks = 100)
    public static void noComboFellsAPlayerFromFullHealth(GameTestHelper helper) {
        helper.getLevel().getServer().setPvpAllowed(true);
        ServerPlayer attacker = player(helper), victim = player(helper);
        com.murimblock.combat.BurstGuard.clearForTests();
        // Three blows of 8 in a row on 20 health: 8, 4, then nothing (60 % at most in three seconds).
        for (int i = 0; i < 3; i++) {
            victim.invulnerableTime = 0;
            victim.hurt(victim.damageSources().playerAttack(attacker), 8);
        }
        helper.assertTrue(Math.abs(victim.getHealth() - 8) < 1e-3, "A combo took more than 60 %: health " + victim.getHealth());
        helper.assertTrue(!com.murimblock.combat.knockdown.KnockdownService.isDowned(victim), "A combo knocked a full-health player down");
        // Three seconds later, a new combo may go on.
        helper.runAfterDelay(com.murimblock.combat.BurstGuard.WINDOW_TICKS + 2, () -> {
            try {
                victim.invulnerableTime = 0;
                victim.hurt(victim.damageSources().playerAttack(attacker), 4);
                helper.assertTrue(Math.abs(victim.getHealth() - 4) < 1e-3, "The next combo dealt nothing: health " + victim.getHealth());
                helper.succeed();
            } finally {
                com.murimblock.combat.knockdown.KnockdownService.clearForTests();
                helper.getLevel().getServer().setPvpAllowed(false);
                cleanup(attacker);
                cleanup(victim);
            }
        });
    }

    @GameTest(template = "foundation_arena", batch = "martial_cast", timeoutTicks = 80)
    public static void cloudStepSlashCutsAZombieOnItsPathAndHasACooldown(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 1));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0); // facing +Z
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        zombie.setNoAi(true);
        zombie.moveTo(player.getX(), player.getY(), player.getZ() + 2.0);
        float health = zombie.getHealth();
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Cloud Step Slash was refused");
        helper.assertTrue(QiService.getQi(player) == 100 - MartialArts.CLOUD_STEP_SLASH.qiCost(), "Cloud Step Slash did not cost its Qi");
        helper.assertTrue(MartialService.castSlot(player, 0) != CastResult.CAST, "Cast again at once");
        // The server-side animator runs on the player's tick (a mock player is ticked by hand).
        for (int t = 1; t <= 30; t++) helper.runAfterDelay(t, player::doTick);
        helper.runAfterDelay(35, () -> {
            try {
                helper.assertTrue(zombie.getHealth() < health, "Cloud Step Slash did not cut the zombie (" + zombie.getHealth() + ")");
                helper.assertTrue(MartialService.cooldownLeft(player, MartialArts.CLOUD_STEP_SLASH.id()) > 0, "No cooldown");
                helper.succeed();
            } finally {
                zombie.discard();
                cleanup(player);
            }
        });
    }
}
