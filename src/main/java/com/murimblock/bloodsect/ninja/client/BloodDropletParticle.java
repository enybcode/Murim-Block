package com.murimblock.bloodsect.ninja.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Short-lived, falling droplets; lit by the world rather than glowing like sparks. */
public final class BloodDropletParticle extends TextureSheetParticle {
    private final float initialSize;
    private BloodDropletParticle(ClientLevel level, double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites) {
        super(level,x,y,z,vx,vy,vz);
        xd=vx;yd=vy+.025;zd=vz;
        gravity=.65F;friction=.88F;hasPhysics=true;
        lifetime=11+random.nextInt(7);
        quadSize=initialSize=.045F+random.nextFloat()*.045F;
        float shade=.65F+random.nextFloat()*.35F;
        setColor(shade,shade,shade);
        pickSprite(sprites);
    }
    @Override public void tick() {
        super.tick();
        alpha=Math.min(1,(lifetime-age)/5F);
        quadSize=initialSize*(1-.4F*age/lifetime);
        if(onGround) {xd*=.25;zd*=.25;}
    }
    @Override public ParticleRenderType getRenderType() {return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz) {
            return new BloodDropletParticle(level,x,y,z,vx,vy,vz,sprites);
        }
    }
}
