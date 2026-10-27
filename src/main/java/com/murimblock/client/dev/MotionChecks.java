package com.murimblock.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import com.murimblock.Murimblock;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.LivingMotion;
import com.murimblock.shenfa.MotionSet;
import com.murimblock.shenfa.client.PlayerAnimators;
import com.murimblock.shenfa.client.SkeletalClips;
import com.murimblock.shenfa.client.SkeletalPlayers;
import com.murimblock.client.dev.EngineTest.Step;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/**
 * Step 5 in game: each player's animator reads the body (a test player moved by hand: standing, walking, running,
 * sneaking, falling), every living motion plays its clip from the motion set (shot from three quarters), combat mode
 * picks the stance, and a guard layered over walking and a jab over the stance are filmed from three sides.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
final class MotionChecks {
    private static final ResourceLocation GUARD = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "fists/guard");
    private static final ResourceLocation JAB = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "fists/jab");
    private static final double X = 0.5, Y = 200, Z = 0.5;
    /** Filmed frames: one every two ticks, as the Blender renders. */
    private static final int FILM_FRAMES = 16;

    private record View(String name, float faces, double dy, float pitch) {
    }

    private static final List<View> VIEWS = List.of(
            new View("front", 180, -0.6, 0),
            new View("side", -90, -0.6, 0),
            new View("three_quarter", -135, -0.3, 6));

    private static SkeletalRenderChecks.Dummy actor;
    private static String filmName;
    private static int filmFrame;
    private static long lastShotTick;
    private static int wait;
    private static Runnable everyShot;

    private MotionChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("motions", mc -> {
            MotionSet set = SkeletalClips.motions(PlayerAnimators.MOTIONS);
            EngineTest.check("the biped motion set is loaded", set != null);
            boolean all = set != null;
            for (LivingMotion motion : LivingMotion.values()) {
                for (boolean combat : new boolean[] {false, true}) {
                    String id = set == null ? null : set.clip(motion, combat);
                    all &= id != null && SkeletalClips.get(ResourceLocation.parse(id)) != null;
                }
            }
            EngineTest.check("every motion, in and out of combat, has a loaded clip", all);
            actor = new SkeletalRenderChecks.Dummy(mc, 15, 60);
            actor.setOnGround(true);
            mc.level.addEntity(actor);
            place(mc, VIEWS.get(2));
            SkeletalPlayers.pose(SkeletalPlayers.ANIMATED);
            SkeletalPlayers.enable(true);
            PlayerAnimators.force(null);
        }));
        // The body read: the test player does not move by itself, so its last position is set by hand.
        read(steps, "standing", LivingMotion.IDLE, () -> actor.moveX = 0);
        read(steps, "walking", LivingMotion.WALK, () -> actor.moveX = 0.2);
        read(steps, "running", LivingMotion.RUN, () -> actor.setSprinting(true));
        read(steps, "sneaking", LivingMotion.SNEAK_WALK, () -> {
            actor.setSprinting(false);
            actor.setPose(Pose.CROUCHING);
        });
        read(steps, "falling", LivingMotion.FALL, () -> {
            actor.setPose(Pose.STANDING);
            actor.moveX = 0;
            actor.setOnGround(false);
            actor.fallDistance = 3;
            actor.moveY = -0.5;
        });
        steps.add(new Step("back on the ground", mc -> {
            actor.setOnGround(true);
            actor.fallDistance = 0;
            actor.moveY = 0;
        }));
        // Each motion forced, shot after its blend.
        for (LivingMotion motion : LivingMotion.values()) {
            steps.add(new Step("motion " + motion.key(), mc -> PlayerAnimators.force(motion)));
            steps.add(new Step("motion " + motion.key() + " (shot)", mc -> {
                Animation base = PlayerAnimators.baseAnimation(actor);
                String expected = SkeletalClips.motions(PlayerAnimators.MOTIONS).clip(motion, false);
                EngineTest.check(motion.key() + " plays " + expected, base != null
                        && ("murimblock:" + base.id()).equals(expected));
                save(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()), "motions/" + motion.key());
            }));
        }
        steps.add(new Step("combat", mc -> {
            PlayerAnimators.force(LivingMotion.IDLE);
            PlayerAnimators.combat(true);
        }));
        steps.add(new Step("combat (check)", mc -> {
            Animation base = PlayerAnimators.baseAnimation(actor);
            EngineTest.check("combat mode stands in the boxing stance", base != null && base.id().equals("fists/stance"));
            PlayerAnimators.combat(false);
        }));
        // Walking with the guard on: the guard takes the arms only, the legs keep walking.
        for (View view : VIEWS) {
            steps.add(new Step("guard walk " + view.name(), mc -> {
                place(mc, view);
                PlayerAnimators.force(LivingMotion.WALK);
                SkeletalPlayers.play(GUARD, Animator.Priority.MIDDLE, 1, false);
                film("guard_walk_" + view.name(), null);
            }));
            waitForFilm(steps);
        }
        steps.add(new Step("guard walk (check)", mc -> {
            EngineTest.check("every frame of the guard walk is saved", filmName == null);
            SkeletalPlayers.stop(Animator.Priority.MIDDLE);
            PlayerAnimators.force(LivingMotion.IDLE);
            PlayerAnimators.combat(true);
        }));
        // A jab out of the stance, again every 0.8 s.
        for (View view : VIEWS) {
            steps.add(new Step("jab " + view.name(), mc -> {
                place(mc, view);
                int[] shots = {0};
                film("jab_" + view.name(), () -> {
                    if (shots[0]++ % 8 == 1) {
                        SkeletalPlayers.play(JAB, Animator.Priority.HIGH, 1, false);
                    }
                });
            }));
            waitForFilm(steps);
        }
        steps.add(new Step("end", mc -> {
            PlayerAnimators.combat(false);
            PlayerAnimators.force(null);
            SkeletalPlayers.enable(false);
            mc.level.removeEntity(actor.getId(), Entity.RemovalReason.DISCARDED);
        }));
        return steps;
    }

    /** A film takes 1.6 s; steps are a second apart at most (60 frames), often less. */
    private static void waitForFilm(List<Step> steps) {
        for (int i = 0; i < 4; i++) {
            steps.add(new Step("film (wait)", mc -> {
            }));
        }
    }

    private static void read(List<Step> steps, String name, LivingMotion expected, Runnable setup) {
        steps.add(new Step("read " + name, mc -> setup.run()));
        steps.add(new Step("read " + name + " (check)", mc -> {
            LivingMotion seen = PlayerAnimators.motion(actor);
            EngineTest.check("a " + name + " body plays " + expected.key() + " (" + seen.key() + ")", seen == expected);
        }));
    }

    private static void place(Minecraft mc, View view) {
        actor.moveTo(X, Y, Z, view.faces(), 0);
        actor.setOldPosAndRot();
        actor.yBodyRot = actor.yBodyRotO = actor.yHeadRot = actor.yHeadRotO = view.faces();
        EngineTest.server(mc, player -> player.connection.teleport(X, Y + view.dy(), Z - 3, 0, view.pitch()));
    }

    /** Films {@link #FILM_FRAMES} frames, one every two ticks; {@code everyShot} (may be null) runs after each. */
    private static void film(String name, Runnable onShot) {
        filmName = name;
        filmFrame = 0;
        wait = 10;
        lastShotTick = -1;
        everyShot = onShot;
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (filmName == null || mc.level == null) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        long tick = mc.level.getGameTime();
        if (tick == lastShotTick || tick % 2 != 0) {
            return;
        }
        lastShotTick = tick;
        save(mc, Screenshot.takeScreenshot(mc.getMainRenderTarget()),
                filmName + "/frame_" + String.format("%03d", filmFrame++));
        if (everyShot != null) {
            everyShot.run();
        }
        if (filmFrame >= FILM_FRAMES) {
            filmName = null;
        }
    }

    private static void save(Minecraft mc, NativeImage image, String name) {
        File file = new File(mc.gameDirectory, "screenshots/step5/" + name + ".png");
        file.getParentFile().mkdirs();
        try (image) {
            image.writeToFile(file);
        } catch (IOException e) {
            Murimblock.LOGGER.error("Engine test: could not save {}", file, e);
        }
    }
}
