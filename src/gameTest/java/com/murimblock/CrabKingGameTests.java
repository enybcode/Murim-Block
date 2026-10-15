package com.murimblock;

import com.murimblock.integration.epicfight.CrabKingAnimations;
import com.murimblock.integration.epicfight.CrabKingPatch;
import com.murimblock.mob.ArmoredCrabEntity;
import com.murimblock.mob.CrabKingEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Crab King, the crab art's boss: a boss on the biped skeleton with his own idle and pincer attacks, a boss bar,
 * never despawning, breathing under water; under half his health he roars and calls two armoured crabs, once; his
 * slam's shockwave hurts whoever is near.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrabKingGameTests {
    private CrabKingGameTests() { }

    private static CrabKingEntity king(GameTestHelper helper, BlockPos at) {
        CrabKingEntity king = MurimEntities.CRAB_KING.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(king != null, "No Crab King spawned");
        return king;
    }

    @GameTest(template = "foundation_arena", batch = "crab_king_boss")
    public static void theCrabKingIsABossOfHisOwn(GameTestHelper helper) {
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 2));
        ServerPlayer player = player(helper);
        // A mock player is not tracked: he is shown to it by hand, as the server shows him to a real one.
        king.startSeenByPlayer(player);
        helper.runAfterDelay(3, () -> {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(king, LivingEntityPatch.class);
            helper.assertTrue(patch instanceof CrabKingPatch, "No Epic Fight patch on the Crab King");
            var idle = patch.getAnimator().getLivingAnimation(LivingMotions.IDLE, null);
            helper.assertTrue(idle == CrabKingAnimations.IDLE, "He should stand in his own idle, not " + idle);
            helper.assertTrue(king.getMaxHealth() == 260, "A boss of 260 health, not " + king.getMaxHealth());
            helper.assertTrue(king.getAttributeValue(Attributes.ARMOR) >= 14, "His shell should give 14 armour");
            helper.assertTrue(king.isPersistenceRequired() && !king.removeWhenFarAway(1.0E6), "The Crab King could despawn");
            helper.assertTrue(king.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER), "The Crab King should breathe under water");
            helper.assertTrue(king.showsBossBarTo(player), "The player near him does not see his boss bar");
            king.discard();
            cleanup(player);
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "crab_king_rage", timeoutTicks = 60)
    public static void underHalfHisHealthHeCallsTwoCrabsOnce(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 2));
        king.setNoAi(true);
        king.setHealth(king.getMaxHealth() * 0.4F);
        king.enrage(helper.getLevel());
        helper.runAfterDelay(2, () -> {
            var crabs = helper.getLevel().getEntitiesOfClass(ArmoredCrabEntity.class, king.getBoundingBox().inflate(6));
            helper.assertTrue(crabs.size() == CrabKingEntity.CRABS_CALLED, "He should have called two crabs, not " + crabs.size());
            helper.assertTrue(king.isEnraged(), "He did not keep his rage");
            var playing = EpicFightCapabilities.getEntityPatch(king, LivingEntityPatch.class).getAnimator().getPlayerFor(null).getRealAnimation();
            helper.assertTrue(playing == CrabKingAnimations.ROAR, "He did not roar: " + playing);
            crabs.forEach(c -> c.discard());
            king.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "crab_king_slam")
    public static void theSlamsShockwaveHurtsWhoIsNear(GameTestHelper helper) {
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        Zombie near = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        near.setNoAi(true);
        float health = near.getHealth();
        king.shockwave(helper.getLevel());
        helper.assertTrue(near.getHealth() < health, "The shockwave spared the zombie beside the landing point");
        near.discard();
        king.discard();
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "crab_king_attack", timeoutTicks = 300)
    public static void theCrabKingFightsWithHisPincers(GameTestHelper helper) {
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 1));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400);
        target.setHealth(400);
        boolean[] swung = {false};
        helper.onEachTick(() -> {
            king.setTarget(target);
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(king, LivingEntityPatch.class);
            var main = patch == null ? null : patch.getAnimator().getPlayerFor(null);
            if (main != null && main.getRealAnimation() != null && main.getRealAnimation().get() instanceof AttackAnimation) {
                var a = main.getRealAnimation();
                helper.assertTrue(a == CrabKingAnimations.SWEEP || a == CrabKingAnimations.SNAP || a == CrabKingAnimations.SLAM,
                        "The Crab King swung something else than his pincers: " + a);
                swung[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(swung[0], "The Crab King never attacked");
            helper.assertTrue(target.getHealth() < 400, "His pincers never struck");
            king.discard();
            target.discard();
        });
    }
}
