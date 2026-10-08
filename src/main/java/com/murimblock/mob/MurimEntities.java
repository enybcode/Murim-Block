package com.murimblock.mob;

import com.murimblock.Murimblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murim entities have stable IDs; the training fixture has no natural spawn. */
public final class MurimEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Murimblock.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<TrainingOpponentEntity>> TRAINING_OPPONENT =
            TYPES.register("training_opponent", () -> EntityType.Builder.of(TrainingOpponentEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("murimblock:training_opponent"));
    private MurimEntities() { }
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(MurimEntities::attributes);
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(TRAINING_OPPONENT.get(), TrainingOpponentEntity.createTrainingAttributes().build());
    }
}
