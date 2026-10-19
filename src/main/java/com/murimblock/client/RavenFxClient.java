package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.murimblock.Murimblock;
import com.murimblock.martial.RavenGateEntity;
import com.murimblock.martial.RavenParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * The Raven Messenger Art's doors and feathers on the client: each door (martial/RavenGateEntity) lights its place with
 * a warm amber light and a cold violet one (Photon, nothing where its lights are off) for as long as it is open, and sheds
 * black feathers from its rim; the feathers (RavenParticles.FEATHER) fall slowly, swaying and turning.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class RavenFxClient {
    private RavenFxClient() {
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RavenParticles.FEATHER.get(), Feather.Provider::new);
    }

    /** A door's tick on the client: its lights when it opens, its feathers while it is open. */
    public static void gateTick(RavenGateEntity gate) {
        ClientLevel level = (ClientLevel) gate.level();
        float r = gate.size();
        double cy = gate.kind() == RavenGateEntity.DOOR ? gate.getY() + r * 1.25 : gate.getY() + (gate.kind() == RavenGateEntity.SKY ? -0.2 : 0.2);
        if (gate.tickCount == 1 && PhotonLights.isEnabled()) {
            int ticks = Math.max(4, gate.life() - 1);
            PhotonLights.flash(new DynamicLight().point().at(gate.getX(), cy, gate.getZ()).color(1.0F, 0.6F, 0.2F)
                    .intensity(1.5F).range(4 + r * 4).sourceRadius(r * 0.3F).volumetric(0), ticks);
            PhotonLights.flash(new DynamicLight().point().at(gate.getX(), cy, gate.getZ()).color(0.5F, 0.3F, 1.0F)
                    .intensity(0.9F).range(3 + r * 3).sourceRadius(r * 0.2F).volumetric(PhotonLights.isVolumetricEnabled() ? 0.3F : 0), ticks);
        }
        if (gate.openness(0) < 0.3F) return;
        RandomSource random = level.random;
        int count = Math.max(1, Math.round(r * 1.5F));
        for (int i = 0; i < count; i++) {
            if (random.nextInt(3) != 0) continue;
            double a = random.nextDouble() * Math.PI * 2, d = r * (0.85 + random.nextDouble() * 0.4);
            double x, y, z;
            if (gate.kind() == RavenGateEntity.DOOR) {
                double side = Math.cos(a) * d, up = Math.sin(a) * d * 1.3, yaw = Math.toRadians(gate.facing());
                x = gate.getX() + Math.cos(yaw) * side;
                z = gate.getZ() + Math.sin(yaw) * side;
                y = cy + up;
            } else {
                x = gate.getX() + Math.cos(a) * d;
                z = gate.getZ() + Math.sin(a) * d;
                y = cy;
            }
            level.addParticle(RavenParticles.FEATHER.get(), x, y, z, (random.nextDouble() - 0.5) * 0.05, 0.02 + random.nextDouble() * 0.04,
                    (random.nextDouble() - 0.5) * 0.05);
        }
    }

    /** A black feather: drifts down slowly, swaying from side to side and turning as it falls. */
    static final class Feather extends TextureSheetParticle {
        private final float swayPhase, swaySpeed, turn;

        Feather(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
            super(level, x, y, z, vx, vy, vz);
            xd = vx;
            yd = vy;
            zd = vz;
            pickSprite(sprites);
            lifetime = 40 + random.nextInt(40);
            quadSize = 0.1F + random.nextFloat() * 0.08F;
            gravity = 0.015F;
            friction = 0.94F;
            swayPhase = random.nextFloat() * Mth.TWO_PI;
            swaySpeed = 0.15F + random.nextFloat() * 0.1F;
            turn = (random.nextFloat() - 0.5F) * 0.2F;
            roll = random.nextFloat() * Mth.TWO_PI;
            oRoll = roll;
        }

        @Override
        public void tick() {
            super.tick();
            oRoll = roll;
            roll += turn;
            float sway = Mth.sin(swayPhase + age * swaySpeed) * 0.012F;
            xd += sway;
            zd += sway * 0.6F;
            if (age > lifetime - 10) alpha = Math.max(0, (lifetime - age) / 10.0F);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
            @Override
            public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
                return new Feather(level, x, y, z, vx, vy, vz, sprites);
            }
        }
    }
}
