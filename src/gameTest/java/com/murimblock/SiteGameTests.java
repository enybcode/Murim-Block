package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.world.structure.site.Canvas;
import com.murimblock.world.structure.site.SitePiece;
import com.murimblock.world.structure.site.SitePlan;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 * The six Murim sites (docs/structures), each built far out on a stone floor: every room roofed, nothing natural
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

    private static void check(GameTestHelper helper, SitePlan plan, String structure, int x, Rotation rotation, int... levels) {
        ServerLevel level = helper.getLevel();
        BoundingBox box = SitePiece.wholeBox(plan, x, Z, rotation, levels).inflatedBy(3);
        int y0 = GROUND - 14, y1 = box.maxY() + 8;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.setChunkForced(cx, cz, true);
        // Forced chunks take a few ticks to load their entities: build once they have (the villagers must be seen).
        helper.runAfterDelay(20, () -> run(helper, level, plan, structure, x, rotation, box, y0, y1, levels));
    }

    private static void run(GameTestHelper helper, ServerLevel level, SitePlan plan, String structure, int x, Rotation rotation,
                            BoundingBox box, int y0, int y1, int[] levels) {
        long seed = 0x5EEDL + x;
        try {
            villagers(level, box).forEach(v -> v.discard()); // left by an earlier run of this persistent test world
            // Whole, as the tests and checks see it.
            ground(level, box, y0, y1);
            SitePiece.Built built = SitePiece.buildWhole(level, plan, x, Z, rotation, seed, levels);
            List<Canvas.Check> checks = built.checks();
            List<String> problems = SiteChecks.verify(level, checks);
            long whole = fingerprint(level, box, y0, y1);
            int blocks = count(level, box, y0, y1);
            // Villagers: each standing in a roofed room (a house), none twice, all really in the world.
            List<Villager> housed = built.spawned().stream().filter(m -> m instanceof Villager && !m.isRemoved()).map(m -> (Villager) m).toList();
            int wholeVillagers = housed.size();
            boolean allInRooms = housed.stream().allMatch(v -> checks.stream().anyMatch(c -> c.room() && c.box().isInside(v.blockPosition())));
            boolean allWorking = housed.stream().allMatch(v -> v.getVillagerData().getProfession() != VillagerProfession.NONE && v.isPersistenceRequired());
            housed.forEach(v -> v.discard());
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
            int chunkedVillagers = (int) canvases.stream().flatMap(c -> c.spawned().stream()).filter(m -> m instanceof Villager && !m.isRemoved()).count();
            canvases.forEach(c -> c.spawned().forEach(m -> m.discard()));
            int expectedVillagers = plan == SitePlan.STILT_VILLAGE ? 3 : 0;
            var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            var sets = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, structure);
            LogUtils.getLogger().info("SITE {}: {} checks, {} blocks, {}", structure, checks.size(), blocks, SiteChecks.summary(problems));
            helper.assertTrue(problems.isEmpty(), structure + ": " + SiteChecks.summary(problems));
            helper.assertTrue(checks.size() > 5, structure + " recorded too few checks: " + checks.size());
            helper.assertTrue(blocks > 1500, structure + " looks unfinished: " + blocks + " blocks");
            helper.assertTrue(whole == chunked, structure + " differs when built chunk by chunk (a part writes outside its box, or reads outside its chunk)");
            helper.assertTrue(wholeVillagers == expectedVillagers && chunkedVillagers == expectedVillagers,
                    structure + ": " + wholeVillagers + " villagers built whole, " + chunkedVillagers + " chunk by chunk, expected " + expectedVillagers);
            helper.assertTrue(allInRooms && allWorking, structure + ": a villager is outside the houses or has no trade");
            helper.assertTrue(registry.containsKey(id) && sets.containsKey(id), structure + " is not registered as a structure with its structure set");
            helper.succeed();
        } finally {
            villagers(level, box).forEach(v -> v.discard());
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int px = box.minX(); px <= box.maxX(); px++) for (int pz = box.minZ(); pz <= box.maxZ(); pz++) for (int py = y0; py <= y1; py++)
                if (!level.getBlockState(p.set(px, py, pz)).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }

    private static List<Villager> villagers(ServerLevel level, BoundingBox box) {
        return level.getEntitiesOfClass(Villager.class, AABB.of(box).inflate(8));
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
