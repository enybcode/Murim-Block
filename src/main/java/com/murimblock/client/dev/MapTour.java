package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.world.MurimBiome;
import com.murimblock.world.MurimMap;
import java.util.UUID;
import java.util.function.Consumer;
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
    private static final int LOAD_FRAMES = 300;

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
        int spot = (step - 1) / 2;
        if (spot >= MurimMap.SPOTS.size()) {
            Murimblock.LOGGER.info("Map tour: done, {} failure(s)", failures);
            mc.stop();
            return;
        }
        MurimMap.Spot s = MurimMap.SPOTS.get(spot);
        double[] block = MurimMap.unwarp(s.x(), s.z());
        int x = (int) Math.round(block[0]);
        int z = (int) Math.round(block[1]);
        if (step % 2 == 1) {
            // Go there, high up, then sit 40 blocks above the ground, south of the spot, looking north at it.
            server(mc, player -> {
                // A spectator: the first-person body would hide the view.
                player.setGameMode(GameType.SPECTATOR);
                int ground = Math.max(ground(player.serverLevel(), x, z), ground(player.serverLevel(), x, z + 60));
                player.connection.teleport(x + 0.5, ground + 30, z + 60.5, 180, 25);
            });
            step++;
            frames = 0;
        } else if (frames >= LOAD_FRAMES) {
            server(mc, player -> {
                ServerLevel level = player.serverLevel();
                int ground = ground(level, x, z);
                Holder<Biome> biome = level.getBiome(new BlockPos(x, ground, z));
                String id = biome.unwrapKey().map(k -> k.location().getPath()).orElse("?");
                check(s.name() + " at " + x + ", " + z + " (Y " + ground + ") is " + s.biome().key + ", found " + id,
                        id.equals(s.biome().key));
            });
            shoot(mc, String.format("murim_map_%02d_%s.png", spot, s.biome().key));
            step++;
            frames = 0;
        }
    }

    /** The ground height, generating the chunk first: a chunk not loaded yet reads as the bottom of the world. */
    private static int ground(ServerLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;
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
