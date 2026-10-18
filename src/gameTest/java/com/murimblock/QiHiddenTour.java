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
import net.minecraft.world.entity.EntityType;
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
 * Opt-in real client look at Qi vision's hidden (QiTraces, QiHiddenClient): a bandit warlord walks a curve across the
 * ground and is gone; a chest with diamonds is buried, an empty barrel walled in (not felt). A player at Nascent Soul opens Qi vision: the warlord's
 * footprints and the caches' threads of Qi. Never included in the player JAR.
 * Run: gradlew -PgameTests runQiHiddenTour; frames in build/qi-hidden-tour-run/screenshots/hidden/.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiHiddenTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;
    private static Mob warlord;

    private QiHiddenTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.qiHiddenTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("qi-hidden", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("qi-hidden-" + System.currentTimeMillis(), settings, new WorldOptions(3L, false, false),
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
                for (int x = -8; x <= 8; x++) for (int z = -3; z <= 12; z++) level.setBlockAndUpdate(new BlockPos(x, -61, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                // A chest buried under the ground, a barrel walled in stone.
                level.setBlockAndUpdate(new BlockPos(3, -63, 7), Blocks.CHEST.defaultBlockState());
                // The chest holds something (felt); the barrel is empty (not felt).
                if (level.getBlockEntity(new BlockPos(3, -63, 7)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)
                    chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
                level.setBlockAndUpdate(new BlockPos(-4, -60, 9), Blocks.BARREL.defaultBlockState());
                for (BlockPos b : BlockPos.betweenClosed(-5, -60, 8, -3, -58, 10))
                    if (!b.equals(new BlockPos(-4, -60, 9))) level.setBlockAndUpdate(b, Blocks.COBBLESTONE.defaultBlockState());
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.teleportTo(level, 0.5, -60, -1.5, 0, 24);
                CultivationService.setCultivation(p, CultivationRealm.NASCENT_SOUL, CultivationStage.EARLY);
                QiService.setQiMax(p, 12000);
                QiService.refillQi(p);
                warlord = com.murimblock.mob.MurimEntities.BANDIT_WARLORD.get().create(level);
                warlord.moveTo(-7, -60, 2, -90, 0);
                warlord.setNoAi(true);
                warlord.setPersistenceRequired();
                level.addFreshEntity(warlord);
            });
            // The warlord walks a curve across, then is gone.
            if (ticks > 25 && ticks <= 85) {
                int k = ticks - 25;
                server.execute(() -> {
                    double x = -7 + k * 0.25, z = 2 + 5 * Math.sin(k / 60.0 * Math.PI);
                    float yaw = (float) Math.toDegrees(Math.atan2(-0.25, 5 * Math.PI / 60 * Math.cos(k / 60.0 * Math.PI)));
                    warlord.moveTo(x, -60, z, yaw, 0);
                    warlord.setOnGround(true);
                });
            }
            if (ticks == 88) server.execute(() -> warlord.discard());
            if (ticks == 95) capture(mc, "hidden/01-as-it-is.png");
            if (ticks == 97) server.execute(() -> QiSense.setActive(server.getPlayerList().getPlayers().get(0), true));
            if (ticks >= 120 && ticks < 160) capture(mc, String.format("hidden/frame-%03d.png", ticks - 120));
            if (ticks == 165) {
                Files.writeString(mc.gameDirectory.toPath().resolve("qi-hidden-tour-report.txt"), "PASSED; sense open="
                        + com.murimblock.client.cultivation.QiSenseClient.open() + "; footprints=" + com.murimblock.client.cultivation.QiHiddenClient.footprints()
                        + "; caches=" + com.murimblock.client.cultivation.QiHiddenClient.caches() + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Qi hidden tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x, double z) {
        Mob mob = type.create(level);
        mob.moveTo(x, -60, z, 180, 0);
        mob.setNoAi(true);
        mob.setYBodyRot(180);
        mob.setYHeadRot(180);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    private static void capture(Minecraft mc, String name) throws Exception {
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name);
        Files.createDirectories(file.getParent());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(file);
        }
    }
}
