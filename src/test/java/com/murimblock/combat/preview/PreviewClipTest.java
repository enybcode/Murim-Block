package com.murimblock.combat.preview;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.MirrorModifier;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real PAL parser/sampler, not just JSON syntax. No OpenGL or client classes. */
class PreviewClipTest {
    private static final Set<String> BONES = Set.of("body", "torso", "head", "right_arm", "left_arm",
            "right_leg", "left_leg", "right_item", "left_item");

    private Animation clip() throws IOException {
        var stream = getClass().getResourceAsStream("/assets/murimblock/player_animations/sword_preview.json");
        assertNotNull(stream);
        var loaded = UniversalAnimLoader.loadAnimations(stream);
        assertEquals(Set.of("sword_preview"), loaded.keySet());
        return loaded.get("sword_preview");
    }

    @Test
    void originalClipLoadsWithExpectedDurationBonesAndNoLoop() throws IOException {
        Animation clip = clip();
        assertEquals(PreviewDefinition.DURATION_TICKS, clip.length());
        assertEquals(Animation.LoopType.PLAY_ONCE, clip.loopType());
        assertTrue(clip.boneAnimations().keySet().containsAll(Set.of("body", "torso", "right_arm", "left_arm", "right_leg", "left_leg", "right_item")));
        assertEquals(0, clip.keyFrames().customInstructions().length);
        assertEquals(0, clip.keyFrames().sounds().length);
        assertEquals(0, clip.keyFrames().particles().length);
    }

    @Test
    void preparationContactAndRecoverySampleFiniteNonIdlePoses() throws IOException {
        var rig = new Sampler(clip());
        for (float age : new float[]{0, 3, 8, 10, 12, 17, 23.999F}) {
            rig.sample(age);
            for (String bone : BONES) {
                PlayerAnimBone pose = rig.get3DTransform(new PlayerAnimBone(bone));
                assertTrue(Float.isFinite(pose.rotX) && Float.isFinite(pose.rotY) && Float.isFinite(pose.rotZ));
                assertEquals(0, pose.positionX);
                assertEquals(0, pose.positionY);
                assertEquals(0, pose.positionZ);
            }
        }
        rig.sample(8);
        assertEquals(Math.toRadians(135), Math.abs(rig.get3DTransform(new PlayerAnimBone("right_arm")).rotX), 0.001);
        assertEquals(Math.toRadians(12), Math.abs(rig.get3DTransform(new PlayerAnimBone("right_item")).rotZ), 0.001);
    }

    @Test
    void mirroredPoseUsesLeftArmAndLeftWeaponSocket() throws IOException {
        Animation clip = clip();
        var right = new Sampler(clip);
        var left = new Sampler(clip);
        left.addModifierLast(new MirrorModifier());
        right.sample(8);
        left.sample(8);
        for (String[] pair : new String[][]{{"right_arm", "left_arm"}, {"right_item", "left_item"}, {"left_leg", "right_leg"}}) {
            var a = right.get3DTransform(new PlayerAnimBone(pair[0]));
            var b = left.get3DTransform(new PlayerAnimBone(pair[1]));
            assertEquals(a.rotX, b.rotX, 0.001);
            assertEquals(a.rotY, -b.rotY, 0.001);
            assertEquals(a.rotZ, -b.rotZ, 0.001);
        }
    }

    @Test
    void recoveryReturnsToNeutralAndClipFinishesWithoutLooping() throws IOException {
        var rig = new Sampler(clip());
        rig.sample(23.999F);
        for (String bone : BONES) {
            var pose = rig.get3DTransform(new PlayerAnimBone(bone));
            assertEquals(0, pose.rotX, 0.001);
            assertEquals(0, pose.rotY, 0.001);
            assertEquals(0, pose.rotZ, 0.001);
        }
        rig.sample(24);
        assertFalse(rig.isActive());
    }

    private static final class Sampler extends AnimationController {
        Sampler(Animation clip) {
            super((controller, state, setter) -> PlayState.STOP, MolangLoader::createNewEngine);
            triggerAnimation(clip);
        }

        void sample(float age) {
            tick = 0;
            startAnimFrom = age;
            setupAnim(new AnimationData(0, 0));
        }

        @Override public void registerBones() { BONES.forEach(this::registerPlayerAnimBone); }
        @Override public Vec3f getBonePosition(String name) { return Vec3f.ZERO; }
    }
}
