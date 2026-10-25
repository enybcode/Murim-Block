package com.murimblock.client.cultivation;

import com.murimblock.cultivation.QiParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

/**
 * A bright speck of Qi carried up inside the smoke: it wavers side to side, twinkles and fades. The spawn's speed
 * arguments are its starting velocity. The falling kind drifts down instead (the shower of a breakthrough), one in
 * eight of them gold.
 */
public final class QiSparkParticle extends TextureSheetParticle {
    private final float baseSize;
    private final float phase;
    private final float maxAlpha;

    private QiSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            float power, boolean falling) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        hasPhysics = false;
        // A little lighter than the smoke around it, so it drifts up through it; the falling kind sinks slowly.
        gravity = falling ? 0.06f : -0.25f;
        friction = falling ? 0.95f : 0.92f;
        lifetime = falling ? 40 + random.nextInt(30) : 30 + random.nextInt(25);
        baseSize = (0.13f + random.nextFloat() * 0.07f) * (0.5f + 0.6f * power);
        maxAlpha = 0.5f + 0.5f * power;
        quadSize = baseSize;
        phase = random.nextFloat() * Mth.TWO_PI;
        // Near white, so the specks stand out of the cyan smoke.
        float tint = random.nextFloat() * 0.25f;
        if (falling && random.nextInt(8) == 0) {
            setColor(1f, 0.82f, 0.36f);
        } else {
            setColor(0.9f - tint, 1f, 1f);
        }
        alpha = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float t = (float) age / lifetime;
        xd += Mth.sin(age * 0.3f + phase) * 0.003;
        zd += Mth.cos(age * 0.3f + phase) * 0.003;
        float twinkle = 0.85f + 0.15f * Mth.sin(age * 0.8f + phase);
        alpha = maxAlpha * Math.min(1, t * 8) * Math.min(1, (1 - t) * 3) * twinkle;
        quadSize = baseSize * (0.8f + 0.2f * twinkle);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static final class Provider implements ParticleProvider<QiParticleOptions> {
        private final SpriteSet sprites;
        private final boolean falling;

        public Provider(SpriteSet sprites, boolean falling) {
            this.sprites = sprites;
            this.falling = falling;
        }

        @Override
        public Particle createParticle(QiParticleOptions options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd) {
            QiSparkParticle particle = new QiSparkParticle(level, x, y, z, xd, yd, zd, options.power(), falling);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
