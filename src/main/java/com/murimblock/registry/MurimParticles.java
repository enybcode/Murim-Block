package com.murimblock.registry;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.QiParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murimblock particle types. */
public final class MurimParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Murimblock.MOD_ID);

    /** A puff of Qi smoke rising from a meditator. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<QiParticleOptions>> QI =
            PARTICLES.register("qi", QiParticleOptions::newType);
    /** A larger, brighter puff of the ring a breakthrough blows out. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<QiParticleOptions>> QI_RING =
            PARTICLES.register("qi_ring", QiParticleOptions::newType);
    /** A bright speck of Qi carried up in the smoke. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<QiParticleOptions>> QI_SPARK =
            PARTICLES.register("qi_spark", QiParticleOptions::newType);

    /** A speck of Qi drifting down, from the burst of a breakthrough. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<QiParticleOptions>> QI_FALL =
            PARTICLES.register("qi_fall", QiParticleOptions::newType);

    private MurimParticles() {
    }
}
