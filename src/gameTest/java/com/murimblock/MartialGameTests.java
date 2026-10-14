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
 * The Azure Cloud Sword Art's first slice: the sanctuary library hands each player one manual, reading it teaches the
 * art and puts Cloud Step Slash on the combat bar, the cast checks (combat mode, Qi, sword, cooldown), and a real Cloud
 * Step Slash cutting a zombie through Epic Fight.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MartialGameTests {
    private MartialGameTests() { }

    @GameTest(template = "foundation_arena", batch = "martial_library")
    public static void theLibraryLecternGivesEachPlayerOneManual(GameTestHelper helper) {
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
            var first = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, lectern, hit);
            SectLibrary.onRightClickBlock(first);
            helper.assertTrue(first.isCanceled(), "The library lectern did not answer");
            helper.assertTrue(player.getInventory().countItem(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get()) == 1, "No manual from the library");
            SectLibrary.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, lectern, hit));
            helper.assertTrue(player.getInventory().countItem(MurimItems.AZURE_CLOUD_SWORD_MANUAL.get()) == 1, "The library gave a second copy");
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
            helper.assertTrue(!manual.isEmpty(), "Reading used up the manual");
            manual.use(player.level(), player, InteractionHand.MAIN_HAND);
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
