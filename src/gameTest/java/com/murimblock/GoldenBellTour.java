package com.murimblock;

import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import java.nio.file.Files;
import java.util.ArrayList;
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
import net.minecraft.world.entity.monster.Zombie;
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

/**
 * Opt-in graphical check of the Golden Bell, bare hands, in survival (blows reach the player). The player faces the
 * camera: their own skin; the golden form taken (the bell coming down, the body in gold, its light at dusk); Iron
 * Rebound (the body iron) and a zombie's blow sent back; Rooted Mountain in the form (the body stone); the form left, the
 * bell's last ring; the own skin back. Never included in the player JAR.
 * Run: gradlew -PgameTests runGoldenBellTour; pictures in build/golden-bell-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GoldenBellTour {
    private static boolean creating, finished;
    private static int ticks;
    private static Zombie zombie;
    private static Marker camera;
    private static final List<String> casts = new ArrayList<>();

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.goldenBellTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Golden Bell", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("golden-bell-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
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
                // Dusk: the gold reads, and so does its light.
                level.setDayTime(12800);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                for (int x = -8; x < 9; x++) for (int z = -8; z < 9; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                ServerPlayer player = player(server);
                MartialService.learn(player, MartialArts.art(MartialArts.GOLDEN_BELL));
                MartialArts.Technique[] all = {MartialArts.GOLDEN_BELL_FORM, MartialArts.IRON_REBOUND, MartialArts.ROOTED_MOUNTAIN};
                for (int i = 0; i < all.length; i++) { MartialService.unlock(player, all[i]); MartialService.assign(player, i, all[i].id()); }
                MartialService.unlock(player, MartialArts.BELL_TOLL);
                com.murimblock.qi.QiService.setQiMax(player, 2000);
                com.murimblock.qi.QiService.setQi(player, 2000);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                com.murimblock.combat.CombatService.setCombatMode(player, true);
                // Facing the camera (south).
                player.teleportTo(level, 0.5, -60, 0.5, 0, 0);
                zombie = EntityType.ZOMBIE.create(level);
                zombie.moveTo(2.3, -60, 0.9, 90, 0);
                zombie.setYHeadRot(90);
                zombie.yBodyRot = 90;
                zombie.setNoAi(true);
                zombie.setPersistenceRequired();
                level.addFreshEntity(zombie);
            });
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                place(0.9, -58.6, 4.3, 175, 8);
                mc.options.fov().set(55);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            if (ticks == 60) capture(mc, "01-own-skin.png");
            // The golden form: the bell coming down, the body turning to gold.
            if (ticks == 70) cast(server, 0);
            if (ticks >= 70 && ticks <= 100 && ticks % 3 == 1) capture(mc, String.format("02-form-%02d.png", ticks - 70));
            if (ticks == 110) capture(mc, "03-gold.png");
            if (ticks == 112) place(0.5, -58.3, 2.6, 180, 10);
            if (ticks == 122) capture(mc, "04-gold-close.png");
            if (ticks == 124) place(0.9, -58.6, 4.3, 175, 8);
            // Iron Rebound, then the zombie's blow sent back.
            if (ticks == 135) cast(server, 1);
            if (ticks == 142) capture(mc, "05-iron.png");
            if (ticks == 146) server.execute(() -> {
                ServerPlayer player = player(server);
                player.invulnerableTime = 0;
                player.hurt(player.damageSources().mobAttack(zombie), 4);
            });
            if (ticks >= 146 && ticks <= 158 && ticks % 2 == 0) capture(mc, String.format("06-rebound-%02d.png", ticks - 146));
            // Rooted Mountain in the form: stone.
            if (ticks == 175) cast(server, 2);
            if (ticks == 190) capture(mc, "07-stone.png");
            if (ticks == 192) place(3.4, -58.9, 2.8, 135, 4);
            if (ticks == 202) capture(mc, "08-stone-side.png");
            if (ticks == 204) place(0.9, -58.6, 4.3, 175, 8);
            // The form left (the mountain over): the bell's last ring, the own skin back.
            if (ticks == 270) server.execute(() -> casts.add("leave=" + MartialService.castSlot(player(server), 0)));
            if (ticks >= 270 && ticks <= 284 && ticks % 2 == 0) capture(mc, String.format("09-ring-%02d.png", ticks - 270));
            if (ticks == 300) capture(mc, "10-own-skin-again.png");
            if (ticks == 310) {
                Files.writeString(mc.gameDirectory.toPath().resolve("golden-bell-tour-report.txt"), "PASSED; " + casts + "; zombie health " + zombie.getHealth() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Golden Bell tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void cast(net.minecraft.server.MinecraftServer server, int slot) {
        server.execute(() -> {
            ServerPlayer player = player(server);
            MartialService.resetCooldowns(player);
            casts.add(slot + "=" + MartialService.castSlot(player, slot));
        });
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
