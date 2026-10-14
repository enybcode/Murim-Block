package com.murimblock;

import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.site.Canvas;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Verifies a built Murim site against the checks it recorded (docs/structures): every room roofed over every column
 * (nothing open to the sky), nothing natural grown into a room (leaves, bamboo, rock), and every floor, path and
 * footing solid with something solid under it (no void beneath). Shared by the GameTests and the client tour.
 */
public final class SiteChecks {
    private SiteChecks() { }

    public static List<String> verify(LevelReader level, List<Canvas.Check> checks) {
        List<String> problems = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Canvas.Check check : checks) {
            BoundingBox b = check.box();
            for (int x = b.minX(); x <= b.maxX(); x++) for (int z = b.minZ(); z <= b.maxZ(); z++) {
                if (check.room()) {
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                    if (top <= b.maxY()) problems.add("room open to the sky at " + x + " " + (b.maxY() + 1) + " " + z);
                    // The ceiling sits right on the room: no gap between the walls and the roof.
                    if (level.getBlockState(p.set(x, b.maxY() + 1, z)).isAir()) problems.add("no ceiling over a room at " + x + " " + (b.maxY() + 1) + " " + z);
                    for (int y = b.minY(); y <= b.maxY(); y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (intruder(s)) problems.add("natural block " + s.getBlock().getDescriptionId() + " inside a room at " + x + " " + y + " " + z);
                    }
                } else {
                    int y = b.minY();
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (!s.getFluidState().isEmpty()) continue; // a well in a square
                    if (s.isAir()) problems.add("hole in a floor at " + x + " " + y + " " + z);
                    else if (!level.getBlockState(p.set(x, y - 1, z)).blocksMotion()) problems.add("nothing under the floor at " + x + " " + y + " " + z);
                }
            }
        }
        return problems;
    }

    /** Things of the wild that must never be found inside a room. */
    static boolean intruder(BlockState s) {
        return s.is(BlockTags.LEAVES) || s.is(Blocks.BAMBOO) || s.is(MurimBlocks.MOSO_BAMBOO.get()) || s.is(MurimBlocks.KARST_LIMESTONE.get())
                || s.is(MurimBlocks.MOSSY_KARST_LIMESTONE.get()) || s.is(MurimBlocks.PALE_GRANITE.get()) || s.is(MurimBlocks.PILLAR_SANDSTONE.get())
                || (s.is(BlockTags.LOGS) && !s.getBlock().getDescriptionId().contains("stripped"));
    }

    /** A short summary for logs and reports. */
    public static String summary(List<String> problems) {
        if (problems.isEmpty()) return "no problem";
        return problems.size() + " problem(s), first: " + String.join("; ", problems.subList(0, Math.min(5, problems.size())));
    }
}
