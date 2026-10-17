package com.murimblock.bloodsect;

import com.murimblock.bloodsect.ninja.BloodSoldier;
import com.murimblock.item.MurimItems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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

/**
 * Opt-in graphical check of the Blood Soldiers (BloodSoldier, CultistMesh.soldier): the three outfits in the Blood Lacquer
 * side by side, from the front and from the back; the middle one with its mask and cuirass taken off; a close look; then
 * one of them walking away and back (its own walk, the left hand at its sash), then the player wearing the whole set (Epic Fight's armour meshes), from the front and from the back. Never included in the
 * player JAR. Run: gradlew -PgameTests runSoldierLookTour; pictures in build/soldier-look-tour-run/screenshots.
 */
@EventBusSubscriber(modid = BloodSect.ID, value = Dist.CLIENT)
public final class SoldierLookTour {
    private static boolean creating, finished;
    private static int ticks;
    private static final List<BloodSoldier> row = new ArrayList<>();
    private static Marker camera;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("bloodsect.soldierLookTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Blood Soldier Look", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("blood-soldier-look-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
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
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                for (int x = -8; x < 9; x++) for (int z = -8; z < 6; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState());
                player(server).teleportTo(level, 0.5, -60, -7, 0, 0);
                for (int i = 0; i < BloodSoldier.SOLDIER_OUTFITS; i++) {
                    BloodSoldier n = BloodSect.BLOOD_SOLDIER.get().create(level);
                    n.moveTo(-1.5 + i * 2.0, -60, 0.5, 180, 0);
                    n.setYHeadRot(180);
                    n.yBodyRot = 180;
                    n.setNoAi(true);
                    n.setGuardPost(n.blockPosition());
                    n.setLook(i, 0);
                    level.addFreshEntity(n);
                    row.add(n);
                }
            });
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                place(0.5, -58.6, -4.6, 0, 8);
                mc.options.fov().set(60);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            if (ticks == 60) capture(mc, "01-soldiers-front.png");
            if (ticks == 62) server.execute(() -> row.forEach(n -> turn(n, 0)));
            if (ticks == 80) capture(mc, "02-soldiers-back.png");
            // The middle one, its mask and cuirass taken off: the cloth under them.
            if (ticks == 82) server.execute(() -> {
                row.forEach(n -> turn(n, 180));
                row.get(1).setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                row.get(1).setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            });
            if (ticks == 100) capture(mc, "03-mask-and-cuirass-off.png");
            if (ticks == 102) {
                server.execute(() -> row.get(1).wearTheLacquer());
                place(1.4, -58.8, -1.7, 20, 12);
            }
            if (ticks == 120) capture(mc, "04-close.png");
            // One walks off toward the camera's left, its own march.
            if (ticks == 122) {
                place(4.5, -58.8, -3.5, 45, 10);
                server.execute(() -> turn(row.get(2), 180));
            }
            // Walked by hand a step a tick at its own pace (it has no AI here), facing its way.
            if (ticks > 122 && ticks <= 150) server.execute(() -> {
                var n = row.get(2);
                n.setPos(n.getX(), n.getY(), n.getZ() - 0.12);
                turn(n, 180);
            });
            if (ticks >= 126 && ticks <= 150 && ticks % 3 == 0) capture(mc, String.format("05-walk-%02d.png", ticks - 126));
            // The player in the whole set, where the soldiers stood.
            if (ticks == 152) server.execute(() -> {
                row.forEach(BloodSoldier::discard);
                ServerPlayer player = player(server);
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(MurimItems.BLOOD_LACQUER_MASK.get()));
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(MurimItems.BLOOD_LACQUER_CUIRASS.get()));
                player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(MurimItems.BLOOD_LACQUER_TASSETS.get()));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(MurimItems.BLOOD_LACQUER_GREAVES.get()));
                player.teleportTo(server.overworld(), 0.5, -60, 0.5, 180, 0);
            });
            if (ticks == 154) place(0.5, -58.7, -2.6, 0, 8);
            // The player's body turned with it (a teleport turns only its head).
            if (ticks > 154 && ticks < 195) {
                float yaw = ticks < 177 ? 180 : 0;
                mc.player.setYRot(yaw);
                mc.player.setYHeadRot(yaw);
                mc.player.yBodyRot = mc.player.yBodyRotO = yaw;
            }
            if (ticks == 175) capture(mc, "06-player-front.png");
            if (ticks == 177) server.execute(() -> player(server).teleportTo(server.overworld(), 0.5, -60, 0.5, 0, 0));
            if (ticks == 195) capture(mc, "07-player-back.png");
            if (ticks == 205) {
                Files.writeString(mc.gameDirectory.toPath().resolve("soldier-look-tour-report.txt"), "PASSED; outfits=" + row.stream().map(BloodSoldier::getOutfit).toList() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Blood Soldier look tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static void turn(BloodSoldier n, float yaw) {
        n.setYRot(yaw);
        n.setYHeadRot(yaw);
        n.yBodyRot = yaw;
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
