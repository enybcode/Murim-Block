package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.MurimMistClient;
import com.murimblock.world.structure.site.Canvas;
import com.murimblock.world.structure.site.SitePiece;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only tour of the six Murim sites in a real client (gradlew -PgameTests runSiteTour): a fresh normal
 * world; for each site, the real /locate structure command, then a visit to what it found: the generated site must
 * pass its checks (roofs closed, nothing grown inside, floors carried), and three photos are taken
 * (build/site-tour-run/screenshots). Writes site-tour-report.txt and quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SiteTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 20261012L;
    static final String[] SITES = {"jade_river_stilt_village", "yellow_river_bandit_den", "jade_gate_beacon_post",
            "misty_bamboo_temple", "lotus_moon_garden", "peach_spring_hamlet"};
    private static final Pattern AT = Pattern.compile("\\[(-?\\d+), ~, (-?\\d+)\\]");
    private static boolean creating, finished, failed;
    private static int ticks, index = -1, step;
    private static volatile boolean done;
    private static volatile Site site;
    private static final List<String> report = new ArrayList<>();

    /** What the server found for the current site. */
    private record Site(BlockPos origin, Direction front, int top, int reach) { } // reach: to the edge on the front side

    private SiteTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.siteTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-site-tour", GameType.SPECTATOR, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-site-tour", settings, new WorldOptions(SEED, true, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            if (ticks == 20) {
                mc.options.hideGui = true;
                mc.options.renderDistance().set(10);
                mc.getSingleplayerServer().execute(() -> {
                    ServerLevel level = mc.getSingleplayerServer().overworld();
                    level.setDayTime(4000);
                    level.setWeatherParameters(1000000, 0, false, false);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
                });
            }
            if (ticks < 60 || finished) return;
            if (index < 0) { index = 0; step = 0; }
            if (index >= SITES.length) { finish(mc); return; }
            String name = SITES[index];
            step++;
            switch (step) {
                case 1 -> { done = false; site = null; mc.getSingleplayerServer().execute(() -> { visit(mc, name); done = true; }); }
                case 2 -> { if (!done) { step = 1; return; } if (site == null) { index++; step = 0; } }
                case 120 -> look(mc, -0.7, -0.7, 34, 30);
                case 200 -> capture(mc, name + "-a.png");
                case 201 -> look(mc, 0.7, 0.7, 34, 26);
                case 280 -> capture(mc, name + "-b.png");
                // At eye height in front of the entrance, looking in.
                case 281 -> {
                    Site s = site;
                    double fx = s.front().getStepX(), fz = s.front().getStepZ();
                    lookAbs(mc, s.origin().getX() + 0.5 + fx * (s.reach() - 1), s.origin().getY() + 2.7, s.origin().getZ() + 0.5 + fz * (s.reach() - 1),
                            s.origin().getX() + 0.5, s.origin().getY() + 3.5, s.origin().getZ() + 0.5);
                }
                case 360 -> capture(mc, name + "-ground.png");
                case 361 -> { index++; step = 0; }
                default -> { }
            }
        } catch (Exception e) {
            LOGGER.error("Site tour failed", e);
            failed = true;
            finish(mc);
        }
    }

    /** Server thread: /locate the site, generate its chunks, check what was built, remember where to look. */
    private static void visit(Minecraft mc, String name) {
        var server = mc.getSingleplayerServer();
        ServerLevel level = server.overworld();
        List<Component> messages = new ArrayList<>();
        CommandSource capture = new CommandSource() {
            @Override public void sendSystemMessage(Component component) { messages.add(component); }
            @Override public boolean acceptsSuccess() { return true; }
            @Override public boolean acceptsFailure() { return true; }
            @Override public boolean shouldInformAdmins() { return false; }
        };
        CommandSourceStack source = new CommandSourceStack(capture, new Vec3(0.5, 100, 0.5), Vec2.ZERO, level, 4, "SiteTour", Component.literal("SiteTour"), server, null);
        long start = System.currentTimeMillis();
        server.getCommands().performPrefixedCommand(source, "locate structure " + Murimblock.MOD_ID + ":" + name);
        String answer = messages.isEmpty() ? "(no answer)" : messages.get(0).getString();
        var located = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
        String plan = located instanceof com.murimblock.world.structure.site.SiteStructure ss ? ss.plan().getSerializedName() : "?";
        StringBuilder stats = new StringBuilder();
        com.murimblock.world.structure.site.SiteStructure.STATS.forEach((k, v) -> { if (k.startsWith(plan + " ")) stats.append(k.substring(plan.length() + 1)).append('=').append(v.get()).append(' '); });
        stats.append("wet=").append(com.murimblock.world.structure.site.Site.WET.getAndSet(0)).append(" steep=").append(com.murimblock.world.structure.site.Site.STEEP.getAndSet(0));
        LOGGER.info("Site tour: {} places tried: {}", name, stats);
        Matcher m = AT.matcher(answer);
        if (!m.find()) { fail(name + ": /locate failed: " + answer + " (places tried: " + stats + ")"); return; }
        int x = Integer.parseInt(m.group(1)), z = Integer.parseInt(m.group(2));
        String line = name + ": /locate -> " + answer + " (" + (System.currentTimeMillis() - start) + " ms; places tried: " + stats + ")";
        // Generate the site and its surroundings, then find the start the command pointed at.
        for (int cx = (x >> 4) - 3; cx <= (x >> 4) + 3; cx++) for (int cz = (z >> 4) - 3; cz <= (z >> 4) + 3; cz++) level.getChunk(cx, cz);
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
        StructureStart start2 = level.getChunk(x >> 4, z >> 4).getStartForStructure(structure);
        if (start2 == null || !start2.isValid()) { fail(line + "; no structure start in that chunk"); return; }
        List<Canvas.Check> checks = new ArrayList<>();
        SitePiece first = null;
        for (var piece : start2.getPieces()) if (piece instanceof SitePiece sp) { checks.addAll(sp.checks()); if (first == null) first = sp; }
        List<String> problems = SiteChecks.verify(level, checks);
        // The pieces themselves (the start's own box is widened by 12 for the terrain adaptation).
        BoundingBox box = BoundingBox.encapsulatingBoxes(start2.getPieces().stream().map(p -> p.getBoundingBox()).toList()).orElseThrow();
        String biome = level.getBiome(first.origin()).unwrapKey().map(k -> k.location().getPath()).orElse("?");
        line += "; origin " + first.origin().toShortString() + " in " + biome + ", " + first.rotation() + ", levels " + java.util.Arrays.toString(first.levels())
                + "; " + checks.size() + " checks: " + SiteChecks.summary(problems);
        report.add(line);
        LOGGER.info("Site tour: {}", line);
        if (!problems.isEmpty() || !biome.equals(expectedBiome(name))) failed = true;
        Direction front = first.rotation().rotate(Direction.SOUTH);
        // How far the site reaches on its front side: the ground photo is taken just inside, on the approach.
        BlockPos o = first.origin();
        int edge = switch (front) {
            case SOUTH -> box.maxZ() - o.getZ();
            case NORTH -> o.getZ() - box.minZ();
            case EAST -> box.maxX() - o.getX();
            default -> o.getX() - box.minX();
        };
        site = new Site(o, front, box.maxY(), edge);
    }

    static String expectedBiome(String name) {
        return switch (name) {
            case "jade_river_stilt_village" -> "jade_karst_forest";
            case "yellow_river_bandit_den" -> "loess_plateau";
            case "jade_gate_beacon_post" -> "hexi_frontier_desert";
            case "misty_bamboo_temple" -> "shu_bamboo_sea";
            case "lotus_moon_garden" -> "jiangnan_waterlands";
            default -> "peach_blossom_valley";
        };
    }

    /** Aerial view from the (sx, sz) side, {@code distance} out and {@code height} up, looking at the site. */
    private static void look(Minecraft mc, double sx, double sz, double distance, double height) {
        Site s = site;
        double len = Math.hypot(sx, sz);
        double x = s.origin().getX() + 0.5 + sx / len * distance, z = s.origin().getZ() + 0.5 + sz / len * distance;
        lookAbs(mc, x, s.origin().getY() + height, z, s.origin().getX() + 0.5, s.origin().getY() + 2, s.origin().getZ() + 0.5);
    }

    private static void lookAbs(Minecraft mc, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        mc.getSingleplayerServer().execute(() -> {
            ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            player.setGameMode(GameType.SPECTATOR);
            player.teleportTo(mc.getSingleplayerServer().overworld(), x, y, z, yaw, pitch);
            MurimMistClient.settleNow();
        });
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(name));
        }
        LOGGER.info("Site tour capture: {}", name);
    }

    private static void fail(String message) {
        report.add("FAILED " + message);
        LOGGER.error("Site tour: {}", message);
        failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("site-tour-report.txt"), String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the site tour report", e);
        }
        LOGGER.info("Site tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
