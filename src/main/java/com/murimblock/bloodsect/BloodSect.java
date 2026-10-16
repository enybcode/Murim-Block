package com.murimblock.bloodsect;

import com.murimblock.bloodsect.ninja.BloodNinja;
import com.murimblock.bloodsect.ninja.BloodSoldier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * The Blood Sect (Colleuhu95's Blood Sect Decor, merged into Murimblock on 16/10/2026): its dark blood (a liquid of its own,
 * not the Sanguine Art's blood), the Blood Ninja, the cultists of its lowest ranks, with their Blood Dao, the Blood Soldier,
 * its middle rank (17/10/2026), and the blood droplet particle. Registered by Murimblock (register).
 */
public final class BloodSect {
    public static final String ID = com.murimblock.Murimblock.MOD_ID;
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, ID);
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> BLOOD_DROPLET =
        PARTICLES.register("blood_droplet", () -> new net.minecraft.core.particles.SimpleParticleType(false));

    public static final DeferredHolder<FluidType, FluidType> BLOOD_TYPE = TYPES.register("dark_blood", () -> new FluidType(
        FluidType.Properties.create().descriptionId("fluid_type.murimblock.dark_blood")
            .density(1080).viscosity(2500).temperature(310).lightLevel(0)
            .canSwim(true).canDrown(true).canHydrate(false).canConvertToSource(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BLOOD = FLUIDS.register("dark_blood", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_BLOOD = FLUIDS.register("flowing_dark_blood", () -> new BaseFlowingFluid.Flowing(properties()));
    public static final DeferredBlock<LiquidBlock> BLOOD_BLOCK = BLOCKS.register("dark_blood", () -> new LiquidBlock(BLOOD.get(),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).replaceable().noCollission()
            .strength(100).noLootTable().liquid().pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY)));
    public static final DeferredItem<BucketItem> BLOOD_BUCKET = ITEMS.register("dark_blood_bucket", () -> new BucketItem(BLOOD.get(),
        new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodNinja>> BLOOD_NINJA = ENTITIES.register("blood_ninja", () ->
        EntityType.Builder.of(BloodNinja::new, MobCategory.MONSTER).sized(.6F, 1.9F).clientTrackingRange(10).updateInterval(1).build(ID + ":blood_ninja"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodSoldier>> BLOOD_SOLDIER = ENTITIES.register("blood_soldier", () ->
        EntityType.Builder.of(BloodSoldier::new, MobCategory.MONSTER).sized(.6F, 1.9F).clientTrackingRange(10).updateInterval(1).build(ID + ":blood_soldier"));
    // Mob equipment for this visual test: no recipe, creative listing or loot entry.
    // The ninja's existing attack attribute controls damage independently of the prop.
    public static final DeferredItem<Item> BLOOD_DAO = ITEMS.register("blood_dao", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<DeferredSpawnEggItem> BLOOD_NINJA_SPAWN_EGG = ITEMS.register("blood_ninja_spawn_egg", () ->
        new DeferredSpawnEggItem(BLOOD_NINJA, 0x1B1C23, 0x670914, new Item.Properties()));

    /** The black iron of its armour and the red of its lacquer. */
    public static final DeferredItem<DeferredSpawnEggItem> BLOOD_SOLDIER_SPAWN_EGG = ITEMS.register("blood_soldier_spawn_egg", () ->
        new DeferredSpawnEggItem(BLOOD_SOLDIER, 0x3A3B40, 0x8A1620, new Item.Properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(BLOOD_TYPE, BLOOD, FLOWING_BLOOD)
            .block(BLOOD_BLOCK).bucket(BLOOD_BUCKET).slopeFindDistance(4)
            .levelDecreasePerBlock(1).tickRate(12).explosionResistance(100);
    }

    private BloodSect() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        PARTICLES.register(bus);
        bus.addListener(BloodSect::attributes);
        bus.addListener(BloodSect::creativeItems);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(BLOOD_NINJA.get(), BloodNinja.createAttributes().build());
        event.put(BLOOD_SOLDIER.get(), BloodSoldier.createAttributes().build());
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(BLOOD_NINJA_SPAWN_EGG.get());
            event.accept(BLOOD_SOLDIER_SPAWN_EGG.get());
        }
    }
}
