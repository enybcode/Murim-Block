package com.murimblock.testing;

import com.murimblock.Murimblock;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Runs only some of the GameTests, while one piece is being worked on (Enzo, 17/10/2026: the whole suite grows with the
 * mod; it still runs whole before anything is pushed). gradlew -PgameTests -PgameTestFilter=sevenstars,bloodsoldier
 * runGameTestServer keeps the tests whose name or batch holds one of the words (case aside); without it, all run.
 * Done as the server is about to start, before it groups its tests into batches (GameTestServer.initServer). A filtered run
 * can hang while the server saves its world on the way out: once it stops, the process is ended after a few seconds (the
 * results are in the log; a whole run is left as it is).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class GameTestFilter {
    private GameTestFilter() {
    }

    @SubscribeEvent
    public static void onAboutToStart(ServerAboutToStartEvent event) throws ReflectiveOperationException {
        String filter = System.getProperty("murimblock.gameTestFilter", "").trim();
        if (filter.isEmpty() || !(event.getServer() instanceof GameTestServer server)) return;
        List<String> words = Arrays.stream(filter.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(w -> !w.isEmpty()).toList();
        Field field = GameTestServer.class.getDeclaredField("testFunctions");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<TestFunction> tests = (List<TestFunction>) field.get(server);
        int before = tests.size();
        tests.removeIf(t -> words.stream().noneMatch(w -> t.testName().toLowerCase(Locale.ROOT).contains(w) || t.batchName().toLowerCase(Locale.ROOT).contains(w)));
        com.mojang.logging.LogUtils.getLogger().info("GameTest filter {}: {} of {} tests kept", words, tests.size(), before);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        if (System.getProperty("murimblock.gameTestFilter", "").isBlank() || !(event.getServer() instanceof GameTestServer)) return;
        Thread t = new Thread(() -> {
            try { Thread.sleep(8000); } catch (InterruptedException ignored) { }
            Runtime.getRuntime().halt(0);
        }, "murimblock-filtered-gametest-exit");
        t.setDaemon(true);
        t.start();
    }
}
