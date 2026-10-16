package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.combat.ShatteredGround;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
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
import yesman.epicfight.gameasset.Animations;

/**
 * Development-only film of the Mountain-Shattering Fist in a real client (gradlew -PgameTests runMountainFistTour): in a
 * fresh flat world, on a stone plateau, a player who has read the scroll casts each technique at straw-men zombies, seen
 * from the side by a fixed camera: the passive's strikes, the punch through a wall, the Earth Breaker, the Bull Charge
 * through a wall, the secret Sky Breaker, a dive from the air, the ultimate, and the ground coming back. A picture every
 * other tick, to build/mountain-fist-tour-run/screenshots/&lt;part&gt;/frame-NNN.png.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MountainFistTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int START = 80;
    private static boolean creating, finished, failed;
    private static int ticks;
    private static int cx, cz, g;
    private static Marker camera;
    private static final List<String> report = new ArrayList<>();
    private static final List<Zombie> dummies = new ArrayList<>();

    /** A part of the film: how long, what happens at its start (server thread), how far the camera stands back. */
    private record Part(String name, int length, java.util.function.Consumer<ServerPlayer> start, double back) { }

    private static final List<Part> PARTS = List.of(
            new Part("passive", 40, p -> { place(p); dummy(p, 0, 1.6); dummy(p, 1.0, 2.2);
                EpicFightBridge.patch(p).playAnimationSynchronized(Animations.FIST_AUTO1, 0); }, 7),
            new Part("crusher", 36, p -> { place(p); wall(p, 3); dummy(p, 0, 1.8); dummy(p, 1.6, 3.4); cast(p, 0); }, 8),
            new Part("earth_breaker", 50, p -> { place(p); dummy(p, 0, 2.5); dummy(p, 0, -2.5); dummy(p, 2.5, 0); dummy(p, -2.5, 0); cast(p, 1); }, 9),
            new Part("bull_charge", 46, p -> { place(p); wall(p, 3); dummy(p, 0, 2.0); cast(p, 2); }, 9),
            new Part("sky_breaker", 50, p -> { place(p); dummy(p, 0, 1.5); p.setShiftKeyDown(true); cast(p, 0); p.setShiftKeyDown(false); }, 9),
            new Part("dive", 50, p -> { place(p); dummy(p, 1.5, 3.0); dummy(p, -1.5, 3.0);
                p.teleportTo(p.serverLevel(), p.getX(), p.getY() + 7, p.getZ() + 3, 0, 0); p.server.execute(() -> cast(p, 0)); }, 10),
            new Part("ultimate", 110, p -> { place(p); dummy(p, 0, 9); dummy(p, 2.5, 10); dummy(p, -2.5, 8.5);
                p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 0, 25); cast(p, 3); }, 22),
            new Part("ground_returns", 330, p -> { }, 18));

    private MountainFistTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.mountainFistTour")) return;
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
                    LevelSettings settings = new LevelSettings("murim-mountain-fist", GameType.SURVIVAL, false, Difficulty.EASY, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("murim-mountain-fist", settings, new WorldOptions(1L, false, false),
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
            LOGGER.error("Mountain fist tour crashed", e);
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
                cx = (int) Math.floor(self.getX());
                cz = (int) Math.floor(self.getZ());
                g = (int) Math.floor(self.getY()) + 9;
                BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
                for (int x = cx - 30; x <= cx + 30; x++) for (int z = cz - 30; z <= cz + 40; z++) for (int y = g - 10; y <= g; y++)
                    level.setBlock(q.set(x, y, z), (y == g ? Blocks.GRASS_BLOCK : y > g - 3 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 2 | 16);
                self.getAbilities().invulnerable = true;
                self.onUpdateAbilities();
                EpicFightCombatDefaults.enforce(self);
                CultivationService.setCultivation(self, MartialArts.MOUNTAIN_FIST_REALM, CultivationStage.EARLY);
                new ItemStack(MurimItems.MOUNTAIN_SHATTERING_FIST_SCROLL.get()).use(level, self, InteractionHand.MAIN_HAND);
                self.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                check(MartialService.data(self).knowsArt(MartialArts.MOUNTAIN_SHATTERING_FIST), "the scroll teaches the art at Qi Condensation");
                for (var t : List.of(MartialArts.EARTH_BREAKER, MartialArts.BULL_CHARGE, MartialArts.HEAVEN_FALLING_MOUNTAIN)) MartialService.unlock(self, t);
                MartialService.assign(self, 1, MartialArts.EARTH_BREAKER.id());
                MartialService.assign(self, 2, MartialArts.BULL_CHARGE.id());
                MartialService.assign(self, 3, MartialArts.HEAVEN_FALLING_MOUNTAIN.id());
                place(self);
            });
            return;
        }
        if (ticks < START) return;
        int at = ticks - START, n = 0;
        while (n < PARTS.size() && at >= PARTS.get(n).length()) { at -= PARTS.get(n).length(); n++; }
        if (n >= PARTS.size()) { finish(mc); return; }
        Part part = PARTS.get(n);
        final int t = at;
        if (t == 0) server.execute(() -> {
            ServerPlayer self = self(server);
            dummies.forEach(z -> z.discard());
            dummies.clear();
            MartialService.clearCooldownsForTests(self);
            QiService.setQiMax(self, 2000);
            QiService.setQi(self, 2000);
            // Each part on whole ground (but the last, which waits for the broken ground to come back by itself).
            if (!part.name().equals("ground_returns")) {
                ShatteredGround.restoreAllForTests(self.serverLevel());
                BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
                for (int x = cx - 8; x <= cx + 8; x++) for (int z = cz - 8; z <= cz + 16; z++) for (int y = g + 1; y <= g + 5; y++)
                    self.serverLevel().setBlock(q.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
            part.start().accept(self);
        });
        // The camera beside the player, looking across at him (wider for the ultimate).
        if (camera == null) {
            camera = new Marker(EntityType.MARKER, mc.level);
            mc.setCameraEntity(camera);
        }
        Vec3 him = mc.player.position();
        Vec3 eye = part.name().equals("ultimate") || part.name().equals("ground_returns")
                ? new Vec3(cx + 0.5 - part.back(), g + 12, cz + 6)
                : him.add(-part.back() * 0.7, 2.2, -1.0);
        Vec3 look = part.name().equals("ultimate") ? him.add(0, 1, 0).lerp(new Vec3(cx + 0.5, g + 1, cz + 9), 0.5)
                : part.name().equals("ground_returns") ? new Vec3(cx + 0.5, g, cz + 6) : him.add(0, 1.2, 1.5);
        Vec3 d = look.subtract(eye);
        camera.moveTo(eye.x, eye.y, eye.z, (float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        camera.xo = camera.xOld = eye.x; camera.yo = camera.yOld = eye.y; camera.zo = camera.zOld = eye.z;
        camera.yRotO = camera.getYRot(); camera.xRotO = camera.getXRot();
        if (part.name().equals("ground_returns") ? t % 10 == 0 : t % 2 == 1) shot(mc, part.name() + "/frame-" + String.format("%03d", t / 2));
        if (t == part.length() - 1) check(true, part.name() + " filmed");
    }

    /** Back to the middle of the plateau, facing south (+z), on the ground. */
    private static void place(ServerPlayer p) {
        p.teleportTo(p.serverLevel(), cx + 0.5, g + 1, cz + 0.5, 0, 0);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static void cast(ServerPlayer p, int slot) {
        var result = MartialService.castSlot(p, slot);
        check(result == MartialService.CastResult.CAST, "slot " + (slot + 1) + " cast (" + result + ")");
    }

    /** A stone wall three wide and three high, `ahead` blocks before the player. */
    private static void wall(ServerPlayer p, int ahead) {
        for (int x = -1; x <= 1; x++) for (int y = 1; y <= 3; y++)
            p.serverLevel().setBlock(new BlockPos(cx + x, g + y, cz + ahead), Blocks.COBBLESTONE.defaultBlockState(), 3);
    }

    /** A straw-man: a zombie with no mind, much health. */
    private static void dummy(ServerPlayer p, double dx, double dz) {
        Zombie z = EntityType.HUSK.spawn(p.serverLevel(), BlockPos.containing(cx + dx, g + 1, cz + dz), MobSpawnType.COMMAND);
        if (z == null) return;
        z.setNoAi(true);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
        z.setHealth(300);
        z.moveTo(cx + 0.5 + dx, g + 1, cz + 0.5 + dz, 180, 0);
        z.setPersistenceRequired();
        dummies.add(z);
    }

    private static ServerPlayer self(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
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
        LOGGER.info("Mountain fist tour: {} {}", ok ? "ok" : "FAILED", what);
        if (!ok) failed = true;
    }

    private static void finish(Minecraft mc) {
        if (finished) return;
        finished = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("mountain-fist-tour-report.txt"),
                    String.join("\n", report) + "\n" + (failed ? "FAILED\n" : "PASSED\n"));
        } catch (Exception e) {
            LOGGER.error("Could not write the mountain fist tour report", e);
        }
        LOGGER.info("Mountain fist tour finished: {}", failed ? "FAILED" : "PASSED");
        mc.stop();
    }
}
