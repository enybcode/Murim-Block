package com.murimblock;

import com.murimblock.integration.epicfight.BanditWarlordAnimations;
import com.murimblock.integration.epicfight.BanditWarlordPatch;
import com.murimblock.mob.BanditWarlordEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.world.structure.site.BanditCampCount;
import com.murimblock.world.structure.site.WarlordCrater;
import com.murimblock.world.structure.site.WarlordEntrance;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Bandit Warlord, the great bandit camps' boss: a boss on the biped skeleton with his own idle, walk and blows, a
 * boss bar, never despawning; he falls from the sky untouchable, lands with an impact that throws everyone back, and
 * where he falls on a camp it becomes a crater (chests kept); his Earth Breaker's shockwave hurts and throws up.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BanditWarlordGameTests {
    private BanditWarlordGameTests() { }

    private static BanditWarlordEntity warlord(GameTestHelper helper, BlockPos at) {
        BanditWarlordEntity w = MurimEntities.BANDIT_WARLORD.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(w != null, "No Bandit Warlord spawned");
        return w;
    }

    @GameTest(template = "foundation_arena", batch = "bandit_warlord")
    public static void theBanditWarlordIsABossOfHisOwn(GameTestHelper helper) {
        BanditWarlordEntity w = warlord(helper, new BlockPos(2, 1, 2));
        ServerPlayer player = player(helper);
        w.startSeenByPlayer(player);
        helper.runAfterDelay(3, () -> {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(w, LivingEntityPatch.class);
            helper.assertTrue(patch instanceof BanditWarlordPatch, "No Epic Fight patch on the Bandit Warlord");
            var idle = patch.getAnimator().getLivingAnimation(LivingMotions.IDLE, null);
            helper.assertTrue(idle == BanditWarlordAnimations.IDLE, "He should stand in his own idle, not " + idle);
            var walk = patch.getAnimator().getLivingAnimation(LivingMotions.WALK, null);
            helper.assertTrue(walk == BanditWarlordAnimations.WALK, "He should walk his own heavy walk, not " + walk);
            helper.assertTrue(w.getMaxHealth() == 400, "A boss of 400 health, not " + w.getMaxHealth());
            helper.assertTrue(w.getAttributeValue(Attributes.ARMOR) >= 12, "His cuirass should give 12 armour");
            helper.assertTrue(w.isPersistenceRequired() && !w.removeWhenFarAway(1.0E6), "The Bandit Warlord could despawn");
            helper.assertTrue(w.showsBossBarTo(player), "The player near him does not see his boss bar");
            w.discard();
            cleanup(player);
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "bandit_warlord", timeoutTicks = 300)
    public static void heFallsFromTheSkyUntouchableAndLandsWithAnImpact(GameTestHelper helper) {
        BanditWarlordEntity w = warlord(helper, new BlockPos(3, 30, 3));
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(5, 2, 3));
        pig.setNoAi(true);
        w.fallFromSky(null);
        helper.assertTrue(w.isMeteor(), "He should be falling as a meteor");
        float health = w.getHealth();
        w.hurt(helper.getLevel().damageSources().generic(), 50);
        helper.assertTrue(w.getHealth() == health, "He was hurt while falling");
        double pigX = pig.getX();
        helper.succeedWhen(() -> {
            helper.assertFalse(w.isMeteor(), "He has not landed yet, at y " + w.getY());
            helper.assertTrue(w.isArriving(), "He should be landing, untouchable");
            helper.assertTrue(w.getHealth() == health, "His fall hurt him");
            helper.assertTrue(Math.abs(pig.getX() - pigX) > 0.5 || pig.getDeltaMovement().horizontalDistance() > 0.05, "The impact did not throw the pig back");
            w.discard();
            pig.discard();
        });
    }

    @GameTest(template = "foundation_arena", batch = "bandit_warlord")
    public static void hisEarthBreakerHurtsAndThrowsUp(GameTestHelper helper) {
        BanditWarlordEntity w = warlord(helper, new BlockPos(2, 1, 2));
        w.setYRot(0);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 5));
        pig.setNoAi(true);
        float health = pig.getHealth();
        w.earthShockwave(helper.getLevel());
        helper.assertTrue(pig.getHealth() < health, "The shockwave did not hurt the pig");
        helper.assertTrue(pig.getDeltaMovement().y > 0.3, "The shockwave did not throw the pig up: " + pig.getDeltaMovement());
        w.discard();
        pig.discard();
        helper.succeed();
    }

    /** A crater opened on flat ground far from the others: the bowl's floor at its depth, the chests kept, the ground beyond untouched. */
    @GameTest(template = "foundation_arena", batch = "bandit_warlord_crater", timeoutTicks = 600)
    public static void theCraterIsABowlThatKeepsTheChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int cx = 11_208, cz = 8, g = 70, reach = 50;
        for (int x = (cx - reach) >> 4; x <= (cx + reach) >> 4; x++) for (int z = (cz - reach) >> 4; z <= (cz + reach) >> 4; z++) level.setChunkForced(x, z, true);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        try {
            for (int x = cx - reach; x <= cx + reach; x++) for (int z = cz - reach; z <= cz + reach; z++) for (int y = g - 14; y <= g + 12; y++)
                level.setBlock(p.set(x, y, z), y <= g ? Blocks.STONE.defaultBlockState() : y == g + 4 ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
            BlockPos chest = new BlockPos(cx + 10, g + 1, cz);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.NORTH), 2);
            WarlordCrater.openNow(level, new BlockPos(cx, g, cz), g);
            int floor = WarlordCrater.floor(g, 0);
            helper.assertTrue(floor == g - WarlordCrater.DEPTH, "The bowl's heart should be " + WarlordCrater.DEPTH + " deep, not " + (g - floor));
            helper.assertTrue(!level.getBlockState(new BlockPos(cx, floor, cz)).isAir(), "No floor at the bowl's heart");
            helper.assertTrue(level.getBlockState(new BlockPos(cx, floor + 1, cz)).isAir(), "The bowl's heart is not hollow");
            helper.assertTrue(level.getBlockState(new BlockPos(cx + 5, g, cz)).isAir(), "The ground in the bowl is still there");
            helper.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "The crater broke a chest");
            helper.assertTrue(level.getBlockState(new BlockPos(cx + 40, g, cz)).is(Blocks.STONE), "The crater dug the ground beyond the bowl");
            helper.assertTrue(level.getBlockState(new BlockPos(cx, g + 4, cz + 3)).isAir(), "A floor stands over the bowl");
            helper.succeed();
        } finally {
            for (int x = cx - reach; x <= cx + reach; x++) for (int z = cz - reach; z <= cz + reach; z++) for (int y = g - 14; y <= g + 12; y++)
                if (!level.getBlockState(p.set(x, y, z)).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    /** A camp's warlord let fall: up in the sky over its plaza, a meteor, and noted (he comes once). */
    @GameTest(template = "foundation_arena", batch = "bandit_warlord")
    public static void aCampsWarlordFallsOnceOverItsPlaza(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plaza = helper.absolutePos(new BlockPos(2, 1, 2));
        long camp = BanditCampCount.camp(plaza.getX(), plaza.getZ());
        BanditCampCount.forgetForTests(level, camp);
        BanditWarlordEntity w = WarlordEntrance.fallForTests(level, camp, plaza);
        helper.assertTrue(w != null && w.isMeteor(), "No warlord falling");
        helper.assertTrue(w.getY() > plaza.getY() + 40, "He should fall from high above the plaza, not from y " + w.getY());
        helper.assertTrue(BanditCampCount.warlordCame(level, camp), "The camp does not know its warlord came");
        w.discard();
        BanditCampCount.forgetForTests(level, camp);
        helper.succeed();
    }

    /** A floor of stone two thick (y 2 and 3) over the arena's, from (x0, z0) to (x1, z1): ground a blow can break. */
    private static void stone(GameTestHelper helper, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = 2; y <= 3; y++)
            helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)), Blocks.STONE.defaultBlockState(), 2);
    }

    /** The Mountain Collapse: a fissure runs out ahead of him, breaking the ground; whoever is on it is hurt and thrown up. */
    @GameTest(template = "foundation_arena", batch = "bandit_warlord_ground", timeoutTicks = 100)
    public static void hisCollapseSplitsTheGroundAndItComesBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        stone(helper, 0, 0, 3, 15);
        BanditWarlordEntity w = warlord(helper, new BlockPos(1, 4, 1));
        w.setNoAi(true);
        // Facing along the stone strip (the test's frame may be turned in the world).
        net.minecraft.world.phys.Vec3 toward = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 4, 12))).subtract(w.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
        w.setYRot(yaw);
        w.yBodyRot = yaw;
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 4, 10));
        pig.setNoAi(true);
        float health = pig.getHealth();
        w.startFissure(level);
        for (int i = 0; i < BanditWarlordEntity.FISSURE_LENGTH; i++) w.fissureStep(level);
        helper.assertTrue(pig.getHealth() < health, "The fissure did not hurt the pig on it");
        helper.assertTrue(com.murimblock.combat.ShatteredGround.waiting(level) > 10, "The fissure broke no ground: " + com.murimblock.combat.ShatteredGround.waiting(level));
        BlockPos split = helper.absolutePos(new BlockPos(1, 3, 9));
        helper.assertTrue(level.getBlockState(split).isAir(), "The ground on the fissure's line is whole: " + level.getBlockState(split));
        com.murimblock.combat.ShatteredGround.restoreAllForTests(level);
        helper.assertTrue(!level.getBlockState(split).isAir(), "The broken ground did not come back");
        w.discard();
        pig.discard();
        helper.succeed();
    }

    /** The Thousand-Jin Drop's landing: a crater round him and a shockwave that hurts and throws; a chest is never broken. */
    @GameTest(template = "foundation_arena", batch = "bandit_warlord_ground", timeoutTicks = 100)
    public static void hisDropLeavesACraterThatSparesChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        stone(helper, 0, 0, 9, 9);
        BanditWarlordEntity w = warlord(helper, new BlockPos(4, 4, 4));
        w.setNoAi(true);
        BlockPos chest = helper.absolutePos(new BlockPos(6, 3, 4));
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 4, 7));
        pig.setNoAi(true);
        float health = pig.getHealth();
        w.leapLand(level);
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(4, 3, 4))).isAir(), "No crater under him");
        helper.assertTrue(level.getBlockState(chest).is(Blocks.CHEST), "The crater broke a chest");
        helper.assertTrue(pig.getHealth() < health && pig.getDeltaMovement().y > 0.1, "The shockwave did not hurt and throw the pig");
        com.murimblock.combat.ShatteredGround.restoreAllForTests(level);
        helper.assertTrue(!level.getBlockState(helper.absolutePos(new BlockPos(4, 3, 4))).isAir(), "The crater did not fill back");
        level.setBlock(chest, Blocks.AIR.defaultBlockState(), 2);
        w.discard();
        pig.discard();
        helper.succeed();
    }

    /** With the game rule murimDestruction off, nothing breaks. */
    @GameTest(template = "foundation_arena", batch = "bandit_warlord_ground")
    public static void theGameRuleKeepsTheGroundWhole(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        stone(helper, 2, 2, 2, 2);
        BlockPos ground = helper.absolutePos(new BlockPos(2, 3, 2));
        level.getGameRules().getRule(com.murimblock.combat.ShatteredGround.RULE).set(false, level.getServer());
        try {
            helper.assertFalse(com.murimblock.combat.ShatteredGround.smash(level, ground), "A block broke with murimDestruction off");
        } finally {
            level.getGameRules().getRule(com.murimblock.combat.ShatteredGround.RULE).set(true, level.getServer());
        }
        helper.succeed();
    }
}
