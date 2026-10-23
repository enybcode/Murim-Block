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

    /** A puff of Qi smoke rising from a meditator. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QI =
            PARTICLES.register("qi", () -> new SimpleParticleType(true));
    /** A larger, brighter puff of the ring a good breath blows out. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QI_RING =
            PARTICLES.register("qi_ring", () -> new SimpleParticleType(true));
    /** A bright speck of Qi carried up in the smoke. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QI_SPARK =
            PARTICLES.register("qi_spark", () -> new SimpleParticleType(true));

    private MurimParticles() {
    }
}
