package com.murimblock;

import com.murimblock.bloodsect.BloodSect;
import com.murimblock.mob.MurimEntities;
import com.murimblock.testing.LootRolls;
import com.murimblock.world.structure.CrabLairPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.*;

/** Actual spawn-event, target-selection and loot-table contracts; no rendered-preview stand-ins. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BanditEcologyGameTests {
    private static BlockPos surface(GameTestHelper h) {
        BlockPos column = h.absolutePos(new BlockPos(4, 2, 4));
        int y = h.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) + 2;
        BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        return pos;
    }
    private static boolean placement(GameTestHelper h, EntityType<?> type, BlockPos p, MobSpawnType reason) {
        var e = new MobSpawnEvent.SpawnPlacementCheck(type, h.getLevel(), reason, p, RandomSource.create(71), true);
        NeoForge.EVENT_BUS.post(e);
        return e.getPlacementCheckResult();
    }

    @GameTest(template = "spear_arena", batch = "ecology_surface")
    public static void vanillaMonstersStayUndergroundButAnimalsAndCommandsRemain(GameTestHelper h) {
        BlockPos p = surface(h);
        for (var type : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.SLIME})
            h.assertFalse(placement(h, type, p, MobSpawnType.NATURAL), "Vanilla monster permitted on surface: " + type);
        h.assertTrue(placement(h, EntityType.COW, p, MobSpawnType.NATURAL), "Surface animals must remain");
        h.assertTrue(placement(h, EntityType.ZOMBIE, p, MobSpawnType.COMMAND), "Explicit command spawn must remain");
        for (int y = 4; y <= 9; y++) for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++)
            h.getLevel().setBlockAndUpdate(p.offset(x-4,y,z-4), Blocks.STONE.defaultBlockState());
        h.assertTrue(placement(h, EntityType.ZOMBIE, p, MobSpawnType.NATURAL), "Vanilla monsters must remain below natural ground");
        h.assertFalse(placement(h, MurimEntities.MOUNTAIN_BANDIT.get(), p, MobSpawnType.NATURAL), "Wandering bandits must stay out of caves");
        h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_lair")
    public static void undergroundLairBlocksRandomSpawnsButKeepsItsGarrison(GameTestHelper h) {
        var data = CrabLairPlacement.get(h.getLevel());
        BlockPos previous = data.centre(), p = h.absolutePos(new BlockPos(4, 2, 4));
        try {
            data.record(p);
            h.assertFalse(placement(h, EntityType.ZOMBIE, p, MobSpawnType.NATURAL), "Random zombie inside registered lair");
            h.assertFalse(placement(h, EntityType.COW, p, MobSpawnType.CHUNK_GENERATION), "Random animal inside structure");
            h.assertTrue(placement(h, MurimEntities.CRAB_KING.get(), p, MobSpawnType.STRUCTURE), "Structure garrison was blocked");
        } finally { data.record(previous); }
        h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_day")
    public static void banditsCanSpawnInDaylightButLessOftenThanAtNight(GameTestHelper h) {
        var level = h.getLevel(); long old = level.getDayTime();
        BlockPos p = surface(h);
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        int day = 0, night = 0;
        try {
            level.setDayTime(6000);
            RandomSource random = RandomSource.create(92);
            for (int i = 0; i < 512; i++) if (SpawnPlacements.checkSpawnRules(MurimEntities.MOUNTAIN_BANDIT.get(), level, MobSpawnType.NATURAL, p, random)) day++;
            level.setDayTime(18000); random = RandomSource.create(92);
            for (int i = 0; i < 512; i++) if (SpawnPlacements.checkSpawnRules(MurimEntities.MOUNTAIN_BANDIT.get(), level, MobSpawnType.NATURAL, p, random)) night++;
        } finally { level.setDayTime(old); }
        h.assertTrue(day > 30 && day < 220, "Rare daylight spawn missing or too frequent: " + day);
        h.assertTrue(night > day * 2, "Night should be busier: day=" + day + " night=" + night);
        h.succeed();
    }

    private static <T extends Mob> T spawn(GameTestHelper h, EntityType<T> type, int x, boolean ai) {
        for (int fx=0; fx<9; fx++) for (int fz=0; fz<9; fz++) h.setBlock(new BlockPos(fx,0,fz), Blocks.STONE);
        T mob = type.spawn(h.getLevel(), h.absolutePos(new BlockPos(x, 1, 4)), MobSpawnType.COMMAND);
        h.assertTrue(mob != null, "Could not create " + type);
        mob.setPersistenceRequired(); mob.setNoAi(!ai);
        mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(5);
        return mob;
    }

    private static void selects(GameTestHelper h, EntityType<? extends Mob> hunterType, EntityType<? extends Mob> victimType) {
        Mob hunter = spawn(h, hunterType, 2, true), victim = spawn(h, victimType, 5, false);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); victim.setHealth(200);
        boolean[] acquired = {false};
        h.onEachTick(() -> acquired[0] |= hunter.getTarget() == victim);
        h.runAfterDelay(45, () -> {
            boolean selected = acquired[0];
            String evidence = " target="+hunter.getTarget()+" alive="+hunter.isAlive()+" victimAlive="+victim.isAlive()+" canAttack="+hunter.canAttack(victim)+" sees="+hunter.hasLineOfSight(victim)+" distance="+hunter.distanceTo(victim)+" pos="+hunter.position()+" victim="+victim.position();
            hunter.discard(); victim.discard();
            h.assertTrue(selected, hunterType + " failed to target " + victimType + evidence); h.succeed();
        });
    }

    @GameTest(template = "spear_arena", batch = "ecology_villager", timeoutTicks = 70)
    public static void banditsAttackVillagers(GameTestHelper h) { selects(h, MurimEntities.MOUNTAIN_BANDIT.get(), EntityType.VILLAGER); }
    @GameTest(template = "spear_arena", batch = "ecology_tiger", timeoutTicks = 70)
    public static void banditsAttackTigers(GameTestHelper h) { selects(h, MurimEntities.MOUNTAIN_BANDIT.get(), MurimEntities.TIGER.get()); }
    @GameTest(template = "spear_arena", batch = "ecology_alliance", timeoutTicks = 70)
    public static void allianceAttacksBanditsWithoutBeingHit(GameTestHelper h) { selects(h, MurimEntities.ALLIANCE_SWORD_DISCIPLE.get(), MurimEntities.MOUNTAIN_BANDIT.get()); }
    @GameTest(template = "spear_arena", batch = "ecology_azure", timeoutTicks = 70)
    public static void azureSectAttacksBanditsWithoutBeingHit(GameTestHelper h) { selects(h, MurimEntities.AZURE_SENSEI.get(), MurimEntities.MOUNTAIN_BANDIT.get()); }
    @GameTest(template = "spear_arena", batch = "ecology_blood", timeoutTicks = 70)
    public static void bloodCultAttacksBanditsWithoutBeingHit(GameTestHelper h) { selects(h, BloodSect.BLOOD_NINJA.get(), MurimEntities.MOUNTAIN_BANDIT.get()); }
    @GameTest(template = "spear_arena", batch = "ecology_blood_soldier", timeoutTicks = 70)
    public static void newBloodSoldiersAlsoAttackBandits(GameTestHelper h) { selects(h, BloodSect.BLOOD_SOLDIER.get(), MurimEntities.MOUNTAIN_BANDIT.get()); }

    @GameTest(template = "spear_arena", batch = "ecology_custom_region")
    public static void worldEditAndPlayerRegionsPersistAndBlockOnlyInside(GameTestHelper h) throws Exception {
        var level = h.getLevel(); BlockPos p = h.absolutePos(new BlockPos(4, 2, 4));
        var commands = level.getServer().getCommands().getDispatcher();
        var source = level.getServer().createCommandSourceStack().withLevel(level).withPermission(2);
        var data = com.murimblock.world.SpawnProtection.get(level);
        String bounds = (p.getX()+2)+" "+(p.getY()+3)+" "+(p.getZ()+2)+" "+(p.getX()-2)+" "+(p.getY()-1)+" "+(p.getZ()-2);
        try {
            int result = commands.execute("murimspawn protect ecology_test " + bounds, source);
            h.assertTrue(result == 1, "Protection command failed");
            h.assertFalse(placement(h, MurimEntities.MOUNTAIN_BANDIT.get(), p, MobSpawnType.NATURAL), "Bandit spawned in custom building");
            h.assertFalse(placement(h, EntityType.COW, p, MobSpawnType.NATURAL), "Animal spawned in custom building");
            h.assertTrue(placement(h, EntityType.COW, p.offset(4,0,0), MobSpawnType.NATURAL), "Protection leaks outside bounds");
            h.assertTrue(placement(h, EntityType.ZOMBIE, p, MobSpawnType.SPAWN_EGG), "Spawn egg blocked");
            var saved = data.save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
            var restored = com.murimblock.world.SpawnProtection.load(saved, level.registryAccess());
            h.assertTrue(restored.contains(p) && !restored.contains(p.offset(4,0,0)), "Saved region did not survive reload");
            h.assertFalse(com.murimblock.world.SpawnProtection.get(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER)).contains(p), "Region leaks into another dimension");
            h.assertTrue(commands.execute("murimspawn remove ecology_test", source) == 1, "Region could not be removed");
            h.assertTrue(placement(h, EntityType.COW, p, MobSpawnType.NATURAL), "Removed region still active");
        } finally { data.remove("ecology_test"); }
        h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_special_spawn")
    public static void phantomSpecialSpawnerCannotBypassSurfaceRule(GameTestHelper h) {
        var level = h.getLevel(); BlockPos p = surface(h);
        var phantom = EntityType.PHANTOM.create(level); phantom.moveTo(p, 0, 0);
        var event = new net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent(phantom, level, p.getX(), p.getY(), p.getZ(),
                level.getCurrentDifficultyAt(p), MobSpawnType.NATURAL, null, null);
        NeoForge.EVENT_BUS.post(event);
        h.assertTrue(event.isSpawnCancelled(), "Phantom special spawner bypassed surface ban"); h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_animals")
    public static void banditsNeverChooseOrdinaryAnimalsOrOtherBandits(GameTestHelper h) {
        var bandit = spawn(h, MurimEntities.MOUNTAIN_BANDIT.get(), 3, false);
        for (var type : new EntityType<?>[]{EntityType.COW, EntityType.WOLF, EntityType.CAT, MurimEntities.MOUNTAIN_BANDIT.get(), MurimEntities.BANDIT_CHIEF.get()}) {
            var target = (net.minecraft.world.entity.LivingEntity) type.create(h.getLevel());
            boolean allowed = bandit.canAttack(target);
            h.assertFalse(allowed, "Bandit may attack protected creature: " + type);
        }
        bandit.discard(); h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_structure_bounds")
    public static void generatedStructureBoundsProtectItsInteriorAndYard(GameTestHelper h) {
        var level = h.getLevel(); BlockPos p = h.absolutePos(new BlockPos(4, 2, 4));
        var chunk = level.getChunkAt(p);
        var starts = new java.util.HashMap<>(chunk.getAllStarts());
        var references = new java.util.HashMap<>(chunk.getAllReferences());
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("desert_pyramid"));
        var piece = new net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece(RandomSource.create(1), p.getX()-5, p.getZ()-5);
        var box = piece.getBoundingBox();
        piece.move(p.getX()-box.minX()-5, p.getY()-box.minY()-2, p.getZ()-box.minZ()-5);
        var start = new net.minecraft.world.level.levelgen.structure.StructureStart(structure, chunk.getPos(), 0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
        try {
            chunk.setStartForStructure(structure, start);
            // Replace this structure's reference set instead of modifying a shared original set.
            var refs = new java.util.HashMap<>(references);
            refs.put(structure, new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[]{chunk.getPos().toLong()}));
            chunk.setAllReferences(refs);
            h.assertFalse(placement(h, EntityType.COW, p, MobSpawnType.NATURAL), "Random mob inside generated structure");
            h.assertFalse(placement(h, EntityType.WANDERING_TRADER, p, MobSpawnType.EVENT), "Random trader bypasses building protection");
            h.assertFalse(placement(h, EntityType.TRADER_LLAMA, p, MobSpawnType.EVENT), "Random trader llama bypasses building protection");
            h.assertTrue(placement(h, EntityType.COW, p.above(40), MobSpawnType.NATURAL), "Structure protection leaks vertically");
            h.assertTrue(placement(h, MurimEntities.BANDIT_WARLORD.get(), p, MobSpawnType.EVENT), "Authored boss entrance blocked");
        } finally { chunk.setAllStarts(starts); chunk.setAllReferences(references); }
        h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_generation")
    public static void generationCheckSurvivesSaveAndRunsOnlyOnceBeforeAi(GameTestHelper h) {
        var level = h.getLevel(); BlockPos p = h.absolutePos(new BlockPos(4, 2, 4));
        // No generation chunk cache: this event must not look up chunks or structures from a worker.
        var region = new net.minecraft.server.level.WorldGenRegion(level, null,
                net.minecraft.world.level.chunk.status.ChunkPyramid.GENERATION_PYRAMID.getStepTo(net.minecraft.world.level.chunk.status.ChunkStatus.SPAWN), level.getChunkAt(p));
        var data = com.murimblock.world.SpawnProtection.get(level);
        var original = EntityType.COW.create(level); original.moveTo(p, 0, 0);
        var event = new net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent(original, region, p.getX(), p.getY(), p.getZ(),
                level.getCurrentDifficultyAt(p), MobSpawnType.CHUNK_GENERATION, null, null);
        NeoForge.EVENT_BUS.post(event);
        var saved = original.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var protectedCow = EntityType.COW.create(level); protectedCow.load(saved);
        var allowedCow = EntityType.COW.create(level); allowedCow.load(saved);
        try {
            data.protect("ecology_generation", p, p.above(3));
            var tick = new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(protectedCow);
            NeoForge.EVENT_BUS.post(tick);
            h.assertTrue(protectedCow.isRemoved() && tick.isCanceled(), "Generated mob survived in protected structure after save/reload");
            data.remove("ecology_generation");
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(allowedCow));
            h.assertFalse(allowedCow.isRemoved(), "Unprotected generated mob removed");
            data.protect("ecology_generation", p, p.above(3));
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(allowedCow));
            h.assertFalse(allowedCow.isRemoved(), "Existing animal rechecked after its first tick");
        } finally { data.remove("ecology_generation"); original.discard(); protectedCow.discard(); allowedCow.discard(); }
        h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_siege")
    public static void midnightSiegeCannotBringZombiesBackToTheSurface(GameTestHelper h) {
        var spectator = player(h);
        try {
            var event = new net.neoforged.neoforge.event.village.VillageSiegeEvent(new net.minecraft.world.entity.ai.village.VillageSiege(),
                    h.getLevel(), spectator, net.minecraft.world.phys.Vec3.atBottomCenterOf(surface(h)));
            NeoForge.EVENT_BUS.post(event);
            h.assertTrue(event.isCanceled(), "Midnight zombie siege bypassed surface rule");
        } finally { cleanup(spectator); }
        h.succeed();
    }

    @GameTest(template = "spear_arena", batch = "ecology_books")
    public static void bothArtsDropRarelyOnlyForPlayers(GameTestHelper h) {
        var bandit = spawn(h, MurimEntities.MOUNTAIN_BANDIT.get(), 3, false);
        var killer = player(h);
        try {
            var tally = LootRolls.roll(h.getLevel(), bandit, killer, 3000);
            for (var book : new net.minecraft.world.item.Item[]{com.murimblock.item.MurimItems.LONG_FIST_SCROLL.get(), com.murimblock.martial.TravellerStaffItems.SCROLL.get()}) {
                h.assertTrue(tally.rollsWith(book) > 80 && tally.rollsWith(book) < 230, "Missing or excessive rare art drop: " + book + " count=" + tally.rollsWith(book));
                h.assertTrue(tally.mostInOneRoll(book) == 1, "Art scroll stacked in one kill");
            }
            var noPlayer = LootRolls.roll(h.getLevel(), bandit, null, 512);
            h.assertTrue(noPlayer.rollsWith(com.murimblock.item.MurimItems.LONG_FIST_SCROLL.get()) == 0 && noPlayer.rollsWith(com.murimblock.martial.TravellerStaffItems.SCROLL.get()) == 0, "Faction kill farm drops scrolls");
        } finally { bandit.discard(); cleanup(killer); }
        h.succeed();
    }
}
