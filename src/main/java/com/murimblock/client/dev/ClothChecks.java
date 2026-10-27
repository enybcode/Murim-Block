package com.murimblock.client.dev;

import com.murimblock.Murimblock;
import com.murimblock.client.dev.EngineTest.Step;
import com.murimblock.shenfa.client.CapeCloth;
import com.murimblock.shenfa.client.SkeletalPlayers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The cape as cloth, on the real player wearing the test cape: standing it hangs straight down behind the back;
 * running it trails behind, lifted by the motion; it never sinks into the body (it stays behind the back).
 */
final class ClothChecks {
    private static final int AREA = 600;
    private static final double X = AREA + 0.5, Y = 200, Z = 0.5;
    private static Vector3f standing;

    private ClothChecks() {
    }

    static List<Step> steps() {
        List<Step> steps = new ArrayList<>();
        steps.add(new Step("cloth setup", mc -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            SkeletalPlayers.enable(true);
            CapeCloth.enable(true);
            CapeCloth.testCape(true);
            EngineTest.server(mc, player -> {
                for (BlockPos pos : BlockPos.betweenClosed(AREA - 3, (int) Y - 1, -3, AREA + 3, (int) Y - 1, 60)) {
                    player.serverLevel().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
                }
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                player.connection.teleport(X, Y, Z, 0, 10);
            });
            mc.player.getAbilities().flying = false;
        }));
        steps.add(new Step("cape standing", mc -> {
            standing = CapeCloth.hang(mc.player);
            EngineTest.check("the cape hangs as cloth (" + standing + ")", standing != null);
            if (standing != null) {
                Vec3 look = mc.player.getViewVector(1).multiply(1, 0, 1).normalize();
                double back = -(standing.x * look.x + standing.z * look.z);
                EngineTest.check(String.format("standing it hangs down its length (%.2f of 1.0) a little behind (%.2f)",
                        -standing.y, back), -standing.y > 0.85 && back > -0.02 && back < 0.35);
            }
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
            EngineTest.shoot(mc, "cloth_standing");
        }));
        steps.add(new Step("cape running", mc -> {
            Vector3f running = CapeCloth.hang(mc.player);
            EngineTest.shoot(mc, "cloth_running");
            mc.options.keyUp.setDown(false);
            mc.options.keySprint.setDown(false);
            if (running != null) {
                Vec3 look = mc.player.getViewVector(1).multiply(1, 0, 1).normalize();
                double back = -(running.x * look.x + running.z * look.z);
                Murimblock.LOGGER.info("Engine test cloth: running, the cape's bottom {} blocks behind, {} down",
                        String.format("%.2f", back), String.format("%.2f", -running.y));
                EngineTest.check(String.format("running it trails behind (%.2f blocks behind, %.2f down)", back,
                        -running.y), back > 0.25);
            }
            CapeCloth.testCape(false);
        }));
        return steps;
    }
}
