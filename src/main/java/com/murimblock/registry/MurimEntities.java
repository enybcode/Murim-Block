package com.murimblock.registry;

import com.murimblock.Murimblock;
import com.murimblock.innerworld.MeditatingBody;
import com.murimblock.innerworld.OldMaster;
import com.murimblock.innerworld.TrialZombie;
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
                    .sized(0.8f, 2.4f).fireImmune().clientTrackingRange(10).build("old_master"));

    public static final Supplier<EntityType<TrialZombie>> TRIAL_ZOMBIE = ENTITIES.register("trial_zombie",
            () -> EntityType.Builder.<TrialZombie>of(TrialZombie::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).eyeHeight(1.74f).clientTrackingRange(8).build("trial_zombie"));

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
            event.put(TRIAL_ZOMBIE.get(), TrialZombie.attributes().build());
        }
    }
}
