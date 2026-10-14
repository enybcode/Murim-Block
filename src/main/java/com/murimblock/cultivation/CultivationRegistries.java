package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The cultivation's own registry entries: the Qi Deviation effect and the particles of the aura and breakthroughs. */
public final class CultivationRegistries {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Murimblock.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Murimblock.MOD_ID);

    /** A failed great breakthrough: slower, weaker, and no Qi comes back while it lasts (QiEvents). */
    public static final DeferredHolder<MobEffect, MobEffect> QI_DEVIATION = EFFECTS.register("qi_deviation", () -> new Deviation()
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("qi_deviation_speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("qi_deviation_damage"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** The aura's soft sparks (tinted by the main style) and the motes of a breakthrough's spiral; both drawn bright. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA = PARTICLES.register("aura", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QI_MOTE = PARTICLES.register("qi_mote", () -> new SimpleParticleType(true));

    private CultivationRegistries() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        PARTICLES.register(modBus);
    }

    private static final class Deviation extends MobEffect {
        Deviation() {
            super(MobEffectCategory.HARMFUL, 0x8B1A1A);
        }
    }
}
