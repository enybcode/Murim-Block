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
 * A puff of Qi smoke: it rises from the meditator, swells, frays (its frames thin out with age) and fades away. The
 * spawn's speed arguments are its starting velocity. Ring puffs, from a breakthrough, are bigger and brighter.
 */
public final class QiParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;
    private final float endSize;
    private final float maxAlpha;
    private final float spin;

    private QiParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            SpriteSet sprites, boolean ring, float power) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        hasPhysics = false;
        // Lifts on its own (vanilla applies 0.04 × gravity per tick), up to about 0.1 block a tick.
        gravity = ring ? -0.15f : -0.3f;
        friction = 0.9f;
        // Little Qi: small, faint, short puffs; a great reserve: big, thick, lingering ones.
        lifetime = (int) (((ring ? 30 : 40) + random.nextInt(20)) * (0.6f + 0.4f * power));
        startSize = ((ring ? 0.35f : 0.22f) + random.nextFloat() * 0.1f) * (0.5f + 0.6f * power);
        endSize = startSize * (ring ? 2.6f : 3.2f);
        maxAlpha = (ring ? 0.7f : 0.45f) * (0.4f + 0.6f * power);
        spin = (random.nextFloat() - 0.5f) * 0.08f;
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
        // Saturated cyan, some puffs paler; ring puffs nearly white at heart.
        float white = ring ? 0.55f : random.nextFloat() < 0.3f ? 0.4f : random.nextFloat() * 0.15f;
        setColor(0.1f + 0.7f * white, 0.75f + 0.25f * white, 1f);
        quadSize = startSize;
        alpha = 0;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float t = (float) age / lifetime;
        oRoll = roll;
        roll += spin;
        quadSize = Mth.lerp(1 - (1 - t) * (1 - t), startSize, endSize);
        // Faint while it leaves the body, so the face stays readable, thickest once clear of it.
        alpha = maxAlpha * Math.min(1, t * 3) * (1 - t) * (1 - t) * 1.6f;
        setSpriteFromAge(sprites);
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
        private final boolean ring;

        public Provider(SpriteSet sprites, boolean ring) {
            this.sprites = sprites;
            this.ring = ring;
        }

        @Override
        public Particle createParticle(QiParticleOptions options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd) {
            return new QiParticle(level, x, y, z, xd, yd, zd, sprites, ring, options.power());
        }
    }
}
