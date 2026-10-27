package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.shenfa.client.SkeletalPlayerRenderer;
import com.murimblock.shenfa.client.SkeletalPlayers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;

/**
 * Shenfa's cost in game: 50 animated players in view (walking, some in the boxing stance), the time each takes to
 * draw (posing, feet, skinning, layers) and the frame rate, against the same view with Shenfa off (vanilla's
 * players).
 */
final class PerfChecks {
    private static final double X = 0.5, Y = 200, Z = 60.5;
    private static final int COUNT = 50;
    private static final List<SkeletalRenderChecks.Dummy> crowd = new ArrayList<>();
    private static int fpsShenfa;

    private PerfChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("perf setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            SkeletalRenderChecks.clearLineup(mc);
            SkeletalPlayers.enable(true);
            for (int i = 0; i < COUNT; i++) {
                SkeletalRenderChecks.Dummy dummy = new SkeletalRenderChecks.Dummy(mc, 30 + i, 300 + i);
                double x = X - 6 + (i % 10) * 1.3, z = Z + 5 + (i / 10) * 2.2;
                dummy.moveTo(x, Y, z, 180, 0);
                dummy.setOldPosAndRot();
                dummy.moveX = i % 3 == 0 ? 0 : 0.2;
                mc.level.addEntity(dummy);
                crowd.add(dummy);
            }
            EngineTest.server(mc, player -> {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.connection.teleport(X, Y + 1, Z - 2, 0, 15);
            });
        }));
        if (GifRecorder.ON) {
            steps.add(new Step("perf crowd recorded", mc -> GifRecorder.start("perf_crowd_of_50")));
            for (int i = 0; i < 3; i++) {
                steps.add(new Step("perf crowd recorded", mc -> {
                }));
            }
        }
        steps.add(new Step("perf warm up", mc -> {
            GifRecorder.stop();
            SkeletalPlayerRenderer.resetTiming();
        }));
        steps.add(new Step("perf with Shenfa", mc -> {
            double micros = SkeletalPlayerRenderer.averageMicros();
            fpsShenfa = mc.getFps();
            Murimblock.LOGGER.info("Engine test perf: {} players, {} us each to draw, {} fps", COUNT,
                    String.format("%.0f", micros), fpsShenfa);
            EngineTest.check(String.format("50 Shenfa players draw in %.0f us each (under 500)", micros),
                    micros > 0 && micros < 500);
            SkeletalPlayers.enable(false);
        }));
        steps.add(new Step("perf vanilla", mc -> {
        }));
        steps.add(new Step("perf done", mc -> {
            int fpsVanilla = mc.getFps();
            Murimblock.LOGGER.info("Engine test perf: the same view with vanilla's players: {} fps (Shenfa {})",
                    fpsVanilla, fpsShenfa);
            EngineTest.check("the frame rate with Shenfa is at least half of vanilla's (" + fpsShenfa + " / "
                    + fpsVanilla + ")", fpsShenfa * 2 >= fpsVanilla);
            crowd.forEach(dummy -> mc.level.removeEntity(dummy.getId(),
                    net.minecraft.world.entity.Entity.RemovalReason.DISCARDED));
            crowd.clear();
        }));
        return steps;
    }
}
