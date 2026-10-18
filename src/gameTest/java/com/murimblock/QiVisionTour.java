package com.murimblock;

import com.mojang.logging.LogUtils;
import com.murimblock.client.cultivation.QiVisionClient;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Opt-in real client look at the mock-up of Qi vision (QiVisionClient): a player at Qi Liquidation in a fresh Murim world,
 * a zombie, a cow, a sheep, a mountain bandit and a Warden (a great Qi) before them, a vein of Qi further on; the view as it is, then with Qi
 * Sense open (the ink wash, the glowing living, the currents running to the vein), and turned toward the vein. Never
 * included in the player JAR. Run: gradlew -PgameTests runQiVisionTour; pictures in build/qi-vision-tour-run/screenshots.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiVisionTour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean creating, finished;
    private static int ticks;
    private static Vec3 eye;
    private static String debug = "";

    private QiVisionTour() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("murimblock.qiVisionTour") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        try {
            if (mc.player == null || mc.getSingleplayerServer() == null) {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen(false));
                if (!creating && mc.screen instanceof TitleScreen) {
                    creating = true;
                    var settings = new LevelSettings("qi-vision", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("qi-vision-" + System.currentTimeMillis(), settings, new WorldOptions(7L, false, false),
                            r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(), mc.screen);
                }
                return;
            }
            ticks++;
            var server = mc.getSingleplayerServer();
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.options.hideGui = true;
            mc.getToasts().clear();
            if (ticks == 40) server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime(5000);
                level.setWeatherParameters(1000000, 0, false, false);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                BlockPos at = p.blockPosition();
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
                // A clearing: everything above the ground cleared round the place, the woods left as a backdrop.
                for (int dx = -14; dx <= 14; dx++) for (int dz = -6; dz <= 22; dz++) {
                    int gx = at.getX() + dx, gz = at.getZ() + dz, g = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);
                    for (int yy = g; yy < g + 16; yy++) level.setBlock(new BlockPos(gx, yy, gz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    // Level ground: filled up or cut down to the place's own height.
                    int flat = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
                    for (int yy = flat - 3; yy < flat + 16; yy++) level.setBlock(new BlockPos(gx, yy, gz), yy < flat ? net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    while (g > level.getMinBuildHeight() && level.getBlockState(new BlockPos(gx, g - 1, gz)).is(net.minecraft.tags.BlockTags.LOGS)) level.setBlock(new BlockPos(gx, --g, gz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
                y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
                p.setGameMode(GameType.SURVIVAL);
                p.teleportTo(level, at.getX() + 0.5, y, at.getZ() + 0.5, 0, 12);
                CultivationService.setCultivation(p, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
                QiService.setQiMax(p, 1500);
                QiService.refillQi(p);
                // Before them (facing +z): a zombie, a cow, a sheep, a bandit; further on, a vein of Qi.
                place(level, EntityType.ZOMBIE, at, -3.0, 7);
                place(level, EntityType.COW, at, -6.0, 9);
                place(level, EntityType.SHEEP, at, 7.5, 11);
                place(level, com.murimblock.mob.MurimEntities.MOUNTAIN_BANDIT.get(), at, -0.5, 12);
                // A creature of great Qi: the Warden, read as a realm above.
                place(level, EntityType.WARDEN, at, 3.0, 8);
                int vy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX() + 1, at.getZ() + 16);
                QiVisionClient.veins.add(new Vec3(at.getX() + 1.5, vy, at.getZ() + 16.5));
            });
            if (ticks == 120) capture(mc, "01-as-it-is.png");
            if (ticks == 122) server.execute(() -> QiSense.setActive(server.getPlayerList().getPlayers().get(0), true));
            if (ticks >= 170 && ticks < 210) capture(mc, String.format("gif/frame-%03d.png", ticks - 170));
            if (ticks == 190) capture(mc, "02-qi-vision.png");
            if (ticks == 212) { mc.player.setYRot(0); mc.player.setXRot(6); }
            if (ticks == 240) capture(mc, "03-toward-the-vein.png");
            // From far off, behind the woods: the Warden's aura climbs over the trees.
            if (ticks == 242) server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(server.overworld(), p.getX() + 3, p.getY() + 12, p.getZ() - 50, 0, -12);
            });
            if (ticks == 300) {
                capture(mc, "04-from-afar.png");
                var w = mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.warden.Warden.class, mc.player.getBoundingBox().inflate(120));
                debug = "wardens seen=" + w.size() + (w.isEmpty() ? "" : " at " + Math.round(w.get(0).distanceTo(mc.player)) + " blocks, read=" + com.murimblock.client.cultivation.QiSenseClient.reading(w.get(0).getId()).isPresent()) + ", sense open=" + com.murimblock.client.cultivation.QiSenseClient.open();
            }
            // The same view, weaker then far stronger: the sense reaches less far, then much further.
            if (ticks == 302) server.execute(() -> CultivationService.setCultivation(server.getPlayerList().getPlayers().get(0), CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY));
            if (ticks == 340) capture(mc, "05-qi-condensation.png");
            if (ticks == 342) server.execute(() -> CultivationService.setCultivation(server.getPlayerList().getPlayers().get(0), CultivationRealm.CORE_FORMATION, CultivationStage.EARLY));
            if (ticks == 380) capture(mc, "06-core-formation.png");
            // Back near the Warden, two realms below it: the view trembles under its pressure (filmed).
            if (ticks == 382) server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                CultivationService.setCultivation(p, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
                p.teleportTo(server.overworld(), p.getX(), p.getY() - 12, p.getZ() + 50 - 2, 0, 8);
            });
            if (ticks >= 420 && ticks < 460) capture(mc, String.format("shake/frame-%03d.png", ticks - 420));
            if (ticks == 461) debug += String.format(", pressure %.2f", com.murimblock.client.cultivation.QiSenseClient.pressure());
            if (ticks == 470) {
                Files.writeString(mc.gameDirectory.toPath().resolve("qi-vision-tour-report.txt"), "PASSED; effect=" + (mc.gameRenderer.currentEffect() != null) + "; " + debug + "\n");
                finished = true;
                mc.stop();
            }
        } catch (Exception e) {
            LOGGER.error("Qi vision tour failed", e);
            finished = true;
            mc.stop();
        }
    }

    private static void place(ServerLevel level, EntityType<? extends Mob> type, BlockPos at, double dx, double dz) {
        Mob mob = type.create(level);
        if (mob == null) return;
        int x = (int) Math.floor(at.getX() + dx), z = (int) Math.floor(at.getZ() + dz);
        mob.moveTo(at.getX() + dx + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), at.getZ() + dz + 0.5, 180, 0);
        mob.setNoAi(true);
        // A helmet, so the zombie does not burn in the sun.
        mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
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
