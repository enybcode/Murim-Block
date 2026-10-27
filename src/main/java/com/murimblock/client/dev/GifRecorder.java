package com.murimblock.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.Murimblock;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Records the engine test's scenes as frames, to make GIFs of them ({@code -PengineGifs=true}, then
 * {@code tools/gifs.sh}): while a scene records, the frame is kept {@link #FPS} times a second of game time, scaled to
 * {@link #WIDTH} pixels, in {@code screenshots/gif/<scene>/}. A scene may also set a camera of its own (a side view of
 * a fight), laid over vanilla's by {@code CameraMixin}.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class GifRecorder {
    public static final boolean ON = Boolean.getBoolean("murimblock.engineTest")
            && Boolean.getBoolean("murimblock.engineGifs");
    private static final int FPS = 25;
    private static final int WIDTH = 640;
    private static final ExecutorService WRITER = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "engine-test-gif");
        thread.setDaemon(true);
        return thread;
    });

    /** Where the camera stands and what it looks at, this frame. */
    public record View(Vec3 eye, Vec3 target) {
    }

    private static String scene;
    private static File folder;
    private static int ticks;
    private static int frames;
    private static float crop = 1;
    private static Function<Float, View> camera;

    private GifRecorder() {
    }

    /** Starts recording {@code name} (a file name), with vanilla's camera unless {@link #camera} sets one. */
    static void start(String name) {
        start(name, 1);
    }

    /** Starts recording {@code name}, keeping only the middle {@code crop} of the window (1 for all of it). */
    static void start(String name, float crop) {
        if (!ON) {
            return;
        }
        stop();
        scene = name.replaceAll("[^a-z0-9_]+", "_");
        folder = new File(Minecraft.getInstance().gameDirectory, "screenshots/gif/" + scene);
        File[] old = folder.listFiles();
        if (old != null) {
            for (File file : old) {
                file.delete();
            }
        }
        folder.mkdirs();
        ticks = 0;
        frames = 0;
        // No tutorial hint or toast over the scene.
        Minecraft mc = Minecraft.getInstance();
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        mc.getToasts().clear();
        GifRecorder.crop = crop;
        Murimblock.LOGGER.info("Engine test gif: recording {}", scene);
    }

    static void stop() {
        if (scene != null) {
            Murimblock.LOGGER.info("Engine test gif: {} recorded, {} frames", scene, frames);
        }
        scene = null;
        camera = null;
    }

    static boolean recording() {
        return scene != null;
    }

    /** The camera of the scene being recorded, from the partial tick; null for vanilla's. */
    static void camera(Function<Float, View> view) {
        camera = ON ? view : null;
    }

    /**
     * A side view of {@code a} and the nearest other living body (its foe): from their left, far enough to see both,
     * looking at their middle.
     */
    static Function<Float, View> side(Entity a) {
        return partialTick -> {
            Vec3 from = a.getPosition(partialTick);
            Entity b = foe(a);
            Vec3 to = b == null ? from.add(a.getViewVector(partialTick).multiply(1.5, 0, 1.5)) : b.getPosition(partialTick);
            double height = b == null ? 1.8 : Math.max(1.8, b.getBbHeight());
            Vec3 middle = from.add(to).scale(0.5).add(0, height * 0.45, 0);
            Vec3 along = to.subtract(from).multiply(1, 0, 1);
            double span = along.length();
            along = span < 1.0E-2 ? new Vec3(0, 0, 1) : along.scale(1 / span);
            Vec3 left = new Vec3(along.z, 0, -along.x);
            double distance = 2.8 + 0.9 * Math.max(span, height);
            return new View(middle.add(left.scale(distance)).add(0, 0.5, 0), middle);
        };
    }

    private static Entity foe(Entity a) {
        Entity best = null;
        double nearest = 144;
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity != a && entity instanceof LivingEntity living && living.isAlive() && !(entity instanceof Player)) {
                double distance = entity.distanceToSqr(a);
                if (distance < nearest) {
                    nearest = distance;
                    best = entity;
                }
            }
        }
        return best;
    }

    public static View view(float partialTick) {
        return camera == null ? null : camera.apply(partialTick);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (scene != null && !Minecraft.getInstance().isPaused()) {
            ticks++;
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (scene == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float time = ticks + mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (time * FPS / 20f < frames) {
            return;
        }
        NativeImage small;
        try (NativeImage shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int w = Math.round(shot.getWidth() * crop), h = Math.round(shot.getHeight() * crop);
            int height = Math.round(WIDTH * h / (float) w) & ~1;
            small = new NativeImage(WIDTH, height, false);
            shot.resizeSubRectTo((shot.getWidth() - w) / 2, (shot.getHeight() - h) / 2, w, h, small);
        }
        File file = new File(folder, String.format("%04d.png", frames++));
        WRITER.execute(() -> {
            try {
                small.writeToFile(file);
            } catch (IOException e) {
                Murimblock.LOGGER.error("Engine test gif: could not save {}", file, e);
            } finally {
                small.close();
            }
        });
    }
}
