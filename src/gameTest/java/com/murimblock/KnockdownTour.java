package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.client.FinishCinematic;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.qi.QiService;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Development-only real-client check of the knock-down (gradlew -PgameTests runKnockdownTour). In a fresh flat world,
 * two server players on mock connections (seen by the client like any others) take part: the opponent is knocked down
 * by a real lethal hit from the local player and crawls; the local player carries them (standing, walking) and puts
 * them down; then the ally carries them, walks, puts them down and executes them while the local player films. At each
 * step the client must show the matching Murim animation through Epic Fight. Screenshots and frame sequences go to
 * build/knockdown-tour-run/screenshots, the report to knockdown-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KnockdownTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    /** The player knocked down, and a second one who carries and executes them; the local player is mostly the camera. */
    private static volatile ServerPlayer opponent, ally, rival;
    /** Where the dummy crawls along; the scene is laid out around it. */
    private static double baseX, baseY, baseZ;
    private static final List<String> report = new ArrayList<>();

    private KnockdownTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.knockdownTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    GameRules rules = new GameRules();
                    LevelSettings settings = new LevelSettings("murim-knockdown", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            rules, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-knockdown", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            // No tutorial or advancement toasts over the pictures.
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            // The dummies have no client of their own: the server ticks them like connected players.
            server.execute(() -> {
                for (ServerPlayer dummy : new ServerPlayer[]{opponent, ally, rival}) if (dummy != null && !dummy.isRemoved()) dummy.doTick();
            });
            step(mc, server);
        } catch (Exception e) {
            fail("tour crashed: " + e);
            LOGGER.error("Knockdown tour crashed", e);
            finish(mc);
        }
    }

    /** Captures one frame of a sequence (for a GIF) every {@code every} ticks between two ticks. */
    private static void record(Minecraft mc, String name, int from, int to, int every) throws Exception {
        if (ticks >= from && ticks <= to && (ticks - from) % every == 0) capture(mc, String.format("%s/frame-%03d.png", name, (ticks - from) / every));
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        // Frame sequences, one per feature (scripts/animation/FramesToGif.java turns them into GIFs).
        record(mc, "01-knockdown", 32, 78, 2);
        record(mc, "02-crawl", 82, 150, 2);
        record(mc, "03-prompt-and-pickup", 158, 182, 2);
        record(mc, "04-carry-pickup", 210, 232, 2);
        record(mc, "05-carry-front", 236, 252, 2);
        record(mc, "06-carry-walk", 256, 316, 2);
        record(mc, "07-put-down", 320, 338, 2);
        record(mc, "08-finish", 346, 392, 1);
        record(mc, "10-get-up", 402, 470, 2);
        record(mc, "11-executor-film", 480, 528, 1);
        record(mc, "12-you-are-down", 540, 558, 2);
        record(mc, "13-victim-film", 560, 612, 1);
        if (ticks > 84 && ticks <= 150) server.execute(() -> opponent.moveTo(opponent.getX() + 0.02, opponent.getY(), opponent.getZ(), -90, 0));
        if (ticks > 258 && ticks <= 316) server.execute(() -> ally.moveTo(ally.getX() + 0.07, ally.getY(), ally.getZ(), -90, 0));
        switch (ticks) {
            case 20 -> {
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    server.setPvpAllowed(true);
                    level.setDayTime(6000);
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                    ServerPlayer self = self(server);
                    baseX = Math.floor(self.getX()) + 0.5;
                    baseY = self.getY();
                    baseZ = Math.floor(self.getZ()) + 4.5;
                    opponent = spawnDummy(server, level, "Opponent");
                    opponent.moveTo(baseX, baseY, baseZ, -90, 0);
                    ally = spawnDummy(server, level, "Ally");
                    ally.moveTo(baseX - 6, baseY, baseZ, -90, 0);
                    ally.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                    QiService.setQiMax(self, 1_000);
                    QiService.setQi(self, 1_000);
                });
            }
            // 1. A real lethal hit from the local player: the opponent falls K.O. instead of dying.
            case 30 -> lookAt(server, baseX + 0.4, baseZ - 3.2, 1.3, baseX + 0.4, 0.7, baseZ);
            case 40 -> server.execute(() -> {
                opponent.hurt(opponent.damageSources().playerAttack(self(server)), 1000);
                check(opponent.isAlive() && KnockdownService.isDowned(opponent), "lethal PvP hit knocks down (server)");
            });
            case 76 -> {
                Player seen = seen(mc, opponent);
                check(seen != null && KnockdownService.isDowned(seen), "the client sees the opponent downed (synced)");
                checkMotion(seen, Motions.DOWNED, "downed, lying still");
                lookAt(server, baseX + 0.9, baseZ - 3.0, 1.0, baseX + 0.9, 0.3, baseZ);
            }
            case 80 -> capture(mc, "01-downed.png");
            // 2. Crawling.
            case 120 -> { checkMotion(seen(mc, opponent), Motions.CRAWL, "downed, crawling"); capture(mc, "02-crawl.png"); }
            // 3. The local player next to the downed opponent: the prompt, then carrying (standing, walking), putting down.
            case 155 -> server.execute(() -> {
                ServerPlayer self = self(server);
                self.setGameMode(GameType.SURVIVAL);
                self.teleportTo(server.overworld(), opponent.getX() - 1.6, baseY, opponent.getZ(), -90, 35);
            });
            case 157 -> mc.options.hideGui = false;
            case 165 -> capture(mc, "03-prompt.png");
            case 168 -> server.execute(() -> check(KnockdownService.carry(self(server), opponent), "the local player can carry the downed opponent"));
            case 183 -> {
                mc.options.hideGui = true;
                checkMotion(mc.player, Motions.CARRY, "local player carrying, standing");
                checkMotion(seen(mc, opponent), Motions.CARRIED, "opponent carried by the local player");
                mc.options.keyUp.setDown(true);
            }
            case 196 -> checkMotion(mc.player, Motions.CARRY_WALK, "local player carrying, walking");
            case 199 -> mc.options.keyUp.setDown(false);
            case 202 -> server.execute(() -> check(KnockdownService.release(self(server)) && opponent.getVehicle() == null
                    && KnockdownService.isDowned(opponent), "the local player puts the opponent down"));
            // 4-7. The ally picks the opponent up, carries them (front, walking) and puts them down; the local player films.
            case 205 -> server.execute(() -> {
                ally.moveTo(opponent.getX() - 1.2, baseY, opponent.getZ(), -90, 0);
                self(server).setGameMode(GameType.SPECTATOR);
            });
            case 207 -> lookAt(server, opponent.getX() - 0.5, opponent.getZ() - 3.2, 1.4, opponent.getX() - 0.5, 0.8, opponent.getZ());
            case 216 -> server.execute(() -> check(KnockdownService.carry(ally, opponent), "the ally can carry the downed opponent"));
            case 230 -> {
                checkMotion(seen(mc, ally), Motions.CARRY, "ally carrying, standing (seen by another client)");
                checkMotion(seen(mc, opponent), Motions.CARRIED, "opponent carried by the ally");
                capture(mc, "04-carry-side.png");
            }
            case 234 -> lookAt(server, ally.getX() + 3.0, ally.getZ() - 0.5, 1.5, ally.getX(), 1.1, ally.getZ());
            case 250 -> capture(mc, "05-carry-front.png");
            case 254 -> lookAt(server, ally.getX() + 2.0, ally.getZ() - 4.0, 1.4, ally.getX() + 2.0, 1.0, ally.getZ());
            case 285 -> { checkMotion(seen(mc, ally), Motions.CARRY_WALK, "ally carrying, walking"); capture(mc, "06-carry-walk.png"); }
            case 318 -> lookAt(server, ally.getX() + 0.8, ally.getZ() - 3.0, 1.3, ally.getX() + 0.8, 0.7, ally.getZ());
            case 326 -> server.execute(() -> check(KnockdownService.release(ally), "the ally puts the opponent down"));
            case 334 -> {
                Player c = seen(mc, ally), d = seen(mc, opponent);
                check(d != null && d.getVehicle() == null && c.getPassengers().isEmpty(), "the client sees the opponent put down");
            }
            // 8. The finish.
            case 340 -> server.execute(() -> ally.moveTo(opponent.getX() - 1.4, baseY, opponent.getZ(), -90, 0));
            case 342 -> lookAt(server, opponent.getX() - 0.6, opponent.getZ() - 3.2, 1.1, opponent.getX() - 0.6, 0.7, opponent.getZ());
            case 348 -> server.execute(() -> check(KnockdownService.execute(ally, opponent), "the ally can execute the downed opponent"));
            case 359 -> capture(mc, "08-finish-raise.png");
            case 366 -> capture(mc, "08-finish-strike.png");
            case 380 -> server.execute(() -> check(opponent.isDeadOrDying(), "the finish kills the opponent"));
            // 9. The chat after the finish.
            case 392 -> mc.options.hideGui = false;
            case 396 -> capture(mc, "09-execution-chat.png");
            // 10. Getting up when the time is over (the ally, knocked down for 2.5 s).
            case 398 -> {
                mc.options.hideGui = true;
                server.execute(() -> {
                    ally.moveTo(baseX, baseY, baseZ + 3, -90, 0);
                    KnockdownService.knockDownFor(ally, 50);
                });
            }
            case 400 -> lookAt(server, baseX + 0.5, baseZ + 0.2, 1.2, baseX + 0.5, 0.6, baseZ + 3);
            case 470 -> server.execute(() -> check(!KnockdownService.isDowned(ally) && ally.getForcedPose() == null, "the ally gets up when the time is over"));
            // 11. The executor's film: the local player executes a downed rival.
            case 474 -> server.execute(() -> {
                ServerPlayer self = self(server);
                self.setGameMode(GameType.SURVIVAL);
                rival = spawnDummy(server, server.overworld(), "Rival");
                rival.moveTo(baseX + 3, baseY, baseZ - 3, 90, 0);
                KnockdownService.knockDownFor(rival, 600);
                self.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
                self.teleportTo(server.overworld(), baseX + 1.6, baseY, baseZ - 3, -90, 30);
            });
            case 482 -> server.execute(() -> check(KnockdownService.execute(self(server), rival), "the local player can execute the downed rival"));
            case 495 -> check(FinishCinematic.active() && mc.getCameraEntity() != mc.player, "the executor sees the finish as a film (camera off the player)");
            case 528 -> {
                check(!FinishCinematic.active() && mc.getCameraEntity() == mc.player, "the executor's film ends, the camera is back on the player");
                server.execute(() -> check(rival.isDeadOrDying(), "the local player's finish kills the rival"));
            }
            // 12-13. The local player is knocked down by the ally, then executed: the victim's film, then the death screen.
            case 532 -> server.execute(() -> {
                ServerPlayer self = self(server);
                self.getInventory().add(new ItemStack(Items.DIAMOND, 5));
                self.teleportTo(server.overworld(), ally.getX() + 1.4, baseY, ally.getZ(), 90, 0);
                ally.moveTo(ally.getX(), baseY, ally.getZ(), -90, 0);
            });
            case 538 -> {
                mc.options.hideGui = false;
                server.execute(() -> {
                    ServerPlayer self = self(server);
                    self.hurt(self.damageSources().playerAttack(ally), 1000);
                    check(self.isAlive() && KnockdownService.isDowned(self), "the local player is knocked down by the ally");
                });
            }
            case 554 -> capture(mc, "12-you-are-down.png");
            // The hit threw the local player back: the ally walks up to them.
            case 558 -> server.execute(() -> { ServerPlayer self = self(server); ally.moveTo(self.getX() - 1.3, self.getY(), self.getZ(), -90, 0); });
            case 562 -> server.execute(() -> check(KnockdownService.execute(ally, self(server)), "the ally can execute the local player"));
            case 590 -> check(FinishCinematic.active() && !(mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen),
                    "the victim sees the finish as a film, the death screen waits");
            case 612 -> {
                check(!FinishCinematic.active() && mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen, "after the victim's film, the death screen");
                capture(mc, "14-death-screen.png");
                check(QiService.getQiMax(mc.player) == 900, "the local player lost 10% of their maximum Qi: " + QiService.getQiMax(mc.player));
            }
            case 620 -> {
                server.execute(() -> {
                    for (ServerPlayer dummy : new ServerPlayer[]{opponent, ally, rival}) if (dummy != null) server.getPlayerList().remove(dummy);
                });
                finish(mc);
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    /** A second player, on a mock connection: the server tracks it and sends it to the client like any other. */
    private static ServerPlayer spawnDummy(MinecraftServer server, ServerLevel level, String name) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), name), 0,
                ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        // Past the login protection, so the hit counts.
        for (int i = 0; i <= 60; i++) player.tick();
        return player;
    }

    /** The client's copy of a server player. */
    private static Player seen(Minecraft mc, ServerPlayer player) {
        return player == null || mc.level == null ? null : mc.level.getPlayerByUUID(player.getUUID());
    }

    /** The client's Epic Fight patch of this player must be in the motion, playing its Murim animation. */
    private static void checkMotion(Player player, Motions expected, String what) {
        PlayerPatch<?> patch = player == null ? null : EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        LivingMotion motion = patch == null ? null : patch.currentLivingMotion;
        var playing = patch == null ? null : patch.getClientAnimator().baseLayer.animationPlayer.getRealAnimation();
        var wanted = switch (expected) {
            case DOWNED -> MurimAnimations.DOWNED_IDLE;
            case CRAWL -> MurimAnimations.DOWNED_CRAWL;
            case CARRY -> MurimAnimations.CARRY_IDLE;
            case CARRY_WALK -> MurimAnimations.CARRY_WALK;
            case CARRIED -> MurimAnimations.CARRIED;
            case FIST_GUARD -> MurimAnimations.LONG_FIST_GUARD;
        };
        check(motion == expected && playing != null && playing.registryName().equals(wanted.registryName()),
                what + ": motion " + motion + ", playing " + (playing == null ? null : playing.registryName()));
    }

    /**
     * Puts the camera (the local player, as a spectator so it can stand anywhere) with its eye {@code eye} blocks above
     * the ground at (x, z), looking at a point {@code target} blocks above the ground at (tx, tz).
     */
    private static void lookAt(MinecraftServer server, double x, double z, double eye, double tx, double target, double tz) {
        double dx = tx - x, dy = target - eye, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        server.execute(() -> {
            ServerPlayer self = self(server);
            self.setGameMode(GameType.SPECTATOR);
            self.teleportTo(server.overworld(), x, baseY + eye - self.getEyeHeight(), z, yaw, pitch);
        });
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Knockdown tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void fail(String message) {
        report.add("FAILED " + message);
        failed = true;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("knockdown-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the knockdown tour report", e);
        }
        LOGGER.info("Knockdown tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
