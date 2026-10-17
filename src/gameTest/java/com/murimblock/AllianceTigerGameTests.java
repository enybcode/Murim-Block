package com.murimblock;

import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The camp's defenders must acquire and hurt a tiger without being attacked first. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AllianceTigerGameTests {
    private AllianceTigerGameTests() { }

    @GameTest(template = "spear_arena", batch = "alliance_tiger_spear", timeoutTicks = 200)
    public static void spearGuardDefendsAgainstTiger(GameTestHelper h) { defends(h, MurimEntities.ALLIANCE_SPEAR_GUARD.get()); }

    @GameTest(template = "spear_arena", batch = "alliance_tiger_sword", timeoutTicks = 200)
    public static void swordDiscipleDefendsAgainstTiger(GameTestHelper h) { defends(h, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get()); }

    @GameTest(template = "spear_arena", batch = "alliance_tiger_archer", timeoutTicks = 200)
    public static void archerDefendsAgainstTiger(GameTestHelper h) { defends(h, MurimEntities.ALLIANCE_ARCHER.get()); }

    @GameTest(template = "spear_arena", batch = "alliance_tiger_monk", timeoutTicks = 200)
    public static void warriorMonkDefendsAgainstTiger(GameTestHelper h) { defends(h, MurimEntities.ALLIANCE_WARRIOR_MONK.get()); }

    @GameTest(template = "spear_arena", batch = "alliance_tiger_sergeant", timeoutTicks = 200)
    public static void sergeantDefendsAgainstTiger(GameTestHelper h) { defends(h, MurimEntities.ALLIANCE_SERGEANT.get()); }

    @GameTest(template = "spear_arena", batch = "alliance_tiger_captain", timeoutTicks = 200)
    public static void peakCaptainDefendsAgainstTiger(GameTestHelper h) { defends(h, MurimEntities.PEAK_CAPTAIN.get()); }

    private static void defends(GameTestHelper h, EntityType<? extends Mob> type) {
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        Mob defender = type.spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 4)), MobSpawnType.STRUCTURE);
        var tiger = MurimEntities.TIGER.get().spawn(h.getLevel(), h.absolutePos(new BlockPos(6, 1, 4)), MobSpawnType.COMMAND);
        h.assertTrue(defender != null && tiger != null, "Could not spawn defender or tiger");
        defender.setPersistenceRequired();
        defender.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(8);
        tiger.setPersistenceRequired();
        // A living, damageable threat that cannot provoke retaliation or wander into another test.
        tiger.setNoAi(true);
        tiger.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        tiger.setHealth(200);
        boolean[] acquired = {false}, hurtByDefender = {false};
        h.onEachTick(() -> {
            acquired[0] |= defender.getTarget() == tiger;
            hurtByDefender[0] |= tiger.getHealth() < 200 && tiger.getLastHurtByMob() == defender;
        });
        h.runAfterDelay(160, () -> {
            String evidence = " target=" + defender.getTarget() + ", tigerHealth=" + tiger.getHealth()
                    + ", sees=" + defender.hasLineOfSight(tiger) + ", distance=" + defender.distanceTo(tiger);
            try {
                h.assertTrue(defender.getLastHurtByMob() == null, "Defender was provoked; this must test proactive defence");
                h.assertTrue(acquired[0], type + " ignored the tiger" + evidence);
                h.assertTrue(hurtByDefender[0], type + " targeted the tiger but never hurt it" + evidence);
            } finally {
                defender.discard();
                tiger.discard();
            }
            h.succeed();
        });
    }
}
