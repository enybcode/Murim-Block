package com.murimblock;

import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.InnerIsland;
import com.murimblock.cultivation.inner.InnerWorldService;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opt-in graphical check of the martial arts' inner worlds (docs/cultivation/inner-worlds.md) in a real client: each
 * art's island built in the inner world at Core Formation, the art made the main style (its sky), seen from the south.
 * Also checks that a block of the island cannot be mined. Never in the player JAR. Run: gradlew -PgameTests runInnerWorldsTour; pictures in build/inner-worlds-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldsTour {
    private static final List<ResourceLocation> ARTS = List.of(MartialArts.AZURE_CLOUD_SWORD, MartialArts.LONG_FIST, MartialArts.GOLDEN_BELL,
            MartialArts.CELESTIAL_PEAK_SPEAR, MartialArts.TRAVELLER_STAFF, MartialArts.THUNDER_SABER, MartialArts.SEVEN_STARS_SWORD,
            MartialArts.MOUNTAIN_SHATTERING_FIST, MartialArts.OVERLORD_SPEAR, MartialArts.SANGUINE_ART, MartialArts.DEVOURING_DEMON, MartialArts.MAJIN_FIST,
            MartialArts.CRESCENT_MOON_HALBERD);
    private static final int REALM = 6, START = 160, EACH = 90;
    private static boolean creating, finished;
    private static int ticks;

    private static BlockPos centre(int i) {
        return new BlockPos(20000 + i * 400, InnerIsland.GROUND, 20000);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.innerWorldsTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Inner", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("inner-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            MinecraftServer server = mc.getSingleplayerServer();
            if (ticks == 15) server.execute(() -> {
                ServerPlayer p = player(server);
                for (ResourceLocation art : ARTS) {
                    var a = MartialArts.art(art);
                    if (a != null) MartialService.learn(p, a);
                }
                QiService.setQiMax(p, 400);
                QiService.setQi(p, 400);
                ServerLevel inner = server.getLevel(InnerWorldService.INNER_WORLD);
                for (int i = 0; i < ARTS.size(); i++)
                    InnerIsland.build(inner, centre(i), new InnerIsland.Look(REALM, Optional.of(ARTS.get(i)),
                            List.of(ARTS.get(i), MartialArts.LONG_FIST, MartialArts.GOLDEN_BELL), 0), 0);
            });
            if (ticks == 20) mc.options.hideGui = true;
            // A first look at the first island, so that the inner world is loaded before the pictures.
            if (ticks == 60) server.execute(() -> {
                ServerPlayer p = player(server);
                p.setGameMode(GameType.SPECTATOR);
                visit(p);
                BlockPos c = centre(0);
                p.teleportTo(server.getLevel(InnerWorldService.INNER_WORLD), c.getX() + 0.5, c.getY() + 15, c.getZ() + 36.5, 180, 18);
            });
            // Nothing is mined in the inner world, even in creative mode (InnerWorldGuard).
            if (ticks == 120) server.execute(() -> {
                ServerPlayer p = player(server);
                p.setGameMode(GameType.CREATIVE);
                BlockPos ground = centre(0).offset(4, 0, 4);
                ServerLevel inner = server.getLevel(InnerWorldService.INNER_WORLD);
                var before = inner.getBlockState(ground);
                p.gameMode.destroyBlock(ground);
                boolean kept = !before.isAir() && inner.getBlockState(ground).equals(before);
                com.mojang.logging.LogUtils.getLogger().info("Inner worlds tour: a block mined in the inner world {}", kept ? "stayed (PASSED)" : "broke (FAILED)");
                p.setGameMode(GameType.SPECTATOR);
            });
            int i = (ticks - START) / EACH, phase = (ticks - START) % EACH;
            if (ticks >= START && i < ARTS.size()) {
                if (phase == 0) server.execute(() -> {
                    ServerPlayer p = player(server);
                    MartialService.setMainArt(p, ARTS.get(i));
                    MeditationService.refreshAura(p);
                    p.setGameMode(GameType.SPECTATOR);
                    visit(p);
                    BlockPos c = centre(i);
                    p.teleportTo(server.getLevel(InnerWorldService.INNER_WORLD), c.getX() + 0.5, c.getY() + 15, c.getZ() + 36.5, 180, 18);
                });
                if (phase == EACH - 5) capture(mc, String.format("%02d-%s.png", i + 1, ARTS.get(i).getPath()));
            }
            if (ticks == START + ARTS.size() * EACH + 5) {
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Inner worlds tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    /** A visit to the inner world, so that it does not send the visitor home (InnerWorldService's session, set by reflection). */
    private static void visit(ServerPlayer p) {
        try {
            var set = InnerWorldService.class.getDeclaredMethod("setSession", ServerPlayer.class, com.murimblock.cultivation.inner.InnerSession.class);
            set.setAccessible(true);
            set.invoke(null, p, new com.murimblock.cultivation.inner.InnerSession(true, net.minecraft.world.level.Level.OVERWORLD.location(), 0, -60, 0, 0, 0,
                    Optional.empty(), com.murimblock.cultivation.inner.InnerSession.VISIT, 0));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(path.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> { try (image) { image.writeToFile(path); } catch (Exception e) { throw new RuntimeException(e); } });
    }
}
