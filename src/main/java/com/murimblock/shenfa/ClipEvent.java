package com.murimblock.shenfa;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;

/**
 * Something a clip makes happen at a moment of it, from the sidecar JSON:
 *
 * <pre>
 * "events": [
 *     {"at": 0.1, "type": "sound", "sound": "minecraft:entity.player.attack.sweep", "volume": 0.6},
 *     {"at": 0.15, "type": "footstep", "foot": "left"},
 *     {"at": 0.15, "type": "particle", "particle": "minecraft:cloud", "joint": "Hand_L", "count": 3},
 *     {"at": 0.15, "type": "shake", "strength": 0.4, "ticks": 4}]
 * </pre>
 *
 * Each type takes its own settings ({@link #TYPES}); {@code side} says where it runs: {@code client} (the default),
 * {@code server} or {@code both}. Server events wait for the server to play clips (network sync); until then only the
 * client runs its share.
 *
 * @param time     seconds into the clip
 * @param settings the type's settings (everything but {@code at}, {@code type} and {@code side})
 */
public record ClipEvent(float time, String type, Side side, JsonObject settings) {
    public enum Side {
        CLIENT, SERVER, BOTH;

        public boolean client() {
            return this != SERVER;
        }
    }

    /** The settings each type may take. */
    public static final Map<String, Set<String>> TYPES = Map.of(
            "sound", Set.of("sound", "volume", "pitch"),
            "particle", Set.of("particle", "joint", "count", "spread", "speed"),
            "footstep", Set.of("foot", "volume"),
            "shake", Set.of("strength", "ticks"),
            "callback", Set.of("name"));

    public String string(String key, String otherwise) {
        return settings.has(key) ? settings.get(key).getAsString() : otherwise;
    }

    public float number(String key, float otherwise) {
        return settings.has(key) ? settings.get(key).getAsFloat() : otherwise;
    }

    /** The same event with left and right swapped (a mirrored clip): its foot, and a joint named {@code _L} or {@code _R}. */
    public ClipEvent mirrored() {
        JsonObject swapped = settings.deepCopy();
        if (swapped.has("foot")) {
            swapped.addProperty("foot", swapped.get("foot").getAsString().equals("left") ? "right" : "left");
        }
        if (swapped.has("joint")) {
            String joint = swapped.get("joint").getAsString();
            swapped.addProperty("joint", joint.endsWith("_L") ? joint.substring(0, joint.length() - 1) + "R"
                    : joint.endsWith("_R") ? joint.substring(0, joint.length() - 1) + "L" : joint);
        }
        return new ClipEvent(time, type, side, swapped);
    }
}
