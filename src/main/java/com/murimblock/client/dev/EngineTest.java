package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.anim.Biped;
import com.murimblock.anim.SkinnedModel;
import com.murimblock.anim.client.SkeletalModels;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Dev tool for the combat engine, off unless {@code murimblock.engineTest} is set (run {@code gradlew runEngineTest}):
 * opens a fresh world, runs each step of {@link #STEPS} a second apart, logs PASS or FAIL for each check and a
 * summary, saves shots to run/engine-test/screenshots, then quits. Each roadmap step adds its checks here.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class EngineTest {
    private static final boolean ENABLED = Boolean.getBoolean("murimblock.engineTest");
    private static final String WORLD = "murim-engine-test";
    private static final int WORLD_FRAMES = 120;
    private static final int STEP_FRAMES = 60;

    /** One step of the test: it runs on a frame, a second after the previous one. */
    record Step(String name, Consumer<Minecraft> run) {
    }

    private static final List<Step> STEPS = Stream.concat(Stream.of(
            new Step("world", mc -> check("the player is in the world", mc.player != null && mc.level != null)),
            new Step("skeletal models", mc -> {
                for (var id : List.of(SkeletalModels.BIPED, SkeletalModels.BIPED_SLIM)) {
                    SkinnedModel model = SkeletalModels.get(id);
                    check(id + " is loaded", model != null);
                    check(id + " fits the biped skeleton", model != null && Biped.check(model.armature()).isEmpty());
                    check(id + " has its 22 parts (12 skin, 10 armor)", model != null && model.parts().size() == 22);
                }
            })), Stream.of(SkeletalRenderChecks.steps(), ClipChecks.steps(), MotionChecks.steps())
                    .flatMap(List::stream)).toList();

    private static boolean started;
    private static int frames = -1;
    private static int step;
    private static int checks;
    private static int failures;

    private EngineTest() {
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            // A fresh game folder opens on the accessibility screen first.
            if (mc.options.onboardAccessibility) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            } else if (mc.screen instanceof TitleScreen) {
                started = true;
                frames = -1;
                openWorld(mc);
            }
            return;
        }
        if (mc.player == null || mc.screen != null) {
            return;
        }
        if (++frames < WORLD_FRAMES || (frames - WORLD_FRAMES) % STEP_FRAMES != 0) {
            return;
        }
        if (step < STEPS.size()) {
            Step current = STEPS.get(step++);
            Murimblock.LOGGER.info("Engine test: step {}", current.name());
            current.run().accept(mc);
            return;
        }
        Murimblock.LOGGER.info("Engine test: done, {} checks, {} failed", checks, failures);
        mc.stop();
    }

    static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
        }
        Murimblock.LOGGER.info("Engine test: {} {}", ok ? "PASS" : "FAIL", what);
    }

    /** Runs {@code action} on the server thread with the test player. */
    static void server(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> action.accept(server.getPlayerList().getPlayer(id)));
    }

    static void shoot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                message -> Murimblock.LOGGER.info("Engine test shot: {}", message.getString()));
    }

    private static void openWorld(Minecraft mc) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> Murimblock.LOGGER.error("Engine test: could not open {}", WORLD));
            return;
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(26102026L, false, false),
                WorldPresets::createNormalWorldDimensions, mc.screen);
    }
}
