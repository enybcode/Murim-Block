package com.murimblock;

import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.ScarletBloom;
import com.murimblock.martial.ScarletPieceEntity;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Scarlet Bloom, the Sanguine Art's finish: only for a master of the art (every technique unlocked, the art their main
 * one); the downed body is thrown into the sky among its crystals, then lands executed, the kill the executor's; every
 * piece of the show is gone at its end; broken off, the body is put back on the ground, alive, and the show is gone.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScarletBloomGameTests {
    private ScarletBloomGameTests() { }

    /** An executor and a downed victim two blocks apart; the executor a master of the Sanguine Art if asked. */
    private static ServerPlayer[] pair(GameTestHelper helper, boolean master) {
        helper.getLevel().getServer().setPvpAllowed(true);
        ServerPlayer executor = player(helper), victim = player(helper);
        EpicFightCombatDefaults.enforce(executor);
        BlockPos p = helper.absolutePos(new BlockPos(2, 1, 1));
        executor.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        victim.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 2.5, 180, 0);
        com.murimblock.cultivation.CultivationService.setCultivation(executor, MartialArts.SANGUINE_REALM, com.murimblock.cultivation.CultivationStage.EARLY);
        new ItemStack(MurimItems.SANGUINE_ART_SCROLL.get()).use(executor.level(), executor, InteractionHand.MAIN_HAND);
        MartialService.setMainArt(executor, MartialArts.SANGUINE_ART);
        if (master) for (MartialArts.Technique t : MartialArts.art(MartialArts.SANGUINE_ART).techniques()) MartialService.unlock(executor, t);
        executor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        executor.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        KnockdownService.knockDownFor(victim, 400);
        return new ServerPlayer[] { executor, victim };
    }

    private static int pieces(GameTestHelper helper, Vec3 around) {
        return helper.getLevel().getEntitiesOfClass(ScarletPieceEntity.class, new AABB(around, around).inflate(24)).size();
    }

    private static void end(GameTestHelper helper, ServerPlayer... players) {
        KnockdownService.clearForTests();
        helper.getLevel().getServer().setPvpAllowed(false);
        for (ServerPlayer p : players) {
            com.murimblock.cultivation.CultivationService.resetCultivation(p);
            cleanup(p);
        }
    }

    @GameTest(template = "foundation_arena", batch = "scarlet_bloom_master", timeoutTicks = 20)
    public static void onlyAMasterOfTheArtPlaysScarletBloom(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper, false);
        ServerPlayer executor = p[0];
        helper.assertTrue(MartialService.data(executor).knowsArt(MartialArts.SANGUINE_ART), "The executor did not learn the art");
        helper.assertFalse(ScarletBloom.finishes(executor), "Scarlet Bloom without every technique of the art");
        for (MartialArts.Technique t : MartialArts.art(MartialArts.SANGUINE_ART).techniques()) MartialService.unlock(executor, t);
        helper.assertTrue(ScarletBloom.finishes(executor), "No Scarlet Bloom for a master of the art");
        end(helper, p);
        helper.succeed();
    }

    @GameTest(template = "foundation_arena", batch = "scarlet_bloom_show", timeoutTicks = ScarletBloom.CLEAR_TICK + 40)
    public static void theBodyFliesThenLandsExecutedAndTheShowIsGone(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper, true);
        ServerPlayer executor = p[0], victim = p[1];
        Vec3 origin = victim.position();
        helper.assertTrue(KnockdownService.execute(executor, victim), "Could not start the finish");
        helper.assertTrue(ScarletBloom.showing(executor), "Not Scarlet Bloom");
        helper.runAfterDelay(ScarletBloom.PEAK_TICK + 4, () -> {
            helper.assertTrue(victim.isAlive() && victim.getY() > origin.y + ScarletBloom.RISE - 1.5,
                    "The body is not in the sky: " + (victim.getY() - origin.y) + " blocks up");
            helper.assertTrue(pieces(helper, origin) > 5, "No crystals around the body: " + pieces(helper, origin));
            helper.assertTrue(KnockdownService.isExecuting(executor), "The finish was abandoned with the body in the sky");
        });
        helper.runAfterDelay(ScarletBloom.BURST_TICK + 4, () -> {
            long limbs = helper.getLevel().getEntitiesOfClass(ScarletPieceEntity.class, new AABB(origin, origin).inflate(24),
                    piece -> piece.kind() == ScarletPieceEntity.LIMB).size();
            helper.assertTrue(limbs == 6, "The body did not burst in six pieces: " + limbs);
            helper.assertTrue(victim.isInvisible(), "The whole body is still seen beside its pieces");
        });
        helper.runAfterDelay(ScarletBloom.HIT_TICK + 4, () -> {
            helper.assertTrue(victim.isDeadOrDying(), "Scarlet Bloom did not kill");
            helper.assertTrue(victim.getKillCredit() == executor, "The kill is not the executor's: " + victim.getKillCredit());
        });
        helper.runAfterDelay(ScarletBloom.CLEAR_TICK + 6, () -> {
            helper.assertTrue(!ScarletBloom.showing(executor), "The show is still running");
            helper.assertTrue(pieces(helper, origin) == 0, "Pieces of the show left behind: " + pieces(helper, origin));
            end(helper, executor, victim);
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "scarlet_bloom_interrupt", timeoutTicks = 80)
    public static void brokenOffTheBodyIsPutBackAndTheShowIsGone(GameTestHelper helper) {
        ServerPlayer[] p = pair(helper, true);
        ServerPlayer executor = p[0], victim = p[1];
        ServerPlayer rescuer = player(helper);
        Vec3 origin = victim.position();
        KnockdownService.execute(executor, victim);
        helper.runAfterDelay(30, () -> executor.hurt(executor.damageSources().playerAttack(rescuer), 2));
        helper.runAfterDelay(36, () -> {
            helper.assertTrue(victim.isAlive() && KnockdownService.isDowned(victim), "The broken-off finish still killed");
            helper.assertTrue(victim.position().distanceTo(origin) < 0.5, "The body was not put back: " + victim.position().distanceTo(origin));
            helper.assertTrue(!ScarletBloom.showing(executor) && pieces(helper, origin) == 0, "The show was not taken away");
            end(helper, executor, victim, rescuer);
            helper.succeed();
        });
    }
}
