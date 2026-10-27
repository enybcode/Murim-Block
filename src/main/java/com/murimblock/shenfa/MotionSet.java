package com.murimblock.shenfa;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Which clip each living motion plays, read from a JSON file:
 *
 * <pre>
 * {"motions": {"idle": "murimblock:biped/idle", "walk": "murimblock:biped/walk"},
 *  "combat": {"idle": "murimblock:fists/stance"},
 *  "additive": ["murimblock:biped/breathe"]}
 * </pre>
 *
 * In combat mode the {@code combat} clips come first; a motion with no clip falls back along
 * {@link LivingMotion#chain()} (a combat walk with no clip of its own walks normally).
 */
public record MotionSet(Map<LivingMotion, String> motions, Map<LivingMotion, String> combat, List<String> additive) {
    public static MotionSet parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (String key : root.keySet()) {
            if (!List.of("motions", "combat", "additive").contains(key)) {
                throw new IllegalArgumentException("unknown key " + key);
            }
        }
        List<String> additive = new ArrayList<>();
        if (root.has("additive")) {
            root.getAsJsonArray("additive").forEach(element -> additive.add(element.getAsString()));
        }
        return new MotionSet(motions(root, "motions"), motions(root, "combat"), List.copyOf(additive));
    }

    private static Map<LivingMotion, String> motions(JsonObject root, String key) {
        Map<LivingMotion, String> result = new EnumMap<>(LivingMotion.class);
        if (root.has(key)) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(key).entrySet()) {
                LivingMotion motion = LivingMotion.byKey(entry.getKey());
                if (motion == null) {
                    throw new IllegalArgumentException(key + ": unknown motion " + entry.getKey());
                }
                result.put(motion, entry.getValue().getAsString());
            }
        }
        return result;
    }

    /** The clip id for {@code motion}, or null when neither it nor its fallbacks have one. */
    public String clip(LivingMotion motion, boolean inCombat) {
        for (LivingMotion step : motion.chain()) {
            if (inCombat && combat.containsKey(step)) {
                return combat.get(step);
            }
            if (motions.containsKey(step)) {
                return motions.get(step);
            }
        }
        return null;
    }
}
