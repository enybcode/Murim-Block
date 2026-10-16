package com.murimblock;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.item.MurimItems;
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
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Development-only real-client film of the Traveller's Staff Art's animations (gradlew -PgameTests runStaffTour): a
 * second player (on a mock connection) holds the Traveller's Staff and plays the Vertical Strike, the Flurry of Points and
 * the Spinning Rampart (start, three turns of the loop held, end), each filmed from the side and from the front. The world
 * runs at 5 ticks a second and every rendered frame is kept, a true slow motion: frames go to
 * build/staff-tour-run/screenshots/<move>-<view>/t-<animation ms>.png, the report to staff-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class StaffTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile ServerPlayer actor;
    private static double baseX, baseY, baseZ;
    private static final List<String> report = new ArrayList<>();
    private static volatile Take filming, lastTake;
    private static int lastMs = -1;

    /** One take: a move, a view, from the tick it starts, how long it is filmed (ticks). */
    private record Take(String move, String view, int start, int length) { }
    private static final List<Take> TAKES = new ArrayList<>();
    static {
        int t = 60;
        for (String move : new String[] {"strike", "flurry", "rampart"}) for (String view : new String[] {"side", "front"}) {
            int length = move.equals("rampart") ? 40 : move.equals("flurry") ? 30 : 26;
            TAKES.add(new Take(move, view, t, length));
            t += length + 24;
        }
    }

    private StaffTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.staffTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-staff", GameType.SPECTATOR, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-staff", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                }
                return;
            }
            ticks++;
            MinecraftServer server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            server.execute(() -> { if (actor != null && !actor.isRemoved()) actor.doTick(); });
            step(mc, server);
        } catch (Exception e) {
            check(false, "tour crashed: " + e);
            LOGGER.error("Staff tour crashed", e);
            finish(mc, mc.getSingleplayerServer());
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == 20) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                // Slow motion (the client follows the server's tick rate): every tick's capture is then a fresh frame.
                server.tickRateManager().setTickRate(5F);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                ServerPlayer self = server.getPlayerList().getPlayers().get(0);
                baseX = Math.floor(self.getX()) + 0.5;
                baseY = Math.floor(self.getY());
                baseZ = Math.floor(self.getZ()) + 0.5;
                actor = dummy(server, level, "Traveller");
                actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.TRAVELLER_STAFF.get()));
                // Facing south (+z).
                actor.teleportTo(level, baseX, baseY, baseZ, 0, 0);
            });
        }
        for (Take take : TAKES) {
            int at = ticks - take.start;
            if (at == -16) view(server, take.view);
            if (at == 0) play(mc, take.move);
            if (take.move.equals("rampart")) {
                // The start (3 ticks), then the loop held for three turns (24 ticks), then the end.
                // Held: the loop is a living motion on the client, as long as the player is marked spinning.
                if (at == 3) com.murimblock.client.StaffClient.SPINNING.add(actor.getId());
                if (at == 27) com.murimblock.client.StaffClient.SPINNING.remove(actor.getId());
                if (at == 27) play(mc, "rampart_end");
            }
            if (at == 0) filming = take;
            if (at == take.length + 1 && filming == take) filming = null;
        }
        Take last = TAKES.get(TAKES.size() - 1);
        if (ticks == last.start + last.length + 6) {
            check(true, "every take filmed");
            finish(mc, server);
        }
    }

    /** Played on the client's own copy of the actor (no network delay: the frames are then exactly 0.05 s apart). */
    /** Every rendered frame of a take, named by its time in the take (milliseconds of animation, the ticks being 50 ms
     *  however slow the world runs), so the film is a true slow motion rather than one frame a tick. */
    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        Take take = filming;
        if (take == null || finished) return;
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int ms = Math.round((ticks - take.start + partial) * 50);
        if (ms < 0 || ms <= lastMs && take == lastTake) return;
        lastMs = ms;
        lastTake = take;
        try {
            capture(mc, String.format("%s-%s/t-%05d.png", take.move, take.view, ms));
        } catch (Exception e) {
            LOGGER.error("Staff tour capture failed", e);
        }
    }

    private static void play(Minecraft mc, String move) {
        var seen = mc.level == null ? null : mc.level.getEntity(actor.getId());
        PlayerPatch<?> patch = seen == null ? null : EpicFightCapabilities.getEntityPatch(seen, PlayerPatch.class);
        if (patch == null) { check(false, "no Epic Fight patch on the actor"); return; }
        var animation = switch (move) {
            case "strike" -> MurimAnimations.STAFF_VERTICAL_STRIKE;
            case "flurry" -> MurimAnimations.STAFF_POINT_FLURRY;
            case "rampart" -> MurimAnimations.STAFF_RAMPART_START;
            default -> MurimAnimations.STAFF_RAMPART_END;
        };
        patch.getAnimator().playAnimation(animation, 0.0F);
    }

    /** The camera (the spectating local player): beside the actor (his right side) or before him, at chest height. */
    private static void view(MinecraftServer server, String view) {
        server.execute(() -> {
            ServerPlayer self = server.getPlayerList().getPlayers().get(0);
            double x = baseX, z = baseZ, eye = 1.35;
            if (view.equals("side")) x = baseX - 2.7; else z = baseZ + 3.1;
            double tx = baseX, tz = baseZ + 0.5, target = 1.2;
            float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
            float pitch = (float) -Math.toDegrees(Math.atan2(target - eye, Math.hypot(tx - x, tz - z)));
            self.teleportTo(server.overworld(), x, baseY + eye - self.getEyeHeight(), z, yaw, pitch);
        });
    }

    private static ServerPlayer dummy(MinecraftServer server, ServerLevel level, String name) {
        var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), name), 0, ClientInformation.createDefault(), false, ConnectionType.NEOFORGE);
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
        LOGGER.info("Staff tour: {} {}", ok ? "ok" : "FAILED", what);
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
        if (server != null) server.execute(() -> { if (actor != null) server.getPlayerList().remove(actor); });
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("staff-tour-report.txt"), String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the staff tour report", e);
        }
        mc.execute(mc::stop);
    }
}
