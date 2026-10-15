package com.murimblock.world.structure.site;

import com.murimblock.mob.MurimEntities;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.map.MurimMap;
import com.murimblock.world.structure.kit.RoofTiles;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.murimblock.world.structure.kit.BuildKit.hangingLantern;
import static com.murimblock.world.structure.kit.BuildKit.slab;
import static com.murimblock.world.structure.kit.BuildKit.stair;

/**
 * An outpost of the Orthodox Alliance (docs/structures/alliance-outpost.md, from the mock-up scripts/concepts/outpost.js):
 * a garrison fort by a road, in the Alliance's colours (azure roof tiles, gold trim, red columns, white walls). A 53 x 47
 * walled yard: the gate and its gatehouse to the south, a paifang where the road starts, three corner towers and the
 * watchtower whose signal fire relays word to the capital, the command hall, the barracks, the stables, the training
 * yard, the infirmary tent, the notice board, the well and the supplies; outside, a tea stall for travellers.
 * <p>A common sight: anywhere in the Murim map, about one every 1 000 blocks (structure_set/alliance_outpost; Enzo,
 * 15/10/2026: the outpost, which has no boss, spawns often; only the arena keeps to the centre and the frontiers).
 * Its people: a commander, a quartermaster, a medic and a tea-house keeper (villagers with trades and names), two
 * horses in the stables, the Alliance merchant by the road, and its garrison of the Alliance's soldiers ({@link #garrison}).
 */
final class AllianceOutpost extends Site {
    /** The walls (outer faces), local. */
    static final int X0 = -26, X1 = 26, Z0 = -22, Z1 = 24;
    /** The watchtower's centre, in the north-east corner. */
    static final int TX = 21, TZ = -17;
    /** How far from a frontier between regions the Alliance's arena may stand (blocks). */
    static final int FRONTIER = 1500;

