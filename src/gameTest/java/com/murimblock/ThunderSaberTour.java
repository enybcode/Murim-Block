package com.murimblock;

import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.ThunderSaber;
import com.murimblock.martial.ThunderSaberItems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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
 * Opt-in graphical check of the Purple Heaven Thunder Saber, at night (the art is lightning). The player with the Purple
 * Heaven Zhanmadao in hand, its combat bar and drums; then each technique against three zombies, thunder versions (drums
 * full), frame by frame: Thunder Descends from the side, Rolling Thunder from above, Heaven's Drum stopping three blows
 * then beaten, Severing Flash charged then let go, Nine Heavens' Wrath from afar (the storm, the bolts, the column), the
 * Thunder Sovereign's Descent and its last bolt. Never included in the player JAR.
 * Run: gradlew -PgameTests runThunderSaberTour; pictures in build/thunder-saber-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ThunderSaberTour {
    private static boolean creating, finished;
    private static int ticks;
    private static final List<Zombie> row = new ArrayList<>();
    private static final Map<String, String> casts = new LinkedHashMap<>();
    private static Marker camera;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.thunderSaberTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Thunder Saber", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("thunder-saber-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getToasts().clear();
            var server = mc.getSingleplayerServer();
            if (ticks == 15) server.execute(() -> {
                var level = server.overworld();
                level.setDayTime(18000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                for (int x = -10; x < 11; x++) for (int z = -10; z < 14; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_STONE.defaultBlockState());
                ServerPlayer player = player(server);
                MartialService.learn(player, MartialArts.art(MartialArts.THUNDER_SABER));
                MartialArts.Technique[] all = {MartialArts.THUNDER_DESCENDS, MartialArts.ROLLING_THUNDER, MartialArts.HEAVENS_DRUM, MartialArts.SEVERING_FLASH,
                        MartialArts.NINE_HEAVENS_WRATH, MartialArts.THUNDER_SOVEREIGN};
                for (int i = 0; i < all.length; i++) { MartialService.unlock(player, all[i]); MartialService.assign(player, i, all[i].id()); }
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThunderSaberItems.ZHANMADAO.get()));
                com.murimblock.combat.CombatService.setCombatMode(player, true);
                com.murimblock.qi.QiService.setQiMax(player, 3000);
                com.murimblock.qi.QiService.setQi(player, 3000);
                player.teleportTo(level, 0.5, -60, -2.5, 0, 0);
                for (int i = 0; i < 3; i++) {
                    Zombie z = EntityType.ZOMBIE.create(level);
                    z.moveTo(-2.5 + i * 3.0, -60, 4.5, 180, 0);
                    z.setNoAi(true);
                    z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(300);
                    z.setHealth(300);
                    z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
                    z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    z.setPersistenceRequired();
                    level.addFreshEntity(z);
                    row.add(z);
                }
                ThunderSaber.setBeats(player, 7);
            });
            // The player in their own eyes: the combat bar, the drums; then seen from the front, the saber in hand.
            if (ticks == 40) { mc.options.hideGui = false; mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); }
            if (ticks == 50) capture(mc, "00-hud.png");
            if (ticks == 52) {
                mc.options.hideGui = true;
                camera = new Marker(EntityType.MARKER, mc.level);
                place(1.6, -58.6, 0.3, 160, 14);
                mc.options.fov().set(60);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            if (ticks == 62) capture(mc, "01-saber-front.png");
            if (ticks == 64) place(-2.2, -58.9, -1.2, -110, 6);
            if (ticks == 74) capture(mc, "02-saber-side.png");
            // ---- Thunder Descends, from the side.
            if (ticks == 90) server.execute(() -> {
                lineUp(server, new double[][] {{0.5, 4.5}, {-2.0, 5.5}, {3.0, 5.5}});
                prepare(server, 0.5, -2.5, 0, 9);
            });
            if (ticks == 92) place(8.0, -58.2, 1.5, 90, 6);
            if (ticks == 100) cast(server, 0, "descends");
            if (ticks >= 100 && ticks <= 132 && ticks % 2 == 0) capture(mc, String.format("10-descends-%02d.png", ticks - 100));
            // ---- Rolling Thunder, from above.
            if (ticks == 150) server.execute(() -> {
                lineUp(server, new double[][] {{-1.8, 2.0}, {2.8, 2.0}, {0.5, 5.0}});
                prepare(server, 0.5, 0, 0, 3);
            });
            if (ticks == 152) place(0.5, -54.5, -4.5, 0, 50);
            if (ticks == 160) cast(server, 1, "rolling");
            if (ticks >= 160 && ticks <= 190 && ticks % 2 == 0) capture(mc, String.format("11-rolling-%02d.png", ticks - 160));
            // ---- Heaven's Drum: three blows stopped, then beaten.
            if (ticks == 205) server.execute(() -> {
                lineUp(server, new double[][] {{0.5, 1.8}, {-3.0, 5.0}, {3.5, 5.0}});
                prepare(server, 0.5, 0, 0, 3);
            });
            if (ticks == 207) place(4.0, -58.0, -3.5, 45, 12);
            if (ticks == 215) server.execute(() -> casts.put("drum-aim", String.valueOf(MartialService.aim(player(server), 2))));
            for (int k = 0; k < 3; k++) if (ticks == 225 + k * 8) server.execute(() -> player(server).hurt(player(server).damageSources().mobAttack(row.get(0)), 4));
            if (ticks == 256) cast(server, 2, "drum");
            if (ticks >= 215 && ticks <= 272 && ticks % 3 == 0) capture(mc, String.format("12-drum-%02d.png", ticks - 215));
            // ---- Severing Flash, charged then let go, from the side.
            if (ticks == 290) server.execute(() -> {
                lineUp(server, new double[][] {{0.5, 3.0}, {0.5, 6.5}, {1.2, 10.0}});
                prepare(server, 0.5, -1.5, 0, 3);
            });
            if (ticks == 292) place(6.5, -55.6, -3.5, 50, 32);
            if (ticks == 300) server.execute(() -> casts.put("flash-aim", String.valueOf(MartialService.aim(player(server), 3))));
            if (ticks == 330) cast(server, 3, "flash");
            if (ticks >= 300 && ticks <= 350 && ticks % 2 == 0) capture(mc, String.format("13-flash-%02d.png", ticks - 300));
            // ---- Nine Heavens' Wrath, from afar: the storm, the bolts, the column.
            if (ticks == 370) server.execute(() -> {
                lineUp(server, new double[][] {{-0.5, 6.0}, {1.8, 7.0}, {0.5, 8.6}});
                prepare(server, 0.5, -1.5, 14, 9);
            });
            if (ticks == 372) place(-14.0, -57.5, -7.0, -45, -14);
            if (ticks == 380) cast(server, 4, "wrath");
            if (ticks >= 380 && ticks <= 470 && ticks % 3 == 0) capture(mc, String.format("14-wrath-%02d.png", ticks - 380));
            // ---- The Thunder Sovereign's Descent, then a strike in it, then its last bolt.
            if (ticks == 490) server.execute(() -> {
                lineUp(server, new double[][] {{0.5, 2.2}, {-3.0, 5.5}, {3.5, 5.5}});
                prepare(server, 0.5, 0, 0, 0);
            });
            if (ticks == 492) place(5.0, -58.0, -3.0, 50, 10);
            if (ticks == 500) cast(server, 5, "sovereign");
            if (ticks >= 500 && ticks <= 530 && ticks % 2 == 0) capture(mc, String.format("15-sovereign-%02d.png", ticks - 500));
            if (ticks == 540) server.execute(() -> ThunderSaber.strikeForTests(player(server), row.get(0), false));
            if (ticks >= 540 && ticks <= 548 && ticks % 2 == 0) capture(mc, String.format("16-sovereign-strike-%02d.png", ticks - 540));
            if (ticks >= 664 && ticks <= 690 && ticks % 2 == 0) capture(mc, String.format("17-sovereign-end-%02d.png", ticks - 664));
            if (ticks == 710) {
                Files.writeString(mc.gameDirectory.toPath().resolve("thunder-saber-tour-report.txt"), "PASSED; casts=" + casts
                        + "; electrified shown=" + com.murimblock.client.ThunderSaberFx.electrifiedShown() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Thunder Saber tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    /** The three zombies at these places (x, z), whole again, facing the player. */
    private static void lineUp(net.minecraft.server.MinecraftServer server, double[][] at) {
        for (int i = 0; i < 3; i++) {
            Zombie z = row.get(i);
            z.setHealth(z.getMaxHealth());
            z.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            z.moveTo(at[i][0], -60, at[i][1], 180, 0);
            z.setYHeadRot(180);
            z.yBodyRot = 180;
        }
    }

    /** The player at (x, z) facing south, looking `pitch` down, with `beats` on their drums, cooldowns cleared. */
    private static void prepare(net.minecraft.server.MinecraftServer server, double x, double z, float pitch, int beats) {
        ServerPlayer player = player(server);
        player.teleportTo(server.overworld(), x, -60, z, 0, pitch);
        MartialService.resetCooldowns(player);
        com.murimblock.qi.QiService.setQi(player, 3000);
        ThunderSaber.setBeats(player, beats);
    }

    private static void cast(net.minecraft.server.MinecraftServer server, int slot, String name) {
        server.execute(() -> {
            ServerPlayer player = player(server);
            if (!name.equals("drum") && !name.equals("flash")) MartialService.resetCooldowns(player);
            casts.put(name, MartialService.castSlot(player, slot).name());
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
