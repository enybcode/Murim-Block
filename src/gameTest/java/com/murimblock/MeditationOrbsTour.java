package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.MeditationOrbs;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.QiMoteEntity;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Opt-in real client look at the motes of Qi round a meditation (MeditationOrbs): a player sits down at dusk on a terrace
 * by water (the view turning round them while they sit, then through their own eyes); the motes form and drift round them
 * (and a golden one); then a mote is set under the crosshair and the gaze
 * draws it in, through the client's own aiming and the server's check (the report says whether it came); one more flies
 * in, filmed. Never included in the player JAR.
 * Run: gradlew -PgameTests runMeditationOrbsTour; pictures in build/meditation-orbs-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MeditationOrbsTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;
    private static int aimed = -1;
    private static double qiBefore;
    private static final List<String> report = new ArrayList<>();

    private MeditationOrbsTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.meditationOrbsTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("meditation-orbs", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("meditation-orbs-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            var server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            if (ticks == 20) server.execute(() -> {
                var level = server.overworld();
                level.setDayTime(12600);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                // A stone terrace, a pool before it, lanterns at its corners.
                for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                    boolean pool = z <= -3 && Math.abs(x) <= 4;
                    level.setBlockAndUpdate(new BlockPos(x, -61, z), pool ? Blocks.WATER.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
                }
                for (int[] c : new int[][]{{-5, 4}, {5, 4}, {-5, -1}, {5, -1}}) {
                    level.setBlockAndUpdate(new BlockPos(c[0], -60, c[1]), Blocks.STONE_BRICK_WALL.defaultBlockState());
                    level.setBlockAndUpdate(new BlockPos(c[0], -59, c[1]), Blocks.LANTERN.defaultBlockState());
                }
                ServerPlayer p = player(server);
                p.teleportTo(level, 0.5, -60, 0.5, 180, 0);
                // The Sanguine Art, for its aura's blood-red flames.
                com.murimblock.martial.MartialService.learn(p, com.murimblock.martial.MartialArts.art(com.murimblock.martial.MartialArts.SANGUINE_ART));
                com.murimblock.martial.MartialService.setMainArt(p, com.murimblock.martial.MartialArts.SANGUINE_ART);
                QiService.setQiMax(p, 200);
                QiService.refillQi(p);
            });
            if (ticks == 40) server.execute(() -> report.add("seated=" + MeditationService.start(player(server))));
            if (ticks == 70) capture(mc, "00-sitting-cinematic.png");
            if (ticks >= 52 && ticks < 98) capture(mc, String.format("gif/frame-%03d.png", ticks - 52));
            if (ticks == 90) capture(mc, "00b-sitting-cinematic.png");
            // Then through one's own eyes, looking about.
            if (ticks == 150) { mc.player.setYRot(150); mc.player.setXRot(18); }
            if (ticks == 180) capture(mc, "01-motes-gather.png");
            if (ticks == 200) { mc.player.setYRot(215); mc.player.setXRot(10); }
            if (ticks == 182) server.execute(() -> MeditationOrbs.spawn(player(server), true));
            if (ticks == 240) capture(mc, "02-golden-mote.png");
            // A mote set just under the crosshair, on the client's own aiming line: the gaze must draw it in.
            if (ticks == 242) {
                var view = mc.gameRenderer.getMainCamera();
                Vec3 at = view.getPosition().add(new Vec3(view.getLookVector()).scale(2.4)).subtract(0, 0.25, 0);
                server.execute(() -> {
                    ServerPlayer p = player(server);
                    QiService.setQi(p, 100);
                    qiBefore = QiService.getQi(p);
                    QiMoteEntity m = MeditationOrbs.spawn(p, false);
                    m.moveTo(at.x, at.y, at.z);
                    aimed = m.getId();
                });
            }
            if (ticks >= 246 && ticks <= 262 && ticks % 2 == 0) capture(mc, String.format("03-drawn-in-%02d.png", ticks - 246));
            if (ticks == 290) server.execute(() -> {
                ServerPlayer p = player(server);
                var e = server.overworld().getEntity(aimed);
                report.add("aimed mote came=" + (e == null || e.isRemoved() || e instanceof QiMoteEntity m && m.pulled()));
                report.add("reserve " + Math.round(qiBefore) + " -> " + Math.round(QiService.getQi(p)));
                report.add("motes waiting=" + MeditationOrbs.motes(p).size());
            });
            if (ticks == 300) {
                boolean passed = report.stream().anyMatch(s -> s.equals("aimed mote came=true"));
                Files.writeString(mc.gameDirectory.toPath().resolve("meditation-orbs-tour-report.txt"), (passed ? "PASSED" : "FAILED") + "; " + report + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Meditation orbs tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder.resolve(name).getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(name));
        }
    }
}
