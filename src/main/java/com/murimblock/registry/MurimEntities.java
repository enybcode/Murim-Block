package com.murimblock.registry;

import com.murimblock.Murimblock;
import com.murimblock.innerworld.MeditatingBody;
import com.murimblock.innerworld.OldMaster;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Murimblock entities. Registry names are saved IDs: keep them. */
public final class MurimEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Murimblock.MOD_ID);

    public static final Supplier<EntityType<MeditatingBody>> MEDITATING_BODY = ENTITIES.register("meditating_body",
            () -> EntityType.Builder.<MeditatingBody>of(MeditatingBody::new, MobCategory.MISC)
                    .sized(0.6f, 1.2f).clientTrackingRange(10).build("meditating_body"));

    public static final Supplier<EntityType<OldMaster>> OLD_MASTER = ENTITIES.register("old_master",
            () -> EntityType.Builder.<OldMaster>of(OldMaster::new, MobCategory.MISC)
                    .sized(0.9f, 1.8f).fireImmune().clientTrackingRange(10).build("old_master"));

    private MurimEntities() {
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(MEDITATING_BODY.get(), LivingEntity.createLivingAttributes().build());
            event.put(OLD_MASTER.get(), OldMaster.attributes().build());
        }
    }
}
