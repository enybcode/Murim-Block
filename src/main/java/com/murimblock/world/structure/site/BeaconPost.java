package com.murimblock.world.structure.site;

import com.murimblock.mob.MurimEntities;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * Jade Gate Beacon Post (Hexi frontier desert): a walled relay station of the Silk Road (after Xuanquanzhi): rammed
 * earth walls with a walkway and crenels, corner turrets, a gatehouse with a red pavilion over the gate, the
 * official's hall, a dormitory and stables under lean-to roofs, a well and stacked cargo; and in the north-east
 * corner the beacon tower, banded with reeds, its signal fire smoking on top.
 * <p>Taken by raiders (Enzo's request, 14/10): four Mountain Bandits in the Dune Raider outfit hold the post, one in
 * the official's hall, one in the yard, one by the caravans' cargo, one in the dormitory.
 */
final class BeaconPost extends Site {
    static final int WALL = 7;
    private static final BlockState EARTH = s(Blocks.SANDSTONE);
    private static final BlockState BASE = s(Blocks.SMOOTH_SANDSTONE);
    private static final BlockState BAND = s(Blocks.CUT_SANDSTONE);
    private static final BlockState RED = log(Blocks.STRIPPED_MANGROVE_LOG, Direction.Axis.Y);

    @Override public int reach() { return 21; }

    @Override public List<Part> parts() { return List.of(new Part(-17, -17, 17, 21, 0, 22)); }

    @Override public int probe() { return 15; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 8, false); }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        prepare(c, -17, -17, 17, 17, g, g + 24, s(Blocks.SAND), s(Blocks.SANDSTONE));
        // The yard: sand, a flagged way from the gate to the hall.
        pave(c, -1, -6, 1, 15, g, BASE, BASE, s(Blocks.CUT_SANDSTONE));
        c.carried(-1, -6, 1, 15, g);
        walls(c, g);
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) turret(c, sx, sz, g);
        beacon(c, g);
        gatehouse(c, g);
        hall(c, g);
        dormitory(c.frame(-10, 2, Rotation.COUNTERCLOCKWISE_90), g);
        stables(c.frame(10, 2, Rotation.CLOCKWISE_90), g);
        yard(c, g);
        // The flagged way runs on out of the gate, on solid ground.
        prepare(c, -2, 18, 2, 21, g, g + 8, s(Blocks.SAND), EARTH);
        pave(c, -1, 16, 1, 21, g, BASE, BASE, s(Blocks.CUT_SANDSTONE));
        c.carried(-1, 16, 1, 21, g);
        // The raiders who took it, in the outfit of the desert.
        raider(c, 0, g + 1, 1, Direction.SOUTH);
        raider(c, 1, g + 1, 5, Direction.WEST);
    }

    /** A Mountain Bandit in the Dune Raider outfit, placed with the post. */
    private static void raider(Canvas c, int x, int y, int z, Direction facing) {
        c.spawn(x, y, z, facing, MurimEntities.MOUNTAIN_BANDIT.get(), bandit -> bandit.setVariant(DUNE_RAIDER));
    }

    /** The Dune Raider's outfit (MountainBanditEntity.OUTFITS). */
    private static final int DUNE_RAIDER = 3;

    /** Rammed earth wall, two thick and seven high, a walkway on top and crenels on the outer edge. */
    private static void walls(Canvas c, int g) {
        for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) {
            int ring = Math.max(Math.abs(x), Math.abs(z));
            if (ring < 13) continue;
            c.footing(x, g, z, EARTH);
            for (int y = g + 1; y <= g + WALL; y++) c.set(x, y, z, y == g + 1 ? BASE : y == g + WALL ? BAND : EARTH);
            if (ring == 14) c.set(x, g + WALL + 1, z, (x + z) % 2 == 0 ? EARTH : AIR);
            c.carried(x, z, x, z, g);
        }
        // Stairs up to the walkway along the south wall, east of the gate, on a solid ramp.
        for (int i = 0; i < WALL; i++) {
            int x = 5 + i, y = g + 1 + i;
            c.set(x, y, 12, stair(Blocks.SANDSTONE_STAIRS, Direction.EAST, Half.BOTTOM));
            for (int yy = g + 1; yy < y; yy++) c.set(x, yy, 12, EARTH);
        }
    }

    /** A corner turret: 5 x 5, standing out of the corner, crenellated. */
    private static void turret(Canvas c, int sx, int sz, int g) {
        for (int a = 12; a <= 16; a++) for (int b = 12; b <= 16; b++) {
            int x = sx * a, z = sz * b;
            c.footing(x, g, z, EARTH);
            c.carried(x, z, x, z, g);
            for (int y = g + 1; y <= g + WALL; y++) c.set(x, y, z, y == g + 1 ? BASE : y == g + WALL ? BAND : EARTH);
            c.set(x, g + WALL + 1, z, (a == 16 || b == 16) && (a + b) % 2 == 0 ? EARTH : AIR);
        }
    }

    /**
     * The beacon (fengsui) on the north-east turret: a 3 x 3 shaft banded with reed bundles, a ladder up its west face,
     * and the signal fire on a bale at the top.
     */
    private static void beacon(Canvas c, int g) {
        int x0 = 13, z0 = -15, base = g + WALL + 1, top = g + WALL + 7;
        for (int y = base; y <= top; y++) for (int x = x0; x <= x0 + 2; x++) for (int z = z0; z <= z0 + 2; z++) {
            boolean band = (y - base) % 3 == 2;
            c.set(x, y, z, band || (y == top && x == x0 + 1 && z == z0 + 1) ? s(Blocks.HAY_BLOCK) : EARTH);
        }
        for (int x = x0; x <= x0 + 2; x++) for (int z = z0; z <= z0 + 2; z++) {
            if (x == x0 + 1 && z == z0 + 1) c.set(x, top + 1, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
            else if (!(x == x0 && z == z0 + 1)) c.set(x, top + 1, z, s(Blocks.SANDSTONE_WALL));
        }
        ladder(c, x0 - 1, base, top, z0 + 1, Direction.WEST);
    }

    /** The gatehouse: a thicker block of wall round the passage, and the red gate pavilion on top. */
    private static void gatehouse(Canvas c, int g) {
        for (int x = -4; x <= 4; x++) for (int z = 12; z <= 15; z++) {
            c.footing(x, g, z, EARTH);
            boolean passage = Math.abs(x) <= 1;
            for (int y = g + 1; y <= g + WALL; y++) {
                if (passage && y <= g + 4) { c.set(x, y, z, AIR); continue; }
                c.set(x, y, z, y == g + 1 ? BASE : y == g + WALL || y == g + 5 ? BAND : EARTH);
            }
            if (passage) c.set(x, g, z, BASE);
            if (z == 15 || z == 12) c.set(x, g + WALL + 1, z, AIR);
        }
        c.carried(-4, 12, 4, 15, g);
        c.room(-1, g + 1, 12, 1, g + 4, 15);
        int f = g + WALL;
        // The pavilion: vermilion columns, lattice between them, a door to the walkway at each end.
        for (int x = -4; x <= 4; x++) for (int z = 12; z <= 15; z++) {
            boolean edge = Math.abs(x) == 4 || z == 12 || z == 15;
            for (int y = f + 1; y <= f + 4; y++) {
                if (!edge) { c.set(x, y, z, AIR); continue; }
                boolean column = (Math.abs(x) == 4 || Math.abs(x) == 1) && (z == 12 || z == 15);
                if (column) c.set(x, y, z, RED);
                else if (Math.abs(x) == 4) c.set(x, y, z, y == f + 1 ? s(Blocks.RED_TERRACOTTA) : lattice(Blocks.DARK_OAK_TRAPDOOR, x < 0 ? Direction.WEST : Direction.EAST));
                else c.set(x, y, z, y == f + 1 ? s(Blocks.RED_TERRACOTTA) : lattice(Blocks.DARK_OAK_TRAPDOOR, z == 12 ? Direction.NORTH : Direction.SOUTH));
            }
        }
        door(c, -4, f + 1, 13, Blocks.DARK_OAK_DOOR, Direction.EAST);
        door(c, 4, f + 1, 13, Blocks.DARK_OAK_DOOR, Direction.WEST);
        c.set(0, f + 1, 14, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR).setValue(BellBlock.FACING, Direction.EAST));
        c.set(-2, f + 1, 14, s(Blocks.NOTE_BLOCK));
        c.stocked(2, f + 1, 14, s(Blocks.BARREL), SiteLoot.CACHE);
        c.set(0, f + 4, 13, hangingLantern());
        c.kit().roof(-4, 12, 4, 15, f + 5, RoofTiles.GREY, false);
        c.room(-3, f + 1, 13, 3, f + 4, 14);
        wallBanner(c, -2, g + 4, 16, Blocks.RED_WALL_BANNER, Direction.SOUTH);
        wallBanner(c, 2, g + 4, 16, Blocks.RED_WALL_BANNER, Direction.SOUTH);
    }

    /** The official's hall at the back of the yard: plastered walls, red columns, lattice front, tiled roof. */
    private static void hall(Canvas c, int g) {
        int x0 = -6, x1 = 6, z0 = -11, z1 = -7, f = g + 1;
        c.fill(x0 - 1, g, z0 - 1, x1 + 1, f, z1 + 1, BASE);
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) c.footing(x, g - 1, z, EARTH);
        c.carried(x0 - 1, z0 - 1, x1 + 1, z1 + 1, f);
        for (int x = -1; x <= 1; x++) c.set(x, g + 1, z1 + 2, stair(Blocks.SMOOTH_SANDSTONE_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int y = f + 1; y <= f + 4; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { c.set(x, y, z, AIR); continue; }
            boolean column = (x == x0 || x == x1 || x % 3 == 0) && (z == z0 || z == z1) || (x == x0 || x == x1) && z == (z0 + z1) / 2;
            if (column) c.set(x, y, z, RED);
            else if (z == z1 && y >= f + 2 && y <= f + 3) c.set(x, y, z, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
            else c.set(x, y, z, BASE);
        }
        door(c, 0, f + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        c.kit().gableRoof(x0, z0, x1, z1, f + 5, RoofTiles.GREY, s(Blocks.DARK_OAK_PLANKS), BASE);
        c.room(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 4, z1 - 1);
        int y = f + 1;
        // The post official's office: a desk with the route ledger, maps, shelves, the imperial banners.
        c.set(0, y, z0 + 1, s(Blocks.LECTERN));
        c.set(-1, y, z0 + 1, s(Blocks.CARTOGRAPHY_TABLE));
        c.set(1, y, z0 + 1, s(Blocks.CARTOGRAPHY_TABLE));
        for (int x = x0 + 1; x <= x1 - 1; x++) if (Math.abs(x) >= 3) { c.set(x, y, z0 + 1, s(Blocks.BOOKSHELF)); c.set(x, y + 1, z0 + 1, s(Blocks.BOOKSHELF)); }
        wallBanner(c, -2, y + 2, z0 + 1, Blocks.YELLOW_WALL_BANNER, Direction.SOUTH);
        wallBanner(c, 2, y + 2, z0 + 1, Blocks.YELLOW_WALL_BANNER, Direction.SOUTH);
        c.set(x0 + 1, y, z1 - 1, s(Blocks.BARREL));
        c.set(x1 - 1, y, z1 - 1, s(Blocks.BARREL));
        c.set(x0 + 1, y, z1 - 2, s(Blocks.POTTED_DEAD_BUSH));
        for (int x : new int[]{-3, 3}) c.set(x, f + 4, -9, hangingLantern());
        // Their leader, over the official's ledger.
        raider(c, 2, y, -9, Direction.SOUTH);
    }

    /** Dormitory for travellers and couriers (frame: front +z to the yard), its lean-to roof against the wall. */
    private static void dormitory(Canvas h, int g) {
        int x0 = -7, x1 = 7, z0 = -2, z1 = 1, f = g + 1;
        shedHouse(h, g, x0, z0, x1, z1);
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            if (x == 0) continue;
            boolean bedHere = Math.abs(x) % 2 == 1;
            if (bedHere) bed(h, x, f, z0 + 2, Direction.NORTH, Blocks.BROWN_BED);
            else h.set(x, f, z0 + 1, s(Blocks.BARREL));
        }
        door(h, 0, f, z1, Blocks.SPRUCE_DOOR, Direction.NORTH);
        for (int x : new int[]{-4, 4}) h.set(x, f + 2, 0, hangingLantern());
        h.set(x1 - 1, f, z1 - 1, s(Blocks.CRAFTING_TABLE));
        h.room(x0 + 1, f, z0 + 1, x1 - 1, f + 2, z1 - 1);
        raider(h, 0, f, -1, Direction.SOUTH);
    }

    /** Stables (frame: front +z to the yard): open on the yard between posts, stalls of fences, hay and troughs. */
    private static void stables(Canvas h, int g) {
        int x0 = -7, x1 = 7, z0 = -2, z1 = 1, f = g + 1;
        shedHouse(h, g, x0, z0, x1, z1);
        for (int x = x0 + 1; x <= x1 - 1; x++) for (int y = f; y <= f + 2; y++) h.set(x, y, z1, x % 3 == 0 ? log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y) : AIR);
        for (int x = x0; x <= x1; x++) h.set(x, g, z1 + 1, s(Blocks.COARSE_DIRT));
        for (int x : new int[]{-3, 3}) for (int z = z0 + 1; z <= z1 - 1; z++) h.set(x, f, z, s(Blocks.SPRUCE_FENCE));
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            if (Math.abs(x) == 3) continue;
            h.set(x, f, z0 + 1, (x + 7) % 3 == 0 ? s(Blocks.WATER_CAULDRON) : s(Blocks.HAY_BLOCK));
            if (h.chance(x, f, z0 + 2, 0.5)) h.set(x, f, z0 + 2, s(Blocks.HAY_BLOCK));
        }
        for (int x : new int[]{-5, 0, 5}) h.set(x, f + 2, 0, hangingLantern());
        h.room(x0 + 1, f, z0 + 1, x1 - 1, f + 2, z1 - 1);
    }

    /** A low building with a lean-to roof (frame: front +z), back against the station wall. */
    private static void shedHouse(Canvas h, int g, int x0, int z0, int x1, int z1) {
        int f = g + 1;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            h.set(x, g, z, BASE);
            h.footing(x, g - 1, z, EARTH);
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            for (int y = f; y <= f + 2; y++) {
                if (!edge) { h.set(x, y, z, AIR); continue; }
                boolean post = (x == x0 || x == x1) && (z == z0 || z == z1);
                h.set(x, y, z, post ? log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y) : BASE);
            }
        }
        h.carried(x0, z0, x1, z1, g);
        for (int x = x0 + 2; x <= x1 - 2; x += 4) if (x != 0) h.set(x, f + 1, z1, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
        h.kit().shedRoof(x0, z0, x1, z1, f + 3, RoofTiles.GREY, s(Blocks.SPRUCE_PLANKS), BASE);
    }

    /** The yard: well, cargo of the caravans, hitching posts, lanterns along the way. */
    private static void yard(Canvas c, int g) {
        int wx = -4, wz = 3;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            boolean centre = dx == 0 && dz == 0;
            for (int y = g - 4; y <= g; y++) c.set(wx + dx, y, wz + dz, centre && y > g - 4 ? s(Blocks.WATER) : BASE);
            if (!centre) c.set(wx + dx, g + 1, wz + dz, s(Blocks.SANDSTONE_WALL));
        }
        for (int dx : new int[]{-1, 1}) c.set(wx + dx, g + 2, wz, s(Blocks.SPRUCE_FENCE));
        for (int dx = -1; dx <= 1; dx++) c.set(wx + dx, g + 3, wz, log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.X));
        c.set(wx, g + 2, wz, s(Blocks.CHAIN));
        // Caravan cargo.
        int[][] cargo = {{3, 4}, {4, 4}, {5, 4}, {3, 5}, {4, 5}, {4, 6}};
        for (int[] p : cargo) {
            c.set(p[0], g + 1, p[1], c.chance(p[0], g, p[1], 0.5) ? s(Blocks.BARREL) : s(Blocks.HAY_BLOCK));
            if (c.chance(p[0], g + 1, p[1], 0.4)) c.set(p[0], g + 2, p[1], s(Blocks.BARREL));
        }
        // Hitching rail in front of the stables, lanterns along the way.
        for (int z = -2; z <= 2; z++) c.set(6, g + 1, z, s(Blocks.SPRUCE_FENCE));
        for (int z : new int[]{-4, 3, 10}) {
            lanternPost(c, -2, g + 1, z, 2, s(Blocks.SANDSTONE_WALL));
            lanternPost(c, 2, g + 1, z, 2, s(Blocks.SANDSTONE_WALL));
        }
    }
}
