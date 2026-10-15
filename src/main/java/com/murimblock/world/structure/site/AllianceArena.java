package com.murimblock.world.structure.site;

import com.murimblock.mob.MurimEntities;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.map.MurimMap;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.slab;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * The arena of the Orthodox Alliance (docs/structures/alliance-arena.md, from the mock-up scripts/concepts/arena.js):
 * a great walled court (73 x 73) round a leitai (27 x 27), the raised fighting platform of the Murim where challenges are
 * settled; whoever falls off loses. The captain waits on it, and calls up his units on the four gold summoning seals of
 * the leitai and out of the two troop halls. Covered stands to the east and west, the judges' pavilion to the north (the
 * captain's seat, the Alliance's banners, a chest and the cache), the gate and a paifang to the south; braziers, drums,
 * weapon racks and banner poles round the court. Its chests hold the arena's loot ({@link SiteLoot#ARENA}).
 * <p>Uncommon (about as often as a pillager outpost) and only where the Alliance holds the land: the centre of the Murim
 * map and the frontiers between its regions. The captain appears nowhere else.
 */
final class AllianceArena extends Site {
    /** The court's walls (outer faces), local. */
    static final int W = 36;
    /** Half the leitai's side, and the height of its top above the court. */
    static final int L = 13, TOP = 3;
    /** The summoning seals on the leitai, at (+-SEAL, +-SEAL). */
    static final int SEAL = 8;
    /** The stands: front and back x, the first gallery x, half their length. */
    static final int SF = L + 8, SB = W - 3, GAL = SF + 4, SZ = 18;

