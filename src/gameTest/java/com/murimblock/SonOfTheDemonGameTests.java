package com.murimblock;

import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.integration.epicfight.SonOfTheDemonPatch;
import com.murimblock.mob.BanditWarlordEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.SonOfTheDemonEntity;
import com.murimblock.mob.majin.MajinFist;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import com.murimblock.world.structure.site.SkyArena;
import com.murimblock.world.structure.site.WarlordTransformation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Bandit Warlord's second phase: a blow that would kill him begins his transformation instead (untouchable, still,
 * his bar gone, the players near frozen and unhurt), and at its flash the Son of the Demon takes his place: a boss on
 * his own armature, with his own idle and walk, untouchable while the film plays, then fighting, his bar titled.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SonOfTheDemonGameTests {
    private SonOfTheDemonGameTests() { }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon")
    public static void theSonOfTheDemonStandsOnHisOwnArmature(GameTestHelper helper) {
        SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), MobSpawnType.COMMAND);
        helper.assertTrue(s != null, "No Son of the Demon spawned");
        ServerPlayer player = player(helper);
        s.startSeenByPlayer(player);
        helper.runAfterDelay(3, () -> {
            // Gone first, whatever fails below: the next test counts the colossi round it.
            boolean bar = s.showsBossBarTo(player);
            s.discard();
            cleanup(player);
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(s, LivingEntityPatch.class);
            helper.assertTrue(patch instanceof SonOfTheDemonPatch, "No Epic Fight patch on the Son of the Demon");
            helper.assertTrue(patch.getArmature() != Armatures.BIPED.get(), "He should stand on his own armature, not the biped's");
            var idle = patch.getAnimator().getLivingAnimation(LivingMotions.IDLE, null);
            helper.assertTrue(idle == ColossusAnimations.IDLE, "He should stand in his own idle, not " + idle);
            var walk = patch.getAnimator().getLivingAnimation(LivingMotions.WALK, null);
            helper.assertTrue(walk == ColossusAnimations.WALK, "He should walk his own walk, not " + walk);
            helper.assertTrue(s.getMaxHealth() == 420, "A boss of 420 health, not " + s.getMaxHealth());
            helper.assertTrue(s.getBbHeight() > 7, "Some seven blocks tall, not " + s.getBbHeight());
            helper.assertTrue(bar, "The player near him does not see his boss bar");
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon_film", timeoutTicks = WarlordTransformation.END + 80)
    public static void aKillingBlowTurnsTheWarlordIntoTheSonOfTheDemon(GameTestHelper helper) {
        // The film without its arena (SkyArena.enabled): he lands on the ground; the arena has its own test and the client film.
        SkyArena.enabled = false;
        BanditWarlordEntity w = MurimEntities.BANDIT_WARLORD.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), MobSpawnType.COMMAND);
        helper.assertTrue(w != null, "No Bandit Warlord spawned");
        ServerPlayer player = player(helper);
        player.teleportTo(w.getX() + 4, w.getY(), w.getZ());
        w.startSeenByPlayer(player);
        w.hurt(helper.getLevel().damageSources().playerAttack(player), 10_000);
        helper.assertTrue(w.isAlive(), "The killing blow killed him instead of transforming him");
        helper.assertTrue(w.transforming(), "The killing blow did not begin his transformation");
        helper.assertTrue(!w.showsBossBarTo(player) || w.getHealth() > 0, "His bar should go");
        float health = player.getHealth();
        SonOfTheDemonEntity[] son = new SonOfTheDemonEntity[1];
        helper.runAfterDelay(20, () -> {
            w.hurt(helper.getLevel().damageSources().playerAttack(player), 10_000);
            helper.assertTrue(w.isAlive(), "He was hurt while transforming");
            helper.assertTrue(w.getTarget() == null, "He has a foe while transforming");
        });
        helper.runAfterDelay(WarlordTransformation.SWAP + 5, () -> {
            helper.assertTrue(w.isRemoved(), "The warlord is still there after the flash");
            AABB around = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48);
            var sons = helper.getLevel().getEntitiesOfClass(SonOfTheDemonEntity.class, around);
            helper.assertTrue(sons.size() == 1, "One Son of the Demon should burst out, not " + sons.size());
            SonOfTheDemonEntity s = sons.get(0);
            son[0] = s;
            // A mock player is never tracked: shown his bar by hand, as a real one near him would be.
            s.startSeenByPlayer(player);
            helper.assertTrue(s.awakening(), "He should be awakening while the film plays");
            helper.assertFalse(s.showsBossBarTo(player), "His bar shows during the film");
            s.hurt(helper.getLevel().damageSources().playerAttack(player), 100);
            helper.assertTrue(s.getHealth() == s.getMaxHealth(), "He was hurt while the film plays");
            helper.assertTrue(player.getHealth() >= health, "The player was hurt during the film");
        });
        helper.runAfterDelay(WarlordTransformation.END + 10, () -> {
            // He has leapt into the sky: onto the arena over the camp, far above the test.
            SonOfTheDemonEntity s = son[0];
            helper.assertTrue(s != null && s.isAlive(), "The Son of the Demon is gone");
            helper.assertFalse(s.awakening(), "The fight did not begin at the film's end");
            helper.assertTrue(s.showsBossBarTo(player), "His bar is not shown when the fight begins");
            s.hurt(helper.getLevel().damageSources().playerAttack(player), 10);
            helper.assertTrue(s.getHealth() < s.getMaxHealth(), "He cannot be hurt once the fight begins");
            s.discard();
            cleanup(player);
            SkyArena.enabled = true;
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon")
    public static void theSkyArenaShipsInTheMod(GameTestHelper helper) {
        SkyArena.Schematic arena = SkyArena.schematic(helper.getLevel());
        helper.assertTrue(arena != null, "The sky arena's schematic is not in the mod");
        helper.assertTrue(arena.width() == 195 && arena.height() == 173 && arena.length() == 204,
                "The arena should be 195 x 173 x 204, not " + arena.width() + " x " + arena.height() + " x " + arena.length());
        long solid = java.util.Arrays.stream(arena.cells()).filter(i -> !arena.palette()[i].isAir()).count();
        helper.assertTrue(solid == 1_475_785, "1,475,785 blocks expected, not " + solid);
        // The fighting floor under the paste point, whole and open above.
        helper.assertFalse(arena.palette()[cell(arena, 0, -1, 0)].isAir(), "No floor under the paste point");
        for (int y = 0; y < 26; y++) helper.assertTrue(arena.palette()[cell(arena, 0, y, 0)].isAir(), "Something stands over the floor's middle at " + y);
        BlockPos middle = SkyArena.floorMiddle(helper.getLevel(), 0, 0);
        helper.assertTrue(middle.getY() + 56 < helper.getLevel().getMaxBuildHeight(), "The arena would reach above the world: floor at " + middle.getY());
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon")
    public static void hisFightingIsBuiltFromTheMajinFist(GameTestHelper helper) {
        helper.assertFalse(MajinFist.MOVES.isEmpty(), "The Majin Fist has no blow");
        for (MajinFist.Move move : MajinFist.MOVES) {
            helper.assertTrue(move.animation().get() != null, move.id() + " has no animation");
            helper.assertTrue(move.minRange() < move.maxRange() && move.cooldown() > 0 && move.weight() > 0, move.id() + " is not set up");
            helper.assertTrue(MajinFist.moveOf(move.animation().get()) == move, move.id() + " is not found by its animation");
            for (int b = 1; b < move.beats().size(); b++)
                helper.assertTrue(move.beats().get(b).at() >= move.beats().get(b - 1).at(), move.id() + "'s beats are out of order");
        }
        helper.assertTrue(MajinFist.behaviors() != null, "His fighting could not be built");
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon")
    public static void aMajinBlastHurtsAndThrowsBack(GameTestHelper helper) {
        SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.COMMAND);
        helper.assertTrue(s != null, "No Son of the Demon spawned");
        s.setNoAi(true);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(5, 1, 2));
        pig.setNoAi(true);
        float health = pig.getHealth();
        var struck = MajinFist.blast(s, helper.getLevel(), s.position(), 6, 0.5F, 0.6);
        helper.assertTrue(struck.contains(pig) && pig.getHealth() < health, "The blast did not strike the pig beside him");
        helper.assertTrue(pig.getDeltaMovement().y > 0.1, "The blast did not throw the pig up");
        s.discard();
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon_arena", timeoutTicks = 60)
    public static void whoFallsOffTheArenaIsBroughtBack(GameTestHelper helper) {
        SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.COMMAND);
        helper.assertTrue(s != null, "No Son of the Demon spawned");
        s.setNoAi(true);
        // An arena whose floor stands 30 blocks over the test: the player, here, has fallen off it.
        BlockPos floor = helper.absolutePos(new BlockPos(2, 31, 2));
        for (int x = -50; x <= 50; x++) for (int z = -50; z <= 50; z++)
            helper.getLevel().setBlock(floor.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        s.setArena(floor);
        ServerPlayer player = player(helper);
        player.teleportTo(helper.getLevel(), floor.getX() + 10.5, floor.getY() - 30, floor.getZ() + 0.5, 0, 0);
        float health = player.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(player.getY() >= floor.getY() - 1, "The fallen player was not brought back (y " + player.getY() + ")");
            helper.assertTrue(player.getHealth() < health, "Falling off cost nothing");
            for (int x = -50; x <= 50; x++) for (int z = -50; z <= 50; z++)
                helper.getLevel().setBlock(floor.offset(x, -1, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            s.discard();
            cleanup(player);
        });
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon_collapse", timeoutTicks = com.murimblock.world.structure.site.ArenaCollapse.DOWN + 60)
    public static void hisDeathTakesTheArenaAndSetsEveryoneDown(GameTestHelper helper) {
        // The arena without its blocks (SkyArena.enabled): a small floor 30 blocks up stands for it.
        SkyArena.enabled = false;
        BlockPos floor = helper.absolutePos(new BlockPos(2, 31, 2));
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
            helper.getLevel().setBlock(floor.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().spawn(helper.getLevel(), floor, MobSpawnType.COMMAND);
        helper.assertTrue(s != null, "No Son of the Demon spawned");
        s.setNoAi(true);
        s.setArena(floor);
        ServerPlayer player = player(helper);
        player.teleportTo(helper.getLevel(), floor.getX() + 2.5, floor.getY(), floor.getZ() + 0.5, 0, 0);
        var loot = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), floor.getX() + 1.5, floor.getY(), floor.getZ() + 1.5,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT));
        loot.setPickUpDelay(400);
        helper.getLevel().addFreshEntity(loot);
        s.hurt(helper.getLevel().damageSources().playerAttack(player), 100_000);
        helper.assertTrue(com.murimblock.world.structure.site.ArenaCollapse.collapsing(), "His death did not begin the arena's collapse");
        helper.runAfterDelay(com.murimblock.world.structure.site.ArenaCollapse.DOWN + 10, () -> {
            helper.assertTrue(player.getY() < floor.getY() - 5, "The player was not set down below the arena (y " + player.getY() + ")");
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING), "The player was set down without slow falling");
            helper.assertTrue(loot.isAlive() && loot.getY() < floor.getY() - 5, "His loot was left on the arena");
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
                helper.getLevel().setBlock(floor.offset(x, -1, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            loot.discard();
            cleanup(player);
            SkyArena.enabled = true;
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "son_of_the_demon", timeoutTicks = 40)
    public static void theOneComesOnceAtAThirdOfHisLife(GameTestHelper helper) {
        SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.COMMAND);
        helper.assertTrue(s != null, "No Son of the Demon spawned");
        s.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(s.theOneTicks() == 0, "The One came at full life");
            s.setHealth(s.getMaxHealth() * 0.3F);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(s.theOneUsed() && s.theOneTicks() > 0, "The One did not come at a third of his life");
            float health = s.getHealth();
            s.hurt(helper.getLevel().damageSources().generic(), 50);
            helper.assertTrue(s.getHealth() == health, "He was hurt while rising into The One");
            s.discard();
            helper.getLevel().getEntitiesOfClass(com.murimblock.mob.majin.CruelSunEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40)).forEach(e -> e.discard());
            helper.succeed();
        });
    }

    private static int cell(SkyArena.Schematic s, int x, int y, int z) {
        int[] o = s.offset();
        return s.cells()[(x - o[0]) + (z - o[2]) * s.width() + (y - o[1]) * s.width() * s.length()];
    }
}
