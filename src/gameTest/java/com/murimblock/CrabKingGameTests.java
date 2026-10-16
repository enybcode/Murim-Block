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
 * armoured crabs, once; his slam's shockwave hurts whoever is near; his grab catches between the jaws, holds, squeezes and throws.
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

    /** He faces +z for the whole test: nothing turns him (a still mob turns its body after its head). */
    private static void pinFacing(GameTestHelper helper, CrabKingEntity king) {
        helper.onEachTick(() -> {
            king.setYRot(0);
            king.yRotO = 0;
            king.yBodyRot = 0;
            king.yBodyRotO = 0;
            king.setYHeadRot(0);
        });
    }

    /** A body put where his jaws shut, its middle on them. */
    private static void onJaws(CrabKingEntity king, net.minecraft.world.entity.LivingEntity e) {
        net.minecraft.world.phys.Vec3 at = king.catchPoint().subtract(0, e.getBbHeight() / 2, 0);
        e.moveTo(at.x, Math.max(at.y, king.getY()), at.z, 180, 0);
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
        pinFacing(helper, king);
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
     * The grab played as in a fight: the pincer shuts on a zombie before him, holds it up off the ground, squeezes it,
     * then throws it: it flies, lands, hurt.
     */
    @GameTest(template = "foundation_arena", batch = "crab_king_grab", timeoutTicks = 200)
    public static void theGrabCatchesHoldsAndSlamsDown(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        pinFacing(helper, king);
        // His right is -x when he faces +z: the pincer catches before him, a little to that side.
        Zombie caught = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        net.minecraft.world.phys.Vec3 jaws = king.catchPoint().subtract(0, caught.getBbHeight() / 2, 0);
        caught.moveTo(jaws.x, Math.max(jaws.y, king.getY()), jaws.z, 180, 0);
        // Left its AI: a mob without it does not move, even thrown.
        caught.setBaby(false);
        float health = caught.getHealth();
        double ground = caught.getY();
        helper.runAfterDelay(2, () -> EpicFightCapabilities.getEntityPatch(king, LivingEntityPatch.class).playAnimationSynchronized(CrabKingAnimations.GRAB, 0));
        // Kept on his jaws until they shut on it (its AI left on so it flies when thrown).
        helper.onEachTick(() -> { if (helper.getTick() <= 16 && king.held() == null) onJaws(king, caught); });
        // The pincer shuts 0.55 s in (11 ticks), the hold lifts it in 0.35 s.
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(king.held() == caught, "He let go too soon");
            helper.assertTrue(caught.getY() > ground + 1.5, "The zombie is not lifted: " + (caught.getY() - ground));
        });
        // Thrown 2.45 s into the hold (49 ticks after the catch).
        boolean[] flew = {false};
        helper.onEachTick(() -> flew[0] |= king.isFlying(caught));
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(king.held() == null, "He still holds the zombie after the throw");
            helper.assertTrue(flew[0], "The zombie was never thrown");
            helper.assertTrue(!king.isFlying(caught), "The zombie never landed");
            helper.assertTrue(caught.getHealth() < health, "The grab did not hurt the zombie");
            // Thrown against the arena's walls, it may land on their edge: on the ground, wherever it fell.
            helper.assertTrue(caught.onGround(), "The zombie is not back on the ground");
            caught.discard();
            king.discard();
            helper.succeed();
        });
    }

    /**
     * A player in his pincer can do nothing: hindered (no attack, item, technique or meditation), pinned in Epic Fight
     * (no move, turn, attack, skill, item, switch), an attack of theirs cancelled; still so while thrown; free once landed
     * (a mock player is not moved by the server: its flight ends on the five-second limit).
     */
    @GameTest(template = "foundation_arena", batch = "crab_king_held", timeoutTicks = 220)
    public static void aPlayerInHisPincerCanDoNothing(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        pinFacing(helper, king);
        ServerPlayer player = player(helper);
        // Where his jaws shut, whatever the arena did to him.
        net.minecraft.world.phys.Vec3 at = king.catchPoint().subtract(0, player.getBbHeight() / 2, 0);
        player.moveTo(at.x, Math.max(at.y, king.getY()), at.z, 180, 0);
        helper.runAfterDelay(2, () -> {
            onJaws(king, player);
            helper.assertTrue(king.catchBefore(helper.getLevel()) && king.held() == player, "The pincer did not catch the player: jaws at " + king.catchPoint() + ", player at " + player.position() + ", king at " + king.position() + " facing " + king.yBodyRot);
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
        helper.runAfterDelay(40, () -> helper.assertTrue(CrabKingEntity.isHeld(player), "He let the player go before the throw"));
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(king.held() == null && !CrabKingEntity.isHeld(player), "The player is still held after the throw");
            helper.assertTrue(!com.murimblock.combat.knockdown.KnockdownService.isHindered(player), "The player is still hindered after landing");
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
        pinFacing(helper, king);
        ServerPlayer player = player(helper);
        // Where his jaws shut, whatever the arena did to him.
        net.minecraft.world.phys.Vec3 at = king.catchPoint().subtract(0, player.getBbHeight() / 2, 0);
        player.moveTo(at.x, Math.max(at.y, king.getY()), at.z, 180, 0);
        helper.runAfterDelay(2, () -> onJaws(king, player));
        helper.runAfterDelay(3, () -> helper.assertTrue(king.catchBefore(helper.getLevel()), "The pincer did not catch the player: jaws at " + king.catchPoint() + ", player at " + player.position() + ", king at " + king.position() + " facing " + king.yBodyRot));
        helper.runAfterDelay(6, () -> king.kill());
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(!CrabKingEntity.isHeld(player) && !com.murimblock.combat.knockdown.KnockdownService.isHindered(player),
                    "The player stays held by a dead king");
            cleanup(player);
            helper.succeed();
        });
    }

    /** Thrown into a stone wall, a zombie knocks a crater into it; it is hurt and lands. */
    @GameTest(template = "foundation_arena", batch = "crab_king_crater", timeoutTicks = 200)
    public static void thrownIntoAWallItLeavesACrater(GameTestHelper helper) {
        helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        // A wall of stone across the far side, three high.
        for (int x = 0; x < 5; x++) for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        king.setYRot(0);
        king.yBodyRot = 0;
        pinFacing(helper, king);
        Zombie caught = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        net.minecraft.world.phys.Vec3 jaws = king.catchPoint().subtract(0, caught.getBbHeight() / 2, 0);
        caught.moveTo(jaws.x, Math.max(jaws.y, king.getY()), jaws.z, 180, 0);
        // Left its AI: a mob without it does not move, even thrown.
        caught.setBaby(false);
        float health = caught.getHealth();
        helper.runAfterDelay(2, () -> onJaws(king, caught));
        helper.runAfterDelay(3, () -> helper.assertTrue(king.catchBefore(helper.getLevel()), "The pincer did not catch the zombie: jaws at " + king.catchPoint() + ", zombie at " + caught.position() + ", king at " + king.position() + " facing " + king.yBodyRot));
        helper.runAfterDelay(150, () -> {
            int stone = 0;
            for (int x = 0; x < 5; x++) for (int y = 1; y <= 3; y++) if (helper.getBlockState(new BlockPos(x, y, 4)).is(Blocks.STONE)) stone++;
            helper.assertTrue(stone < 15, "The wall has no crater: all its 15 blocks stand");
            helper.assertTrue(!king.isFlying(caught) && caught.getHealth() < health, "The zombie did not land hurt");
            caught.discard();
            king.discard();
            helper.succeed();
        });
    }

    /**
     * The pincer from the deep, its three blows each on a still zombie: the sweep on one on the ground near its foot, the
     * strike on one on its side, the pinch on one where it shuts; it is gone at the end of its animation.
     */
    @GameTest(template = "foundation_arena", batch = "crab_king_pincer", timeoutTicks = 140)
    public static void theGiantPincerSweepsStrikesAndPinches(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        pinFacing(helper, king);
        net.minecraft.world.phys.Vec3 foot = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 2)));
        com.murimblock.mob.CrabPincerEntity pincer = com.murimblock.mob.CrabPincerEntity.call(helper.getLevel(), king, foot, foot.add(0, 0, 5));
        Zombie[] z = new Zombie[3];
        float[] health = new float[3];
        net.minecraft.world.phys.Vec3[] at = {foot.add(1.5, 0, 0), foot.add(pincer.strikeSide().scale(4)), pincer.pinchPoint()};
        for (int i = 0; i < 3; i++) {
            z[i] = EntityType.ZOMBIE.create(helper.getLevel());
            z[i].moveTo(at[i].x, foot.y, at[i].z, 0, 0);
            z[i].setNoAi(true);
            z[i].setNoGravity(true);
            helper.getLevel().addFreshEntity(z[i]);
            health[i] = z[i].getHealth();
        }
        // Each still zombie kept on its spot until its blow (the sweep throws them).
        helper.onEachTick(() -> {
            int tick = pincer.tickCount;
            for (int i = 0; i < 3; i++) {
                int blowAt = i == 0 ? com.murimblock.mob.CrabPincerEntity.SWEEP_FROM : i == 1 ? com.murimblock.mob.CrabPincerEntity.STRIKE : com.murimblock.mob.CrabPincerEntity.PINCH;
                if (tick < blowAt && i > 0) { z[i].teleportTo(at[i].x, foot.y, at[i].z); z[i].invulnerableTime = 0; }
                // Out of the sweep's reach until their own blow: lifted above it.
                if (i > 0 && tick >= com.murimblock.mob.CrabPincerEntity.SWEEP_FROM - 1 && tick <= com.murimblock.mob.CrabPincerEntity.SWEEP_TO + 1)
                    z[i].teleportTo(at[i].x, foot.y + 4, at[i].z);
            }
        });
        helper.runAfterDelay(100, () -> {
            String[] blows = {"sweep", "strike", "pinch"};
            for (int i = 0; i < 3; i++) helper.assertTrue(z[i].getHealth() < health[i], "The " + blows[i] + " did not land");
            helper.assertTrue(pincer.isRemoved(), "The pincer stayed after its animation");
            for (Zombie zz : z) zz.discard();
            king.discard();
            helper.succeed();
        });
    }

    /** Called on a target, two giant pincers come before it, one on either side, the second a little after, both pinching it. */
    @GameTest(template = "foundation_arena", batch = "crab_king_two_pincers", timeoutTicks = 60)
    public static void twoPincersComeBeforeTheTarget(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        CrabKingEntity king = king(helper, new BlockPos(2, 1, 0));
        king.setNoAi(true);
        pinFacing(helper, king);
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        target.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            king.callPincers(helper.getLevel(), target);
            helper.assertTrue(king.pincersToCome() == 2, "Not two pincers called: " + king.pincersToCome());
        });
        helper.runAfterDelay(4, () -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(com.murimblock.mob.CrabPincerEntity.class, king.getBoundingBox().inflate(16)).size() == 1,
                "The first pincer should be out, not the second yet"));
        helper.runAfterDelay(12, () -> {
            var pincers = helper.getLevel().getEntitiesOfClass(com.murimblock.mob.CrabPincerEntity.class, king.getBoundingBox().inflate(16));
            helper.assertTrue(pincers.size() == 2, "Not two pincers out: " + pincers.size());
            double side = 0;
            for (var p : pincers) {
                helper.assertTrue(p.pinchPoint().distanceTo(target.position().multiply(1, 0, 1).add(0, p.getY(), 0)) < 1.2,
                        "A pincer does not pinch its target: " + p.pinchPoint() + " for " + target.position());
                side += p.position().subtract(target.position()).cross(target.position().subtract(king.position())).y;
            }
            helper.assertTrue(Math.abs(side) < 1.0, "The two pincers are not either side of the target");
            pincers.forEach(net.minecraft.world.entity.Entity::discard);
            target.discard();
            king.discard();
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
        pinFacing(helper, king);
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
