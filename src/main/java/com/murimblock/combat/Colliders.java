package com.murimblock.combat;

import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.AttackData;
import com.murimblock.shenfa.Biped;
import com.murimblock.shenfa.Joint;
import com.murimblock.shenfa.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Where a strike's colliders sweep in the world between two moments of its clip (Epic Fight's swept colliders): each
 * box is placed at a few moments in between, so a fast fist does not pass through a body between two ticks. A box on
 * a joint follows the biped's pose from the clip; a box without a joint sits in the body's own space. A phase with
 * no box strikes in front of the body, as far as it reaches.
 */
public final class Colliders {
    /** Moments per tick at which the boxes are placed. */
    private static final int STEPS = 3;

    private Colliders() {
    }

    /** The boxes swept by {@code phase} of {@code animation} from {@code from} to {@code to} seconds. */
    public static List<AABB> sweep(LivingEntity body, Animation animation, AttackData.Phase phase, float from, float to,
                                   float yaw) {
        List<AABB> boxes = new ArrayList<>();
        for (int step = 0; step <= STEPS; step++) {
            float time = from + (to - from) * step / STEPS;
            Matrix4f[] joints = null;
            if (phase.colliders().stream().anyMatch(collider -> collider.joint() != null)) {
                Pose pose = animation.clip().sample(time, Biped.ARMATURE.bindPose());
                Biped.settleHelpers(pose);
                joints = pose.toModelMatrices(Biped.ARMATURE);
            }
            if (phase.colliders().isEmpty()) {
                boxes.add(front(body, yaw));
                continue;
            }
            for (AttackData.Collider collider : phase.colliders()) {
                Joint joint = collider.joint() == null ? null : Biped.ARMATURE.find(collider.joint());
                Matrix4f place = joint == null || joints == null ? new Matrix4f() : joints[joint.index()];
                boxes.add(box(body, place, collider, yaw));
            }
        }
        return boxes;
    }

    /** Blocks per model pixel for {@code body}: vanilla draws players at 15/16. */
    public static float pixel(LivingEntity body) {
        return (body instanceof Player ? 0.9375f : 1f) / 16 * body.getScale();
    }

    /** A collider placed by a joint's model matrix, turned with the body and set in the world (axis-aligned around it). */
    private static AABB box(LivingEntity body, Matrix4f place, AttackData.Collider collider, float yaw) {
        float pixel = pixel(body);
        Vector3f half = new Vector3f(collider.size()).mul(0.5f);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        Vec3 at = body.position();
        for (int corner = 0; corner < 8; corner++) {
            Vector3f point = new Vector3f(collider.center()).add((corner & 1) == 0 ? -half.x : half.x,
                    (corner & 2) == 0 ? -half.y : half.y, (corner & 4) == 0 ? -half.z : half.z);
            place.transformPosition(point).mul(pixel).rotateY((float) Math.toRadians(-yaw));
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(at);
    }

    /** In front of the body: as wide and tall as it, as deep as it reaches. */
    public static AABB front(LivingEntity body, float yaw) {
        double width = body.getBbWidth(), reach = reach(body);
        Vec3 ahead = Vec3.directionFromRotation(0, yaw);
        Vec3 center = body.position().add(ahead.scale(width / 2 + reach / 2)).add(0, body.getBbHeight() / 2, 0);
        double half = Math.max(width, reach) / 2 + 0.3;
        return new AABB(center.x - half, center.y - body.getBbHeight() / 2, center.z - half, center.x + half,
                center.y + body.getBbHeight() / 2, center.z + half);
    }

    /** How far a body strikes past its own edge (blocks): vanilla's melee reach, a little more for big bodies. */
    public static double reach(LivingEntity body) {
        return 0.8 + body.getBbWidth() * 0.5;
    }
}
