package com.murimblock;

import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.integration.epicfight.SonOfTheDemonPatch;
import com.murimblock.mob.BanditWarlordEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.SonOfTheDemonEntity;
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
            helper.assertTrue(s.getMaxHealth() == 500, "A boss of 500 health, not " + s.getMaxHealth());
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

    private static int cell(SkyArena.Schematic s, int x, int y, int z) {
        int[] o = s.offset();
        return s.cells()[(x - o[0]) + (z - o[2]) * s.width() + (y - o[1]) * s.width() * s.length()];
    }
}
