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

    private QiParticle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz) {
        super(level, x, y, z);
        startX = x;
        startY = y;
        startZ = z;
        targetX = tx;
        targetY = ty;
        targetZ = tz;
        double distance = Math.sqrt((tx - x) * (tx - x) + (ty - y) * (ty - y) + (tz - z) * (tz - z));
        lifetime = (int) (14 + distance * 14 + random.nextInt(8));
        swirl = (random.nextBoolean() ? 1 : -1) * (0.6f + random.nextFloat() * 0.9f);
        baseSize = 0.05f + random.nextFloat() * 0.05f;
        quadSize = baseSize;
        hasPhysics = false;
        gravity = 0;
        // Pale cyan to white, the System's Qi colour.
        float white = random.nextFloat() * 0.5f;
        setColor(0.55f + 0.45f * white, 0.88f + 0.12f * white, 1f);
        alpha = 0;
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
        alpha = Math.min(1, t * 5) * Math.min(1, (1 - t) * 4) * 0.9f;
        quadSize = baseSize * (1 - 0.6f * t);
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

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double tx, double ty, double tz) {
            QiParticle particle = new QiParticle(level, x, y, z, tx, ty, tz);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
