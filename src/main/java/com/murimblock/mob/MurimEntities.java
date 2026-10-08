package com.murimblock.mob;

import com.murimblock.Murimblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Future Murim entity types live here; no placeholder mob or world spawn is registered. */
public final class MurimEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Murimblock.MOD_ID);
    private MurimEntities() { }
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