    private static final BlockState BASE = s(Blocks.STONE_BRICKS);
    private static final BlockState WHITE = s(Blocks.CALCITE);
    private static final BlockState RED = log(Blocks.STRIPPED_MANGROVE_LOG, Direction.Axis.Y);
    private static final BlockState SKIRT = s(Blocks.MANGROVE_PLANKS);
    private static final BlockState BEAM = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X);
    private static final BlockState DARK = s(Blocks.DARK_OAK_PLANKS);
    private static final BlockState GOLD = s(Blocks.HONEYCOMB_BLOCK);

    private static RoofTiles azure() {
        return new RoofTiles(MurimBlocks.AZURE_ROOF_TILE_STAIRS.get(), MurimBlocks.AZURE_ROOF_TILE_SLAB.get(), MurimBlocks.AZURE_ROOF_TILES.get());
    }

    @Override public int reach() { return 60; }

    @Override public List<Part> parts() { return List.of(new Part(-W - 3, -W - 3, W + 3, 60, 0, 34)); }

    @Override public int probe() { return 30; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 8, false); }

    /** Where the Alliance holds the land: the centre of the Murim map and the frontiers between its regions. */
    @Override public boolean allowedAt(int x, int z) {
        return MurimMap.inBubble(x, z) && MurimMap.centreOrFrontier(x, z, AllianceOutpost.FRONTIER);
    }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        prepare(c, -W - 3, -W - 3, W + 3, W + 3, g, g + 34, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        prepare(c, -9, W + 4, 9, 60, g, g + 16, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        court(c, g);
        walls(c, g);
        gate(c, g);
        paifang(c, g);
        leitai(c, g);
        stand(c, g, -1);
        stand(c, g, 1);
        troopHall(c, g, -1);
        troopHall(c, g, 1);
        pavilion(c, g);
        surroundings(c, g);
        captain(c, g);
        guards(c, g);
    }

    // ---------------------------------------------------------------- the court, the walls, the gate

    /** Flagstones, a pale way from the gate to the leitai, the road out to the paifang, lanterns along the way. */
    private static void court(Canvas c, int g) {
        pave(c, -W + 2, -W + 2, W - 2, W - 2, g, BASE, BASE, s(Blocks.POLISHED_ANDESITE), s(Blocks.CRACKED_STONE_BRICKS));
        c.carried(-W + 2, -W + 2, W - 2, W - 2, g);
        c.fill(-3, g, L + 1, 3, g, W, s(Blocks.POLISHED_DIORITE));
        pave(c, -3, W + 1, 3, 60, g, BASE, BASE, s(Blocks.COBBLESTONE), s(Blocks.CRACKED_STONE_BRICKS));
        c.carried(-3, W + 1, 3, 60, g);
        for (int z : new int[]{18, 24, 30}) for (int x : new int[]{-5, 5}) lanternPost(c, x, g + 1, z, 2, s(Blocks.STONE_BRICK_WALL));
    }

    /** The walls, two thick and ten high: stone bricks then white, coped with azure tiles; banners along the north. */
    private static void walls(Canvas c, int g) {
        for (int x = -W; x <= W; x++) for (int z = -W; z <= W; z++) {
            int ring = Math.min(Math.min(x + W, W - x), Math.min(z + W, W - z));
            if (ring > 1) continue;
            if (Math.abs(x) <= 4 && z >= W - 1) continue;
            c.footing(x, g, z, BASE);
            c.carried(x, z, x, z, g);
            for (int y = g; y <= g + 8; y++) c.set(x, y, z, y <= g + 4 ? BASE : WHITE);
            c.set(x, g + 9, z, slab(MurimBlocks.AZURE_ROOF_TILE_SLAB.get(), SlabType.BOTTOM));
        }
        for (int x : new int[]{-26, -16, 16, 26}) c.set(x, g + 9, -W + 1, Blocks.BLUE_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 0));
    }

    /** The gate: red posts eleven high, a dark lintel faced with gold, an azure gable roof, banners and two lanterns. */
    private static void gate(Canvas c, int g) {
        for (int x = -4; x <= 4; x++) for (int z = W - 1; z <= W; z++) {
            c.footing(x, g, z, BASE);
            c.set(x, g, z, BASE);
            for (int y = g + 1; y <= g + 10; y++) c.set(x, y, z, AIR);
        }
        for (int x : new int[]{-5, 5}) for (int z = W - 1; z <= W; z++) for (int y = g + 1; y <= g + 11; y++) c.set(x, y, z, RED);
        for (int x = -7; x <= 7; x++) for (int z = W - 1; z <= W; z++) c.set(x, g + 11, z, BEAM);
        for (int x = -4; x <= 4; x++) { c.set(x, g + 10, W, GOLD); c.set(x, g + 10, W - 1, BEAM); }
        c.kit().gableRoof(-7, W - 1, 7, W, g + 12, azure(), DARK, WHITE);
        for (int x : new int[]{-5, 5}) wallBanner(c, x, g + 7, W + 1, Blocks.BLUE_WALL_BANNER, Direction.SOUTH);
        for (int x : new int[]{-2, 2}) c.set(x, g + 9, W - 1, hangingLantern());
    }

    /** The paifang where the road reaches the arena, fourteen blocks out. */
    private static void paifang(Canvas c, int g) {
        int z = W + 14;
        for (int x : new int[]{-7, -3, 3, 7}) {
            c.footing(x, g, z, BASE);
            for (int y = g + 1; y <= g + 9; y++) c.set(x, y, z, RED);
        }
        for (int x = -8; x <= 8; x++) c.set(x, g + 10, z, BEAM);
        for (int x = -2; x <= 2; x++) c.set(x, g + 9, z, GOLD);
        c.kit().gableRoof(-8, z, 8, z, g + 11, azure(), DARK, RED);
    }

    // ---------------------------------------------------------------- the leitai

    /**
     * The leitai: 27 x 27, its top three blocks above the court; a red skirt of planks between red corner posts, a smooth
     * stone top with the ring marked in red carpet and four gold summoning seals, a lantern on each corner post, steps up
     * on the gate side, banner poles off its corners.
     */
    private static void leitai(Canvas c, int g) {
        int t = g + TOP;
        for (int x = -L; x <= L; x++) for (int z = -L; z <= L; z++) {
            boolean edge = Math.abs(x) == L || Math.abs(z) == L, corner = Math.abs(x) == L && Math.abs(z) == L;
            c.footing(x, g - 1, z, BASE);
            c.set(x, g, z, BASE);
            for (int y = g + 1; y < t; y++) c.set(x, y, z, corner ? RED : edge ? SKIRT : BASE);
            c.set(x, t, z, corner ? RED : s(Blocks.SMOOTH_STONE));
            if (Math.max(Math.abs(x), Math.abs(z)) == L - 2) c.set(x, t + 1, z, s(Blocks.RED_CARPET));
        }
        c.carried(-L, -L, L, L, t);
        // The summoning seals: a ring of gold round an azure heart, where the captain's units rise.
        for (int sx : new int[]{-SEAL, SEAL}) for (int sz : new int[]{-SEAL, SEAL})
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                c.set(sx + dx, t, sz + dz, dx != 0 || dz != 0 ? GOLD : MurimBlocks.AZURE_ROOF_TILES.get().defaultBlockState());
        for (int x : new int[]{-L, L}) for (int z : new int[]{-L, L}) {
            for (int y = t + 1; y <= t + 5; y++) c.set(x, y, z, RED);
            c.set(x, t + 6, z, s(Blocks.LANTERN));
        }
        for (int i = 1; i <= TOP; i++) {
            int z = L + i, top = t + 1 - i;
            for (int x = -3; x <= 3; x++) {
                for (int y = g + 1; y < top; y++) c.set(x, y, z, BASE);
                c.set(x, top, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
            }
        }
        for (int x : new int[]{-L - 2, L + 2}) for (int z : new int[]{-L - 2, L + 2}) {
            for (int y = g + 1; y <= g + 10; y++) c.set(x, y, z, s(Blocks.DARK_OAK_FENCE));
            c.set(x, g + 11, z, Blocks.BLUE_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 0));
        }
    }

    // ---------------------------------------------------------------- stands, troop halls, the pavilion

    /**
     * A covered stand along one side ({@code side}: -1 west, 1 east): four tiers of stone steps rising away from the
     * leitai, a gallery of dark planks behind them, red columns, a white back wall and an azure hip roof.
     */
    private static void stand(Canvas c, int g, int side) {
        int front = SF * side, back = SB * side;
        Direction up = side < 0 ? Direction.WEST : Direction.EAST;
        for (int z = -SZ; z <= SZ; z++) {
            for (int i = 0; i < 4; i++) {
                int x = front + i * side, top = g + 1 + i;
                for (int y = g + 1; y < top; y++) c.set(x, y, z, BASE);
                c.set(x, top, z, stair(Blocks.STONE_BRICK_STAIRS, up, Half.BOTTOM));
            }
            for (int x = GAL; x <= SB; x++) {
                for (int y = g + 1; y <= g + 4; y++) c.set(x * side, y, z, BASE);
                c.set(x * side, g + 5, z, DARK);
            }
        }
        c.carried(Math.min(GAL * side, back), -SZ, Math.max(GAL * side, back), SZ, g + 5);
        for (int z = -SZ; z <= SZ; z += 6) {
            for (int y = g + 1; y <= g + 9; y++) c.set(front, y, z, RED);
            for (int y = g + 6; y <= g + 9; y++) c.set(back, y, z, RED);
        }
        for (int z = -SZ; z <= SZ; z++) if (z % 6 != 0) for (int y = g + 6; y <= g + 9; y++) c.set(back, y, z, y == g + 9 ? BEAM : WHITE);
        c.kit().roof(Math.min(front, back), -SZ, Math.max(front, back), SZ, g + 10, azure(), false);
        for (int z : new int[]{-12, 0, 12}) c.set(29 * side, g + 9, z, hangingLantern());
    }

    /**
     * A troop hall in a south corner of the court ({@code side}: -1 west, 1 east), its wide doorway open towards the
     * leitai: the captain's reinforcements come out of it. Inside: weapon racks, barrels, a chest of the arena's loot.
     */
    private static void troopHall(Canvas c, int g, int side) {
        int x0 = Math.min(22 * side, 32 * side), x1 = Math.max(22 * side, 32 * side), z0 = 23, z1 = 32, f = g + 1;
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) {
            c.footing(x, g - 1, z, BASE);
            c.set(x, g, z, BASE);
            c.set(x, f, z, x >= x0 && x <= x1 && z >= z0 && z <= z1 ? DARK : BASE);
        }
        c.carried(x0 - 1, z0 - 1, x1 + 1, z1 + 1, f);
        for (int y = f + 1; y <= f + 5; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { c.set(x, y, z, AIR); continue; }
            boolean column = (x - x0) % 5 == 0 && (z == z0 || z == z1) || (z - z0) % 3 == 0 && (x == x0 || x == x1);
            c.set(x, y, z, column ? RED : y == f + 5 ? BEAM : WHITE);
        }
        // The doorway, five wide, towards the leitai; steps up to it.
        for (int i = 0; i < 5; i++) {
            int x = (25 + i) * side;
            for (int y = f + 1; y <= f + 4; y++) c.set(x, y, z0, AIR);
            c.set(x, f, z0 - 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        c.kit().roof(x0, z0, x1, z1, f + 6, azure(), false);
        c.room(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 5, z1 - 1);
        // Weapon racks along the back wall, barrels in the corners, the chest.
        for (int x = x0 + 2; x <= x1 - 2; x++) if (x % 2 == 0) {
            c.set(x, f + 1, z1 - 1, s(Blocks.DARK_OAK_FENCE));
            c.set(x, f + 2, z1 - 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
        int inner = side > 0 ? x1 - 1 : x0 + 1, outer = side > 0 ? x0 + 1 : x1 - 1;
        c.set(inner, f + 1, z0 + 1, s(Blocks.BARREL));
        c.set(inner, f + 1, z0 + 2, s(Blocks.BARREL));
        c.set(outer, f + 1, z1 - 1, s(Blocks.BARREL));
        c.stocked(inner, f + 1, z1 - 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), SiteLoot.ARENA);
        for (int x : new int[]{24 * side, 30 * side}) c.set(x, f + 5, 28, hangingLantern());
        for (int y = g + 1; y <= g + 8; y++) c.set(23 * side, y, z0 - 2, s(Blocks.DARK_OAK_FENCE));
        c.set(23 * side, g + 9, z0 - 2, Blocks.BLUE_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 0));
    }

    /**
     * The judges' pavilion at the north end: a raised floor, red columns open to the leitai, white walls hung with the
     * Alliance's banners, the captain's seat between gold blocks, two bells, a chest of the arena's loot and the cache.
     */
    private static void pavilion(Canvas c, int g) {
        int x0 = -13, x1 = 13, z0 = -W + 3, z1 = -W + 11, f = g + 2;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            c.footing(x, g - 1, z, BASE);
            for (int y = g; y < f; y++) c.set(x, y, z, BASE);
            c.set(x, f, z, DARK);
        }
        c.carried(x0, z0, x1, z1, f);
        for (int i = 1; i <= 2; i++) for (int x = -4; x <= 4; x++) {
            int z = z1 + i, top = f + 1 - i;
            for (int y = g + 1; y < top; y++) c.set(x, y, z, BASE);
            c.set(x, top, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        for (int y = f + 1; y <= f + 7; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean back = z == z0, sideWall = x == x0 || x == x1, front = z == z1;
            boolean column = (front || back) && Math.abs(x) % 4 == 1 || sideWall && (back || front);
            if (column) c.set(x, y, z, RED);
            else if (y == f + 7 && (front || back || sideWall)) c.set(x, y, z, BEAM);
            else if (back || sideWall) c.set(x, y, z, WHITE);
            else c.set(x, y, z, AIR);
        }
        c.kit().roof(x0, z0, x1, z1, f + 8, azure(), false);
        c.room(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 7, z1 - 1);
        int y = f + 1;
        c.set(0, y, z0 + 1, DARK);
        c.set(0, y + 1, z0 + 1, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int x : new int[]{-1, 1}) c.set(x, y, z0 + 1, GOLD);
        for (int x : new int[]{-6, -3, 3, 6}) wallBanner(c, x, y + 3, z0 + 1, Blocks.BLUE_WALL_BANNER, Direction.SOUTH);
        for (int x : new int[]{-9, 9}) c.set(x, y, z0 + 2, s(Blocks.BELL));
        c.stocked(x1 - 1, y, z0 + 1, s(Blocks.BARREL), SiteLoot.CACHE);
        c.stocked(x0 + 1, y, z0 + 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), SiteLoot.ARENA);
        for (int x : new int[]{-6, 6}) table(c, x, y, z1 - 2, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
        for (int x : new int[]{-8, 0, 8}) c.set(x, f + 7, z0 + 4, hangingLantern());
    }

    /** Round the court: braziers along the way, war drums by the gate, weapon racks against the north wall. */
    private static void surroundings(Canvas c, int g) {
        for (int x : new int[]{-12, 12}) for (int z : new int[]{20, 28}) {
            c.set(x, g + 1, z, s(Blocks.STONE_BRICK_WALL));
            c.set(x, g + 2, z, s(Blocks.CAMPFIRE));
        }
        for (int x : new int[]{-8, 8}) {
            c.set(x, g + 1, 31, s(Blocks.DARK_OAK_FENCE));
            c.set(x, g + 2, 31, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.SOUTH));
        }
        for (int x0 : new int[]{-20, 17}) for (int i = 0; i < 4; i++) {
            c.set(x0 + i, g + 1, -W + 2, s(Blocks.DARK_OAK_FENCE));
            c.set(x0 + i, g + 2, -W + 2, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
    }

    // ---------------------------------------------------------------- its people

    /**
     * The captain, on the leitai just north of its centre, facing the gate: the only place he appears
     * (mob/PeakCaptainEntity, docs/mobs/peak-captain.md). He keeps that spot as his post.
     */
    private static void captain(Canvas c, int g) {
        c.spawn(0, g + TOP + 1, -2, Direction.SOUTH, MurimEntities.PEAK_CAPTAIN.get(), captain -> { });
    }

    /** Two spear guards inside the gate, two sword disciples watching from the stands' galleries. */
    private static void guards(Canvas c, int g) {
        c.spawn(-3, g + 1, W - 3, Direction.SOUTH, MurimEntities.ALLIANCE_SPEAR_GUARD.get(), guard -> { });
        c.spawn(3, g + 1, W - 3, Direction.SOUTH, MurimEntities.ALLIANCE_SPEAR_GUARD.get(), guard -> { });
        c.spawn(-29, g + 6, 3, Direction.EAST, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), disciple -> { });
        c.spawn(29, g + 6, -3, Direction.WEST, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), disciple -> { });
    }
}
