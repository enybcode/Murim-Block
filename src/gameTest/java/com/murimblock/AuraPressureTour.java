package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.cultivation.QiSense;
import com.murimblock.qi.QiService;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
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
import org.slf4j.Logger;

/**
 * Opt-in real client look at the aura's pressure (AuraPressure): three husks walk in at a player at Qi Crystallization
 * (no pressure), then three more at Core Formation with 30 000 Qi held and the Thunder Saber known: waves and flames of the aura, the husks
 * crushed to the ground, the ground breaking round the feet and space rippling as the player walks at them. Filmed from the side. Never included in the player JAR.
 * Run: gradlew -PgameTests runAuraPressureTour; frames in build/aura-pressure-tour-run/screenshots/pressure/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class AuraPressureTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;
    private static net.minecraft.world.entity.Marker camera;

    private AuraPressureTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.auraPressureTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("aura-pressure", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("aura-pressure-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            var server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.hideGui = true;
            mc.getToasts().clear();
            if (ticks == 20) server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                for (int x = -6; x <= 6; x++) for (int z = -3; z <= 16; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, 0.5, -60, 0.5, 0, 10);
                CultivationService.setCultivation(p, CultivationRealm.QI_CRYSTALLIZATION, CultivationStage.PEAK);
                com.murimblock.cultivation.AuraPressure.release(p, true);
                QiService.setQiMax(p, 5000);
                QiService.refillQi(p);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 120, 4, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 120, 4, false, false));
            });
            // Twice three husks walk in from 14 blocks: first at Qi Crystallization (no pressure), then at Foundation Establishment.
            if (ticks == 40 || ticks == 140) server.execute(() -> {
                ServerLevel level = server.overworld();
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                level.getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, p.getBoundingBox().inflate(30)).forEach(z -> z.discard());
                for (int i = -1; i <= 1; i++) {
                    var z = net.minecraft.world.entity.EntityType.HUSK.create(level);
                    z.moveTo(0.5 + i * 2, -60, 14.5, 180, 0);
                    z.setPersistenceRequired();
                    level.addFreshEntity(z);
                    z.setTarget(p);
                }
            });
            // Then Core Formation, 30 000 Qi held, the Thunder Saber known (its purple): the pressure, overflowing (the ground breaking, space rippling).
            if (ticks == 139) server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                CultivationService.setCultivation(p, CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
                // An art known, its colour in the aura: the Purple Heaven Thunder Saber.
                com.murimblock.martial.MartialService.learn(p, com.murimblock.martial.MartialArts.art(com.murimblock.martial.MartialArts.THUNDER_SABER));
                QiService.setQiMax(p, 30000);
                QiService.refillQi(p);
            });
            if (ticks == 41) {
                camera = new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityType.MARKER, mc.level);
                camera.moveTo(18.0, -52.5, 5.0, 90, 24);
                camera.xo = camera.xOld = 18.0; camera.yo = camera.yOld = -52.5; camera.zo = camera.zOld = 5.0;
                camera.yRotO = 90; camera.xRotO = 24;
            }
            if (camera != null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
            // Overflowing, the player walks at the husks a walking pace, moved by the server (with the marker camera the
            // client never answers the first teleport, so its own steps are not taken), then stops.
            if (ticks >= 150 && ticks < 200) server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(server.overworld(), p.getX(), -60, p.getZ() + 0.15, 0, 10);
                p.setOnGround(true);
            });
            if (ticks >= 44 && ticks < 240 && ticks % 2 == 0) capture(mc, String.format("pressure/frame-%03d.png", (ticks - 44) / 2));
            if (ticks == 245) {
                Files.writeString(mc.gameDirectory.toPath().resolve("aura-pressure-tour-report.txt"), "PASSED\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Aura pressure tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }
}
