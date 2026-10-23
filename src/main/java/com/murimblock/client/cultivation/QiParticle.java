package com.murimblock.client.cultivation;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A glowing mote of Qi drawn from the air into a point (the meditator's dantian). It spirals in, speeding up, and
 * fades as it is absorbed. The spawn's speed arguments are the target point, not a velocity.
 */
public final class QiParticle extends TextureSheetParticle {
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    /** Radians the mote turns around the target on its way in. */
    private final float swirl;
    private final float baseSize;

    private QiParticle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, boolean ring) {
        super(level, x, y, z);
        startX = x;
        startY = y;
        startZ = z;
        targetX = tx;
        targetY = ty;
        targetZ = tz;
        double distance = Math.sqrt((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z));
        lifetime = (int) (14 + distance * 14 + random.nextInt(8));
        hasPhysics = false;
        gravity = 0;
        alpha = 0;
        if (ring) {
            // The ring holds its shape: one slow, even turn, bigger and brighter motes.
            swirl = 0.4f;
            baseSize = 0.17f + random.nextFloat() * 0.04f;
            setColor(0.45f, 0.92f, 1f);
        } else {
            swirl = (random.nextBoolean() ? 1 : -1) * (0.6f + random.nextFloat() * 0.9f);
            baseSize = 0.09f + random.nextFloat() * 0.07f;
            // Saturated cyan, a few motes paler, so they read against a bright sky.
            float white = random.nextFloat() < 0.25f ? 0.5f : random.nextFloat() * 0.15f;
            setColor(0.1f + 0.6f * white, 0.75f + 0.25f * white, 1f);
        }
        quadSize = baseSize;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float t = (float) age / lifetime;
        // Slow drift at first, then pulled in fast.
        float pull = t * t * (3 - 2 * t) * (0.4f + 0.6f * t);
        double ox = startX - targetX;
        double oz = startZ - targetZ;
        float angle = swirl * pull;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        double keep = 1 - pull;
        setPos(targetX + (ox * cos - oz * sin) * keep,
                Mth.lerp(pull, startY, targetY) + Mth.sin(t * Mth.PI) * 0.15,
                targetZ + (ox * sin + oz * cos) * keep);
        alpha = Math.min(1, t * 10) * Math.min(1, (1 - t) * 4);
        quadSize = baseSize * (1 - 0.5f * t);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean ring;

        public Provider(SpriteSet sprites, boolean ring) {
            this.sprites = sprites;
            this.ring = ring;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double tx, double ty, double tz) {
            QiParticle particle = new QiParticle(level, x, y, z, tx, ty, tz, ring);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
