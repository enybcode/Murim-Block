package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.mob.BanditChiefEntity;
import com.murimblock.mob.MountainBanditEntity;
import com.murimblock.world.structure.site.Canvas;
import com.murimblock.world.structure.site.SitePiece;
import com.murimblock.world.structure.site.SiteResidents;
import com.murimblock.world.structure.site.SitePlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The seven Murim sites (docs/structures), each built far out on a stone floor: every room roofed, nothing natural
 * inside, every floor and footing carried; built chunk by chunk (as world generation does, only in the chunks its
 * pieces touch) it must come out block for block the same as built whole; and each is a registered structure with its
 * structure set and biome tag, so /locate can look for it.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SiteGameTests {
    private static final int GROUND = 70, Z = 0;

    private SiteGameTests() { }

    @GameTest(template = "foundation_arena", batch = "site_stilt_village", timeoutTicks = 600)
    public static void stiltVillageIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.STILT_VILLAGE, "jade_river_stilt_village", 5000, Rotation.NONE, GROUND); }

    @GameTest(template = "foundation_arena", batch = "site_bandit_den", timeoutTicks = 600)
    public static void banditDenIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.BANDIT_DEN, "yellow_river_bandit_den", 5200, Rotation.CLOCKWISE_90, GROUND); }

    @GameTest(template = "foundation_arena", batch = "site_beacon_post", timeoutTicks = 600)
    public static void beaconPostIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.BEACON_POST, "jade_gate_beacon_post", 5400, Rotation.CLOCKWISE_180, GROUND); }

    @GameTest(template = "foundation_arena", batch = "site_bamboo_temple", timeoutTicks = 600)
    public static void bambooTempleIsWholeAndCarried(GameTestHelper helper) {
        check(helper, SitePlan.BAMBOO_TEMPLE, "misty_bamboo_temple", 5600, Rotation.COUNTERCLOCKWISE_90, GROUND, GROUND + 4, GROUND + 9);
    }

    @GameTest(template = "foundation_arena", batch = "site_lotus_garden", timeoutTicks = 600)
    public static void lotusGardenIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.LOTUS_GARDEN, "lotus_moon_garden", 5800, Rotation.NONE, GROUND); }

    @GameTest(template = "foundation_arena", batch = "site_peach_hamlet", timeoutTicks = 600)
    public static void peachHamletIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.PEACH_HAMLET, "peach_spring_hamlet", 6000, Rotation.CLOCKWISE_90, GROUND); }

    @GameTest(template = "foundation_arena", batch = "site_alliance_outpost", timeoutTicks = 900)
    public static void allianceOutpostIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.ALLIANCE_OUTPOST, "alliance_outpost", 6400, Rotation.CLOCKWISE_180, GROUND); }

    @GameTest(template = "foundation_arena", batch = "site_bandit_camp", timeoutTicks = 1200)
    public static void banditCampIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.BANDIT_CAMP, "bandit_camp", 7600, Rotation.CLOCKWISE_90, GROUND); }

    @GameTest(template = "foundation_arena", batch = "site_bandit_camp_north", timeoutTicks = 1200)
    public static void theNorthsBanditCampIsDarkWoodUnderSnow(GameTestHelper helper) { regionalCamp(helper, "north", Blocks.DARK_OAK_LOG); }

    @GameTest(template = "foundation_arena", batch = "site_bandit_camp_west", timeoutTicks = 1200)
    public static void theWestsBanditCampIsRammedEarth(GameTestHelper helper) { regionalCamp(helper, "west", Blocks.PACKED_MUD); }

    @GameTest(template = "foundation_arena", batch = "site_bandit_camp_south", timeoutTicks = 1200)
    public static void theSouthsBanditCampIsBamboo(GameTestHelper helper) { regionalCamp(helper, "south", Blocks.BAMBOO_BLOCK); }

    @GameTest(template = "foundation_arena", batch = "site_bandit_camp_east", timeoutTicks = 1200)
    public static void theEastsBanditCampHasAMoat(GameTestHelper helper) { regionalCamp(helper, "east", Blocks.SPRUCE_LOG); }

    /**
     * A bandit camp in the heart of an outer region (rotation none: local north is world north): the whole site's checks
     * and thirty bandits, and its palisade (local 0, -42) of that region's stuff; in the north snow on its tips, in the
     * east water in its ditch, elsewhere none.
     */
    private static void regionalCamp(GameTestHelper helper, String region, Block wall) {
        // The heart of that region in the test world's layout (the regions are dealt anew in each world), on a grid of 16.
        int[] heart = com.murimblock.world.map.MurimMap.heart(com.murimblock.world.map.MurimMap.Region.valueOf(region.toUpperCase(java.util.Locale.ROOT)), 0.75);
        int x = heart[0] & ~15 | 8, z = heart[1] & ~15 | 8;
        check(helper, SitePlan.BANDIT_CAMP, "bandit_camp", x, z, Rotation.NONE, level -> {
            BlockPos post = new BlockPos(x, GROUND + 2, z - 42);
            helper.assertTrue(level.getBlockState(post).is(wall), region + " camp's palisade is " + level.getBlockState(post));
            boolean snow = false;
            for (int y = GROUND + 6; y <= GROUND + 9; y++) snow |= level.getBlockState(post.atY(y)).is(Blocks.SNOW);
            helper.assertTrue(snow == region.equals("north"), region + " camp: snow on the palisade " + snow);
            boolean water = level.getBlockState(new BlockPos(x, GROUND - 1, z - 45)).is(Blocks.WATER);
            helper.assertTrue(water == region.equals("east"), region + " camp: water in the ditch " + water);
        }, GROUND);
    }

    @GameTest(template = "foundation_arena", batch = "site_alliance_arena", timeoutTicks = 1200)
    public static void allianceArenaIsWholeAndCarried(GameTestHelper helper) { check(helper, SitePlan.ALLIANCE_ARENA, "alliance_arena", 6900, Rotation.COUNTERCLOCKWISE_90, GROUND); }

    private static void check(GameTestHelper helper, SitePlan plan, String structure, int x, Rotation rotation, int... levels) {
        check(helper, plan, structure, x, Z, rotation, null, levels);
    }

    /** As above, the site's centre at (x, z); {@code whole}, if given, looks at the site once it is built whole. */
    private static void check(GameTestHelper helper, SitePlan plan, String structure, int x, int z, Rotation rotation,
                              java.util.function.Consumer<ServerLevel> whole, int... levels) {
        ServerLevel level = helper.getLevel();
        BoundingBox box = SitePiece.wholeBox(plan, x, z, rotation, levels).inflatedBy(3);
        int y0 = GROUND - 14, y1 = box.maxY() + 8;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.setChunkForced(cx, cz, true);
        // Forced chunks take a few ticks to load their entities: build once they all have (the residents must be seen).
        whenLoaded(helper, level, box, 0, () -> run(helper, level, plan, structure, x, z, rotation, whole, box, y0, y1, levels));
    }

    /** Runs {@code then} once every chunk under {@code box} has its entities loaded (looked at every 5 ticks, 20 s at most). */
    private static void whenLoaded(GameTestHelper helper, ServerLevel level, BoundingBox box, int waited, Runnable then) {
        helper.runAfterDelay(5, () -> {
            if (SiteResidents.entitiesLoaded(level, box) || waited >= 400) then.run();
            else whenLoaded(helper, level, box, waited + 5, then);
        });
    }

    private static void run(GameTestHelper helper, ServerLevel level, SitePlan plan, String structure, int x, int Z, Rotation rotation,
                            java.util.function.Consumer<ServerLevel> lookAt, BoundingBox box, int y0, int y1, int[] levels) {
        long seed = 0x5EEDL + x + Z;
        try {
            residents(level, box).forEach(m -> m.discard()); // left by an earlier run of this persistent test world
            // Whole, as the tests and checks see it.
            ground(level, box, y0, y1);
            SitePiece.Built built = SitePiece.buildWhole(level, plan, x, Z, rotation, seed, levels);
            List<Canvas.Check> checks = built.checks();
            if (lookAt != null) lookAt.accept(level);
            List<String> problems = SiteChecks.verify(level, checks);
            long whole = fingerprint(level, box, y0, y1);
            int blocks = count(level, box, y0, y1);
            // Its cache: a barrel it already had, holding the sites' loot (a weaponsmith's, and often the Long Fist Scroll); the
            // Lotus Moon Garden has no barrel, so none.
            int caches = 0;
            for (BlockPos p : BlockPos.betweenClosed(box.minX(), y0, box.minZ(), box.maxX(), y1, box.maxZ()))
                if (level.getBlockEntity(p) instanceof net.minecraft.world.RandomizableContainer chest
                        && com.murimblock.world.structure.site.SiteLoot.CACHE.equals(chest.getLootTable())) caches++;
            helper.assertTrue(plan == SitePlan.LOTUS_GARDEN ? caches == 0 : caches >= 1, structure + " has " + caches + " cache(s)");
            // Residents (villagers, bandits): the expected number of each, none twice, all really in the world, standing free
            // on solid ground; villagers in a roofed room (a house) with their trade, the chief in a room (his cave).
            List<Mob> spawned = built.spawned().stream().filter(m -> !m.isRemoved()).toList();
            Map<String, Integer> wholeResidents = tally(spawned);
            List<String> misplaced = new ArrayList<>();
            for (Mob m : spawned) {
                String who = BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()) + " at " + m.blockPosition().toShortString();
                boolean inRoom = checks.stream().anyMatch(c -> c.room() && c.box().isInside(m.blockPosition()));
                if (!level.noCollision(m, m.getBoundingBox())) {
                    List<String> hits = new ArrayList<>();
                    for (var shape : level.getBlockCollisions(m, m.getBoundingBox())) hits.add(shape.bounds().toString());
                    misplaced.add(who + " (box " + m.getBoundingBox() + ") is stuck in blocks " + hits);
                }
                if (!level.getBlockState(BlockPos.containing(m.getX(), m.getY() - 0.1, m.getZ())).blocksMotion()) misplaced.add(who + " stands on nothing");
                if (!m.isPersistenceRequired()) misplaced.add(who + " may despawn");
                if (m instanceof Villager v && (!inRoom || v.getVillagerData().getProfession() == VillagerProfession.NONE)) misplaced.add(who + " is not at home with a trade");
                if (m instanceof BanditChiefEntity && !inRoom) misplaced.add(who + " is not in his cave");
            }
            spawned.forEach(m -> m.discard());
            // Chunk by chunk, each part only in the chunks its box touches, in reverse order.
            ground(level, box, y0, y1);
            List<Canvas> canvases = new ArrayList<>();
            for (int part = plan.site().parts().size() - 1; part >= 0; part--) {
                BoundingBox pb = SitePiece.partBox(plan, part, x, Z, rotation, levels);
                for (int cx = pb.maxX() >> 4; cx >= pb.minX() >> 4; cx--) for (int cz = pb.maxZ() >> 4; cz >= pb.minZ() >> 4; cz--) {
                    BoundingBox chunk = new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight() - 1, (cz << 4) + 15);
                    Canvas c = new Canvas(level, chunk, x, Z, rotation, seed, null);
                    plan.site().build(c, part, levels);
                    canvases.add(c);
                }
            }
            canvases.forEach(Canvas::finish);
            long chunked = fingerprint(level, box, y0, y1);
            Map<String, Integer> chunkedResidents = tally(canvases.stream().flatMap(c -> c.spawned().stream()).filter(m -> !m.isRemoved()).toList());
            canvases.forEach(c -> c.spawned().forEach(m -> m.discard()));
            Map<String, Integer> expected = expectedResidents(plan);
            // A site generated before it had residents (an older world): SiteResidents places them, once.
            residents(level, box).forEach(m -> m.discard());
            List<SitePiece> pieces = new ArrayList<>();
            for (int part = 0; part < plan.site().parts().size(); part++) pieces.add(new SitePiece(plan, part, x, Z, rotation, seed, levels));
            BoundingBox siteBox = SitePiece.wholeBox(plan, x, Z, rotation, levels);
            int late = SiteResidents.settle(level, pieces, siteBox);
            int again = SiteResidents.settle(level, pieces, siteBox);
            Map<String, Integer> lateResidents = tally(level.getEntitiesOfClass(Mob.class, AABB.of(siteBox).inflate(4, 16, 4),
                    m -> expected.containsKey(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString())));
            int expectedCount = expected.values().stream().mapToInt(Integer::intValue).sum();
            var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, structure);
            LogUtils.getLogger().info("SITE {}: {} checks, {} blocks, {}", structure, checks.size(), blocks, SiteChecks.summary(problems));
            helper.assertTrue(problems.isEmpty(), structure + ": " + SiteChecks.summary(problems));
            helper.assertTrue(checks.size() > 5, structure + " recorded too few checks: " + checks.size());
            helper.assertTrue(blocks > 1500, structure + " looks unfinished: " + blocks + " blocks");
            helper.assertTrue(whole == chunked, structure + " differs when built chunk by chunk (a part writes outside its box, or reads outside its chunk)");
            helper.assertTrue(wholeResidents.equals(expected) && chunkedResidents.equals(expected),
                    structure + ": residents " + wholeResidents + " built whole, " + chunkedResidents + " chunk by chunk, expected " + expected);
            helper.assertTrue(misplaced.isEmpty(), structure + ": " + misplaced);
            helper.assertTrue(late == expectedCount && lateResidents.equals(expected),
                    structure + ": an empty older site got " + late + " resident(s) " + lateResidents + ", expected " + expected
                            + "; planned at " + pieces.stream().flatMap(pc -> pc.residents().stream()).map(r -> r.at().toShortString() + (level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(r.at())) ? "" : " (entities not loaded)")).toList()
                            + ", found at " + level.getEntitiesOfClass(Mob.class, AABB.of(siteBox).inflate(8, 32, 8)).stream().map(m -> m.getType().toShortString() + " " + m.blockPosition().toShortString() + (m.isRemoved() ? " removed" : "")).toList());
            helper.assertTrue(again == 0, structure + ": its residents were placed a second time (" + again + ")");
            helper.assertTrue(registry.containsKey(id) && sets.containsKey(id), structure + " is not registered as a structure with its structure set");
            helper.succeed();
        } finally {
            residents(level, box).forEach(m -> m.discard());
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int px = box.minX(); px <= box.maxX(); px++) for (int pz = box.minZ(); pz <= box.maxZ(); pz++) for (int py = y0; py <= y1; py++)
                if (!level.getBlockState(p.set(px, py, pz)).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    /** Who must live in each site, by entity type. */
    static Map<String, Integer> expectedResidents(SitePlan plan) {
        return switch (plan) {
            case STILT_VILLAGE -> Map.of("minecraft:villager", 3);
            case BANDIT_DEN -> Map.of("murimblock:bandit_chief", 1, "murimblock:mountain_bandit", 4);
            case BAMBOO_TEMPLE -> Map.of("murimblock:azure_sensei", 1);
            case PEACH_HAMLET -> Map.of("murimblock:crab_man", 3);
            case BEACON_POST -> Map.of("murimblock:mountain_bandit", 4);
            case LOTUS_GARDEN -> Map.of("minecraft:villager", 3);
            case BANDIT_CAMP -> Map.of("murimblock:mountain_bandit", 30);
            case ALLIANCE_ARENA -> Map.of("murimblock:peak_captain", 1, "murimblock:alliance_merchant", 1, "murimblock:alliance_spear_guard", 2, "murimblock:alliance_sword_disciple", 2);
            case ALLIANCE_OUTPOST -> Map.of("minecraft:villager", 4, "minecraft:horse", 2, "murimblock:alliance_merchant", 1, "murimblock:alliance_spear_guard", 2,
                    "murimblock:alliance_sword_disciple", 2, "murimblock:alliance_archer", 1, "murimblock:alliance_sergeant", 1, "murimblock:alliance_warrior_monk", 1);
            default -> Map.of();
        };
    }

    private static Map<String, Integer> tally(List<Mob> mobs) {
        Map<String, Integer> n = new TreeMap<>();
        for (Mob m : mobs) n.merge(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString(), 1, Integer::sum);
        return n;
    }

    /** Residents a site may leave in the test world: villagers and bandits. */
    private static List<Mob> residents(ServerLevel level, BoundingBox box) {
        return level.getEntitiesOfClass(Mob.class, AABB.of(box).inflate(8, 48, 8), m -> m instanceof Villager || m instanceof MountainBanditEntity || m instanceof BanditChiefEntity
                || m instanceof com.murimblock.mob.AzureSenseiEntity || m instanceof com.murimblock.mob.CrabManEntity || m instanceof com.murimblock.mob.ArmoredCrabEntity
                || m instanceof net.minecraft.world.entity.animal.horse.AbstractHorse || m instanceof com.murimblock.mob.AllianceSoldierEntity
                || m instanceof com.murimblock.mob.PeakCaptainEntity);
    }

    /** Clears the box and lays a thick stone floor whose top is GROUND - 1. */
    private static void ground(ServerLevel level, BoundingBox box, int y0, int y1) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState stone = Blocks.STONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) for (int y = y0; y <= y1; y++)
            level.setBlock(p.set(x, y, z), y < GROUND ? stone : air, 2 | 16);
    }

    private static long fingerprint(ServerLevel level, BoundingBox box, int y0, int y1) {
        long print = 17;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) for (int y = y0; y <= y1; y++)
            print = print * 31 + Block.getId(level.getBlockState(p.set(x, y, z)));
        return print;
    }

    /** Blocks the site placed: anything but air and the stone floor laid for the test. */
    private static int count(ServerLevel level, BoundingBox box, int y0, int y1) {
        int n = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) for (int y = y0; y <= y1; y++)
            if (!level.getBlockState(p.set(x, y, z)).isAir() && !level.getBlockState(p).is(Blocks.STONE)) n++;
        return n;
    }
}
