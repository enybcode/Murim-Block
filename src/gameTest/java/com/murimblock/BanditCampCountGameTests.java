package com.murimblock;

import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.world.structure.site.BanditCampCount;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The count of a bandit camp's garrison (BanditCampCount), its thirty bandits and five Bandit Chiefs: each counted once
 * when he dies, the camp cleared when all thirty-five are dead.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BanditCampCountGameTests {
    /** A camp's centre nowhere near the others' (only its mark is used here). */
    private static final int CX = 9_104, CZ = 9_104;

    @GameTest(template = "foundation_arena", batch = "bandit_camp_count", timeoutTicks = 200)
    public static void aCampsGarrisonIsCountedAsItDiesUntilItIsCleared(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long camp = BanditCampCount.camp(CX, CZ);
        BanditCampCount.forgetForTests(level, camp);
        List<Mob> thirty = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            MountainBanditEntity bandit = helper.spawn(MurimEntities.MOUNTAIN_BANDIT.get(), new BlockPos(1 + i % 6, 2, 1 + i / 6));
            bandit.setNoAi(true);
            BanditCampCount.enlist(bandit, CX, CZ);
            thirty.add(bandit);
        }
        for (int i = 0; i < 5; i++) {
            Mob chief = helper.spawn(MurimEntities.BANDIT_CHIEF.get(), new BlockPos(1 + 2 * i, 2, 7));
            chief.setNoAi(true);
            BanditCampCount.enlist(chief, CX, CZ);
            thirty.add(chief);
        }
        // A bandit of no camp (a wandering one) does not count.
        MountainBanditEntity stray = helper.spawn(MurimEntities.MOUNTAIN_BANDIT.get(), new BlockPos(8, 2, 8));
        stray.kill();
        helper.assertTrue(BanditCampCount.campOf(thirty.get(0)).orElse(0) == camp, "a camp's bandit carries its mark");
        helper.assertTrue(BanditCampCount.killed(level, camp) == 0, "a stray bandit was counted: " + BanditCampCount.killed(level, camp));
        for (int i = 0; i < 12; i++) thirty.get(i).kill();
        thirty.get(0).kill(); // already dead: not again
        helper.assertTrue(BanditCampCount.killed(level, camp) == 12, "12 dead, counted " + BanditCampCount.killed(level, camp));
        helper.assertFalse(BanditCampCount.cleared(level, camp), "cleared at 12");
        for (int i = 12; i < 30; i++) thirty.get(i).kill();
        helper.assertFalse(BanditCampCount.cleared(level, camp), "cleared with the five chiefs standing");
        for (int i = 30; i < 35; i++) thirty.get(i).kill();
        helper.assertTrue(BanditCampCount.killed(level, camp) == 35 && BanditCampCount.cleared(level, camp),
                "all thirty-five dead, counted " + BanditCampCount.killed(level, camp));
        BanditCampCount.forgetForTests(level, camp);
        helper.succeed();
    }
}
