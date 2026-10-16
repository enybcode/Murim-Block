package com.murimblock;

import com.murimblock.mob.GoldenDragonSageEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.world.structure.SanctuaryBuilder;
import com.murimblock.world.structure.SanctuaryPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Golden Dragon in human form (docs/mobs/golden-dragon.md): for now he waits seated in the Cloud Cliff Sanctuary,
 * harmed by nothing, moved by nothing, speaking when spoken to; placed on his seat once per world.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GoldenDragonSageGameTests {
    private GoldenDragonSageGameTests() { }

    @GameTest(template = "foundation_arena", batch = "golden_dragon_sage", timeoutTicks = 80)
    public static void theSageCannotBeHurtNorMoved(GameTestHelper helper) {
        GoldenDragonSageEntity sage = MurimEntities.GOLDEN_DRAGON_SAGE.get().spawn(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.COMMAND);
        ServerPlayer p = player(helper);
        helper.assertTrue(sage != null && sage.isPersistenceRequired() && !sage.removeWhenFarAway(1e6), "The sage could despawn");
        helper.assertTrue(EpicFightCapabilities.getEntityPatch(sage, LivingEntityPatch.class) != null, "No Epic Fight patch on the sage");
        float health = sage.getHealth();
        sage.hurt(p.damageSources().playerAttack(p), 500);
        helper.assertTrue(sage.getHealth() == health && sage.isAlive(), "The sage was hurt: " + sage.getHealth());
        BlockPos home = sage.blockPosition();
        sage.push(3, 0, 3);
        sage.interact(p, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(40, () -> {
            // Settling onto the floor is fine; drifting off his seat is not.
            helper.assertTrue(sage.blockPosition().getX() == home.getX() && sage.blockPosition().getZ() == home.getZ(), "The sage left his seat: " + sage.blockPosition() + " from " + home);
            sage.discard();
            cleanup(p);
            helper.succeed();
        });
    }

    @GameTest(template = "foundation_arena", batch = "golden_dragon_sage_seat")
    public static void theSageTakesHisSeatInTheSanctuaryOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        SanctuaryPlacement data = SanctuaryPlacement.get(level);
        BlockPos oldCentre = data.centre(), oldFoot = data.foot();
        int oldTop = data.top();
        try {
            // A sanctuary recorded so that the sage's seat falls on this test's floor.
            BlockPos seat = helper.absolutePos(new BlockPos(2, 1, 2));
            BlockPos centre = new BlockPos(seat.getX(), seat.getY() - 3 - SanctuaryBuilder.HEIGHT, seat.getZ() + 11);
            data.clear();
            data.record(centre, centre, seat.getY() - 3);
            helper.assertTrue(SanctuaryBuilder.sageSeat(centre, seat.getY() - 3).equals(seat), "The seat is not where the test put it");
            helper.assertTrue(SanctuaryPlacement.placeSage(level), "The sage was not placed");
            var placed = level.getEntitiesOfClass(GoldenDragonSageEntity.class, new AABB(seat).inflate(2));
            helper.assertTrue(placed.size() == 1 && placed.getFirst().blockPosition().equals(seat), "The sage is not on his seat: " + placed);
            helper.assertTrue(!SanctuaryPlacement.placeSage(level) && data.sagePlaced(), "The sage was placed twice");
            placed.forEach(GoldenDragonSageEntity::discard);
        } finally {
            data.clear();
            if (oldCentre != null) data.record(oldCentre, oldFoot, oldTop);
        }
        helper.succeed();
    }
}
