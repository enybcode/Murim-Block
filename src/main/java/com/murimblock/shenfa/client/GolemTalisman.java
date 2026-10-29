package com.murimblock.shenfa.client;

import com.murimblock.shenfa.Armature;
import com.murimblock.shenfa.Joint;
import com.murimblock.shenfa.JointTransform;
import com.murimblock.shenfa.Pose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Gardien-talisman's paper talisman ({@code docs/mobs/boards/golem.png}), which its clips do not key: it flutters
 * on the statue's face, harder as it walks; at its death it is torn off (tick 10) and the statue is stone again.
 */
final class GolemTalisman {
    private static final int TORN = 10;

    private GolemTalisman() {
    }

    static void apply(Mob mob, Armature armature, Pose pose, float partialTick) {
        Joint talisman = armature.find("Talisman");
        if (talisman == null) {
            return;
        }
        if (mob.deathTime >= TORN) {
            pose.set(talisman.index(), JointTransform.of(talisman.bind().translation(), new Quaternionf(),
                    new Vector3f(0)));
            return;
        }
        float now = mob.tickCount + partialTick;
        float wind = 0.06f + Mth.clamp(mob.walkAnimation.speed(partialTick), 0, 1) * 0.35f;
        float flap = Mth.sin(now * 0.13f) * wind + Mth.sin(now * 0.31f + 1) * wind * 0.4f;
        JointTransform current = pose.get(talisman.index());
        pose.set(talisman.index(), JointTransform.of(current.translation(),
                new Quaternionf(current.rotation()).rotateX(-Math.abs(flap) - wind * 0.5f).rotateZ(flap * 0.3f),
                current.scale()));
    }
}
