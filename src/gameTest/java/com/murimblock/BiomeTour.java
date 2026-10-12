package com.murimblock;

import com.murimblock.client.MurimMistClient;
import com.murimblock.world.MurimBiomes;
import com.murimblock.world.block.MurimBlocks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Development-only tour of the Murim biomes in a real client (gradlew -PgameTests runBiomeTour): creates a fresh
 * normal world, finds each biome, flies there, checks that its own rock and flora were generated, and takes two
 * screenshots (build/biome-tour-run/screenshots). Writes biome-tour-report.txt and quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class BiomeTour {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final long SEED = 20261012L;
    private static boolean creating;
    private static int ticks;
    private static int index = -1;
    private static int stepTicks;
    private static volatile BlockPos found;
    /** First the Cloud Cliff Sanctuary (built near the spawn of the fresh world), then the biomes. */
    private static int sanctuaryStep = 0;
    private static volatile com.murimblock.world.structure.SanctuaryPlacement sanctuaryData;
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
    }

    private BiomeTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.biomeTour")) return;
        Minecraft mc = Minecraft.getInstance();
        // Unattended: never pause when the window loses focus.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
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
            if (index >= biomes.size()) return;
            if (index < 0) { index = 0; stepTicks = 0; }
            ResourceKey<Biome> biome = biomes.get(index);
            String name = biome.location().getPath();
            stepTicks++;
            switch (stepTicks) {
                case 1 -> { searchDone = false; scanDone = false; found = null; mc.getSingleplayerServer().execute(() -> {
                    ServerLevel level = mc.getSingleplayerServer().overworld();
                    // The largest stretch of the biome within 6000 blocks (its first occurrence can be a sliver).
                    BlockPos largest = largest(level, biome);
                    var result = largest != null ? com.mojang.datafixers.util.Pair.of(largest, (Object) null)
                            : level.findClosestBiome3d(h -> h.is(biome), BlockPos.ZERO.atY(80), 9600, 32, 64);
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
                case 281 -> { index++; stepTicks = 0; }
                default -> { }
            }
            if (index >= biomes.size()) finish(mc);
        } catch (Exception e) {
            LOGGER.error("Biome tour failed", e);
            failed = true;
            finish(mc);
        }
    }

    private static void sanctuary(Minecraft mc) throws Exception {
        sanctuaryStep++;
        var server = mc.getSingleplayerServer();
        switch (sanctuaryStep) {
            case 1 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                sanctuaryData = com.murimblock.world.structure.SanctuaryPlacement.get(level);
                BlockPos c = sanctuaryData.centre();
                String line = c == null ? "FAILED sanctuary not built" : "sanctuary at " + c.getX() + " " + c.getY() + " " + c.getZ() + ", top " + sanctuaryData.top()
                        + ", foot " + sanctuaryData.foot() + ", world spawn " + level.getSharedSpawnPos() + ", biome " + level.getBiome(c).unwrapKey().map(k -> k.location().toString()).orElse("?");
                report.add(line);
                LOGGER.info("Biome tour: {}", line);
                if (c == null || !level.getSharedSpawnPos().equals(sanctuaryData.foot())) failed = true;
            });
            case 20 -> { if (sanctuaryData == null || sanctuaryData.centre() == null) { sanctuaryStep = -1; return; } look(mc, 62, 18, 62, 0, -12, 0); }
            case 120 -> capture(mc, "sanctuary-aerial.png");
            case 121 -> look(mc, 0, 7, 13, 0, 5, -12);
            case 160 -> capture(mc, "sanctuary-court.png");
            case 161 -> { BlockPos f = sanctuaryData.foot(); var c = sanctuaryData.centre();
                // Exactly what a new player sees on arrival: from the spawn, at eye height, looking up at the spire.
                lookAbs(mc, f.getX() + 0.5, f.getY() + 0.1, f.getZ() + 0.5, c.getX() + 0.5, sanctuaryData.top() - 20, c.getZ() + 0.5);
                server.execute(() -> {
                    int clutter = 0;
                    for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) for (int h = 0; h <= 6; h++) {
                        var s = server.overworld().getBlockState(f.offset(dx, h, dz));
                        if (s.is(net.minecraft.tags.BlockTags.LEAVES) || s.is(net.minecraft.tags.BlockTags.LOGS)) clutter++;
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
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.murimblock.item.MurimItems.SECT_TOKEN.get()));
                BlockPos c = sanctuaryData.centre();
                // 300 blocks away, looking east: the needle should point left-ish towards the sanctuary.
                player.teleportTo(server.overworld(), c.getX() - 200.5, sanctuaryData.top() + 10, c.getZ() + 220.5, -90, 0);
            });
            case 262 -> mc.options.hideGui = false;
            case 320 -> capture(mc, "sanctuary-token.png");
            case 321 -> { mc.options.hideGui = true; sanctuaryStep = -1; }
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
    static BlockPos largest(ServerLevel level, ResourceKey<Biome> biome) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        int n = 94, step = 64;
        boolean[][] in = new boolean[2 * n + 1][2 * n + 1];
        for (int i = -n; i <= n; i++) for (int j = -n; j <= n; j++) {
            in[i + n][j + n] = source.getNoiseBiome((i * step) >> 2, 80 >> 2, (j * step) >> 2, sampler).is(biome);
        }
        BlockPos best = null;
        int bestScore = 0;
        for (int i = -n + 3; i <= n - 3; i++) for (int j = -n + 3; j <= n - 3; j++) {
            if (!in[i + n][j + n]) continue;
            int score = 0;
            for (int a = -3; a <= 3; a++) for (int b = -3; b <= 3; b++) if (in[i + a + n][j + b + n]) score++;
            if (score > bestScore) { bestScore = score; best = new BlockPos(i * step, 80, j * step); }
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
