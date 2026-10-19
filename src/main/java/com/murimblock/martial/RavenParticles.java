package com.murimblock.martial;

import com.murimblock.Murimblock;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Raven Messenger Art's own particle: a black feather (client/RavenFxClient draws and moves it). */
public final class RavenParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Murimblock.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FEATHER = PARTICLES.register("raven_feather", () -> new SimpleParticleType(false));

    private RavenParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
