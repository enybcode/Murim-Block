package com.murimblock.registry;

import com.murimblock.Murimblock;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murimblock particle types. */
public final class MurimParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Murimblock.MOD_ID);

    /** A mote of Qi drawn toward a point; its speed arguments carry that point (see {@code QiParticle}). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QI =
            PARTICLES.register("qi", () -> new SimpleParticleType(true));

    private MurimParticles() {
    }
}
