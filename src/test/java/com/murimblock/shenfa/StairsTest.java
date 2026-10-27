package com.murimblock.shenfa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** The walk up and down a staircase (half a block a block) with planted feet: no joint jumps between frames. */
class StairsTest {
    private static final Path DIR = Path.of("src/main/resources/assets/murimblock/skeletal/animations");
    private static final float TO_BLOCKS = 0.9375f / 16;
    private static final float FRAME = 1 / 60f;

    /** Stairs from z 2 to 5, half a block up each block, then a landing. */
    private static float stairs(float z) {
        return z < 2 ? 0 : z >= 5 ? 1.5f : (float) (Math.floor(z - 2) + 1) * 0.5f;
    }

    private static Animation walk() {
        try {
            return ClipLoader.load("biped", Files.readString(DIR.resolve("biped.gltf")), name -> {
                try {
                    return Files.readAllBytes(DIR.resolve(name));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Biped.ARMATURE, Files.readString(DIR.resolve("biped.json"))).get("walk");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The largest joint turn between two frames (degrees) walking up or down. */
    static float run(boolean down) {
        Animation walk = walk();
        float speed = 4.317f, rate = Math.min(2.5f, speed / walk.moveSpeed());
        FootPlanter planter = new FootPlanter();
        Pose last = null;
        float most = 0;
        String where = "";
        float bodyY = down ? 1.5f : 0;
        for (int frame = 0; frame < 120; frame++) {
            float t = frame * FRAME;
            float z = down ? 7 - speed * t : speed * t;
            // The body stands on the highest ground under its 0.6-wide box, stepping up at once and down within a tick.
            float under = Math.max(stairs(z - 0.3f), stairs(z + 0.3f));
            bodyY = under >= bodyY ? under : Math.max(under, bodyY - 0.5f * FRAME * 20);
            Pose pose = walk.clip().sample((t * rate) % walk.clip().duration(), Biped.ARMATURE.bindPose());
            Biped.settleHelpers(pose);
            float yaw = down ? (float) Math.PI : 0;
            planter.apply(pose, new Vector3f(0, bodyY, z), yaw, TO_BLOCKS, FRAME, FootPlanter.Mode.STEP,
                    new Vector3f(0, 0, 1), (x, gz, near) -> stairs(gz));
            if (last != null) {
                for (int i = 0; i < pose.size(); i++) {
                    float dot = Math.abs(new Quaternionf(pose.get(i).rotation()).dot(new Quaternionf(last.get(i).rotation())));
                    float turn = (float) Math.toDegrees(2 * Math.acos(Math.min(1, dot)));
                    if (turn > most) {
                        most = turn;
                        where = Biped.ARMATURE.joint(i).name() + " frame " + frame + " z " + z;
                    }
                }
            }
            last = pose.copy();
        }
        System.out.printf("%s: largest turn %.1f° (%s)%n", down ? "down" : "up", most, where);
        return most;
    }

    @Test
    void stairsDoNotMakeTheLegsJump() {
        // Walking on the flat peaks near 31° a frame (the quick cadence); a stair adds the foot's rise to the next
        // step, but never a jump (a foot thrown up or down a whole step in one frame turns the knee over 85°).
        assertTrue(run(false) < 70);
        assertTrue(run(true) < 70);
    }
}
