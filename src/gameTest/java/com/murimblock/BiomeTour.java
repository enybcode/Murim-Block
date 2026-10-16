package com.murimblock;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.murimblock.client.MurimMistClient;
import com.murimblock.item.MurimItems;
import com.murimblock.world.MurimBiomes;
import com.murimblock.world.block.MurimBlocks;
import com.murimblock.world.structure.SanctuaryPlacement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only tour of the Murim biomes in a real client (gradlew -PgameTests runBiomeTour): creates a fresh
 * normal world, finds each biome, flies there, checks that its own rock and flora were generated, and takes two
 * screenshots (build/biome-tour-run/screenshots). Writes biome-tour-report.txt and quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class BiomeTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 20261012L;
    private static boolean creating;
    private static int ticks;
    private static int index = -1;
    private static int stepTicks;
    private static volatile BlockPos found;
    /** First the Cloud Cliff Sanctuary (built near the spawn of the fresh world), then the biomes. */
    private static int sanctuaryStep = 0;
    private static volatile SanctuaryPlacement sanctuaryData;
    private static volatile BlockPos pavilion;
    /** Set by the server thread when the biome search (or the block scan) is done. */
    private static volatile boolean searchDone, scanDone;
    private static int surfaceY;
    private static final List<String> report = new ArrayList<>();
    private static boolean failed;
    private static boolean finished;

    /** Signature blocks of each biome: the tour fails if one is missing around the biome. */
    private static final Map<ResourceKey<Biome>, Predicate<BlockState>> SIGNATURE = new LinkedHashMap<>();

    static {
        SIGNATURE.put(MurimBiomes.JADE_KARST_FOREST, s -> s.is(MurimBlocks.KARST_LIMESTONE.get()) || s.is(MurimBlocks.OSMANTHUS.log().get()) || s.is(MurimBlocks.BANYAN.log().get()));
        SIGNATURE.put(MurimBiomes.LOESS_PLATEAU, s -> s.is(MurimBlocks.LOESS.get()) || s.is(MurimBlocks.FEATHER_GRASS.get()));
        SIGNATURE.put(MurimBiomes.HEXI_FRONTIER_DESERT, s -> s.is(MurimBlocks.GOBI_GRAVEL.get()) || s.is(MurimBlocks.SAXAUL.get()));
        SIGNATURE.put(MurimBiomes.CLOUD_SEA_PEAKS, s -> s.is(MurimBlocks.PALE_GRANITE.get()) || s.is(MurimBlocks.HUANGSHAN_PINE.log().get()));
        SIGNATURE.put(MurimBiomes.SHU_BAMBOO_SEA, s -> s.is(MurimBlocks.MOSO_BAMBOO.get()));
        SIGNATURE.put(MurimBiomes.JIANGNAN_WATERLANDS, s -> s.is(MurimBlocks.WILLOW.log().get()) || s.is(MurimBlocks.LOTUS_PAD.get()) || s.is(MurimBlocks.WILD_RICE.get()));
        SIGNATURE.put(MurimBiomes.PEACH_BLOSSOM_VALLEY, s -> s.is(MurimBlocks.PEACH.log().get()) || s.is(MurimBlocks.PILLAR_SANDSTONE.get()));
        SIGNATURE.put(MurimBiomes.TIANSHAN_SNOWFIELDS, s -> s.is(MurimBlocks.TIANSHAN_SPRUCE.log().get()) || s.is(MurimBlocks.KUNLUN_SEDGE.get())
                || s.is(MurimBlocks.SNOW_LOTUS.get()) || s.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK));
        SIGNATURE.put(MurimBiomes.CAMPHOR_HILLS, s -> s.is(MurimBlocks.CAMPHOR.log().get()) || s.is(MurimBlocks.CAMPHOR.leaves().get())
                || s.is(MurimBlocks.MILK_VETCH.get()));
        SIGNATURE.put(MurimBiomes.KUNLUN_GLACIERS, s -> s.is(MurimBlocks.KUNLUN_ROCK.get()) || s.is(MurimBlocks.RAW_WHITE_JADE.get())
                || s.is(net.minecraft.world.level.block.Blocks.PACKED_ICE));
    }

    /** Where to look for a biome on the Murim map: the heart of its region (the centre's own at the origin). */
    static BlockPos regionHeart(ResourceKey<Biome> biome) {
        int far = com.murimblock.world.map.MurimMap.RADIUS * 3 / 5;
        if (biome == MurimBiomes.TIANSHAN_SNOWFIELDS || biome == MurimBiomes.KUNLUN_GLACIERS) return new BlockPos(0, 80, -far);
        if (biome == MurimBiomes.HEXI_FRONTIER_DESERT || biome == MurimBiomes.LOESS_PLATEAU) return new BlockPos(-far, 80, 0);
        if (biome == MurimBiomes.JADE_KARST_FOREST || biome == MurimBiomes.SHU_BAMBOO_SEA) return new BlockPos(0, 80, far);
        if (biome == MurimBiomes.JIANGNAN_WATERLANDS) return new BlockPos(far, 80, 0);
        if (biome == MurimBiomes.CAMPHOR_HILLS) return new BlockPos(far, 80, 0);
        return new BlockPos(0, 80, 0);
    }

    /** After the biomes: the bubble's coast and its ocean ring, seen from the shore looking out. */
    private static int coastTicks;

    private BiomeTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.biomeTour")) return;
        Minecraft mc = Minecraft.getInstance();
        // Unattended: never pause when the window loses focus.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                // First launch of this game directory shows the accessibility onboarding instead of the title screen.
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-biome-tour", GameType.SPECTATOR, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-biome-tour", settings, new WorldOptions(SEED, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            if (ticks == 20) {
                mc.options.hideGui = true;
                mc.options.renderDistance().set(12);
                mc.getSingleplayerServer().execute(() -> {
                    ServerLevel level = mc.getSingleplayerServer().overworld();
                    level.setDayTime(5000);
                    level.setWeatherParameters(1000000, 0, false, false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                });
            }
            if (ticks < 60) return;
            if (sanctuaryStep >= 0) { sanctuary(mc); return; }
            List<ResourceKey<Biome>> biomes = new ArrayList<>(SIGNATURE.keySet());
            if (index >= biomes.size()) { coast(mc); return; }
            if (index < 0) { index = 0; stepTicks = 0; }
            ResourceKey<Biome> biome = biomes.get(index);
            String name = biome.location().getPath();
            stepTicks++;
            switch (stepTicks) {
                case 1 -> { searchDone = false; scanDone = false; found = null; mc.getSingleplayerServer().execute(() -> {
                    ServerLevel level = mc.getSingleplayerServer().overworld();
                    // The largest stretch of the biome within 6000 blocks of its region's heart (the Murim map puts each
                    // biome in its region; its first occurrence can be a sliver).
                    BlockPos heart = regionHeart(biome);
                    BlockPos largest = largest(level, biome, heart);
                    var result = largest != null ? Pair.of(largest, (Object) null)
                            : level.findClosestBiome3d(h -> h.is(biome), heart, 9600, 32, 64);
                    if (result == null) { searchDone = true; return; }
                    // The search stops at the biome edge: look around for its heart, the spot with the most of it nearby.
                    found = heart(level, biome, result.getFirst());
                    teleport(mc, found.getX() + 0.5, 230, found.getZ() + 0.5, 0, 90);
                    searchDone = true;
                }); }
                case 2 -> {
                    // Wait on the server search before going on.
                    if (!searchDone) { stepTicks = 1; return; }
                    if (found == null) fail(name + ": not found within 9600 blocks");
                }
                case 160 -> mc.getSingleplayerServer().execute(() -> {
                    ServerLevel level = mc.getSingleplayerServer().overworld();
                    surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, found.getX(), found.getZ());
                    int signature = 0, biomeColumns = 0;
                    BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
                    for (int dx = -48; dx <= 48; dx += 2) for (int dz = -48; dz <= 48; dz += 2) {
                        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, found.getX() + dx, found.getZ() + dz);
                        if (level.getBiome(p.set(found.getX() + dx, top, found.getZ() + dz)).is(biome)) biomeColumns++;
                        for (int y = top; y > top - 24; y--) {
                            if (SIGNATURE.get(biome).test(level.getBlockState(p.set(found.getX() + dx, y, found.getZ() + dz)))) { signature++; break; }
                        }
                    }
                    String line = name + " at " + found.getX() + " " + surfaceY + " " + found.getZ() + ": " + biomeColumns
                            + " biome columns, " + signature + " columns with its own blocks";
                    report.add(line);
                    LOGGER.info("Biome tour: {}", line);
                    if (signature == 0) failed = true;
                    // View A: from the south-west, above the canopy, looking at the found point.
                    camera(mc, found, surfaceY, -1, -1);
                    scanDone = true;
                });
                case 161 -> { if (!scanDone) stepTicks = 160; }
                case 200 -> capture(mc, name + "-a.png");
                case 201 -> mc.getSingleplayerServer().execute(() -> camera(mc, found, surfaceY, 1, 1));
                case 240 -> capture(mc, name + "-b.png");
                case 241 -> mc.getSingleplayerServer().execute(() -> camera(mc, found, surfaceY, 0, -1, 14));
                case 280 -> capture(mc, name + "-ground.png");
                // View far: from 170 blocks away and high above, the biome's landscape at its scale (the Kunlun's ranges).
                // (The Kunlun's ranges rise over the clouds: seen from below instead, from the snows at their foot.)
                case 281 -> mc.getSingleplayerServer().execute(() -> { if (biome == MurimBiomes.KUNLUN_GLACIERS) fromBelow(mc, found, surfaceY); else farView(mc, found, surfaceY); });
                case 360 -> capture(mc, name + "-far.png");
                // The camphor country: also the edge of one of its woods, seen from the meadow.
                case 361 -> { if (biome == MurimBiomes.CAMPHOR_HILLS) mc.getSingleplayerServer().execute(() -> edgeView(mc, found)); else { index++; stepTicks = 0; } }
                case 440 -> capture(mc, name + "-edge.png");
                case 441 -> { index++; stepTicks = 0; }
                default -> { }
            }
        } catch (Exception e) {
            LOGGER.error("Biome tour failed", e);
            failed = true;
            finish(mc);
        }
    }

    private static void sanctuary(Minecraft mc) throws Exception {
        // The sanctuary is built over the first ticks of the world: wait for it.
        if (sanctuaryStep == 0 && SanctuaryPlacement.building()) return;
        sanctuaryStep++;
        var server = mc.getSingleplayerServer();
        switch (sanctuaryStep) {
            case 1 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                sanctuaryData = SanctuaryPlacement.get(level);
                BlockPos c = sanctuaryData.centre();
                String line = c == null ? "FAILED sanctuary not built" : "sanctuary at " + c.getX() + " " + c.getY() + " " + c.getZ() + ", top " + sanctuaryData.top()
                        + ", foot " + sanctuaryData.foot() + ", world spawn " + level.getSharedSpawnPos() + ", biome " + level.getBiome(c).unwrapKey().map(k -> k.location().toString()).orElse("?");
                report.add(line);
                LOGGER.info("Biome tour: {}", line);
                // The spawn: on the bubble's Murim land, drawn at random (world/map/MurimSpawn).
                BlockPos spawn = level.getSharedSpawnPos();
                boolean murimSpawn = level.getBiome(spawn).is(MurimBiomes.MURIM_TAG);
                String spawnLine = "spawn at " + spawn.toShortString() + " in " + level.getBiome(spawn).unwrapKey().map(k -> k.location().toString()).orElse("?")
                        + (murimSpawn ? "" : " (FAILED: not a Murim biome)");
                report.add(spawnLine);
                LOGGER.info("Biome tour: {}", spawnLine);
                if (!murimSpawn || (Math.abs(spawn.getX()) < 64 && Math.abs(spawn.getZ()) < 64)) failed = true;
                // Near the spawn, but the spawn stays in the wild: never on the sanctuary.
                if (c == null || Math.hypot(c.getX() - level.getSharedSpawnPos().getX(), c.getZ() - level.getSharedSpawnPos().getZ()) < 48) failed = true;
            });
            case 20 -> { if (sanctuaryData == null || sanctuaryData.centre() == null) { sanctuaryStep = -1; return; } look(mc, 62, 18, 62, 0, -12, 0); }
            case 120 -> capture(mc, "sanctuary-aerial.png");
            case 121 -> { look(mc, 0, 7, 13, 0, 5, -12); server.execute(() -> {
                // Lighting must be consistent all round the spire: nothing see-through (air, stairs, slabs...) left dark next to air lit by the sky.
                ServerLevel level = server.overworld();
                BlockPos c = sanctuaryData.centre();
                int dark = 0;
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
                for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) for (int y = c.getY() - 10; y <= sanctuaryData.top() + 20; y++) {
                    p.set(c.getX() + x, y, c.getZ() + z);
                    var here = level.getBlockState(p);
                    if (here.getLightBlock(level, p) >= 15 || level.getBrightness(LightLayer.SKY, p) > 0) continue;
                    for (Direction d : Direction.values()) {
                        BlockPos n = p.relative(d);
                        if (level.getBlockState(n).isAir() && level.getBrightness(LightLayer.SKY, n) >= 3) { dark++; break; }
                    }
                }
                report.add("sanctuary lighting: " + dark + " see-through blocks left dark next to sky-lit air");
                LOGGER.info("Biome tour: sanctuary lighting {} dark", dark);
                if (dark > 0) failed = true;
            }); }
            case 160 -> capture(mc, "sanctuary-court.png");
            case 161 -> { BlockPos f = sanctuaryData.foot(); var c = sanctuaryData.centre();
                // What a player sees arriving at the foot of the road, at eye height, looking up at the spire.
                lookAbs(mc, f.getX() + 0.5, f.getY() + 0.1, f.getZ() + 0.5, c.getX() + 0.5, sanctuaryData.top() - 20, c.getZ() + 0.5);
                server.execute(() -> {
                    int clutter = 0;
                    for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int h = 0; h <= 6; h++) {
                        var s = server.overworld().getBlockState(f.offset(dx, h, dz));
                        if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)) clutter++;
                    }
                    report.add("sanctuary foot: " + clutter + " leaf/log blocks within 4 blocks");
                    if (clutter > 0) failed = true;
                }); }
            case 200 -> capture(mc, "sanctuary-foot.png");
            case 201 -> { server.execute(() -> server.overworld().setDayTime(18000)); look(mc, 46, 10, 46, 0, -4, 0); }
            case 260 -> capture(mc, "sanctuary-night.png");
            case 261 -> server.execute(() -> {
                server.overworld().setDayTime(5000);
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.setGameMode(GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.SECT_TOKEN.get()));
                BlockPos c = sanctuaryData.centre();
                // 300 blocks away, looking east: the needle should point left-ish towards the sanctuary.
                player.teleportTo(server.overworld(), c.getX() - 200.5, sanctuaryData.top() + 10, c.getZ() + 220.5, -90, 0);
            });
            case 262 -> mc.options.hideGui = false;
            case 320 -> capture(mc, "sanctuary-token.png");
            // The entrance as a player walks it: from the end of the plank road, then from above.
            case 321 -> { mc.options.hideGui = true; look(mc, 0, 1.6, 26, 0, 2, 0); }
            case 380 -> capture(mc, "sanctuary-gate.png");
            case 381 -> look(mc, 6, 14, 33, 0, 0, 21);
            case 440 -> capture(mc, "sanctuary-gate-above.png");
            case 441 -> look(mc, 0, 4, 12, -12, 7, 1);
            case 500 -> capture(mc, "sanctuary-roofs.png");
            // Under the gangway, where it leaves the rock.
            case 501 -> look(mc, 3, -3, 25, 0, -5, 18);
            case 560 -> capture(mc, "sanctuary-under-gangway.png");
            // The rest pavilion half-way up, seen from out in the air (found by its hanging lantern's chain).
            case 561 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                BlockPos c = sanctuaryData.centre();
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
                for (int x = -32; x <= 32 && pavilion == null; x++) for (int z = -32; z <= 32 && pavilion == null; z++)
                    for (int y = c.getY(); y < sanctuaryData.top() - 10; y++)
                        if (level.getBlockState(p.set(c.getX() + x, y, c.getZ() + z)).is(Blocks.CHAIN)) { pavilion = p.immutable(); break; }
                report.add("rest pavilion at " + pavilion);
                if (pavilion == null) failed = true;
            });
            case 563 -> { if (pavilion == null) { sanctuaryStep = -1; return; }
                BlockPos c = sanctuaryData.centre();
                double ox = pavilion.getX() - c.getX(), oz = pavilion.getZ() - c.getZ(), d = Math.hypot(ox, oz);
                lookAbs(mc, pavilion.getX() + 0.5 + ox / d * 9 - oz / d * 4, pavilion.getY() + 1, pavilion.getZ() + 0.5 + oz / d * 9 + ox / d * 4,
                        pavilion.getX() + 0.5, pavilion.getY() - 1, pavilion.getZ() + 0.5); }
            case 620 -> { capture(mc, "sanctuary-pavilion.png"); sanctuaryStep = -1; }
            default -> { }
        }
    }

    /** Camera relative to the sanctuary summit (dx, dy, dz from the centre at the top), looking at a point relative to it. */
    private static void look(Minecraft mc, double dx, double dy, double dz, double tx, double ty, double tz) {
        BlockPos c = sanctuaryData.centre();
        int top = sanctuaryData.top();
        lookAbs(mc, c.getX() + 0.5 + dx, top + dy, c.getZ() + 0.5 + dz, c.getX() + 0.5 + tx, top + ty, c.getZ() + 0.5 + tz);
    }

    private static void lookAbs(Minecraft mc, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        mc.getSingleplayerServer().execute(() -> teleport(mc, x, y, z, yaw, pitch));
    }

    /** Centre of the cell (64-block grid, 6000-block radius) with the most of the biome in its 7x7 neighbourhood. */
    /**
     * The bubble's eastern shore, then out over the ocean ring: the shore is Murim land, the ring deep ocean, the world
     * beyond it vanilla again.
     */
    private static void coast(Minecraft mc) throws Exception {
        coastTicks++;
        var server = mc.getSingleplayerServer();
        int shore = com.murimblock.world.map.MurimMap.RADIUS - 400;
        switch (coastTicks) {
            case 1 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                var source = level.getChunkSource().getGenerator().getBiomeSource();
                var sampler = level.getChunkSource().randomState().sampler();
                java.util.function.IntFunction<String> at = x -> source.getNoiseBiome(x >> 2, 200 >> 2, 0, sampler).unwrapKey().map(k -> k.location().toString()).orElse("?");
                String ring = at.apply(com.murimblock.world.map.MurimMap.RADIUS + com.murimblock.world.map.MurimMap.RING / 2), beyond = at.apply(com.murimblock.world.map.MurimMap.RADIUS + com.murimblock.world.map.MurimMap.RING + 6_000);
                String line = "coast: " + at.apply(shore - 1_000) + " inland, " + ring + " in the ring, " + beyond + " beyond it";
                report.add(line);
                LOGGER.info("Biome tour: {}", line);
                if (!ring.contains("ocean")) failed = true;
                teleport(mc, shore + 0.5, 150, 0.5, -90, 12);
            });
            case 200 -> capture(mc, "bubble-coast.png");
            case 201 -> finish(mc);
            default -> { }
        }
    }

    static BlockPos largest(ServerLevel level, ResourceKey<Biome> biome, BlockPos centre) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        int n = 94, step = 64;
        boolean[][] in = new boolean[2 * n + 1][2 * n + 1];
        for (int i = -n; i <= n; i++) for (int j = -n; j <= n; j++) {
            in[i + n][j + n] = source.getNoiseBiome((centre.getX() + i * step) >> 2, 80 >> 2, (centre.getZ() + j * step) >> 2, sampler).is(biome);
        }
        BlockPos best = null;
        int bestScore = 0;
        for (int i = -n + 3; i <= n - 3; i++) for (int j = -n + 3; j <= n - 3; j++) {
            if (!in[i + n][j + n]) continue;
            int score = 0;
            for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) if (in[i + a + n][j + b + n]) score++;
            if (score > bestScore) { bestScore = score; best = new BlockPos(centre.getX() + i * step, 80, centre.getZ() + j * step); }
        }
        return best;
    }

    /** Among points 24 blocks apart within 192 blocks, the one surrounded by the most of the biome. */
    static BlockPos heart(ServerLevel level, ResourceKey<Biome> biome, BlockPos edge) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        int n = 16, step = 24;
        boolean[][] in = new boolean[2 * n + 1][2 * n + 1];
        for (int i = -n; i <= n; i++) for (int j = -n; j <= n; j++) {
            int x = edge.getX() + i * step, z = edge.getZ() + j * step;
            in[i + n][j + n] = source.getNoiseBiome(x >> 2, 80 >> 2, z >> 2, sampler).is(biome);
        }
        BlockPos best = edge;
        int bestScore = -1;
        for (int i = -n + 3; i <= n - 3; i++) for (int j = -n + 3; j <= n - 3; j++) {
            if (!in[i + n][j + n]) continue;
            int score = 0;
            for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) if (in[i + a + n][j + b + n]) score++;
            if (score > bestScore) { bestScore = score; best = new BlockPos(edge.getX() + i * step, 80, edge.getZ() + j * step); }
        }
        return best;
    }

    private static void camera(Minecraft mc, BlockPos target, int ground, int sx, int sz) {
        camera(mc, target, ground, sx, sz, 30);
    }

    /** Places the spectator `height` blocks above the ground, 44 blocks away on (sx, sz), looking at the target. */
    private static void camera(Minecraft mc, BlockPos target, int ground, int sx, int sz, int height) {
        double distance = height < 20 ? 22 : 44;
        double len = Math.hypot(sx, sz);
        double x = target.getX() + sx / len * distance, z = target.getZ() + sz / len * distance;
        ServerLevel level = mc.getSingleplayerServer().overworld();
        double y = Math.max(ground, level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z)) + height;
        double dx = target.getX() - x, dz = target.getZ() - z, dy = ground + 4 - y;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        teleport(mc, x, y, z, yaw, pitch);
    }

    /** From the lowest ground within 180 blocks south of the target, at eye height, looking up at it. */
    private static void fromBelow(Minecraft mc, BlockPos target, int top) {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        int bestX = target.getX(), bestZ = target.getZ() + 180, bestY = Integer.MAX_VALUE;
        for (int dx = -120; dx <= 120; dx += 20) for (int dz = 120; dz <= 180; dz += 20) {
            level.getChunk((target.getX() + dx) >> 4, (target.getZ() + dz) >> 4); // generated, so its height is real
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, target.getX() + dx, target.getZ() + dz);
            if (y < bestY) { bestY = y; bestX = target.getX() + dx; bestZ = target.getZ() + dz; }
        }
        double dx = target.getX() - bestX, dz = target.getZ() - bestZ;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(top - bestY - 4, Math.hypot(dx, dz)));
        teleport(mc, bestX + 0.5, bestY + 4, bestZ + 0.5, yaw, pitch);
    }

    /**
     * A wood's edge: near the target, a column under camphor leaves whose neighbour 12 blocks away (in one of eight
     * directions) is open meadow; the camera stands further out in the meadow, low, looking at the edge.
     */
    private static void edgeView(Minecraft mc, BlockPos target) {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        var leaves = MurimBlocks.CAMPHOR.leaves().get();
        java.util.function.BiPredicate<Integer, Integer> canopy = (x, z) -> {
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            return level.getBlockState(new BlockPos(x, y - 1, z)).is(leaves);
        };
        // How much of a 15 x 15 square round a point is under camphor leaves (25 samples).
        java.util.function.BiFunction<Integer, Integer, Integer> cover = (x, z) -> {
            int n = 0;
            for (int i = -6; i <= 6; i += 3) for (int j = -6; j <= 6; j += 3) if (canopy.test(x + i, z + j)) n++;
            return n;
        };
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int r = 0; r <= 320; r += 16) for (int dx = -r; dx <= r; dx += 16) for (int dz = -r; dz <= r; dz += 16) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            int px = target.getX() + dx, pz = target.getZ() + dz;
            if (cover.apply(px, pz) < 18) continue; // a real wood
            for (int[] d : dirs) {
                if (cover.apply(px + d[0] * 24, pz + d[1] * 24) > 1 || cover.apply(px + d[0] * 36, pz + d[1] * 36) > 1) continue; // open meadow out there
                // The edge itself: walking out of the wood, the last column under leaves.
                int k = 0;
                while (k < 24 && canopy.test(px + d[0] * (k + 1), pz + d[1] * (k + 1))) k++;
                int edgeX = px + d[0] * k, edgeZ = pz + d[1] * k;
                int ex = edgeX + d[0] * 30, ez = edgeZ + d[1] * 30;
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, ex, ez);
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, edgeX, edgeZ);
                double ddx = edgeX - ex, ddz = edgeZ - ez;
                float yaw = (float) Math.toDegrees(Math.atan2(-ddx, ddz));
                float pitch = (float) -Math.toDegrees(Math.atan2(top - 3 - (ground + 9), Math.hypot(ddx, ddz)));
                LOGGER.info("Biome tour: a camphor wood's edge at {} {}, seen from {} {}", edgeX, edgeZ, ex, ez);
                teleport(mc, ex + 0.5, ground + 9, ez + 0.5, yaw, pitch);
                return;
            }
        }
        LOGGER.info("Biome tour: no camphor wood edge found near {}", target);
    }

    /** From 170 blocks south of the target and 70 above the ground there, looking at it. */
    private static void farView(Minecraft mc, BlockPos target, int ground) {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        double x = target.getX() + 0.5, z = target.getZ() + 170.5;
        double y = Math.max(ground, level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z)) + 70;
        float pitch = (float) Math.toDegrees(Math.atan2(y - ground - 10, 170));
        teleport(mc, x, y, z, 180, pitch);
    }

    private static void teleport(Minecraft mc, double x, double y, double z, float yaw, float pitch) {
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(mc.getSingleplayerServer().overworld(), x, y, z, yaw, pitch);
        MurimMistClient.settleNow();
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(name));
        }
        LOGGER.info("Biome tour capture: {}", name);
    }

    private static void fail(String message) {
        report.add("FAILED " + message);
        LOGGER.error("Biome tour: {}", message);
        failed = true;
        index++;
        stepTicks = 0;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("biome-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the biome tour report", e);
        }
        LOGGER.info("Biome tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
