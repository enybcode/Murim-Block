package com.murimblock.bloodsect;

import com.murimblock.bloodsect.ninja.BloodNinja;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Cow;
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
 * Opt-in graphical check of the Blood Cultists' look (Murimblock's model, CultistMesh): the five ranks side by side, each in
 * one of the armours (or none), from the front and from the back, then the armours swapped round, then one of them
 * swinging its dao at a cow (the blade in the hand of the longer forearm). Never included in the player JAR.
 * Run: gradlew -PgameTests runCultistLookTour; pictures in build/cultist-look-tour-run/screenshots.
 */
@EventBusSubscriber(modid = BloodSect.ID, value = Dist.CLIENT)
public final class CultistLookTour {
    private static boolean creating, finished;
    private static int ticks;
    private static final List<BloodNinja> row = new ArrayList<>();
    private static Cow cow;
    private static Marker camera;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("bloodsect.cultistLookTour") || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("Blood Cultist Look", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("blood-cultist-look-" + System.currentTimeMillis(), settings, new WorldOptions(1L, false, false),
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
                server.getPlayerList().getPlayer(mc.player.getUUID()).teleportTo(level, 0.5, -60, -7, 0, 0);
                for (int i = 0; i < BloodNinja.OUTFITS; i++) {
                    BloodNinja n = BloodSect.BLOOD_NINJA.get().create(level);
                    n.moveTo(-3.5 + i * 2.0, -60, 0.5, 180, 0);
                    n.setYHeadRot(180);
                    n.yBodyRot = 180;
                    n.setNoAi(true);
                    n.setGuardPost(n.blockPosition());
                    // Each rank in another armour: the four, and one without.
                    n.setLook(i, (i + 1) % BloodNinja.ARMOURS);
                    level.addFreshEntity(n);
                    row.add(n);
                }
            });
            if (ticks == 40) {
                camera = new Marker(EntityType.MARKER, mc.level);
                place(0.5, -58.6, -5.4, 0, 8);
                mc.options.fov().set(60);
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            if (ticks == 60) capture(mc, "01-ranks-front.png");
            if (ticks == 62) server.execute(() -> row.forEach(n -> { n.setYRot(0); n.setYHeadRot(0); n.yBodyRot = 0; }));
            if (ticks == 80) capture(mc, "02-ranks-back.png");
            if (ticks == 82) server.execute(() -> {
                for (int i = 0; i < row.size(); i++) {
                    BloodNinja n = row.get(i);
                    n.setYRot(180); n.setYHeadRot(180); n.yBodyRot = 180;
                    n.setLook(i, (i + 3) % BloodNinja.ARMOURS);
                }
            });
            if (ticks == 100) capture(mc, "03-armours-swapped.png");
            // A close look at the middle one, three-quarter, then its dao swing at a cow.
            if (ticks == 102) {
                place(2.2, -58.8, -1.8, 35, 12);
                server.execute(() -> {
                    var level = server.overworld();
                    for (int i = 0; i < row.size(); i++) if (i != 2) row.get(i).discard();
                    cow = EntityType.COW.create(level);
                    cow.moveTo(0.5, -60, -2.0);
                    cow.setNoAi(true);
                    cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
                    level.addFreshEntity(cow);
                });
            }
            if (ticks == 120) capture(mc, "04-close.png");
            if (ticks == 125) server.execute(() -> {
                BloodNinja n = row.get(2);
                n.setOnGround(true);
                n.setTarget(cow);
                var goal = (MeleeAttackGoal) n.goalSelector.getAvailableGoals().stream().map(w -> w.getGoal()).filter(g -> g instanceof MeleeAttackGoal).findFirst().orElseThrow();
                goal.start();
                goal.tick();
            });
            if (ticks >= 126 && ticks <= 150 && ticks % 2 == 0) capture(mc, String.format("05-swing-%02d.png", ticks - 126));
            if (ticks == 160) {
                Files.writeString(mc.gameDirectory.toPath().resolve("cultist-look-tour-report.txt"), "PASSED; looks=" + row.stream().map(n -> n.getOutfit() + "/" + n.getArmour()).toList() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Blood Cultist look tour failed", e);
            finished = true;
            mc.stop();
        }
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
