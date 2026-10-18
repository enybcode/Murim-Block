package com.murimblock.client.cultivation;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A soft spark of Qi, drawn bright whatever the light (the aura, a breakthrough's spiral, the inner world's motes): it
 * drifts with its speed, fades and shrinks. Its colour is set by whoever makes it ({@link AuraClient}).
 */
public class AuraParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;
    private boolean still;

    public AuraParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.94F;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 18 + random.nextInt(14);
        this.startSize = 0.06F + random.nextFloat() * 0.07F;
        this.quadSize = startSize;
        setSpriteFromAge(sprites);
    }

    /** Larger and longer (the flames of a full reserve, a pillar of light). */
    public AuraParticle big(float size, int lifetime) {
        this.quadSize = size;
        this.lifetime = lifetime;
        return this;
    }

    /** Held on its first, fullest frame (a short-lived mark that must not run through to the hollow ones). */
    public AuraParticle still() {
        this.still = true;
        setSprite(sprites.get(0, 1));
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        this.alpha = Math.max(0, 1 - t * t);
        if (!still) setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    /** Made by `level.addParticle` (white): tinted by whoever spawns it directly. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new AuraParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
