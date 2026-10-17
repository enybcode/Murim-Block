package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.client.FinishCinematic;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.ScarletBloom;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;

/**
 * Development-only real-client film of Scarlet Bloom, the Sanguine Art's finish (gradlew -PgameTests runScarletBloomTour):
 * first the executor's own film (the local player, a master of the art, executes a downed rival: the moving camera, the
 * manga frames), then the show as an onlooker sees it (a second master executes another rival while the local player
 * watches from aside). Frames go to build/scarlet-bloom-tour-run/screenshots/01-executor-film and 02-onlooker, the
 * report to scarlet-bloom-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ScarletBloomTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile ServerPlayer rival, master, second;
    private static double baseX, baseY, baseZ;
    private static final List<String> report = new ArrayList<>();
    /** The two films' starts (ticks of the tour). */
    private static final int FILM = 40, ONLOOK = 270, LENGTH = 160;

    private ScarletBloomTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.scarletBloomTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-scarlet-bloom", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-scarlet-bloom", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            server.execute(() -> {
                for (ServerPlayer dummy : new ServerPlayer[]{rival, master, second}) if (dummy != null && !dummy.isRemoved()) dummy.doTick();
            });
            step(mc, server);
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Scarlet Bloom tour crashed", e);
            finish(mc, mc.getSingleplayerServer());
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks >= FILM && ticks <= FILM + LENGTH && (ticks - FILM) % 2 == 0) capture(mc, String.format("01-executor-film/frame-%03d.png", (ticks - FILM) / 2));
        if (ticks >= ONLOOK && ticks <= ONLOOK + LENGTH && (ticks - ONLOOK) % 2 == 0) capture(mc, String.format("02-onlooker/frame-%03d.png", (ticks - ONLOOK) / 2));
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = false;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    server.setPvpAllowed(true);
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    ServerPlayer self = self(server);
                    baseX = Math.floor(self.getX()) + 0.5;
                    baseY = self.getY();
                    baseZ = Math.floor(self.getZ()) + 0.5;
                    master(self);
                    rival = dummy(server, level, "Rival");
                    rival.moveTo(baseX, baseY, baseZ + 2.4, 180, 0);
                    KnockdownService.knockDownFor(rival, 1200);
                    self.teleportTo(level, baseX, baseY, baseZ, 0, 20);
                });
            }
            case 30 -> server.execute(() -> check(ScarletBloom.finishes(self(server)), "a master of the Sanguine Art plays Scarlet Bloom"));
            case FILM -> server.execute(() -> check(KnockdownService.execute(self(server), rival), "the local player executes the downed rival"));
            case FILM + 30 -> check(FinishCinematic.active() && mc.getCameraEntity() != mc.player, "the executor sees the finish as a film");
            case FILM + 60 -> server.execute(() -> check(rival.getY() > baseY + ScarletBloom.RISE - 2, "the rival is thrown into the sky"));
            case FILM + ScarletBloom.HIT_TICK + 6 -> server.execute(() -> check(rival.isDeadOrDying(), "Scarlet Bloom kills the rival"));
            case FILM + LENGTH + 4 -> check(!FinishCinematic.active() && mc.getCameraEntity() == mc.player, "the film ends, the camera is back on the player");
            // 2. As an onlooker: another master executes another rival; the local player watches from aside.
            case ONLOOK - 30 -> server.execute(() -> {
                ServerLevel level = server.overworld();
                server.getPlayerList().remove(rival);
                second = dummy(server, level, "Second rival");
                second.moveTo(baseX + 10, baseY, baseZ + 2.4, 180, 0);
                KnockdownService.knockDownFor(second, 1200);
                master = dummy(server, level, "Master");
                master(master);
                master.moveTo(baseX + 10, baseY, baseZ, 0, 0);
                ServerPlayer self = self(server);
                self.setGameMode(GameType.SPECTATOR);
                // From the side, far enough to see the body thrown 9 blocks up.
                double x = baseX + 23, z = baseZ + 1.2, eye = 4.5, tx = baseX + 10, tz = baseZ + 1.2, target = 4.5;
                float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
                float pitch = (float) -Math.toDegrees(Math.atan2(target - eye, Math.hypot(tx - x, tz - z)));
                self.teleportTo(level, x, baseY + eye - self.getEyeHeight(), z, yaw, pitch);
            });
            case ONLOOK -> server.execute(() -> check(KnockdownService.execute(master, second), "a second master executes another rival"));
            case ONLOOK + ScarletBloom.HIT_TICK + 6 -> server.execute(() -> check(second.isDeadOrDying(), "the second rival is executed"));
            case ONLOOK + LENGTH + 10 -> finish(mc, server);
            default -> { }
        }
    }

    /** Makes a player a master of the Sanguine Art, its main art, bare-handed. */
    private static void master(ServerPlayer p) {
        com.murimblock.cultivation.CultivationService.setCultivation(p, MartialArts.SANGUINE_REALM, com.murimblock.cultivation.CultivationStage.EARLY);
        new ItemStack(MurimItems.SANGUINE_ART_SCROLL.get()).use(p.level(), p, InteractionHand.MAIN_HAND);
        MartialService.setMainArt(p, MartialArts.SANGUINE_ART);
        for (MartialArts.Technique t : MartialArts.art(MartialArts.SANGUINE_ART).techniques()) MartialService.unlock(p, t);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    /** A second player, on a mock connection: the server tracks it and sends it to the client like any other. */
    private static ServerPlayer dummy(MinecraftServer server, ServerLevel level, String name) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), name), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i <= 60; i++) player.tick();
        return player;
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Scarlet Bloom tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void finish(Minecraft mc, MinecraftServer server) {
        if (finished) return;
        finished = true;
        // The dummies leave first, or the shutdown waits for them forever.
        if (server != null) server.execute(() -> {
            for (ServerPlayer dummy : new ServerPlayer[]{rival, master, second}) if (dummy != null) server.getPlayerList().remove(dummy);
        });
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("scarlet-bloom-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the Scarlet Bloom tour report", e);
        }
        LOGGER.info("Scarlet Bloom tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.execute(mc::stop);
    }
}
