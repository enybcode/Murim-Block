package com.murimblock.martial;

import com.murimblock.Murimblock;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Purple Heaven Thunder Saber's own particle: a thunder spark (client/ThunderSaberFx draws, colours and moves it). */
public final class ThunderSaberParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Murimblock.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THUNDER_SPARK = PARTICLES.register("thunder_spark", () -> new SimpleParticleType(true));

    private ThunderSaberParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
