package com.murimblock.client;

import com.murimblock.Murimblock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * The few kinds of entity that client effects look for every frame (blood crystals, Blood Ninjas, Cruel Suns), kept as
 * they come into and leave the client's world, so those effects never walk every entity of the world each frame.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class ClientEntityIndex {
    private static final List<Class<? extends Entity>> KINDS = List.of(com.murimblock.martial.BloodCrystalEntity.class,
            com.murimblock.bloodsect.ninja.BloodNinja.class, com.murimblock.mob.majin.CruelSunEntity.class);
    private static final Map<Class<? extends Entity>, Set<Entity>> BY_KIND = new IdentityHashMap<>();

    static {
        for (Class<? extends Entity> kind : KINDS) BY_KIND.put(kind, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private ClientEntityIndex() {
    }

    /** The entities of this kind (one of KINDS) in the client's world now, a copy safe to walk while drawing. */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> of(Class<T> kind) {
        Set<Entity> set = BY_KIND.get(kind);
        if (set == null || set.isEmpty()) return List.of();
        List<T> out = new ArrayList<>(set.size());
        for (Entity e : set) if (!e.isRemoved()) out.add((T) e);
        return out;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        for (Class<? extends Entity> kind : KINDS) if (kind.isInstance(event.getEntity())) BY_KIND.get(kind).add(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        for (Class<? extends Entity> kind : KINDS) BY_KIND.get(kind).remove(event.getEntity());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        for (Set<Entity> set : BY_KIND.values()) set.clear();
    }
}
