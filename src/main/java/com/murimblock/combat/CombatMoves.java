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

/**
 * The strikes, read from data packs ({@code data/<namespace>/combat_moves/}). {@code /reload} reads them again and
 * sends them to every client, so they can be tuned without restarting the game.
 */
public final class CombatMoves {
    /** On the server, the loaded moves; on a client, the copy the server sent. */
    private static Map<ResourceLocation, CombatMove> moves = Map.of();

    private CombatMoves() {
    }

    public static CombatMove get(ResourceLocation id) {
        return moves.get(id);
    }

    public static Map<ResourceLocation, CombatMove> all() {
        return moves;
    }

    public static void replace(Map<ResourceLocation, CombatMove> received) {
        moves = Map.copyOf(received);
    }

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(new Gson(), "combat_moves");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, CombatMove> loaded = new HashMap<>();
            files.forEach((id, json) -> CombatMove.CODEC.parse(JsonOps.INSTANCE, json)
                    .ifSuccess(move -> loaded.put(id, move))
                    .ifError(error -> Murimblock.LOGGER.error("Bad combat move {}: {}", id, error.message())));
            replace(loaded);
            Murimblock.LOGGER.info("Loaded {} combat moves", loaded.size());
        }
    }
}
