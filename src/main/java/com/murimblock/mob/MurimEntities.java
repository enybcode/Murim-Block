package com.murimblock.mob;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightMobSupport;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murim entities have stable IDs; the training fixture has no natural spawn, the armored crab does. */
public final class MurimEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Murimblock.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<TrainingOpponentEntity>> TRAINING_OPPONENT =
            TYPES.register("training_opponent", () -> EntityType.Builder.of(TrainingOpponentEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("murimblock:training_opponent"));
    /** Slightly smaller than a vanilla spider (1.4 x 0.9). */
    public static final DeferredHolder<EntityType<?>, EntityType<ArmoredCrabEntity>> ARMORED_CRAB =
            TYPES.register("armored_crab", () -> EntityType.Builder.of(ArmoredCrabEntity::new, MobCategory.MONSTER)
                    .sized(1.1F, 0.6F).clientTrackingRange(8).build("murimblock:armored_crab"));
    private MurimEntities() { }
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(MurimEntities::attributes);
        bus.addListener(MurimEntities::spawnPlacements);
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(TRAINING_OPPONENT.get(), TrainingOpponentEntity.createTrainingAttributes().build());
        event.put(ARMORED_CRAB.get(), EpicFightMobSupport.creatureAttributes(ArmoredCrabEntity.createCrabAttributes()).build());
    }
    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Biomes, weight and group size live in data/murimblock/neoforge/biome_modifier/armored_crab_spawns.json.
        event.register(ARMORED_CRAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ArmoredCrabEntity::checkCrabSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
