package com.murimblock;

import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.SevenStarsSword;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * Opt-in graphical check of the Seven Stars Sword, one piece at a time. Its passive, Star Marks, at night (the stars are
 * light): three zombies before the player carrying 2, 5 and 7 stars (the last in its Dipper), from the front and close;
 * then the full one burst, frame by frame. Then Shooting Star: the player crosses a line of three zombies (the last at
 * seven), seen from the side frame by frame, then from behind. Then each of the others in turn: Dipper Steps, Orbiting
 * Stars stopping two blows, Constellation Link on three marked zombies, Falling Star Rain, the Seven Stars Sword Array,
 * Polaris Riposte. Never included in the player JAR.
 * Run: gradlew -PgameTests runSevenStarsTour; pictures in build/seven-stars-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SevenStarsTour {
    private static boolean creating, finished;
    private static int ticks;
    private static final List<Zombie> row = new ArrayList<>();
    private static Marker camera;
    private static String cast = "none";

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.sevenStarsTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Seven Stars", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("seven-stars-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
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
                level.setDayTime(18000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                for (int x = -8; x < 9; x++) for (int z = -8; z < 8; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                ServerPlayer player = player(server);
                MartialService.learn(player, MartialArts.art(MartialArts.SEVEN_STARS_SWORD));
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("epicfight_dd", "iron_florett"))));
                // Out of the pictures, behind the camera, looking away.
                player.teleportTo(level, 0.5, -60, -7.5, 180, 0);
                for (int i = 0; i < 3; i++) {
                    Zombie z = EntityType.ZOMBIE.create(level);
                    z.moveTo(-2.5 + i * 3.0, -60, 1.5, 180, 0);
                    z.setYHeadRot(180);
                    z.yBodyRot = 180;
                    z.setNoAi(true);
                    z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    z.setPersistenceRequired();
                    level.addFreshEntity(z);
                    row.add(z);
                }
            });
            if (ticks == 30) server.execute(() -> {
                ServerPlayer player = player(server);
                int[] stars = {2, 5, 7};
                for (int i = 0; i < 3; i++) for (int k = 0; k < stars[i]; k++) SevenStarsSword.addStars(player, row.get(i), 1);
            });
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                place(0.5, -58.2, -5.5, 0, 12);
                mc.options.fov().set(60);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            if (ticks == 60) capture(mc, "01-two-five-seven.png");
            if (ticks == 62) place(1.9, -58.5, -0.6, -37, -6);
            if (ticks == 75) capture(mc, "02-close-on-seven.png");
            if (ticks == 77) place(0.5, -57.0, -6.5, 0, 24);
            if (ticks == 90) capture(mc, "03-from-above.png");
            if (ticks == 92) place(3.5, -58.6, -2.5, 0, 10);
            if (ticks == 100) server.execute(() -> {
                ServerPlayer player = player(server);
                SevenStarsSword.strikeForTests(player, row.get(2));
            });
            if (ticks >= 100 && ticks <= 118) capture(mc, String.format("04-burst-%02d.png", ticks - 100));
            // ---- Shooting Star
            if (ticks == 140) server.execute(() -> {
                ServerPlayer player = player(server);
                com.murimblock.combat.CombatService.setCombatMode(player, true);
                for (int i = 0; i < 3; i++) {
                    Zombie z = row.get(i);
                    z.setHealth(z.getMaxHealth());
                    z.moveTo(0.5, -60, 1.5 + i * 2.5, 180, 0);
                }
                for (int k = 0; k < 7; k++) SevenStarsSword.addStars(player, row.get(2), 1);
                player.teleportTo(server.overworld(), 0.5, -60, -2.5, 0, 0);
            });
            if (ticks == 145) place(7.5, -58.6, 3.0, 90, 6);
            if (ticks == 160) server.execute(() -> {
                ServerPlayer player = player(server);
                MartialService.clearCooldownsForTests(player);
                cast = MartialService.castSlot(player, 0).name();
            });
            if (ticks >= 158 && ticks <= 176) capture(mc, String.format("05-streak-side-%02d.png", ticks - 158));
            if (ticks == 190) server.execute(() -> {
                ServerPlayer player = player(server);
                for (int i = 0; i < 3; i++) row.get(i).setHealth(row.get(i).getMaxHealth());
                for (int k = 0; k < 7; k++) SevenStarsSword.addStars(player, row.get(2), 1);
                player.teleportTo(server.overworld(), 0.5, -60, -2.5, 0, 0);
            });
            if (ticks == 195) place(0.5, -57.4, -6.5, 0, 14);
            if (ticks == 210) server.execute(() -> {
                ServerPlayer player = player(server);
                MartialService.clearCooldownsForTests(player);
                MartialService.castSlot(player, 0);
            });
            if (ticks >= 208 && ticks <= 224) capture(mc, String.format("06-streak-behind-%02d.png", ticks - 208));
            // ---- The other techniques: all unlocked, in slots 2 to 6.
            if (ticks == 238) server.execute(() -> {
                ServerPlayer player = player(server);
                MartialArts.Technique[] all = {MartialArts.DIPPER_STEPS, MartialArts.ORBITING_STARS, MartialArts.CONSTELLATION_LINK,
                        MartialArts.FALLING_STAR_RAIN, MartialArts.SEVEN_STARS_ARRAY};
                for (int i = 0; i < all.length; i++) { MartialService.unlock(player, all[i]); MartialService.assign(player, i + 1, all[i].id()); }
                MartialService.unlock(player, MartialArts.POLARIS_RIPOSTE);
                com.murimblock.qi.QiService.setQiMax(player, 2000);
                com.murimblock.qi.QiService.setQi(player, 2000);
                lineUp(server, new double[][] {{0.5, 3.0}, {-3.5, 5.5}, {4.5, 5.5}});
                player.teleportTo(server.overworld(), 0.5, -60, 0.0, 0, 0);
            });
            // Dipper Steps, from the side.
            if (ticks == 242) place(7.0, -58.4, 2.0, 90, 8);
            if (ticks == 250) cast(server, 1);
            if (ticks >= 250 && ticks <= 276 && ticks % 2 == 0) capture(mc, String.format("07-dipper-%02d.png", ticks - 250));
            // Orbiting Stars, two blows stopped.
            if (ticks == 290) server.execute(() -> {
                lineUp(server, new double[][] {{0.5, 2.0}, {-3.5, 5.5}, {4.5, 5.5}});
                player(server).teleportTo(server.overworld(), 0.5, -60, 0.0, 0, 0);
            });
            if (ticks == 292) place(4.0, -58.2, -3.5, 45, 12);
            if (ticks == 300) cast(server, 2);
            if (ticks == 312 || ticks == 320) server.execute(() -> player(server).hurt(player(server).damageSources().mobAttack(row.get(0)), 4));
            if (ticks >= 300 && ticks <= 336 && ticks % 3 == 0) capture(mc, String.format("08-orbit-%02d.png", ticks - 300));
            // Constellation Link on three marked zombies.
            if (ticks == 345) server.execute(() -> {
                ServerPlayer player = player(server);
                lineUp(server, new double[][] {{-2.5, 3.5}, {0.5, 5.0}, {3.5, 3.5}});
                int[] n = {3, 5, 7};
                for (int i = 0; i < 3; i++) for (int k = 0; k < n[i]; k++) SevenStarsSword.addStars(player, row.get(i), 1);
                player.teleportTo(server.overworld(), 0.5, -60, -1.0, 0, 0);
            });
            if (ticks == 347) place(0.5, -57.6, -6.0, 0, 14);
            if (ticks == 355) cast(server, 3);
            if (ticks >= 355 && ticks <= 377 && ticks % 2 == 0) capture(mc, String.format("09-link-%02d.png", ticks - 355));
            // Falling Star Rain onto the three, two of them marked.
            if (ticks == 390) server.execute(() -> {
                ServerPlayer player = player(server);
                lineUp(server, new double[][] {{-0.5, 6.0}, {1.5, 7.0}, {0.5, 8.0}});
                for (int k = 0; k < 3; k++) SevenStarsSword.addStars(player, row.get(0), 1);
                for (int k = 0; k < 5; k++) SevenStarsSword.addStars(player, row.get(1), 1);
                player.teleportTo(server.overworld(), 0.5, -60, -1.0, 0, 14);
            });
            if (ticks == 392) place(-5.5, -56.5, -1.5, -40, 22);
            if (ticks == 400) cast(server, 4);
            if (ticks >= 400 && ticks <= 451 && ticks % 3 == 0) capture(mc, String.format("10-rain-%02d.png", ticks - 400));
            // The Seven Stars Sword Array.
            if (ticks == 465) server.execute(() -> {
                ServerPlayer player = player(server);
                lineUp(server, new double[][] {{0.5, 4.5}, {-6.5, 9.5}, {7.5, 9.5}});
                for (int k = 0; k < 4; k++) SevenStarsSword.addStars(player, row.get(0), 1);
                player.teleportTo(server.overworld(), 0.5, -60, -1.0, 0, 10);
            });
            if (ticks == 467) place(5.5, -56.8, -2.5, 40, 22);
            if (ticks == 475) cast(server, 5);
            if (ticks >= 475 && ticks <= 547 && ticks % 4 == 0) capture(mc, String.format("11-array-%02d.png", ticks - 475));
            // Polaris Riposte: a perfect parry, then the strike.
            if (ticks == 560) server.execute(() -> {
                ServerPlayer player = player(server);
                lineUp(server, new double[][] {{0.5, 1.6}, {-6.5, 9.5}, {7.5, 9.5}});
                player.teleportTo(server.overworld(), 0.5, -60, 0.0, 0, 0);
            });
            if (ticks == 562) place(4.0, -58.0, -2.5, 50, 10);
            if (ticks == 570) server.execute(() -> {
                ServerPlayer player = player(server);
                com.murimblock.martial.MartialCombat.onPerfectParry(player, row.get(0));
                SevenStarsSword.setStars(player, row.get(0), 0);
                SevenStarsSword.strikeForTests(player, row.get(0));
            });
            if (ticks >= 570 && ticks <= 594 && ticks % 3 == 0) capture(mc, String.format("12-polaris-%02d.png", ticks - 570));
            if (ticks == 610) {
                Files.writeString(mc.gameDirectory.toPath().resolve("seven-stars-tour-report.txt"), "PASSED; cast=" + cast + "; shown=" + row.stream().map(z -> com.murimblock.client.SevenStarsFx.shown(z.getId())).toList() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Seven Stars tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    /** Puts the three zombies at these places (x, z), whole again, their stars gone. */
    private static void lineUp(net.minecraft.server.MinecraftServer server, double[][] at) {
        ServerPlayer player = player(server);
        for (int i = 0; i < 3; i++) {
            Zombie z = row.get(i);
            SevenStarsSword.setStars(player, z, 0);
            z.setHealth(z.getMaxHealth());
            z.moveTo(at[i][0], -60, at[i][1], 180, 0);
            z.setYHeadRot(180);
            z.yBodyRot = 180;
        }
    }

    private static void cast(net.minecraft.server.MinecraftServer server, int slot) {
        server.execute(() -> {
            ServerPlayer player = player(server);
            MartialService.resetCooldowns(player);
            com.mojang.logging.LogUtils.getLogger().info("Seven Stars tour: slot {} -> {}", slot, MartialService.castSlot(player, slot));
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
