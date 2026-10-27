package com.murimblock.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.Murimblock;
import com.murimblock.anim.Animation;
import com.murimblock.anim.client.SkeletalClips;
import com.murimblock.anim.client.SkeletalPlayers;
import com.murimblock.client.dev.EngineTest.Step;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Step 4 in game: the test clips exported from Blender are loaded, play in time and loop, survive a resource reload,
 * and are filmed frame by frame from the three views of tools/blender/render.py (front, left side, three quarters),
 * to put next to Blender's render of the same clip.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class ClipChecks {
    private static final ResourceLocation BOUNCE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "test/bounce");
    private static final ResourceLocation WAVE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "test/wave");
    private static final double X = 0.5, Y = 200, Z = 0.5;
    /** Seconds between two filmed frames: two ticks, as render.py's default step. */
    private static final float FILM_STEP = 0.1f;

    /** A camera: which way the player faces (the camera looks south at it) and the camera's height and pitch. */
    private record View(String name, float faces, double dy, float pitch) {
    }

    private static final List<View> VIEWS = List.of(
            new View("front", 180, -0.6, 0),
            new View("side", -90, -0.6, 0),
            new View("three_quarter", -135, -0.3, 6));

    private static SkeletalRenderChecks.Dummy actor;
    private static final Queue<Float> film = new ArrayDeque<>();
    private static String filmName;
    private static int filmFrame;
    private static boolean shotPending;
    /** Frames to wait before filming, for the camera teleport to reach the client. */
    private static int wait;
    private static float timeBefore;

    private ClipChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("clips", mc -> {
            Animation bounce = SkeletalClips.get(BOUNCE), wave = SkeletalClips.get(WAVE);
            EngineTest.check("the test clips are loaded", bounce != null && wave != null);
            EngineTest.check("bounce lasts 1 s and loops", bounce != null && Math.abs(bounce.clip().duration() - 1) < 1e-4
                    && bounce.loop());
            EngineTest.check("wave lasts 2 s", wave != null && Math.abs(wave.clip().duration() - 2) < 1e-4);
            SkeletalRenderChecks.clearLineup(mc);
            actor = new SkeletalRenderChecks.Dummy(mc, 15, 50);
            mc.level.addEntity(actor);
            SkeletalPlayers.enable(true);
            SkeletalPlayers.slowMotion(1);
            EngineTest.check("/skeletal anim play finds a clip", SkeletalPlayers.play(BOUNCE, 1));
            EngineTest.check("and refuses an unknown one", !SkeletalPlayers.play(
                    ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "test/nothing"), 1));
            timeBefore = SkeletalPlayers.player().time();
        }));
        steps.add(new Step("clips play in time", mc -> {
            // A second of ticks has gone by: the looping clip is still on, back near where it started.
            float time = SkeletalPlayers.player().time();
            EngineTest.check("bounce still plays after a second, looped (" + time + " s)",
                    SkeletalPlayers.player().playing() && time >= 0 && time < 1);
            SkeletalPlayers.slowMotion(0.25f);
            timeBefore = time;
        }));
        steps.add(new Step("slow motion", mc -> {
            float moved = SkeletalPlayers.player().time() - timeBefore;
            if (moved < 0) {
                moved += 1;
            }
            // A quarter speed for a second: about 0.25 s of clip (the steps are frame counted, so give some room).
            EngineTest.check("slow motion at 0.25 moves a quarter as fast (" + moved + " s)", moved > 0.1 && moved < 0.4);
            SkeletalPlayers.slowMotion(1);
        }));
        for (ResourceLocation clip : List.of(BOUNCE, WAVE)) {
            for (View view : VIEWS) {
                String name = clip.getPath().substring(clip.getPath().indexOf('/') + 1) + "_" + view.name();
                steps.add(new Step("film " + name, mc -> {
                    place(mc, view);
                    SkeletalPlayers.play(clip, 1);
                    SkeletalPlayers.slowMotion(0);
                    float duration = SkeletalClips.get(clip).clip().duration();
                    for (int frame = 0; frame * FILM_STEP < duration - 1e-4f; frame++) {
                        film.add(frame * FILM_STEP);
                    }
                    filmName = name;
                    filmFrame = 0;
                    wait = 10;
                    shotPending = false;
                }));
            }
        }
        steps.add(new Step("reload", mc -> {
            EngineTest.check("every filmed frame is saved", film.isEmpty());
            SkeletalPlayers.stop();
            mc.reloadResourcePacks();
        }));
        steps.add(new Step("wait for the reload", mc -> {
        }));
        steps.add(new Step("after the reload", mc -> {
            EngineTest.check("the clips load again after a resource reload", SkeletalClips.get(BOUNCE) != null
                    && SkeletalClips.get(WAVE) != null);
            SkeletalPlayers.slowMotion(1);
            SkeletalPlayers.enable(false);
        }));
        return steps;
    }

    private static void place(Minecraft mc, View view) {
        actor.moveTo(X, Y, Z, view.faces(), 0);
        actor.setOldPosAndRot();
        actor.yBodyRot = actor.yBodyRotO = actor.yHeadRot = actor.yHeadRotO = view.faces();
        EngineTest.server(mc, player -> player.connection.teleport(X, Y + view.dy(), Z - 3, 0, view.pitch()));
    }

    /** Films one frame per rendered frame: the frame drawn after a seek is saved, then the next time is set. */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (filmName == null) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (shotPending) {
            save(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()), filmName + "/frame_" + String.format("%03d", filmFrame++));
            shotPending = false;
        }
        Float next = film.poll();
        if (next == null) {
            filmName = null;
            return;
        }
        SkeletalPlayers.player().seek(next);
        shotPending = true;
    }

    private static void save(Minecraft mc, NativeImage image, String name) {
        File file = new File(mc.gameDirectory, "screenshots/step4/" + name + ".png");
        file.getParentFile().mkdirs();
        try (image) {
            image.writeToFile(file);
        } catch (IOException e) {
            Murimblock.LOGGER.error("Engine test: could not save {}", file, e);
        }
    }
}
