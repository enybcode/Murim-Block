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
 * The Crab King, the crab art's boss: a boss four blocks tall on the biped skeleton with his own idle, heavy walk and
 * pincer attacks, a boss bar, never despawning, breathing under water; under half his health he roars and calls two
 * armoured crabs, once; his slam's shockwave hurts whoever is near; his grab catches, holds and slams down.
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
            var walk = patch.getAnimator().getLivingAnimation(LivingMotions.WALK, null);
            helper.assertTrue(walk == CrabKingAnimations.WALK, "He should walk his own heavy walk, not " + walk);
            helper.assertTrue(Math.abs(king.getBbHeight() - 3.9F) < 0.01F, "He should be four blocks tall, not " + king.getBbHeight());
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

    /**
     * The grab played as in a fight: the pincer shuts on a zombie before him, holds it up off the ground, then slams it
     * down before him, hurt.
     */
    @GameTest(template = "foundation_arena", batch = "crab_king_grab", timeoutTicks = 140)
    public static void theGrabCatchesHoldsAndSlamsDown(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        // His right is -x when he faces +z: the pincer catches before him, a little to that side.
        Zombie caught = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        caught.setNoAi(true);
        caught.setBaby(false);
        float health = caught.getHealth();
        double ground = caught.getY();
        helper.runAfterDelay(2, () -> EpicFightCapabilities.getEntityPatch(king, LivingEntityPatch.class).playAnimationSynchronized(CrabKingAnimations.GRAB, 0));
        // The pincer shuts 0.55 s in (11 ticks), the hold lifts it in 0.35 s.
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(king.held() == caught, "He let go too soon");
            helper.assertTrue(caught.getY() > ground + 1.5, "The zombie is not lifted: " + (caught.getY() - ground));
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(king.held() == null, "He still holds the zombie after the slam");
            helper.assertTrue(caught.getHealth() < health, "The grab did not hurt the zombie");
            helper.assertTrue(Math.abs(caught.getY() - ground) < 0.6, "The zombie is not back on the ground: " + (caught.getY() - ground));
            caught.discard();
            king.discard();
            helper.succeed();
        });
    }

    /**
     * A player in his pincer can do nothing: hindered (no attack, item, technique or meditation), pinned in Epic Fight
     * (no move, turn, attack, skill, item, switch), an attack of theirs cancelled; free again once slammed down.
     */
    @GameTest(template = "foundation_arena", batch = "crab_king_held", timeoutTicks = 140)
    public static void aPlayerInHisPincerCanDoNothing(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        ServerPlayer player = player(helper);
        net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 2)));
        player.moveTo(at.x, at.y, at.z, 180, 0);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(king.catchBefore(helper.getLevel()) && king.held() == player, "The pincer did not catch the player");
            helper.assertTrue(CrabKingEntity.isHeld(player), "The player is not counted held");
            helper.assertTrue(com.murimblock.combat.knockdown.KnockdownService.isHindered(player), "A held player is not hindered");
        });
        helper.runAfterDelay(8, () -> {
            var state = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class).getEntityState();
            var pp = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);
            helper.assertTrue(state.inaction() && state.movementLocked() && state.turningLocked(), "A held player can still move: " + state
                    + ", playing " + pp.getAnimator().getPlayerFor(null).getRealAnimation() + ", armature biped " + (pp.getArmature() == yesman.epicfight.gameasset.Armatures.BIPED.get()) + " " + pp.getArmature());
            helper.assertTrue(!state.canBasicAttack() && !state.canUseSkill() && !state.canUseItem() && !state.canSwitchHoldingItem(),
                    "A held player can still attack, use a skill or an item: " + state);
            helper.assertTrue(com.murimblock.martial.MartialService.castSlot(player, 0) == com.murimblock.martial.MartialService.CastResult.HINDERED
                    || com.murimblock.martial.MartialService.castSlot(player, 0) == com.murimblock.martial.MartialService.CastResult.NO_TECHNIQUE,
                    "A held player could cast");
            var attack = new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(player, king);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(attack);
            helper.assertTrue(attack.isCanceled(), "A held player's attack on the king went through");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(king.held() == null && !CrabKingEntity.isHeld(player), "The player is still held after the slam");
            helper.assertTrue(!com.murimblock.combat.knockdown.KnockdownService.isHindered(player), "The player is still hindered after the slam");
            king.discard();
            cleanup(player);
            helper.succeed();
        });
    }

    /** Killed while he holds a player, he lets go: no one stays stuck. */
    @GameTest(template = "foundation_arena", batch = "crab_king_held_kill", timeoutTicks = 60)
    public static void killedHeLetsGo(GameTestHelper helper) {
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        ServerPlayer player = player(helper);
        net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 2)));
        player.moveTo(at.x, at.y, at.z, 180, 0);
        helper.runAfterDelay(2, () -> helper.assertTrue(king.catchBefore(helper.getLevel()), "The pincer did not catch the player"));
        helper.runAfterDelay(6, () -> king.kill());
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(!CrabKingEntity.isHeld(player) && !com.murimblock.combat.knockdown.KnockdownService.isHindered(player),
                    "The player stays held by a dead king");
            cleanup(player);
            helper.succeed();
        });
    }

    /** Nothing before him: the pincer shuts on air. */
    @GameTest(template = "foundation_arena", batch = "crab_king_grab_miss")
    public static void theGrabCatchesNothingOutOfReach(GameTestHelper helper) {
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        helper.assertTrue(!king.catchBefore(helper.getLevel()) && king.held() == null, "The pincer caught something with no one before him");
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
                helper.assertTrue(a == CrabKingAnimations.SWEEP || a == CrabKingAnimations.SNAP || a == CrabKingAnimations.SLAM || a == CrabKingAnimations.GRAB,
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