    private static final BlockState BASE = s(Blocks.STONE_BRICKS);
    private static final BlockState WHITE = s(Blocks.CALCITE);
    private static final BlockState RED = log(Blocks.STRIPPED_MANGROVE_LOG, Direction.Axis.Y);
    private static final BlockState POST = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
    private static final BlockState BEAM = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X);
    private static final BlockState DARK = s(Blocks.DARK_OAK_PLANKS);
    private static final BlockState GOLD = s(Blocks.HONEYCOMB_BLOCK);
    private static final BlockState CAP = s(Blocks.DEEPSLATE_TILES);

    /** The Alliance's azure glazed tiles (a method: the blocks exist only once registered). */
    private static RoofTiles azure() {
        return new RoofTiles(MurimBlocks.AZURE_ROOF_TILE_STAIRS.get(), MurimBlocks.AZURE_ROOF_TILE_SLAB.get(), MurimBlocks.AZURE_ROOF_TILES.get());
    }

    @Override public int reach() { return 46; }

    @Override public List<Part> parts() { return List.of(new Part(-30, -26, 30, 46, 0, 42)); }

    @Override public int probe() { return 24; }

    @Override public int[] levels(Probe p) { return flat(p, probe(), 8, false); }

    /** Anywhere in the Murim map (never in its ocean ring nor beyond). */
    @Override public boolean allowedAt(int x, int z) {
        return MurimMap.inBubble(x, z);
    }

    /** Frequent, so it steps aside for the other sites (a peach hamlet, the arena...) rather than fall on them. */
    @Override public boolean keepsClearOfOtherSites() { return true; }

    @Override public void build(Canvas c, int part, int[] levels) {
        int g = levels[0];
        prepare(c, X0 - 3, Z0 - 3, X1 + 3, Z1 + 3, g, g + 34, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        prepare(c, -7, Z1 + 3, 7, 46, g, g + 14, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        prepare(c, 8, Z1 + 3, 18, 38, g, g + 12, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
        yard(c, g);
        walls(c, g);
        tower(c, X0 + 2, Z0 + 2, g, 9, 2, Direction.SOUTH);
        tower(c, X0 + 2, Z1 - 2, g, 9, 2, Direction.NORTH);
        tower(c, X1 - 2, Z1 - 2, g, 9, 2, Direction.NORTH);
        watchtower(c, g);
        gatehouse(c, g);
        paifang(c, g);
        hall(c, g);
        barracks(c, g);
        stables(c, g);
        training(c, g);
        infirmary(c, g);
        noticeBoard(c, g);
        well(c, -8, 14, g);
        supplies(c, g);
        teaStall(c, g);
        garrison(c, g);
    }

    // ---------------------------------------------------------------- the yard and the walls

    /** Trodden earth inside the walls, a flagged way from the gate to the hall, and the road out to the paifang. */
    private static void yard(Canvas c, int g) {
        pave(c, X0 + 2, Z0 + 2, X1 - 2, Z1 - 2, g, s(Blocks.COARSE_DIRT), s(Blocks.PACKED_MUD), s(Blocks.COARSE_DIRT), s(Blocks.DIRT_PATH));
        c.carried(X0 + 2, Z0 + 2, X1 - 2, Z1 - 2, g);
        pave(c, -2, -10, 2, Z1, g, BASE, BASE, s(Blocks.CRACKED_STONE_BRICKS));
        pave(c, -2, Z1 + 1, 2, 46, g, BASE, BASE, s(Blocks.COBBLESTONE), s(Blocks.CRACKED_STONE_BRICKS));
        c.carried(-2, Z1 + 1, 2, 46, g);
        for (int z : new int[]{8, 16}) for (int x : new int[]{-3, 3}) lanternPost(c, x, g + 1, z, 2, s(Blocks.STONE_BRICK_WALL));
    }

    /**
     * The walls, two thick: stone bricks three high, then white, a walkway of dark planks on the inner ring, crenels of
     * dark tiles on the outer; azure banners along the north; stairs up to the walkway west of the gate.
     */
    private static void walls(Canvas c, int g) {
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            int ring = Math.min(Math.min(x - X0, X1 - x), Math.min(z - Z0, Z1 - z));
            if (ring > 1) continue;
            if (Math.abs(x) <= 2 && z >= Z1 - 1) continue; // the gate passage (the gatehouse)
            c.footing(x, g, z, BASE);
            c.carried(x, z, x, z, g);
            for (int y = g; y <= g + 6; y++) c.set(x, y, z, y <= g + 3 ? BASE : ring == 1 && y == g + 6 ? DARK : WHITE);
            if (ring == 0 && ((x + z) & 1) == 0) c.set(x, g + 7, z, CAP);
        }
        for (int i = 0; i < 6; i++) {
            int x = -9 + i, y = g + 1 + i;
            c.set(x, y, Z1 - 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, Half.BOTTOM));
            for (int yy = g + 1; yy < y; yy++) c.set(x, yy, Z1 - 2, BASE);
        }
        for (int x : new int[]{-18, -8, 8, 18}) c.set(x, g + 7, Z0 + 1, Blocks.BLUE_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, 0));
    }

    /**
     * A square tower ({@code w}: half its side) {@code h} high: stone bricks, then white between red corner posts, arrow
     * slits; a lookout of dark planks one block out all round, red posts and a railing, an azure pyramid roof. A door at
     * its foot on the side towards the yard, a ladder inside up to the lookout.
     */
    private static void tower(Canvas c, int cx, int cz, int g, int h, int w, Direction door) {
        int top = g + h;
        for (int x = cx - w; x <= cx + w; x++) for (int z = cz - w; z <= cz + w; z++) {
            boolean edge = Math.abs(x - cx) == w || Math.abs(z - cz) == w, corner = Math.abs(x - cx) == w && Math.abs(z - cz) == w;
            c.footing(x, g, z, BASE);
            c.carried(x, z, x, z, g);
            c.set(x, g, z, BASE);
            for (int y = g + 1; y <= top; y++) {
                if (!edge) { c.set(x, y, z, AIR); continue; }
                boolean slit = (x == cx || z == cz) && (y - g) % 5 == 1 && y > g + 3 && y < top;
                c.set(x, y, z, corner ? RED : slit ? AIR : y <= g + 3 ? BASE : WHITE);
            }
        }
        // The lookout: a floor one block out, red posts at its corners, a railing between them.
        int lw = w + 1;
        for (int x = cx - lw; x <= cx + lw; x++) for (int z = cz - lw; z <= cz + lw; z++) {
            c.set(x, top + 1, z, DARK);
            boolean edge = Math.abs(x - cx) == lw || Math.abs(z - cz) == lw, corner = Math.abs(x - cx) == lw && Math.abs(z - cz) == lw;
            if (corner) for (int y = top + 2; y <= top + 4; y++) c.set(x, y, z, RED);
            else if (edge) c.set(x, top + 2, z, s(Blocks.DARK_OAK_FENCE));
        }
        c.kit().roof(cx - lw, cz - lw, cx + lw, cz + lw, top + 5, azure(), true);
        // The ladder, against the wall opposite the door, up through the lookout floor.
        Direction back = door.getOpposite();
        int lx = cx + back.getStepX() * (w - 1), lz = cz + back.getStepZ() * (w - 1);
        ladder(c, lx, g + 1, top + 1, lz, door);
        door(c, cx + door.getStepX() * w, g + 1, cz + door.getStepZ() * w, Blocks.SPRUCE_DOOR, door.getOpposite());
    }

    /** The watchtower: a tall tower whose lookout holds the signal fire; a shaft open over it for the smoke. */
    private static void watchtower(Canvas c, int g) {
        int h = 18, top = g + h;
        tower(c, TX, TZ, g, h, 3, Direction.SOUTH);
        c.set(TX, top + 1, TZ, s(Blocks.HAY_BLOCK));
        c.set(TX, top + 2, TZ, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true));
        for (int y = top + 3; y <= top + 16; y++) c.set(TX, y, TZ, AIR); // the smoke's way out through the roof
        for (int x = TX - 1; x <= TX + 1; x++) for (int z = TZ - 1; z <= TZ + 1; z++)
            if (x != TX || z != TZ) c.set(x, top + 2, z, s(Blocks.STONE_BRICK_WALL));
    }

    /** The gatehouse: red posts each side of the passage, a gold-faced lintel, an azure gable roof, banners on the posts. */
    private static void gatehouse(Canvas c, int g) {
        for (int x = -2; x <= 2; x++) for (int z = Z1 - 1; z <= Z1; z++) {
            c.footing(x, g, z, BASE);
            c.set(x, g, z, BASE);
            for (int y = g + 1; y <= g + 6; y++) c.set(x, y, z, AIR);
        }
        c.carried(-2, Z1 - 1, 2, Z1, g);
        for (int x : new int[]{-3, 3}) for (int z = Z1 - 1; z <= Z1; z++) for (int y = g + 1; y <= g + 8; y++) c.set(x, y, z, RED);
        for (int x = -4; x <= 4; x++) {
            c.set(x, g + 8, Z1 - 1, BEAM);
            c.set(x, g + 8, Z1, BEAM);
        }
        for (int x = -2; x <= 2; x++) { c.set(x, g + 7, Z1, GOLD); c.set(x, g + 7, Z1 - 1, BEAM); }
        c.kit().gableRoof(-4, Z1 - 1, 4, Z1, g + 9, azure(), DARK, WHITE);
        for (int x : new int[]{-3, 3}) wallBanner(c, x, g + 5, Z1 + 1, Blocks.BLUE_WALL_BANNER, Direction.SOUTH);
        c.set(0, g + 6, Z1, hangingLantern());
    }

    /** The paifang where the road comes to the outpost: four red posts, a gold beam, an azure roof. */
    private static void paifang(Canvas c, int g) {
        int z = Z1 + 12;
        for (int x : new int[]{-5, -2, 2, 5}) {
            c.footing(x, g, z, BASE);
            for (int y = g + 1; y <= g + 7; y++) c.set(x, y, z, RED);
        }
        for (int x = -6; x <= 6; x++) c.set(x, g + 8, z, BEAM);
        for (int x = -1; x <= 1; x++) c.set(x, g + 7, z, GOLD);
        c.kit().gableRoof(-6, z, 6, z, g + 9, azure(), DARK, RED);
    }

    // ---------------------------------------------------------------- the buildings

    /** The command hall at the back of the yard: red columns, white walls, lattice front, an azure hip roof. */
    private static void hall(Canvas c, int g) {
        int x0 = -10, x1 = 10, z0 = -18, z1 = -12, f = g + 1;
        c.fill(x0 - 1, g, z0 - 1, x1 + 1, f, z1 + 1, BASE);
        for (int x = x0 - 1; x <= x1 + 1; x++) for (int z = z0 - 1; z <= z1 + 1; z++) c.footing(x, g - 1, z, BASE);
        c.fill(x0 + 1, f, z0 + 1, x1 - 1, f, z1 - 1, DARK);
        c.carried(x0 - 1, z0 - 1, x1 + 1, z1 + 1, f);
        for (int x = -2; x <= 2; x++) c.set(x, g + 1, z1 + 2, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.BOTTOM));
        for (int y = f + 1; y <= f + 5; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { c.set(x, y, z, AIR); continue; }
            boolean column = (x == x0 || x == x1 || x % 3 == 0) && (z == z0 || z == z1) || (x == x0 || x == x1) && z == (z0 + z1) / 2;
            if (column) c.set(x, y, z, RED);
            else if (z == z1 && y >= f + 2 && y <= f + 3) c.set(x, y, z, lattice(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH));
            else c.set(x, y, z, y == f + 5 ? BEAM : WHITE);
        }
        for (int x = -1; x <= 1; x++) door(c, x, f + 1, z1, Blocks.DARK_OAK_DOOR, Direction.NORTH);
        c.kit().roof(x0, z0, x1, z1, f + 6, azure(), false);
        c.room(x0 + 1, f + 1, z0 + 1, x1 - 1, f + 5, z1 - 1);
        int y = f + 1;
        // The commander's office: the desk with the post's ledger, maps, shelves, the Alliance's banners.
        c.set(0, y, z0 + 1, s(Blocks.LECTERN));
        c.set(-1, y, z0 + 1, s(Blocks.CARTOGRAPHY_TABLE));
        c.set(1, y, z0 + 1, s(Blocks.CARTOGRAPHY_TABLE));
        for (int x = x0 + 1; x <= x1 - 1; x++) if (Math.abs(x) >= 4 && Math.abs(x) <= 7) { c.set(x, y, z0 + 1, s(Blocks.BOOKSHELF)); c.set(x, y + 1, z0 + 1, s(Blocks.BOOKSHELF)); }
        for (int x : new int[]{-2, 2}) wallBanner(c, x, y + 2, z0 + 1, Blocks.BLUE_WALL_BANNER, Direction.SOUTH);
        c.set(0, y + 3, z0 + 1, GOLD);
        c.stocked(x1 - 1, y, z0 + 1, s(Blocks.BARREL), SiteLoot.CACHE);
        c.set(x0 + 1, y, z0 + 1, s(Blocks.BARREL));
        for (int x = -6; x <= 6; x += 3) if (x != 0) table(c, x, y, z0 + 3, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_PRESSURE_PLATE);
        c.set(x0 + 1, y, z1 - 1, s(Blocks.POTTED_AZALEA));
        c.set(x1 - 1, y, z1 - 1, s(Blocks.POTTED_AZALEA));
        for (int x : new int[]{-6, 0, 6}) c.set(x, f + 5, -15, hangingLantern());
        for (int x : new int[]{-3, 3}) wallBanner(c, x, f + 3, z1 + 1, Blocks.BLUE_WALL_BANNER, Direction.SOUTH);
        resident(c, 0, y, -15, VillagerProfession.CARTOGRAPHER, "commander");
    }

    /** The barracks along the west wall: white walls between dark posts, a grey hip roof, a row of blue beds. */
    private static void barracks(Canvas c, int g) {
        int x0 = -22, x1 = -15, z0 = -12, z1 = 8;
        c.fill(x0, g, z0, x1, g, z1, BASE);
        c.fill(x0 + 1, g, z0 + 1, x1 - 1, g, z1 - 1, s(Blocks.SPRUCE_PLANKS));
        for (int y = g + 1; y <= g + 4; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            if (!edge) { c.set(x, y, z, AIR); continue; }
            boolean post = (x == x0 || x == x1) && (z - z0) % 4 == 0 || (z == z0 || z == z1) && (x == x0 || x == x1);
            if (post) c.set(x, y, z, POST);
            else if (x == x1 && y == g + 2 && (z - z0) % 4 == 2) c.set(x, y, z, lattice(Blocks.SPRUCE_TRAPDOOR, Direction.EAST));
            else c.set(x, y, z, WHITE);
        }
        door(c, x1, g + 1, -2, Blocks.SPRUCE_DOOR, Direction.WEST);
        c.kit().roof(x0, z0, x1, z1, g + 5, RoofTiles.GREY, false);
        c.room(x0 + 1, g + 1, z0 + 1, x1 - 1, g + 4, z1 - 1);
        for (int z = z0 + 1; z <= z1 - 1; z += 2) if (z != -2 && z != -3) bed(c, x0 + 2, g + 1, z, Direction.WEST, Blocks.BLUE_BED);
        for (int z = z0 + 1; z <= z1 - 1; z += 4) c.set(x1 - 1, g + 1, z, s(Blocks.BARREL));
        c.set(x1 - 1, g + 1, z1 - 1, s(Blocks.CHEST));
        for (int z : new int[]{-6, 4}) c.set(x0 + 4, g + 4, z, hangingLantern());
        resident(c, x1 - 2, g + 1, 0, VillagerProfession.WEAPONSMITH, "quartermaster");
    }

    /** The stables along the east wall: posts under a grey roof, stalls behind gates, hay and water; two horses. */
    private static void stables(Canvas c, int g) {
        int x0 = 15, x1 = 22, z0 = 1, z1 = 13;
        c.fill(x0, g, z0, x1, g, z1, s(Blocks.COARSE_DIRT));
        for (int z = z0; z <= z1; z += 4) for (int x : new int[]{x0, x1}) for (int y = g + 1; y <= g + 4; y++) c.set(x, y, z, POST);
        for (int z = z0 + 1; z <= z1 - 1; z++) if ((z - z0) % 4 != 0) c.set(x1, g + 1, z, s(Blocks.DARK_OAK_FENCE));
        // Stalls: dividers across, gates on the yard side.
        for (int z = z0 + 4; z < z1; z += 4) for (int x = x0 + 1; x < x1; x++) c.set(x, g + 1, z, s(Blocks.DARK_OAK_FENCE));
        for (int z = z0 + 1; z < z1; z++) if ((z - z0) % 4 != 0)
            c.set(x0, g + 1, z, (z - z0) % 4 == 2 ? Blocks.DARK_OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.EAST) : s(Blocks.DARK_OAK_FENCE));
        for (int z = z0 + 2; z < z1; z += 4) {
            c.set(x1 - 1, g + 1, z, s(Blocks.HAY_BLOCK));
            c.set(x1 - 1, g + 1, z + 1, s(Blocks.WATER_CAULDRON));
        }
        c.kit().roof(x0, z0, x1, z1, g + 5, RoofTiles.GREY, false);
        for (int z : new int[]{3, 11}) c.set(20, g + 4, z, hangingLantern());
        c.spawn(18, g + 1, 3, Direction.WEST, EntityType.HORSE, horse -> { });
        c.spawn(18, g + 1, 11, Direction.WEST, EntityType.HORSE, horse -> { });
    }

    /** The training yard: dummies in two rows, weapon racks, a grindstone and an anvil. */
    private static void training(Canvas c, int g) {
        for (int x = -6; x <= 6; x += 4) for (int z : new int[]{-6, -2}) {
            int dx = x == -2 ? -4 : x == 2 ? 4 : x; // keep the flagged way (x -2..2) clear
            c.set(dx, g + 1, z, s(Blocks.DARK_OAK_FENCE));
            c.set(dx, g + 2, z, s(Blocks.HAY_BLOCK));
            c.set(dx, g + 3, z, s(Blocks.TARGET));
            c.set(dx - 1, g + 2, z, s(Blocks.DARK_OAK_FENCE));
            c.set(dx + 1, g + 2, z, s(Blocks.DARK_OAK_FENCE));
        }
        for (int[] r : new int[][]{{-10, 4}, {6, 4}}) {
            for (int i = 0; i < 4; i++) {
                c.set(r[0] + i, g + 1, r[1], s(Blocks.DARK_OAK_FENCE));
                c.set(r[0] + i, g + 2, r[1], slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
            }
        }
        c.set(-11, g + 1, 4, s(Blocks.GRINDSTONE));
        c.set(10, g + 1, 4, s(Blocks.ANVIL));
        c.set(10, g + 1, 5, s(Blocks.SMITHING_TABLE));
    }

    /** The infirmary: a blue awning on posts, two beds, a brewing stand and water. */
    private static void infirmary(Canvas c, int g) {
        int x0 = -20, x1 = -12, z0 = 14, z1 = 20;
        for (int[] p : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) for (int y = g + 1; y <= g + 3; y++) c.set(p[0], y, p[1], POST);
        c.fill(x0, g + 4, z0, x1, g + 4, z1, s(Blocks.LIGHT_BLUE_WOOL));
        for (int x = x0; x <= x1; x++) for (int z : new int[]{z0, z1}) c.set(x, g + 5, z, slab(MurimBlocks.AZURE_ROOF_TILE_SLAB.get(), SlabType.BOTTOM));
        c.fill(x0 + 1, g, z0 + 1, x1 - 1, g, z1 - 1, s(Blocks.SPRUCE_PLANKS));
        c.room(x0 + 1, g + 1, z0 + 1, x1 - 1, g + 3, z1 - 1);
        bed(c, -18, g + 1, 16, Direction.WEST, Blocks.WHITE_BED);
        bed(c, -18, g + 1, 18, Direction.WEST, Blocks.WHITE_BED);
        c.set(-14, g + 1, 19, s(Blocks.BREWING_STAND));
        c.set(-13, g + 1, 19, s(Blocks.WATER_CAULDRON));
        c.set(-13, g + 1, 15, s(Blocks.BARREL));
        c.set(-16, g + 3, 17, hangingLantern());
        resident(c, -15, g + 1, 17, VillagerProfession.CLERIC, "medic");
    }

    /** The notice board by the gate: two posts, a pale board, a small azure roof; missions of the Alliance. */
    private static void noticeBoard(Canvas c, int g) {
        int z = 19;
        for (int x : new int[]{5, 9}) for (int y = g + 1; y <= g + 4; y++) c.set(x, y, z, POST);
        for (int x = 6; x <= 8; x++) for (int y = g + 2; y <= g + 4; y++) c.set(x, y, z, s(Blocks.BIRCH_PLANKS));
        for (int x = 4; x <= 10; x++) c.set(x, g + 5, z, slab(MurimBlocks.AZURE_ROOF_TILE_SLAB.get(), SlabType.BOTTOM));
        c.set(7, g + 1, z, s(Blocks.LECTERN));
    }

    /** A well with a mossy kerb, a windlass on two posts. */
    private static void well(Canvas c, int wx, int wz, int g) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            boolean centre = dx == 0 && dz == 0;
            for (int y = g - 4; y <= g; y++) c.set(wx + dx, y, wz + dz, centre && y > g - 4 ? s(Blocks.WATER) : s(Blocks.MOSSY_STONE_BRICKS));
            if (!centre) c.set(wx + dx, g + 1, wz + dz, s(Blocks.STONE_BRICK_WALL));
        }
        for (int dx : new int[]{-1, 1}) c.set(wx + dx, g + 2, wz, s(Blocks.DARK_OAK_FENCE));
        for (int dx = -1; dx <= 1; dx++) c.set(wx + dx, g + 3, wz, BEAM);
        c.set(wx, g + 2, wz, s(Blocks.CHAIN));
    }

    /** The supplies by the gate: barrels and bales, one barrel stocked. */
    private static void supplies(Canvas c, int g) {
        int[][] cargo = {{12, 18}, {13, 18}, {14, 18}, {12, 19}, {14, 19}};
        for (int[] p : cargo) c.set(p[0], g + 1, p[1], c.chance(p[0], g, p[1], 0.5) ? s(Blocks.BARREL) : s(Blocks.HAY_BLOCK));
        c.stocked(13, g + 1, 19, s(Blocks.BARREL), SiteLoot.CACHE);
        c.set(12, g + 2, 18, s(Blocks.BARREL));
    }

    /** The tea stall outside the walls, by the road: four posts, a grey gable roof, a table and stools, a kettle. */
    private static void teaStall(Canvas c, int g) {
        int x0 = 10, x1 = 16, z0 = 28, z1 = 34;
        c.fill(x0, g, z0, x1, g, z1, s(Blocks.SPRUCE_PLANKS));
        c.carried(x0, z0, x1, z1, g);
        for (int[] p : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) for (int y = g + 1; y <= g + 3; y++) c.set(p[0], y, p[1], POST);
        c.kit().gableRoof(x0, z0, x1, z1, g + 4, RoofTiles.GREY, s(Blocks.SPRUCE_PLANKS), s(Blocks.SPRUCE_PLANKS));
        c.room(x0 + 1, g + 1, z0 + 1, x1 - 1, g + 3, z1 - 1);
        table(c, 12, g + 1, 31, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_PRESSURE_PLATE);
        c.set(11, g + 1, 31, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, Half.BOTTOM));
        c.set(13, g + 1, 31, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM));
        c.set(15, g + 1, 33, Blocks.CAMPFIRE.defaultBlockState());
        c.set(15, g + 1, 29, s(Blocks.BARREL));
        c.set(11, g + 1, 33, s(Blocks.FLOWER_POT));
        c.set(13, g + 3, 31, hangingLantern());
        resident(c, 14, g + 1, 30, VillagerProfession.FARMER, "tea_keeper");
    }

    /**
     * The garrison (mob/AllianceSoldierEntity): two spear guards just inside the gate, two sword disciples between the
     * training dummies, an archer on the watchtower's lookout, the sergeant in the hall by the commander, a warrior monk
     * in the infirmary. Each keeps to his post.
     */
    private static void garrison(Canvas c, int g) {
        c.spawn(-2, g + 1, 21, Direction.SOUTH, MurimEntities.ALLIANCE_SPEAR_GUARD.get(), guard -> { });
        c.spawn(2, g + 1, 21, Direction.SOUTH, MurimEntities.ALLIANCE_SPEAR_GUARD.get(), guard -> { });
        c.spawn(-5, g + 1, -4, Direction.EAST, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), disciple -> { });
        c.spawn(5, g + 1, -4, Direction.WEST, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), disciple -> { });
        c.spawn(24, g + 20, -14, Direction.SOUTH, MurimEntities.ALLIANCE_ARCHER.get(), archer -> { });
        c.spawn(4, g + 2, -14, Direction.SOUTH, MurimEntities.ALLIANCE_SERGEANT.get(), sergeant -> { });
        c.spawn(-14, g + 1, 16, Direction.EAST, MurimEntities.ALLIANCE_WARRIOR_MONK.get(), monk -> { });
        // The Alliance merchant, by the road outside the gate, across from the tea stall: in every Alliance site.
        c.spawn(-5, g + 1, 31, Direction.EAST, MurimEntities.ALLIANCE_MERCHANT.get(), merchant -> { });
    }

    /** One of the outpost's people: a villager of the plains with a trade and a name, kept here. */
    private static void resident(Canvas c, int x, int y, int z, VillagerProfession trade, String title) {
        c.spawn(x, y, z, Direction.SOUTH, EntityType.VILLAGER, villager -> {
            villager.setVillagerData(villager.getVillagerData().setType(VillagerType.PLAINS).setProfession(trade).setLevel(1));
            villager.setVillagerXp(1); // keeps the trade even if the workstation is moved
            villager.setCustomName(Component.translatable("villager.murimblock.alliance_outpost." + title));
        });
    }

}
