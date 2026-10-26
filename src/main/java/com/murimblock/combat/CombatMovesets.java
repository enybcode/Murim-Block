package com.murimblock.combat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.murimblock.Murimblock;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

/**
 * The fighting styles, read from data packs ({@code data/<namespace>/combat_movesets/}), reloaded by {@code /reload}
 * and sent to every client with the moves.
 */
public final class CombatMovesets {
    public static final ResourceLocation BARE_HANDS = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "bare_hands");

    /** On the server, the loaded styles; on a client, the copy the server sent. */
    private static Map<ResourceLocation, CombatMoveset> movesets = Map.of();

    private CombatMovesets() {
    }

    public static CombatMoveset get(ResourceLocation id) {
        return id == null ? null : movesets.get(id);
    }

    public static Map<ResourceLocation, CombatMoveset> all() {
        return movesets;
    }

    public static void replace(Map<ResourceLocation, CombatMoveset> received) {
        movesets = Map.copyOf(received);
    }

    /** The style fought with this main-hand stack, or null when it has none. */
    public static ResourceLocation forHeld(ItemStack stack) {
        ResourceLocation best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (Map.Entry<ResourceLocation, CombatMoveset> entry : movesets.entrySet()) {
            CombatMoveset set = entry.getValue();
            // Ties go to the smaller id, so every side picks the same style whatever the map order.
            if (set.matches(stack) && (best == null || set.priority() > bestPriority
                    || set.priority() == bestPriority && entry.getKey().compareTo(best) < 0)) {
                best = entry.getKey();
                bestPriority = set.priority();
            }
        }
        return best;
    }

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), "combat_movesets");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, CombatMoveset> loaded = new HashMap<>();
            files.forEach((id, json) -> CombatMoveset.CODEC.parse(JsonOps.INSTANCE, json)
                    .ifSuccess(set -> {
                        if (set.chain().isEmpty() || set.held().isEmpty()) {
                            Murimblock.LOGGER.error("Bad combat moveset {}: empty chain or held", id);
                        } else {
                            loaded.put(id, set);
                        }
                    })
                    .ifError(error -> Murimblock.LOGGER.error("Bad combat moveset {}: {}", id, error.message())));
            replace(loaded);
            Murimblock.LOGGER.info("Loaded {} combat movesets", loaded.size());
        }
    }
}
