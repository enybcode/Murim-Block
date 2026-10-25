package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.client.innerworld.TrialEntry;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
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
 * saves one shot per tick to {@code screenshots/rise/}, then quits. For tuning {@code trial_rise.json} quickly.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RiseFilm {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.riseFilm");
    private static final String WORLD = "murim-rise-film";
    private static final int SETTLE = 80;
    private static final int FRAMES = 50;

    private static boolean started;
    private static int ticks = -1;

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
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -1, -4), center.offset(4, -1, 4))) {
                    player.serverLevel().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.connection.teleport(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 12);
            });
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.options.hideGui = true;
            new File(mc.gameDirectory, "screenshots/rise").mkdirs();
        }
        // Seen from the side: the body faces east while the camera looks south.
        mc.player.setYBodyRot(mc.player.getYRot() - 90);
        mc.player.yBodyRotO = mc.player.yBodyRot;
        mc.player.setYHeadRot(mc.player.getYRot() - 90);
        mc.player.yHeadRotO = mc.player.yHeadRot;
        int frame = ticks - SETTLE;
        if (frame == 0 && PlayerAnimationAccess.getPlayerAnimationLayer(mc.player, ClientCultivation.LAYER)
                instanceof PlayerAnimationController controller) {
            controller.triggerAnimation(TrialEntry.RISE);
        }
        if (frame >= 0 && frame < FRAMES) {
            Screenshot.grab(mc.gameDirectory, String.format("rise/murim_rise_%02d.png", frame), mc.getMainRenderTarget(),
                    message -> { });
        }
        if (frame == FRAMES + 5) {
            Murimblock.LOGGER.info("Rise film: done");
            mc.stop();
        }
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
