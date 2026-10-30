package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiome;
import com.murimblock.world.MurimMap;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Dev tool, off unless {@code murimblock.mapTour} is set (run {@code gradlew runMapTour}): creates a new default
 * world, checks that the spawn is on Murim land, then visits every {@link MurimMap#SPOTS spot}, checks its biome and
 * takes a shot from above; logs PASS or FAIL for each, saves the shots to run/map-tour/screenshots, then quits.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MapTour {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.mapTour");
    private static final String WORLD = "murim-map-tour";
    private static final long SEED = 29102026L;
    private static final int WORLD_FRAMES = 200;
    /** Frames to let the chunks around a spot load and draw before the shot. */
    private static final int LOAD_FRAMES = 600;

    /** Relief views after the biome spots, in guide coordinates; no biome is checked there. */
    private static final List<MurimMap.Spot> VIEWS = List.of(
            new MurimMap.Spot("north wall", null, -2000, -2050),
            new MurimMap.Spot("north wall pass", null, -900, -2050),
            new MurimMap.Spot("Qinghai escarpment", null, -2000, -1200),
            new MurimMap.Spot("Sichuan gorges", null, -200, 1100),
            new MurimMap.Spot("yellow river", null, 30, -1250),
            new MurimMap.Spot("great river delta", null, 3300, 2050),
            new MurimMap.Spot("great lake", null, 1250, 2350),
            new MurimMap.Spot("Qinghai salt lake", null, -3500, -1100));

    private static boolean started;
    private static int frames = -1;
    private static int step;
    private static int failures;

    private MapTour() {
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.options.onboardAccessibility) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            } else if (mc.screen instanceof TitleScreen) {
                started = true;
                frames = -1;
                openWorld(mc);
            }
            return;
        }
        if (mc.player == null || mc.screen != null) {
            return;
        }
        frames++;
        if (step == 0) {
            if (frames >= WORLD_FRAMES) {
                mc.options.hideGui = true;
                // Clouds would hide the high ground.
                mc.options.cloudStatus().set(CloudStatus.OFF);
                // The pause menu would stop the tour whenever the window loses focus.
                mc.options.pauseOnLostFocus = false;
                server(mc, player -> {
                    ServerLevel level = player.serverLevel();
                    BlockPos spawn = level.getSharedSpawnPos();
                    MurimBiome there = MurimMap.biome(spawn.getX(), spawn.getZ());
                    check("spawn on Murim land (" + spawn.toShortString() + ", " + there + ")", there.ours());
                });
                step++;
                frames = 0;
            }
            return;
        }
        int stop = (step - 1) / 2;
        if (stop >= MurimMap.SPOTS.size() + VIEWS.size()) {
            Murimblock.LOGGER.info("Map tour: done, {} failure(s)", failures);
            mc.stop();
            return;
        }
        MurimMap.Spot s = stop < MurimMap.SPOTS.size() ? MurimMap.SPOTS.get(stop) : VIEWS.get(stop - MurimMap.SPOTS.size());
        double[] block = MurimMap.unwarp(s.x(), s.z());
        int x = (int) Math.round(block[0]);
        int z = (int) Math.round(block[1]);
        if (step % 2 == 1) {
            // Sit above the ground 100 blocks south of the spot, looking north at it.
            server(mc, player -> {
                // A spectator: the first-person body would hide the view.
                player.setGameMode(GameType.SPECTATOR);
                ServerLevel level = player.serverLevel();
                // From the terrain alone: a rock pillar or a tree under the camera would lift it out of the view.
                var generator = level.getChunkSource().getGenerator();
                var noise = level.getChunkSource().randomState();
                int ground = Math.max(generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, noise),
                        generator.getBaseHeight(x, z + 100, Heightmap.Types.OCEAN_FLOOR_WG, level, noise));
                player.connection.teleport(x + 0.5, ground + 45, z + 100.5, 180, 20);
            });
            step++;
            frames = 0;
        } else if (frames >= LOAD_FRAMES) {
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                // The middle of five points 8 blocks apart, under any water: neither a stray tree nor a crack between peaks.
                int[][] offsets = {{0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}};
                int[] grounds = new int[offsets.length];
                for (int i = 0; i < offsets.length; i++) {
                    grounds[i] = ground(level, x + offsets[i][0], z + offsets[i][1], Heightmap.Types.OCEAN_FLOOR);
                }
                Arrays.sort(grounds);
                int ground = grounds[2];
                MurimMap.Relief relief = MurimMap.relief(x, z);
                if (relief.weight() >= .7) {
                    check(s.name() + " ground Y " + ground + ", wanted " + Math.round(relief.height()),
                            Math.abs(ground - relief.height()) <= 20);
                } else {
                    Murimblock.LOGGER.info("Map tour: {} ground Y {} (wanted about {}, loosely)", s.name(), ground,
                            Math.round(relief.height()));
                }
                if (s.biome() != null) {
                    Holder<Biome> biome = level.getBiome(new BlockPos(x, ground, z));
                    String id = biome.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                    check(s.name() + " at " + x + ", " + z + " is " + s.biome().key + ", found " + id, id.equals(s.biome().key));
                }
            });
            shoot(mc, String.format("murim_map_%02d_%s.png", stop, s.name().replaceAll("[^A-Za-z]+", "_").toLowerCase()));
            step++;
            frames = 0;
        }
    }

    /** The ground height, generating the chunk first: a chunk not loaded yet reads as the bottom of the world. */
    private static int ground(ServerLevel level, int x, int z, Heightmap.Types type) {
        return level.getChunk(x >> 4, z >> 4).getHeight(type, x & 15, z & 15) + 1;
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
        }
        Murimblock.LOGGER.info("Map tour: {} {}", ok ? "PASS" : "FAIL", what);
    }

    private static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    private static void shoot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> Murimblock.LOGGER.info("Map tour shot: {}", message.getString()));
    }

    private static void openWorld(Minecraft mc) {
        // Always a new world: the map only applies where chunks are generated with it.
        if (mc.getLevelSource().levelExists(WORLD)) {
            try {
                mc.getLevelSource().createAccess(WORLD).deleteLevel();
            } catch (Exception e) {
                Murimblock.LOGGER.error("Map tour: could not delete the old {}", WORLD, e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(SEED, false, false),
                WorldPresets::createNormalWorldDimensions, mc.screen);
    }
}
