package com.murimblock.shenfa;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * Reads a clip set: the animations of a glTF file (as Blender exports an armature's actions) and the sidecar JSON
 * that gives each clip its settings:
 *
 * <pre>
 * {"clips": {
 *     "guard": {"loop": true, "transition": 0.1, "masks": {"walk": ["Shoulder_L", "Shoulder_R"]}},
 *     "walk": {"loop": true, "move_speed": "auto"},
 *     "breathe": {"loop": true, "additive": true, "mask": ["Chest"]}}}
 * </pre>
 *
 * {@code mask} lists joints taken with their children (all joints when it is left out); {@code masks} gives other
 * masks by living motion; {@code additive} and {@code move_speed} are explained in {@link Animation} ({@code "auto"}
 * measures it from the clip's feet, {@link Strides}); {@code events} in {@link ClipEvent}, {@code root_motion} in
 * {@link RootMotion}. A clip missing
 * from the sidecar gets the defaults (no loop, {@link Animation#DEFAULT_TRANSITION}, all joints); a name or key the sidecar
 * does not know is refused, to catch typos.
 */
public final class ClipLoader {
    private static final Set<String> KEYS = Set.of("loop", "transition", "mask", "masks", "additive", "move_speed",
            "events", "root_motion", "attack");
    private static final Set<String> ATTACK_KEYS = Set.of("phases", "invulnerable", "super_armor");
    private static final Set<String> PHASE_KEYS = Set.of("windup", "active", "recovery", "hand", "colliders", "damage",
            "impact", "armor_negation", "max_targets", "stun", "knockback", "kind");

    private ClipLoader() {
    }

    /**
     * @param set     the clip set's name, the start of each animation's id
     * @param json    the .gltf file's text
     * @param buffer  reads a buffer file named in the .gltf
     * @param sidecar the sidecar JSON's text, or null when there is none
     * @return the animations by clip name, in the file's order
     */
    public static Map<String, Animation> load(String set, String json, Function<String, byte[]> buffer,
            Armature armature, String sidecar) {
        Map<String, Clip> clips = clips(new GltfLoader(json, buffer), armature);
        JsonObject settings = sidecar == null ? new JsonObject() : JsonParser.parseString(sidecar).getAsJsonObject();
        JsonObject entries = settings.has("clips") ? settings.getAsJsonObject("clips") : new JsonObject();
        for (String name : entries.keySet()) {
            if (!clips.containsKey(name)) {
                throw new IllegalArgumentException("sidecar names clip " + name + ", which is not in the file");
            }
        }
        Map<String, Animation> animations = new LinkedHashMap<>();
        clips.forEach((name, clip) -> {
            JsonObject entry = entries.has(name) ? entries.getAsJsonObject(name) : new JsonObject();
            animations.put(name, animation(set, name, clip, entry, armature, false, KEYS));
        });
        return animations;
    }

    /** A clip with its settings from a sidecar entry. */
    private static Animation animation(String set, String name, Clip clip, JsonObject entry, Armature armature,
            boolean additiveByDefault, Set<String> allowed) {
        check(name, "the clip", entry, allowed);
        boolean loop = entry.has("loop") && entry.get("loop").getAsBoolean();
        float transition = entry.has("transition") ? entry.get("transition").getAsFloat()
                : Animation.DEFAULT_TRANSITION;
        JointMask mask = entry.has("mask") ? mask(armature, name, entry.getAsJsonArray("mask"))
                : JointMask.all(armature);
        Map<String, JointMask> masks = new LinkedHashMap<>();
        if (entry.has("masks")) {
            JsonObject byMotion = entry.getAsJsonObject("masks");
            for (String motion : byMotion.keySet()) {
                if (LivingMotion.byKey(motion) == null) {
                    throw new IllegalArgumentException("clip " + name + ": masks names unknown motion " + motion);
                }
                masks.put(motion, mask(armature, name, byMotion.getAsJsonArray(motion)));
            }
        }
        boolean additive = entry.has("additive") ? entry.get("additive").getAsBoolean() : additiveByDefault;
        float moveSpeed = 0;
        if (entry.has("move_speed")) {
            moveSpeed = entry.get("move_speed").getAsString().equals("auto") ? Strides.speed(armature, clip)
                    : entry.get("move_speed").getAsFloat();
        }
        List<ClipEvent> events = entry.has("events") ? events(armature, name, clip, entry.getAsJsonArray("events"))
                : List.of();
        RootMotion rootMotion = entry.has("root_motion") ? rootMotion(name, entry.get("root_motion")) : null;
        AttackData attack = entry.has("attack") ? attack(armature, name, clip, entry.getAsJsonObject("attack")) : null;
        return new Animation(set + "/" + name, clip, loop, transition, mask, masks, additive, moveSpeed, events,
                rootMotion, attack);
    }

    private static AttackData attack(Armature armature, String name, Clip clip, JsonObject entry) {
        check(name, "attack", entry, ATTACK_KEYS);
        List<AttackData.Phase> phases = new ArrayList<>();
        if (entry.has("phases")) {
            for (JsonElement element : entry.getAsJsonArray("phases")) {
                phases.add(phase(armature, name, clip, element.getAsJsonObject()));
            }
        }
        List<float[]> invulnerable = new ArrayList<>(), superArmor = new ArrayList<>();
        if (entry.has("invulnerable")) {
            entry.getAsJsonArray("invulnerable").forEach(window -> invulnerable.add(window(window.getAsJsonArray())));
        }
        if (entry.has("super_armor")) {
            entry.getAsJsonArray("super_armor").forEach(window -> superArmor.add(window(window.getAsJsonArray())));
        }
        return new AttackData(phases, invulnerable, superArmor);
    }

    private static AttackData.Phase phase(Armature armature, String name, Clip clip, JsonObject phase) {
        check(name, "phase", phase, PHASE_KEYS);
        if (!phase.has("active")) {
            throw new IllegalArgumentException("clip " + name + ": a phase needs its active window");
        }
        float[] active = window(phase.getAsJsonArray("active"));
        float windup = number(phase, "windup", 0);
        float recovery = number(phase, "recovery", active[1]);
        if (!(windup <= active[0] && active[0] <= active[1] && active[1] <= recovery
                && recovery <= clip.duration() + 1e-4f)) {
            throw new IllegalArgumentException("clip " + name + ": a phase needs windup <= active <= recovery <= the"
                    + " clip's length");
        }
        List<AttackData.Collider> colliders = new ArrayList<>();
        if (phase.has("colliders")) {
            for (JsonElement box : phase.getAsJsonArray("colliders")) {
                JsonObject collider = box.getAsJsonObject();
                check(name, "collider", collider, Set.of("joint", "center", "size"));
                String joint = collider.has("joint") ? collider.get("joint").getAsString() : null;
                if (joint != null && armature.find(joint) == null) {
                    throw new IllegalArgumentException("clip " + name + ": collider on unknown joint " + joint);
                }
                colliders.add(new AttackData.Collider(joint, vector(collider, "center", 0), vector(collider, "size", 4)));
            }
        }
        String hand = phase.has("hand") ? phase.get("hand").getAsString() : "none";
        if (!Set.of("left", "right", "both", "none").contains(hand)) {
            throw new IllegalArgumentException("clip " + name + ": hand is left, right, both or none");
        }
        String kind = phase.has("kind") ? phase.get("kind").getAsString() : "blunt";
        if (!Set.of("blunt", "cut").contains(kind)) {
            throw new IllegalArgumentException("clip " + name + ": kind is blunt or cut");
        }
        String stun = phase.has("stun") ? phase.get("stun").getAsString() : "short";
        return new AttackData.Phase(windup, active[0], active[1], recovery, hand, colliders, number(phase, "damage", 1),
                number(phase, "impact", 1), number(phase, "armor_negation", 0), (int) number(phase, "max_targets", 3),
                AttackData.Stun.valueOf(stun.toUpperCase(Locale.ROOT)), number(phase, "knockback", 0.3f), kind);
    }

    private static void check(String clip, String what, JsonObject entry, Set<String> allowed) {
        for (String key : entry.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("clip " + clip + ": " + what + " has unknown setting " + key);
            }
        }
    }

    private static float[] window(JsonArray pair) {
        return new float[] {pair.get(0).getAsFloat(), pair.get(1).getAsFloat()};
    }

    private static float number(JsonObject entry, String key, float otherwise) {
        return entry.has(key) ? entry.get(key).getAsFloat() : otherwise;
    }

    private static Vector3f vector(JsonObject entry, String key, float otherwise) {
        if (!entry.has(key)) {
            return new Vector3f(otherwise);
        }
        JsonArray xyz = entry.getAsJsonArray(key);
        return new Vector3f(xyz.get(0).getAsFloat(), xyz.get(1).getAsFloat(), xyz.get(2).getAsFloat());
    }

    private static List<ClipEvent> events(Armature armature, String name, Clip clip, JsonArray list) {
        List<ClipEvent> events = new ArrayList<>();
        for (JsonElement element : list) {
            JsonObject entry = element.getAsJsonObject().deepCopy();
            if (!entry.has("at") || !entry.has("type")) {
                throw new IllegalArgumentException("clip " + name + ": an event needs at and type");
            }
            float at = entry.remove("at").getAsFloat();
            String type = entry.remove("type").getAsString();
            ClipEvent.Side side = entry.has("side")
                    ? ClipEvent.Side.valueOf(entry.remove("side").getAsString().toUpperCase()) : ClipEvent.Side.CLIENT;
            if (!ClipEvent.TYPES.containsKey(type)) {
                throw new IllegalArgumentException("clip " + name + ": unknown event type " + type);
            }
            for (String key : entry.keySet()) {
                if (!ClipEvent.TYPES.get(type).contains(key)) {
                    throw new IllegalArgumentException("clip " + name + ": " + type + " event has unknown setting " + key);
                }
            }
            if (at < 0 || at > clip.duration() + 1e-4f) {
                throw new IllegalArgumentException("clip " + name + ": event at " + at + " is outside the clip");
            }
            if (entry.has("joint") && armature.find(entry.get("joint").getAsString()) == null) {
                throw new IllegalArgumentException("clip " + name + ": event names unknown joint " + entry.get("joint"));
            }
            if (entry.has("foot") && !Set.of("left", "right").contains(entry.get("foot").getAsString())) {
                throw new IllegalArgumentException("clip " + name + ": foot is left or right");
            }
            events.add(new ClipEvent(at, type, side, entry));
        }
        return events;
    }

    private static RootMotion rootMotion(String name, JsonElement setting) {
        if (setting.isJsonPrimitive()) {
            return setting.getAsBoolean() ? RootMotion.GROUND : null;
        }
        JsonObject entry = setting.getAsJsonObject();
        for (String key : entry.keySet()) {
            if (!Set.of("vertical", "no_gravity").contains(key)) {
                throw new IllegalArgumentException("clip " + name + ": root_motion has unknown setting " + key);
            }
        }
        List<float[]> windows = new ArrayList<>();
        if (entry.has("no_gravity")) {
            for (JsonElement window : entry.getAsJsonArray("no_gravity")) {
                JsonArray pair = window.getAsJsonArray();
                windows.add(new float[] {pair.get(0).getAsFloat(), pair.get(1).getAsFloat()});
            }
        }
        return new RootMotion(entry.has("vertical") && entry.get("vertical").getAsBoolean(), windows);
    }

    private static JointMask mask(Armature armature, String clip, JsonArray joints) {
        String[] names = new String[joints.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = joints.get(i).getAsString();
            if (armature.find(names[i]) == null) {
                throw new IllegalArgumentException("clip " + clip + ": mask names unknown joint " + names[i]);
            }
        }
        return JointMask.withChildren(armature, names);
    }

    /** The file's animations as clips on {@code armature}: channels are matched to joints by node name. */
    static Map<String, Clip> clips(GltfLoader file, Armature armature) {
        JsonObject root = file.json();
        JsonArray nodes = GltfLoader.array(root, "nodes");
        int[] parents = GltfLoader.parentsOf(nodes);
        Map<Integer, Integer> jointOfNode = new HashMap<>();
        int rootNode = -1;
        for (int node = 0; node < nodes.size(); node++) {
            JsonObject entry = nodes.get(node).getAsJsonObject();
            Joint joint = entry.has("name") ? armature.find(entry.get("name").getAsString()) : null;
            if (joint != null) {
                jointOfNode.put(node, joint.index());
                if (joint.parent() < 0) {
                    rootNode = node;
                }
            }
        }
        // As for the model, whatever sits above the root (the armature object) is folded into the root's values.
        Matrix4f above = new Matrix4f();
        if (rootNode >= 0) {
            for (int up = parents[rootNode]; up >= 0; up = parents[up]) {
                above.mulLocal(GltfLoader.matrix(nodes, up));
            }
        }

        Map<String, Clip> clips = new LinkedHashMap<>();
        for (JsonElement element : GltfLoader.array(root, "animations")) {
            JsonObject animation = element.getAsJsonObject();
            String name = animation.has("name") ? animation.get("name").getAsString() : "clip" + clips.size();
            JsonArray samplers = animation.getAsJsonArray("samplers");
            List<Clip.Channel> channels = new ArrayList<>();
            for (JsonElement channelElement : animation.getAsJsonArray("channels")) {
                JsonObject channel = channelElement.getAsJsonObject();
                JsonObject target = channel.getAsJsonObject("target");
                if (!target.has("node")) {
                    continue;
                }
                Integer joint = jointOfNode.get(target.get("node").getAsInt());
                String pathName = target.get("path").getAsString();
                if (joint == null || pathName.equals("weights")) {
                    // Nodes outside the skeleton (meshes, the armature object) and morph weights are not ours.
                    continue;
                }
                Clip.Path path = Clip.Path.valueOf(pathName.toUpperCase());
                JsonObject sampler = samplers.get(channel.get("sampler").getAsInt()).getAsJsonObject();
                Clip.Interpolation interpolation = sampler.has("interpolation")
                        ? Clip.Interpolation.valueOf(sampler.get("interpolation").getAsString()) : Clip.Interpolation.LINEAR;
                float[] values = file.floats(sampler.get("output").getAsInt());
                if (joint == armature.joint(0).index() && !above.equals(new Matrix4f(), 1e-6f)) {
                    fold(above, path, interpolation, values);
                }
                channels.add(new Clip.Channel(joint, path, interpolation, file.floats(sampler.get("input").getAsInt()),
                        values));
            }
            if (clips.put(name, new Clip(name, channels)) != null) {
                throw new IllegalArgumentException("two clips named " + name);
            }
        }
        return clips;
    }

    /** Puts the root's keyframes in the space above the armature object (values only; tangents are left alone). */
    private static void fold(Matrix4f above, Clip.Path path, Clip.Interpolation interpolation, float[] values) {
        int n = path.size;
        int stride = interpolation == Clip.Interpolation.CUBICSPLINE ? 3 * n : n;
        int first = interpolation == Clip.Interpolation.CUBICSPLINE ? n : 0;
        Quaternionf turn = above.getUnnormalizedRotation(new Quaternionf());
        for (int at = first; at < values.length; at += stride) {
            switch (path) {
                case TRANSLATION -> {
                    Vector3f point = above.transformPosition(new Vector3f(values[at], values[at + 1], values[at + 2]));
                    values[at] = point.x;
                    values[at + 1] = point.y;
                    values[at + 2] = point.z;
                }
                case ROTATION -> {
                    Quaternionf q = new Quaternionf(turn).mul(new Quaternionf(values[at], values[at + 1],
                            values[at + 2], values[at + 3]));
                    values[at] = q.x;
                    values[at + 1] = q.y;
                    values[at + 2] = q.z;
                    values[at + 3] = q.w;
                }
                case SCALE -> {
                    Vector3f scale = above.getScale(new Vector3f());
                    values[at] *= scale.x;
                    values[at + 1] *= scale.y;
                    values[at + 2] *= scale.z;
                }
            }
        }
    }
}
