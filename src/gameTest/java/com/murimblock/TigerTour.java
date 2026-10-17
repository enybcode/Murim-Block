package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.integration.epicfight.TigerEpicFight;
import com.murimblock.integration.epicfight.TigerPatch;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.TigerEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
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
import org.slf4j.Logger;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Development-only film of the tiger in a real client (gradlew -PgameTests runTigerTour): in a fresh flat world, the
 * tiger stands, walks, plays each of its five attacks through Epic Fight, and dies; a picture every other tick, to
 * build/tiger-tour-run/screenshots/&lt;part&gt;/frame-NNN.png (GIFs with scripts/animation/FramesToGif.java).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TigerTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int START = 60;
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile TigerEntity tiger;
    private static double sx, sy, sz;
    private static final List<String> report = new ArrayList<>();

    /** A part of the film: how long, what the tiger does at its start (server thread). */
    private record Part(String name, int length, Runnable start) { }

    private static Runnable play(Supplier<AnimationAccessor<? extends StaticAnimation>> animation) {
        return () -> {
            place();
            var patch = EpicFightCapabilities.getEntityPatch(tiger, TigerPatch.class);
            patch.playAnimationSynchronized(animation.get(), 0);
        };
    }

    private static final List<Part> PARTS = List.of(
            new Part("idle", 60, TigerTour::place),
            new Part("walk", 50, () -> { place(); tiger.setNoAi(false); tiger.getNavigation().moveTo(sx - 9, sy, sz + 2, 1.0); }),
            new Part("swipe", 24, play(() -> TigerEpicFight.SWIPE)),
            new Part("bite", 26, play(() -> TigerEpicFight.BITE)),
            new Part("double_swipe", 32, play(() -> TigerEpicFight.DOUBLE_SWIPE)),
            new Part("pounce", 40, () -> { play(() -> TigerEpicFight.POUNCE_ATTACK).run(); tiger.setNoAi(false); tiger.setNoGravity(false); }),
            new Part("maul", 44, play(() -> TigerEpicFight.MAUL)),
            new Part("death", 40, () -> { place(); tiger.setNoAi(false); tiger.hurt(tiger.damageSources().generic(), 1000); }));

    private TigerTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.tigerTour")) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        yesman.epicfight.config.ClientConfig.showTargetIndicator = false;
        yesman.epicfight.config.ClientConfig.enableTargetEntityGuide = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    LevelSettings settings = new LevelSettings("murim-tiger", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-tiger", settings, new WorldOptions(1L, false, false),
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
            report.add("FAILED tour crashed: " + e);
            failed = true;
            LOGGER.error("Tiger tour crashed", e);
            finish(mc);
        }
    }

    private static void step(Minecraft mc, MinecraftServer server) throws Exception {
        if (ticks == 20) {
            mc.options.hideGui = true;
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer self = self(server);
                sx = Math.floor(self.getX()) + 0.5;
                sy = self.getY();
                sz = Math.floor(self.getZ()) + 6.5;
                self.getAbilities().flying = true;
                self.getAbilities().invulnerable = true;
                self.onUpdateAbilities();
                tiger = MurimEntities.TIGER.get().spawn(level, BlockPos.containing(sx, sy, sz), MobSpawnType.COMMAND);
                check(tiger != null, "the tiger stands in the world");
                if (tiger != null) tiger.setPersistenceRequired();
            });
            return;
        }
        if (ticks < START || tiger == null) return;
        int at = ticks - START, n = 0;
        while (n < PARTS.size() && at >= PARTS.get(n).length()) { at -= PARTS.get(n).length(); n++; }
        if (n >= PARTS.size()) { finish(mc); return; }
        Part part = PARTS.get(n);
        final int t = at;
        server.execute(() -> {
            if (t == 0) part.start().run();
            // The camera three-quarters ahead of the tiger, following it.
            double tx = tiger.getX(), ty = tiger.getY(), tz = tiger.getZ();
            look(self(server), tx - 5.2, ty + 3.2, tz - 5.6, tx, ty + 1.0, tz + 0.4);
        });
        if (t % 2 == 1) shot(mc, part.name() + "/frame-" + String.format("%03d", t / 2));
        if (t == part.length() - 1) check(true, part.name() + " filmed");
    }

    /** Back to its mark, frozen, facing north (towards the camera's side). */
    private static void place() {
        tiger.setNoAi(true);
        tiger.setNoGravity(false);
        tiger.setDeltaMovement(0, 0, 0);
        tiger.moveTo(sx, sy, sz, 180, 0);
        tiger.setYBodyRot(180);
        tiger.setYHeadRot(180);
        tiger.yRotO = 180;
        tiger.yHeadRotO = 180;
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void look(ServerPlayer self, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        self.teleportTo(self.serverLevel(), ex, ey - self.getEyeHeight(), ez, yaw, pitch);
    }

    private static void shot(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name + ".png");
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Tiger tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("tiger-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the tiger tour report", e);
        }
        LOGGER.info("Tiger tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
