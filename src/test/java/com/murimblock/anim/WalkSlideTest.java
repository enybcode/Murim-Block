package com.murimblock.anim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * The shipped step cycles played on a body moving at Minecraft's speeds, at 60 frames a second, with and without
 * {@link FootPlanter}: how fast the foot on the ground slides, measured as the real-player tour measures it in game
 * (the lower sole, when within a pixel of the ground, should not move in the world).
 */
class WalkSlideTest {
    private static final Path DIR = Path.of("src/main/resources/assets/murimblock/skeletal/animations");
    private static final float TO_BLOCKS = 0.9375f / 16;
    private static final float FRAME = 1 / 60f;

    private static Map<String, Animation> load(String set) {
        try {
            return ClipLoader.load(set, Files.readString(DIR.resolve(set + ".gltf")), name -> {
                try {
                    return Files.readAllBytes(DIR.resolve(name));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Biped.ARMATURE, Files.readString(DIR.resolve(set + ".json")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Average and largest slide (blocks a second) of a clip played for a body moving at {@code speed}. */
    static float[] slide(Animation clip, float speed, boolean plant) {
        float rate = Math.max(0.4f, Math.min(2.5f, speed / clip.moveSpeed()));
        FootPlanter planter = new FootPlanter();
        int[] shins = {Biped.ARMATURE.get("Leg_L").index(), Biped.ARMATURE.get("Leg_R").index()};
        Vector3f last = null;
        int lastFoot = -1;
        float total = 0, time = 0, most = 0;
        Pose previous = null;
        float turn = 0;
        String where = "";
        for (int frame = 0; frame < 240; frame++) {
            float t = frame * FRAME;
            Pose pose = clip.clip().sample((t * rate) % clip.clip().duration(), Biped.ARMATURE.bindPose());
            Biped.settleHelpers(pose);
            Vector3f at = new Vector3f(0, 0, speed * t);
            if (plant) {
                planter.apply(pose, at, 0, TO_BLOCKS, FRAME, true, new Vector3f(0, 0, 1));
            }
            if (previous != null && frame > 30) {
                for (int i = 0; i < pose.size(); i++) {
                    float dot = Math.abs(new org.joml.Quaternionf(pose.get(i).rotation()).dot(new org.joml.Quaternionf(previous.get(i).rotation())));
                    float degrees = (float) Math.toDegrees(2 * Math.acos(Math.min(1, dot)));
                    if (degrees > turn) {
                        turn = degrees;
                        where = Biped.ARMATURE.joint(i).name() + " frame " + frame;
                    }
                }
            }
            previous = pose.copy();
            int lowest = -1;
            Vector3f sole = null;
            for (int shin : shins) {
                Vector3f s = LegIk.sole(Biped.ARMATURE, pose, shin, FootPlanter.SOLE);
                if (sole == null || s.y < sole.y) {
                    sole = s;
                    lowest = shin;
                }
            }
            if (sole.y > 1) {
                last = null;
                continue;
            }
            Vector3f world = new Vector3f(sole).mul(TO_BLOCKS).add(at);
            world.y = 0;
            if (last != null && lowest == lastFoot && frame > 30) {
                float v = world.distance(last) / FRAME;
                total += v * FRAME;
                time += FRAME;
                most = Math.max(most, v);
            }
            last = world;
            lastFoot = lowest;
        }
        System.out.printf("  %s planted=%s: largest turn %.1f° (%s)%n", clip.id(), plant, turn, where);
        return new float[] {time > 0 ? total / time : 0, most};
    }

    @Test
    void plantedFeetSlideLess() {
        Map<String, Animation> biped = load("biped");
        StringBuilder report = new StringBuilder();
        for (Object[] row : new Object[][] {{"walk", 4.317f}, {"run", 5.612f}, {"sneak_walk", 1.295f}}) {
            Animation clip = biped.get((String) row[0]);
            float speed = (float) row[1];
            float[] free = slide(clip, speed, false), planted = slide(clip, speed, true);
            report.append(String.format("%s at %.1f b/s: slide %.2f (max %.2f) alone, %.2f (max %.2f) planted%n",
                    row[0], speed, free[0], free[1], planted[0], planted[1]));
            // Planted, a foot on the ground moves less than a tenth of the body's speed (the ease of letting go).
            assertTrue(planted[0] < 0.3f, row[0] + ": " + planted[0]);
        }
        for (String name : new String[] {"walk", "run", "sneak_walk"}) {
            report.append(name).append(" ground speed at speed 1: ").append(biped.get(name).moveSpeed()).append("%n".formatted());
        }
        System.out.print(report);
    }

}
