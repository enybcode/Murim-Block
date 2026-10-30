package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.SkinnedModel;
import com.murimblock.shenfa.client.SkeletalModels;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
            new Step("world", mc -> {
                check("the player is in the world", mc.player != null && mc.level != null);
                // A fresh world spawns the player anywhere: bring it over the test area first, so the test players added
                // next land in loaded chunks (the client drops entities added where nothing is loaded).
                // No effect left from an earlier run (the tour eats a golden apple): its particles would cloud the shots.
                server(mc, player -> {
                    player.getAbilities().flying = true;
                    player.removeAllEffects();
                    player.onUpdateAbilities();
                    player.connection.teleport(0.5, 200, -4, 0, 0);
                });
            }),
            new Step("skeletal models", mc -> {
                for (var id : List.of(SkeletalModels.BIPED, SkeletalModels.BIPED_SLIM)) {
                    SkinnedModel model = SkeletalModels.get(id);
                    check(id + " is loaded", model != null);
                    check(id + " fits the biped skeleton", model != null && Biped.check(model.armature()).isEmpty());
                    check(id + " has its 22 parts (12 skin, 10 armor)", model != null && model.parts().size() == 22);
                }
            })), groups().flatMap(List::stream)).toList();

    /**
     * The groups of checks, by name; {@code -PengineGroups=player,vanilla} (system property
     * {@code murimblock.engineGroups}) runs only those, all when it is empty.
     */
    private static Stream<List<Step>> groups() {
        Map<String, Supplier<List<Step>>> groups = new LinkedHashMap<>();
        groups.put("render", SkeletalRenderChecks::steps);
        groups.put("clips", ClipChecks::steps);
        groups.put("motions", MotionChecks::steps);
        groups.put("vanilla", VanillaChecks::steps);
        groups.put("player", PlayerChecks::steps);
        groups.put("events", EventChecks::steps);
        groups.put("combat", CombatChecks::steps);
        groups.put("film", FilmChecks::steps);
        groups.put("perf", PerfChecks::steps);
        groups.put("cloth", ClothChecks::steps);
        groups.put("mobs", MobLookChecks::steps);
        groups.put("moine", MoineChecks::steps);
        groups.put("ground", GroundChecks::steps);
        groups.put("golem", GolemChecks::steps);
        groups.put("witch", WitchChecks::steps);
        groups.put("meditation", MeditationChecks::steps);
        groups.put("trial", TrialChecks::steps);
        String wanted = System.getProperty("murimblock.engineGroups", "");
        Set<String> only = wanted.isBlank() ? groups.keySet() : Set.of(wanted.split(","));
        return groups.entrySet().stream().filter(entry -> only.contains(entry.getKey()))
                .map(entry -> entry.getValue().get());
    }

    /** While this says so (a scripted run playing out tick by tick), the next step waits. */
    static BooleanSupplier busy = () -> false;
    /** The step being run, by name. */
    static String current = "";

    private static boolean started;
    private static int frames = -1;
    private static int step;
    private static int checks;
    private static int failures;
    private static boolean finished;

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
        if (busy.getAsBoolean()) {
            return;
        }
        if (++frames < WORLD_FRAMES || (frames - WORLD_FRAMES) % STEP_FRAMES != 0) {
            return;
        }
        if (step < STEPS.size()) {
            Step next = STEPS.get(step++);
            current = next.name();
            Murimblock.LOGGER.info("Engine test: step {}", current);
            next.run().accept(mc);
            return;
        }
        if (finished) {
            return;
        }
        finished = true;
        Murimblock.LOGGER.info("Engine test: done, {} checks, {} failed", checks, failures);
        // Saving a world full of test mobs has hung the shutdown before: a scripted run must end anyway.
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                return;
            }
            Murimblock.LOGGER.warn("Engine test: the game did not stop in a minute, halting");
            Runtime.getRuntime().halt(0);
        }, "engine-test-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
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
