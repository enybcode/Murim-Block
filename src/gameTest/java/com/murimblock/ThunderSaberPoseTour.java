package com.murimblock;

import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.martial.ThunderSaberItems;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;

/**
 * Opt-in graphical check of the Purple Heaven Thunder Saber's player animations alone, by day, without their effects:
 * each clip played on the player holding the Purple Heaven Zhanmadao, filmed from the side then from the front
 * three-quarter, a frame every two ticks (scripts/animation/thunder-saber-art.cjs). Never included in the player JAR.
 * Run: gradlew -PgameTests runThunderSaberPoseTour; pictures in build/thunder-saber-pose-tour-run/screenshots/<clip>/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ThunderSaberPoseTour {
    private static boolean creating, finished;
    private static int ticks;
    private static Marker camera;

    /** A clip, its name, and how long to film it (ticks). */
    private record Clip(String name, java.util.function.Supplier<AnimationAccessor<? extends StaticAnimation>> clip, int length) { }

    private static final List<Clip> CLIPS = List.of(
            new Clip("idle", () -> Animations.BIPED_HOLD_GREATSWORD, 10),
            new Clip("auto1", () -> Animations.GREATSWORD_AUTO1, 30),
            new Clip("descends", () -> MurimAnimations.TS_DESCENDS, 30),
            new Clip("rolling", () -> MurimAnimations.TS_ROLLING, 28),
            new Clip("drum_plant", () -> MurimAnimations.TS_DRUM_PLANT, 16),
            new Clip("drum_strike", () -> MurimAnimations.TS_DRUM_STRIKE, 14),
            new Clip("flash_charge", () -> MurimAnimations.TS_FLASH_CHARGE, 12),
            new Clip("flash_cut", () -> MurimAnimations.TS_FLASH_CUT, 16),
            new Clip("wrath", () -> MurimAnimations.TS_WRATH, 80),
            new Clip("sovereign", () -> MurimAnimations.TS_SOVEREIGN, 28),
            new Clip("finish", () -> MurimAnimations.FINISH_THUNDER_SABER, 50));
    private static final int GAP = 12, START = 60;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.thunderSaberPoseTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Thunder Poses", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("thunder-poses-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.hideGui = true;
            mc.getToasts().clear();
            var server = mc.getSingleplayerServer();
            if (ticks == 15) server.execute(() -> {
                var level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                for (int x = -6; x < 7; x++) for (int z = -6; z < 7; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                ServerPlayer player = player(server);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThunderSaberItems.ZHANMADAO.get()));
                com.murimblock.combat.CombatService.setCombatMode(player, true);
                player.teleportTo(level, 0.5, -60, 0.5, 0, 0);
            });
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                mc.options.fov().set(50);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            // Each clip twice: from the right side, then from the front three-quarter.
            int t = START;
            for (Clip c : CLIPS) for (int view = 0; view < 2; view++) {
                int at = t, v = view;
                if (ticks == at - 4) {
                    server.execute(() -> player(server).teleportTo(server.overworld(), 0.5, -60, 0.5, 0, 0));
                    if (v == 0) place(5.6, -58.6, 1.4, 90, 6); else place(3.3, -58.4, 4.6, 145, 8);
                }
                if (ticks == at) server.execute(() -> {
                    var patch = EpicFightBridge.patch(player(server));
                    if (patch != null) patch.playAnimationSynchronized(c.clip.get(), 0.0F);
                });
                if (ticks >= at && ticks <= at + c.length && (ticks - at) % 2 == 0)
                    capture(mc, c.name + "/" + (v == 0 ? "side" : "front") + String.format("-%02d.png", ticks - at));
                t += c.length + GAP;
            }
            if (ticks == t + 10) {
                Files.writeString(mc.gameDirectory.toPath().resolve("thunder-saber-pose-tour-report.txt"), "PASSED\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Thunder Saber pose tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void place(double x, double y, double z, float yaw, float pitch) {
        camera.setPos(x, y, z);
        camera.xo = camera.xOld = x;
        camera.yo = camera.yOld = y;
        camera.zo = camera.zOld = z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        var path = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(path.getParent());
        var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        net.minecraft.Util.ioPool().execute(() -> { try (image) { image.writeToFile(path); } catch (Exception e) { throw new RuntimeException(e); } });
    }
}
