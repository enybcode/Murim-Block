package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.registry.MurimBlocks;
import com.murimblock.shenfa.GroundFracture;
import com.murimblock.shenfa.client.GroundFractures;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The ground fracturing in game ({@code -PengineGroups=ground}): on a field of grass, flowers and a stone path, a
 * small blow (radius 2.5), then a great one (radius 6) a little further, filmed from above at three quarters; the
 * blocks break out of the world, then all come back as they were.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class GroundChecks {
    private static final int AREA = 880;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static Consumer<Integer> running;
    private static int at, length;
    private static Runnable after;

    private GroundChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("ground setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            EngineTest.server(mc, player -> {
                ServerLevel level = player.serverLevel();
                level.setDayTime(6000);
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 14, (int) Y - 3, -14, AREA + 14, (int) Y + 6, 22)) {
                    int depth = (int) Y - pos.getY();
                    level.setBlockAndUpdate(pos, depth == 1 ? (Math.abs(pos.getX() - AREA) <= 1 ? Blocks.COBBLESTONE
                            : Blocks.GRASS_BLOCK).defaultBlockState() : depth > 1 ? Blocks.DIRT.defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
                }
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 14, (int) Y, -14, AREA + 14, (int) Y, 22)) {
                    long h = pos.asLong() * 0x9E3779B97F4A7C15L >>> 59;
                    if (Math.abs(pos.getX() - AREA) > 1 && h < 6) {
                        level.setBlockAndUpdate(pos, (h < 4 ? Blocks.SHORT_GRASS : h < 5 ? Blocks.POPPY
                                : Blocks.DANDELION).defaultBlockState());
                    }
                }
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.connection.teleport(X + 6, Y + 4, Z - 3, 34, 24);
            });
        }));
        steps.add(new Step("ground settle", mc -> run(40, t -> {
        }, () -> {
        })));
        steps.add(new Step("ground fracture", mc -> {
            int[] most = {0};
            run(300, t -> {
                if (t == 0) {
                    GifRecorder.start("ground_fracture");
                }
                if (t == 10) {
                    EngineTest.server(mc, player -> GroundFracture.at(player.serverLevel(), new Vec3(X, Y, Z - 2), 2.5f));
                }
                if (t == 80) {
                    EngineTest.server(mc, player -> GroundFracture.at(player.serverLevel(), new Vec3(X, Y, Z + 6), 6));
                }
                most[0] = Math.max(most[0], GroundFractures.broken());
                if (t == 16 || t == 90 || t == 120) {
                    EngineTest.shoot(mc, "ground_fracture_" + t);
                }
            }, () -> {
                BlockPos centre = BlockPos.containing(X, Y - 1, Z + 6);
                EngineTest.check("the ground breaks round the blow (" + most[0] + " blocks at most)", most[0] >= 60);
                EngineTest.check("and every block comes back as it was", !GroundFractures.busy()
                        && mc.level.getBlockState(centre).is(Blocks.COBBLESTONE)
                        && BlockPos.betweenClosedStream(AREA - 8, (int) Y - 1, -8, AREA + 8, (int) Y - 1, 14)
                        .noneMatch(pos -> mc.level.getBlockState(pos).is(MurimBlocks.FRACTURED_GROUND.get())));
            });
        }));
        return steps;
    }

    private static void run(int ticks, Consumer<Integer> tick, Runnable end) {
        at = -10;
        length = ticks;
        running = tick;
        after = end;
        EngineTest.busy = () -> running != null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (running == null || mc.player == null || mc.isPaused()) {
            return;
        }
        if (at >= 0) {
            running.accept(at);
        }
        if (++at >= length) {
            running = null;
            GifRecorder.stop();
            after.run();
        }
    }
}
