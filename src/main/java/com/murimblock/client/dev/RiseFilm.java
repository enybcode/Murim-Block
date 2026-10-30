package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.innerworld.TrialEntry;
import com.murimblock.innerworld.TrialZombie;
import com.murimblock.registry.MurimEntities;
import java.io.File;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
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
 * Dev tool, off unless {@code murimblock.riseFilm} is set (run {@code gradlew runRiseFilm}): stands the player on a
 * platform in the sky, plays the trial's getting-up animation seen from the side (body turned, not the camera),
 * saves one shot per tick to {@code screenshots/rise/}, then shoots the Grave Warden from four sides (front, three
 * quarters, back, at night) once it is out of the mausoleum, and quits. For tuning the {@code trial/rise} clip and the
 * Warden's look quickly.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RiseFilm {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.riseFilm");
    private static final String WORLD = "murim-rise-film";
    private static final int SETTLE = 80;
    private static final int FRAMES = 66;

    private static boolean started;
    private static int ticks = -1;
    private static TrialZombie warden;

    private RiseFilm() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.options.onboardAccessibility) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            } else if (mc.screen instanceof TitleScreen) {
                started = true;
                openWorld(mc);
            }
            return;
        }
        if (mc.player == null || mc.screen != null) {
            return;
        }
        ticks++;
        if (ticks == 1) {
            MinecraftServer server = mc.getSingleplayerServer();
            UUID id = mc.player.getUUID();
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                BlockPos center = BlockPos.containing(player.getX(), 200, player.getZ());
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-12, -1, -12), center.offset(12, -1, 12))) {
                    player.serverLevel().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.connection.teleport(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 12);
                server.overworld().setDayTime(1000);
            });
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.hideGui = true;
            new File(mc.gameDirectory, "screenshots/rise").mkdirs();
        }
        // Seen three quarters from the side: the body turned 55 degrees from the camera, so both legs show.
        mc.player.setYBodyRot(mc.player.getYRot() - 55);
        mc.player.yBodyRotO = mc.player.yBodyRot;
        mc.player.setYHeadRot(mc.player.getYRot() - 55);
        mc.player.yHeadRotO = mc.player.yHeadRot;
        int frame = ticks - SETTLE;
        if (frame == 0) {
            TrialEntry.rise(mc.player);
        }
        if (frame >= 0 && frame < FRAMES) {
            Screenshot.grab(mc.gameDirectory, String.format("rise/murim_rise_%02d.png", frame), mc.getMainRenderTarget(),
                    message -> { });
        }
        warden(mc, frame - FRAMES);
    }

    /**
     * Then the Grave Warden, 5 blocks away, once out of the mausoleum (60 ticks): from the front, three quarters, the
     * back, and at night.
     */
    private static void warden(Minecraft mc, int step) {
        MinecraftServer server = mc.getSingleplayerServer();
        switch (step) {
            case 5 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                    player.connection.teleport(player.getX(), player.getY(), player.getZ(), 0, 4);
                    TrialZombie zombie = MurimEntities.TRIAL_ZOMBIE.get().create(player.serverLevel());
                    zombie.makeWarden();
                    zombie.setNoAi(true);
                    zombie.moveTo(player.getX(), player.getY(), player.getZ() + 5, 180, 0);
                    zombie.setYHeadRot(180);
                    zombie.setYBodyRot(180);
                    player.serverLevel().addFreshEntity(zombie);
                    warden = zombie;
                });
            }
            case 75 -> shoot(mc, "warden_front", 50);
            case 85 -> shoot(mc, "warden_three_quarters", 180);
            case 95 -> {
                shoot(mc, "warden_back", 0);
                server.execute(() -> server.overworld().setDayTime(18000));
            }
            case 115 -> shoot(mc, "warden_night", 0);
            case 120 -> {
                Murimblock.LOGGER.info("Rise film: done");
                mc.stop();
            }
            default -> {
            }
        }
    }

    /** Saves a shot, then walks the camera around the Warden to look at it from this angle (0 = its front). */
    private static void shoot(Minecraft mc, String name, float around) {
        Screenshot.grab(mc.gameDirectory, "rise/" + name + ".png", mc.getMainRenderTarget(), message -> { });
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            if (warden == null) {
                return;
            }
            // The Warden faces north (-z); its front is seen from the north.
            double angle = Math.toRadians(around);
            double x = warden.getX() + Math.sin(angle) * 5;
            double z = warden.getZ() - Math.cos(angle) * 5;
            float yaw = (float) Math.toDegrees(Math.atan2(-(warden.getX() - x), warden.getZ() - z));
            player.connection.teleport(x, player.getY(), z, yaw, 4);
        });
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> Murimblock.LOGGER.error("Rise film: could not open {}", WORLD));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(23102026L, false, false),
                WorldPresets::createNormalWorldDimensions, mc.screen);
    }
}
