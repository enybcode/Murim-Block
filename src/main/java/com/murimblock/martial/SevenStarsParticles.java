package com.murimblock.martial;

import com.murimblock.Murimblock;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Seven Stars Sword's own particle: star dust, a tiny bright sparkle (client/SevenStarsFx draws and moves it). */
public final class SevenStarsParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Murimblock.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_DUST = PARTICLES.register("star_dust", () -> new SimpleParticleType(true));

    private SevenStarsParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
