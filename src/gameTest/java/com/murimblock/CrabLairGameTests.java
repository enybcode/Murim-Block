package com.murimblock;

import com.murimblock.mob.ArmoredCrabEntity;
import com.murimblock.mob.CrabKingEntity;
import com.murimblock.world.structure.CrabLairBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Crab King's lair, built whole in solid rock far from the other tests: an open cavern with the headroom a giant
 * needs over the whole fighting floor, a shaft open to the sky with a pool deep enough to jump into, the broken junk,
 * the king and his crabs beside the pool, the floor lit bright (the fight easy to see, nothing else spawning).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrabLairGameTests {
    private CrabLairGameTests() { }

    private static final int X = 12000, Z = 0, SURFACE = 70;

    @GameTest(template = "foundation_arena", batch = "crab_lair", timeoutTicks = 1600)
    public static void theRootWellIsWholeAndHeld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int r = CrabLairBuilder.RADIUS + 6;
        for (int cx = (X - r) >> 4; cx <= (X + r) >> 4; cx++) for (int cz = (Z - r) >> 4; cz <= (Z + r) >> 4; cz++) level.setChunkForced(cx, cz, true);
        helper.runAfterDelay(20, () -> {
            int bottom = SURFACE - CrabLairBuilder.DEPTH - 8;
            // Old dwellers of an earlier run of this persistent world, then solid rock with the swamp's ground on top.
            AABB area = new AABB(X - r, bottom, Z - r, X + r, SURFACE + 4, Z + r);
            level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area).forEach(m -> m.discard());
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = X - r; x <= X + r; x++) for (int z = Z - r; z <= Z + r; z++) for (int y = bottom; y <= SURFACE + 4; y++)
                level.setBlock(p.set(x, y, z), y < SURFACE - 1 ? Blocks.STONE.defaultBlockState() : y == SURFACE - 1 ? Blocks.GRASS_BLOCK.defaultBlockState()
                        : Blocks.AIR.defaultBlockState(), 2 | 16);
            CrabLairBuilder lair = new CrabLairBuilder(level, X, SURFACE, Z, 42L);
            lair.buildAll();
            // The light spreads over a few ticks.
            helper.runAfterDelay(400, () -> check(helper, level, lair.floorY(), r, area));
        });
    }

    private static void check(GameTestHelper helper, ServerLevel level, int floor, int r, AABB area) {
            try {
                // The headroom: over the fighting floor (all but the junk's corner, west), open air at least 22 blocks up.
                int blocked = 0, checked = 0;
                for (int dx = -30; dx <= 30; dx += 3) for (int dz = -30; dz <= 30; dz += 3) {
                    if (Math.hypot(dx, dz) > 30 || dx < -6 && dz < 14) continue;
                    checked++;
                    for (int dy = 2; dy <= 22; dy++) if (level.getBlockState(new BlockPos(X + dx, floor + dy, Z + dz)).isSolid()) { blocked++; break; }
                }
                helper.assertTrue(checked > 60 && blocked * 10 < checked, "The fighting floor lacks headroom: " + blocked + " of " + checked + " columns blocked under 22 blocks");
                // The floor is mud underfoot.
                helper.assertTrue(level.getBlockState(new BlockPos(X + 20, floor, Z - 5)).isSolid(), "No floor under the arena");
                // The shaft: open from the swamp down into the cavern.
                int sz = Z + CrabLairBuilder.SHAFT_Z;
                for (int y = floor + 1; y < SURFACE + 2; y++)
                    helper.assertTrue(!level.getBlockState(new BlockPos(X, y, sz)).isSolid(), "The shaft is blocked at y " + y);
                // The pool under it, four deep.
                for (int dy = -CrabLairBuilder.POOL_DEPTH; dy <= 0; dy++)
                    helper.assertTrue(level.getBlockState(new BlockPos(X, floor + dy, sz)).is(Blocks.WATER), "No water at " + dy + " in the pool");
                // The junk: a good many of its planks.
                int planks = 0;
                for (int x = X - r; x <= X + 10; x++) for (int z = Z - r; z <= Z + 20; z++) for (int y = floor; y <= floor + 40; y++)
                    if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.DARK_OAK_PLANKS)) planks++;
                helper.assertTrue(planks > 300, "The junk has only " + planks + " hull planks");
                // The king and his crabs, on the floor, inside the lair.
                var king = level.getEntitiesOfClass(CrabKingEntity.class, area);
                helper.assertTrue(king.size() == 1, king.size() + " Crab Kings in the lair");
                helper.assertTrue(Math.abs(king.get(0).getY() - (floor + 1)) < 1.5 && king.get(0).isPersistenceRequired(), "The king is not on the floor, kept: " + king.get(0).position());
                helper.assertTrue(!king.get(0).isInWall(), "The king is in a block");
                helper.assertTrue(level.getEntitiesOfClass(ArmoredCrabEntity.class, area).size() == 4, "Not four crabs with him");
                // Lit bright enough to see the fight (block light 10 or more wherever one stands); no monster spawns there.
                int dark = 0, lowest = 15;
                for (int dx = -28; dx <= 28; dx += 4) for (int dz = -28; dz <= 28; dz += 4)
                    if (!level.getBlockState(new BlockPos(X + dx, floor + 1, Z + dz)).isSolid())
                    if (level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new BlockPos(X + dx, floor + 1, Z + dz)) < 10) { dark++; lowest = Math.min(lowest, level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new BlockPos(X + dx, floor + 1, Z + dz))); }
                helper.assertTrue(dark == 0, dark + " spots on the floor lit under 10 (the darkest " + lowest + ", light block at the centre: " + level.getBlockState(new BlockPos(X, floor + 2, Z)) + ")");
            } finally {
                level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area).forEach(m -> m.discard());
                for (int cx = (X - r) >> 4; cx <= (X + r) >> 4; cx++) for (int cz = (Z - r) >> 4; cz <= (Z + r) >> 4; cz++) level.setChunkForced(cx, cz, false);
            }
            helper.succeed();
    }
}
