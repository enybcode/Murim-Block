package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.mob.MurimEntities;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only mugshots of every Murim mob for the wiki (gradlew -PgameTests runMugshotTour): in a fresh flat world,
 * each mob in turn stands still before a police line-up wall, its height marked in bands, facing the camera, which is
 * framed on its head; one picture from the front and one from three-quarters. Pictures go to
 * build/mugshot-tour-run/screenshots/&lt;mob&gt;-front.png and -side.png, the report to mugshot-tour-report.txt.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MugshotTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int START = 40, EACH = 60;
    private static boolean creating, finished, failed;
    private static int ticks;
    private static volatile Mob sitter;
    private static double sx, sy, sz;
    private static final List<String> report = new ArrayList<>();

    private record Sitter(String name, Supplier<EntityType<? extends Mob>> type) { }

    private static final List<Sitter> SITTERS = List.of(
            new Sitter("azure-sensei", MurimEntities.AZURE_SENSEI::get),
            new Sitter("mountain-bandit", MurimEntities.MOUNTAIN_BANDIT::get),
            new Sitter("bandit-chief", MurimEntities.BANDIT_CHIEF::get),
            new Sitter("crab-man", MurimEntities.CRAB_MAN::get),
            new Sitter("armored-crab", MurimEntities.ARMORED_CRAB::get),
            new Sitter("training-opponent", MurimEntities.TRAINING_OPPONENT::get),
            new Sitter("heart-demon", MurimEntities.HEART_DEMON::get));

    private MugshotTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.mugshotTour")) return;
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
                    LevelSettings settings = new LevelSettings("murim-mugshots", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-mugshots", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Mugshot tour crashed", e);
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
                sz = Math.floor(self.getZ()) + 0.5;
                // The line-up wall behind the sitter (south of it): white and pale grey bands, one per block of height.
                BlockPos wall = BlockPos.containing(sx, sy, sz + 1.5);
                for (int dx = -6; dx <= 6; dx++) for (int dy = 0; dy < 6; dy++) {
                    level.setBlockAndUpdate(wall.offset(dx, dy, 0), (dy % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE).defaultBlockState());
                }
                self.getAbilities().flying = true;
                self.getAbilities().invulnerable = true;
                self.onUpdateAbilities();
            });
            return;
        }
        int n = (ticks - START) / EACH, t = (ticks - START) % EACH;
        if (ticks < START) return;
        if (n >= SITTERS.size()) {
            if (t == 0) finish(mc);
            return;
        }
        Sitter s = SITTERS.get(n);
        if (t == 0) server.execute(() -> {
            ServerLevel level = server.overworld();
            sitter = s.type().get().spawn(level, BlockPos.containing(sx, sy, sz), MobSpawnType.COMMAND);
            check(sitter != null, s.name() + " stands in the line-up");
            if (sitter == null) return;
            sitter.setNoAi(true);
            sitter.setPersistenceRequired();
            sitter.moveTo(sx, sy, sz, 180, 0);
        });
        if (sitter == null) return;
        server.execute(() -> {
            // Only the sitter in the picture: its escort and anything else it brought along go.
            for (Entity e : server.overworld().getEntities((Entity) null, new AABB(sx - 10, sy - 4, sz - 10, sx + 10, sy + 8, sz + 10),
                    e -> e != sitter && !(e instanceof Player))) e.discard();
            face(180);
            // The camera on its head: closer for small heads, further for a tall one.
            double dist = Math.max(1.5, sitter.getBbHeight() * 0.9), hy = sy + sitter.getBbHeight() * 0.84;
            if (t < 34) look(self(server), sx, hy + 0.08, sz - dist, sx, hy, sz);
            else look(self(server), sx + dist * 0.62, hy + 0.12, sz - dist * 0.82, sx, hy, sz);
        });
        if (t == 30) shot(mc, s.name() + "-front");
        if (t == 54) shot(mc, s.name() + "-side");
        if (t == EACH - 1) server.execute(() -> { if (sitter != null) sitter.discard(); sitter = null; });
    }

    private static void face(float yaw) {
        sitter.setYRot(yaw);
        sitter.yRotO = yaw;
        sitter.setYBodyRot(yaw);
        sitter.setYHeadRot(yaw);
        sitter.yHeadRotO = yaw;
        sitter.setXRot(0);
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
        check(true, "picture " + name);
    }

    private static void check(boolean ok, String what) {
        report.add((ok ? "ok     " : "FAILED ") + what);
        LOGGER.info("Mugshot tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("mugshot-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the mugshot tour report", e);
        }
        LOGGER.info("Mugshot tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
