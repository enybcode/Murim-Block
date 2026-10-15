package com.murimblock.martial;

import com.murimblock.Murimblock;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import yesman.epicfight.particle.HitParticleType;

/**
 * The Sanguine Art's hit "particle": none. Epic Fight puffs a white burst on every punch that lands; this art shows no
 * particles at all (Enzo, 14/10/2026), so its blows name this type, whose client provider draws nothing.
 */
public final class SanguineParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Murimblock.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, HitParticleType> NO_HIT = PARTICLES.register("no_hit", () -> new HitParticleType(false));

    private SanguineParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
