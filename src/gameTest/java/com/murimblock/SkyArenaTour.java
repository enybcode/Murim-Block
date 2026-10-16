package com.murimblock;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only real-client look at the Bandit Warlord's sky arena, from its own schematic
 * (art/structures/warlord-sky-arena/warlord_sky_arena.schem): in a new flat world it is pasted block by block with its
 * floor's middle at (0, 250, 0), then seen from afar, from its rim, on its fighting floor, from below, by its ring of
 * lava, and at night. Run with the weapon tour's client: MURIM_SKY_ARENA=1 gradlew -PgameTests runWeaponTour. Shots go
 * to build/weapon-tour-run/screenshots/sky-arena, the report to weapon-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SkyArenaTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The fighting floor's middle: the schematic's paste point stands on it. */
    private static final int FX = 0, FY = 250, FZ = 0;
    /** Blocks set per tick while pasting. */
    private static final int PER_TICK = 120_000;
    private static boolean creating, finished, failed;
    private static int ticks;
    private static final List<String> report = new ArrayList<>();
    // The schematic, read once.
    private static int width, height, length, offX, offY, offZ, cursor = -1, placed;
    private static int[] cells;
    private static BlockState[] states;
    /** The tick the paste ended (0 until then). */
    private static volatile int pasted;

    private SkyArenaTour() { }

    /** Whether this run is the sky arena's, not the weapon tour's. */
    public static boolean wanted() {
        return "1".equals(System.getenv("MURIM_SKY_ARENA"));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.weaponTour") || !wanted()) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    String name = "murim-sky-arena-" + System.currentTimeMillis();
                    LevelSettings settings = new LevelSettings(name, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(7L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            step(mc, mc.getSingleplayerServer());
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Sky arena tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == 20) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.renderDistance().set(16);
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.setWeatherParameters(6000, 0, false, false);
                read(level);
                look(server, 0, 60, 0, 0, 0, 1);
            });
        }
        // The paste, spread over ticks, on the server.
        if (ticks > 22 && pasted == 0 && cells != null) server.execute(SkyArenaTour::pasteSome);
        if (pasted == 0) return;
        switch (ticks - pasted) {
            case 1 -> check(placed > 1_400_000, "the schematic was pasted: " + placed + " blocks");
            case 2 -> look(server, 140, 95, 150, 0, -25, 0);
            case 140 -> capture(mc, "01-from-afar.png");
            case 142 -> look(server, 52, 44, 58, -8, -2, -8);
            case 200 -> capture(mc, "02-from-the-rim.png");
            case 202 -> look(server, -22, 3.6, 26, 6, 3, -8);
            case 250 -> capture(mc, "03-on-the-fighting-floor.png");
            case 252 -> look(server, 38, 4, 10, 48, 0, -6);
            case 300 -> capture(mc, "04-the-ring-of-lava.png");
            case 302 -> look(server, 110, -150, 95, 0, -40, 0);
            case 380 -> capture(mc, "05-from-below.png");
            // At night, lit by its lava and braziers.
            case 382 -> {
                server.execute(() -> server.overworld().setDayTime(18000));
                look(server, 52, 44, 58, -8, -2, -8);
            }
            case 440 -> capture(mc, "06-at-night.png");
            case 446 -> finish(mc);
            default -> { }
        }
    }

    /** Reads the schematic (Sponge v3): its size, offset, palette and cells. */
    private static void read(ServerLevel level) {
        try {
            // From the run's directory (build/weapon-tour-run) up to the repository.
            Path file = null;
            for (Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath(); dir != null && file == null; dir = dir.getParent()) {
                Path candidate = dir.resolve("art/structures/warlord-sky-arena/warlord_sky_arena.schem");
                if (Files.isRegularFile(candidate)) file = candidate;
            }
            if (file == null) throw new IllegalStateException("warlord_sky_arena.schem not found above " + System.getProperty("user.dir"));
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            CompoundTag s = root.getCompound("Schematic");
            width = s.getShort("Width"); height = s.getShort("Height"); length = s.getShort("Length");
            int[] off = s.getIntArray("Offset");
            offX = off[0]; offY = off[1]; offZ = off[2];
            CompoundTag blocks = s.getCompound("Blocks"), palette = blocks.getCompound("Palette");
            states = new BlockState[palette.size()];
            for (String name : palette.getAllKeys())
                states[palette.getInt(name)] = BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), name, false).blockState();
            byte[] data = blocks.getByteArray("Data");
            cells = new int[width * height * length];
            for (int i = 0, k = 0; i < data.length; k++) {
                int v = 0, shift = 0, b;
                do { b = data[i++]; v |= (b & 0x7f) << shift; shift += 7; } while ((b & 0x80) != 0);
                cells[k] = v;
            }
            cursor = 0;
            check(true, "read " + file.getFileName() + ": " + width + "x" + height + "x" + length + ", " + states.length + " states");
        } catch (Exception e) {
            check(false, "could not read the schematic: " + e);
        }
    }

    private static void pasteSome() {
        if (cursor < 0 || pasted != 0) return;
        ServerLevel level = server().overworld();
        int end = Math.min(cells.length, cursor + PER_TICK * 4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int done = 0; cursor < end && done < PER_TICK; cursor++) {
            BlockState state = states[cells[cursor]];
            if (state.isAir()) continue;
            int x = cursor % width, z = (cursor / width) % length, y = cursor / (width * length);
            // The paste point stands on the floor: the schematic's offset is from the player's feet.
            pos.set(FX + offX + x, FY + offY + y, FZ + offZ + z);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            placed++;
            done++;
        }
        if (cursor >= cells.length) pasted = ticks;
    }

    private static MinecraftServer server() {
        return Minecraft.getInstance().getSingleplayerServer();
    }

    /** Puts the camera at (x, y, z) from the fighting floor's middle, looking at (tx, ty, tz) from there. */
    private static void look(MinecraftServer server, double x, double y, double z, double tx, double ty, double tz) {
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer self = server.getPlayerList().getPlayers().get(0);
            float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
            float pitch = (float) -Math.toDegrees(Math.atan2(ty - y, Math.hypot(tx - x, tz - z)));
            self.teleportTo(server.overworld(), FX + x, FY + y - self.getEyeHeight(), FZ + z, yaw, pitch);
        });
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Sky arena tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve("sky-arena").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("weapon-tour-report.txt"), String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Sky arena tour: could not write its report", e);
        }
        mc.stop();
    }
}
