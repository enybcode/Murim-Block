package com.murimblock.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The bandits' faces (MountainBanditRenderer, BanditChiefRenderer). The biped has no bones in the face, so a face is a
 * texture: one atlas per expression, painted by scripts/animation/bandit-3d.cjs and bandit-chief-3d.cjs. Each tick picks
 * one from the mob's id and age, so every bandit keeps its own rhythm: a blink every few seconds (now and then a second
 * one), a glance to the side, a mutter; in combat the brows knot and glances are rare; a strike is a shout, a blow
 * screws the eyes shut; dead, the eyes close.
 */
public final class MobFaces {
    private MobFaces() { }

    /** The expressions, in the order of their atlases; {@link #suffix} names the file (nothing for the calm face). */
    public enum Face {
        CALM(""), BLINK("blink"), LEFT("left"), RIGHT("right"), MUTTER("mutter"),
        ANGRY("angry"), ANGRY_BLINK("angry_blink"), ANGRY_LEFT("angry_left"), ANGRY_RIGHT("angry_right"), SHOUT("shout");

        public final String suffix;

        Face(String suffix) {
            this.suffix = suffix;
        }
    }

    /** A blink comes once in each window of this many ticks (3.5 s), for three ticks. */
    static final int BLINK_WINDOW = 70;
    /** Glances: a new look every this many ticks when calm, every {@link #ANGRY_GLANCE} in combat. */
    static final int CALM_GLANCE = 36, ANGRY_GLANCE = 40;
    /** A mutter may come in each window of this many ticks (6 s): lips moving for about a second. */
    static final int MUTTER_WINDOW = 120;

    /** The face of this mob now (render thread). */
    public static Face of(LivingEntity entity, @Nullable LivingEntityPatch<?> patch) {
        boolean angry = entity instanceof Mob mob && mob.isAggressive();
        boolean striking = patch != null && patch.getEntityState().attacking();
        return face(entity.getId(), entity.tickCount, angry, striking, entity.hurtTime > 0, entity.isDeadOrDying());
    }

    /** The face for a mob with this id at this age, in this mood. */
    public static Face face(int id, int tick, boolean angry, boolean striking, boolean hurt, boolean dead) {
        if (dead) return Face.BLINK;
        if (hurt) return Face.ANGRY_BLINK;
        if (striking) return Face.SHOUT;
        if (blinking(id, tick)) return angry ? Face.ANGRY_BLINK : Face.BLINK;
        if (angry) {
            int look = mix(id, tick / ANGRY_GLANCE, 1) % 10;
            return look == 0 ? Face.ANGRY_LEFT : look == 1 ? Face.ANGRY_RIGHT : Face.ANGRY;
        }
        int at = tick % MUTTER_WINDOW;
        if (mix(id, tick / MUTTER_WINDOW, 2) % 3 == 0 && at >= 20 && at < 38 && (at / 3) % 2 == 0) return Face.MUTTER;
        int look = mix(id, tick / CALM_GLANCE, 3) % 10;
        return look < 2 ? Face.LEFT : look < 4 ? Face.RIGHT : Face.CALM;
    }

    static boolean blinking(int id, int tick) {
        int window = tick / BLINK_WINDOW, at = tick % BLINK_WINDOW, start = mix(id, window, 0) % (BLINK_WINDOW - 10);
        if (at >= start && at < start + 3) return true;
        // A second, quicker blink one time in four.
        return mix(id, window, 4) % 4 == 0 && at >= start + 6 && at < start + 8;
    }

    /** A well-mixed number from the mob, the time window and what it is for (the same for the same three). */
    static int mix(int id, int window, int salt) {
        int h = id * 0x9E3779B1 + window * 0x85EBCA6B + salt * 0xC2B2AE35;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return h & 0x7FFFFFFF;
    }
}
