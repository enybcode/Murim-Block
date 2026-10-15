package com.murimblock.martial;

import com.murimblock.Murimblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Blood, a liquid: it runs in the Blood Iceberg's channel and pours down its tiers. Thicker than water (it flows slower
 * and spreads 4 blocks), never makes new springs; one swims in it as in water. No bucket.
 */
public final class SanguineFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Murimblock.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Murimblock.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> BLOOD_TYPE = TYPES.register("blood", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.murimblock.blood").density(1100).viscosity(1800).canSwim(true).canDrown(true).canExtinguish(true)
            .canConvertToSource(false).supportsBoating(false).motionScale(.010)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BLOOD = FLUIDS.register("blood", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_BLOOD = FLUIDS.register("flowing_blood", () -> new BaseFlowingFluid.Flowing(properties()));

    private SanguineFluids() {
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(BLOOD_TYPE, BLOOD, FLOWING_BLOOD).block(SanguineBlocks.BLOOD)
                .tickRate(8).slopeFindDistance(3).levelDecreasePerBlock(2).explosionResistance(100);
    }

    static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
